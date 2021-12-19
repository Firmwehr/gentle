package com.github.firmwehr.gentle.asciiart.parsing;

import com.github.firmwehr.gentle.asciiart.parsing.filter.ModeFilter;
import com.github.firmwehr.gentle.lexer.StringReader;
import com.github.firmwehr.gentle.util.Pair;
import firm.nodes.Add;
import firm.nodes.Address;
import firm.nodes.And;
import firm.nodes.Block;
import firm.nodes.Call;
import firm.nodes.Cmp;
import firm.nodes.Cond;
import firm.nodes.Const;
import firm.nodes.Conv;
import firm.nodes.Div;
import firm.nodes.Dummy;
import firm.nodes.End;
import firm.nodes.Eor;
import firm.nodes.IJmp;
import firm.nodes.Jmp;
import firm.nodes.Load;
import firm.nodes.Member;
import firm.nodes.Minus;
import firm.nodes.Mod;
import firm.nodes.Mul;
import firm.nodes.Mulh;
import firm.nodes.Node;
import firm.nodes.Not;
import firm.nodes.Offset;
import firm.nodes.Or;
import firm.nodes.Phi;
import firm.nodes.Proj;
import firm.nodes.Return;
import firm.nodes.Shl;
import firm.nodes.Shr;
import firm.nodes.Shrs;
import firm.nodes.Size;
import firm.nodes.Start;
import firm.nodes.Store;
import firm.nodes.Sub;
import spoon.reflect.code.CtCodeSnippetExpression;
import spoon.reflect.code.CtExpression;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;

import java.util.Locale;
import java.util.Map;

import static java.util.function.Predicate.not;

public class NodeFilterParser {
	private static final Map<String, Class<? extends Node>> NORMAL_NODES =
		Map.ofEntries(Map.entry("add", Add.class), Map.entry("address", Address.class), Map.entry("and", And.class),
			Map.entry("block", Block.class), Map.entry("call", Call.class), Map.entry("cond", Cond.class),
			Map.entry("conv", Conv.class), Map.entry("div", Div.class), Map.entry("dummy", Dummy.class),
			Map.entry("end", End.class), Map.entry("eor", Eor.class), Map.entry("ijmp", IJmp.class),
			Map.entry("jmp", Jmp.class), Map.entry("load", Load.class), Map.entry("member", Member.class),
			Map.entry("minus", Minus.class), Map.entry("mod", Mod.class), Map.entry("mul", Mul.class),
			Map.entry("mulh", Mulh.class), Map.entry("not", Not.class), Map.entry("offset", Offset.class),
			Map.entry("or", Or.class), Map.entry("return", Return.class), Map.entry("shl", Shl.class),
			Map.entry("shr", Shr.class), Map.entry("shrs", Shrs.class), Map.entry("size", Size.class),
			Map.entry("start", Start.class), Map.entry("store", Store.class), Map.entry("sub", Sub.class));

	private final Factory factory;

	public NodeFilterParser(Factory factory) {
		this.factory = factory;
	}

