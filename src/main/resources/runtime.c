#include <stdio.h>
#include <inttypes.h>
#include <stdlib.h>

void* allocate() {
    size_t nmemb;
    size_t memberSize;
    __asm__ __volatile__("movq 16(%%rbp), %0\n\t" : "=r" (nmemb));
    __asm__ __volatile__("movq 24(%%rbp), %0\n\t" : "=r" (memberSize));
    // allocate should never return NULL if there is memory left to allocate
    // Therefore we always allocate at least one byte using calloc(1, 1).
    return calloc(nmemb == 0 ? 1 : nmemb, memberSize == 0 ? 1 : memberSize);
}

void println() {
    int64_t value;
    __asm__ __volatile__("movq 16(%%rbp), %0\n\t" : "=r" (value));
    printf("%d\n", value);
}

void write() {
    int64_t value;
    __asm__ __volatile__("movq 16(%%rbp), %0\n\t" : "=r" (value));
    putchar(value);
}

void flush() {
    fflush(stdout);
}
