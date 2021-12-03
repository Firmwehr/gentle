package com.github.firmwehr.gentle.firm.construction;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.semantic.Namespace;
import com.github.firmwehr.gentle.semantic.ast.LocalVariableDeclaration;
import com.github.firmwehr.gentle.semantic.ast.SClassDeclaration;
import com.github.firmwehr.gentle.semantic.ast.SField;
import com.github.firmwehr.gentle.semantic.ast.SMethod;
import com.github.firmwehr.gentle.semantic.ast.SProgram;
import com.github.firmwehr.gentle.semantic.ast.expression.SArrayAccessExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SBinaryOperatorExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SBooleanValueExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SFieldAccessExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SIntegerValueExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SLocalVariableExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SMethodInvocationExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SNewArrayExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SNewObjectExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SNullExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SSystemInReadExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SSystemOutFlushExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SSystemOutPrintlnExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SSystemOutWriteExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SThisExpression;
import com.github.firmwehr.gentle.semantic.ast.expression.SUnaryOperatorExpression;
import com.github.firmwehr.gentle.semantic.ast.statement.SBlock;
import com.github.firmwehr.gentle.semantic.ast.statement.SExpressionStatement;
import com.github.firmwehr.gentle.semantic.ast.statement.SIfStatement;
import com.github.firmwehr.gentle.semantic.ast.statement.SReturnStatement;
import com.github.firmwehr.gentle.semantic.ast.statement.SStatement;
import com.github.firmwehr.gentle.semantic.ast.statement.SWhileStatement;
import com.github.firmwehr.gentle.semantic.ast.type.SVoidType;
import com.github.firmwehr.gentle.source.SourceSpan;
import com.github.firmwehr.gentle.util.GraphDumper;
import com.google.common.base.Preconditions;
import firm.ClassType;
import firm.Construction;
import firm.Entity;
import firm.Graph;
import firm.Mode;
import firm.Relation;
import firm.Type;
import firm.bindings.binding_ircons;
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
import java.util.function.Consumer;

public class FirmGraphBuilder {

	private final TypeHelper typeHelper;
	private final EntityHelper entityHelper;

	public FirmGraphBuilder() {
		this.typeHelper = new TypeHelper();
		this.entityHelper = new EntityHelper(typeHelper);
	}

	public void buildGraph(SProgram program) {
		layoutClasses(program.classes());
		for (SClassDeclaration declaration : program.classes().getAll()) {
			processClass(declaration);
		}
	}

	private void processClass(SClassDeclaration declaration) {
		for (SMethod method : declaration.methods().getAll()) {
			processMethod(method);
		}
	}

	private void processMethod(SMethod method) {
		SlotTable slotTable = SlotTable.forMethod(method);

		Entity entity = this.entityHelper.computeMethodEntity(method);

		Graph currentGraph = new Graph(entity, slotTable.size());
		Construction construction = new Construction(currentGraph);

		if (!method.isStatic()) {
			Node startNode = currentGraph.getStart();
			Node argsTuple = construction.newProj(startNode, Mode.getT(), Start.pnTArgs);

			// the implicit receiver parameter is at pos 0 and needs to be handled separately
			Node thisProj = construction.newProj(argsTuple, typeHelper.getMode(method.classDecl().type()), 0);
			construction.setVariable(0, thisProj);

			List<LocalVariableDeclaration> parameters = method.parameters();
			for (LocalVariableDeclaration parameter : parameters) {
				var index = slotTable.computeIndex(parameter);

				Node proj = construction.newProj(argsTuple, typeHelper.getMode(parameter.type()), index);
				construction.setVariable(index, proj);
			}
		}

		processMethodBody(new Context(construction, slotTable, method), method);

		construction.finish();

		GraphDumper.dumpGraph(currentGraph, "after-mature");
	}