	private Pair<CtExpression<?>, CtTypeReference<?>> parseNodeFilter(StringReader reader) {
		reader.readWhitespace();

		String name = reader.readWhile(not(Character::isWhitespace)).toLowerCase(Locale.ROOT);
		if (NORMAL_NODES.containsKey(name)) {
			CtCodeSnippetExpression<?> filter = factory.createCodeSnippetExpression("""
				new com.github.firmwehr.gentle.asciiart.parsing.filter.ClassFilter(
				    %s.class
				)
				""".formatted(NORMAL_NODES.get(name).getName()));
			return new Pair<>(filter, factory.Class().get(NORMAL_NODES.get(name)).getReference());
		}
		String argument = reader.readWhile(c -> c != ';').strip();
		switch (name) {
			case "cmp" -> {
				CtExpression<?> filter;
				if (argument.isEmpty()) {
					filter = factory.createCodeSnippetExpression("""
						new com.github.firmwehr.gentle.asciiart.parsing.filter.CmpFilter(
						    java.util.Optional.empty()
						)
						""");
				} else {
					filter = factory.createCodeSnippetExpression("""
						new com.github.firmwehr.gentle.asciiart.parsing.filter.CmpFilter(
						    java.util.Optional.of(firm.Relation.valueOf("%s"))
						)
						""".formatted(argument));
				}
				return new Pair<>(filter, factory.Class().get(Cmp.class).getReference());
			}
			case "const" -> {
				CtExpression<?> filter;
				if (argument.isEmpty()) {
					filter = factory.createCodeSnippetExpression("""
						new com.github.firmwehr.gentle.asciiart.parsing.filter.ConstFilter(
						    java.util.OptionalLong.empty()
						)
						""");
				} else {
					filter = factory.createCodeSnippetExpression("""
						new com.github.firmwehr.gentle.asciiart.parsing.filter.ConstFilter(
						    java.util.OptionalLong.of(%s)
						)
						""".formatted(argument));
				}
				return new Pair<>(filter, factory.Class().get(Const.class).getReference());
			}
			case "phi" -> {
				CtExpression<?> filter;

				if (argument.isEmpty()) {
					filter = factory.createCodeSnippetExpression("""
						new com.github.firmwehr.gentle.asciiart.parsing.filter.PhiFilter(
						    java.util.Optional.empty()
						)
						""");
				} else {
					filter = factory.createCodeSnippetExpression("""
						new com.github.firmwehr.gentle.asciiart.parsing.filter.PhiFilter(
						    java.util.Optional.of(%s)
						)
						""".formatted(argument.equals("+loop") ? "true" : "false"));
				}
				return new Pair<>(filter, factory.Class().get(Phi.class).getReference());
			}
			case "proj" -> {
				CtExpression<?> filter;

				if (argument.isEmpty()) {
					filter = factory.createCodeSnippetExpression("""
						new com.github.firmwehr.gentle.asciiart.parsing.filter.ProjFilter(
						    java.util.OptionalInt.empty()
						)
						""");
				} else {
					filter = factory.createCodeSnippetExpression("""
						new com.github.firmwehr.gentle.asciiart.parsing.filter.ProjFilter(
						    java.util.OptionalInt.of(%s)
						)
						""".formatted(argument));
				}
				return new Pair<>(filter, factory.Class().get(Proj.class).getReference());
			}
			case "*" -> {
				CtExpression<?> filter = factory.createCodeSnippetExpression("""
					new com.github.firmwehr.gentle.asciiart.parsing.filter.ClassFilter(
					    %s
					)
					""".formatted("firm.nodes.Node.class"));
				return new Pair<>(filter, factory.Class().get(Node.class).getReference());
			}
		}

		throw new IllegalArgumentException("Unknown node type '" + name + "'");
	}

	private CtExpression<?> parseMode(StringReader reader, CtExpression<?> underlying) {
		String lambda;
		if (reader.peekIgnoreCase("+memory")) {
			lambda = "mode.equals(firm.Mode.getM())";
		} else if (reader.peekIgnoreCase("-memory")) {
			lambda = "!mode.equals(firm.Mode.getM())";
		} else if (reader.peekIgnoreCase("Ls")) {
			lambda = "mode.equals(firm.Mode.getLs())";
		} else if (reader.peekIgnoreCase("P")) {
			lambda = "mode.equals(firm.Mode.getP())";
		} else if (reader.peekIgnoreCase("Is")) {
			lambda = "mode.equals(firm.Mode.getIs())";
		} else if (reader.peekIgnoreCase("Bu")) {
			lambda = "mode.equals(firm.Mode.getBu())";
		} else {
			lambda = "true";
		}

		return factory.createConstructorCall(factory.Class().get(ModeFilter.class).getReference(),
			factory.createCodeSnippetExpression("mode -> %s".formatted(lambda)), underlying);
	}

	public Pair<CtExpression<?>, CtTypeReference<?>> parseFilter(StringReader reader) {
		Pair<CtExpression<?>, CtTypeReference<?>> pair = parseNodeFilter(reader);
		CtExpression<?> filter = pair.first();

		if (reader.canRead()) {
			reader.readWhitespace();
			if (reader.readChar() != ';') {
				throw new IllegalArgumentException("Expected ';' as separator after node type");
			}
			reader.readWhitespace();
		}

		if (reader.canRead()) {
			filter = parseMode(reader, filter);
			reader.readWhitespace();
		}

		return new Pair<>(filter, pair.second());
	}
}
