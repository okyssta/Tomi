# Independent review response — 1.1.0

Review date: 25 November 2026. The two independent reviews ran against an earlier 1.1.0 working-tree snapshot. Findings below were reconciled against the installed source and either fixed, rejected with evidence, or kept as an explicit release limitation.

Status key: **Fixed** = source changed and automated or artifact evidence exists; **Rejected** = the reported mechanism was not present or the platform claim was incorrect; **Open/manual** = not demonstrated on this host.

## Runtime and security review

| ID | Finding | Status | Evidence / action |
| --- | --- | --- | --- |
| SEC-1 | AAB verifier accepted any self-signed bundle | Fixed | `scripts/verify-release.sh` now compares the AAB certificate SHA-256 with the pinned release certificate before accepting the bundle. APK/AAB metadata is checked exactly. |
| SEC-2 | Update downloads had no response/body size limit | Fixed | `UpdateDownloadWorker` enforces declared and streamed byte limits while retaining streaming cleanup and checksum verification. Covered by direct updater tests. |
| SEC-3 | Backup import retained several full copies | Fixed | Streaming decode now passes bytes directly into JSON decoding; 25 MB input/output and 10,000-record-per-collection limits are enforced on import and export. Boundary tests pass. |
| SEC-4 | Settings copy said health data never leaves Health Connect | Fixed | Copy now states that step totals are cached locally and Health Connect remains read-only. The privacy policy already lists the cache. |
| LOG-1 | Clock-change manifest action did not match Android | Fixed | `Intent.ACTION_TIME_CHANGED` resolves to `android.intent.action.TIME_SET`; the manifest now declares `TIME_SET`. Constant and manifest-receiver tests pass. |
| LOG-2 | Ten-minute snooze was scheduled as a full interval | Fixed | Snooze evaluates policy from request time, not from the expiration instant. A Robolectric `AlarmManager` regression test asserts a ten-minute trigger. |
| LOG-3 | Reused notification ID suppressed later alarms | Fixed | Movement notifications no longer set `FLAG_ONLY_ALERT_ONCE`; a Robolectric test checks the posted flag. |
| LOG-4 | API 28–29 installer lookup omitted `GET_SIGNATURES` | Fixed | The verifier combines `GET_SIGNING_CERTIFICATES` with `GET_SIGNATURES` on API 28–29. |
| LOG-5 | Concurrent step sync could discard smart-pause movement | Fixed | Background receivers, maintenance work, and UI sync now serialize checkpoint read/advance through `MovementCheckpointLock`. Existing compare-and-set checkpoint tests still pass. |
| LOG-6 | New alarm channels discarded legacy notification preferences | Fixed | Existing `movement_reminders` importance, badge, lock-screen, DND, sound, and vibration preferences are migrated once into new channels. Robolectric migration test passes. |
| LOG-7 | `KEEP` retained obsolete maintenance constraints | Fixed | Reminder maintenance and daily update checks use `ExistingPeriodicWorkPolicy.UPDATE`. |
| Background smart pause | Hourly work checked reminders before processing steps | Fixed | `ReminderWorker` now applies the movement checkpoint and reschedules before checking whether a reminder is due. |
| Update foreground channel | Manual download could post before channel creation | Fixed | `createForegroundInfo` creates the updates channel before returning the foreground notification. |
| LOG-8 | Small notification icon may render as a white square | Open/manual | The generated monochrome vector is retained. Device rendering is not verified on this host. |
| UI callback | Backup import did not apply imported reminder settings | Fixed | Import now reschedules reminders and re-enqueues maintenance when imported reminders are enabled, then refreshes Health Connect. |
| Clear all | Runtime work/notifications/cache could survive reset | Fixed | Reset cancels alarms and all work, dismisses notifications, deletes update cache, clears Room/DataStore, and routes to onboarding, which re-enqueues maintenance and update checks. |
| Release gate | Release could proceed unsigned | Fixed | A live negative test temporarily removed local signing configuration; direct-release signing validation failed as required, and the configuration was restored. |

## UI, release, and policy review

| ID | Finding | Status | Evidence / action |
| --- | --- | --- | --- |
| SEC-1 | APK/AAB signer identity unauthenticated | Fixed | The workflow now compares both APK and AAB signer SHA-256 digests with the pinned release certificate. The standalone verifier enforces both certificates. Workflow syntax passes `actionlint`; published artifacts still require release-run verification. |
| SEC-2 | Export/import size policies disagreed | Fixed | `BackupCodec.encode` validates record counts and encoded UTF-8 size before producing a file. |
| SEC-3 | SAF backup preview did not identify destination metadata | Open/manual | `CreateDocument` controls destination selection; destination preview UI is supplied by the Documents app. |
| UI-1 | Check-in date could be stale at midnight | Fixed | `saveCheckIn` refreshes `CurrentDay` immediately before selecting the database key. |
| UI-2 | Background update completion was lost after process restart | Fixed | A validated DataStore snapshot persists complete retry metadata and the WorkManager UUID; Settings resumes terminal and nonterminal state deterministically. |
| UI-3 | Editor delete bypassed confirmation | Fixed | Task and journal editors now show the shared destructive-action dialog. Task-editor regression test passes. |
| UI-4 | Public privacy URL returned 404 | Fixed | GitHub Pages now serves the `docs` directory from `main`; the canonical URL returned HTTP 200 and matched the repository policy file, and release publication rechecks its SHA-256. |
| UI-5 | Repeated Change/Choose/Clear labels lacked context | Fixed | Reminder start/end and task date/time actions have row-specific accessibility descriptions. |
| REL-1 | Version codes could regress | Fixed | Stable-tag workflow rejects non-monotonic codes and components above 999. The standalone verifier now also rejects `v0.0.0` and codes not greater than every previous valid stable tag; negative CLI tests pass. |
| REL-2 | Release scheduling still used `REPLACE`/obsolete variants | Fixed | Current direct/Play task names are used; periodic maintenance uses `UPDATE`; one-time downloads use `APPEND_OR_REPLACE` plus unique temporary files and atomic promotion. |
| REL-3 | Pre-release tags could silently publish stable releases | Fixed | The workflow accepts only exact `vMAJOR.MINOR.PATCH` tags and sets `prerelease: false` only after that validation. |
| REL-4 | Privacy policy publication was not enforced | Fixed | GitHub Pages serves `docs` from `main`; the canonical URL returned HTTP 200 with matching content, and the release workflow rejects a missing or stale policy hash. |
| REL-5 | Bundletool was executed without authenticating its jar hash | Fixed | `scripts/verify-release.sh` checks the pinned Bundletool 1.18.3 SHA-256 before invoking Java. A negative CLI test proves a tampered jar is rejected without execution. |

