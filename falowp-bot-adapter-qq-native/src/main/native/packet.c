#define _GNU_SOURCE
#include <node_api.h>
#include <stdint.h>
#include <stdbool.h>
#include <stdlib.h>
#include <string.h>
#include <dlfcn.h>
#include <link.h>
#include <sys/mman.h>
#include <unistd.h>

/* NTQQ 3.2.34-53644 converts Buffer through ToString(), corrupting protobuf.
 * Preserve bytes at that conversion boundary and return the SSO rsp as Buffer.
 * No network interception, QQ authentication changes or third-party binary.
 * Offsets and prologues are tied to the SHA-256-pinned client installation.
 */
#if defined(__aarch64__)
#define CONVERT_OFFSET 0x4aaba5c
#define RESPONSE_RETURN 0x182c65c
#define UTF8_GOT 0x8c01720
static const unsigned char prologue[] = {0xff,0x43,0x01,0xd1,0xfd,0x7b,0x02,0xa9,0xf5,0x1b,0x00,0xf9,0xf4,0x4f,0x04,0xa9};
#elif defined(__x86_64__)
#define CONVERT_OFFSET 0x6e5ab20
#define RESPONSE_RETURN 0x3a12e05
#define UTF8_GOT 0x90faa98
static const unsigned char prologue[] = {0x41,0x57,0x41,0x56,0x53,0x48,0x83,0xec,0x30,0x49,0x89,0xd7,0x49,0x89,0xf6,0x48,0x89,0xfb};
#else
#error Unsupported NTQQ architecture
#endif

/* The vendor's libc++ string ABI has a 22-byte inline representation. */
typedef struct { uintptr_t words[3]; } native_string;
typedef native_string (*convert_fn)(napi_env,napi_value);
static convert_fn original_convert;
static uintptr_t base;
static napi_status (*original_utf8)(napi_env,const char*,size_t,napi_value*);

static native_string convert(napi_env env,napi_value value) {
    bool buffer=false;
    if (napi_is_buffer(env,value,&buffer)!=napi_ok || !buffer) return original_convert(env,value);
    void *data=NULL; size_t len=0;
    native_string out={{0,0,0}};
    if (napi_get_buffer_info(env,value,&data,&len)!=napi_ok) return out;
    if (len<=22) {
        ((unsigned char*)&out)[0]=(unsigned char)(len*2);
        memcpy((char*)&out+1,data,len);
    } else {
        if (len>SIZE_MAX-16) {napi_throw_range_error(env,NULL,"NTQQ packet too large");return out;}
        size_t capacity=(len+16)&~(size_t)15;
        char *p=malloc(capacity);
        if (!p) {napi_throw_error(env,NULL,"NTQQ packet allocation failed");return out;}
        memcpy(p,data,len);p[len]=0;
        out.words[0]=capacity|1;out.words[1]=len;out.words[2]=(uintptr_t)p;
    }
    return out;
}
static napi_status utf8(napi_env env,const char *str,size_t len,napi_value *out) {
    if ((uintptr_t)__builtin_return_address(0)==base+RESPONSE_RETURN)
        return napi_create_buffer_copy(env,len,str,NULL,out);
    return original_utf8(env,str,len,out);
}
static int protect(void *p,size_t length,int flags) {
    size_t page=(size_t)sysconf(_SC_PAGESIZE);
    uintptr_t lo=(uintptr_t)p&~(page-1);
    return mprotect((void*)lo,((uintptr_t)p+length-lo+page-1)&~(page-1),flags);
}
static void jump(void *dst,void *target) {
#if defined(__aarch64__)
    const uint32_t op[]={0x58000050,0xd61f0200};
    memcpy(dst,op,8);memcpy((char*)dst+8,&target,8);
    __builtin___clear_cache(dst,(char*)dst+16);
#else
    const unsigned char op[]={0xff,0x25,0,0,0,0};
    memcpy(dst,op,6);memcpy((char*)dst+6,&target,8);
#endif
}
static int locate(struct dl_phdr_info *info,size_t size,void *data) {
    (void)size;(void)data;
    const char *name=strrchr(info->dlpi_name,'/');
    if(name && !strcmp(name,"/wrapper.node")) {base=info->dlpi_addr;return 1;}
    return 0;
}
static napi_value init(napi_env env,napi_value exports) {
    dl_iterate_phdr(locate,NULL);
    if(!base) {napi_throw_error(env,NULL,"Load NTQQ before its packet bridge");return NULL;}
    void *entry=(void*)(base+CONVERT_OFFSET);
    if(memcmp(entry,prologue,sizeof(prologue))) {
        napi_throw_error(env,NULL,"Unsupported NTQQ packet ABI; expected 3.2.34-53644");return NULL;
    }
    original_utf8=dlsym(RTLD_DEFAULT,"napi_create_string_utf8");
    if(!original_utf8) {napi_throw_error(env,NULL,"N-API UTF-8 conversion is unavailable");return NULL;}
    size_t size=sizeof(prologue);
    void *trampoline=mmap(NULL,size+16,PROT_READ|PROT_WRITE,MAP_PRIVATE|MAP_ANONYMOUS,-1,0);
    if(trampoline==MAP_FAILED) {napi_throw_error(env,NULL,"Cannot allocate NTQQ packet bridge");return NULL;}
    memcpy(trampoline,entry,size);jump((char*)trampoline+size,(char*)entry+size);
    if(protect(trampoline,size+16,PROT_READ|PROT_EXEC)) {
        munmap(trampoline,size+16);napi_throw_error(env,NULL,"Cannot activate NTQQ packet bridge");return NULL;
    }
    original_convert=(convert_fn)trampoline;
    void **got=(void**)(base+UTF8_GOT);
    if(protect(got,sizeof(void*),PROT_READ|PROT_WRITE) || protect(entry,size,PROT_READ|PROT_WRITE|PROT_EXEC)) {
        napi_throw_error(env,NULL,"Cannot update NTQQ packet conversion");return NULL;
    }
    *got=(void*)utf8;
    jump(entry,(void*)convert);
    protect(entry,size,PROT_READ|PROT_EXEC);
    return exports;
}
NAPI_MODULE(NODE_GYP_MODULE_NAME,init)
