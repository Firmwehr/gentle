#include <stdio.h>
#include <inttypes.h>
#include <stdlib.h>

void* allocate(size_t nmemb, size_t memberSize) {
    // allocate should never return NULL if there is memory left to allocate
    // Therefore we always allocate at least one byte using calloc(1, 1).
    return calloc(nmemb == 0 ? 1 : nmemb, memberSize == 0 ? 1 : memberSize);
}

void println(int input) {
    printf("%d\n", input);
}

void flush() {
    fflush(stdout);
}