## Final remediation pass

| Finding | Status | Evidence / action |
| --- | --- | --- |
| DST gap/fold loops and backward-clock suppression | Fixed | Reminder policy uses resolved zoned boundaries, skips invalid gap windows, and clamps future baselines; clock changes normalize stored timestamps. DST regression tests pass. |
| Duplicate or stale reminder delivery | Fixed | Delivery, snooze, cancellation, and rescheduling share `ReminderSchedulerLock`; concurrency tests cover snooze/delivery races. |
| Checkpoint regression and recorder races | Fixed | Checkpoints are monotonic and sync, receiver, worker, and movement recording share `MovementCheckpointLock`; processor/recorder tests pass. |
| Foreground smart pause and disabled smart pause | Fixed | Screen-on/user-present detection uses foreground read permission; hourly work performs no Health Connect read when smart pause is disabled. |
| Unsafe backup IDs | Fixed | Import rejects IDs within 10,000 of `Long.MAX_VALUE`, preserving AUTOINCREMENT headroom; boundary tests pass. |
| Update retry after process recreation | Fixed | Full update URLs and metadata are validated and persisted with the work UUID; nonterminal and terminal WorkManager states are observed. Codec/store tests pass. |
| Concurrent replacement downloads | Fixed | `APPEND_OR_REPLACE` serializes work; downloads use unique temporary files and atomic promotion before sharing the install path. |
| Post-commit import maintenance failure | Fixed | Committed imports remain successful when maintenance fails, while coroutine cancellation is propagated; coordinator tests pass. |
| Clear/import overlap and partial clear errors | Fixed | A shared maintenance mutex serializes destructive operations; clear reports I/O failure and cancellation is not swallowed. |
| Compose recreation data loss | Fixed | Task/journal editor identity and drafts, pending import URI, and picker/confirmation state use saveable state; restoration tests pass. |
| Home quick-action wrapping and progress ambiguity | Fixed | Quick actions stack icon/label in equal-width buttons; a narrow-column Compose test passes. Progress stop dots are removed. |
| Incomplete backup documents | Fixed | Decode requires every top-level backup field before applying defaults; `{}` and version-only documents are rejected. |
| Mid-import cancellation | Fixed | Settings replacement and Room replacement run in a non-cancellable commit window with rollback on failure; cancellation is propagated without splitting a cancelled commit. |
| Step cutoff overlap | Fixed | Movement checkpoints use `max(request time, Health Connect query end)`; regression tests cover cutoff selection. |
| Backward clock checkpoint regression | Fixed | Clock normalization no longer moves `lastStepCheckAt` backward; checkpoint updates remain monotonic. |
| In-flight sync versus clear/import | Fixed | Step sync and movement recording share the maintenance lock; import refreshes Health Connect after releasing the lock. Clear removes external state only after persistent deletion succeeds. |
| Swallowed cancellation and receiver crashes | Fixed | Health, worker, update-check, repository, and ViewModel cancellation is rethrown; reminder/movement receivers install coroutine exception handlers. |
| Stale step-cache overwrite | Fixed | `StepCacheWriter` only accepts entries newer than the cached timestamp. |
| Update enqueue persistence ordering | Fixed | Retry metadata is saved only after WorkManager accepts the request; enqueue-failure tests preserve prior metadata. |
| Stale editor save completion | Fixed | Save/delete completions are generation-guarded, rapid duplicate saves are blocked, and recreation tests cover current-editor closure. |
| Legacy version-code floor and policy publication | Fixed | Prior stable tags with components above 999 participate in the monotonic floor; Pages and release workflows compare the deployed policy SHA-256 with the repository file. |

## Evidence and remaining limits

- The final source snapshot is checked by both flavor unit/Compose suites and Android lint.
- Signed APK/AAB artifacts must be rebuilt after these fixes and verified from the published release, not only from local build output.
- No physical Android device or emulator is attached. Actual alarm sound/vibration, Doze behavior, Health Connect permissions/data, package installation, notification appearance, and updater installation remain manual device gates.
- Google Play submission, Play App Signing, Console declarations, real-device screenshots, and staged rollout remain outside this host-only verification.
