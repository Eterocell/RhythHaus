#import <AVFoundation/AVFoundation.h>
#import <AppKit/AppKit.h>
#import <Foundation/Foundation.h>
#import <MediaPlayer/MediaPlayer.h>
#import <CoreAudio/CoreAudio.h>

#include <jni.h>
#include <atomic>
#include <dispatch/dispatch.h>
#include <vector>

@class RhythHausAudioPlayer;

@interface RhythHausRouteCallbackContext : NSObject
{
    std::atomic_bool _stopped;
}
@property(nonatomic, weak) RhythHausAudioPlayer *player;
@property(nonatomic, strong) dispatch_queue_t queue;
@property(nonatomic, copy) AudioObjectPropertyListenerBlock devicesBlock;
@property(nonatomic, copy) AudioObjectPropertyListenerBlock defaultBlock;
@property(nonatomic, assign) BOOL devicesRegistered;
@property(nonatomic, assign) BOOL defaultRegistered;
- (void)processRouteSnapshot;
- (void)stopAndDrain;
- (void)stopAndDrainWithListenerRemover:(void (^)(const AudioObjectPropertyAddress *, dispatch_queue_t, AudioObjectPropertyListenerBlock))remover;
- (BOOL)isStopped;
@end

@interface RhythHausAudioPlayer : NSObject
{
    std::atomic_bool _routeDisconnectPending;
    std::atomic_bool _expectedActive;
    AudioDeviceID _trackedDefaultDevice;
    std::vector<AudioDeviceID> _availableDevices;
    RhythHausRouteCallbackContext *_routeContext;
    BOOL _routeSnapshotInjected;
}
@property(nonatomic, strong) AVAudioPlayer *player;
@property(nonatomic, assign) BOOL remoteCommandsRegistered;
@property(nonatomic, strong) MPMediaItemArtwork *artwork;
@property(nonatomic, assign) BOOL transportEnabled;
@property(nonatomic, strong) NSMutableArray<NSDictionary *> *remoteCommandTargets;
- (void)refreshRouteSnapshotWithAvailable:(const std::vector<AudioDeviceID> &)available defaultDevice:(AudioDeviceID)defaultDevice;
- (void)removeRouteListeners;
- (BOOL)simulateRouteSnapshotWithAvailable:(const std::vector<AudioDeviceID> &)available defaultDevice:(AudioDeviceID)defaultDevice;
- (BOOL)consumeRouteDisconnected;
- (void)invokeRouteDisconnect;
- (void)setRouteExpectedActiveForTest:(BOOL)active;
- (BOOL)releaseOnRouteQueueForTest;
- (BOOL)play;
- (void)pause;
- (void)stop;
- (void)seekToMillis:(jlong)positionMillis;
@end

static void releaseNativePlayer(RhythHausAudioPlayer *player);

static MPRemoteCommandHandlerStatus performRemotePlay(RhythHausAudioPlayer *player) {
    if (player == nil || player.player == nil) return MPRemoteCommandHandlerStatusNoSuchContent;
    if (!player.transportEnabled) return MPRemoteCommandHandlerStatusCommandFailed;
    return [player play] ? MPRemoteCommandHandlerStatusSuccess : MPRemoteCommandHandlerStatusCommandFailed;
}

static MPRemoteCommandHandlerStatus performRemotePause(RhythHausAudioPlayer *player) {
    if (player == nil || player.player == nil) return MPRemoteCommandHandlerStatusNoSuchContent;
    if (!player.transportEnabled) return MPRemoteCommandHandlerStatusCommandFailed;
    [player pause];
    return MPRemoteCommandHandlerStatusSuccess;
}

static MPRemoteCommandHandlerStatus performRemoteToggle(RhythHausAudioPlayer *player) {
    if (player == nil || player.player == nil) return MPRemoteCommandHandlerStatusNoSuchContent;
    if (!player.transportEnabled) return MPRemoteCommandHandlerStatusCommandFailed;
    return player.player.isPlaying ? performRemotePause(player) : performRemotePlay(player);
}

static MPRemoteCommandHandlerStatus performRemoteStop(RhythHausAudioPlayer *player) {
    if (player == nil || player.player == nil) return MPRemoteCommandHandlerStatusNoSuchContent;
    if (!player.transportEnabled) return MPRemoteCommandHandlerStatusCommandFailed;
    [player stop];
    return MPRemoteCommandHandlerStatusSuccess;
}

