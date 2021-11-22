package com.github.firmwehr.gentle.firm;

import com.github.firmwehr.gentle.semantic.SemanticException;
import com.github.firmwehr.gentle.semantic.Visitor;
import com.github.firmwehr.gentle.semantic.ast.LocalVariableDeclaration;
import com.github.firmwehr.gentle.semantic.ast.SClassDeclaration;
import com.github.firmwehr.gentle.semantic.ast.SField;
import com.github.firmwehr.gentle.semantic.ast.SMethod;
import com.github.firmwehr.gentle.semantic.ast.expression.SArrayAccessExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SBinaryOperatorExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SBooleanValueExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SFieldAccessExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SIntegerValueExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SLocalVariableExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SMethodInvocationExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SNewObjectExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SNullExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SSystemInReadExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SSystemOutFlushExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SSystemOutPrintlnExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SSystemOutWriteExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SThisExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SUnaryOperatorExpression;
import com.github.firmwehr.gentle.semantic.ast.statement.SIfStatement;
import com.github.firmwehr.gentle.semantic.ast.statement.SReturnStatement;
import com.github.firmwehr.gentle.semantic.ast.statement.SWhileStatement;
import com.github.firmwehr.gentle.semantic.ast.type.SVoidType;
import com.github.firmwehr.gentle.source.SourceSpan;
import firm.ClassType;
import firm.Construction;
import firm.Dump;
import firm.Entity;
import firm.Graph;
import firm.Mode;
import firm.Relation;
import firm.Type;
import firm.bindings.binding_ircons;
import firm.bindings.binding_irnode;
import firm.nodes.Block;
import firm.nodes.Call;
import firm.nodes.Cond;
import firm.nodes.Div;
import firm.nodes.Load;
import firm.nodes.Mod;
import firm.nodes.Node;
import firm.nodes.Start;
import firm.nodes.Store;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

class FirmVisitor implements Visitor<Node> {

	private final TypeHelper typeHelper;
	private final EntityHelper entityHelper;

	private SlotTable slotTable;
	private Construction construction;
	private Graph currentGraph;
	private Set<Block> returningBlocks;

	private SClassDeclaration currentClass;

	public FirmVisitor() {
		this.typeHelper = new TypeHelper();
		this.entityHelper = new EntityHelper(typeHelper);
	}

	@Override
	public Node defaultReturnValue() {
		return null;
	}

	@Override
	public Node visit(SClassDeclaration classDeclaration) throws SemanticException {
		this.currentClass = classDeclaration;
		for (SField sField : classDeclaration.fields().getAll()) {
			visit(sField);
		}
		typeHelper.getClassType(classDeclaration).layoutFields();
		typeHelper.getClassType(classDeclaration).finishLayout();
		for (SMethod sMethod : classDeclaration.methods().getAll()) {
			visit(sMethod);
		}
		return defaultReturnValue();
	}

	@Override
	public Node visit(SField field) {
		entityHelper.setFieldEntity(field, currentClass);
		return Visitor.super.visit(field);
	}

	@Override
	public Node visit(SMethod method) throws SemanticException {
		this.slotTable = SlotTable.forMethod(method);
		this.returningBlocks = new HashSet<>();

		Entity entity = this.entityHelper.computeMethodEntity(method);

		this.currentGraph = new Graph(entity, this.slotTable.size());
		this.construction = new Construction(currentGraph);

		if (!method.isStatic()) {
			Node startNode = currentGraph.getStart();
			construction.setCurrentMem(construction.newProj(startNode, Mode.getM(), Start.pnM));
			Node argsTuple = construction.newProj(startNode, Mode.getT(), Start.pnTArgs);

			// the implicit receiver parameter is at pos 0 and needs to be handled separately
			Node thisProj = construction.newProj(argsTuple, typeHelper.getMode(currentClass.type()), 0);
			construction.setVariable(0, thisProj);

			List<LocalVariableDeclaration> parameters = method.parameters();
			for (LocalVariableDeclaration parameter : parameters) {
				var index = slotTable.computeIndex(parameter);

				Node proj = construction.newProj(argsTuple, typeHelper.getMode(parameter.type()), index);
				construction.setVariable(index, proj);
			}
		}

		Visitor.super.visit(method);

		if (method.returnType() instanceof SVoidType &&
			(method.body().isEmpty() || !(method.body().get(method.body().size() - 1) instanceof SReturnStatement))) {
			visit(new SReturnStatement(Optional.empty(), SourceSpan.dummy()));
		}

		Dump.dumpGraph(currentGraph, "before-mature");

		construction.finish();

		Dump.dumpGraph(currentGraph, "after-mature");

		return null;
	}

