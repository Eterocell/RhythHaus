#include "rh_taglib.h"

#include <array>
#include <cstdint>
#include <cstdlib>
#include <cstring>
#include <fcntl.h>
#include <fstream>
#include <iostream>
#include <iterator>
#include <stdexcept>
#include <string>
#include <unistd.h>
#include <vector>

namespace {

using Bytes = std::vector<unsigned char>;
using ReplayGainValues = std::array<const char*, 4>;

void require(bool condition, const char* message) {
    if (!condition) throw std::runtime_error(message);
}

void append_text(Bytes& bytes, const char* text) {
    bytes.insert(bytes.end(), text, text + std::strlen(text));
}

void append_int_le(Bytes& bytes, std::uint32_t value, int size = 4) {
    for (int i = 0; i < size; ++i) {
        bytes.push_back(static_cast<unsigned char>((value >> (i * 8)) & 0xff));
    }
}

void append_frame(Bytes& frames, const char* id, const Bytes& text) {
    append_text(frames, id);
    const auto size = static_cast<std::uint32_t>(text.size());
    for (int shift = 24; shift >= 0; shift -= 8) {
        frames.push_back(static_cast<unsigned char>((size >> shift) & 0xff));
    }
    frames.insert(frames.end(), {0, 0});
    frames.insert(frames.end(), text.begin(), text.end());
}

void append_tag_text(Bytes& frames, const char* id, const char* value) {
    Bytes text{0}; // ID3v2 Latin-1 encoding; fixture text is ASCII.
    append_text(text, value);
    text.push_back(0);
    append_frame(frames, id, text);
}

Bytes id3v2_tag(const ReplayGainValues& values) {
    constexpr std::array<const char*, 4> keys{
        "replaygain_track_gain", "REPLAYGAIN_TRACK_PEAK",
        "REPLAYGAIN_ALBUM_GAIN", "REPLAYGAIN_ALBUM_PEAK"
    };
    Bytes frames;
    append_tag_text(frames, "TIT2", "Native Fixture Title");
    append_tag_text(frames, "TPE1", "Native Fixture Artist");
    append_tag_text(frames, "TALB", "Native Fixture Album");
    for (std::size_t i = 0; i < values.size(); ++i) {
        if (values[i] == nullptr) continue;
        Bytes text{0};
        append_text(text, keys[i]);
        text.push_back(0);
        append_text(text, values[i]);
        text.push_back(0);
        append_frame(frames, "TXXX", text);
    }
    Bytes tag;
    append_text(tag, "ID3");
    tag.insert(tag.end(), {3, 0, 0}); // ID3v2.3, no flags.
    const auto frame_size = static_cast<std::uint32_t>(frames.size());
    for (int shift = 21; shift >= 0; shift -= 7) {
        tag.push_back(static_cast<unsigned char>((frame_size >> shift) & 0x7f));
    }
    tag.insert(tag.end(), frames.begin(), frames.end());
    return tag;
}

Bytes wav_fixture(const ReplayGainValues& values) {
    const auto tag = id3v2_tag(values);
    Bytes wav;
    constexpr std::uint32_t data_size = 8000;
    const auto tag_size = static_cast<std::uint32_t>(tag.size());
    append_text(wav, "RIFF");
    append_int_le(wav, 4 + 24 + 8 + data_size + 8 + tag_size + tag_size % 2);
    append_text(wav, "WAVEfmt ");
    append_int_le(wav, 16);
    append_int_le(wav, 1, 2); // PCM.
    append_int_le(wav, 1, 2); // Mono.
    append_int_le(wav, 8000);
    append_int_le(wav, 8000);
    append_int_le(wav, 1, 2);
    append_int_le(wav, 8, 2);
    append_text(wav, "data");
    append_int_le(wav, data_size);
    wav.insert(wav.end(), data_size, 0x80);
    append_text(wav, "ID3 ");
    append_int_le(wav, tag_size);
    wav.insert(wav.end(), tag.begin(), tag.end());
    if (tag_size % 2 != 0) wav.push_back(0);
    return wav;
}

void append_ape_item(Bytes& items, const char* key, const char* value) {
    append_int_le(items, static_cast<std::uint32_t>(std::strlen(value)));
    append_int_le(items, 0); // UTF-8 text item.
    append_text(items, key);
    items.push_back(0);
    append_text(items, value);
}

Bytes apev2_tag(const ReplayGainValues& values) {
    constexpr std::array<const char*, 4> keys{
        "REPLAYGAIN_TRACK_GAIN", "REPLAYGAIN_TRACK_PEAK",
        "REPLAYGAIN_ALBUM_GAIN", "REPLAYGAIN_ALBUM_PEAK"
    };
    Bytes tag;
    append_ape_item(tag, "TITLE", "Secondary APE Title");
    append_ape_item(tag, "ARTIST", "Secondary APE Artist");
    append_ape_item(tag, "ALBUM", "Secondary APE Album");
    std::uint32_t item_count = 3;
    for (std::size_t i = 0; i < values.size(); ++i) {
        if (values[i] == nullptr) continue;
        append_ape_item(tag, keys[i], values[i]);
        ++item_count;
    }
    const auto tag_size = static_cast<std::uint32_t>(tag.size() + 32);
    append_text(tag, "APETAGEX");
    append_int_le(tag, 2000);
    append_int_le(tag, tag_size); // Items plus footer, no header.
    append_int_le(tag, item_count);
    append_int_le(tag, 0);
    tag.insert(tag.end(), 8, 0); // Reserved footer bytes.
    return tag;
}

Bytes read_bytes(const char* path) {
    std::ifstream file(path, std::ios::binary);
    require(file.is_open(), "Could not open fixture bytes");
    return Bytes(std::istreambuf_iterator<char>(file), std::istreambuf_iterator<char>());
}

Bytes mp3_fixture(const ReplayGainValues& primary, const ReplayGainValues& secondary) {
    auto mp3 = id3v2_tag(primary);
    // Untagged, encoder-produced MPEG audio from the pinned TagLib fixtures.
    // Copy the bytes into our disposable file; never modify the upstream seed.
    const auto audio = read_bytes(RH_TAGLIB_TEST_MP3_FIXTURE);
    mp3.insert(mp3.end(), audio.begin(), audio.end());
    const auto ape = apev2_tag(secondary);
    mp3.insert(mp3.end(), ape.begin(), ape.end());
    return mp3;
}

class Fixture {
public:
    explicit Fixture(const Bytes& bytes, const char* suffix = ".wav")
        : path(std::string("/tmp/rhythhaus-replaygain-XXXXXX") + suffix) {
        fd = mkstemps(path.data(), static_cast<int>(std::strlen(suffix)));
        require(fd >= 0, "Could not create disposable media fixture");
        std::size_t offset = 0;
        while (offset < bytes.size()) {
            const auto count = write(fd, bytes.data() + offset, bytes.size() - offset);
            if (count <= 0) {
                close(fd);
                unlink(path.c_str());
                throw std::runtime_error("Could not write disposable media fixture");
            }
            offset += static_cast<std::size_t>(count);
        }
    }

