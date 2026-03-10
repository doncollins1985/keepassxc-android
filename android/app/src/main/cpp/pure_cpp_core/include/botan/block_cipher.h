#ifndef BOTAN_BLOCK_CIPHER_H
#define BOTAN_BLOCK_CIPHER_H
#include <memory>
#include <vector>
namespace Botan {
    class BlockCipher {
    public:
        static std::unique_ptr<BlockCipher> create(const char*) { return std::make_unique<BlockCipher>(); }
        virtual ~BlockCipher() {}
        virtual void set_key(const unsigned char*, size_t) {}
        virtual void encrypt(std::vector<unsigned char>&) {}
        virtual void encrypt(unsigned char*) {}
    };
}
#endif