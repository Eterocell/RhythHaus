#include "rh_taglib.h"

#include <cstdlib>
#include <cstring>

#ifdef RH_TAGLIB_HAS_TAGLIB
#include <audioproperties.h>
#include <fileref.h>
#include <tag.h>
#include <tstring.h>

#include <ape/apefile.h>
#include <ape/apetag.h>
#include <flac/flacfile.h>
#include <mpeg/id3v2/id3v2tag.h>
#include <mpeg/mpegfile.h>
#include <mp4/mp4file.h>
#include <mp4/mp4tag.h>
#include <mp4/mp4item.h>
#include <ogg/flac/oggflacfile.h>
#include <ogg/opus/opusfile.h>
#include <ogg/speex/speexfile.h>
#include <ogg/vorbis/vorbisfile.h>
#include <ogg/xiphcomment.h>
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

void rh_extract_album_artist(RhTagLibMetadata& metadata, TagLib::FileRef& fileRef) {
    // Try ID3v2 (MPEG files)
    if (auto* mpegFile = dynamic_cast<TagLib::MPEG::File*>(fileRef.file())) {
        if (auto* id3v2Tag = mpegFile->ID3v2Tag()) {
            const auto& frameList = id3v2Tag->frameList("TPE2");
            if (!frameList.isEmpty()) {
                metadata.album_artist = rh_taglib_duplicate(frameList.front()->toString().to8Bit(true).c_str());
            }
        }
        return;
    }
    // Try APE (MPC, WavPack, APE files)
    if (auto* apeFile = dynamic_cast<TagLib::APE::File*>(fileRef.file())) {
        if (auto* apeTag = apeFile->APETag()) {
            const auto& items = apeTag->itemListMap();
            for (const auto& [key, item] : items) {
                if (key == "Album Artist" || key == "ALBUM ARTIST" || key == "ALBUMARTIST") {
                    metadata.album_artist = rh_taglib_duplicate(item.toString().to8Bit(true).c_str());
                    break;
                }
            }
        }
        return;
    }
    // Try XiphComment (FLAC, Ogg FLAC, Vorbis, Opus, Speex)
    auto* xiphComment = dynamic_cast<TagLib::Ogg::XiphComment*>(fileRef.file()->tag());
    if (xiphComment == nullptr) {
        if (auto* flacFile = dynamic_cast<TagLib::FLAC::File*>(fileRef.file())) {
            xiphComment = dynamic_cast<TagLib::Ogg::XiphComment*>(flacFile->tag());
        } else if (auto* oggFlacFile = dynamic_cast<TagLib::Ogg::FLAC::File*>(fileRef.file())) {
            xiphComment = dynamic_cast<TagLib::Ogg::XiphComment*>(oggFlacFile->tag());
        } else if (auto* vorbisFile = dynamic_cast<TagLib::Vorbis::File*>(fileRef.file())) {
            xiphComment = dynamic_cast<TagLib::Ogg::XiphComment*>(vorbisFile->tag());
        } else if (auto* opusFile = dynamic_cast<TagLib::Ogg::Opus::File*>(fileRef.file())) {
            xiphComment = dynamic_cast<TagLib::Ogg::XiphComment*>(opusFile->tag());
        } else if (auto* speexFile = dynamic_cast<TagLib::Ogg::Speex::File*>(fileRef.file())) {
            xiphComment = dynamic_cast<TagLib::Ogg::XiphComment*>(speexFile->tag());
        }
    }
    if (xiphComment != nullptr) {
        const auto& fieldList = xiphComment->fieldListMap()["ALBUMARTIST"];
        if (!fieldList.isEmpty()) {
            metadata.album_artist = rh_taglib_duplicate(fieldList.front().to8Bit(true).c_str());
        }
        return;
    }
    // Try MP4
    if (auto* mp4File = dynamic_cast<TagLib::MP4::File*>(fileRef.file())) {
        if (auto* mp4Tag = dynamic_cast<TagLib::MP4::Tag*>(mp4File->tag())) {
            if (mp4Tag->contains("aART")) {
                metadata.album_artist = rh_taglib_duplicate(
                    mp4Tag->item("aART").toStringList().toString(", ").to8Bit(true).c_str()
                );
            }
        }
    }
}