    ~Fixture() {
        close(fd);
        unlink(path.c_str());
    }

    Fixture(const Fixture&) = delete;
    Fixture& operator=(const Fixture&) = delete;

    int fd = -1;
    std::string path;
};

class OwnedResult {
public:
    explicit OwnedResult(RhTagLibResult result) : value(result) {}
    ~OwnedResult() { rh_taglib_free_result(value); }
    OwnedResult(const OwnedResult&) = delete;
    OwnedResult& operator=(const OwnedResult&) = delete;

    RhTagLibResult value;
};

bool equals(const char* actual, const char* expected) {
    return expected == nullptr ? actual == nullptr
        : actual != nullptr && std::strcmp(actual, expected) == 0;
}

void verify_metadata(const RhTagLibResult& result, const ReplayGainValues& expected,
                     int sample_rate, int duration_seconds) {
    require(result.status == 0, "Optional tags invalidated playable media metadata");
    require(equals(result.metadata.title, "Native Fixture Title"), "Existing title was lost");
    require(equals(result.metadata.artist, "Native Fixture Artist"), "Existing artist was lost");
    require(equals(result.metadata.album, "Native Fixture Album"), "Existing album was lost");
    require(result.metadata.duration_seconds == duration_seconds, "Existing duration changed");
    require(result.metadata.sample_rate == sample_rate, "Existing sample rate changed");
    if (sample_rate == 44100) {
        require(result.metadata.bitrate == 64, "Existing MP3 bitrate changed");
        require(result.metadata.channels == 1, "Existing MP3 channel count changed");
    }
    require(equals(result.metadata.replay_gain_track_gain, expected[0]), "Track gain mismatch");
    require(equals(result.metadata.replay_gain_track_peak, expected[1]), "Track peak mismatch");
    require(equals(result.metadata.replay_gain_album_gain, expected[2]), "Album gain mismatch");
    require(equals(result.metadata.replay_gain_album_peak, expected[3]), "Album peak mismatch");
}

void verify_fixture_path_and_fd(const Bytes& original, const char* suffix, const char* display_name,
                                const ReplayGainValues& expected, int sample_rate, int duration_seconds) {
    const Fixture fixture(original, suffix);
    {
        const OwnedResult result(rh_taglib_read_path(fixture.path.c_str()));
        verify_metadata(result.value, expected, sample_rate, duration_seconds);
    }
    require(read_bytes(fixture.path.c_str()) == original, "Path read changed media bytes");
    for (int i = 0; i < 3; ++i) {
        require(lseek(fixture.fd, 0, SEEK_SET) == 0, "Caller descriptor was closed");
        const OwnedResult result(rh_taglib_read_fd(fixture.fd, display_name));
        verify_metadata(result.value, expected, sample_rate, duration_seconds);
        require(fcntl(fixture.fd, F_GETFD) != -1, "FD read closed caller descriptor");
    }
    require(read_bytes(fixture.path.c_str()) == original, "Descriptor read changed media bytes");
}

void verify_path_and_fd(const ReplayGainValues& values) {
    verify_fixture_path_and_fd(wav_fixture(values), ".wav", "song.wav", values, 8000, 1);
}

void verify_mp3_path_and_fd(const ReplayGainValues& primary, const ReplayGainValues& secondary,
                          const ReplayGainValues& expected) {
    verify_fixture_path_and_fd(mp3_fixture(primary, secondary), ".mp3", "song.mp3", expected, 44100, 3);
}

} // namespace

