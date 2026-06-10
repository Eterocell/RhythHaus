#import <AVFoundation/AVFoundation.h>
#import <Foundation/Foundation.h>

#include <jni.h>

@interface RhythHausAudioPlayer : NSObject
@property(nonatomic, strong) AVAudioPlayer *player;
@end

@implementation RhythHausAudioPlayer

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
    return self.player != nil && [self.player play];
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

extern "C" JNIEXPORT void JNICALL Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_nativeRelease(JNIEnv *, jobject, jlong handle) {
    RhythHausAudioPlayer *player = playerFromHandle(handle);
    if (player != nil) {
        [player stop];
        CFRelease((__bridge CFTypeRef)player);
    }
}
