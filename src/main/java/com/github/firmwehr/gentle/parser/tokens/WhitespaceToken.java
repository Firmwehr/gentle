package com.github.firmwehr.gentle.parser.tokens;

import com.github.firmwehr.gentle.source.SourceSpan;

public record WhitespaceToken(SourceSpan sourceSpan) implements Token {
}
