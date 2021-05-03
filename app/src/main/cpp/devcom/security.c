#include "security.h"

void generate_key_pair() {

    __android_log_print(ANDROID_LOG_DEBUG, "DevComJavaMobile_SECURITY_C", "Generate key pair is called\n" );
    ERR_load_crypto_strings(); // Readable OpenSSL error messages
    srand(time(NULL));

    char *file_pem = PRIVATE_KEY;
    char *file_pem_pub = PUBLIC_KEY;
    FILE *fp;
    private_key = EVP_PKEY_new();

    // Check if key files exists
    fp = fopen(file_pem, "r");
    if(fp) {

        if (!PEM_read_RSAPrivateKey(fp, &key_pair, NULL, NULL)) {
            //fprintf(stderr, "Error loading RSA Private Key File.\n");
            __android_log_print(ANDROID_LOG_DEBUG, "DevComJavaMobile_SECURITY_C", "Error loading RSA Private key file\n" );
            return;
        }

        fclose(fp);

        fp = fopen(file_pem_pub, "r");
        if(fp) {
            __android_log_print(ANDROID_LOG_DEBUG, "DevComJavaMobile_SECURITY_C", "Key files already exists. Not creating new ones\n" );
            // if(verbose) { fprintf(stderr, "Key files already exist. Not creating a new ones...\n"); }

            fclose(fp);
            return;
        } else {
          __android_log_print(ANDROID_LOG_DEBUG, "DevComJavaMobile_SECURITY_C", "Didn't find RSA Public key file\n" );
        }
    }

    // Generate key
    __android_log_print(ANDROID_LOG_DEBUG, "DevComJavaMobile_SECURITY_C", "Generating keys...\n" );
    //if(verbose) { fprintf(stdout, "Generating keys...\n"); }
    //key_pair = RSA_generate_key(KEY_LENGTH, RSA_F4, NULL, NULL);

    key_pair = RSA_new();
    BIGNUM *f4 = BN_new();

    unsigned long e = RSA_F4;
    BN_set_word(f4, e);

    RSA_generate_key_ex(key_pair, KEY_LENGTH, f4, NULL);

    // Write private key to file
    fp = fopen(file_pem, "w");
    if(!fp){
        __android_log_print(ANDROID_LOG_VERBOSE, "DevComJavaMobile_SECURITY_C", "Error opening PEM file %s\n", file_pem );
        // fprintf(stderr, "Error opening PEM file %s\n", file_pem);
        return;
    }

    // Blank passphrase
    //const char *passphrase = "";
    if(!PEM_write_RSAPrivateKey(fp, key_pair, NULL, NULL, 0, 0, NULL)) {
        //if(!PEM_write_RSAPrivateKey(fp, key_pair, EVP_des_ede3_cbc(), (unsigned char *) passphrase, 0, NULL, NULL)) {
        __android_log_print(ANDROID_LOG_VERBOSE, "DevComJavaMobile_SECURITY_C", "Error writing PEM file %s\n", file_pem );
        //fprintf(stderr, "Error writing PEM file %s\n", file_pem);
        return;
    }
    fclose(fp);

    // Write public key to file
    fp = fopen(file_pem_pub, "w");
    if(!fp) {
        __android_log_print(ANDROID_LOG_VERBOSE, "DevComJavaMobile_SECURITY_C", "Error opening PEM file %s\n", file_pem_pub );
        // fprintf(stderr, "Error opening PEM file %s\n", file_pem_pub);
        return;
    }

    if(!PEM_write_RSAPublicKey(fp, key_pair)){
        __android_log_print(ANDROID_LOG_VERBOSE, "DevComJavaMobile_SECURITY_C", "Error writing PEM file %s\n", file_pem_pub );
        // fprintf(stderr, "Error writing PEM file %s\n", file_pem_pub);
        return;
    }

    fclose(fp);

    if(!EVP_PKEY_assign_RSA(private_key, key_pair)) {
        __android_log_print(ANDROID_LOG_VERBOSE, "DevComJavaMobile_SECURITY_C", "EVP_PKEY_assign_RSA: failed.\n");
        // fprintf(stderr, "EVP_PKEY_assign_RSA: failed.\n");
        return;
    }
}