void rh_extract_disc_track_totals(RhTagLibMetadata& metadata, TagLib::FileRef& fileRef) {
    // Try ID3v2 (MPEG files)
    if (auto* mpegFile = dynamic_cast<TagLib::MPEG::File*>(fileRef.file())) {
        if (auto* id3v2Tag = mpegFile->ID3v2Tag()) {
            const auto& trckFrame = id3v2Tag->frameList("TRCK");
            if (!trckFrame.isEmpty()) {
                metadata.track = trckFrame.front()->toString().split('/').front().toInt();
                const auto parts = trckFrame.front()->toString().split('/');
                if (parts.size() > 1) {
                    metadata.track_total = parts.back().toInt();
                }
            }
            const auto& tposFrame = id3v2Tag->frameList("TPOS");
            if (!tposFrame.isEmpty()) {
                const auto parts = tposFrame.front()->toString().split('/');
                metadata.disc_number = parts.front().toInt();
                if (parts.size() > 1) {
                    metadata.disc_total = parts.back().toInt();
                }
            }
        }
        return;
    }
    // Try APE
    if (auto* apeFile = dynamic_cast<TagLib::APE::File*>(fileRef.file())) {
        if (auto* apeTag = apeFile->APETag()) {
            for (const auto& [key, item] : apeTag->itemListMap()) {
                if (key == "TRACK" && metadata.track_total == 0) {
                    const auto parts = item.toString().split('/');
                    if (parts.size() > 1) {
                        metadata.track_total = parts.back().toInt();
                    }
                }
                if (key == "DISC" && metadata.disc_number == 0) {
                    const auto parts = item.toString().split('/');
                    metadata.disc_number = parts.front().toInt();
                    if (parts.size() > 1) {
                        metadata.disc_total = parts.back().toInt();
                    }
                }
            }
        }
        return;
    }
    // Try XiphComment
    auto* xiphComment = dynamic_cast<TagLib::Ogg::XiphComment*>(fileRef.file()->tag());
    if (xiphComment == nullptr) {
        if (auto* flacFile = dynamic_cast<TagLib::FLAC::File*>(fileRef.file())) {
            xiphComment = dynamic_cast<TagLib::Ogg::XiphComment*>(flacFile->tag());
        } else if (auto* oggFlacFile = dynamic_cast<TagLib::Ogg::FLAC::File*>(fileRef.file())) {
            xiphComment = dynamic_cast<TagLib::Ogg::XiphComment*>(oggFlacFile->tag());
        }
    }
    if (xiphComment != nullptr) {
        const auto& trackField = xiphComment->fieldListMap()["TRACKNUMBER"];
        if (!trackField.isEmpty()) {
            auto parts = trackField.front().split('/');
            // Already handled by generic tag, track total may be here
            if (parts.size() > 1) {
                metadata.track_total = parts.back().toInt();
            }
        }
        const auto& discField = xiphComment->fieldListMap()["DISCNUMBER"];
        if (!discField.isEmpty()) {
            auto parts = discField.front().split('/');
            metadata.disc_number = parts.front().toInt();
            if (parts.size() > 1) {
                metadata.disc_total = parts.back().toInt();
            }
        }
        return;
    }
    // Try MP4
    if (auto* mp4File = dynamic_cast<TagLib::MP4::File*>(fileRef.file())) {
        if (auto* mp4Tag = dynamic_cast<TagLib::MP4::Tag*>(mp4File->tag())) {
            if (mp4Tag->contains("trkn")) {
                const auto& trkn = mp4Tag->item("trkn").toIntPair();
                metadata.track = trkn.first;
                metadata.track_total = trkn.second;
            }
            if (mp4Tag->contains("disk")) {
                const auto& disk = mp4Tag->item("disk").toIntPair();
                metadata.disc_number = disk.first;
                metadata.disc_total = disk.second;
            }
        }
    }
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
            result.metadata.comment = rh_taglib_duplicate_tag_string(tag->comment());
            result.metadata.year = static_cast<int>(tag->year());
            result.metadata.track = static_cast<int>(tag->track());
        }

        if (auto* properties = file.audioProperties()) {
            result.metadata.duration_seconds = properties->lengthInSeconds();
            result.metadata.bitrate = properties->bitrate();
            result.metadata.sample_rate = properties->sampleRate();
            result.metadata.channels = properties->channels();
        }

        // Format-specific metadata
        rh_extract_album_artist(result.metadata, file);
        rh_extract_disc_track_totals(result.metadata, file);

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
    std::free(result.metadata.comment);
}
