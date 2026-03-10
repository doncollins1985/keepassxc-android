#ifndef BOTAN_MAC_H
#define BOTAN_MAC_H
#include <vector>
#include <string>
#include <stdint.h>
#include <memory>
namespace Botan {
    class MessageAuthenticationCode {
    public:
        static std::unique_ptr<MessageAuthenticationCode> create(const std::string&) { return std::make_unique<MessageAuthenticationCode>(); }
        virtual ~MessageAuthenticationCode() {}
        virtual void set_key(const uint8_t*, size_t) {}
        virtual void update(const uint8_t*, size_t) {}
        virtual std::vector<uint8_t> final() { return std::vector<uint8_t>(); }
    };
}
#endif