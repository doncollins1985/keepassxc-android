#ifndef BOTAN_AUTO_RNG_H
#define BOTAN_AUTO_RNG_H

#include "rng.h"

namespace Botan {
    class AutoSeeded_RNG : public RandomNumberGenerator {
    public:
        AutoSeeded_RNG() {}
        void randomize(uint8_t* out, size_t len) override {}
        bool is_seeded() const override { return true; }
        void add_entropy(const uint8_t* in, size_t len) override {}
        std::string name() const override { return "auto"; }
    };
}

#endif