	private void processMethodBody(Context context, SMethod method) {
		List<SStatement> body = method.body();
		processBlock(context, body);

		if (method.isStatic()) {
			SIntegerValueExpression returnValue = new SIntegerValueExpression(0, SourceSpan.dummy());
			processStatement(context, new SReturnStatement(Optional.of(returnValue), SourceSpan.dummy()));
			return;
		}

		if (method.returnType() instanceof SVoidType &&
			!context.isReturning(context.construction().getCurrentBlock())) {
			if (body.isEmpty() || !(body.get(body.size() - 1) instanceof SReturnStatement)) {
				processStatement(context, new SReturnStatement(Optional.empty(), SourceSpan.dummy()));
			}
		}
	}

	private void processStatement(Context context, SStatement statement) {
		switch (statement) {
			case SBlock block -> processBlock(context, block.statements());
			case SExpressionStatement expressionStatement -> processValueExpression(context,
				expressionStatement.expression());
			case SIfStatement ifStatement -> processIf(context, ifStatement);
			case SReturnStatement returnStatement -> processReturn(context, returnStatement);
			case SWhileStatement whileStatement -> processWhile(context, whileStatement);
		}
	}

	private void processBlock(Context context, List<SStatement> block) {
		for (SStatement statement : block) {
			processStatement(context, statement);
			if (statement instanceof SReturnStatement) {
				return; // don't process dead code
			}
		}
	}

	private void processWhile(Context context, SWhileStatement whileStatement) {
		Construction construction = context.construction();
		Block header = construction.newBlock();
		Block body = construction.newBlock();
		Block after = construction.newBlock();

		jumpIfNotReturning(context, header);
		construction.setCurrentBlock(header);

		JumpTarget jumpTarget = new JumpTarget(body, after);
		processLogicalExpression(context, whileStatement.condition(), jumpTarget);

		body.mature();
		after.mature();

		construction.setCurrentBlock(body);
		processStatement(context, whileStatement.body());
		jumpIfNotReturning(context, header);
		header.mature();

		construction.setCurrentBlock(after);

	}

	private void jumpIfNotReturning(Context context, Block target) {
		if (!context.isReturning(context.construction().getCurrentBlock())) {
			target.addPred(context.construction().newJmp());
		}
	}

	private void processIf(Context context, SIfStatement ifStatement) {
		Construction construction = context.construction();
		Block afterBlock = construction.newBlock();
		Block trueBlock = construction.newBlock();
		Block falseBlock = construction.newBlock();

		JumpTarget jumpTarget = new JumpTarget(trueBlock, falseBlock);
		processLogicalExpression(context, ifStatement.condition(), jumpTarget);
		trueBlock.mature();
		falseBlock.mature();

		construction.setCurrentBlock(trueBlock);
		processStatement(context, ifStatement.body());
		jumpIfNotReturning(context, afterBlock);

		construction.setCurrentBlock(falseBlock);
		if (ifStatement.elseBody().isPresent()) {
			processStatement(context, ifStatement.elseBody().get());
		}
		jumpIfNotReturning(context, afterBlock);

		construction.setCurrentBlock(afterBlock);
		afterBlock.mature();
	}

	private void processReturn(Context context, SReturnStatement returnStatement) {
		Node[] returnValues = new Node[0];
		if (returnStatement.returnValue().isPresent()) {
			returnValues = new Node[]{processValueExpression(context, returnStatement.returnValue().get())};
		}
		if (context.currentMethod().isStatic()) {
			returnValues =
				new Node[]{processValueExpression(context, new SIntegerValueExpression(0, SourceSpan.dummy()))};
		}
		Construction construction = context.construction();
		Node returnNode = construction.newReturn(construction.getCurrentMem(), returnValues);
		construction.getGraph().getEndBlock().addPred(returnNode);
		context.setReturns(construction.getCurrentBlock());
	}

	private void processLogicalExpression(Context context, SExpression expression, JumpTarget jumpTarget) {
		switch (expression) {
			case SBinaryOperatorExpression expr -> processLogicalBinaryOperator(context, expr, jumpTarget);
			case SUnaryOperatorExpression expr -> processLogicalUnaryOperator(context, expr, jumpTarget);
			default -> booleanToJump(context, processValueExpression(context, expression), jumpTarget);
		}
	}

