package com.github.firmwehr.gentle.asciiart.parsing;

import com.github.firmwehr.gentle.asciiart.elements.AsciiBox;
import com.github.firmwehr.gentle.asciiart.elements.AsciiElement;
import com.github.firmwehr.gentle.asciiart.elements.AsciiMergeNode;
import com.github.firmwehr.gentle.asciiart.parsing.filter.NodeFilter;
import com.github.firmwehr.gentle.asciiart.parsing.filter.WithInputsOrderedFilter;
import com.github.firmwehr.gentle.asciiart.parsing.filter.WithInputsUnorderedFilter;
import com.github.firmwehr.gentle.asciiart.util.Connection;
import com.github.firmwehr.gentle.lexer.StringReader;
import com.github.firmwehr.gentle.source.Source;
import com.github.firmwehr.gentle.util.Pair;
import spoon.FluentLauncher;
import spoon.Launcher;
import spoon.compiler.Environment;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtExpression;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.DefaultJavaPrettyPrinter;
import spoon.reflect.visitor.ForceImportProcessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class NodeDeclarationParser {

	private final Factory factory;
	private final CtClass<?> matchClass;
	private final CtClass<?> filterClass;
	private final Map<AsciiBox, AsciiBoxInformation> localFilters;

	public NodeDeclarationParser() {
		Launcher launcher = new Launcher();
		launcher.getEnvironment().setAutoImports(true);
		this.factory = launcher.getFactory();
		this.localFilters = new HashMap<>();

		this.filterClass = factory.Class().create("com.github.firmwehr.gentle.generated.FooBar");
		this.matchClass = factory.Class().create("com.github.firmwehr.gentle.generated.FooBar$Match");

		this.filterClass.addNestedType(this.matchClass);
	}

	private void generateMatchClass(AsciiElement baseElement, Set<AsciiElement> visited) {
		if (!visited.add(baseElement)) {
			return;
		}

		switch (baseElement) {
			case AsciiBox box -> {
				setFieldAndParseFilter(box);

				box.ins().forEach(it -> generateMatchClass(it.start(), visited));
				box.outs().forEach(it -> generateMatchClass(it.end(), visited));
			}
			case AsciiMergeNode node -> {
				generateMatchClass(node.out().end(), visited);
				node.in().forEach(it -> generateMatchClass(it.start(), visited));
			}
		}
	}

	private void setFieldAndParseFilter(AsciiBox box) {
		String input = String.join(" ", box.lines());
		StringReader reader = new StringReader(new Source(input));

		String name = reader.readWhile(c -> c != ':').strip();
		reader.readChar();

		String filterString = reader.readWhile(c -> true);

		Pair<CtExpression<?>, CtTypeReference<?>> pair =
			new NodeFilterParser(factory).parseFilter(new StringReader(new Source(filterString)));
		CtExpression<Object> filter = (CtExpression<Object>) pair.first();
		localFilters.put(box, new AsciiBoxInformation(name, filter));

		CtTypeReference<?> fieldType = pair.second();
		EnumSet<ModifierKind> modifiers = EnumSet.of(ModifierKind.PUBLIC, ModifierKind.FINAL);
		CtField<?> field = factory.Field().create(matchClass, modifiers, fieldType, name);
		matchClass.addField(field);
	}

	private CtExpression<?> buildCompositeFilter() {
		Set<AsciiBox> work = new HashSet<>(localFilters.keySet());
		Set<AsciiElement> visited = new HashSet<>();
		Map<AsciiBox, CtExpression<Object>> armedFilters = new HashMap<>();
		AsciiBox lastBox = null;

		while (!work.isEmpty()) {
			for (Iterator<AsciiBox> iterator = work.iterator(); iterator.hasNext(); ) {
				AsciiBox box = iterator.next();
				if (hasInputs(box, visited)) {
					continue;
				}
				visited.add(box);
				iterator.remove();
				CtExpression<Object> filter = localFilters.get(box).baseFilter();

				lastBox = box;
				if (box.ins().isEmpty()) {
					armedFilters.put(box, filter);
					continue;
				}

				if (box.ins().get(0).start() instanceof AsciiMergeNode node) {
					visited.add(node);

					List<CtExpression<Object>> orderedInFilter = node.in()
						.stream()
						.map(Connection::start)
						.filter(it -> it instanceof AsciiBox)
						.map(it -> (AsciiBox) it)
						.map(armedFilters::get)
						.collect(Collectors.toCollection(ArrayList::new));

					CtTypeReference<Object> type = factory.Class().get(WithInputsUnorderedFilter.class).getReference();
					orderedInFilter.add(0, filter);
					CtConstructorCall<Object> orderedFilter =
						factory.Code().createConstructorCall(type, orderedInFilter.toArray(new CtExpression[0]));

					armedFilters.put(box, orderedFilter);
				} else {
					List<CtExpression<Object>> orderedInFilter = box.ins()
						.stream()
						.map(Connection::start)
						.filter(it -> it instanceof AsciiBox)
						.map(it -> (AsciiBox) it)
						.map(armedFilters::get)
						.collect(Collectors.toCollection(ArrayList::new));

					CtTypeReference<Object> type = factory.Class().get(WithInputsOrderedFilter.class).getReference();
					orderedInFilter.add(0, filter);
					CtConstructorCall<Object> orderedFilter =
						factory.Code().createConstructorCall(type, orderedInFilter.toArray(new CtExpression[0]));

					armedFilters.put(box, orderedFilter);
				}
			}
		}

		return armedFilters.get(lastBox);
	}

	private boolean hasInputs(AsciiBox box, Set<AsciiElement> visited) {
		for (Connection it : box.ins()) {
			if (it.start() instanceof AsciiMergeNode node) {
				if (node.in().stream().anyMatch(a -> !visited.contains(a.start()))) {
					return true;
				}
				continue;
			}
			if (!visited.contains(it.start())) {
				return true;
			}
		}
		return false;
	}

	private void generateCompositeFilterField() {
		CtExpression<?> filter = buildCompositeFilter();

		Set<ModifierKind> modifiers = EnumSet.of(ModifierKind.PRIVATE, ModifierKind.FINAL);
		CtTypeReference<?> nodeFilterType = factory.Type().get(NodeFilter.class).getReference();
		CtField<?> field = factory.createField(filterClass, modifiers, nodeFilterType, "filter", filter);

		filterClass.addField(field);
	}

	public void generateForSample(AsciiElement sample) {
		generateMatchClass(sample, new HashSet<>());
		generateCompositeFilterField();

		prettyPrint();
	}

	private void prettyPrint() {
		Environment environment = factory.getEnvironment();
		environment.setAutoImports(true);
		DefaultJavaPrettyPrinter printer = new DefaultJavaPrettyPrinter(environment);
		printer.setPreprocessors(List.of(new ForceImportProcessor()));
		try {
			Files.writeString(Path.of("/tmp/foo/Foo.java"), printer.prettyprint(filterClass));
			for (CtType<?> ctType : new FluentLauncher().autoImports(true)
				.inputResource("/tmp/foo/Foo.java")
				.complianceLevel(11)
				.buildModel()
				.getAllTypes()) {
				System.out.println(ctType);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private record AsciiBoxInformation(
		String name,
		CtExpression<Object> baseFilter
	) {
	}
}