static MPRemoteCommandHandlerStatus performRemoteSeek(RhythHausAudioPlayer *player, jlong positionMillis) {
    if (player == nil || player.player == nil) return MPRemoteCommandHandlerStatusNoSuchContent;
    if (!player.transportEnabled) return MPRemoteCommandHandlerStatusCommandFailed;
    [player seekToMillis:positionMillis];
    return MPRemoteCommandHandlerStatusSuccess;
}

static NSInteger liveRemoteHandlerCount = 0;
static NSInteger liveRouteListenerCount = 0;
static void *routeQueueSpecificKey = &routeQueueSpecificKey;
static const AudioObjectPropertyAddress devicesAddress = {
    kAudioHardwarePropertyDevices, kAudioObjectPropertyScopeGlobal, kAudioObjectPropertyElementMain};
static const AudioObjectPropertyAddress defaultOutputAddress = {
    kAudioHardwarePropertyDefaultOutputDevice, kAudioObjectPropertyScopeGlobal, kAudioObjectPropertyElementMain};

static bool containsDevice(const std::vector<AudioDeviceID> &devices, AudioDeviceID device) {
    for (AudioDeviceID candidate : devices) if (candidate == device) return true;
    return false;
}

static bool readRouteSnapshot(std::vector<AudioDeviceID> &available, AudioDeviceID &defaultDevice) {
    UInt32 size = 0;
    if (AudioObjectGetPropertyDataSize(kAudioObjectSystemObject, &devicesAddress, 0, nullptr, &size) != noErr) return false;
    std::vector<AudioDeviceID> nextAvailable(size / sizeof(AudioDeviceID));
    if (size > 0 && AudioObjectGetPropertyData(kAudioObjectSystemObject, &devicesAddress, 0, nullptr, &size, nextAvailable.data()) != noErr) return false;
    AudioDeviceID nextDefaultDevice = kAudioObjectUnknown;
    size = sizeof(defaultDevice);
    if (AudioObjectGetPropertyData(kAudioObjectSystemObject, &defaultOutputAddress, 0, nullptr, &size, &nextDefaultDevice) != noErr) return false;
    available = std::move(nextAvailable);
    defaultDevice = nextDefaultDevice;
    return true;
}

// Keep registration bookkeeping separate from CoreAudio so native lifecycle tests can model
// partial registration without depending on the machine's available audio properties.
static NSInteger recordRouteListenerRegistration(RhythHausRouteCallbackContext *context,
                                                 OSStatus devicesStatus,
                                                 OSStatus defaultStatus) {
    context.devicesRegistered = devicesStatus == noErr;
    context.defaultRegistered = defaultStatus == noErr;
    NSInteger registered = (context.devicesRegistered ? 1 : 0) + (context.defaultRegistered ? 1 : 0);
    if (registered > 0) {
        @synchronized([RhythHausAudioPlayer class]) { liveRouteListenerCount += registered; }
    }
    return registered;
}

static void removeRouteListener(const AudioObjectPropertyAddress *address,
                                dispatch_queue_t queue,
                                AudioObjectPropertyListenerBlock block) {
    AudioObjectRemovePropertyListenerBlock(kAudioObjectSystemObject, address, queue, block);
}

@implementation RhythHausRouteCallbackContext
- (instancetype)init {
    self = [super init];
    if (self != nil) _stopped.store(false);
    return self;
}

- (void)processRouteSnapshot {
    if (_stopped.load()) return;
    RhythHausAudioPlayer *player = self.player;
    if (player == nil || _stopped.load()) return;
    std::vector<AudioDeviceID> available;
    AudioDeviceID defaultDevice = kAudioObjectUnknown;
    if (readRouteSnapshot(available, defaultDevice)) {
        @synchronized (player) {
            [player refreshRouteSnapshotWithAvailable:available defaultDevice:defaultDevice];
        }
    }
}

- (void)stopAndDrain {
    [self stopAndDrainWithListenerRemover:^(const AudioObjectPropertyAddress *address, dispatch_queue_t queue, AudioObjectPropertyListenerBlock block) {
        removeRouteListener(address, queue, block);
    }];
}

