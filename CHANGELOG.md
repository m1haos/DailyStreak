# Changelog

## [1.1.0] — 2026-09-29

### Added

- `/daily set <player> <day>` makes the chosen calendar day available right now, online or offline.
  Handy for giving streaks back after downtime and for testing rewards. Permission `dailystreak.command.set`.

### Fixed

- Messages added in an update showed up as raw keys (like `day-out-of-range`) when `lang/*.yml` was left
  from an older version. Missing lines now come from the built-in file, no need to delete your translations.

## [1.0.0] — 2026-09-29

First release.

### Added

- Reward calendar of up to 28 days, opened with `/daily`
- Item, money (Vault) and console command rewards
- Streaks with configurable grace days and behaviour after the last day
- Day reset at a set time in a set time zone
- SQLite and MySQL/MariaDB storage
- PlaceholderAPI placeholders: `streak`, `day`, `total`, `available`, `next`
- `/daily reload` and `/daily reset <player>`
- English and Russian messages
