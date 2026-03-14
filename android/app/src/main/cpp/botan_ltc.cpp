#include "botan/block_cipher.h"
#include "botan/cipher_mode.h"
#include "botan/hash.h"
#include "botan/mac.h"

#include "tomcrypt.h"

#include <algorithm>
#include <cstdint>
#include <cstring>
#include <stdexcept>
#include <string>

namespace {
enum class HashAlgo { Sha256, Sha512 };

class LtcHashFunction final : public Botan::HashFunction
{
public:
    explicit LtcHashFunction(HashAlgo algo)
        : m_algo(algo)
    {
        reset();
    }

    void update(const uint8_t* data, size_t len) override
    {
        if (!data || len == 0) {
            return;
        }
        const auto inLen = static_cast<unsigned long>(len);
        int rc = CRYPT_ERROR;
        if (m_algo == HashAlgo::Sha256) {
            rc = sha256_process(&m_state, data, inLen);
        } else {
            rc = sha512_process(&m_state, data, inLen);
        }
        if (rc != CRYPT_OK) {
            throw std::runtime_error("hash update failed");
        }
    }

    Botan::secure_vector<uint8_t> final() override
    {
        Botan::secure_vector<uint8_t> out(digestSize());
        int rc = CRYPT_ERROR;
        if (m_algo == HashAlgo::Sha256) {
            rc = sha256_done(&m_state, out.data());
        } else {
            rc = sha512_done(&m_state, out.data());
        }
        if (rc != CRYPT_OK) {
            throw std::runtime_error("hash final failed");
        }
        reset();
        return out;
    }

    size_t digestSize() const
    {
        return m_algo == HashAlgo::Sha256 ? 32u : 64u;
    }

private:
    void reset()
    {
        int rc = CRYPT_ERROR;
        if (m_algo == HashAlgo::Sha256) {
            rc = sha256_init(&m_state);
        } else {
            rc = sha512_init(&m_state);
        }
        if (rc != CRYPT_OK) {
            throw std::runtime_error("hash init failed");
        }
    }

    HashAlgo m_algo;
    hash_state m_state {};
};

class LtcHmac final : public Botan::MessageAuthenticationCode
{
public:
    explicit LtcHmac(HashAlgo algo)
        : m_algo(algo)
    {
        const size_t blockSize = m_algo == HashAlgo::Sha256 ? 64u : 128u;
        m_ipad.assign(blockSize, 0x36);
        m_opad.assign(blockSize, 0x5c);
        m_hasher.reset(new LtcHashFunction(m_algo));
    }

    void set_key(const uint8_t* key, size_t len) override
    {
        const size_t blockSize = m_algo == HashAlgo::Sha256 ? 64u : 128u;
        Botan::secure_vector<uint8_t> keyBlock(blockSize, 0);

        if (len > blockSize) {
            LtcHashFunction keyHash(m_algo);
            keyHash.update(key, len);
            const auto reduced = keyHash.final();
            std::copy(reduced.begin(), reduced.end(), keyBlock.begin());
        } else if (key && len > 0) {
            std::copy(key, key + len, keyBlock.begin());
        }

        for (size_t i = 0; i < blockSize; ++i) {
            m_ipad[i] = static_cast<uint8_t>(keyBlock[i] ^ 0x36u);
            m_opad[i] = static_cast<uint8_t>(keyBlock[i] ^ 0x5cu);
        }

        m_hasher.reset(new LtcHashFunction(m_algo));
        m_hasher->update(m_ipad.data(), m_ipad.size());
        m_keySet = true;
    }

    void update(const uint8_t* data, size_t len) override
    {
        if (!m_keySet) {
            throw std::runtime_error("hmac key not set");
        }
        m_hasher->update(data, len);
    }

    Botan::secure_vector<uint8_t> final() override
    {
        if (!m_keySet) {
            throw std::runtime_error("hmac key not set");
        }

        const auto inner = m_hasher->final();
        LtcHashFunction outer(m_algo);
        outer.update(m_opad.data(), m_opad.size());
        outer.update(inner.data(), inner.size());
        auto out = outer.final();

        m_hasher.reset(new LtcHashFunction(m_algo));
        m_hasher->update(m_ipad.data(), m_ipad.size());
        return out;
    }

private:
    HashAlgo m_algo;
    Botan::secure_vector<uint8_t> m_ipad;
    Botan::secure_vector<uint8_t> m_opad;
    std::unique_ptr<LtcHashFunction> m_hasher;
    bool m_keySet {false};
};

class Aes256BlockCipher final : public Botan::BlockCipher
{
public:
    void set_key(const uint8_t* key, size_t len) override
    {
        if (!key || len != 32) {
            throw std::runtime_error("invalid AES-256 key length");
        }
        if (rijndael_setup(key, static_cast<int>(len), 0, &m_key) != CRYPT_OK) {
            throw std::runtime_error("rijndael setup failed");
        }
        m_keySet = true;
    }

