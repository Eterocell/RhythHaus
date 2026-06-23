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

// --- Property map: exposes ALL TagLib properties as key-value pairs ---

typedef struct RhTagLibProperties {
    int status;          // 0 found, 1 unsupported, 2 failed
    char* error_message;
    int property_count;
    char** keys;         // array of key strings, owned by caller
    char** values;       // array of value strings, owned by caller
} RhTagLibProperties;

RhTagLibProperties rh_taglib_read_properties(const char* path);
void rh_taglib_free_properties(RhTagLibProperties properties);

// --- Write API: updates metadata and persists to disk ---

typedef struct RhTagLibWriteMeta {
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
    // Property map: parallel arrays, may be NULL/empty
    int property_count;
    char** property_keys;
    char** property_values;
} RhTagLibWriteMeta;

// Write metadata to the file at path. Returns status:
//   0  success — metadata written and saved to disk
//   1  unsupported — file format not writable by TagLib
//   2  failed — error during write
// On error, *error_out receives a caller-owned message; pass NULL to ignore.
int rh_taglib_write_path(const char* path, const RhTagLibWriteMeta* meta, char** error_out);

// Frees heap-owned strings inside meta. Safe to call with NULL or zeroed fields.
void rh_taglib_free_write_meta(RhTagLibWriteMeta* meta);

#ifdef __cplusplus
}
#endif
