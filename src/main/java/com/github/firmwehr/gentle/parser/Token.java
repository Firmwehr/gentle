package com.github.firmwehr.gentle.parser;

// Invariants:
// type == INTEGER_LITERAL ==> parseInt(text) doesn't throw
public record Token(TokenType type, String text) {
}