# KeePassXC Project Overview

KeePassXC is a modern, secure, and open-source password manager that stores and manages sensitive information in an offline, encrypted database file (KDBX format). It is a community fork of KeePassX and aims to provide a cross-platform, feature-rich password management solution.

## Main Technologies
- **Language:** C++ (C++17 or C++20 if Botan 3 is used)
- **Framework:** Qt 5 (minimum 5.12.0)
- **Cryptography:** Botan, Argon2
- **Build System:** CMake (minimum 3.10.0)
- **Dependencies:** zlib, minizip, libqrencode, readline

## Project Structure
- `src/`: Main source code
    - `core/`: Core logic, database handling (KDBX), and models.
    - `gui/`: User interface components using Qt Widgets.
    - `crypto/`: Cryptography wrappers and utilities.
    - `cli/`: Command-line interface (`keepassxc-cli`).
    - `browser/`: Browser integration host code.
    - `autotype/`: Platform-specific Auto-Type implementation.
    - `keeshare/`: Database sharing and synchronization logic.
    - `keys/`: Key handling (passwords, key files, hardware keys).
- `tests/`: Comprehensive unit and GUI tests.
- `share/`: Icons, translations, and other assets.
- `docs/`: User guides and man pages.

## Building and Running

### Build Dependencies
Ensure you have the following installed:
- CMake (>= 3.10.0)
- G++ (>= 4.9) or Clang++ (>= 6.0)
- Qt 5 (>= 5.12.0)
- Botan (>= 2.11.0)
- zlib, minizip, libqrencode, argon2

### Build Instructions
```bash
mkdir build
cd build
cmake -DWITH_XC_ALL=ON ..
make
```
*Note: Using `-DWITH_XC_ALL=ON` enables all features including browser integration, Auto-Type, and SSH agent.*

### Running Tests
```bash
make test ARGS+="--output-on-failure"
```

### Installation
```bash
sudo make install
```

## Development Conventions

### Coding Style
The project uses `clang-format` to enforce coding standards. You can run `make format` from the build directory.
- **Indentation:** 4 spaces for C++/Header files, 2 spaces for Qt UI files.
- **Naming:** `lowerCamelCase` for functions and variables.
- **Member Variables:** Prefix with `m_` (e.g., `m_database`).
- **Includes:** Order should be Class includes, Application includes, then Global includes.

### Git Workflow
- **Branching:**
    - `develop`: Main development branch for the next release.
    - `feature/[name]`: New features.
    - `fix/[name]`: Bug fixes.
- **Commits:**
    - Use present tense and imperative mood (e.g., "Add feature" instead of "Added feature").
    - Limit the first line to 72 characters.
    - Reference issues using "Fixes #123".

### Testing Strategy
- Always add unit tests for new features or bug fixes in the `tests/` directory.
- Use `WITH_GUI_TESTS=ON` to build and run UI-related tests.
- For bug fixes, reproduce the failure with a test case before applying the fix.

## Generative AI Policy
Submissions made using Generative AI (agent-based or vibe coding) must be documented in the pull request, specifying the service and/or model used.
