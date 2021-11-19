package com.github.firmwehr.gentle.firm;

import com.github.firmwehr.gentle.semantic.SemanticException;
import com.github.firmwehr.gentle.semantic.Visitor;
import com.github.firmwehr.gentle.semantic.ast.LocalVariableDeclaration;
import com.github.firmwehr.gentle.semantic.ast.SClassDeclaration;
import com.github.firmwehr.gentle.semantic.ast.SMethod;
import com.github.firmwehr.gentle.semantic.ast.SProgram;
import com.github.firmwehr.gentle.semantic.ast.expression.SBinaryOperatorExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SBooleanValueExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SIntegerValueExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SLocalVariableExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SNullExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SUnaryOperatorExpression;
import com.github.firmwehr.gentle.semantic.ast.statement.SIfStatement;
import com.github.firmwehr.gentle.semantic.ast.statement.SReturnStatement;
import com.github.firmwehr.gentle.semantic.ast.statement.SStatement;
import com.github.firmwehr.gentle.semantic.ast.type.SNormalType;
import com.github.firmwehr.gentle.semantic.ast.type.SVoidyType;
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
import firm.bindings.binding_irnode;
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
import java.util.Objects;
import java.util.Set;

public class FirmBuilder {

	public void convert(Path file, SProgram program) throws IOException, SemanticException {
		System.out.println(Path.of("").toAbsolutePath());
		System.in.read();
		//		Backend.option("dump=all");
		Firm.init("x86_64-linux-gnu", new String[]{"pic=1"});

		for (SClassDeclaration classDeclaration : program.classes().getAll()) {
			for (SMethod method : classDeclaration.methods().getAll()) {
				buildMethod(file, method);
			}
		}

		String basename = FilenameUtils.removeExtension(file.getFileName().toString());
		String assemblerFile = basename + ".s";
		Backend.createAssembler(assemblerFile, assemblerFile);

		Firm.finish();
	}

	private int countLocalVars(SMethod method) throws SemanticException {
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
		return variables.size();
	}

	private void buildMethod(Path file, SMethod method) throws SemanticException, IOException {
		Type[] inputs = method.parameters()
			.stream()
			.map(LocalVariableDeclaration::type)
			.map(FirmBuilder::getMode)
			.filter(Objects::nonNull)
			.map(Mode::getType)
			.toArray(Type[]::new);
		Type[] resultTypes = {getMode(method.returnType()).getType()};
		MethodType methodType = new MethodType(inputs, resultTypes);

		Entity methodEntity = new Entity(Program.getGlobalType(), method.name().ident(), methodType);

		Graph mainGraph = new Graph(methodEntity, countLocalVars(method));

		Construction construction = new Construction(mainGraph);

		Map<LocalVariableDeclaration, Integer> localVariables = new HashMap<>();

		for (LocalVariableDeclaration parameter : method.parameters()) {
			int index = localVariables.computeIfAbsent(parameter, ignored -> localVariables.size());
			Mode mode = getMode(parameter.type());
			if (mode != null) {
				construction.setVariable(index, construction.newConst(0, mode));
			}
		}

		Visitor<Node> generateVisitor = new Visitor<>() {

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
					case EQUAL -> construction.newCmp(lhs, rhs, Relation.Equal);
					case LESS_OR_EQUAL -> construction.newCmp(lhs, rhs, Relation.LessEqual);
					case LESS_THAN -> construction.newCmp(lhs, rhs, Relation.Less);
					case GREATER_OR_EQUAL -> construction.newCmp(lhs, rhs, Relation.GreaterEqual);
					case GREATER_THAN -> construction.newCmp(lhs, rhs, Relation.Greater);
					default -> throw new RuntimeException("TODO");
				};
			}

			@Override
			public Node visit(SIfStatement ifStatement) throws SemanticException {
				Block startBlock = construction.getCurrentBlock();
				Block afterBlock = construction.newBlock();

				Node conditionValue = ifStatement.condition().accept(this);
				construction.setCurrentBlock(startBlock);

				Node condition;
				if (conditionValue.getOpCode() == binding_irnode.ir_opcode.iro_Cmp) {
					condition = construction.newCond(conditionValue);
				} else {
					Node cmp = construction.newCmp(conditionValue, construction.newConst(0, conditionValue.getMode()),
						Relation.Equal);
					condition = construction.newCond(cmp);
				}

				// Swapped because we compare to 0
				Node falseCaseNode = construction.newProj(condition, Mode.getX(), 1);
				Node trueCaseNode = construction.newProj(condition, Mode.getX(), 0);

				Block trueBlock = construction.newBlock();
				trueBlock.addPred(trueCaseNode);
				construction.setCurrentBlock(trueBlock);
				ifStatement.body().accept(this);
				construction.setCurrentBlock(trueBlock);
				Node trueToAfter = construction.newJmp();
				afterBlock.addPred(trueToAfter);

				construction.setCurrentBlock(startBlock);

				if (ifStatement.elseBody().isPresent()) {
					Block falseBlock = construction.newBlock();
					falseBlock.addPred(falseCaseNode);

					construction.setCurrentBlock(falseBlock);
					ifStatement.elseBody().get().accept(this);
					construction.setCurrentBlock(falseBlock);
					Node falseToAfter = construction.newJmp();
					afterBlock.addPred(falseToAfter);
				} else {
					afterBlock.addPred(falseCaseNode);
				}

				construction.setCurrentBlock(afterBlock);

				//				mainGraph.keepAlive(afterBlock);

				return afterBlock;
			}

