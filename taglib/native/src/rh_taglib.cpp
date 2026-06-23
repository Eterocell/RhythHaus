#include "rh_taglib.h"

#include <cstdlib>
#include <cstring>

#ifdef RH_TAGLIB_HAS_TAGLIB
#include <audioproperties.h>
#include <fileref.h>
#include <tag.h>
#include <tstring.h>
#include <tpropertymap.h>

#include <ape/apefile.h>
#include <ape/apetag.h>
#include <flac/flacfile.h>
#include <flac/flacpicture.h>
#include <mpeg/id3v2/id3v2tag.h>
#include <mpeg/id3v2/frames/textidentificationframe.h>
#include <mpeg/id3v2/frames/attachedpictureframe.h>
#include <mpeg/mpegfile.h>
#include <mp4/mp4file.h>
#include <mp4/mp4tag.h>
#include <mp4/mp4item.h>
#include <mp4/mp4coverart.h>
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
    if (auto* mpegFile = dynamic_cast<TagLib::MPEG::File*>(fileRef.file())) {
        if (auto* id3v2Tag = mpegFile->ID3v2Tag()) {
            const auto& frameList = id3v2Tag->frameList("TPE2");
            if (!frameList.isEmpty()) {
                metadata.album_artist = rh_taglib_duplicate(frameList.front()->toString().to8Bit(true).c_str());
            }
        }
        return;
    }
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

void rh_extract_artwork(RhTagLibMetadata& metadata, TagLib::FileRef& fileRef) {
    if (auto* mpegFile = dynamic_cast<TagLib::MPEG::File*>(fileRef.file())) {
        if (auto* id3v2Tag = mpegFile->ID3v2Tag()) {
            const auto& frameList = id3v2Tag->frameList("APIC");
            if (!frameList.isEmpty()) {
                auto* apic = dynamic_cast<TagLib::ID3v2::AttachedPictureFrame*>(frameList.front());
                if (apic) {
                    const auto bytes = apic->picture();
                    metadata.artwork_size = bytes.size();
                    metadata.artwork_data = static_cast<unsigned char*>(std::malloc(bytes.size()));
                    if (metadata.artwork_data) std::memcpy(metadata.artwork_data, bytes.data(), bytes.size());
                    metadata.artwork_mime_type = rh_taglib_duplicate(apic->mimeType().to8Bit(true).c_str());
                    return;
                }
            }
        }
        return;
    }
    TagLib::FLAC::File* flacFile = dynamic_cast<TagLib::FLAC::File*>(fileRef.file());
    if (!flacFile) flacFile = dynamic_cast<TagLib::FLAC::File*>(dynamic_cast<TagLib::Ogg::FLAC::File*>(fileRef.file()));
    if (flacFile && !flacFile->pictureList().isEmpty()) {
        const auto* pic = flacFile->pictureList().front();
        const auto bytes = pic->data();
        metadata.artwork_size = bytes.size();
        metadata.artwork_data = static_cast<unsigned char*>(std::malloc(bytes.size()));
        if (metadata.artwork_data) std::memcpy(metadata.artwork_data, bytes.data(), bytes.size());
        metadata.artwork_mime_type = rh_taglib_duplicate(pic->mimeType().to8Bit(true).c_str());
        return;
    }
    if (auto* mp4File = dynamic_cast<TagLib::MP4::File*>(fileRef.file())) {
        auto* mp4Tag = dynamic_cast<TagLib::MP4::Tag*>(mp4File->tag());
        if (mp4Tag && mp4Tag->contains("covr")) {
            const auto& coverList = mp4Tag->item("covr").toCoverArtList();
            if (!coverList.isEmpty()) {
                const auto bytes = coverList.front().data();
                metadata.artwork_size = bytes.size();
                metadata.artwork_data = static_cast<unsigned char*>(std::malloc(bytes.size()));
                if (metadata.artwork_data) std::memcpy(metadata.artwork_data, bytes.data(), bytes.size());
                metadata.artwork_mime_type = rh_taglib_duplicate("image/jpeg");
            }
        }
    }
}

// --- Write helpers ---

bool rh_is_positive(int value) { return value > 0; }

void rh_set_tag_string(TagLib::Tag* tag, const char* value,
                       void (TagLib::Tag::*setter)(const TagLib::String&)) {
    if (value != nullptr && value[0] != '\0') {
        (tag->*setter)(TagLib::String(value, TagLib::String::UTF8));
    }
}