- (void)stopAndDrainWithListenerRemover:(void (^)(const AudioObjectPropertyAddress *, dispatch_queue_t, AudioObjectPropertyListenerBlock))remover {
    _stopped.store(true);
    // Keep the context alive as a tombstone until CoreAudio has stopped referring to either
    // listener block and all already-enqueued work has completed.
    RhythHausRouteCallbackContext *tombstone = self;
    dispatch_queue_t queue = tombstone.queue;
    AudioObjectPropertyListenerBlock devicesBlock = tombstone.devicesBlock;
    AudioObjectPropertyListenerBlock defaultBlock = tombstone.defaultBlock;
    NSInteger registered = tombstone.devicesRegistered + tombstone.defaultRegistered;
    if (tombstone.devicesRegistered) {
        remover(&devicesAddress, queue, devicesBlock);
        tombstone.devicesRegistered = NO;
    }
    if (tombstone.defaultRegistered) {
        remover(&defaultOutputAddress, queue, defaultBlock);
        tombstone.defaultRegistered = NO;
    }
    if (queue && dispatch_get_specific(routeQueueSpecificKey) != (__bridge void *)tombstone) {
        dispatch_sync(queue, ^{});
    }
    tombstone.devicesBlock = nil;
    tombstone.defaultBlock = nil;
    tombstone.player = nil;
    if (registered > 0) {
        @synchronized([RhythHausAudioPlayer class]) { liveRouteListenerCount -= registered; }
    }
}

- (BOOL)isStopped {
    return _stopped.load();
}
@end

@implementation RhythHausAudioPlayer

- (instancetype)init {
    self = [super init];
    if (self != nil) {
        _transportEnabled = YES;
        _routeDisconnectPending.store(false);
        _expectedActive.store(false);
        _trackedDefaultDevice = kAudioObjectUnknown;
        _routeSnapshotInjected = NO;
        readRouteSnapshot(_availableDevices, _trackedDefaultDevice);
        _routeContext = [[RhythHausRouteCallbackContext alloc] init];
        _routeContext.player = self;
        _routeContext.queue = dispatch_queue_create("com.eterocell.rhythhaus.route", DISPATCH_QUEUE_SERIAL);
        dispatch_queue_set_specific(_routeContext.queue, routeQueueSpecificKey, (__bridge void *)_routeContext, nullptr);
        __weak RhythHausRouteCallbackContext *weakContext = _routeContext;
        _routeContext.devicesBlock = ^(UInt32, const AudioObjectPropertyAddress *) {
            RhythHausRouteCallbackContext *context = weakContext;
            if (context != nil) dispatch_async(context.queue, ^{ [context processRouteSnapshot]; });
        };
        _routeContext.defaultBlock = ^(UInt32, const AudioObjectPropertyAddress *) {
            RhythHausRouteCallbackContext *context = weakContext;
            if (context != nil) dispatch_async(context.queue, ^{ [context processRouteSnapshot]; });
        };
        OSStatus devicesStatus = AudioObjectAddPropertyListenerBlock(kAudioObjectSystemObject, &devicesAddress, _routeContext.queue, _routeContext.devicesBlock);
        OSStatus defaultStatus = AudioObjectAddPropertyListenerBlock(kAudioObjectSystemObject, &defaultOutputAddress, _routeContext.queue, _routeContext.defaultBlock);
        NSInteger registered = recordRouteListenerRegistration(_routeContext, devicesStatus, defaultStatus);
        if (devicesStatus != noErr || defaultStatus != noErr) [self removeRouteListeners];
    }
    return self;
}

- (BOOL)loadPath:(NSString *)path {
    NSURL *url = [NSURL fileURLWithPath:path];
    NSError *error = nil;
    self.player = [[AVAudioPlayer alloc] initWithContentsOfURL:url error:&error];
    if (self.player == nil || error != nil) {
        self.player = nil;
        return NO;
    }
    return [self.player prepareToPlay];
}

- (BOOL)play {
    if (self.player == nil) {
        return NO;
    }
    @synchronized (self) {
        [self.player play];
        _expectedActive.store(self.player.isPlaying);
        _routeDisconnectPending.store(false);
    }
    return YES;
}

- (void)pause {
    @synchronized (self) {
        _expectedActive.store(false);
        _routeDisconnectPending.store(false);
        [self.player pause];
    }
}

- (void)stop {
    @synchronized (self) {
        _expectedActive.store(false);
        _routeDisconnectPending.store(false);
        [self.player stop];
        self.player.currentTime = 0.0;
    }
}

