package com.github.firmwehr.gentle.parser.tokens;

import com.github.firmwehr.gentle.parser.source.SourceSpan;

public record WhitespaceToken(SourceSpan sourceSpan) implements Token {
}