int main() {
    try {
        verify_path_and_fd({nullptr, nullptr, nullptr, nullptr});
        verify_path_and_fd({"-6.00 dB", "1.2", "-4.00 dB", "1.1"});
        verify_path_and_fd({"-6 dB", "2", "NaN dB", "junk"});
        verify_mp3_path_and_fd(
            {nullptr, nullptr, nullptr, nullptr},
            {"-6.00 dB", "1.2", "-4.00 dB", "1.1"},
            {"-6.00 dB", "1.2", "-4.00 dB", "1.1"}
        );
        verify_mp3_path_and_fd(
            {"-3.00 dB", nullptr, nullptr, nullptr},
            {"-6.00 dB", "1.2", "-4.00 dB", "1.1"},
            {"-3.00 dB", "1.2", "-4.00 dB", "1.1"}
        );
        verify_mp3_path_and_fd(
            {"-3.00 dB", "0.9", "-2.00 dB", "0.8"},
            {"-6.00 dB", "1.2", "-4.00 dB", "1.1"},
            {"-3.00 dB", "0.9", "-2.00 dB", "0.8"}
        );
        std::cout << "Native WAV/MP3 path/FD ReplayGain regressions passed; media bytes unchanged\n";
        return EXIT_SUCCESS;
    } catch (const std::exception& error) {
        std::cerr << error.what() << '\n';
        return EXIT_FAILURE;
    }
}