	@Override
	public Node visit(SMethodInvocationExpression methodInvocationExpression) throws SemanticException {
		// only non-static methods can be invoked, so we can always add 1 for the receiver
		int argumentSize = methodInvocationExpression.arguments().size() + 1;
		Node[] fArguments = new Node[argumentSize];
		fArguments[0] = methodInvocationExpression.expression().accept(this);
		List<SExpression> sArguments = methodInvocationExpression.arguments();
		for (int i = 0; i < sArguments.size(); i++) {
			SExpression argument = sArguments.get(i);
			fArguments[i + 1] = argument.accept(this);
		}
		SMethod method = methodInvocationExpression.method();
		Entity methodEntity = entityHelper.computeMethodEntity(method);
		Node address = construction.newAddress(methodEntity);
		Node call = construction.newCall(construction.getCurrentMem(), address, fArguments, methodEntity.getType());
		Node proj = construction.newProj(call, Mode.getT(), Call.pnTResult);
		construction.setCurrentMem(construction.newProj(call, Mode.getM(), Call.pnM));
		if (method.returnType() instanceof SVoidType) {
			return defaultReturnValue();
		}
		// 0 as we only have one return element
		return construction.newProj(proj, typeHelper.getMode(method.returnType().asExprType()), 0);
	}

	@Override
	public Node visit(SBinaryOperatorExpression binaryOperatorExpression) throws SemanticException {
		Node lhs = binaryOperatorExpression.lhs().accept(this);

		return switch (binaryOperatorExpression.operator()) {
			case ASSIGN -> {
				Node rhs = binaryOperatorExpression.rhs().accept(this);
				if (binaryOperatorExpression.lhs() instanceof SLocalVariableExpression localVar) {
					int index = slotTable.computeIndex(localVar.localVariable());
					construction.setVariable(index, rhs);
					yield rhs;
				}
				if (binaryOperatorExpression.lhs() instanceof SFieldAccessExpression fieldAccess) {
					Node member = construction.newMember(fieldAccess.expression().accept(this),
						entityHelper.getEntity(fieldAccess.field()));
					Node storeNode = construction.newStore(construction.getCurrentMem(), member, rhs);
					construction.setCurrentMem(construction.newProj(storeNode, Mode.getM(), Store.pnM));
					yield storeNode;
				}
				throw new RuntimeException(":(");
			}
			case ADD -> construction.newAdd(lhs, binaryOperatorExpression.rhs().accept(this));
			case SUBTRACT -> construction.newSub(lhs, binaryOperatorExpression.rhs().accept(this));
			case MULTIPLY -> construction.newMul(lhs, binaryOperatorExpression.rhs().accept(this));
			case DIVIDE -> {
				Node divNode =
					construction.newDiv(construction.getCurrentMem(), lhs, binaryOperatorExpression.rhs().accept(this),
						binding_ircons.op_pin_state.op_pin_state_pinned);
				construction.setCurrentMem(construction.newProj(divNode, Mode.getM(), Div.pnM));
				yield construction.newProj(divNode, Mode.getIs(), Div.pnRes);
			}
			case MODULO -> {
				Node modNode =
					construction.newMod(construction.getCurrentMem(), lhs, binaryOperatorExpression.rhs().accept(this),
						binding_ircons.op_pin_state.op_pin_state_pinned);
				construction.setCurrentMem(construction.newProj(modNode, Mode.getM(), Mod.pnM));
				yield construction.newProj(modNode, Mode.getIs(), Div.pnRes);
			}
			case EQUAL -> construction.newCmp(lhs, binaryOperatorExpression.rhs().accept(this), Relation.Equal);
			case NOT_EQUAL -> {
				Node equalCmp = construction.newCmp(lhs, binaryOperatorExpression.rhs().accept(this), Relation.Equal);
				Node equalCond = construction.newCond(equalCmp);
				yield condToBu(equalCond, 0, 1);
			}
			case LESS_OR_EQUAL -> construction.newCmp(lhs, binaryOperatorExpression.rhs().accept(this),
				Relation.LessEqual);
			case LESS_THAN -> construction.newCmp(lhs, binaryOperatorExpression.rhs().accept(this), Relation.Less);
			case GREATER_OR_EQUAL -> construction.newCmp(lhs, binaryOperatorExpression.rhs().accept(this),
				Relation.GreaterEqual);
			case GREATER_THAN -> construction.newCmp(lhs, binaryOperatorExpression.rhs().accept(this),
				Relation.Greater);
			case LOGICAL_OR -> {
				construction.getCurrentBlock().mature();
				Block afterBlock = construction.newBlock();
				Block aIsTrueBlock = construction.newBlock();
				Block aIsFalseBlock = construction.newBlock();

				Node aIsFalseCond = condFromBooleanExpr(lhs);

				Node aIsTrueProj = construction.newProj(aIsFalseCond, Mode.getX(), Cond.pnFalse);
				Node aIsFalseProj = construction.newProj(aIsFalseCond, Mode.getX(), Cond.pnTrue);

				// A TRUE BLOCK
				construction.setCurrentBlock(aIsTrueBlock);
				aIsTrueBlock.addPred(aIsTrueProj);
				aIsTrueBlock.mature();
				Node constTrue = construction.newConst(1, Mode.getBu());
				afterBlock.addPred(construction.newJmp());

				// A FALSE BLOCK
				construction.setCurrentBlock(aIsFalseBlock);
				aIsFalseBlock.addPred(aIsFalseProj);
				aIsFalseBlock.mature();
				Node bValue = condToBu(condFromBooleanExpr(binaryOperatorExpression.rhs().accept(this)));
				afterBlock.addPred(construction.newJmp());

				// AFTER BLOCK
				construction.setCurrentBlock(afterBlock);
				afterBlock.mature();
				yield construction.newPhi(new Node[]{constTrue, bValue}, Mode.getBu());
			}
			case LOGICAL_AND -> {
				construction.getCurrentBlock().mature();
				Block afterBlock = construction.newBlock();
				Block aIsTrueBlock = construction.newBlock();
				Block aIsFalseBlock = construction.newBlock();

				Node aIsFalseCond = condFromBooleanExpr(lhs);

				Node aIsTrueProj = construction.newProj(aIsFalseCond, Mode.getX(), Cond.pnFalse);
				Node aIsFalseProj = construction.newProj(aIsFalseCond, Mode.getX(), Cond.pnTrue);

				// A FALSE BLOCK
				construction.setCurrentBlock(aIsFalseBlock);
				aIsFalseBlock.addPred(aIsFalseProj);
				aIsFalseBlock.mature();
				Node constFalse = construction.newConst(0, Mode.getBu());
				afterBlock.addPred(construction.newJmp());

				// A TRUE BLOCK
				construction.setCurrentBlock(aIsTrueBlock);
				aIsTrueBlock.addPred(aIsTrueProj);
				aIsTrueBlock.mature();
				Node bValue = condToBu(condFromBooleanExpr(binaryOperatorExpression.rhs().accept(this)));
				afterBlock.addPred(construction.newJmp());

				// AFTER BLOCK
				construction.setCurrentBlock(afterBlock);
				afterBlock.mature();
				yield construction.newPhi(new Node[]{constFalse, bValue}, Mode.getBu());
			}
		};
	}