void rh_write_album_artist(TagLib::FileRef& file, const char* value) {
    if (value == nullptr || value[0] == '\0') return;
    const auto str = TagLib::String(value, TagLib::String::UTF8);

    if (auto* mpegFile = dynamic_cast<TagLib::MPEG::File*>(file.file())) {
        if (auto* id3v2Tag = mpegFile->ID3v2Tag()) {
            id3v2Tag->addFrame(new TagLib::ID3v2::TextIdentificationFrame("TPE2", TagLib::String::Latin1));
            id3v2Tag->frameList("TPE2").front()->setText(str);
        }
        return;
    }
    auto* xiphComment = dynamic_cast<TagLib::Ogg::XiphComment*>(file.file()->tag());
    if (xiphComment != nullptr) {
        xiphComment->addField("ALBUMARTIST", str, true);
        return;
    }
    if (auto* mp4File = dynamic_cast<TagLib::MP4::File*>(file.file())) {
        mp4File->tag()->setItem("aART", TagLib::StringList(str));
        return;
    }
    if (auto* apeFile = dynamic_cast<TagLib::APE::File*>(file.file())) {
        apeFile->APETag()->setItem("Album Artist", TagLib::APE::Item("Album Artist", str));
    }
}

void rh_write_disc_track(TagLib::FileRef& file, int track, int trackTotal,
                         int discNumber, int discTotal) {
    if (auto* mpegFile = dynamic_cast<TagLib::MPEG::File*>(file.file())) {
        if (auto* id3v2Tag = mpegFile->ID3v2Tag()) {
            if (track > 0) {
                TagLib::String trck;
                trck += TagLib::String::number(track);
                if (trackTotal > 0) { trck += "/"; trck += TagLib::String::number(trackTotal); }
                id3v2Tag->addFrame(new TagLib::ID3v2::TextIdentificationFrame("TRCK", TagLib::String::Latin1));
                id3v2Tag->frameList("TRCK").front()->setText(trck);
            }
            if (discNumber > 0) {
                TagLib::String tpos;
                tpos += TagLib::String::number(discNumber);
                if (discTotal > 0) { tpos += "/"; tpos += TagLib::String::number(discTotal); }
                id3v2Tag->addFrame(new TagLib::ID3v2::TextIdentificationFrame("TPOS", TagLib::String::Latin1));
                id3v2Tag->frameList("TPOS").front()->setText(tpos);
            }
        }
        return;
    }
    auto* xiphComment = dynamic_cast<TagLib::Ogg::XiphComment*>(file.file()->tag());
    if (xiphComment != nullptr) {
        if (track > 0) {
            TagLib::String val = TagLib::String::number(track);
            if (trackTotal > 0) { val += "/"; val += TagLib::String::number(trackTotal); }
            xiphComment->addField("TRACKNUMBER", val, true);
        }
        if (discNumber > 0) {
            TagLib::String val = TagLib::String::number(discNumber);
            if (discTotal > 0) { val += "/"; val += TagLib::String::number(discTotal); }
            xiphComment->addField("DISCNUMBER", val, true);
        }
        return;
    }
    if (auto* mp4File = dynamic_cast<TagLib::MP4::File*>(file.file())) {
        auto* mp4Tag = mp4File->tag();
        if (track > 0) mp4Tag->setItem("trkn", TagLib::MP4::Item(track, trackTotal > 0 ? trackTotal : 0));
        if (discNumber > 0) mp4Tag->setItem("disk", TagLib::MP4::Item(discNumber, discTotal > 0 ? discTotal : 0));
        return;
    }
    if (auto* apeFile = dynamic_cast<TagLib::APE::File*>(file.file())) {
        auto* apeTag = apeFile->APETag();
        if (track > 0) {
            TagLib::String val = TagLib::String::number(track);
            if (trackTotal > 0) { val += "/"; val += TagLib::String::number(trackTotal); }
            apeTag->setItem("TRACK", TagLib::APE::Item("TRACK", val));
        }
        if (discNumber > 0) {
            TagLib::String val = TagLib::String::number(discNumber);
            if (discTotal > 0) { val += "/"; val += TagLib::String::number(discTotal); }
            apeTag->setItem("DISC", TagLib::APE::Item("DISC", val));
        }
    }
}

#endif // RH_TAGLIB_HAS_TAGLIB

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

        rh_extract_album_artist(result.metadata, file);
        rh_extract_disc_track_totals(result.metadata, file);
        rh_extract_artwork(result.metadata, file);

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
    std::free(result.metadata.artwork_mime_type);
    std::free(result.metadata.artwork_data);
}

