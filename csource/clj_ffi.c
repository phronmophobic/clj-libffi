#include <ffi.h>
#include <dlfcn.h>
#include <stdio.h>
#import "clj_ffi.h"

/* typedef int (*compress_t)(unsigned char*, size_t*, const unsigned char*, size_t); */

typedef double (*cos_t)(double x);

ffi_type* argtype_to_ffi_type(int argtype){
    ffi_type* ret;

    switch (argtype){
    case 0: ret = &ffi_type_void; break;
    case 1: ret = &ffi_type_pointer; break;
    case 2: ret = &ffi_type_sint8; break;
    case 3: ret = &ffi_type_sint16; break;
    case 4: ret = &ffi_type_sint32; break;
    case 5: ret = &ffi_type_sint64; break;
    case 6: ret = &ffi_type_float; break;
    case 7: ret = &ffi_type_double; break;
    default: ret = &ffi_type_void;
    }

    return ret;
}


void callc(void* fptr, int rettype, void* ret, int nargs, int* argtypes, void** values){

    ffi_type* ffi_argtypes[nargs];
    for ( int i = 0; i < nargs; i ++){
        ffi_argtypes[i] = argtype_to_ffi_type(argtypes[i]);
    }

    
    ffi_cif cif;
    ffi_status status = ffi_prep_cif(&cif, FFI_DEFAULT_ABI, nargs, argtype_to_ffi_type(rettype),
                                     ffi_argtypes);

    
    

    ffi_call(&cif, FFI_FN(fptr), ret, values);


}


#ifdef TEST

int main(){

    /* void *zlib = dlopen("/opt/local/lib/libz.dylib",RTLD_LAZY); */

    /* printf("zlib : %p\n", zlib); */

    cos_t cos = dlsym(RTLD_DEFAULT, "cos");

    printf("cos : %p\n", cos);

    double result = cos(42.0);

    printf("result: %f %lu \n", result, sizeof(result));

    return 0;

}


#endif
