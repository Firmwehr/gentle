package com.github.firmwehr.gentle.parser.tokens;

/**
 * All keywords from the MiniJava language report.
 */
public enum Keyword {
	ABSTRACT("abstract"),
	ASSERT("assert"),
	BOOLEAN("boolean"),
	BREAK("break"),
	BYTE("byte"),
	CASE("case"),
	CATCH("catch"),
	CHAR("char"),
	CLASS("class"),
	CONST("const"),
	CONTINUE("continue"),
	DEFAULT("default"),
	DOUBLE("double"),
	DO("do"),
	ELSE("else"),
	ENUM("enum"),
	EXTENDS("extends"),
	FALSE("false"),
	FINALLY("finally"),
	FINAL("final"),
	FLOAT("float"),
	FOR("for"),
	GOTO("goto"),
	IF("if"),
	IMPLEMENTS("implements"),
	IMPORT("import"),
	INSTANCEOF("instanceof"),
	INTERFACE("interface"),
	INT("int"),
	LONG("long"),
	NATIVE("native"),
	NEW("new"),
	NULL("null"),
	PACKAGE("package"),
	PRIVATE("private"),
	PROTECTED("protected"),
	PUBLIC("public"),
	RETURN("return"),
	SHORT("short"),
	STATIC("static"),
	STRICTFP("strictfp"),
	SUPER("super"),
	SWITCH("switch"),
	SYNCHRONIZED("synchronized"),
	THIS("this"),
	THROWS("throws"),
	THROW("throw"),
	TRANSIENT("transient"),
	TRUE("true"),
	TRY("try"),
	VOID("void"),
	VOLATILE("volatile"),
	WHILE("while"),
	;

	private final String name;

	Keyword(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