extern "C" RhTagLibProperties rh_taglib_read_properties(const char* path) {
    RhTagLibProperties props{};
    props.status = RH_TAGLIB_STATUS_FAILED;
    if (path == nullptr || path[0] == '\0') {
        props.error_message = rh_taglib_duplicate("Path is required");
        return props;
    }

#ifndef RH_TAGLIB_HAS_TAGLIB
    props.status = RH_TAGLIB_STATUS_UNSUPPORTED;
    props.error_message = rh_taglib_duplicate(
        "Native TagLib library was not found at build time"
    );
    return props;
#else
    try {
        TagLib::FileRef file(path);
        if (file.isNull()) {
            props.status = RH_TAGLIB_STATUS_UNSUPPORTED;
            props.error_message = rh_taglib_duplicate("TagLib could not open this file path");
            return props;
        }

        const auto propertyMap = file.file()->properties();

        props.status = RH_TAGLIB_STATUS_FOUND;
        props.property_count = static_cast<int>(propertyMap.size());
        props.keys = static_cast<char**>(std::calloc(propertyMap.size(), sizeof(char*)));
        props.values = static_cast<char**>(std::calloc(propertyMap.size(), sizeof(char*)));

        int index = 0;
        for (const auto& entry : propertyMap) {
            props.keys[index] = rh_taglib_duplicate(entry.first.to8Bit(true).c_str());
            props.values[index] = rh_taglib_duplicate(
                entry.second.toString(", ").to8Bit(true).c_str()
            );
            ++index;
        }

        return props;
    } catch (...) {
        props.status = RH_TAGLIB_STATUS_FAILED;
        props.error_message = rh_taglib_duplicate("TagLib failed while reading properties");
        return props;
    }
#endif
}

extern "C" void rh_taglib_free_properties(RhTagLibProperties properties) {
    std::free(properties.error_message);
    for (int i = 0; i < properties.property_count; ++i) {
        std::free(properties.keys[i]);
        std::free(properties.values[i]);
    }
    std::free(properties.keys);
    std::free(properties.values);
}

extern "C" int rh_taglib_write_path(const char* path, const RhTagLibWriteMeta* meta, char** error_out) {
    if (path == nullptr || path[0] == '\0') {
        if (error_out) *error_out = rh_taglib_duplicate("Path is required");
        return RH_TAGLIB_STATUS_FAILED;
    }
    if (meta == nullptr) {
        if (error_out) *error_out = rh_taglib_duplicate("Metadata is required");
        return RH_TAGLIB_STATUS_FAILED;
    }

#ifndef RH_TAGLIB_HAS_TAGLIB
    if (error_out) *error_out = rh_taglib_duplicate("TagLib not available at build time");
    return RH_TAGLIB_STATUS_UNSUPPORTED;
#else
    try {
        TagLib::FileRef file(path);
        if (file.isNull()) {
            if (error_out) *error_out = rh_taglib_duplicate("TagLib could not open file for writing");
            return RH_TAGLIB_STATUS_UNSUPPORTED;
        }

        auto* tag = file.tag();
        if (tag == nullptr) {
            if (error_out) *error_out = rh_taglib_duplicate("File has no tag interface");
            return RH_TAGLIB_STATUS_UNSUPPORTED;
        }

        rh_set_tag_string(tag, meta->title, &TagLib::Tag::setTitle);
        rh_set_tag_string(tag, meta->artist, &TagLib::Tag::setArtist);
        rh_set_tag_string(tag, meta->album, &TagLib::Tag::setAlbum);
        rh_set_tag_string(tag, meta->genre, &TagLib::Tag::setGenre);
        rh_set_tag_string(tag, meta->comment, &TagLib::Tag::setComment);
        if (meta->year > 0) tag->setYear(static_cast<unsigned int>(meta->year));
        if (meta->track > 0) tag->setTrack(static_cast<unsigned int>(meta->track));

        rh_write_album_artist(file, meta->album_artist);
        rh_write_disc_track(file, meta->track, meta->track_total,
                            meta->disc_number, meta->disc_total);

        if (meta->property_count > 0 && meta->property_keys != nullptr && meta->property_values != nullptr) {
            TagLib::PropertyMap props;
            for (int i = 0; i < meta->property_count; ++i) {
                if (meta->property_keys[i] != nullptr && meta->property_values[i] != nullptr) {
                    TagLib::String key(meta->property_keys[i], TagLib::String::UTF8);
                    TagLib::String value(meta->property_values[i], TagLib::String::UTF8);
                    const auto parts = value.split(", ");
                    for (const auto& part : parts) {
                        props[key].append(part);
                    }
                }
            }
            if (!props.isEmpty()) {
                file.file()->setProperties(props);
            }
        }

        if (!file.save()) {
            if (error_out) *error_out = rh_taglib_duplicate("TagLib failed to save file");
            return RH_TAGLIB_STATUS_FAILED;
        }

        if (error_out) *error_out = nullptr;
        return RH_TAGLIB_STATUS_FOUND;

    } catch (...) {
        if (error_out) *error_out = rh_taglib_duplicate("TagLib threw an exception while writing");
        return RH_TAGLIB_STATUS_FAILED;
    }
#endif
}

extern "C" void rh_taglib_free_write_meta(RhTagLibWriteMeta* meta) {
    if (meta == nullptr) return;
    std::free(meta->title);
    std::free(meta->artist);
    std::free(meta->album);
    std::free(meta->album_artist);
    std::free(meta->genre);
    std::free(meta->comment);
    for (int i = 0; i < meta->property_count; ++i) {
        std::free(meta->property_keys[i]);
        std::free(meta->property_values[i]);
    }
    std::free(meta->property_keys);
    std::free(meta->property_values);
}
