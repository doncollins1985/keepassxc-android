#ifndef BOTAN_RNG_H
#define BOTAN_RNG_H
#include <stdint.h>
#include <stddef.h>
#include <stdlib.h>

namespace Botan {
    inline void secure_random_fill(uint8_t* out, size_t len) {
        arc4random_buf(out, len);
    }

    class RandomNumberGenerator {
    public:
        virtual ~RandomNumberGenerator() {}
        virtual void randomize(uint8_t*, size_t) {}
    };
    class Autoseeded_RNG : public RandomNumberGenerator {
    public:
        void randomize(uint8_t* out, size_t len) override {
            secure_random_fill(out, len);
        }
    };
    class System_RNG : public RandomNumberGenerator {
    public:
        void randomize(uint8_t* out, size_t len) override {
            secure_random_fill(out, len);
        }
    };
}
#endif