# KeePassXC Android Roadmap

## Goal
Ship a production-quality native Android app using KeePassXC core, with strong security guarantees and maximum practical feature parity.

## Current Status (March 9, 2026)
- Debug Kotlin/native build passes (`assembleDebug`).
- Instrumentation tests compile (`compileDebugAndroidTestKotlin`); execution still requires connected device/emulator.
- App is functional prototype; core crypto mocks were replaced, with production hardening and validation still in progress.
- Release signing now uses externalized secrets (env/Gradle properties), and committed test keystore material has been removed.

## Phase 1: Security Foundation (Critical)
### Scope
- Remove crypto/platform mocks used in production paths.
- Ensure real cryptographic behavior for KDBX read/write and key derivation.
- Add deterministic regression tests with known test vectors/databases.

### Exit Criteria
- No security-critical mock/stub code in release path.
- On-device tests validate open/save/decrypt/encrypt correctness.
- Threat/risk checklist completed for key handling and memory lifecycle.

Status (2026-03-07): `Code complete, device verification pending`
- Completed:
  - Replaced RNG/Argon2/Botan crypto stubs with linked implementations.
  - Added deterministic native self-test coverage (`NativeCore.runNativeSelfTest`).
  - Android debug builds succeed across all configured ABIs.
- Pending:
  - Execute instrumentation suite on connected emulator/device (`connectedDebugAndroidTest` currently fails only due no connected device).

## Phase 2: Core Data Parity
### Scope
- JNI + Kotlin support for group hierarchy (not flattened only).
- Add/edit/delete/move entries and groups.
- Recycle bin, history, tags, custom attributes, attachments.

### Exit Criteria
- Daily database management workflows complete on Android.
- Parity matrix shows implemented vs deferred features with rationale.

Status (2026-03-07): `Code complete (backend + ViewModel), UI expansion pending in Phase 3`
- Completed:
  - Group hierarchy browsing with UUID-scoped entry listing.
  - Entry CRUD/move JNI APIs and Kotlin bindings.
  - Group CRUD/move JNI APIs and Kotlin bindings.
  - Recycle bin, history, attributes, and attachment JNI APIs and Kotlin bindings.
  - `DatabaseViewModel` wrappers added for all Phase 2 operations.
- Pending:
  - Compose screens for all new management flows (currently partial UI coverage).

## Phase 3: Android Product Polish
### Scope
- Refactor state/navigation for reliability.
- Improve unlock/error/loading flows and offline file edge cases.
- Add lock timeout, clipboard timeout behavior, secure-screen handling.

### Exit Criteria
- Stable UX under rotation/background/resume/low-memory events.
- No blocker-severity UX bugs in test pass.

Status (2026-03-07): `Code complete, device UX validation pending`
- Completed:
  - Screen state resilience improved with saveable top-level navigation state.
  - Unlock flow improved with busy state, actionable errors, and error dismissal.
  - SAF URI persistence handling added to reduce reopen/offline permission failures.
  - Background auto-lock implemented with configurable lock timeout.
  - Clipboard copy auto-clear implemented with configurable timeout.
  - Secure-screen handling implemented via `FLAG_SECURE` with user setting.
- Pending:
  - End-to-end rotation/background/low-memory validation pass on real device/emulator.

## Phase 4: Integrations
### Scope
- Production biometric secret wrapping/unwrapping.
- Autofill matching improvements (package/domain + confidence rules).
- Share/open intent support and safer file permission persistence.

### Exit Criteria
- Autofill and biometric flows are reliable across supported Android versions.

## Phase 5: Validation and Release
### Scope
- CI jobs for Android build + emulator instrumentation.
- Performance/battery sanity pass.
- Replace placeholder signing pipeline and remove committed test keystores/secrets.

### Exit Criteria
- Release candidate signed via secure process.
- Test matrix passes and release notes prepared.

## Sprint 1 (Next 7-10 Days)
1. Inventory all mock/stub files and classify by risk (security/functionality/test-only). Completed in `android/SPRINT1_INVENTORY.md` on 2026-03-07.
2. Replace security-critical mock dependencies first (Botan/Argon2/JNI key paths). Completed in code on 2026-03-07 (real Argon2 + libtomcrypt-backed Botan shim).
3. Add JNI regression tests for open/save/totp/password-generator behavior. Completed in `app/src/androidTest/java/org/keepassxc/android/JniBridgeTest.kt` on 2026-03-09.
4. Set up emulator-based `connectedDebugAndroidTest` in CI. Completed in `.github/workflows/android-instrumentation.yml` on 2026-03-09.
5. Produce a feature parity table (`implemented`, `in progress`, `deferred`). Completed in `android/FEATURE_PARITY_MATRIX.md` on 2026-03-07.