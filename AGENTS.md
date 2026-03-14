# KeePassXC Android Port - Development Status

## Project Information
This repository is porting the core C++ logic of **KeePassXC** into a native Android application using JNI and Jetpack Compose. Because Android enforces Scoped Storage (Storage Access Framework) and different lifecycle management than desktop operating systems, standard Qt libraries and POSIX C++ File I/O paths have been carefully mocked out and bridged to Android-native primitives.

## What Has Been Completed
During this session, we have successfully hardened the project and unblocked production features:

1. **C++ Mock Replacements:**
   - Rewrote dummy `QByteArray`, `QString`, `QUuid`, and `QDateTime` mocks to use native standard libraries (`<chrono>`, `<regex>`, `/dev/urandom`).
   - Fixed the `Botan` mock headers (`BlockCipher`, `MessageAuthenticationCode`, `HashFunction`) to instantiate functional stub objects instead of returning `nullptr`, which resolved `SIGSEGV` native crashes when executing cryptographic routines.
   - Guaranteed `\0` null-termination at the end of mocked C-strings to fix JNI `NewStringUTF` crashes.

2. **Hardware Security (YubiKey):**
   - Successfully integrated the official Maven `yubikit-android` SDK into the `build.gradle` pipeline.
   - Hooked up `NativeCore` callbacks in `MainActivity.kt` to trigger physical USB/NFC challenge-response operations for `HMAC-SHA1` calculations (Slot 2).

3. **Storage Access Framework (File I/O):**
   - Implemented native database serialization using KeePassXC's `KeePass2Writer`.
   - Wired the byte stream through Kotlin `ContentResolver` to ensure Android 11+ compatibility without requesting broad or dangerous storage permissions. 
   - Prevented the 0-byte truncation bug during writes by delaying the opening of the `OutputStream` until *after* the native layer fully finishes computing the encrypted bytes.
   - Added persistent URI permission grants using `takePersistableUriPermission` so that databases remain accessible across app restarts.

4. **Jetpack Compose UI (Phase 2 Data Parity):**
   - Built a fully-scrollable `AddEditEntryScreen` directly connected to JNI database modification APIs (`addEntry`, `updateEntry`).
   - Added support for supplying a Keyfile alongside the standard password when unlocking.
   - Added a workflow to securely generate and initialize brand new `.kdbx` databases from scratch.
   - Converted the official KeePassXC Desktop SVG icons into Android VectorDrawables and updated the UI menus to use the native KeePassXC iconography.

## What Needs To Be Done
The project is quickly approaching a release candidate, but a few critical features remain:

1. **Group Management:** Implement UI flows to natively create, delete, and rename Groups within the database.
2. **Recycle Bin & History:** Expose the Recycle Bin to the user, and allow viewing/restoring historical states of an entry.
3. **Biometric Keystore:** Securely wrap the KDBX master database key using the Android Hardware Keystore when the user unlocks via a fingerprint, allowing for seamless biometric-only logins later.
4. **Time-Based One-Time Passwords (TOTP):** Implement a dedicated UI component to render the current TOTP string with a ticking 30-second progress ring.

## Action Plan (Next Steps)
- **Sprint 2, Task 1:** Add the `Jetpack Compose` dialogs to manage Groups.
- **Sprint 2, Task 2:** Wire `BiometricHelper.kt` into the Android `KeyStore` to securely encrypt the master password for biometric unlock.
- **Sprint 2, Task 3:** Add the TOTP ticking timer UI to the Entry Detail Screen.
- **Sprint 2, Task 4:** Final QA pass on physical devices to ensure background lifecycle stability (app suspend/resume behavior).
