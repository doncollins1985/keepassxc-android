#ifndef BOTAN_SECMEM_H
#define BOTAN_SECMEM_H

#include <vector>

namespace Botan {
    template <typename T>
    using secure_vector = std::vector<T>;
}

#endif
