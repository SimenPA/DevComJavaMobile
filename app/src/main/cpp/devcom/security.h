#define _GNU_SORCE

#include <openssl/evp.h>
#include <openssl/rsa.h>
#include <openssl/pem.h>
#include <openssl/ssl.h>
#include <openssl/err.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <android/log.h>

#define PRIVATE_KEY "/data/data/com.example.devcomjavamobile/public_key.pem.tramp"
#define PUBLIC_KEY "/data/data/com.example.devcomjavamobile/public_key.pem.tramp"
#define KEY_LENGTH 4096
#define AES_BLOCK_SIZE 256
#define PASSWORD_LENGTH 32 // 8 bits * 32 chars = 256 bit keys

RSA *key_pair;
EVP_PKEY *private_key;

void generate_key_pair();

char* generate_ip(char *community, char *file_pem);
char* create_fingerprint_forcpp();
char* create_fingerprint(char *public_key_file);

void generate_password(unsigned char* password, int length);

int aes_init(unsigned char* key_data, int key_data_len, unsigned char* salt, EVP_CIPHER_CTX* encrypt_ctx, EVP_CIPHER_CTX* decrypt_ctx);
unsigned char* aes_encrypt(EVP_CIPHER_CTX* e, unsigned char* plaintext, int* len);
unsigned char* aes_decrypt(EVP_CIPHER_CTX* e, unsigned char* ciphertext, int* len);
