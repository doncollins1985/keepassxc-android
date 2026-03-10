cmake_minimum_required(VERSION 3.22.1)

project("keepassxc-android-core")

set(KEEPASSXC_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../..")
set(SHIM_INCLUDE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/pure_cpp_core/include")

# Add include directories
include_directories(
    "${SHIM_INCLUDE_DIR}"
    "${SHIM_INCLUDE_DIR}/QtCore"
    "${KEEPASSXC_SOURCE_DIR}/src"
    "${KEEPASSXC_SOURCE_DIR}/src/core"
    "${KEEPASSXC_SOURCE_DIR}/src/crypto"
    "${KEEPASSXC_SOURCE_DIR}/src/keys"
    "${KEEPASSXC_SOURCE_DIR}/src/format"
    "${KEEPASSXC_SOURCE_DIR}/src/thirdparty/zxcvbn"
    "${CMAKE_CURRENT_BINARY_DIR}/generated"
)

# Core definitions
add_definitions(-DKEEPASSX_BUILDING_CORE)

# Add the native library
add_library(
    keepassxc-android-core
    SHARED
    keepassxc-jni.cpp
    # Thirdparty
    "${KEEPASSXC_SOURCE_DIR}/src/thirdparty/zxcvbn/zxcvbn.c"
    # Core
    "${KEEPASSXC_SOURCE_DIR}/src/core/Alloc.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/core/Base32.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/core/Clock.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/core/Config.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/core/CustomData.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/core/Database.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/core/Entry.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/core/EntryAttachments.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/core/EntryAttributes.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/core/EntrySearcher.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/core/Group.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/core/Merger.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/core/Metadata.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/core/ModifiableObject.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/core/PasswordGenerator.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/core/PasswordHealth.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/core/AutoTypeAssociations.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/core/TimeInfo.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/core/Tools.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/core/Totp.cpp"
    # Crypto
    "${KEEPASSXC_SOURCE_DIR}/src/crypto/Crypto.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/crypto/CryptoHash.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/crypto/Random.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/crypto/SymmetricCipher.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/crypto/kdf/Kdf.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/crypto/kdf/AesKdf.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/crypto/kdf/Argon2Kdf.cpp"
    # Format
    "${KEEPASSXC_SOURCE_DIR}/src/format/KdbxReader.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/format/KdbxWriter.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/format/Kdbx3Reader.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/format/Kdbx3Writer.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/format/Kdbx4Reader.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/format/Kdbx4Writer.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/format/KdbxXmlReader.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/format/KdbxXmlWriter.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/format/KeePass2.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/format/KeePass2Reader.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/format/KeePass2Writer.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/format/KeePass2RandomStream.cpp"
    # Keys
    "${KEEPASSXC_SOURCE_DIR}/src/keys/CompositeKey.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/keys/FileKey.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/keys/PasswordKey.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/keys/ChallengeResponseKey.cpp"
    # Streams
    "${KEEPASSXC_SOURCE_DIR}/src/streams/HashedBlockStream.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/streams/HashingStream.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/streams/HmacBlockStream.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/streams/LayeredStream.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/streams/StoreDataStream.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/streams/SymmetricCipherStream.cpp"
    "${KEEPASSXC_SOURCE_DIR}/src/streams/qtiocompressor.cpp"
    "${CMAKE_CURRENT_SOURCE_DIR}/android_mocks.cpp"
)

target_link_libraries(
    keepassxc-android-core
    log
    z
)
