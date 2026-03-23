# KeePassXC Android Feature Parity Matrix

<<<<<<< HEAD
<!--toc:start-->
- [KeePassXC Android Feature Parity Matrix](#keepassxc-android-feature-parity-matrix)
  - [Priority For Next Milestones](#priority-for-next-milestones)
<!--toc:end-->

=======
>>>>>>> 6675bcd4c6350900c3077bf10203a5af88e3ba18
Date: 2026-03-07

Status labels:
- `Implemented (Prototype)` = present but not yet production-hardened
- `In Progress` = partial implementation
- `Not Started` = no meaningful implementation yet
- `Deferred (Non-Portable)` = desktop-specific feature not practical on Android

| Feature Area | Status | Notes |
|---|---|---|
| Open KDBX database (password) | Implemented (Prototype) | JNI open from SAF bytes is wired. |
| Open KDBX with key file | Implemented (Prototype) | Key file bytes pass through JNI; needs deeper verification tests. |
| Open KDBX with YubiKey challenge-response | In Progress | Currently mock callback bytes in UI; not real hardware flow. |
| Create new KDBX database | Implemented (Prototype) | Create + initial save supported. |
| Save modified database | Implemented (Prototype) | Serialize via JNI and write using SAF URI. |
| Browse groups hierarchy | Implemented (Prototype) | JNI traversal + group-scoped listing are wired; advanced tree UI refinement remains. |
| Search entries | Implemented (Prototype) | Basic title/username search in Compose list. |
<<<<<<< HEAD
| View entry details | Implemented | Title, username, password, url, notes, ticking TOTP progress ring. |
| Add entry | Implemented | Added to currently selected group via group UUID. |
| Edit/delete/move entry | Implemented | JNI + `NativeCore` + `DatabaseViewModel` + Compose UI. |
| Group management (add/edit/delete/move) | Implemented | JNI + `NativeCore` + `DatabaseViewModel` + Drawer-based navigation and Dialog management. |
| Entry history | Implemented | JNI + Kotlin model support; restored via Detail Screen history dialog. |
| Recycle bin | Implemented | JNI restore/empty operations; Empty Bin action in Group drawer. |
| Tags and custom attributes | Implemented (Prototype) | Entry tag update + custom attribute APIs implemented; UX polish pending. |
| Attachments | Implemented (Prototype) | Attachment list/read/write/remove APIs implemented; full attachment UI pending. |
| Password generator | Implemented | JNI generator available in UI. |
| TOTP generation | Implemented | Real-time ticking TOTP with 30s auto-refresh in Entry Detail View. |
| Password health/reporting | Not Started | No Android-side reporting workflow. |
| Autofill service | Implemented (Prototype) | Service exists; matching/authorization hardening pending. |
| Biometric unlock | Implemented | Secure Android Keystore wrapping of master password when biometric unlock enabled. |
=======
| View entry details | Implemented (Prototype) | Title, username, password, URL, notes, TOTP string display. |
| Add entry | Implemented (Prototype) | Added to currently selected group via group UUID. |
| Edit/delete/move entry | Implemented (Prototype) | JNI + `NativeCore` + `DatabaseViewModel` implemented; full Compose workflows pending. |
| Group management (add/edit/delete/move) | Implemented (Prototype) | JNI + `NativeCore` + `DatabaseViewModel` implemented; full Compose workflows pending. |
| Entry history | Implemented (Prototype) | JNI + Kotlin model support implemented; UI/history timeline not fully built. |
| Recycle bin | Implemented (Prototype) | JNI restore/empty operations implemented; dedicated recycle-bin UI pending. |
| Tags and custom attributes | Implemented (Prototype) | Entry tag update + custom attribute APIs implemented; UX polish pending. |
| Attachments | Implemented (Prototype) | Attachment list/read/write/remove APIs implemented; full attachment UI pending. |
| Password generator | Implemented (Prototype) | JNI generator available in UI. |
| TOTP generation | In Progress | TOTP string shown if available; no dedicated lifecycle/refresh UX. |
| Password health/reporting | Not Started | No Android-side reporting workflow. |
| Autofill service | Implemented (Prototype) | Service exists; matching/authorization hardening pending. |
| Biometric unlock | In Progress | Prompt is wired; secure credential storage lifecycle incomplete. |
>>>>>>> 6675bcd4c6350900c3077bf10203a5af88e3ba18
| Settings persistence | Implemented (Prototype) | DataStore settings for autofill, biometric, lock timeout, clipboard timeout, secure screen, and last URI. |
| Auto-lock on background | Implemented (Prototype) | Configurable lock timeout applied on lifecycle resume/start transitions. |
| Secure-screen anti-screenshot | Implemented (Prototype) | `FLAG_SECURE` toggled from settings to protect recent-app snapshots/screenshots. |
| Import/export formats (CSV/XML/HTML/3rd-party) | Not Started | No Android UI/API for import/export workflows. |
| Browser integration (desktop extension protocol) | Deferred (Non-Portable) | Desktop-specific native messaging model. |
| Auto-Type | Deferred (Non-Portable) | Desktop window/input automation does not map cleanly to Android. |
| SSH Agent integration | Deferred (Non-Portable) | Desktop-oriented agent workflow. |
| KeeShare | Not Started | No Android-specific integration yet. |
| CLI (`keepassxc-cli`) | Deferred (Non-Portable) | Not part of native Android app scope. |

## Priority For Next Milestones
1. Security parity first: remove crypto mocks and validate KDBX correctness.
2. Data parity second: groups + edit/delete + recycle bin + history.
<<<<<<< HEAD
3. Platform parity third: production-grade biometrics and autofill trust model.
=======
3. Platform parity third: production-grade biometrics and autofill trust model.
>>>>>>> 6675bcd4c6350900c3077bf10203a5af88e3ba18