    void encrypt(Botan::secure_vector<uint8_t>& buffer) override
    {
        if (buffer.size() % 16 != 0) {
            throw std::runtime_error("AES block encrypt requires 16-byte aligned data");
        }
        for (size_t i = 0; i < buffer.size(); i += 16) {
            encrypt(buffer.data() + i);
        }
    }

    void encrypt(uint8_t* block) override
    {
        if (!m_keySet) {
            throw std::runtime_error("AES key not set");
        }
        uint8_t out[16];
        if (rijndael_ecb_encrypt(block, out, &m_key) != CRYPT_OK) {
            throw std::runtime_error("AES encrypt failed");
        }
        std::memcpy(block, out, sizeof(out));
    }

private:
    symmetric_key m_key {};
    bool m_keySet {false};
};

enum class ModeImpl {
    Aes128Cbc,
    Aes256Cbc,
    Aes128Ctr,
    Aes256Ctr,
    TwofishCbc,
    Salsa20,
    ChaCha20
};

inline void incrementCounterBE(uint8_t* ctr, size_t len)
{
    for (size_t i = len; i > 0; --i) {
        ctr[i - 1] = static_cast<uint8_t>(ctr[i - 1] + 1);
        if (ctr[i - 1] != 0) {
            break;
        }
    }
}

class LtcCipherMode final : public Botan::Cipher_Mode
{
public:
    LtcCipherMode(ModeImpl mode, bool encrypt)
        : m_mode(mode)
        , m_encrypt(encrypt)
    {
    }

    void set_key(const uint8_t* key, size_t len) override
    {
        switch (m_mode) {
        case ModeImpl::Aes128Cbc:
        case ModeImpl::Aes128Ctr:
            if (len != 16 || rijndael_setup(key, static_cast<int>(len), 0, &m_aesKey) != CRYPT_OK) {
                throw std::runtime_error("AES-128 key setup failed");
            }
            break;
        case ModeImpl::Aes256Cbc:
        case ModeImpl::Aes256Ctr:
            if (len != 32 || rijndael_setup(key, static_cast<int>(len), 0, &m_aesKey) != CRYPT_OK) {
                throw std::runtime_error("AES-256 key setup failed");
            }
            break;
        case ModeImpl::TwofishCbc:
            if (len != 32 || twofish_setup(key, static_cast<int>(len), 0, &m_twofishKey) != CRYPT_OK) {
                throw std::runtime_error("Twofish key setup failed");
            }
            break;
        case ModeImpl::Salsa20:
            if ((len != 16 && len != 32) || salsa20_setup(&m_salsa, key, static_cast<unsigned long>(len), 20) != CRYPT_OK) {
                throw std::runtime_error("Salsa20 key setup failed");
            }
            break;
        case ModeImpl::ChaCha20:
            if ((len != 16 && len != 32) || chacha_setup(&m_chacha, key, static_cast<unsigned long>(len), 20) != CRYPT_OK) {
                throw std::runtime_error("ChaCha20 key setup failed");
            }
            break;
        }
        m_keySet = true;
    }

    void start(const uint8_t* nonce, size_t len) override
    {
        if (!m_keySet) {
            throw std::runtime_error("cipher key not set");
        }
        if (!valid_nonce_length(len)) {
            throw std::runtime_error("invalid nonce/iv length");
        }

        switch (m_mode) {
        case ModeImpl::Aes128Cbc:
        case ModeImpl::Aes256Cbc:
        case ModeImpl::TwofishCbc:
        case ModeImpl::Aes128Ctr:
        case ModeImpl::Aes256Ctr:
            std::memcpy(m_iv.data(), nonce, len);
            break;
        case ModeImpl::Salsa20:
            if (salsa20_ivctr64(&m_salsa, nonce, static_cast<unsigned long>(len), 0) != CRYPT_OK) {
                throw std::runtime_error("Salsa20 IV setup failed");
            }
            break;
        case ModeImpl::ChaCha20:
            if ((len == 8 && chacha_ivctr64(&m_chacha, nonce, static_cast<unsigned long>(len), 0) != CRYPT_OK)
                || (len == 12 && chacha_ivctr32(&m_chacha, nonce, static_cast<unsigned long>(len), 0) != CRYPT_OK)) {
                throw std::runtime_error("ChaCha20 IV setup failed");
            }
            break;
        }
        m_started = true;
    }

