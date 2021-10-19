package com.github.firmwehr.gentle.parser;

import java.io.*;
import java.util.HashMap;
import java.util.Optional;

public class Lexer {
    public static void main(String args[]) throws FileNotFoundException, IOException {
        String path = "src/test/resources/Collatz.java";
        BufferedReader input = new BufferedReader(new FileReader(path));
        Lexer l = new Lexer(input);

        Token t;
        while ((t = l.lex()).type() != TokenType.EOF) {
            System.out.println(t);
        }
    }

    private static final HashMap<String, TokenType> KEYWORDS = new HashMap<>();
    private static final SymbolTrie<TokenType> SYMBOLS = new SymbolTrie<>();
    static {
        // TODO: Complete this
        Lexer.KEYWORDS.put("abstract", TokenType.ABSTRACT);
        Lexer.KEYWORDS.put("assert", TokenType.ASSERT);
        Lexer.KEYWORDS.put("boolean", TokenType.BOOLEAN);
        Lexer.KEYWORDS.put("new", TokenType.NEW);

        Lexer.SYMBOLS.put("!=", TokenType.NOT_EQUALS);
        Lexer.SYMBOLS.put("!", TokenType.LOGICAL_NOT);
        Lexer.SYMBOLS.put("(", TokenType.LEFT_PAREN);
        Lexer.SYMBOLS.put(")", TokenType.RIGHT_PAREN);
        Lexer.SYMBOLS.put("*=", TokenType.ASSIGN_MULTIPLY);
        Lexer.SYMBOLS.put("*", TokenType.MULTIPLY);
        Lexer.SYMBOLS.put("++", TokenType.POSTFIX_INCREMENT);
        Lexer.SYMBOLS.put("+=", TokenType.ASSIGN_ADD);
        Lexer.SYMBOLS.put("+", TokenType.ADD);
        Lexer.SYMBOLS.put(",", TokenType.COMMA);
        Lexer.SYMBOLS.put("-=", TokenType.ASSIGN_SUBTRACT);
        Lexer.SYMBOLS.put("--", TokenType.POSTFIX_DECREMENT);
        Lexer.SYMBOLS.put("-", TokenType.SUBTRACT);
        Lexer.SYMBOLS.put(".", TokenType.DOT);
        // There is an impostor among us: This "symbol" needs to be handled seperately
        Lexer.SYMBOLS.put("/*", TokenType.COMMENT);
        Lexer.SYMBOLS.put("/=", TokenType.ASSIGN_DIVIDE);
        Lexer.SYMBOLS.put("/", TokenType.DIVIDE);
        Lexer.SYMBOLS.put(":", TokenType.COLON);
        Lexer.SYMBOLS.put(";", TokenType.SEMICOLON);
        Lexer.SYMBOLS.put("<<=", TokenType.ASSIGN_BITSHIFT_LEFT);
        Lexer.SYMBOLS.put("<<", TokenType.BITSHIFT_LEFT);
        Lexer.SYMBOLS.put("<=", TokenType.LESS_THAN);
        Lexer.SYMBOLS.put("<", TokenType.LESS_THAN_EQUALS);
        Lexer.SYMBOLS.put("==", TokenType.EQUALS);
        Lexer.SYMBOLS.put("=", TokenType.ASSIGN);
        Lexer.SYMBOLS.put(">=", TokenType.GREATER_THAN_EQUALS);
        Lexer.SYMBOLS.put(">>=", TokenType.ASSIGN_BITSHIFT_RIGHT);
        Lexer.SYMBOLS.put(">>>=", TokenType.ASSIGN_BITSHIFT_RIGHT_ZEROFILL);
        Lexer.SYMBOLS.put(">>>", TokenType.BITSHIFT_RIGHT_ZEROFILL);
        Lexer.SYMBOLS.put(">>", TokenType.BITSHIFT_RIGHT);
        Lexer.SYMBOLS.put(">", TokenType.GREATER_THAN);
        Lexer.SYMBOLS.put("?", TokenType.QUESTION_MARK);
        Lexer.SYMBOLS.put("%=", TokenType.ASSIGN_MODULO);
        Lexer.SYMBOLS.put("%", TokenType.MODULO);
        Lexer.SYMBOLS.put("&=", TokenType.ASSIGN_BITWISE_AND);
        Lexer.SYMBOLS.put("&&", TokenType.LOGICAL_AND);
        Lexer.SYMBOLS.put("&", TokenType.BITWISE_AND);
        Lexer.SYMBOLS.put("[", TokenType.LEFT_BRACKET);
        Lexer.SYMBOLS.put("]", TokenType.RIGHT_BRACKET);
        Lexer.SYMBOLS.put("^=", TokenType.ASSIGN_BITWISE_XOR);
        Lexer.SYMBOLS.put("^", TokenType.BITWISE_XOR);
        Lexer.SYMBOLS.put("{", TokenType.LEFT_BRACE);
        Lexer.SYMBOLS.put("}", TokenType.RIGHT_BRACE);
        Lexer.SYMBOLS.put("~", TokenType.BITWISE_NOT);
        Lexer.SYMBOLS.put("|=", TokenType.ASSIGN_BITWISE_OR);
        Lexer.SYMBOLS.put("||", TokenType.LOGICAL_OR);
        Lexer.SYMBOLS.put("|", TokenType.BITWISE_OR);
    };