char* create_fingerprint_forcpp() {
  return create_fingerprint(PUBLIC_KEY);
}

char* create_fingerprint(char *public_key_file) {
    char fingerprint[17] = {'\0'};
    FILE *fp;
    RSA *rsa;

    fp = fopen(public_key_file, "r");
    rsa = PEM_read_RSAPublicKey(fp, NULL, NULL, NULL);
    fclose(fp);

    char *tmp = BN_bn2hex(rsa->n);

    //TODO: FIXME This is a stupid way of using the same algo. as in generate_ip but skipping the separating colons (FFFF:BBBB)
    memcpy(fingerprint, tmp, 4);
    memcpy(fingerprint + 4, tmp + 5, 4);
    memcpy(fingerprint + 8, tmp + 10, 4);
    memcpy(fingerprint + 12, tmp + 15, 4);

    return strdup(fingerprint);
}

char* generate_ip(char *community, char *file_pem) {
    RSA *rsa;
    FILE *fp;
    char ip[39] = { [0 ... 38] = ':' };
    int i = -1;
    int read = -1;

    // Overwrite first tuple with link local prefix
    memcpy(ip, "fe80:", 5);

    // Set community name to 0000000 48 in dec is 0 in ascii
    memset(ip + 5, 48, 14);

    // Overwrite next three tuples with community name (padding after with zeros, e.g. 'all   ')
    read = 0;
    for(i = 0; i < 14; i++) {
        if(i == 4 || i == 9) {
            memcpy(ip + 5 + i, ":", 1);
        } else {
            if(community[read] == 0) {
                sprintf(ip + 5 + i, "00");
            } else {
                sprintf(ip + 5 + i, "%x", (unsigned int) community[read]);
            }
            read++;
            i++;
        }

        if(read > strlen(community)) {
            break;
        }

    }

    // Overwrite sprintf's \0 byte with 0 in ascii
    memset(ip + i + 6, 48, 1);

    // Add separators in community name: fe80:6d6f:6269:6c65:
    memcpy(ip + 4, ":", 1);
    memcpy(ip + 9, ":", 1);
    memcpy(ip + 14, ":", 1);
    memcpy(ip + 19, ":", 1);

    // Overwrite next four tuples with public key.
    fp = fopen(file_pem, "r");
    if(fp == NULL) {
        fprintf(stderr, "Error opening public key file '%s' in directory '%s'\n", file_pem, community);
        exit(EXIT_FAILURE);
    }
    rsa = PEM_read_RSAPublicKey(fp, NULL, NULL, NULL);
    fclose(fp);

    char *tmp = BN_bn2hex(rsa->n);
    memcpy(ip + 20, tmp, 19);

    // Replace certain bytes for :
    // This is not stictly using the public key, but it works for now
    memcpy(ip + 24, ":", 1);
    memcpy(ip + 29, ":", 1);
    memcpy(ip + 34, ":", 1);

    return strdup(ip);
}

void generate_password(unsigned char* password, int length) {
    int i = 0;

    for(i = 0; i <= length - 1; i++) {
        int n = rand() % 26;
        char c = (char) (n + 65);
        password[i] = c;
        //if(verbose) { fprintf(stdout, "password[%d] = '%c';\n", i, c); }
    }
    __android_log_print(ANDROID_LOG_VERBOSE, "DevComJavaMobile_SECURITY_C", "Generaated key '%s' of length '%d'...\n", password, length );
    //if(verbose) { fprintf(stdout, "Generaated key '%s' of length '%d'...\n", password, length); }
}

