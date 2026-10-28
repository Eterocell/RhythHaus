# Spec Delta

## ADDED Requirements

### Requirement: Search rows expose metadata editing
When app-local metadata editing is available, each Search result row SHALL expose a localized, named Edit metadata action through its secondary-actions menu. Invoking the action SHALL open the existing editor for that row's stable track identity without changing the query, playback selection, or playback queue.

#### Scenario: Open metadata editing from Search
- **WHEN** a listener chooses Edit metadata from a Search result's named secondary-actions menu
- **THEN** the existing editor SHALL open for that exact track while Search retains its query and results

#### Scenario: Cancel metadata editing from Search
- **WHEN** the listener cancels an unsaved Search-originated metadata edit
- **THEN** no override SHALL be saved and the Search result SHALL retain its prior effective metadata
