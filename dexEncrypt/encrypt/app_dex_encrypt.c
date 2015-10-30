
#include <stdio.h>
#include <stdlib.h>
#include <openssl/bio.h>
#include <openssl/evp.h>
#include <openssl/buffer.h>

#include <openssl/des.h>
#include <openssl/rand.h>

#include <time.h>
#include "app_dex_encrypt.h"

#include <string.h>
#include <math.h>

#ifdef  __cplusplus
extern "C" {
#endif

//static DES_cblock seed = {0xFE, 0xDC, 0xBA, 0x98, 0x76, 0x54, 0x32, 0x10};
static DES_cblock ivsetup = {0xE1, 0xE2, 0xE3, 0xD4, 0xD5, 0xC6, 0xC7, 0xA8};
static DES_key_schedule ks1, ks2, ks3;



int count=0;

void* safe_malloc(int size){
    count++;
    return malloc(size);
}

void safe_free(void* _ptr){
    if (_ptr) {
        free(_ptr);
    }
}

int set_ecrypt_key(char * key){
    int key_len =sizeof(DES_key_schedule);
    memcpy(&ks1, key, key_len);
    memcpy(&ks2, key+key_len, key_len);
    memcpy(&ks3, key+key_len*2, key_len);
    
    return 0;
}

int generate_key(){
    srand((unsigned int)time((time_t*)NULL));
    DES_cblock seed = {rand()%255, rand()%255, rand()%255, rand()%255, rand()%255, rand()%255, rand()%255, rand()%255};
    DES_cblock key1, key2, key3;
    RAND_seed(seed, sizeof(DES_cblock));
    
    DES_random_key(&key1);
    DES_random_key(&key2);
    DES_random_key(&key3);
    
    DES_set_key((C_Block *)key1, &ks1);
    DES_set_key((C_Block *)key2, &ks2);
    DES_set_key((C_Block *)key3, &ks3);
    return 0;
}

//return in heap
char* get_ecrypt_key(char *dest_key)
{
    int key_len =sizeof(DES_key_schedule);
//    char * key = safe_malloc(key_len*3);
    memcpy(dest_key, &ks1, key_len);
    memcpy(dest_key+key_len, &ks2, key_len);
    memcpy(dest_key+key_len*2, &ks3, key_len);
    return dest_key;
}

// return in heap
char* encryptBlock(char* _block, char* re_out,int len)
{
    if (len>64) {
        perror("error 1");
        return NULL;
    }
    
//    char* re_out = safe_malloc(BUFSIZE);
    unsigned char in[BUFSIZE],out[BUFSIZE];
    memset(in, 0, BUFSIZE);
    memset(out, 0, BUFSIZE);
    
    memcpy(in, _block, len);
    
    DES_cblock ivec;
    memcpy(ivec, ivsetup, sizeof(ivsetup));
    
    DES_ede3_cbc_encrypt(in, out, len, &ks1, &ks2, &ks3, &ivec, DES_ENCRYPT);
    
    memcpy(re_out, out, BUFSIZE);
    return re_out;
}

//return in heap
char* decryptBlock(char * _block,char* re_out,int len){
    if (len>64) {
        perror("error 1");
        return NULL;
    }
    
//    char re_out[BUFSIZE];
    unsigned char in[BUFSIZE],out[BUFSIZE];
    memset(in, 0, BUFSIZE);
    memset(out, 0, BUFSIZE);
    
    memcpy(in, _block, len);
    
    DES_cblock ivec;
    memcpy(ivec, ivsetup, sizeof(ivsetup));
    
    DES_ede3_cbc_encrypt(in, out, len, &ks1, &ks2, &ks3, &ivec, DES_DECRYPT);
    
    memcpy(re_out, out, BUFSIZE);
    
    return re_out;
}

DATA_B * encryptByte(DATA_B* _data){
    generate_key();

    int key_len =sizeof(DES_key_schedule)*3;

    int ll = sizeof(DATA_B);
    DATA_B* out = safe_malloc(ll);
    int l=_data->len/BUFSIZE;
    int al_len = _data->len%BUFSIZE==0?l:l+1;
    
    out->data=safe_malloc(BUFSIZE*al_len+4+key_len);
    memcpy(out->data, &_data->len, 4);
    get_ecrypt_key(out->data+4);
//    memcpy(out->data+4,key,key_len);
//    safe_free(key);
    
    out->len=al_len*BUFSIZE+4+key_len;
    
    char out_buff[BUFSIZE];
    memset(out_buff, 0, BUFSIZE);
    
    int i=0;
    int size=_data->len;
    int s_l=0;
    
    int offset_len=0;
    for (i=0; i<al_len; i++) {
        s_l=BUFSIZE*i;
        offset_len=size-s_l;
        if (offset_len>=BUFSIZE) {
            encryptBlock(_data->data+s_l,out_buff, BUFSIZE);
        }else{
            encryptBlock(_data->data+s_l,out_buff, offset_len);
        }
        memcpy(out->data+s_l+4+key_len, out_buff, BUFSIZE);
        memset(out_buff, 0, BUFSIZE);
    }


    return out;
}

//解密的数据块长度必然为64的整数倍
DATA_B * decryptByte(DATA_B* _data){
    int ll = sizeof(DATA_B);
    // DATA_B* out = safe_malloc(ll);
    int lll=0;
    memcpy(&lll, _data->data, 4);
    int key_len = sizeof(DES_key_schedule)*3;
    char *key = _data->data+4;
    set_ecrypt_key(key);
    int block=_data->len-4-key_len;
    int l=block/BUFSIZE;
    if(block%BUFSIZE!=0){
        perror("解密数据有错");
        return NULL;
    };
    DATA_B* out = safe_malloc(ll);
    out->len=lll;
    out->data=safe_malloc(lll);
    
    char out_buff[BUFSIZE];
    memset(out_buff, 0, BUFSIZE);
    
    int i=0;
    int s_l=0;
    
    for (i=0; i<l; i++) {
        s_l=BUFSIZE*i;
        decryptBlock(_data->data+s_l+4+key_len,out_buff, BUFSIZE);
        memcpy(out->data+s_l, out_buff, BUFSIZE);
        memset(out_buff, 0, BUFSIZE);
    }
    return out;
}

static int base64_encode(char *str,int str_len,char *encode,int encode_len){
    BIO *bmem,*b64;
    BUF_MEM *bptr;
    b64=BIO_new(BIO_f_base64());
    bmem=BIO_new(BIO_s_mem());
    b64=BIO_push(b64,bmem);
    BIO_write(b64,str,str_len); //encode
    BIO_flush(b64);
    BIO_get_mem_ptr(b64,&bptr);
    if(bptr->length>encode_len){
//        DPRINTF("encode_len too small\n");
        return -1;
    }
    encode_len=bptr->length;
    memcpy(encode,bptr->data,bptr->length);
    //  write(1,encode,bptr->length);
    BIO_free_all(b64);
    return encode_len;
}

static int base64_decode(char *str,int str_len,char *decode,int decode_buffer_len){
    int len=0;
    BIO *b64,*bmem;
    b64=BIO_new(BIO_f_base64());
    bmem=BIO_new_mem_buf(str,str_len);
    bmem=BIO_push(b64,bmem);
    len=BIO_read(bmem,decode,str_len);
    decode[len]=0;
    BIO_free_all(bmem);
    return 0;
}

#ifdef  __cplusplus
}
#endif