	@Override
	public Node visit(SThisExpression thisExpression) throws SemanticException {
		return construction.getVariable(0, Mode.getP());
	}

	@Override
	public Node visit(SArrayAccessExpression arrayExpression) throws SemanticException {
		Node arrayNode = arrayExpression.expression().accept(this);
		Node indexNode = arrayExpression.index().accept(this);

		Type innerType = typeHelper.getType(arrayExpression.type());
		Mode innerMode = typeHelper.getMode(arrayExpression.type());
		Node typeSizeNode = construction.newConst(innerType.getSize(), Mode.getLs());
		Node offsetNode = construction.newMul(construction.newConv(indexNode, Mode.getLs()), typeSizeNode);
		Node targetAddressNode = construction.newAdd(arrayNode, offsetNode);
		Node loadNode = construction.newLoad(construction.getCurrentMem(), targetAddressNode, innerMode);
		construction.setCurrentMem(construction.newProj(loadNode, Mode.getM(), Load.pnM));
		return construction.newProj(loadNode, innerMode, Load.pnRes);
	}

	@Override
	public Node visit(SIfStatement ifStatement) throws SemanticException {
		Block startBlock = construction.getCurrentBlock();
		returningBlocks.add(startBlock);
		startBlock.mature();
		Block afterBlock = construction.newBlock();

		Node conditionValue = ifStatement.condition().accept(this);
		construction.setCurrentBlock(startBlock);

		Node condition = condFromBooleanExpr(conditionValue);

		// Swapped because we compare to 0
		Node falseCaseNode = construction.newProj(condition, Mode.getX(), 1);
		Node trueCaseNode = construction.newProj(condition, Mode.getX(), 0);

		Block trueBlock = construction.newBlock();
		trueBlock.addPred(trueCaseNode);
		trueBlock.mature();
		construction.setCurrentBlock(trueBlock);
		ifStatement.body().accept(this);
		construction.setCurrentBlock(trueBlock);
		jumpIfNotReturned(trueBlock, afterBlock);

		construction.setCurrentBlock(startBlock);

		if (ifStatement.elseBody().isPresent()) {
			Block falseBlock = construction.newBlock();
			falseBlock.addPred(falseCaseNode);
			falseBlock.mature();

			construction.setCurrentBlock(falseBlock);
			ifStatement.elseBody().get().accept(this);
			construction.setCurrentBlock(falseBlock);
			jumpIfNotReturned(falseBlock, afterBlock);
		} else {
			afterBlock.addPred(falseCaseNode);
		}

		construction.setCurrentBlock(afterBlock);
		afterBlock.mature();

		return afterBlock;
	}

