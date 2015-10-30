#ifndef __APP_DEX_ENCRYPT_H_
#define __APP_DEX_ENCRYPT_H_


#define BUFSIZE 64

struct DATA{
    char* data;
    int len;
};

typedef struct DATA DATA_B;


void* safe_malloc(int size);
void safe_free(void* _ptr);

int set_ecrypt_key(char * key);

int generate_key();

//return in heap
char* get_ecrypt_key();
// return in heap
char* encryptBlock(char* _block, char* re_out,int len);
//return in heap
char* decryptBlock(char * _block,char* re_out,int len);

DATA_B * encryptByte(DATA_B* _data);

//解密的数据块长度必然为64的整数倍
DATA_B * decryptByte(DATA_B* _data);

#endif 