	private Node processValueExpression(Context context, SExpression expression) {
		return switch (expression) {
			case SArrayAccessExpression expr -> processArrayAccess(context, expr);
			case SBinaryOperatorExpression expr -> processBinaryOperator(context, expr);
			case SBooleanValueExpression expr -> processBooleanValue(context, expr);
			case SFieldAccessExpression expr -> processFieldAccess(context, expr);
			case SIntegerValueExpression expr -> processIntegerValue(context, expr);
			case SLocalVariableExpression expr -> processLocalVariable(context, expr);
			case SMethodInvocationExpression expr -> processMethodInvocation(context, expr);
			case SNewArrayExpression expr -> processNewArray(context, expr);
			case SNewObjectExpression expr -> processNewObject(context, expr);
			case SNullExpression ignored -> processNull(context);
			case SSystemInReadExpression ignored -> processSystemInRead(context);
			case SSystemOutFlushExpression ignored -> processSystemOutFlush(context);
			case SSystemOutPrintlnExpression expr -> processSystemOutPrintln(context, expr);
			case SSystemOutWriteExpression expr -> processSystemOutWrite(context, expr);
			case SThisExpression ignored -> processThis(context);
			case SUnaryOperatorExpression expr -> processUnaryOperator(context, expr);
		};
	}

	private void booleanToJump(Context context, Node node, JumpTarget jumpTarget) {
		Preconditions.checkArgument(node.getMode().isInt(), "Expected boolean literal (Bu), got " + node);

		Node right = context.construction().newConst(0, Mode.getBu());
		JumpTarget invertedTarget = new JumpTarget(jumpTarget.falseBlock(), jumpTarget.trueBlock());

		processRelation(context, node, right, Relation.Equal, invertedTarget);
	}

	private Node processArrayAccess(Context context, SArrayAccessExpression expr) {
		Construction construction = context.construction();
		Node target = computeArrayAccessTarget(context, expr);
		Mode innerMode = typeHelper.getMode(expr.type());
		Node loadNode = construction.newLoad(construction.getCurrentMem(), target, innerMode);
		construction.setCurrentMem(construction.newProj(loadNode, Mode.getM(), Load.pnM));
		return construction.newProj(loadNode, innerMode, Load.pnRes);
	}

	private void processLogicalBinaryOperator(Context context, SBinaryOperatorExpression expr, JumpTarget target) {
		switch (expr.operator()) {
			case ASSIGN -> processAssignment(context, expr, target);
			case LOGICAL_OR -> processLogicalOr(context, expr, target);
			case LOGICAL_AND -> processLogicalAnd(context, expr, target);
			case EQUAL -> processRelation(context, expr, Relation.Equal, target);
			case NOT_EQUAL -> processRelation(context, expr, Relation.UnorderedLessGreater, target);
			case LESS_THAN -> processRelation(context, expr, Relation.Less, target);
			case LESS_OR_EQUAL -> processRelation(context, expr, Relation.LessEqual, target);
			case GREATER_THAN -> processRelation(context, expr, Relation.Greater, target);
			case GREATER_OR_EQUAL -> processRelation(context, expr, Relation.GreaterEqual, target);
			default -> throw new InternalCompilerException(
				"arithmetic expression in condition binary operator " + expr);
		}
	}