			@Override
			public Node visit(SIntegerValueExpression integerValueExpression) {
				return construction.newConst(integerValueExpression.value(), Mode.getIs());
			}

			@Override
			public Node visit(SBooleanValueExpression booleanValueExpression) {
				return construction.newConst(booleanValueExpression.value() ? 1 : 0, Mode.getBu());
			}

			@Override
			public Node visit(SLocalVariableExpression localVariableExpression) {
				LocalVariableDeclaration variable = localVariableExpression.localVariable();
				int index = localVariables.computeIfAbsent(variable, ignored -> localVariables.size());

				SNormalType type = localVariableExpression.localVariable().type();
				if (type.asBooleanType().isPresent()) {
					return construction.getVariable(index, Mode.getBu());
				} else if (type.asIntType().isPresent()) {
					return construction.getVariable(index, Mode.getIs());
				}
				throw new RuntimeException("TODO");
			}

			@Override
			public Node visit(SNullExpression nullExpression) {
				return construction.newConst(0, Mode.getP());
			}

			@Override
			public Node visit(SReturnStatement returnStatement) throws SemanticException {
				Node[] returnValues = new Node[0];
				if (returnStatement.returnValue().isPresent()) {
					returnValues = new Node[]{returnStatement.returnValue().get().accept(this)};
				} else {
					returnValues = new Node[]{construction.newConst(0, Mode.getIs())};
				}
				Node returnNode = construction.newReturn(construction.getCurrentMem(), returnValues);
				mainGraph.getEndBlock().addPred(returnNode);
				return returnNode;
			}

			@Override
			public Node visit(SUnaryOperatorExpression unaryOperatorExpression) throws SemanticException {
				return switch (unaryOperatorExpression.operator()) {
					case NEGATION -> construction.newMinus(unaryOperatorExpression.expression().accept(this));
					case LOGICAL_NOT -> {
						Node expr = unaryOperatorExpression.expression().accept(this);
						Node isZeroCmp =
							construction.newCmp(construction.newConst(0, expr.getMode()), expr, Relation.Equal);
						Node isZeroCond = construction.newCond(isZeroCmp);

						yield condToBu(isZeroCond);
					}
				};
			}

			private Node condToBu(Node isZeroCond) {
				Block startBlock = construction.getCurrentBlock();
				Block afterBlock = construction.newBlock();

				// Is not zero
				Node isNotZeroProj = construction.newProj(isZeroCond, Mode.getX(), 0);
				Block isNotZeroBlock = construction.newBlock();
				construction.setCurrentBlock(isNotZeroBlock);
				isNotZeroBlock.addPred(isNotZeroProj);
				afterBlock.addPred(construction.newJmp());

				construction.setCurrentBlock(startBlock);

				// Is zero
				Node isZeroProj = construction.newProj(isZeroCond, Mode.getX(), 1);
				Block isZeroBlock = construction.newBlock();
				construction.setCurrentBlock(isZeroBlock);
				isZeroBlock.addPred(isZeroProj);
				afterBlock.addPred(construction.newJmp());

				// After the conditional
				construction.setCurrentBlock(afterBlock);

				Node constZero = construction.newConst(0, Mode.getBu());
				Node constOne = construction.newConst(1, Mode.getBu());

				return construction.newPhi(new Node[]{constOne, constZero}, Mode.getBu());
			}
		};

		for (SStatement statement : method.body()) {
			statement.accept(generateVisitor);
		}

		Dump.dumpGraph(mainGraph, "before-mature");

		construction.finish();

		Dump.dumpGraph(mainGraph, "after-mature");
	}

	private static Mode getMode(SNormalType type) {
		if (type.asBooleanType().isPresent()) {
			return Mode.getBu();
		} else if (type.asIntType().isPresent()) {
			return Mode.getIs();
		}
		return null;
	}

	private static Mode getMode(SVoidyType type) {
		if (type.asExprType().asNormalType().isPresent()) {
			return getMode(type.asExprType().asNormalType().get());
		}
		if (type.asExprType().asVoidType().isPresent()) {
			return Mode.getIs();
		}
		return null;
	}
}
