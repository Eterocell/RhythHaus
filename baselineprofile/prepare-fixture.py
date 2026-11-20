#!/usr/bin/env python3
"""Generate the disposable RhythHaus Library benchmark catalog without overwriting it."""

import argparse
import wave
from pathlib import Path


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("directory", type=Path, help="a new RhythHausBenchmark directory")
    directory = parser.parse_args().directory
    directory.mkdir()

    frames = bytes(8_000 * 2)
    for index in range(1, 257):
        with wave.open(str(directory / f"RhythHausBenchmark-{index:03}.wav"), "wb") as audio:
            audio.setparams((1, 2, 8_000, 0, "NONE", "not compressed"))
            audio.writeframes(frames)


if __name__ == "__main__":
    main()