	private Node processBinaryOperator(Context context, SBinaryOperatorExpression expr) {
		Construction construction = context.construction();

		return switch (expr.operator()) {
			case ASSIGN -> processAssignment(context, expr);
			case LOGICAL_OR -> condToBool(context, target -> processLogicalOr(context, expr, target));
			case LOGICAL_AND -> condToBool(context, target -> processLogicalAnd(context, expr, target));
			case EQUAL -> condToBool(context, target -> processRelation(context, expr, Relation.Equal, target));
			case NOT_EQUAL -> condToBool(context,
				target -> processRelation(context, expr, Relation.UnorderedLessGreater, target));
			case LESS_THAN -> condToBool(context, target -> processRelation(context, expr, Relation.Less, target));
			case LESS_OR_EQUAL -> condToBool(context,
				target -> processRelation(context, expr, Relation.LessEqual, target));
			case GREATER_THAN -> condToBool(context,
				target -> processRelation(context, expr, Relation.Greater, target));
			case GREATER_OR_EQUAL -> condToBool(context,
				target -> processRelation(context, expr, Relation.GreaterEqual, target));
			case ADD -> construction.newAdd(processValueExpression(context, expr.lhs()),
				processValueExpression(context, expr.rhs()));
			case SUBTRACT -> construction.newSub(processValueExpression(context, expr.lhs()),
				processValueExpression(context, expr.rhs()));
			case MULTIPLY -> construction.newMul(processValueExpression(context, expr.lhs()),
				processValueExpression(context, expr.rhs()));
			case DIVIDE -> {
				Node divNode =
					construction.newDiv(construction.getCurrentMem(), processValueExpression(context, expr.lhs()),
						processValueExpression(context, expr.rhs()), binding_ircons.op_pin_state.op_pin_state_pinned);
				construction.setCurrentMem(construction.newProj(divNode, Mode.getM(), Div.pnM));
				yield construction.newProj(divNode, Mode.getIs(), Div.pnRes);
			}
			case MODULO -> {
				Node modNode =
					construction.newMod(construction.getCurrentMem(), processValueExpression(context, expr.lhs()),
						processValueExpression(context, expr.rhs()), binding_ircons.op_pin_state.op_pin_state_pinned);
				construction.setCurrentMem(construction.newProj(modNode, Mode.getM(), Mod.pnM));
				yield construction.newProj(modNode, Mode.getIs(), Mod.pnRes);
			}
		};
	}

	private Node condToBool(Context context, Consumer<JumpTarget> processInner) {
		// TODO: Can we use the MUX node in some cases?
		Construction construction = context.construction();
		Block trueBlock = construction.newBlock();
		Block falseBlock = construction.newBlock();
		Block afterBlock = construction.newBlock();

		processInner.accept(new JumpTarget(trueBlock, falseBlock));
		trueBlock.mature();
		falseBlock.mature();

		assertNotReturning(context, falseBlock);
		assertNotReturning(context, trueBlock);
		construction.setCurrentBlock(falseBlock);
		afterBlock.addPred(construction.newJmp());
		construction.setCurrentBlock(trueBlock);
		afterBlock.addPred(construction.newJmp());

		construction.setCurrentBlock(afterBlock);
		afterBlock.mature();
		return construction.newPhi(
			new Node[]{construction.newConst(0, Mode.getBu()), construction.newConst(1, Mode.getBu())}, Mode.getBu());
	}

	private void assertNotReturning(Context context, Block block) {
		if (context.isReturning(block)) {
			throw new InternalCompilerException("block " + block + " should not return but is marked as returning");
		}
	}

	private void processAssignment(Context context, SBinaryOperatorExpression expr, JumpTarget jumpTarget) {
		Node rhs = processAssignment(context, expr);
		booleanToJump(context, rhs, jumpTarget);
	}

