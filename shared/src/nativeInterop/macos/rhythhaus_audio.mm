#import <AVFoundation/AVFoundation.h>
#import <AppKit/AppKit.h>
#import <Foundation/Foundation.h>
#import <MediaPlayer/MediaPlayer.h>

#include <jni.h>

@interface RhythHausAudioPlayer : NSObject
@property(nonatomic, strong) AVAudioPlayer *player;
@property(nonatomic, assign) BOOL remoteCommandsRegistered;
@property(nonatomic, strong) MPMediaItemArtwork *artwork;
@property(nonatomic, assign) BOOL transportEnabled;
@end

@implementation RhythHausAudioPlayer

- (instancetype)init {
    self = [super init];
    if (self != nil) {
        _transportEnabled = YES;
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
    [self.player play];
    return YES;
}

- (void)pause {
    [self.player pause];
}

- (void)stop {
    [self.player stop];
    self.player.currentTime = 0.0;
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
    [commandCenter.playCommand addTargetWithHandler:^MPRemoteCommandHandlerStatus(MPRemoteCommandEvent *) {
        RhythHausAudioPlayer *strongSelf = weakSelf;
        if (strongSelf == nil || strongSelf.player == nil) return MPRemoteCommandHandlerStatusNoSuchContent;
        if (!strongSelf.transportEnabled) return MPRemoteCommandHandlerStatusCommandFailed;
        return [strongSelf play] ? MPRemoteCommandHandlerStatusSuccess : MPRemoteCommandHandlerStatusCommandFailed;
    }];
    [commandCenter.pauseCommand addTargetWithHandler:^MPRemoteCommandHandlerStatus(MPRemoteCommandEvent *) {
        RhythHausAudioPlayer *strongSelf = weakSelf;
        if (strongSelf == nil || strongSelf.player == nil) return MPRemoteCommandHandlerStatusNoSuchContent;
        if (!strongSelf.transportEnabled) return MPRemoteCommandHandlerStatusCommandFailed;
        [strongSelf pause];
        return MPRemoteCommandHandlerStatusSuccess;
    }];
    [commandCenter.togglePlayPauseCommand addTargetWithHandler:^MPRemoteCommandHandlerStatus(MPRemoteCommandEvent *) {
        RhythHausAudioPlayer *strongSelf = weakSelf;
        if (strongSelf == nil || strongSelf.player == nil) return MPRemoteCommandHandlerStatusNoSuchContent;
        if (!strongSelf.transportEnabled) return MPRemoteCommandHandlerStatusCommandFailed;
        if (strongSelf.player.isPlaying) {
            [strongSelf pause];
            return MPRemoteCommandHandlerStatusSuccess;
        }
        return [strongSelf play] ? MPRemoteCommandHandlerStatusSuccess : MPRemoteCommandHandlerStatusCommandFailed;
    }];
    [commandCenter.stopCommand addTargetWithHandler:^MPRemoteCommandHandlerStatus(MPRemoteCommandEvent *) {
        RhythHausAudioPlayer *strongSelf = weakSelf;
        if (strongSelf == nil || strongSelf.player == nil) return MPRemoteCommandHandlerStatusNoSuchContent;
        if (!strongSelf.transportEnabled) return MPRemoteCommandHandlerStatusCommandFailed;
        [strongSelf stop];
        return MPRemoteCommandHandlerStatusSuccess;
    }];
    [commandCenter.changePlaybackPositionCommand addTargetWithHandler:^MPRemoteCommandHandlerStatus(MPRemoteCommandEvent *event) {
        RhythHausAudioPlayer *strongSelf = weakSelf;
        if (strongSelf == nil || strongSelf.player == nil || ![event isKindOfClass:[MPChangePlaybackPositionCommandEvent class]]) return MPRemoteCommandHandlerStatusNoSuchContent;
        if (!strongSelf.transportEnabled) return MPRemoteCommandHandlerStatusCommandFailed;
        MPChangePlaybackPositionCommandEvent *positionEvent = (MPChangePlaybackPositionCommandEvent *)event;
        [strongSelf seekToMillis:(jlong)(positionEvent.positionTime * 1000.0)];
        return MPRemoteCommandHandlerStatusSuccess;
    }];
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

@end

static RhythHausAudioPlayer *playerFromHandle(jlong handle) {
    return (__bridge RhythHausAudioPlayer *)(void *)handle;
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
    RhythHausAudioPlayer *player = playerFromHandle(handle);
    if (player == nil || player.player == nil || !player.transportEnabled) return JNI_FALSE;
    return [player play] ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeInvokeRemoteSeekForTest(JNIEnv *, jobject, jlong handle, jlong positionMillis) {
    RhythHausAudioPlayer *player = playerFromHandle(handle);
    if (player == nil || player.player == nil || !player.transportEnabled) return JNI_FALSE;
    [player seekToMillis:positionMillis];
    return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeIsPlayingForTest(JNIEnv *, jobject, jlong handle) {
    RhythHausAudioPlayer *player = playerFromHandle(handle);
    return player != nil && player.player.isPlaying ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeRelease(JNIEnv *, jobject, jlong handle) {
    RhythHausAudioPlayer *player = playerFromHandle(handle);
    if (player != nil) {
        [player clearNowPlayingInfo];
        [player stop];
        CFRelease((__bridge CFTypeRef)player);
    }
}