// Symmetric key stuff (AES) below this line------------------------------------
int aes_init(unsigned char* key_data, int key_data_len, unsigned char* salt, EVP_CIPHER_CTX* encrypt_ctx, EVP_CIPHER_CTX* decrypt_ctx) {
    int i = 0;
    int nrounds = 3;
    unsigned char key[32] = {'\0'};
    unsigned char iv[32] = {'\0'};

    // Generate key and IV for AES 256 CBC mode. A SHA1 digest is used to hash the supplied key material.
    // nrounds is the number of times the we hash the material. More rounds are more secure but slower.
    i = EVP_BytesToKey(EVP_aes_256_cbc(), EVP_sha1(), salt, key_data, key_data_len, nrounds, key, iv);
    if(i != 32) {
        fprintf(stderr, "Key size is %d bits - should be 256 bits\n", i);
        return -1;
    }

    EVP_CIPHER_CTX_init(encrypt_ctx);
    EVP_CIPHER_CTX_set_padding(encrypt_ctx, 0);
    EVP_EncryptInit_ex(encrypt_ctx, EVP_aes_256_cbc(), NULL, key, iv);
    EVP_CIPHER_CTX_init(decrypt_ctx);
    EVP_CIPHER_CTX_set_padding(decrypt_ctx, 0);
    EVP_DecryptInit_ex(decrypt_ctx, EVP_aes_256_cbc(), NULL, key, iv);

  __android_log_print(ANDROID_LOG_VERBOSE, "DevComJavaMobile_SECURITY_C", "Initialized AES session key...\n" );
  //if(verbose) { fprintf(stdout, "Initialized AES session key...\n"); }

    return 0;
}

unsigned char* aes_encrypt(EVP_CIPHER_CTX* e, unsigned char* plaintext, int* len) {
    // Max ciphertext len for a n bytes of plaintext is n + AES_BLOCK_SIZE -1 bytes
    int c_len = *len + AES_BLOCK_SIZE - 1;
    int f_len = 0;
    unsigned char* ciphertext = (unsigned char*) malloc(c_len);

    // Allows reusing of 'e' for multiple encryption cycles
    if(!EVP_EncryptInit_ex(e, NULL, NULL, NULL, NULL)) {
        fprintf(stderr, "ERROR in EVP_EncryptInit_ex!\n");
        return NULL;
    }

    // Update ciphertext, c_len is filled with the length of ciphertext generated, len is the size of plaintext in bytes
    if(!EVP_EncryptUpdate(e, ciphertext, &c_len, plaintext, *len)) {
        fprintf(stderr, "ERROR in EVP_EncryptUpdate!\n");
        return NULL;
    }

    // Update ciphertext with the final remaining bytes
    if(!EVP_EncryptFinal_ex(e, ciphertext+c_len, &f_len)) {
        fprintf(stderr, "ERROR in EVP_EncryptFinal_ex!\n");
        return NULL;
    }

    *len = c_len + f_len;

    return ciphertext;
}

unsigned char* aes_decrypt(EVP_CIPHER_CTX* e, unsigned char* ciphertext, int* len) {
    // Plaintext will always be equal to or lesser than length of ciphertext
    int p_len = *len;
    int f_len = 0;
    unsigned char *plaintext = (unsigned char*) malloc(p_len);

    if(!EVP_DecryptInit_ex(e, NULL, NULL, NULL, NULL)) {
        fprintf(stderr, "ERROR in EVP_DecryptInit_ex!\n");
        return NULL;
    }

    if(!EVP_DecryptUpdate(e, plaintext, &p_len, ciphertext, *len)) {
        fprintf(stderr, "ERROR in EVP_DecryptUpdate!\n");
        return NULL;
    }

    if(!EVP_DecryptFinal_ex(e, plaintext+p_len, &f_len)) {
        fprintf(stderr, "ERROR in EVP_DecryptFinal_ex!\n");
        return NULL;
    }

    *len = p_len + f_len;
    return plaintext;
}
