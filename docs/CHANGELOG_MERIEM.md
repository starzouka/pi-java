# Changelog - Branch `meriem`

This file tracks the most visible changes pushed from branch `meriem`.

## [UI]

- Updated multiple admin and front controllers to support refreshed screens and interactions.
- Improved route-linked pages for captain, organizer, and admin flows.
- Kept JavaFX layouts aligned with the no-`TableView` project constraint.

## [EMAIL]

- Refactored email sending logic in `MailService`.
- Added `TeamEmailTemplateBuilder` to centralize team-related email content.
- Improved email content consistency across invitation and notification flows.

## [PDF]

- Added `TeamPdfGenerator` for team-focused exports.
- Updated export flow integration through `ExportService`.
- Aligned PDF output style with recent UI/email changes for a consistent user-facing experience.
