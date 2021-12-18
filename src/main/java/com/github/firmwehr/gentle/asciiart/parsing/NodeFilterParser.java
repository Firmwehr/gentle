package com.github.firmwehr.gentle.asciiart.parsing;

import com.github.firmwehr.gentle.asciiart.parsing.filter.ClassFilter;
import com.github.firmwehr.gentle.asciiart.parsing.filter.CmpFilter;
import com.github.firmwehr.gentle.asciiart.parsing.filter.ConstFilter;
import com.github.firmwehr.gentle.asciiart.parsing.filter.ModeFilter;
import com.github.firmwehr.gentle.asciiart.parsing.filter.NodeFilter;
import com.github.firmwehr.gentle.asciiart.parsing.filter.PhiFilter;
import com.github.firmwehr.gentle.asciiart.parsing.filter.ProjFilter;
import com.github.firmwehr.gentle.lexer.StringReader;
import firm.Mode;
import firm.Relation;
import firm.nodes.Add;
import firm.nodes.Address;
import firm.nodes.And;
import firm.nodes.Block;
import firm.nodes.Call;
import firm.nodes.Cond;
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
import firm.nodes.Return;
import firm.nodes.Shl;
import firm.nodes.Shr;
import firm.nodes.Shrs;
import firm.nodes.Size;
import firm.nodes.Start;
import firm.nodes.Store;
import firm.nodes.Sub;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

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


	private NodeFilter<?> parseNodeFilter(StringReader reader) {
		String name = reader.readWhile(not(Character::isWhitespace)).toLowerCase(Locale.ROOT);
		if (NORMAL_NODES.containsKey(name)) {
			return new ClassFilter<>(NORMAL_NODES.get(name));
		}
		String argument = reader.readWhile(c -> c != ';').strip();
		switch (name) {
			case "cmp" -> {
				if (argument.isEmpty()) {
					return new CmpFilter(Optional.empty());
				}
				return new CmpFilter(Optional.of(Relation.valueOf(argument)));
			}
			case "const" -> {
				if (argument.isEmpty()) {
					return new ConstFilter(OptionalLong.empty());
				}
				return new ConstFilter(OptionalLong.of(Long.parseLong(argument)));
			}
			case "phi" -> {
				if (argument.isEmpty()) {
					return new PhiFilter(Optional.empty());
				}
				return new PhiFilter(Optional.of(argument.equals("+loop")));
			}
			case "proj" -> {
				if (argument.isEmpty()) {
					return new ProjFilter(OptionalInt.empty());
				}
				return new ProjFilter(OptionalInt.of(Integer.parseInt(argument)));
			}
			case "*" -> {
				return new ClassFilter<>(Node.class);
			}
		}

		throw new IllegalArgumentException("Unknown node type '" + name + "'");
	}

	private <T extends Node> NodeFilter<T> parseMode(StringReader reader, NodeFilter<T> underlying) {
		if (reader.peekIgnoreCase("+memory")) {
			return new ModeFilter<>(mode -> mode.equals(Mode.getM()), underlying);
		}
		if (reader.peekIgnoreCase("-memory")) {
			return new ModeFilter<>(mode -> !mode.equals(Mode.getM()), underlying);
		}
		if (reader.peekIgnoreCase("Ls")) {
			return new ModeFilter<>(mode -> mode.equals(Mode.getLs()), underlying);
		}
		if (reader.peekIgnoreCase("P")) {
			return new ModeFilter<>(mode -> mode.equals(Mode.getP()), underlying);
		}
		if (reader.peekIgnoreCase("Is")) {
			return new ModeFilter<>(mode -> mode.equals(Mode.getIs()), underlying);
		}
		if (reader.peekIgnoreCase("Bu")) {
			return new ModeFilter<>(mode -> mode.equals(Mode.getBu()), underlying);
		}

		return new ModeFilter<>(ignored -> true, underlying);
	}

	public <T extends Node> NodeFilter<T> parseFilter(StringReader reader) {
		@SuppressWarnings("unchecked") //
		NodeFilter<T> filter = (NodeFilter<T>) parseNodeFilter(reader);

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

		return filter;
	}
}
