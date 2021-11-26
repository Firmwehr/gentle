#include <stdio.h>
#include <inttypes.h>
#include <stdlib.h>

void* allocate(size_t nmemb, size_t memberSize) {
    return calloc(nmemb, memberSize == 0 ? 1 : memberSize);
}

void println(int input) {
    printf("%d\n", input);
}
