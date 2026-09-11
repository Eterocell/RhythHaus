
## Fix round 3

Moved Review page-zero route construction into the required production `LibraryRouteOverlays` adapter seam. `LibraryAppShell` supplies its real `pushRoute`, and tests now pass `LibraryAppState::pushRoute` rather than duplicating route construction. Close coverage uses the distinct production `OnboardingCloseTestTag` and asserts restoration of the exact captured navigation entry identity. Focused JVM behavior tests and iOS simulator compiles pass.