- (void)seekToMillis:(jlong)positionMillis {
    if (self.player == nil) {
        return;
    }
    self.player.currentTime = MAX(0.0, ((double)positionMillis) / 1000.0);
}

- (jlong)currentPositionMillis {
    if (self.player == nil) {
        return 0;
    }
    return (jlong)(self.player.currentTime * 1000.0);
}

- (jlong)durationMillis {
    if (self.player == nil || self.player.duration <= 0.0) {
        return 0;
    }
    return (jlong)(self.player.duration * 1000.0);
}

- (void)updateNowPlayingTitle:(NSString *)title artist:(NSString *)artist album:(NSString *)album durationMillis:(jlong)durationMillis positionMillis:(jlong)positionMillis {
    NSMutableDictionary *info = [NSMutableDictionary dictionary];
    if (title.length > 0) {
        info[MPMediaItemPropertyTitle] = title;
    }
    if (artist.length > 0) {
        info[MPMediaItemPropertyArtist] = artist;
    }
    if (album.length > 0) {
        info[MPMediaItemPropertyAlbumTitle] = album;
    }
    if (durationMillis > 0) {
        info[MPMediaItemPropertyPlaybackDuration] = @(((double)durationMillis) / 1000.0);
    }
    if (self.artwork != nil) {
        info[MPMediaItemPropertyArtwork] = self.artwork;
    }
    info[MPNowPlayingInfoPropertyElapsedPlaybackTime] = @(((double)MAX((jlong)0, positionMillis)) / 1000.0);
    info[MPNowPlayingInfoPropertyPlaybackRate] = @(self.player.isPlaying ? 1.0 : 0.0);
    [MPNowPlayingInfoCenter defaultCenter].nowPlayingInfo = info;
}

- (void)updateNowPlayingPositionMillis:(jlong)positionMillis durationMillis:(jlong)durationMillis {
    NSMutableDictionary *info = [[MPNowPlayingInfoCenter defaultCenter].nowPlayingInfo mutableCopy];
    if (info == nil) {
        info = [NSMutableDictionary dictionary];
    }
    if (self.artwork != nil) {
        info[MPMediaItemPropertyArtwork] = self.artwork;
    }
    info[MPNowPlayingInfoPropertyElapsedPlaybackTime] = @(((double)MAX((jlong)0, positionMillis)) / 1000.0);
    if (durationMillis > 0) {
        info[MPMediaItemPropertyPlaybackDuration] = @(((double)durationMillis) / 1000.0);
    }
    info[MPNowPlayingInfoPropertyPlaybackRate] = @(self.player.isPlaying ? 1.0 : 0.0);
    [MPNowPlayingInfoCenter defaultCenter].nowPlayingInfo = info;
}

- (void)updateNowPlayingPlaybackStateCode:(jint)playbackStateCode {
    MPNowPlayingInfoCenter *center = [MPNowPlayingInfoCenter defaultCenter];
    NSMutableDictionary *info = [center.nowPlayingInfo mutableCopy] ?: [NSMutableDictionary dictionary];
    if (self.artwork != nil) {
        info[MPMediaItemPropertyArtwork] = self.artwork;
    }
    if (playbackStateCode == 1) {
        center.playbackState = MPNowPlayingPlaybackStatePlaying;
        info[MPNowPlayingInfoPropertyPlaybackRate] = @(1.0);
    } else if (playbackStateCode == 2) {
        center.playbackState = MPNowPlayingPlaybackStatePaused;
        info[MPNowPlayingInfoPropertyPlaybackRate] = @(0.0);
    } else {
        center.playbackState = MPNowPlayingPlaybackStateStopped;
        info[MPNowPlayingInfoPropertyPlaybackRate] = @(0.0);
    }
    center.nowPlayingInfo = info;
}

