package com.github.firmwehr.gentle.firm;

import com.github.firmwehr.gentle.semantic.SemanticException;
import com.github.firmwehr.gentle.semantic.Visitor;
import com.github.firmwehr.gentle.semantic.ast.LocalVariableDeclaration;
import com.github.firmwehr.gentle.semantic.ast.SMethod;
import com.github.firmwehr.gentle.semantic.ast.SProgram;
import com.github.firmwehr.gentle.semantic.ast.expression.SBinaryOperatorExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SIntegerValueExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SLocalVariableExpression;
import com.github.firmwehr.gentle.semantic.ast.statement.SIfStatement;
import com.github.firmwehr.gentle.semantic.ast.statement.SStatement;
import firm.Backend;
import firm.Construction;
import firm.Dump;
import firm.Entity;
import firm.Firm;
import firm.Graph;
import firm.MethodType;
import firm.Mode;
import firm.Program;
import firm.Relation;
import firm.Type;
import firm.bindings.binding_ircons;
import firm.nodes.Block;
import firm.nodes.Div;
import firm.nodes.Mod;
import firm.nodes.Node;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FirmBuilder {

	public void convert(Path file, SProgram program) throws IOException, SemanticException {
		System.out.println(Path.of("").toAbsolutePath());
		Firm.init("x86_64-linux-gnu", new String[]{"pic=1"});

		Dump.setDumpFlags(2 * (1 << 6) - 1);

		SMethod method = program.mainMethod();
		Set<LocalVariableDeclaration> variables = new HashSet<>();
		Visitor<Void> visitor = new Visitor<>() {

			@Override
			public Void defaultReturnValue() {
				return null;
			}

			@Override
			public Void visit(SLocalVariableExpression localVariableExpression) {
				variables.add(localVariableExpression.localVariable());
				return null;
			}
		};
		for (SStatement statement : method.body()) {
			statement.accept(visitor);
		}


		MethodType mainMethod = new MethodType(new Type[]{}, new Type[]{Mode.getIs().getType()});
		Entity mainEntity = new Entity(Program.getGlobalType(), "main", mainMethod);

		Graph mainGraph = new Graph(mainEntity, variables.size());
		Construction construction = new Construction(mainGraph);

		Visitor<Node> generateVisitor = new Visitor<>() {

			private Map<LocalVariableDeclaration, Integer> localVariables = new HashMap<>();

			@Override
			public Node defaultReturnValue() {
				return null;
			}

			@Override
			public Node visit(SBinaryOperatorExpression binaryOperatorExpression) throws SemanticException {
				Node lhs = binaryOperatorExpression.lhs().accept(this);
				Node rhs = binaryOperatorExpression.rhs().accept(this);

				return switch (binaryOperatorExpression.operator()) {
					case ASSIGN -> {
						if (binaryOperatorExpression.lhs() instanceof SLocalVariableExpression localVar) {
							int index = localVariables.computeIfAbsent(localVar.localVariable(),
								ignored -> localVariables.size());
							construction.setVariable(index, rhs);
							yield rhs;
						}
						throw new RuntimeException(":(");
					}
					case ADD -> construction.newAdd(lhs, rhs);
					case SUBTRACT -> construction.newSub(lhs, rhs);
					case MULTIPLY -> construction.newMul(lhs, rhs);
					case DIVIDE -> {
						Node divNode = construction.newDiv(construction.getCurrentMem(), lhs, rhs,
							binding_ircons.op_pin_state.op_pin_state_exc_pinned);
						construction.setCurrentMem(construction.newProj(divNode, Mode.getM(), Div.pnM));
						yield construction.newProj(divNode, Mode.getIs(), Div.pnRes);
					}
					case MODULO -> {
						Node modNode = construction.newMod(construction.getCurrentMem(), lhs, rhs,
							binding_ircons.op_pin_state.op_pin_state_exc_pinned);
						construction.setCurrentMem(construction.newProj(modNode, Mode.getM(), Mod.pnM));
						yield construction.newProj(modNode, Mode.getIs(), Div.pnRes);
					}
					case EQUAL -> {
						Node cmpNode = construction.newCmp(lhs, rhs, Relation.Equal);
						yield construction.newCond(cmpNode);
					}
					case LESS_OR_EQUAL -> construction.newCmp(lhs, rhs, Relation.LessEqual);
					case LESS_THAN -> construction.newCmp(lhs, rhs, Relation.Less);
					case GREATER_OR_EQUAL -> construction.newCmp(lhs, rhs, Relation.GreaterEqual);
					case GREATER_THAN -> construction.newCmp(lhs, rhs, Relation.Greater);
					default -> throw new RuntimeException("TODO");
				};
			}

			@Override
			public Node visit(SIfStatement ifStatement) throws SemanticException {
				Block afterBlock = construction.newBlock();

				Node condition = ifStatement.condition().accept(this);

				Node falseCaseNode = construction.newProj(condition, Mode.getX(), 0);
				Node trueCaseNode = construction.newProj(condition, Mode.getX(), 1);

				Block trueBlock = construction.newBlock();
				trueBlock.addPred(trueCaseNode);
				construction.setCurrentBlock(trueBlock);
				ifStatement.body().accept(this);
				Node trueToAfter = construction.newJmp();
				afterBlock.addPred(trueToAfter);

				if (ifStatement.elseBody().isPresent()) {
					Block falseBlock = construction.newBlock();
					falseBlock.addPred(falseCaseNode);

					construction.setCurrentBlock(falseBlock);
					ifStatement.elseBody().get().accept(this);
					Node falseToAfter = construction.newJmp();
					afterBlock.addPred(falseToAfter);
				}

				construction.setCurrentBlock(afterBlock);

				mainGraph.keepAlive(afterBlock);

				return afterBlock;
			}

			@Override
			public Node visit(SIntegerValueExpression integerValueExpression) {
				return construction.newConst(integerValueExpression.value(), Mode.getIs());
			}

			@Override
			public Node visit(SLocalVariableExpression localVariableExpression) {
				LocalVariableDeclaration variable = localVariableExpression.localVariable();
				int index = localVariables.computeIfAbsent(variable, ignored -> localVariables.size());
				return construction.getVariable(index, Mode.getIs());
			}
		};

		for (SStatement statement : method.body()) {
			statement.accept(generateVisitor);
		}

		for (int i = 0; i < variables.size(); i++) {
			mainGraph.keepAlive(construction.getVariable(i, Mode.getIs()));
		}

		Node oneConst = construction.newConst(1, Mode.getIs());
		Node returnNode = construction.newReturn(construction.getCurrentMem(), new Node[]{oneConst});
		mainGraph.getEndBlock().addPred(returnNode);

		construction.finish();

		Dump.dumpGraph(mainGraph, "test");

		String basename = FilenameUtils.removeExtension(file.getFileName().toString());
		String assemblerFile = basename + ".s";
		Backend.createAssembler(assemblerFile, assemblerFile);
		Runtime.getRuntime().exec(new String[]{"gcc", assemblerFile, "-o", basename});

		Firm.finish();
	}
}
