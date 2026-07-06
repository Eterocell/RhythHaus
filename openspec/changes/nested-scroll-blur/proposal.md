# Change: Nested Scroll Blur

## Why

Library/Home and track-list screens currently scroll as flat lists. The roadmap calls for a Material 3 Expressive-like nested-scroll effect with a Backdrop/Haze-style blur treatment to add depth while preserving the existing RhythHaus look.

## What Changes

- Add shared top scroll chrome that reacts to list scroll position.
- Apply it to Library/Home and album/artist track-list pages.
- Use existing common Compose APIs instead of adding a new dependency in this pass.