- (void)registerRemoteCommands {
    if (self.remoteCommandsRegistered) {
        return;
    }
    self.remoteCommandsRegistered = YES;
    MPRemoteCommandCenter *commandCenter = [MPRemoteCommandCenter sharedCommandCenter];
    commandCenter.playCommand.enabled = YES;
    commandCenter.pauseCommand.enabled = YES;
    commandCenter.togglePlayPauseCommand.enabled = YES;
    commandCenter.stopCommand.enabled = YES;
    commandCenter.changePlaybackPositionCommand.enabled = YES;

    __weak RhythHausAudioPlayer *weakSelf = self;
    self.remoteCommandTargets = [NSMutableArray arrayWithCapacity:5];
    id playTarget = [commandCenter.playCommand addTargetWithHandler:^MPRemoteCommandHandlerStatus(MPRemoteCommandEvent *) {
        RhythHausAudioPlayer *strongSelf = weakSelf;
        return performRemotePlay(strongSelf);
    }];
    id pauseTarget = [commandCenter.pauseCommand addTargetWithHandler:^MPRemoteCommandHandlerStatus(MPRemoteCommandEvent *) {
        RhythHausAudioPlayer *strongSelf = weakSelf;
        return performRemotePause(strongSelf);
    }];
    id toggleTarget = [commandCenter.togglePlayPauseCommand addTargetWithHandler:^MPRemoteCommandHandlerStatus(MPRemoteCommandEvent *) {
        RhythHausAudioPlayer *strongSelf = weakSelf;
        return performRemoteToggle(strongSelf);
    }];
    id stopTarget = [commandCenter.stopCommand addTargetWithHandler:^MPRemoteCommandHandlerStatus(MPRemoteCommandEvent *) {
        RhythHausAudioPlayer *strongSelf = weakSelf;
        return performRemoteStop(strongSelf);
    }];
    id seekTarget = [commandCenter.changePlaybackPositionCommand addTargetWithHandler:^MPRemoteCommandHandlerStatus(MPRemoteCommandEvent *event) {
        RhythHausAudioPlayer *strongSelf = weakSelf;
        if (strongSelf == nil || ![event isKindOfClass:[MPChangePlaybackPositionCommandEvent class]]) return MPRemoteCommandHandlerStatusNoSuchContent;
        MPChangePlaybackPositionCommandEvent *positionEvent = (MPChangePlaybackPositionCommandEvent *)event;
        return performRemoteSeek(strongSelf, (jlong)(positionEvent.positionTime * 1000.0));
    }];
    NSArray *registrations = @[
        @{ @"command": commandCenter.playCommand, @"target": playTarget },
        @{ @"command": commandCenter.pauseCommand, @"target": pauseTarget },
        @{ @"command": commandCenter.togglePlayPauseCommand, @"target": toggleTarget },
        @{ @"command": commandCenter.stopCommand, @"target": stopTarget },
        @{ @"command": commandCenter.changePlaybackPositionCommand, @"target": seekTarget },
    ];
    [self.remoteCommandTargets addObjectsFromArray:registrations];
    @synchronized([RhythHausAudioPlayer class]) { liveRemoteHandlerCount += registrations.count; }
}

- (void)removeRemoteCommands {
    if (!self.remoteCommandsRegistered) return;
    for (NSDictionary *registration in self.remoteCommandTargets) {
        MPRemoteCommand *command = registration[@"command"];
        [command removeTarget:registration[@"target"]];
    }
    @synchronized([RhythHausAudioPlayer class]) { liveRemoteHandlerCount -= self.remoteCommandTargets.count; }
    [self.remoteCommandTargets removeAllObjects];
    self.remoteCommandTargets = nil;
    self.remoteCommandsRegistered = NO;
}

- (void)setArtworkFromBytes:(const unsigned char *)bytes length:(NSUInteger)length {
    if (bytes == NULL || length == 0) {
        self.artwork = nil;
        return;
    }
    NSData *data = [NSData dataWithBytes:bytes length:length];
    NSImage *image = [[NSImage alloc] initWithData:data];
    if (image == nil) {
        self.artwork = nil;
        return;
    }
    self.artwork = [[MPMediaItemArtwork alloc] initWithBoundsSize:image.size requestHandler:^NSImage * _Nonnull(CGSize size) {
        return image;
    }];
}

- (void)clearNowPlayingInfo {
    [MPNowPlayingInfoCenter defaultCenter].playbackState = MPNowPlayingPlaybackStateStopped;
    [MPNowPlayingInfoCenter defaultCenter].nowPlayingInfo = nil;
}

- (void)refreshRouteSnapshotWithAvailable:(const std::vector<AudioDeviceID> &)available defaultDevice:(AudioDeviceID)defaultDevice {
    @synchronized (self) {
        if (_expectedActive.load() && _trackedDefaultDevice != kAudioObjectUnknown && !containsDevice(available, _trackedDefaultDevice)) {
            _routeDisconnectPending.store(true);
        }
        _availableDevices = available;
        _trackedDefaultDevice = defaultDevice;
    }
}

