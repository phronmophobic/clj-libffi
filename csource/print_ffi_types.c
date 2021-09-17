#include <ffi.h>
#include <stdio.h>

void print_ffi_type2(const char* name, ffi_type t){
    printf("------------------\n");
    printf("%s:\n", name);
    printf("  size      : %zu\n", t.size);
    printf("  alignment : %u\n", t.alignment);
    printf("  type      : %u\n", t.type);
    printf("  elements  : %p\n", t.elements);
}

void print_ffi_type(const char* name, ffi_type t){
    printf("[\"%s\", %zu, %u, %u, %p]\n", name, t.size, t.alignment, t.type, t.elements);
}

int main(){



    print_ffi_type("ffi_type_complex_double", ffi_type_complex_double);
    print_ffi_type("ffi_type_complex_float", ffi_type_complex_float);
    print_ffi_type("ffi_type_double", ffi_type_double);
    print_ffi_type("ffi_type_float", ffi_type_float);
    print_ffi_type("ffi_type_pointer", ffi_type_pointer);
    print_ffi_type("ffi_type_sint16", ffi_type_sint16);
    print_ffi_type("ffi_type_sint32", ffi_type_sint32);
    print_ffi_type("ffi_type_sint64", ffi_type_sint64);
    print_ffi_type("ffi_type_sint8", ffi_type_sint8);
    print_ffi_type("ffi_type_uint16", ffi_type_uint16);
    print_ffi_type("ffi_type_uint32", ffi_type_uint32);
    print_ffi_type("ffi_type_uint64", ffi_type_uint64);
    print_ffi_type("ffi_type_uint8", ffi_type_uint8);
    print_ffi_type("ffi_type_void", ffi_type_void);
}
