#import <AVFoundation/AVFoundation.h>
#import <AppKit/AppKit.h>
#import <Foundation/Foundation.h>
#import <MediaPlayer/MediaPlayer.h>
#import <CoreAudio/CoreAudio.h>

#include <jni.h>
#include <atomic>
#include <cstdio>
#include <cstdlib>
#include <dispatch/dispatch.h>
#include <vector>

@class RhythHausAudioPlayer;
@class RhythHausVoiceCallbackContext;

static std::atomic_long liveAudioVoiceCount(0);

// Opt-in probes distinguish decoding/preparation, the device clock and the
// synchronous audio-hardware start. They never alter the selected output or retry.
static bool audioStartDiagnosticsEnabled() {
    static const bool enabled = [] {
        const char *value = std::getenv("RHYTHHAUS_MAC_AUDIO_DIAGNOSTICS");
        return value != nullptr && value[0] == '1' && value[1] == '\0';
    }();
    return enabled;
}

@interface RhythHausAudioVoice : NSObject
@property(nonatomic, strong) AVAudioPlayer *audioPlayer;
@property(nonatomic, strong) RhythHausVoiceCallbackContext *callback;
@property(nonatomic, assign) jlong generation;
@property(nonatomic, assign) jlong playbackRunId;
@property(nonatomic, assign) BOOL hasPlaybackRun;
@property(nonatomic, assign) BOOL expectedPlaying;
@property(nonatomic, assign) NSTimeInterval expectedEndDeviceTime;
@property(nonatomic, assign) BOOL finished;
@property(nonatomic, assign) BOOL failed;
@property(nonatomic, assign) BOOL terminalPending;
@property(nonatomic, assign) jlong terminalGeneration;
@property(nonatomic, assign) jlong terminalRunId;
@property(nonatomic, assign) float gain;
@end

// A playback delegate is a run-owned immutable callback context. Neither an outgoing
// callback nor work queued before replacement reads the handle's mutable current token.
@interface RhythHausVoiceCallbackContext : NSObject <AVAudioPlayerDelegate>
@property(nonatomic, weak) RhythHausAudioPlayer *owner;
@property(nonatomic, weak) AVAudioPlayer *audioPlayer;
@property(nonatomic, assign) jlong generation;
@property(nonatomic, assign) jlong playbackRunId;
@end

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
    float _playbackGain;
    JavaVM *_callbackVM;
    jweak _remoteBridge;
}
@property(nonatomic, readonly) AVAudioPlayer *player;
@property(nonatomic, strong) RhythHausAudioVoice *currentVoice;
@property(nonatomic, strong) RhythHausAudioVoice *outgoingVoice;
@property(nonatomic, strong) RhythHausAudioVoice *preparedVoice;
@property(nonatomic, copy) NSString *lastStartFailure;
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
- (BOOL)loadPath:(NSString *)path generation:(jlong)generation;
- (BOOL)prepareIncomingPath:(NSString *)path generation:(jlong)generation;
- (BOOL)startIncomingWithRunId:(jlong)playbackRunId;
- (jlong)incomingDurationMillis;
- (void)setCrossfadeOutgoingGain:(jfloat)outgoingGain incomingGain:(jfloat)incomingGain;
- (void)finishCrossfade;
- (void)cancelPreparedIncoming;
- (BOOL)outgoingHasCompleted;
- (BOOL)consumePlaybackEvent:(jlong *)event;
- (void)recordTerminalForPlayer:(AVAudioPlayer *)player context:(RhythHausVoiceCallbackContext *)context failed:(BOOL)failed;
- (BOOL)playWithRunId:(jlong)playbackRunId;
- (void)pause;
- (void)stop;
- (void)seekToMillis:(jlong)positionMillis;
- (void)setPlaybackGain:(jfloat)gain;
- (jfloat)playbackGain;
- (jfloat)playerVolumeForTest;
- (void)installRemoteBridge:(JNIEnv *)env bridge:(jobject)bridge;
- (void)removeRemoteBridge;
- (BOOL)dispatchRemoteCommand:(jint)command positionMillis:(jlong)positionMillis;
@end

static void releaseNativePlayer(RhythHausAudioPlayer *player);

static void refreshVoiceCompletion(RhythHausAudioVoice *voice) {
    // JNI callers do not necessarily have a Cocoa run loop for AVAudioPlayer's
    // delegate. Polling still records the original run-owned callback token.
    if (voice != nil && voice.expectedPlaying && !voice.audioPlayer.isPlaying && !voice.finished &&
        voice.audioPlayer.deviceCurrentTime >= voice.expectedEndDeviceTime) {
        voice.finished = YES;
        voice.expectedPlaying = NO;
        voice.terminalPending = YES;
        voice.terminalGeneration = voice.callback.generation;
        voice.terminalRunId = voice.callback.playbackRunId;
    }
}