- (BOOL)simulateRouteSnapshotWithAvailable:(const std::vector<AudioDeviceID> &)available defaultDevice:(AudioDeviceID)defaultDevice {
    @synchronized (self) {
    if (!_routeSnapshotInjected) {
        _availableDevices = available;
        _trackedDefaultDevice = defaultDevice;
        _routeSnapshotInjected = YES;
    } else {
        [self refreshRouteSnapshotWithAvailable:available defaultDevice:defaultDevice];
    }
    return YES;
    }
}

- (void)invokeRouteDisconnect {
    @synchronized (self) {
    if (_expectedActive.load()) _routeDisconnectPending.store(true);
    }
}

- (void)setRouteExpectedActiveForTest:(BOOL)active {
    @synchronized (self) {
        _expectedActive.store(active);
    }
}

- (BOOL)routeLifecyclePartialRegistrationForTest {
    RhythHausRouteCallbackContext *context = [[RhythHausRouteCallbackContext alloc] init];
    context.player = self;
    context.queue = dispatch_queue_create("com.eterocell.rhythhaus.route.test", DISPATCH_QUEUE_SERIAL);
    dispatch_queue_set_specific(context.queue, routeQueueSpecificKey, (__bridge void *)context, nullptr);
    __weak RhythHausRouteCallbackContext *weakContext = context;
    context.devicesBlock = ^(UInt32, const AudioObjectPropertyAddress *) {
        RhythHausRouteCallbackContext *callbackContext = weakContext;
        if (callbackContext != nil) dispatch_async(callbackContext.queue, ^{ [callbackContext processRouteSnapshot]; });
    };
    context.defaultBlock = ^(UInt32, const AudioObjectPropertyAddress *) {};
    recordRouteListenerRegistration(context, noErr, -1);
    __block NSInteger removed = 0;
    dispatch_sync(context.queue, ^{
        [context stopAndDrainWithListenerRemover:^(const AudioObjectPropertyAddress *, dispatch_queue_t, AudioObjectPropertyListenerBlock) {
            removed += 1;
        }];
    });
    BOOL partialRegistrationDrained = [context isStopped] && removed == 1 && !context.devicesRegistered && !context.defaultRegistered;

    return partialRegistrationDrained;
}

- (BOOL)releaseOnRouteQueueForTest {
    __block BOOL releaseCompleted = NO;
    dispatch_queue_t routeQueue = _routeContext.queue;
    if (routeQueue == nil) {
        releaseNativePlayer(self);
        releaseCompleted = YES;
    } else {
        dispatch_sync(routeQueue, ^{
            releaseNativePlayer(self);
            releaseCompleted = YES;
        });
    }
    return releaseCompleted;
}

- (BOOL)consumeRouteDisconnected {
    @synchronized (self) {
    return _routeDisconnectPending.exchange(false);
    }
}

- (void)removeRouteListeners {
    RhythHausRouteCallbackContext *context = _routeContext;
    if (context == nil) return;
    [context stopAndDrain];
    _routeContext = nil;
}

@end

static RhythHausAudioPlayer *playerFromHandle(jlong handle) {
    return (__bridge RhythHausAudioPlayer *)(void *)handle;
}

static void releaseNativePlayer(RhythHausAudioPlayer *player) {
    if (player != nil) {
        [player removeRouteListeners];
        [player pause];
        [player removeRemoteCommands];
        [player clearNowPlayingInfo];
        [player stop];
        CFRelease((__bridge CFTypeRef)player);
    }
}

