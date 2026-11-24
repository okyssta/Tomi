# Contributing

Thanks for helping improve Tomi.

## Before starting

- Check existing issues and pull requests.
- For a new feature, open an issue describing the user problem and expected behavior.
- Keep changes offline-first and privacy-preserving.
- Use standard Material 3 components; do not add a custom decorative design system.

## Development loop

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

For behavior changes, add or update tests first. Keep business logic in pure Kotlin or repository classes rather than composables.

## Pull requests

Include:

- the problem and proposed change;
- screenshots for UI changes, captured on a real device or emulator;
- test commands and results;
- permission, privacy, migration, or release-impact notes;
- confirmation that no secrets, keystores, tokens, or personal data are included.

CI must pass before merge. Do not commit `local.properties`, `keystore.properties`, signing keys, build outputs, or exported backups.