	private Node processAssignment(Context context, SBinaryOperatorExpression expr) {
		// We need to evaluate the LHS before the RHS.
		// https://docs.oracle.com/javase/specs/jls/se17/html/jls-15.html#jls-15.26.1
		Construction construction = context.construction();

		return switch (expr.lhs()) {
			case SLocalVariableExpression localVar -> {
				int index = context.slotTable().computeIndex(localVar.localVariable());
				Node rhs = processValueExpression(context, expr.rhs());
				construction.setVariable(index, rhs);
				yield rhs;
			}
			case SFieldAccessExpression fieldAccess -> {
				Node member = construction.newMember(processValueExpression(context, fieldAccess.expression()),
					entityHelper.getEntity(fieldAccess.field()));
				Node rhs = processValueExpression(context, expr.rhs());
				Node storeNode = construction.newStore(construction.getCurrentMem(), member, rhs);
				construction.setCurrentMem(construction.newProj(storeNode, Mode.getM(), Store.pnM));
				yield rhs;
			}
			case SArrayAccessExpression arrayAccess -> {
				Node target = computeArrayAccessTarget(context, arrayAccess);
				Node rhs = processValueExpression(context, expr.rhs());
				Node arrayStore = construction.newStore(construction.getCurrentMem(), target, rhs);
				construction.setCurrentMem(construction.newProj(arrayStore, Mode.getM(), Store.pnM));
				yield rhs;
			}
			default -> throw new InternalCompilerException("unexpected lhs in assignment. Got " + expr.lhs());
		};
	}

	private Node computeArrayAccessTarget(Context context, SArrayAccessExpression expr) {
		Construction construction = context.construction();
		// We need to evaluate the reference before the index
		// https://docs.oracle.com/javase/specs/jls/se17/html/jls-15.html#jls-15.26.1
		Node arrayNode = processValueExpression(context, expr.expression());
		Node indexNode = processValueExpression(context, expr.index());

		Type innerType = typeHelper.getType(expr.type());
		Node typeSizeNode = construction.newConst(innerType.getSize(), Mode.getLs());
		Node offsetNode = construction.newMul(construction.newConv(indexNode, Mode.getLs()), typeSizeNode);
		return construction.newAdd(arrayNode, offsetNode);
	}

	private void processLogicalOr(Context context, SBinaryOperatorExpression expr, JumpTarget jumpTarget) {
		Block falseBlock = context.construction().newBlock();
		JumpTarget newJumpTarget = new JumpTarget(jumpTarget.trueBlock(), falseBlock);
		processLogicalExpression(context, expr.lhs(), newJumpTarget);
		falseBlock.mature();
		context.construction().setCurrentBlock(falseBlock);
		processLogicalExpression(context, expr.rhs(), jumpTarget);
	}

	private void processLogicalAnd(Context context, SBinaryOperatorExpression expr, JumpTarget jumpTarget) {
		Block trueBlock = context.construction().newBlock();
		JumpTarget newJumpTarget = new JumpTarget(trueBlock, jumpTarget.falseBlock());
		processLogicalExpression(context, expr.lhs(), newJumpTarget);
		trueBlock.mature();
		context.construction().setCurrentBlock(trueBlock);
		processLogicalExpression(context, expr.rhs(), jumpTarget);
	}

	private void processRelation(
		Context context, SBinaryOperatorExpression expr, Relation relation, JumpTarget target
	) {
		Node left = processValueExpression(context, expr.lhs());
		Node right = processValueExpression(context, expr.rhs());
		processRelation(context, left, right, relation, target);
	}

	private void processRelation(Context context, Node left, Node right, Relation relation, JumpTarget jumpTarget) {
		Construction construction = context.construction();
		Node cmp = construction.newCmp(left, right, relation);
		Node cond = construction.newCond(cmp);
		Node trueProj = construction.newProj(cond, Mode.getX(), Cond.pnTrue);
		Node falseProj = construction.newProj(cond, Mode.getX(), Cond.pnFalse);
		jumpTarget.trueBlock().addPred(trueProj);
		jumpTarget.falseBlock().addPred(falseProj);
	}

	private Node processBooleanValue(Context context, SBooleanValueExpression expr) {
		return context.construction().newConst(expr.value() ? 1 : 0, Mode.getBu());
	}

	private Node processFieldAccess(Context context, SFieldAccessExpression expr) {
		Construction construction = context.construction();
		Node exprNode = processValueExpression(context, expr.expression());
		Node member = construction.newMember(exprNode, entityHelper.getEntity(expr.field()));
		Mode mode = typeHelper.getMode(expr.type());
		Node load = construction.newLoad(construction.getCurrentMem(), member, mode);
		construction.setCurrentMem(construction.newProj(load, Mode.getM(), Load.pnM));
		return construction.newProj(load, mode, Load.pnRes);
	}