	private void jumpIfNotReturned(Block from, Block to) {
		if (returningBlocks.contains(from)) {
			return;
		}
		construction.setCurrentBlock(from);
		to.addPred(construction.newJmp());
	}

	private Node condFromBooleanExpr(Node conditionValue) {
		if (conditionValue.getOpCode() == binding_irnode.ir_opcode.iro_Cmp) {
			conditionValue = condToBu(construction.newCond(conditionValue), 0, 1);
		}
		Node cmp =
			construction.newCmp(conditionValue, construction.newConst(0, conditionValue.getMode()), Relation.Equal);
		return construction.newCond(cmp);
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

		int index = slotTable.computeIndex(variable);
		Mode mode = typeHelper.getMode(localVariableExpression.localVariable().type());

		return construction.getVariable(index, mode);
	}

	@Override
	public Node visit(SNullExpression nullExpression) {
		return construction.newConst(0, Mode.getP());
	}

	@Override
	public Node visit(SFieldAccessExpression fieldAccessExpression) throws SemanticException {
		Node exprNode = fieldAccessExpression.expression().accept(this);
		Node member = construction.newMember(exprNode, entityHelper.getEntity(fieldAccessExpression.field()));
		Mode mode = typeHelper.getMode(fieldAccessExpression.type());
		Node load = construction.newLoad(construction.getCurrentMem(), member, mode);
		construction.setCurrentMem(construction.newProj(load, Mode.getM(), Load.pnM));
		return construction.newProj(load, mode, Load.pnRes);
	}

	@Override
	public Node visit(SReturnStatement returnStatement) throws SemanticException {
		Node[] returnValues = new Node[0];
		if (returnStatement.returnValue().isPresent()) {
			returnValues = new Node[]{returnStatement.returnValue().get().accept(this)};
		}
		Node returnNode = construction.newReturn(construction.getCurrentMem(), returnValues);
		currentGraph.getEndBlock().addPred(returnNode);
		returningBlocks.add(construction.getCurrentBlock());
		return returnNode;
	}

	@Override
	public Node visit(SUnaryOperatorExpression unaryOperatorExpression) throws SemanticException {
		return switch (unaryOperatorExpression.operator()) {
			case NEGATION -> construction.newMinus(unaryOperatorExpression.expression().accept(this));
			case LOGICAL_NOT -> {
				Node expr = unaryOperatorExpression.expression().accept(this);
				yield condToBu(condFromBooleanExpr(expr), 0, 1);
			}
		};
	}

	@Override
	public Node visit(SWhileStatement whileStatement) throws SemanticException {
		Block header = construction.newBlock();
		Block body = construction.newBlock();
		Block after = construction.newBlock();

		header.addPred(construction.newJmp());
		construction.setCurrentBlock(header);
		Node cond = condFromBooleanExpr(whileStatement.condition().accept(this));

		Node trueCase = construction.newProj(cond, Mode.getX(), Cond.pnTrue);
		Node falseCase = construction.newProj(cond, Mode.getX(), Cond.pnFalse);

		construction.setCurrentBlock(body);
		body.addPred(trueCase);
		body.mature();
		whileStatement.body().accept(this);
		header.addPred(construction.newJmp());
		header.mature();

		construction.setCurrentBlock(after);
		after.addPred(falseCase);
		after.mature();

		return null;
	}


	@Override
	public Node visit(SSystemInReadExpression systemInReadExpression) throws SemanticException {
		Entity getCharEntity = entityHelper.getEntity(StdLibEntity.GETCHAR);
		Node getCharAddress = construction.newAddress(getCharEntity);
		var call =
			construction.newCall(construction.getCurrentMem(), getCharAddress, new Node[]{}, getCharEntity.getType());
		construction.setCurrentMem(construction.newProj(call, Mode.getM(), Call.pnM));
		Node proj = construction.newProj(call, Mode.getT(), Call.pnTResult);
		return construction.newProj(proj, Mode.getIs(), 0);
	}

