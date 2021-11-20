package com.github.firmwehr.gentle.firm;

import com.github.firmwehr.gentle.semantic.SemanticException;
import com.github.firmwehr.gentle.semantic.Visitor;
import com.github.firmwehr.gentle.semantic.ast.LocalVariableDeclaration;
import com.github.firmwehr.gentle.semantic.ast.SClassDeclaration;
import com.github.firmwehr.gentle.semantic.ast.SMethod;
import com.github.firmwehr.gentle.semantic.ast.expression.SBinaryOperatorExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SBooleanValueExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SIntegerValueExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SLocalVariableExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SNullExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SUnaryOperatorExpression;
import com.github.firmwehr.gentle.semantic.ast.statement.SIfStatement;
import com.github.firmwehr.gentle.semantic.ast.statement.SReturnStatement;
import com.github.firmwehr.gentle.semantic.ast.type.SNormalType;
import firm.Construction;
import firm.Dump;
import firm.Entity;
import firm.Graph;
import firm.MethodType;
import firm.Mode;
import firm.Relation;
import firm.Type;
import firm.bindings.binding_ircons;
import firm.bindings.binding_irnode;
import firm.nodes.Block;
import firm.nodes.Div;
import firm.nodes.Mod;
import firm.nodes.Node;
import firm.nodes.Start;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class FirmVisitor implements Visitor<Node> {

	private Map<LocalVariableDeclaration, Integer> localVariables;
	private Construction construction;
	private Graph currentGraph;
	private final TypeHelper typeHelper;

	private SClassDeclaration currentClass;

	public FirmVisitor() {
		this.typeHelper = new TypeHelper();
	}

	@Override
	public Node defaultReturnValue() {
		return null;
	}

	@Override
	public Node visit(SClassDeclaration classDeclaration) throws SemanticException {
		this.currentClass = classDeclaration;
		return Visitor.super.visit(classDeclaration);
	}

	@Override
	public Node visit(SMethod method) throws SemanticException {
		this.localVariables = new HashMap<>();

		Type[] types = method.parameters()
			.stream()
			.map(LocalVariableDeclaration::type)
			.map(typeHelper::getType)
			.toArray(Type[]::new);

		Type returnType =
			method.returnType().asExprType().asNormalType().map(typeHelper::getType).orElse(Mode.getIs().getType());

		MethodType methodType = new MethodType(types, new Type[]{returnType});
		Entity entity = new Entity(typeHelper.getType(currentClass), method.name().ident(), methodType);

		this.currentGraph = new Graph(entity, Utils.countLocalVars(method) + method.parameters().size());
		this.construction = new Construction(currentGraph);

		if (!method.parameters().isEmpty()) {
			Node startNode = construction.newStart();
			Node argsTuple = construction.newProj(startNode, Mode.getT(), Start.pnTArgs);

			List<LocalVariableDeclaration> parameters = method.parameters();
			for (int i = 0; i < parameters.size(); i++) {
				LocalVariableDeclaration parameter = parameters.get(i);
				localVariables.put(parameter, i);

				Node proj = construction.newProj(argsTuple, typeHelper.getMode(parameter.type()), i);
				construction.setVariable(i, proj);
			}
		}

		Visitor.super.visit(method);

		Dump.dumpGraph(currentGraph, "before-mature");

		construction.finish();

		Dump.dumpGraph(currentGraph, "after-mature");

		return null;
	}

	@Override
	public Node visit(SBinaryOperatorExpression binaryOperatorExpression) throws SemanticException {
		Node lhs = binaryOperatorExpression.lhs().accept(this);
		Node rhs = binaryOperatorExpression.rhs().accept(this);

		return switch (binaryOperatorExpression.operator()) {
			case ASSIGN -> {
				if (binaryOperatorExpression.lhs() instanceof SLocalVariableExpression localVar) {
					int index =
						localVariables.computeIfAbsent(localVar.localVariable(), ignored -> localVariables.size());
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
			Node cmp =
				construction.newCmp(conditionValue, construction.newConst(0, conditionValue.getMode()),
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
		currentGraph.getEndBlock().addPred(returnNode);
		return returnNode;
	}

	@Override
	public Node visit(SUnaryOperatorExpression unaryOperatorExpression) throws SemanticException {
		return switch (unaryOperatorExpression.operator()) {
			case NEGATION -> construction.newMinus(unaryOperatorExpression.expression().accept(this));
			case LOGICAL_NOT -> {
				Node expr = unaryOperatorExpression.expression().accept(this);
				Node isZeroCmp = construction.newCmp(construction.newConst(0, expr.getMode()), expr, Relation.Equal);
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
}
