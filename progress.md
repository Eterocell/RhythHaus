Route: openspec+superpowers
Owner: implementation
Input: `feature/local-lyrics`, `feature/gapless-playback`
Output: both implementations integrated into local `main` (lyrics commit `d3a63130` is an ancestor; gapless fast-forwarded at `6540ebd6`); gapless controller test race stabilized and roadmap statuses updated.
Next owner: implementation — commit the remaining test/roadmap update, then plan Output and session settings as the next Phase 4 item.
Blockers: user-owned manual audio/UI/device acceptance; `openspec validate --all` currently has one unrelated active-change failure in `playlist-interoperability` (no spec delta).