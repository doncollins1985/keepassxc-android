#ifndef BOTAN_MEM_OPS_H
#define BOTAN_MEM_OPS_H

#include <cstdint>
#include <cstring>

namespace Botan {
    inline void secure_scrub_memory(void* p, size_t n) {
        std::memset(p, 0, n);
    }
    
    inline bool constant_time_compare(const uint8_t* a, const uint8_t* b, size_t n) {
        return std::memcmp(a, b, n) == 0;
    }
}

#endif
