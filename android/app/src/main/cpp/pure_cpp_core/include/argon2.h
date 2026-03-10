#ifndef ARGON2_H
#define ARGON2_H
#include <stdint.h>
#include <stddef.h>
#ifdef __cplusplus
extern "C" {
#endif
#define ARGON2_OK 0
#define ARGON2_VERSION_10 0x10
#define ARGON2_VERSION_13 0x13
typedef enum { Argon2_d = 0, Argon2_i = 1, Argon2_id = 2 } argon2_type;
int argon2_hash(const uint32_t t_cost, const uint32_t m_cost, const uint32_t parallelism, const void *pwd, const size_t pwdlen, const void *salt, const size_t saltlen, void *hash, const size_t hashlen, char *encoded, const size_t encodedlen, argon2_type type, const uint32_t version);
int argon2id_hash_raw(const uint32_t t_cost, const uint32_t m_cost, const uint32_t parallelism, const void *pwd, const size_t pwdlen, const void *salt, const size_t saltlen, void *hash, const size_t hashlen);
const char* argon2_error_message(int error_code);
#ifdef __cplusplus
}
#endif
#endif