# Release Checklist (`meriem`)

## UI

- [ ] Critical screens open without JavaFX errors.
- [ ] Navigation links route to expected pages.
- [ ] Admin/captain/organizer actions complete with expected feedback.

## EMAIL

- [ ] SMTP configuration is loaded from env (`MAILER_DSN` or desktop override).
- [ ] Team invitation emails render correctly.
- [ ] Sender identity is valid and consistent.

## PDF

- [ ] Team export is generated successfully.
- [ ] PDF layout is readable (spacing, titles, row alignment).
- [ ] Exported file naming is consistent with current flow.

## Build

- [ ] `mvn clean package` succeeds.