extern "C" JNIEXPORT jlong JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeCreate(JNIEnv *, jobject) {
    RhythHausAudioPlayer *player = [[RhythHausAudioPlayer alloc] init];
    return (jlong)(__bridge_retained void *)player;
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeLoad(JNIEnv *env, jobject, jlong handle, jstring path) {
    RhythHausAudioPlayer *player = playerFromHandle(handle);
    if (player == nil || path == NULL) {
        return JNI_FALSE;
    }
    const char *utfPath = env->GetStringUTFChars(path, NULL);
    if (utfPath == NULL) {
        return JNI_FALSE;
    }
    NSString *nsPath = [NSString stringWithUTF8String:utfPath];
    env->ReleaseStringUTFChars(path, utfPath);
    return [player loadPath:nsPath] ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativePlay(JNIEnv *, jobject, jlong handle) {
    RhythHausAudioPlayer *player = playerFromHandle(handle);
    return player != nil && [player play] ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativePause(JNIEnv *, jobject, jlong handle) {
    [playerFromHandle(handle) pause];
}

extern "C" JNIEXPORT void JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeStop(JNIEnv *, jobject, jlong handle) {
    [playerFromHandle(handle) stop];
}

extern "C" JNIEXPORT void JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeSeekTo(JNIEnv *, jobject, jlong handle, jlong positionMillis) {
    [playerFromHandle(handle) seekToMillis:positionMillis];
}

extern "C" JNIEXPORT jlong JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeCurrentPositionMillis(JNIEnv *, jobject, jlong handle) {
    return [playerFromHandle(handle) currentPositionMillis];
}

extern "C" JNIEXPORT jlong JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeDurationMillis(JNIEnv *, jobject, jlong handle) {
    return [playerFromHandle(handle) durationMillis];
}

extern "C" JNIEXPORT void JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeUpdateNowPlayingInfo(JNIEnv *env, jobject, jlong handle, jstring title, jstring artist, jstring album, jlong durationMillis, jlong positionMillis) {
    RhythHausAudioPlayer *player = playerFromHandle(handle);
    if (player == nil) {
        return;
    }
    const char *utfTitle = title == NULL ? NULL : env->GetStringUTFChars(title, NULL);
    const char *utfArtist = artist == NULL ? NULL : env->GetStringUTFChars(artist, NULL);
    const char *utfAlbum = album == NULL ? NULL : env->GetStringUTFChars(album, NULL);
    NSString *nsTitle = utfTitle == NULL ? @"" : [NSString stringWithUTF8String:utfTitle];
    NSString *nsArtist = utfArtist == NULL ? @"" : [NSString stringWithUTF8String:utfArtist];
    NSString *nsAlbum = utfAlbum == NULL ? nil : [NSString stringWithUTF8String:utfAlbum];
    [player updateNowPlayingTitle:nsTitle artist:nsArtist album:nsAlbum durationMillis:durationMillis positionMillis:positionMillis];
    if (utfTitle != NULL) {
        env->ReleaseStringUTFChars(title, utfTitle);
    }
    if (utfArtist != NULL) {
        env->ReleaseStringUTFChars(artist, utfArtist);
    }
    if (utfAlbum != NULL) {
        env->ReleaseStringUTFChars(album, utfAlbum);
    }
}

extern "C" JNIEXPORT void JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeUpdateNowPlayingPosition(JNIEnv *, jobject, jlong handle, jlong positionMillis, jlong durationMillis) {
    [playerFromHandle(handle) updateNowPlayingPositionMillis:positionMillis durationMillis:durationMillis];
}

extern "C" JNIEXPORT void JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeUpdateNowPlayingPlaybackState(JNIEnv *, jobject, jlong handle, jint playbackStateCode) {
    [playerFromHandle(handle) updateNowPlayingPlaybackStateCode:playbackStateCode];
}

extern "C" JNIEXPORT void JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeRegisterNowPlayingRemoteCommands(JNIEnv *, jobject, jlong handle) {
    [playerFromHandle(handle) registerRemoteCommands];
}

extern "C" JNIEXPORT void JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeClearNowPlayingInfo(JNIEnv *, jobject, jlong handle) {
    [playerFromHandle(handle) clearNowPlayingInfo];
}

extern "C" JNIEXPORT void JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeSetArtwork(JNIEnv *env, jobject, jlong handle, jbyteArray artworkBytes) {
    RhythHausAudioPlayer *player = playerFromHandle(handle);
    if (player == nil) {
        return;
    }
    if (artworkBytes == NULL) {
        [player setArtworkFromBytes:NULL length:0];
        return;
    }
    jsize length = env->GetArrayLength(artworkBytes);
    if (length <= 0) {
        [player setArtworkFromBytes:NULL length:0];
        return;
    }
    jbyte *bytes = env->GetByteArrayElements(artworkBytes, NULL);
    if (bytes == NULL) {
        return;
    }
    [player setArtworkFromBytes:(const unsigned char *)bytes length:(NSUInteger)length];
    env->ReleaseByteArrayElements(artworkBytes, bytes, JNI_ABORT);
}

extern "C" JNIEXPORT void JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeSetTransportEnabled(JNIEnv *, jobject, jlong handle, jboolean enabled) {
    RhythHausAudioPlayer *player = playerFromHandle(handle);
    if (player != nil) {
        player.transportEnabled = enabled == JNI_TRUE;
    }
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeInvokeRemotePlayForTest(JNIEnv *, jobject, jlong handle) {
    return performRemotePlay(playerFromHandle(handle)) == MPRemoteCommandHandlerStatusSuccess ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeInvokeRemotePauseForTest(JNIEnv *, jobject, jlong handle) {
    return performRemotePause(playerFromHandle(handle)) == MPRemoteCommandHandlerStatusSuccess ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeInvokeRemoteToggleForTest(JNIEnv *, jobject, jlong handle) {
    return performRemoteToggle(playerFromHandle(handle)) == MPRemoteCommandHandlerStatusSuccess ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeInvokeRemoteStopForTest(JNIEnv *, jobject, jlong handle) {
    return performRemoteStop(playerFromHandle(handle)) == MPRemoteCommandHandlerStatusSuccess ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeInvokeRemoteSeekForTest(JNIEnv *, jobject, jlong handle, jlong positionMillis) {
    return performRemoteSeek(playerFromHandle(handle), positionMillis) == MPRemoteCommandHandlerStatusSuccess ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeIsPlayingForTest(JNIEnv *, jobject, jlong handle) {
    RhythHausAudioPlayer *player = playerFromHandle(handle);
    return player != nil && player.player.isPlaying ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeInvokeRouteDisconnectForTest(JNIEnv *, jobject, jlong handle) {
    RhythHausAudioPlayer *player = playerFromHandle(handle);
    if (player == nil) return JNI_FALSE;
    [player invokeRouteDisconnect];
    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeSetRouteExpectedActiveForTest(JNIEnv *, jobject, jlong handle, jboolean active) {
    [playerFromHandle(handle) setRouteExpectedActiveForTest:active == JNI_TRUE];
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeRouteLifecyclePartialRegistrationForTest(JNIEnv *, jobject, jlong handle) {
    RhythHausAudioPlayer *player = playerFromHandle(handle);
    return player != nil && [player routeLifecyclePartialRegistrationForTest] ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_eterocell_rhythhaus_JvmPlaybackEngineTest_nativeReleaseOnRouteQueueForTest(JNIEnv *, jclass, jlong handle) {
    RhythHausAudioPlayer *player = playerFromHandle(handle);
    return player != nil && [player releaseOnRouteQueueForTest] ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeConsumeRouteDisconnected(JNIEnv *, jobject, jlong handle) {
    return [playerFromHandle(handle) consumeRouteDisconnected] ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeSimulateRouteSnapshotForTest(JNIEnv *env, jobject, jlong handle, jlongArray ids, jlong defaultDevice) {
    RhythHausAudioPlayer *player = playerFromHandle(handle);
    if (player == nil || ids == NULL) return JNI_FALSE;
    jsize count = env->GetArrayLength(ids);
    std::vector<jlong> values((size_t)count);
    env->GetLongArrayRegion(ids, 0, count, values.data());
    std::vector<AudioDeviceID> available;
    for (jlong value : values) available.push_back((AudioDeviceID)value);
    return [player simulateRouteSnapshotWithAvailable:available defaultDevice:(AudioDeviceID)defaultDevice] ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jlong JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeNowPlayingPositionMillisForTest(JNIEnv *, jobject, jlong handle) {
    if (playerFromHandle(handle) == nil) return 0;
    NSNumber *position = [MPNowPlayingInfoCenter defaultCenter].nowPlayingInfo[MPNowPlayingInfoPropertyElapsedPlaybackTime];
    return position == nil ? 0 : (jlong)(position.doubleValue * 1000.0);
}

extern "C" JNIEXPORT jlong JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeLiveRouteListenerCountForTest(JNIEnv *, jobject) {
    @synchronized([RhythHausAudioPlayer class]) { return (jlong)liveRouteListenerCount; }
}

extern "C" JNIEXPORT jlong JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeLiveRemoteHandlerCountForTest(JNIEnv *, jobject) {
    @synchronized([RhythHausAudioPlayer class]) { return (jlong)liveRemoteHandlerCount; }
}

extern "C" JNIEXPORT void JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeRelease(JNIEnv *, jobject, jlong handle) {
    releaseNativePlayer(playerFromHandle(handle));
}