static jlong voicePositionMillis(RhythHausAudioVoice *voice) {
    if (voice == nil) return 0;
    refreshVoiceCompletion(voice);
    return (jlong)((voice.finished && !voice.failed ? voice.audioPlayer.duration : voice.audioPlayer.currentTime) * 1000.0);
}

static void disposeVoice(RhythHausAudioVoice *voice) {
    if (voice == nil) return;
    voice.callback.owner = nil;
    voice.audioPlayer.delegate = nil;
    [voice.audioPlayer stop];
    voice.callback = nil;
    voice.audioPlayer = nil;
}

@implementation RhythHausAudioVoice
- (instancetype)init {
    self = [super init];
    if (self != nil) liveAudioVoiceCount.fetch_add(1);
    return self;
}

- (void)dealloc {
    _callback.owner = nil;
    _audioPlayer.delegate = nil;
    [_audioPlayer stop];
    liveAudioVoiceCount.fetch_sub(1);
}
@end

@implementation RhythHausVoiceCallbackContext
- (void)audioPlayerDidFinishPlaying:(AVAudioPlayer *)player successfully:(BOOL)flag {
    RhythHausAudioPlayer *owner = self.owner;
    [owner recordTerminalForPlayer:player context:self failed:!flag];
}

- (void)audioPlayerDecodeErrorDidOccur:(AVAudioPlayer *)player error:(NSError *)error {
    if (audioStartDiagnosticsEnabled()) {
        std::fprintf(stderr, "[rhythhaus-mac-start] decode-error player=%p generation=%lld run=%lld domain=%s code=%ld\n",
                     (__bridge void *)player, (long long)self.generation, (long long)self.playbackRunId,
                     error == nil ? "(none)" : error.domain.UTF8String, (long)error.code);
    }
    RhythHausAudioPlayer *owner = self.owner;
    [owner recordTerminalForPlayer:player context:self failed:YES];
}
@end

static MPRemoteCommandHandlerStatus performRemotePlay(RhythHausAudioPlayer *player) {
    if (player == nil || player.player == nil) return MPRemoteCommandHandlerStatusNoSuchContent;
    if (!player.transportEnabled) return MPRemoteCommandHandlerStatusCommandFailed;
    return [player dispatchRemoteCommand:1 positionMillis:0] ? MPRemoteCommandHandlerStatusSuccess : MPRemoteCommandHandlerStatusCommandFailed;
}

static MPRemoteCommandHandlerStatus performRemotePause(RhythHausAudioPlayer *player) {
    if (player == nil || player.player == nil) return MPRemoteCommandHandlerStatusNoSuchContent;
    if (!player.transportEnabled) return MPRemoteCommandHandlerStatusCommandFailed;
    return [player dispatchRemoteCommand:2 positionMillis:0] ? MPRemoteCommandHandlerStatusSuccess : MPRemoteCommandHandlerStatusCommandFailed;
}

static MPRemoteCommandHandlerStatus performRemoteToggle(RhythHausAudioPlayer *player) {
    if (player == nil || player.player == nil) return MPRemoteCommandHandlerStatusNoSuchContent;
    if (!player.transportEnabled) return MPRemoteCommandHandlerStatusCommandFailed;
    return player.player.isPlaying ? performRemotePause(player) : performRemotePlay(player);
}

static MPRemoteCommandHandlerStatus performRemoteStop(RhythHausAudioPlayer *player) {
    if (player == nil || player.player == nil) return MPRemoteCommandHandlerStatusNoSuchContent;
    if (!player.transportEnabled) return MPRemoteCommandHandlerStatusCommandFailed;
    return [player dispatchRemoteCommand:3 positionMillis:0] ? MPRemoteCommandHandlerStatusSuccess : MPRemoteCommandHandlerStatusCommandFailed;
}

