# Sprint 1 Inventory: Mock/Stub and Production Risk Audit

Date: 2026-03-07

## Summary
Android builds and packages successfully. Security-critical crypto/KDF mocks in the native path have been replaced by linked implementations (Argon2 + libtomcrypt-backed Botan shim).

Update (2026-03-07, later):
- RNG shim uses OS entropy (`/dev/urandom`) for Botan RNG and `QRandomGenerator`.
- Argon2 now uses vendored reference implementation (`third_party/argon2`) instead of mocks.
- Botan shim interfaces are backed by vendored `libtomcrypt` primitives in `botan_ltc.cpp`.
- Verified with:
  - `./gradlew :app:externalNativeBuildDebug`
  - `./gradlew :app:assembleDebug`
  - `./gradlew :app:compileDebugAndroidTestKotlin`

## High Risk (Blocker)
1. Crypto providers are stubbed in Android shim headers.
- Evidence:
  - `android/app/src/main/cpp/pure_cpp_core/include/botan/hash.h`
  - `android/app/src/main/cpp/pure_cpp_core/include/botan/mac.h`
  - `android/app/src/main/cpp/pure_cpp_core/include/botan/block_cipher.h`
  - `android/app/src/main/cpp/pure_cpp_core/include/botan/cipher_mode.h`
- Status: resolved via real backend implementation in `android/app/src/main/cpp/botan_ltc.cpp`.

2. Argon2 is mocked in native glue (now fail-closed, still blocker for parity).
- Status: resolved; Argon2 reference sources are linked.
- Evidence:
  - `android/app/src/main/cpp/third_party/argon2`
  - `android/app/src/main/cpp/CMakeLists.txt`

3. Randomness shim returns constant values. Resolved.
- Evidence: `android/app/src/main/cpp/pure_cpp_core/include/QtCore/QRandomGenerator`.
- Status: replaced with OS-backed entropy path; no constant-return RNG remains in this shim.
- Risk: reduced; keep under verification with regression tests.

4. YubiKey path is prototype-only.
- Evidence:
  - `android/app/src/main/java/org/keepassxc/android/MainActivity.kt`
- Status: mock callback removed; current UI no longer injects fake challenge-response bytes.
- Risk: feature remains non-functional until real hardware path is implemented.

5. Desktop core behavior was modified to simplify async path.
- Evidence: `src/core/AsyncTask.h` (`QtConcurrent` wait/callback replaced with synchronous execution).
- Risk: behavior divergence/regressions vs upstream KeePassXC core.

## Medium Risk
1. Broad storage permissions are requested.
- Evidence: `android/app/src/main/AndroidManifest.xml` (`MANAGE_EXTERNAL_STORAGE`, legacy read/write).
- Risk: over-privileged app surface and Play policy friction.
- Status (2026-03-09): resolved in current manifest; app relies on SAF URI permissions.

2. Release signing is placeholder and committed.
- Evidence:
  - `android/app/build.gradle` (release signing now sourced from env/Gradle properties)
- Risk: insecure signing process and secret exposure.
- Status (2026-03-09): hardcoded credentials removed from build script; committed test keystore removed.

3. Biometric flow not fully wired to encrypted credential storage lifecycle.
- Evidence:
  - `android/app/src/main/java/org/keepassxc/android/biometrics/BiometricHelper.kt`
  - `android/app/src/main/java/org/keepassxc/android/SettingsManager.kt`
  - `android/app/src/main/java/org/keepassxc/android/MainActivity.kt`
- Status (2026-03-09): improved by binding encrypted credential blobs to the selected database URI and clearing stale secrets when database changes.

## Low Risk / Technical Debt
1. Extensive Qt shim surface has placeholder methods.
- Inventory: 84 shim files total, 25 files with explicit mock/TODO/constant-return patterns.
- Evidence root: `android/app/src/main/cpp/pure_cpp_core/include`.

2. Local unit test target has no source files.
- Evidence: `./gradlew :app:testDebugUnitTest` => `NO-SOURCE`.

## Recommended Remediation Order
1. Replace/disable all security-critical mocks (Botan, Argon2, RNG) and prove KDBX open/save correctness.
   - Progress: completed in code. Deterministic native self-test added (`NativeCore.runNativeSelfTest` + `JniBridgeTest`).
2. Remove prototype YubiKey behavior or gate it behind explicit experimental flag.
3. Reconcile/contain core patches (especially `AsyncTask.h`) to avoid desktop regressions.
4. Remove broad storage permission requirements; rely on SAF persisted URIs.
5. Replace committed signing materials with secure CI/local secret handling.
6. Add JVM + instrumentation + JNI regression tests for unlock/create/save/totp/password generation.