#pragma once

#ifdef __cplusplus
extern "C" {
#endif

typedef struct RhTagLibMetadata {
    char* title;
    char* artist;
    char* album;
    char* album_artist;
    char* genre;
    char* comment;
    int year;
    int track;
    int track_total;
    int disc_number;
    int disc_total;
    int duration_seconds;
    int bitrate;
    int sample_rate;
    int channels;
} RhTagLibMetadata;

typedef struct RhTagLibResult {
    int status; // 0 found, 1 unsupported, 2 failed
    char* error_message;
    RhTagLibMetadata metadata;
} RhTagLibResult;

// Reads metadata from a filesystem path.
// Returned char* fields are owned by the caller and must be released by passing
// the complete result value to rh_taglib_free_result.
RhTagLibResult rh_taglib_read_path(const char* path);

// Frees all heap-owned string fields inside result. It is safe to pass a result
// whose string fields are null.
void rh_taglib_free_result(RhTagLibResult result);

#ifdef __cplusplus
}
#endif
