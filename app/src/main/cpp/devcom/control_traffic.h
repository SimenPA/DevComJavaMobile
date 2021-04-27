#define _GNU_SORCE

#include <fcntl.h>
#include <netinet/tcp.h>
#include <sys/select.h>
#include <pthread.h>
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <sys/types.h>
#include <dirent.h>
#include <limits.h>
#include <errno.h>
#include <glob.h>
#include <ctype.h>
#include <asm/types.h>
#include <sys/socket.h>
#include <string.h>
#include <netinet/in.h>
#include <linux/netlink.h>
#include <linux/rtnetlink.h>

#include <openssl/evp.h>
#include <openssl/rsa.h>
#include <openssl/pem.h>
#include <openssl/ssl.h>
#include <openssl/err.h>

#define PORT_CONTROL 3283
#define EVER ;;

int control_packet_encrypt(unsigned char* packet, unsigned char* payload, const char *key_file);