	@Override
	public Node visit(SSystemOutFlushExpression systemOutFlushExpression) throws SemanticException {
		Node stdOutAddress = construction.newAddress(entityHelper.getEntity(StdLibEntity.STDOUT));
		Entity fflushEntity = entityHelper.getEntity(StdLibEntity.FFLUSH);
		Node fflushAddress = construction.newAddress(fflushEntity);
		Node stdOutLoad = construction.newLoad(construction.getCurrentMem(), stdOutAddress, Mode.getP());
		construction.setCurrentMem(construction.newProj(stdOutLoad, Mode.getM(), Load.pnM));
		Node stdOutResult = construction.newProj(stdOutLoad, Mode.getP(), Load.pnRes);
		var call = construction.newCall(construction.getCurrentMem(), fflushAddress, new Node[]{stdOutResult},
			fflushEntity.getType());
		construction.setCurrentMem(construction.newProj(call, Mode.getM(), Call.pnM));
		return defaultReturnValue(); // TODO the return value should never be used
	}

	@Override
	public Node visit(SSystemOutPrintlnExpression systemOutPrintlnExpression) throws SemanticException {
		Entity printlnEntity = entityHelper.getEntity(StdLibEntity.PRINTLN);
		Node printlnAddress = construction.newAddress(printlnEntity);
		Node argument = systemOutPrintlnExpression.argument().accept(this);
		var call = construction.newCall(construction.getCurrentMem(), printlnAddress, new Node[]{argument},
			printlnEntity.getType());
		construction.setCurrentMem(construction.newProj(call, Mode.getM(), Call.pnM));
		return defaultReturnValue(); // TODO the return value should never be used
	}

	@Override
	public Node visit(SSystemOutWriteExpression systemOutWriteExpression) throws SemanticException {
		Entity putCharEntity = entityHelper.getEntity(StdLibEntity.PUTCHAR);
		Node putCharAddress = construction.newAddress(putCharEntity);
		Node argument = systemOutWriteExpression.argument().accept(this);
		var call = construction.newCall(construction.getCurrentMem(), putCharAddress, new Node[]{argument},
			putCharEntity.getType());
		construction.setCurrentMem(construction.newProj(call, Mode.getM(), Call.pnM));
		return defaultReturnValue(); // TODO the return value should never be used
	}

	@Override
	public Node visit(SNewObjectExpression newObjectExpression) throws SemanticException {
		ClassType type = typeHelper.getClassType(newObjectExpression.classDecl());
		int size = type.getSize();
		Entity mallocEntity = entityHelper.getEntity(StdLibEntity.MALLOC);
		Node mallocAddress = construction.newAddress(mallocEntity);
		Node sizeConst = construction.newConst(size, Mode.getLu());
		Node call = construction.newCall(construction.getCurrentMem(), mallocAddress, new Node[]{sizeConst},
			mallocEntity.getType());
		construction.setCurrentMem(construction.newProj(call, Mode.getM(), Call.pnM));
		Node proj = construction.newProj(call, Mode.getT(), Call.pnTResult);
		return construction.newProj(proj, Mode.getP(), 0);
	}

	private Node condToBu(Node isZeroCond) {
		return condToBu(isZeroCond, 1, 0);
	}

	private Node condToBu(Node isZeroCond, int valueForTrue, int valueForFalse) {
		Block startBlock = construction.getCurrentBlock();
		Block afterBlock = construction.newBlock();

		// Is not zero
		Node isNotZeroProj = construction.newProj(isZeroCond, Mode.getX(), 0);
		Block isNotZeroBlock = construction.newBlock();
		construction.setCurrentBlock(isNotZeroBlock);
		isNotZeroBlock.addPred(isNotZeroProj);
		isNotZeroBlock.mature();
		afterBlock.addPred(construction.newJmp());

		construction.setCurrentBlock(startBlock);

		// Is zero
		Node isZeroProj = construction.newProj(isZeroCond, Mode.getX(), 1);
		Block isZeroBlock = construction.newBlock();
		construction.setCurrentBlock(isZeroBlock);
		isZeroBlock.addPred(isZeroProj);
		isZeroBlock.mature();
		afterBlock.addPred(construction.newJmp());

		// After the conditional
		construction.setCurrentBlock(afterBlock);
		afterBlock.mature();

		Node constTrue = construction.newConst(valueForFalse, Mode.getBu());
		Node constFalse = construction.newConst(valueForTrue, Mode.getBu());

		return construction.newPhi(new Node[]{constFalse, constTrue}, Mode.getBu());
	}
}