	private Node processIntegerValue(Context context, SIntegerValueExpression expr) {
		return context.construction().newConst(expr.value(), Mode.getIs());
	}

	private Node processLocalVariable(Context context, SLocalVariableExpression expr) {
		LocalVariableDeclaration variable = expr.localVariable();

		int index = context.slotTable().computeIndex(variable);
		Mode mode = typeHelper.getMode(expr.localVariable().type());

		return context.construction().getVariable(index, mode);
	}

	private Node processMethodInvocation(Context context, SMethodInvocationExpression expr) {
		Construction construction = context.construction();
		// only non-static methods can be invoked, so we can always add 1 for the receiver
		int argumentSize = expr.arguments().size() + 1;
		Node[] fArguments = new Node[argumentSize];
		fArguments[0] = processValueExpression(context, expr.expression());
		List<SExpression> sArguments = expr.arguments();
		for (int i = 0; i < sArguments.size(); i++) {
			SExpression argument = sArguments.get(i);
			fArguments[i + 1] = processValueExpression(context, argument);
		}
		SMethod method = expr.method();
		Entity methodEntity = entityHelper.computeMethodEntity(method);
		Node address = construction.newAddress(methodEntity);
		Node call = construction.newCall(construction.getCurrentMem(), address, fArguments, methodEntity.getType());
		Node proj = construction.newProj(call, Mode.getT(), Call.pnTResult);
		construction.setCurrentMem(construction.newProj(call, Mode.getM(), Call.pnM));
		if (method.returnType() instanceof SVoidType) {
			return construction.newBad(Mode.getANY());
		}
		// 0 as we only have one return element
		return construction.newProj(proj, typeHelper.getMode(method.returnType().asExprType()), 0);
	}

	private Node processNewArray(Context context, SNewArrayExpression expr) {
		Type type = typeHelper.getType(expr.type()
			.withDecrementedLevel()
			.orElseThrow(
				() -> new InternalCompilerException("array type " + expr.type() + " could not be decremented")));

		int typeSize = type.getSize();
		Construction construction = context.construction();

		Node memberCount = construction.newConv(processValueExpression(context, expr.size()), Mode.getLu());

		return allocateMemory(construction, typeSize, memberCount);
	}

	private Node processNewObject(Context context, SNewObjectExpression expr) {
		Construction construction = context.construction();
		ClassType type = typeHelper.getClassType(expr.classDecl());

		return allocateMemory(construction, type.getSize(), construction.newConst(1, Mode.getLu()));
	}

	private Node allocateMemory(
		Construction construction, int memberSize, Node memberCount
	) {
		Entity allocateEntity = entityHelper.getEntity(StdLibEntity.ALLOCATE);
		Node allocateAddress = construction.newAddress(allocateEntity);

		Node[] arguments = {memberCount, construction.newConst(memberSize, Mode.getLu())};
		Node call =
			construction.newCall(construction.getCurrentMem(), allocateAddress, arguments, allocateEntity.getType());

		construction.setCurrentMem(construction.newProj(call, Mode.getM(), Call.pnM));
		Node proj = construction.newProj(call, Mode.getT(), Call.pnTResult);
		return construction.newProj(proj, Mode.getP(), 0);
	}

	private Node processNull(Context context) {
		return context.construction().newConst(0, Mode.getP());
	}

	private Node processSystemInRead(Context context) {
		Construction construction = context.construction();
		Entity getCharEntity = entityHelper.getEntity(StdLibEntity.GETCHAR);
		Node getCharAddress = construction.newAddress(getCharEntity);
		var call =
			construction.newCall(construction.getCurrentMem(), getCharAddress, new Node[]{}, getCharEntity.getType());
		construction.setCurrentMem(construction.newProj(call, Mode.getM(), Call.pnM));
		Node proj = construction.newProj(call, Mode.getT(), Call.pnTResult);
		return construction.newProj(proj, Mode.getIs(), 0);
	}