static MPRemoteCommandHandlerStatus performRemoteSeek(RhythHausAudioPlayer *player, jlong positionMillis) {
    if (player == nil || player.player == nil) return MPRemoteCommandHandlerStatusNoSuchContent;
    if (!player.transportEnabled) return MPRemoteCommandHandlerStatusCommandFailed;
    return [player dispatchRemoteCommand:4 positionMillis:positionMillis] ? MPRemoteCommandHandlerStatusSuccess : MPRemoteCommandHandlerStatusCommandFailed;
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

static void logAudioOutputAfterStartFailure() {
    std::vector<AudioDeviceID> available;
    AudioDeviceID defaultDevice = kAudioObjectUnknown;
    bool snapshotRead = readRouteSnapshot(available, defaultDevice);
    UInt32 alive = 0;
    UInt32 aliveSize = sizeof(alive);
    AudioObjectPropertyAddress aliveAddress = {
        kAudioDevicePropertyDeviceIsAlive, kAudioObjectPropertyScopeGlobal, kAudioObjectPropertyElementMain};
    OSStatus aliveStatus = defaultDevice == kAudioObjectUnknown ? kAudioHardwareBadDeviceError :
        AudioObjectGetPropertyData(defaultDevice, &aliveAddress, 0, nullptr, &aliveSize, &alive);
    Float64 sampleRate = 0.0;
    UInt32 rateSize = sizeof(sampleRate);
    AudioObjectPropertyAddress rateAddress = {
        kAudioDevicePropertyNominalSampleRate, kAudioObjectPropertyScopeGlobal, kAudioObjectPropertyElementMain};
    OSStatus rateStatus = defaultDevice == kAudioObjectUnknown ? kAudioHardwareBadDeviceError :
        AudioObjectGetPropertyData(defaultDevice, &rateAddress, 0, nullptr, &rateSize, &sampleRate);
    std::fprintf(stderr, "[rhythhaus-mac-start] output-after-rejection snapshotRead=%d defaultDevice=%u deviceCount=%zu defaultPresent=%d aliveStatus=%d alive=%u rateStatus=%d sampleRate=%.0f\n",
                 snapshotRead, defaultDevice, available.size(), containsDevice(available, defaultDevice),
                 (int)aliveStatus, alive, (int)rateStatus, sampleRate);
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

- (AVAudioPlayer *)player {
    @synchronized (self) {
        return self.currentVoice.audioPlayer;
    }
}

- (RhythHausAudioVoice *)createVoiceForPath:(NSString *)path generation:(jlong)generation gain:(float)gain {
    const bool diagnostics = audioStartDiagnosticsEnabled();
    const CFAbsoluteTime loadStarted = diagnostics ? CFAbsoluteTimeGetCurrent() : 0.0;
    if (diagnostics)
        std::fprintf(stderr, "[rhythhaus-mac-start] load-begin owner=%p generation=%lld mainThread=%d\n",
                     (__bridge void *)self, (long long)generation, [NSThread isMainThread]);
    NSError *error = nil;
    AVAudioPlayer *audioPlayer = [[AVAudioPlayer alloc] initWithContentsOfURL:[NSURL fileURLWithPath:path] error:&error];
    if (diagnostics)
        std::fprintf(stderr, "[rhythhaus-mac-start] load-end owner=%p player=%p elapsedMs=%.1f domain=%s code=%ld\n",
                     (__bridge void *)self, (__bridge void *)audioPlayer,
                     (CFAbsoluteTimeGetCurrent() - loadStarted) * 1000.0,
                     error == nil ? "(none)" : error.domain.UTF8String, (long)error.code);
    if (audioPlayer == nil || error != nil) return nil;
    audioPlayer.volume = gain;
    const CFAbsoluteTime prepareStarted = diagnostics ? CFAbsoluteTimeGetCurrent() : 0.0;
    if (diagnostics)
        std::fprintf(stderr, "[rhythhaus-mac-start] prepare-begin player=%p\n", (__bridge void *)audioPlayer);
    BOOL prepared = [audioPlayer prepareToPlay];
    if (diagnostics)
        std::fprintf(stderr, "[rhythhaus-mac-start] prepare-end player=%p accepted=%d elapsedMs=%.1f channels=%lu sampleRate=%.0f duration=%.3f\n",
                     (__bridge void *)audioPlayer, prepared, (CFAbsoluteTimeGetCurrent() - prepareStarted) * 1000.0,
                     (unsigned long)audioPlayer.numberOfChannels, audioPlayer.format.sampleRate, audioPlayer.duration);
    if (!prepared) return nil;
    RhythHausAudioVoice *voice = [[RhythHausAudioVoice alloc] init];
    voice.audioPlayer = audioPlayer;
    voice.generation = generation;
    voice.gain = gain;
    return voice;
}

- (void)installCallbackForVoice:(RhythHausAudioVoice *)voice runId:(jlong)runId {
    voice.callback.owner = nil;
    RhythHausVoiceCallbackContext *context = [[RhythHausVoiceCallbackContext alloc] init];
    context.owner = self;
    context.audioPlayer = voice.audioPlayer;
    context.generation = voice.generation;
    context.playbackRunId = runId;
    voice.callback = context;
    voice.audioPlayer.delegate = context;
    voice.playbackRunId = runId;
    voice.hasPlaybackRun = YES;
    voice.terminalPending = NO;
}

- (BOOL)startVoice:(RhythHausAudioVoice *)voice runId:(jlong)runId {
    self.lastStartFailure = nil;
    if (voice == nil || voice.audioPlayer == nil) {
        self.lastStartFailure = voice == nil ? @"No native macOS audio voice is loaded." :
            @"The loaded native macOS audio voice has no player.";
        return NO;
    }
    const bool diagnostics = audioStartDiagnosticsEnabled();
    if (diagnostics)
        std::fprintf(stderr, "[rhythhaus-mac-start] start-begin owner=%p voice=%p player=%p generation=%lld run=%lld gain=%.3f trackedDefault=%u deviceCount=%zu mainThread=%d\n",
                     (__bridge void *)self, (__bridge void *)voice, (__bridge void *)voice.audioPlayer,
                     (long long)voice.generation, (long long)runId, voice.gain,
                     _trackedDefaultDevice, _availableDevices.size(), [NSThread isMainThread]);
    [self installCallbackForVoice:voice runId:runId];
    const CFAbsoluteTime clockStarted = diagnostics ? CFAbsoluteTimeGetCurrent() : 0.0;
    if (diagnostics)
        std::fprintf(stderr, "[rhythhaus-mac-start] device-clock-begin player=%p\n", (__bridge void *)voice.audioPlayer);
    NSTimeInterval deviceTime = voice.audioPlayer.deviceCurrentTime;
    voice.expectedEndDeviceTime = deviceTime + MAX(0.0, voice.audioPlayer.duration - voice.audioPlayer.currentTime);
    if (diagnostics)
        std::fprintf(stderr, "[rhythhaus-mac-start] device-clock-end player=%p elapsedMs=%.1f deviceTime=%.3f expectedEnd=%.3f\n",
                     (__bridge void *)voice.audioPlayer, (CFAbsoluteTimeGetCurrent() - clockStarted) * 1000.0,
                     deviceTime, voice.expectedEndDeviceTime);
    const CFAbsoluteTime playStarted = diagnostics ? CFAbsoluteTimeGetCurrent() : 0.0;
    if (diagnostics)
        std::fprintf(stderr, "[rhythhaus-mac-start] play-begin player=%p\n", (__bridge void *)voice.audioPlayer);
    BOOL played = [voice.audioPlayer play];
    if (diagnostics)
        std::fprintf(stderr, "[rhythhaus-mac-start] play-end owner=%p voice=%p player=%p accepted=%d playing=%d elapsedMs=%.1f\n",
                     (__bridge void *)self, (__bridge void *)voice, (__bridge void *)voice.audioPlayer,
                     played, voice.audioPlayer.isPlaying, (CFAbsoluteTimeGetCurrent() - playStarted) * 1000.0);
    if (!played) {
        self.lastStartFailure = @"AVAudioPlayer.play returned false for a prepared native macOS audio voice.";
        if (diagnostics) logAudioOutputAfterStartFailure();
        return NO;
    }
    voice.finished = NO;
    voice.failed = NO;
    voice.expectedPlaying = YES;
    return YES;
}

- (void)recordTerminalForPlayer:(AVAudioPlayer *)player context:(RhythHausVoiceCallbackContext *)context failed:(BOOL)failed {
    @synchronized (self) {
        if (player == nil) return;
        RhythHausAudioVoice *voice = nil;
        if (self.currentVoice.audioPlayer == player) voice = self.currentVoice;
        else if (self.outgoingVoice.audioPlayer == player) voice = self.outgoingVoice;
        if (voice == nil || voice.callback != context || voice.finished) return;
        voice.finished = YES;
        voice.failed = failed;
        voice.expectedPlaying = NO;
        voice.terminalPending = YES;
        voice.terminalGeneration = context.generation;
        voice.terminalRunId = context.playbackRunId;
    }
}

- (void)installRemoteBridge:(JNIEnv *)env bridge:(jobject)bridge {
    @synchronized (self) {
        env->GetJavaVM(&_callbackVM);
        _remoteBridge = env->NewWeakGlobalRef(bridge);
    }
}

- (void)removeRemoteBridge {
    @synchronized (self) {
        if (_remoteBridge == nullptr) return;
        JNIEnv *env = nullptr;
        bool attached = false;
        if (_callbackVM->GetEnv((void **)&env, JNI_VERSION_1_6) != JNI_OK &&
            _callbackVM->AttachCurrentThread((void **)&env, nullptr) == JNI_OK)
            attached = true;
        if (env != nullptr)
            env->DeleteWeakGlobalRef(_remoteBridge);
        _remoteBridge = nullptr;
        if (attached) _callbackVM->DetachCurrentThread();
    }
}

- (BOOL)dispatchRemoteCommand:(jint)command positionMillis:(jlong)positionMillis {
    JavaVM *vm = _callbackVM;
    if (vm == nullptr) return NO;
    JNIEnv *env = nullptr;
    bool attached = false;
    if (vm->GetEnv((void **)&env, JNI_VERSION_1_6) != JNI_OK) {
        if (vm->AttachCurrentThread((void **)&env, nullptr) != JNI_OK) return NO;
        attached = true;
    }
    jobject bridge = nullptr;
    @synchronized (self) {
        if (_remoteBridge != nullptr) bridge = env->NewLocalRef(_remoteBridge);
    }
    BOOL accepted = NO;
    if (bridge != nullptr) {
        jclass type = env->GetObjectClass(bridge);
        jmethodID method = env->GetMethodID(type, "onRemoteTransport", "(IJ)Z");
        if (method != nullptr)
            accepted = env->CallBooleanMethod(bridge, method, command, positionMillis) == JNI_TRUE;
        if (env->ExceptionCheck()) {
            env->ExceptionDescribe();
            env->ExceptionClear();
            accepted = NO;
        }
        env->DeleteLocalRef(type);
        env->DeleteLocalRef(bridge);
    }
    if (attached) vm->DetachCurrentThread();
    return accepted;
}

- (instancetype)init {
    self = [super init];
    if (self != nil) {
        _transportEnabled = YES;
        _routeDisconnectPending.store(false);
        _expectedActive.store(false);
        _trackedDefaultDevice = kAudioObjectUnknown;
        _routeSnapshotInjected = NO;
        _playbackGain = 1.0f;
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

- (BOOL)loadPath:(NSString *)path generation:(jlong)generation {
    @synchronized (self) {
        RhythHausAudioVoice *nextVoice = [self createVoiceForPath:path generation:generation gain:_playbackGain];
        if (nextVoice == nil) return NO;
        disposeVoice(self.currentVoice);
        disposeVoice(self.outgoingVoice);
        disposeVoice(self.preparedVoice);
        self.currentVoice = nextVoice;
        self.outgoingVoice = nil;
        self.preparedVoice = nil;
        _expectedActive.store(false);
        _routeDisconnectPending.store(false);
        return YES;
    }
}

- (BOOL)prepareIncomingPath:(NSString *)path generation:(jlong)generation {
    @synchronized (self) {
        if (self.currentVoice == nil || self.outgoingVoice != nil) return NO;
        [self cancelPreparedIncoming];
        self.preparedVoice = [self createVoiceForPath:path generation:generation gain:0.0f];
        return self.preparedVoice != nil;
    }
}

- (jlong)incomingDurationMillis {
    @synchronized (self) {
        return (jlong)(self.preparedVoice.audioPlayer.duration * 1000.0);
    }
}

- (BOOL)startIncomingWithRunId:(jlong)playbackRunId {
    @synchronized (self) {
        if (self.currentVoice == nil || self.outgoingVoice != nil || self.preparedVoice == nil) {
            self.lastStartFailure = @"Crossfade requires a current voice and a prepared incoming voice without a retiring voice.";
            return NO;
        }
        if (![self startVoice:self.preparedVoice runId:playbackRunId]) {
            [self cancelPreparedIncoming];
            return NO;
        }
        self.outgoingVoice = self.currentVoice;
        self.currentVoice = self.preparedVoice;
        self.preparedVoice = nil;
        _playbackGain = self.currentVoice.gain;
        _expectedActive.store(true);
        return YES;
    }
}

- (void)setCrossfadeOutgoingGain:(jfloat)outgoingGain incomingGain:(jfloat)incomingGain {
    @synchronized (self) {
        RhythHausAudioVoice *outgoing = self.outgoingVoice ?: self.currentVoice;
        RhythHausAudioVoice *incoming = self.outgoingVoice == nil ? self.preparedVoice : self.currentVoice;
        outgoing.gain = outgoingGain;
        outgoing.audioPlayer.volume = outgoingGain;
        incoming.gain = incomingGain;
        incoming.audioPlayer.volume = incomingGain;
        _playbackGain = self.currentVoice == nil ? _playbackGain : self.currentVoice.gain;
    }
}

- (void)finishCrossfade {
    @synchronized (self) {
        disposeVoice(self.outgoingVoice);
        self.outgoingVoice = nil;
    }
}

- (void)cancelPreparedIncoming {
    @synchronized (self) {
        disposeVoice(self.preparedVoice);
        self.preparedVoice = nil;
        if (self.outgoingVoice != nil) {
            disposeVoice(self.currentVoice);
            self.currentVoice = self.outgoingVoice;
            self.outgoingVoice = nil;
            _playbackGain = self.currentVoice.gain;
            _expectedActive.store(self.currentVoice.audioPlayer.isPlaying);
        }
    }
}

- (BOOL)outgoingHasCompleted {
    @synchronized (self) {
        refreshVoiceCompletion(self.outgoingVoice);
        return self.outgoingVoice != nil && self.outgoingVoice.finished && !self.outgoingVoice.failed;
    }
}

- (BOOL)consumePlaybackEvent:(jlong *)event {
    @synchronized (self) {
        refreshVoiceCompletion(self.currentVoice);
        refreshVoiceCompletion(self.outgoingVoice);
        RhythHausAudioVoice *voice = self.currentVoice.terminalPending ? self.currentVoice : self.outgoingVoice;
        if (voice == nil || !voice.terminalPending) return NO;
        event[0] = voice.terminalGeneration;
        event[1] = voice.terminalRunId;
        event[2] = voice.failed ? 2 : 1;
        voice.terminalPending = NO;
        return YES;
    }
}

- (BOOL)playWithRunId:(jlong)playbackRunId {
    @synchronized (self) {
        if (![self startVoice:self.currentVoice runId:playbackRunId]) return NO;
        refreshVoiceCompletion(self.outgoingVoice);
        if (self.outgoingVoice != nil && !self.outgoingVoice.finished) {
            // Resuming overlap does not rebind the retiring voice to the new
            // logical current generation or playback run.
            self.outgoingVoice.expectedEndDeviceTime = self.outgoingVoice.audioPlayer.deviceCurrentTime + MAX(0.0, self.outgoingVoice.audioPlayer.duration - self.outgoingVoice.audioPlayer.currentTime);
            self.outgoingVoice.expectedPlaying = [self.outgoingVoice.audioPlayer play];
        }
        _expectedActive.store(self.player.isPlaying || self.outgoingVoice.audioPlayer.isPlaying);
        _routeDisconnectPending.store(false);
        return YES;
    }
}

- (void)pause {
    @synchronized (self) {
        _expectedActive.store(false);
        _routeDisconnectPending.store(false);
        refreshVoiceCompletion(self.currentVoice);
        refreshVoiceCompletion(self.outgoingVoice);
        self.currentVoice.expectedPlaying = NO;
        self.outgoingVoice.expectedPlaying = NO;
        [self.currentVoice.audioPlayer pause];
        [self.outgoingVoice.audioPlayer pause];
    }
}

- (void)stop {
    @synchronized (self) {
        _expectedActive.store(false);
        _routeDisconnectPending.store(false);
        disposeVoice(self.currentVoice);
        disposeVoice(self.outgoingVoice);
        disposeVoice(self.preparedVoice);
        self.currentVoice = nil;
        self.outgoingVoice = nil;
        self.preparedVoice = nil;
        _playbackGain = 1.0f;
    }
}

- (void)setPlaybackGain:(jfloat)gain {
    @synchronized (self) {
        _playbackGain = gain;
        self.currentVoice.gain = gain;
        self.player.volume = gain;
    }
}

- (jfloat)playbackGain {
    @synchronized (self) {
        return _playbackGain;
    }
}

- (jfloat)playerVolumeForTest {
    @synchronized (self) {
        return self.player == nil ? _playbackGain : self.player.volume;
    }
}

- (void)seekToMillis:(jlong)positionMillis {
    @synchronized (self) {
        [self finishCrossfade];
        [self cancelPreparedIncoming];
        RhythHausAudioVoice *voice = self.currentVoice;
        if (voice == nil) return;
        if (voice.hasPlaybackRun)
            [self installCallbackForVoice:voice runId:voice.playbackRunId];
        voice.finished = NO;
        voice.failed = NO;
        self.player.currentTime = MAX(0.0, ((double)positionMillis) / 1000.0);
        voice.expectedEndDeviceTime = self.player.deviceCurrentTime + MAX(0.0, self.player.duration - self.player.currentTime);
    }
}

- (jlong)currentPositionMillis {
    @synchronized (self) {
        return voicePositionMillis(self.currentVoice);
    }
}

- (jlong)durationMillis {
    @synchronized (self) {
        if (self.player == nil || self.player.duration <= 0.0) return 0;
        return (jlong)(self.player.duration * 1000.0);
    }
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
        [player removeRemoteBridge];
        [player removeRouteListeners];
        [player pause];
        [player removeRemoteCommands];
        [player clearNowPlayingInfo];
        [player stop];
        CFRelease((__bridge CFTypeRef)player);
    }
}

extern "C" JNIEXPORT jlong JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeCreate(JNIEnv *env, jobject bridge) {
    RhythHausAudioPlayer *player = [[RhythHausAudioPlayer alloc] init];
    [player installRemoteBridge:env bridge:bridge];
    return (jlong)(__bridge_retained void *)player;
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeLoad(JNIEnv *env, jobject, jlong handle, jstring path, jlong generation) {
    @autoreleasepool {
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
        return [player loadPath:nsPath generation:generation] ? JNI_TRUE : JNI_FALSE;
    }
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativePlay(JNIEnv *, jobject, jlong handle, jlong playbackRunId) {
    RhythHausAudioPlayer *player = playerFromHandle(handle);
    return player != nil && [player playWithRunId:playbackRunId] ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeLastStartFailure(JNIEnv *env, jobject, jlong handle) {
    @autoreleasepool {
        RhythHausAudioPlayer *player = playerFromHandle(handle);
        @synchronized (player) {
            NSString *failure = player.lastStartFailure;
            return failure == nil ? nullptr : env->NewStringUTF(failure.UTF8String);
        }
    }
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativePrepareIncoming(JNIEnv *env, jobject, jlong handle, jstring path, jlong generation) {
    @autoreleasepool {
        RhythHausAudioPlayer *player = playerFromHandle(handle);
        if (player == nil || path == NULL) return JNI_FALSE;
        const char *utfPath = env->GetStringUTFChars(path, NULL);
        if (utfPath == NULL) return JNI_FALSE;
        NSString *nsPath = [NSString stringWithUTF8String:utfPath];
        env->ReleaseStringUTFChars(path, utfPath);
        return [player prepareIncomingPath:nsPath generation:generation] ? JNI_TRUE : JNI_FALSE;
    }
}

extern "C" JNIEXPORT jlong JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeIncomingDurationMillis(JNIEnv *, jobject, jlong handle) {
    return [playerFromHandle(handle) incomingDurationMillis];
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeStartIncoming(JNIEnv *, jobject, jlong handle, jlong playbackRunId) {
    return [playerFromHandle(handle) startIncomingWithRunId:playbackRunId] ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeSetCrossfadeGains(JNIEnv *, jobject, jlong handle, jfloat outgoingGain, jfloat incomingGain) {
    [playerFromHandle(handle) setCrossfadeOutgoingGain:outgoingGain incomingGain:incomingGain];
}

extern "C" JNIEXPORT void JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeFinishCrossfade(JNIEnv *, jobject, jlong handle) {
    [playerFromHandle(handle) finishCrossfade];
}

extern "C" JNIEXPORT void JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeCancelPreparedIncoming(JNIEnv *, jobject, jlong handle) {
    [playerFromHandle(handle) cancelPreparedIncoming];
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeOutgoingHasCompleted(JNIEnv *, jobject, jlong handle) {
    return [playerFromHandle(handle) outgoingHasCompleted] ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeConsumePlaybackEvent(JNIEnv *env, jobject, jlong handle, jlongArray result) {
    if (result == NULL || env->GetArrayLength(result) < 3) return JNI_FALSE;
    jlong event[3];
    if (![playerFromHandle(handle) consumePlaybackEvent:event]) return JNI_FALSE;
    env->SetLongArrayRegion(result, 0, 3, event);
    return JNI_TRUE;
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

extern "C" JNIEXPORT jfloat JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativePlaybackGain(JNIEnv *, jobject, jlong handle) {
    RhythHausAudioPlayer *player = playerFromHandle(handle);
    return player == nil ? 1.0f : [player playbackGain];
}

extern "C" JNIEXPORT void JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeSetPlaybackGain(JNIEnv *, jobject, jlong handle, jfloat gain) {
    RhythHausAudioPlayer *player = playerFromHandle(handle);
    if (player != nil) {
        [player setPlaybackGain:gain];
    }
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

// Test-owned JNI fixtures inspect the real voices without adding transport or
// lifecycle methods to the production engine solely for tests.
extern "C" JNIEXPORT void JNICALL Java_com_eterocell_rhythhaus_JvmPlaybackEngineTest_nativeInvalidateCurrentPlayerForTest(JNIEnv *, jclass, jlong handle) {
    RhythHausAudioPlayer *player = playerFromHandle(handle);
    @synchronized (player) {
        [player.currentVoice.audioPlayer stop];
        player.currentVoice.audioPlayer = nil;
    }
}

extern "C" JNIEXPORT void JNICALL Java_com_eterocell_rhythhaus_JvmPlaybackEngineTest_nativeInvalidatePreparedPlayerForTest(JNIEnv *, jclass, jlong handle) {
    RhythHausAudioPlayer *player = playerFromHandle(handle);
    @synchronized (player) {
        [player.preparedVoice.audioPlayer stop];
        player.preparedVoice.audioPlayer = nil;
    }
}

extern "C" JNIEXPORT jlongArray JNICALL Java_com_eterocell_rhythhaus_JvmPlaybackEngineTest_nativeCrossfadeSnapshotForTest(JNIEnv *env, jclass, jlong handle) {
    RhythHausAudioPlayer *player = playerFromHandle(handle);
    jlong snapshot[6];
    @synchronized (player) {
        snapshot[0] = voicePositionMillis(player.currentVoice);
        snapshot[1] = player.outgoingVoice == nil ? -1 : voicePositionMillis(player.outgoingVoice);
        snapshot[2] = player.preparedVoice == nil ? -1 : voicePositionMillis(player.preparedVoice);
        snapshot[3] = player.currentVoice.audioPlayer.isPlaying ? 1 : 0;
        snapshot[4] = player.outgoingVoice.audioPlayer.isPlaying ? 1 : 0;
        snapshot[5] = (player.currentVoice != nil) + (player.outgoingVoice != nil) + (player.preparedVoice != nil);
    }
    jlongArray result = env->NewLongArray(6);
    if (result != NULL) env->SetLongArrayRegion(result, 0, 6, snapshot);
    return result;
}

extern "C" JNIEXPORT jfloatArray JNICALL Java_com_eterocell_rhythhaus_JvmPlaybackEngineTest_nativeCrossfadeVolumesForTest(JNIEnv *env, jclass, jlong handle) {
    RhythHausAudioPlayer *player = playerFromHandle(handle);
    jfloat gains[2];
    @synchronized (player) {
        gains[0] = [player playerVolumeForTest];
        RhythHausAudioVoice *secondary = player.outgoingVoice ?: player.preparedVoice;
        gains[1] = secondary.audioPlayer.volume;
    }
    jfloatArray result = env->NewFloatArray(2);
    if (result != NULL) env->SetFloatArrayRegion(result, 0, 2, gains);
    return result;
}

extern "C" JNIEXPORT jlong JNICALL Java_com_eterocell_rhythhaus_JvmPlaybackEngineTest_nativeLiveAudioVoiceCountForTest(JNIEnv *, jclass) {
    return (jlong)liveAudioVoiceCount.load();
}

extern "C" JNIEXPORT jlong JNICALL Java_com_eterocell_rhythhaus_JvmPlaybackEngineTest_nativeCaptureTerminalCallbackForTest(JNIEnv *, jclass, jlong handle) {
    RhythHausAudioPlayer *player = playerFromHandle(handle);
    @synchronized (player) {
        RhythHausVoiceCallbackContext *context = player.currentVoice.callback;
        return context == nil ? 0 : (jlong)(__bridge_retained void *)context;
    }
}

extern "C" JNIEXPORT void JNICALL Java_com_eterocell_rhythhaus_JvmPlaybackEngineTest_nativeDeliverCapturedTerminalCallbackForTest(JNIEnv *, jclass, jlong callback, jboolean completed) {
    if (callback == 0) return;
    RhythHausVoiceCallbackContext *context = (__bridge RhythHausVoiceCallbackContext *)(void *)callback;
    if (completed == JNI_TRUE)
        [context audioPlayerDidFinishPlaying:context.audioPlayer successfully:YES];
    else
        [context audioPlayerDecodeErrorDidOccur:context.audioPlayer error:nil];
    CFRelease((__bridge CFTypeRef)context);
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

extern "C" JNIEXPORT jfloat JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativePlayerVolumeForTest(JNIEnv *, jobject, jlong handle) {
    RhythHausAudioPlayer *player = playerFromHandle(handle);
    return player == nil ? 0.0f : [player playerVolumeForTest];
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
