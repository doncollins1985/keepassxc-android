#ifndef BOTAN_CIPHER_MODE_H
#define BOTAN_CIPHER_MODE_H
#include "secmem.h"
#include <vector>
#include <string>
#include <stdint.h>
#include <memory>
namespace Botan {
    enum class Cipher_Dir { Encryption, Decryption, ENCRYPTION, DECRYPTION };
    class Cipher_Mode {
    public:
        static std::unique_ptr<Cipher_Mode> create_or_throw(const std::string&, Cipher_Dir) { return std::make_unique<Cipher_Mode>(); }
        virtual ~Cipher_Mode() {}
        virtual void set_key(const uint8_t*, size_t) {}
        virtual void start(const uint8_t*, size_t) {}
        virtual void process(uint8_t*, size_t) {}
        virtual void finish(std::vector<uint8_t>&) {}
        virtual bool valid_nonce_length(size_t) const { return true; }
    };
}
#endif