#include "control_traffic.h"

int control_packet_encrypt(unsigned char* packet, unsigned char* payload, const char *key_file) {

	RSA* key_pair;
	char path[64] = {'\0'};
	snprintf(path, 64, "%s", key_file);
	FILE *keyfile = fopen(path, "r");
	if(keyfile) {
		key_pair = RSA_new();
		key_pair = PEM_read_RSAPublicKey(keyfile, NULL, NULL, NULL);
	} else {
		//fprintf(stderr, "Unable to read public key from '%s' into peer index '%d'...\n", path, peer_index);
	}

	fclose(keyfile);

	int bytes_encrypted = 0;

	bytes_encrypted += RSA_public_encrypt(500, payload, packet + 23, key_pair, RSA_PKCS1_PADDING);
	bytes_encrypted += RSA_public_encrypt(500, payload + 500, packet + 23 + 512, key_pair, RSA_PKCS1_PADDING);
	bytes_encrypted += RSA_public_encrypt(500, payload + 1000, packet + 23 + 1024, key_pair, RSA_PKCS1_PADDING);

	if(bytes_encrypted < 0) {
		//fprintf(stderr, "ERROR: '%s'\n", ERR_error_string(ERR_get_error(), NULL));
		return -1;
	}

	// if(verbose) { fprintf(stdout, "Bytes encrypted '%d'...\n", bytes_encrypted); }

	return bytes_encrypted;
}