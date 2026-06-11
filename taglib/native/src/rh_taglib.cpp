#include "rh_taglib.h"

#include <cstdlib>
#include <cstring>

#ifdef RH_TAGLIB_HAS_TAGLIB
#include <audioproperties.h>
#include <fileref.h>
#include <tag.h>
#include <tstring.h>
#endif

namespace {

constexpr int RH_TAGLIB_STATUS_FOUND = 0;
constexpr int RH_TAGLIB_STATUS_UNSUPPORTED = 1;
constexpr int RH_TAGLIB_STATUS_FAILED = 2;

char* rh_taglib_duplicate(const char* value) {
    if (value == nullptr) {
        return nullptr;
    }

    const auto size = std::strlen(value) + 1;
    auto* copy = static_cast<char*>(std::malloc(size));
    if (copy == nullptr) {
        return nullptr;
    }

    std::memcpy(copy, value, size);
    return copy;
}

RhTagLibMetadata rh_taglib_empty_metadata() {
    RhTagLibMetadata metadata{};
    return metadata;
}

RhTagLibResult rh_taglib_result(int status, const char* message) {
    RhTagLibResult result{};
    result.status = status;
    result.error_message = rh_taglib_duplicate(message);
    result.metadata = rh_taglib_empty_metadata();
    return result;
}

#ifdef RH_TAGLIB_HAS_TAGLIB
char* rh_taglib_duplicate_tag_string(const TagLib::String& value) {
    if (value.isEmpty()) {
        return nullptr;
    }

    return rh_taglib_duplicate(value.to8Bit(true).c_str());
}
#endif

} // namespace

extern "C" RhTagLibResult rh_taglib_read_path(const char* path) {
    if (path == nullptr || path[0] == '\0') {
        return rh_taglib_result(RH_TAGLIB_STATUS_FAILED, "Path is required");
    }

#ifndef RH_TAGLIB_HAS_TAGLIB
    return rh_taglib_result(
        RH_TAGLIB_STATUS_UNSUPPORTED,
        "Native TagLib library was not found at build time; rebuild rhythhaus_taglib with TagLib available"
    );
#else
    try {
        TagLib::FileRef file(path);
        if (file.isNull()) {
            return rh_taglib_result(RH_TAGLIB_STATUS_UNSUPPORTED, "TagLib could not open this file path");
        }

        RhTagLibResult result{};
        result.status = RH_TAGLIB_STATUS_FOUND;
        result.error_message = nullptr;
        result.metadata = rh_taglib_empty_metadata();

        if (auto* tag = file.tag()) {
            result.metadata.title = rh_taglib_duplicate_tag_string(tag->title());
            result.metadata.artist = rh_taglib_duplicate_tag_string(tag->artist());
            result.metadata.album = rh_taglib_duplicate_tag_string(tag->album());
            result.metadata.genre = rh_taglib_duplicate_tag_string(tag->genre());
            result.metadata.year = static_cast<int>(tag->year());
            result.metadata.track = static_cast<int>(tag->track());
        }

        if (auto* properties = file.audioProperties()) {
            result.metadata.duration_seconds = properties->lengthInSeconds();
            result.metadata.bitrate = properties->bitrate();
            result.metadata.sample_rate = properties->sampleRate();
            result.metadata.channels = properties->channels();
        }

        return result;
    } catch (...) {
        return rh_taglib_result(RH_TAGLIB_STATUS_FAILED, "TagLib failed while reading metadata");
    }
#endif
}

extern "C" void rh_taglib_free_result(RhTagLibResult result) {
    std::free(result.error_message);
    std::free(result.metadata.title);
    std::free(result.metadata.artist);
    std::free(result.metadata.album);
    std::free(result.metadata.album_artist);
    std::free(result.metadata.genre);
}