    void process(uint8_t* data, size_t len) override
    {
        if (!m_started) {
            throw std::runtime_error("cipher not started");
        }
        if (!data || len == 0) {
            return;
        }

        switch (m_mode) {
        case ModeImpl::Aes128Cbc:
        case ModeImpl::Aes256Cbc:
            processCbc(data, len, m_aesKey, false);
            break;
        case ModeImpl::TwofishCbc:
            processCbc(data, len, m_twofishKey, true);
            break;
        case ModeImpl::Aes128Ctr:
        case ModeImpl::Aes256Ctr:
            processCtr(data, len);
            break;
        case ModeImpl::Salsa20:
            if (salsa20_crypt(&m_salsa, data, static_cast<unsigned long>(len), data) != CRYPT_OK) {
                throw std::runtime_error("Salsa20 process failed");
            }
            break;
        case ModeImpl::ChaCha20:
            if (chacha_crypt(&m_chacha, data, static_cast<unsigned long>(len), data) != CRYPT_OK) {
                throw std::runtime_error("ChaCha20 process failed");
            }
            break;
        }
    }

    void finish(Botan::secure_vector<uint8_t>& data) override
    {
        if (m_mode == ModeImpl::Aes128Cbc || m_mode == ModeImpl::Aes256Cbc || m_mode == ModeImpl::TwofishCbc) {
            if (m_encrypt) {
                // Add PKCS7 padding
                size_t pad_len = 16 - (data.size() % 16);
                for (size_t i = 0; i < pad_len; ++i) {
                    data.push_back(static_cast<uint8_t>(pad_len));
                }
                process(data.data(), data.size());
            } else {
                // Decrypt first
                if (!data.empty()) {
                    if (data.size() % 16 != 0) {
                        throw std::runtime_error("Ciphertext not multiple of block size");
                    }
                    process(data.data(), data.size());
                }
                // Remove PKCS7 padding
                if (!data.empty()) {
                    uint8_t pad_len = data.back();
                    if (pad_len > 0 && pad_len <= 16 && data.size() >= pad_len) {
                        data.resize(data.size() - pad_len);
                    } else {
                        throw std::runtime_error("Invalid PKCS7 padding");
                    }
                }
            }
        } else {
            if (!data.empty()) {
                process(data.data(), data.size());
            }
        }
    }

    bool valid_nonce_length(size_t len) const override
    {
        switch (m_mode) {
        case ModeImpl::Aes128Cbc:
        case ModeImpl::Aes256Cbc:
        case ModeImpl::TwofishCbc:
        case ModeImpl::Aes128Ctr:
        case ModeImpl::Aes256Ctr:
            return len == 16;
        case ModeImpl::Salsa20:
            return len == 8;
        case ModeImpl::ChaCha20:
            // Keep compatibility with KeePassXC self-tests (8-byte nonce) while
            // also accepting the 96-bit nonce variant.
            return len == 8 || len == 12;
        }
        return false;
    }

private:
    void processCbc(uint8_t* data, size_t len, const symmetric_key& key, bool useTwofish)
    {
        if (len % 16 != 0) {
            throw std::runtime_error("CBC data length must be a multiple of 16");
        }

        for (size_t offset = 0; offset < len; offset += 16) {
            uint8_t* block = data + offset;
            uint8_t tmp[16];
            if (m_encrypt) {
                for (int i = 0; i < 16; ++i) {
                    block[i] ^= m_iv[i];
                }
                if (useTwofish) {
                    if (twofish_ecb_encrypt(block, tmp, &key) != CRYPT_OK) {
                        throw std::runtime_error("Twofish CBC encrypt failed");
                    }
                } else {
                    if (rijndael_ecb_encrypt(block, tmp, &key) != CRYPT_OK) {
                        throw std::runtime_error("AES CBC encrypt failed");
                    }
                }
                std::memcpy(block, tmp, 16);
                std::memcpy(m_iv.data(), block, 16);
            } else {
                std::memcpy(tmp, block, 16);
                uint8_t plain[16];
                if (useTwofish) {
                    if (twofish_ecb_decrypt(block, plain, &key) != CRYPT_OK) {
                        throw std::runtime_error("Twofish CBC decrypt failed");
                    }
                } else {
                    if (rijndael_ecb_decrypt(block, plain, &key) != CRYPT_OK) {
                        throw std::runtime_error("AES CBC decrypt failed");
                    }
                }
                for (int i = 0; i < 16; ++i) {
                    block[i] = static_cast<uint8_t>(plain[i] ^ m_iv[i]);
                }
                std::memcpy(m_iv.data(), tmp, 16);
            }
        }
    }

