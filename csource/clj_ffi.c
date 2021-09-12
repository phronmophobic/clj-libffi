#include <ffi.h>

#ifdef TEST

#include <dlfcn.h>
#include <stdio.h>

#endif

#import "clj_ffi.h"



ffi_type* argtype_to_ffi_type(int argtype){
    ffi_type* ret;

    switch (argtype){
    case 1: ret = &ffi_type_complex_double; break;
    case 2: ret = &ffi_type_complex_float; break;
    case 3: ret = &ffi_type_double; break;
    case 4: ret = &ffi_type_float; break;
    case 5: ret = &ffi_type_pointer; break;
    case 6: ret = &ffi_type_sint16; break;
    case 7: ret = &ffi_type_sint32; break;
    case 8: ret = &ffi_type_sint64; break;
    case 9: ret = &ffi_type_sint8; break;
    case 10: ret = &ffi_type_uint16; break;
    case 11: ret = &ffi_type_uint32; break;
    case 12: ret = &ffi_type_uint64; break;
    case 13: ret = &ffi_type_uint8; break;
    case 14: ret = &ffi_type_void; break;
    default: ret = &ffi_type_void;

    }

    return ret;
}

#ifdef TEST
/* typedef int (*compress_t)(unsigned char*, size_t*, const unsigned char*, size_t); */
typedef double (*cos_t)(double x);

int main(){

    /* void *zlib = dlopen("/opt/local/lib/libz.dylib",RTLD_LAZY); */

    /* printf("zlib : %p\n", zlib); */

    cos_t cos = dlsym(RTLD_DEFAULT, "cos");

    printf("cos : %p\n", cos);

    double result = cos(42.0);

    printf("result: %f %lu \n", result, sizeof(result));

    printf("cif_size: %lu\n", sizeof(ffi_cif));

    printf("default abi: %d, %lu\n", FFI_DEFAULT_ABI, sizeof(FFI_DEFAULT_ABI));

    return 0;

}


#endif