    private final BufferedReader input;
    private final StringBuilder sb;

    private int lookahead;

    // line and column describe the position of the character in lookahead
    private int line;
    private int column;

    public Lexer(BufferedReader input) throws IOException {
        this.input = input;

        this.sb = new StringBuilder();
        this.lookahead = this.input.read();
        this.line = 0;
        this.column = 0;
    }

    public Token lex() throws IOException {
        this.resetSb();

        if (this.lookahead == -1) {
            return this.emit(TokenType.EOF);
        }

        if (Lexer.isWhitespace(this.lookahead)) {
            return this.lexWhitespace();
        }

        if (Lexer.isIdentifierStart(this.lookahead)) {
            return this.lexIdentifierOrKeyword();
        }

        if (Lexer.isDigit(this.lookahead)) {
            return this.lexIntegerLiteral();
        }

        return this.lexSymbolCommentOrError();
    }

    private Token lexWhitespace() throws IOException {
        while (Lexer.isWhitespace(this.lookahead)) {
            this.consume();
        }
        return this.emit(TokenType.WHITESPACE);
    }

    private Token lexIdentifierOrKeyword() throws IOException {
        while (Lexer.isIdentifierStart(this.lookahead) || Lexer.isDigit(this.lookahead)) {
            this.consume();
        }

        // TODO: Add guard for longest keyword length (otherwise this branch will never be taken)
        if (Lexer.KEYWORDS.containsKey(this.sb.toString())) {
            return this.emit(this.KEYWORDS.get(this.sb.toString()));
        }

        return this.emit(TokenType.IDENTIFIER);
    }

    private Token lexIntegerLiteral() throws IOException {
        while (Lexer.isDigit(this.lookahead)) {
            this.consume();
        }
        return this.emit(TokenType.INTEGER_LITERAL);
    }

    private Token lexSymbolCommentOrError() throws IOException {
        // Greedily read the next symbol using the symbol trie
        SymbolTrie.STNode<TokenType> node = Lexer.SYMBOLS.getRoot();
        while (node.getChildren().containsKey((char) this.lookahead)) {
            node = node.getChildren().get((char) this.lookahead);
            this.consume();
        }

        // Still at the root node, no symbol was read
        if (node.getContent().isEmpty()) {
            throw new RuntimeException("Symbol or comment expected");
        }
        // Comment "operator", special case
        if (node.getContent().get().equals(TokenType.COMMENT)) {
            return this.lexCommentRest();
        }
        // Otherwise, the node contains a valid symbol
        return this.emit(node.getContent().get());
    }

    private Token lexCommentRest() throws IOException {
        while (true) {
            switch (this.lookahead) {
                case '*':
                    this.consume();
                    switch (this.lookahead) {
                        case '/':
                            this.consume();
                            return this.emit(TokenType.COMMENT);
                        default:
                            // **Don't consume next token, restart loop**
                            continue;
                    }
                case -1:
                    throw new RuntimeException("Unterminated comment");
            }
            this.consume();
        }
    }

    // Helper methods for lex* methods

    private Token emit(TokenType type) {
        return new Token(type, this.sb.toString());
    }

    // TODO: This method is called whenever a new token is started
    // TODO: Remember the position so it can be passed to the new token in emit()
    private void resetSb() {
        this.sb.setLength(0);
    }

    private void consume() throws IOException {
        this.sb.append((char) this.lookahead);
        this.lookahead = this.input.read();
    }

    // Character classification functions

    private static boolean isWhitespace(int c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    private static boolean isIdentifierStart(int c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private static boolean isDigit(int c) {
        return c >= '0' && c <= '9';
    }
}