    void processCtr(uint8_t* data, size_t len)
    {
        uint8_t streamBlock[16];
        size_t offset = 0;
        while (offset < len) {
            std::memcpy(streamBlock, m_iv.data(), 16);
            if (rijndael_ecb_encrypt(streamBlock, streamBlock, &m_aesKey) != CRYPT_OK) {
                throw std::runtime_error("AES CTR keystream generation failed");
            }
            const size_t n = std::min<size_t>(16, len - offset);
            for (size_t i = 0; i < n; ++i) {
                data[offset + i] ^= streamBlock[i];
            }
            offset += n;
            incrementCounterBE(m_iv.data(), m_iv.size());
        }
    }

    ModeImpl m_mode;
    bool m_encrypt {false};
    bool m_keySet {false};
    bool m_started {false};

    symmetric_key m_aesKey {};
    symmetric_key m_twofishKey {};
    salsa20_state m_salsa {};
    chacha_state m_chacha {};
    Botan::secure_vector<uint8_t> m_iv {16, 0};
};

} // namespace

namespace Botan {
std::unique_ptr<HashFunction> HashFunction::create(const std::string& name)
{
    if (name == "SHA-256") {
        return std::unique_ptr<HashFunction>(new LtcHashFunction(HashAlgo::Sha256));
    }
    if (name == "SHA-512") {
        return std::unique_ptr<HashFunction>(new LtcHashFunction(HashAlgo::Sha512));
    }
    return nullptr;
}

std::unique_ptr<MessageAuthenticationCode> MessageAuthenticationCode::create(const std::string& name)
{
    if (name == "HMAC(SHA-256)") {
        return std::unique_ptr<MessageAuthenticationCode>(new LtcHmac(HashAlgo::Sha256));
    }
    if (name == "HMAC(SHA-512)") {
        return std::unique_ptr<MessageAuthenticationCode>(new LtcHmac(HashAlgo::Sha512));
    }
    return nullptr;
}

std::unique_ptr<BlockCipher> BlockCipher::create(const char* name)
{
    if (name && std::string(name) == "AES-256") {
        return std::unique_ptr<BlockCipher>(new Aes256BlockCipher());
    }
    return nullptr;
}

std::unique_ptr<Cipher_Mode> Cipher_Mode::create_or_throw(const std::string& name, Cipher_Dir direction)
{
    const bool encrypt = direction == Cipher_Dir::Encryption || direction == Cipher_Dir::ENCRYPTION;

    if (name == "AES-128/CBC") {
        return std::unique_ptr<Cipher_Mode>(new LtcCipherMode(ModeImpl::Aes128Cbc, encrypt));
    }
    if (name == "AES-256/CBC") {
        return std::unique_ptr<Cipher_Mode>(new LtcCipherMode(ModeImpl::Aes256Cbc, encrypt));
    }
    if (name == "CTR(AES-128)") {
        return std::unique_ptr<Cipher_Mode>(new LtcCipherMode(ModeImpl::Aes128Ctr, encrypt));
    }
    if (name == "CTR(AES-256)") {
        return std::unique_ptr<Cipher_Mode>(new LtcCipherMode(ModeImpl::Aes256Ctr, encrypt));
    }
    if (name == "Twofish/CBC") {
        return std::unique_ptr<Cipher_Mode>(new LtcCipherMode(ModeImpl::TwofishCbc, encrypt));
    }
    if (name == "Salsa20") {
        return std::unique_ptr<Cipher_Mode>(new LtcCipherMode(ModeImpl::Salsa20, encrypt));
    }
    if (name == "ChaCha20") {
        return std::unique_ptr<Cipher_Mode>(new LtcCipherMode(ModeImpl::ChaCha20, encrypt));
    }
    if (name == "AES-256/GCM") {
        throw std::runtime_error("AES-256/GCM not supported in Android native shim");
    }

    throw std::runtime_error("unsupported cipher mode: " + name);
}
} // namespace Botan
