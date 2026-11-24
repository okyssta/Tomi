# Security policy

## Reporting a vulnerability

Please report security issues privately through GitHub's [private vulnerability reporting](https://github.com/okyssta/Tomi/security/advisories/new). Do not open a public issue for a vulnerability until a fix is available.

Include the app version, Android version, reproduction steps, impact, and any relevant logs. Remove personal data from evidence.

## Scope

Tomi processes sensitive local data: tasks, journal entries, mood check-ins, movement history, and Health Connect step totals. Important boundaries include:

- app-private database and DataStore files;
- Health Connect read permissions;
- notification and alarm `PendingIntent`s;
- FileProvider access for downloaded update APKs;
- GitHub release parsing, HTTPS downloads, and checksum verification;
- backup import validation and replacement behavior;
- release signing secrets.

## Update security

Release downloads must use HTTPS GitHub URLs and match the published SHA-256 checksum. Android's package installer verifies the APK signature. A checksum alone is not treated as proof of publisher identity; signature verification remains mandatory.

Never include signing keys, passwords, GitHub tokens, exported backups, or Health Connect data in issues or pull requests.
