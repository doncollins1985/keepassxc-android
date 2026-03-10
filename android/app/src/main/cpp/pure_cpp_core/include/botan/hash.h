#ifndef BOTAN_HASH_H
#define BOTAN_HASH_H
#include "secmem.h"
#include <vector>
#include <string>
#include <stdint.h>
#include <memory>
namespace Botan {
    class HashFunction {
    public:
        static std::unique_ptr<HashFunction> create(const std::string&) { return std::make_unique<HashFunction>(); }
        virtual ~HashFunction() {}
        virtual void update(const uint8_t*, size_t) {}
        virtual std::vector<uint8_t> final() { return std::vector<uint8_t>(); }
    };
}
#endif