	private Node processSystemOutFlush(Context context) {
		Construction construction = context.construction();
		Entity flushEntity = entityHelper.getEntity(StdLibEntity.FLUSH);
		Node flushAddress = construction.newAddress(flushEntity);
		var call = construction.newCall(construction.getCurrentMem(), flushAddress, new Node[0],
			flushEntity.getType());
		construction.setCurrentMem(construction.newProj(call, Mode.getM(), Call.pnM));
		return construction.newBad(Mode.getANY());
	}

	private Node processSystemOutPrintln(Context context, SSystemOutPrintlnExpression expr) {
		return doSysout(context, expr.argument(), StdLibEntity.PRINTLN);
	}

	private Node processSystemOutWrite(Context context, SSystemOutWriteExpression expr) {
		return doSysout(context, expr.argument(), StdLibEntity.PUTCHAR);
	}

	private Node doSysout(Context context, SExpression argument, StdLibEntity entity) {
		if (entity != StdLibEntity.PUTCHAR && entity != StdLibEntity.PRINTLN) {
			throw new InternalCompilerException("expected PUTCHAR or PRINTLN, got " + entity);
		}

		Construction construction = context.construction();
		Entity putCharEntity = entityHelper.getEntity(entity);
		Node putCharAddress = construction.newAddress(putCharEntity);

		Node argumentNode = processValueExpression(context, argument);

		var call = construction.newCall(construction.getCurrentMem(), putCharAddress, new Node[]{argumentNode},
			putCharEntity.getType());

		construction.setCurrentMem(construction.newProj(call, Mode.getM(), Call.pnM));
		return construction.newBad(Mode.getANY());
	}

	private Node processThis(Context context) {
		return context.construction().getVariable(0, Mode.getP());
	}

	private void processLogicalUnaryOperator(Context context, SUnaryOperatorExpression expr, JumpTarget jumpTarget) {
		Construction construction = context.construction();
		switch (expr.operator()) {
			case NEGATION -> construction.newMinus(processValueExpression(context, expr.expression()));
			case LOGICAL_NOT -> {
				JumpTarget invertedJumpTarget = new JumpTarget(jumpTarget.falseBlock(), jumpTarget.trueBlock());
				processLogicalExpression(context, expr.expression(), invertedJumpTarget);
			}
		}
	}

	private Node processUnaryOperator(Context context, SUnaryOperatorExpression expr) {
		Construction construction = context.construction();
		return switch (expr.operator()) {
			case NEGATION -> construction.newMinus(processValueExpression(context, expr.expression()));
			case LOGICAL_NOT -> {
				// !b => (b == false)
				Node innerExpr = processValueExpression(context, expr.expression());
				Node constFalse = construction.newConst(0, Mode.getBu());
				yield condToBool(context,
					target -> processRelation(context, innerExpr, constFalse, Relation.Equal, target));
			}
		};
	}

	private void layoutClasses(Namespace<SClassDeclaration> classes) {
		for (SClassDeclaration declaration : classes.getAll()) {
			for (SField field : declaration.fields().getAll()) {
				entityHelper.setFieldEntity(field);
			}
			typeHelper.getClassType(declaration).layoutFields();
			typeHelper.getClassType(declaration).finishLayout();
		}
	}

	private record Context(
		Construction construction,
		SlotTable slotTable,
		Set<Block> returningBlocks,
		SMethod currentMethod
	) {

		public Context(Construction construction, SlotTable slotTable, SMethod method) {
			this(construction, slotTable, new HashSet<>(), method);
		}

		public void setReturns(Block block) {
			returningBlocks.add(block);
		}

		public boolean isReturning(Block block) {
			return returningBlocks.contains(block);
		}
	}

	private record JumpTarget(
		Block trueBlock,
		Block falseBlock
	) {
	}
}
