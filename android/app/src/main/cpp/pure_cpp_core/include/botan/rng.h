#ifndef BOTAN_RNG_H
#define BOTAN_RNG_H
#include <stdint.h>
#include <stddef.h>
#include <stdio.h>
namespace Botan {
    class RandomNumberGenerator {
    public:
        virtual ~RandomNumberGenerator() {}
        virtual void randomize(uint8_t*, size_t) {}
    };
    class Autoseeded_RNG : public RandomNumberGenerator {};
    class System_RNG : public RandomNumberGenerator {};

    inline void secure_random_fill(uint8_t* out, size_t len) {
        FILE* f = fopen("/dev/urandom", "rb");
        if (f) {
            size_t read = fread(out, 1, len, f);
            (void)read;
            fclose(f);
        }
    }
}
#endif