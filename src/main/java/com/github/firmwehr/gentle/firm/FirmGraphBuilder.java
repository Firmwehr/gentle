package com.github.firmwehr.gentle.firm;

import com.github.firmwehr.gentle.parser.ast.expression.BinaryOperator;
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
			construction.setCurrentMem(construction.newProj(startNode, Mode.getM(), Start.pnM));
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

		processMethodBody(new Context(construction, slotTable), method);

		Dump.dumpGraph(currentGraph, "before-mature");

		construction.finish();

		Dump.dumpGraph(currentGraph, "after-mature");
	}

	private void processMethodBody(Context context, SMethod method) {
		List<SStatement> body = method.body();
		for (SStatement statement : body) {
			processStatement(context, statement);
		}
		if (method.returnType() instanceof SVoidType) {
			if (body.isEmpty() || !(body.get(body.size() - 1) instanceof SReturnStatement)) {
				processStatement(context, new SReturnStatement(Optional.empty(), SourceSpan.dummy()));
			}
		}
		// TODO implicit return?
	}

	private void processStatement(Context context, SStatement statement) {
		switch (statement) {
			case SBlock block -> block.statements().forEach(s -> processStatement(context, s));
			case SExpressionStatement expressionStatement -> processExpression(context,
				expressionStatement.expression()); // TODO build block
			case SIfStatement ifStatement -> processIf(context, ifStatement);
			case SReturnStatement returnStatement -> processReturn(context, returnStatement);
			case SWhileStatement whileStatement -> processWhile(context, whileStatement);
		}
	}

	private void processWhile(Context context, SWhileStatement whileStatement) {
		Construction construction = context.construction();
		Block header = construction.newBlock();
		Block body = construction.newBlock();
		Block after = construction.newBlock();

		jumpIfNotReturning(context, header);
		construction.setCurrentBlock(header);

		Context newContext = context.withJumpTarget(body, after);
		processCondition(newContext, whileStatement.condition());

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
		Context newContext = context.withJumpTarget(trueBlock, falseBlock);
		processCondition(newContext, ifStatement.condition());
		trueBlock.mature();
		falseBlock.mature();

		construction.setCurrentBlock(trueBlock);
		processStatement(context, ifStatement.body());
		jumpIfNotReturning(context, afterBlock);

		if (ifStatement.elseBody().isPresent()) {
			construction.setCurrentBlock(falseBlock);
			processStatement(context, ifStatement.elseBody().get());
			jumpIfNotReturning(context, afterBlock);
		} else {
			construction.setCurrentBlock(falseBlock);
			jumpIfNotReturning(context, afterBlock);
		}

		construction.setCurrentBlock(afterBlock);
		afterBlock.mature();
	}

	private void processReturn(Context context, SReturnStatement returnStatement) {
		Node[] returnValues = new Node[0];
		if (returnStatement.returnValue().isPresent()) {
			returnValues = new Node[]{processExpression(context, returnStatement.returnValue().get())};
		}
		Construction construction = context.construction();
		Node returnNode = construction.newReturn(construction.getCurrentMem(), returnValues);
		construction.getGraph().getEndBlock().addPred(returnNode);
		context.setReturns(construction.getCurrentBlock());
	}

	private Node processExpression(Context context, SExpression expression) {
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

	private Node processCondition(Context context, SExpression expression) {
		Node node = processExpression(context, expression);
		Optional<JumpTarget> jumpTarget = context.jumpTarget();
		if (node.getMode().isInt() && node.getOpCode() != binding_irnode.ir_opcode.iro_Bad && jumpTarget.isPresent()) {
			Node right = context.construction().newConst(0, Mode.getBu());
			Context invertedContext =
				context.withJumpTarget(jumpTarget.get().falseBlock(), jumpTarget.get().trueBlock());
			return processRelation(invertedContext, node, right, Relation.Equal);
		}
		return node;
	}

	private Node processArrayAccess(Context context, SArrayAccessExpression expr) {
		Construction construction = context.construction();
		Node target = computeArrayAccessTarget(context, expr);
		Mode innerMode = typeHelper.getMode(expr.type());
		Node loadNode = construction.newLoad(construction.getCurrentMem(), target, innerMode);
		construction.setCurrentMem(construction.newProj(loadNode, Mode.getM(), Load.pnM));
		return construction.newProj(loadNode, innerMode, Load.pnRes);
	}

	private Node processBinaryOperator(Context context, SBinaryOperatorExpression expr) {
		Construction construction = context.construction();
		if (expr.operator() != BinaryOperator.ASSIGN && expr.type().asBooleanType().isPresent() &&
			context.jumpTarget().isEmpty()) {
			Block trueBlock = construction.newBlock();
			Block falseBlock = construction.newBlock();
			Block afterBlock = construction.newBlock();

			Context newContext = context.withJumpTarget(trueBlock, falseBlock);
			processBinaryOperator(newContext, expr);
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
				new Node[]{construction.newConst(0, Mode.getBu()), construction.newConst(1, Mode.getBu())},
				Mode.getBu());
		}
		return switch (expr.operator()) {
			case ASSIGN -> processAssignment(context, expr);
			case LOGICAL_OR -> processLogicalOr(context, expr);
			case LOGICAL_AND -> processLogicalAnd(context, expr);
			case EQUAL -> processRelation(context, expr, Relation.Equal);
			case NOT_EQUAL -> processRelation(context, expr, Relation.UnorderedLessGreater);
			case LESS_THAN -> processRelation(context, expr, Relation.Less);
			case LESS_OR_EQUAL -> processRelation(context, expr, Relation.LessEqual);
			case GREATER_THAN -> processRelation(context, expr, Relation.Greater);
			case GREATER_OR_EQUAL -> processRelation(context, expr, Relation.GreaterEqual);
			case ADD -> construction.newAdd(processExpression(context, expr.lhs()),
				processExpression(context, expr.rhs()));
			case SUBTRACT -> construction.newSub(processExpression(context, expr.lhs()),
				processExpression(context, expr.rhs()));
			case MULTIPLY -> construction.newMul(processExpression(context, expr.lhs()),
				processExpression(context, expr.rhs()));
			case DIVIDE -> {
				Node divNode = construction.newDiv(construction.getCurrentMem(), processExpression(context,
						expr.lhs()),
					processExpression(context, expr.rhs()), binding_ircons.op_pin_state.op_pin_state_pinned);
				construction.setCurrentMem(construction.newProj(divNode, Mode.getM(), Div.pnM));
				yield construction.newProj(divNode, Mode.getIs(), Div.pnRes);
			}
			case MODULO -> {
				Node modNode = construction.newMod(construction.getCurrentMem(), processExpression(context,
						expr.lhs()),
					processExpression(context, expr.rhs()), binding_ircons.op_pin_state.op_pin_state_pinned);
				construction.setCurrentMem(construction.newProj(modNode, Mode.getM(), Mod.pnM));
				yield construction.newProj(modNode, Mode.getIs(), Mod.pnRes);
			}
		};
	}

	private void assertNotReturning(Context context, Block block) {
		if (context.isReturning(block)) {
			throw new IllegalStateException("Block should not return " + block);
		}
	}

	private Node processAssignment(Context context, SBinaryOperatorExpression expr) {
		Node rhs = processExpression(context, expr.rhs());
		Construction construction = context.construction();
		return switch (expr.lhs()) {
			case SLocalVariableExpression localVar -> {
				int index = context.slotTable().computeIndex(localVar.localVariable());
				construction.setVariable(index, rhs);
				yield rhs;
			}
			case SFieldAccessExpression fieldAccess -> {
				Node member = construction.newMember(processExpression(context, fieldAccess.expression()),
					entityHelper.getEntity(fieldAccess.field()));
				Node storeNode = construction.newStore(construction.getCurrentMem(), member, rhs);
				construction.setCurrentMem(construction.newProj(storeNode, Mode.getM(), Store.pnM));
				yield rhs;
			}
			case SArrayAccessExpression arrayAccess -> {
				Node target = computeArrayAccessTarget(context, arrayAccess);
				Node arrayStore = construction.newStore(construction.getCurrentMem(), target, rhs);
				construction.setCurrentMem(construction.newProj(arrayStore, Mode.getM(), Store.pnM));
				yield rhs;
			}
			default -> throw new IllegalStateException("unexpected lhs " + expr.lhs());
		};
	}

	private Node computeArrayAccessTarget(Context context, SArrayAccessExpression expr) {
		Construction construction = context.construction();
		Node arrayNode = processExpression(context, expr.expression());
		Node indexNode = processExpression(context, expr.index());

		Type innerType = typeHelper.getType(expr.type());
		Node typeSizeNode = construction.newConst(innerType.getSize(), Mode.getLs());
		Node offsetNode = construction.newMul(construction.newConv(indexNode, Mode.getLs()), typeSizeNode);
		return construction.newAdd(arrayNode, offsetNode);
	}

	private Node processLogicalOr(Context context, SBinaryOperatorExpression expr) {
		JumpTarget jumpTarget = context.jumpTarget().orElseThrow(); // TODO exception
		Block falseBlock = context.construction().newBlock();
		Context newContext = context.withJumpTarget(jumpTarget.trueBlock(), falseBlock);
		processCondition(newContext, expr.lhs());
		falseBlock.mature();
		context.construction().setCurrentBlock(falseBlock);
		processCondition(context, expr.rhs());
		return context.construction().newBad(Mode.getANY());
	}

	private Node processLogicalAnd(Context context, SBinaryOperatorExpression expr) {
		JumpTarget jumpTarget = context.jumpTarget().orElseThrow(); // TODO exception
		Block trueBlock = context.construction().newBlock();
		Context newContext = context.withJumpTarget(trueBlock, jumpTarget.falseBlock());
		processCondition(newContext, expr.lhs());
		trueBlock.mature();
		context.construction().setCurrentBlock(trueBlock);
		processCondition(context, expr.rhs());
		return context.construction().newBad(Mode.getANY());
	}

	private Node processRelation(Context context, SBinaryOperatorExpression expr, Relation relation) {
		Node left = processExpression(context, expr.lhs());
		Node right = processExpression(context, expr.rhs());
		return processRelation(context, left, right, relation);
	}

	private Node processRelation(Context context, Node left, Node right, Relation relation) {
		Construction construction = context.construction();
		Node cmp = construction.newCmp(left, right, relation);
		Node cond = construction.newCond(cmp);
		JumpTarget jumpTarget = context.jumpTarget().orElseThrow(); // TODO exception
		Node trueProj = construction.newProj(cond, Mode.getX(), Cond.pnTrue);
		Node falseProj = construction.newProj(cond, Mode.getX(), Cond.pnFalse);
		jumpTarget.trueBlock().addPred(trueProj);
		jumpTarget.falseBlock().addPred(falseProj);
		return construction.newBad(Mode.getANY());
	}

	private Node processBooleanValue(Context context, SBooleanValueExpression expr) {
		return context.construction().newConst(expr.value() ? 1 : 0, Mode.getBu());
	}

	private Node processFieldAccess(Context context, SFieldAccessExpression expr) {
		Construction construction = context.construction();
		Node exprNode = processExpression(context, expr.expression());
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
		fArguments[0] = processExpression(context, expr.expression());
		List<SExpression> sArguments = expr.arguments();
		for (int i = 0; i < sArguments.size(); i++) {
			SExpression argument = sArguments.get(i);
			fArguments[i + 1] = processExpression(context, argument);
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
		// FIXME array size 0
		// FIXME calloc
		Type type = typeHelper.getType(expr.type().withDecrementedLevel().orElseThrow()); // TODO error handling
		int size = type.getSize();
		Construction construction = context.construction();
		Entity mallocEntity = entityHelper.getEntity(StdLibEntity.MALLOC);
		Node mallocAddress = construction.newAddress(mallocEntity);
		Node sizeConst = construction.newConst(size, Mode.getLu());
		Node arraySize =
			construction.newMul(sizeConst, construction.newConv(processExpression(context, expr.size()),
				Mode.getLu()));
		Node call = construction.newCall(construction.getCurrentMem(), mallocAddress, new Node[]{arraySize},
			mallocEntity.getType());
		construction.setCurrentMem(construction.newProj(call, Mode.getM(), Call.pnM));
		Node proj = construction.newProj(call, Mode.getT(), Call.pnTResult);
		return construction.newProj(proj, Mode.getP(), 0);
	}

	private Node processNewObject(Context context, SNewObjectExpression expr) {
		Construction construction = context.construction();
		ClassType type = typeHelper.getClassType(expr.classDecl());
		// malloc returns null if called with zero bytes (class without fields)
		int size = Math.max(1, type.getSize());
		Entity mallocEntity = entityHelper.getEntity(StdLibEntity.MALLOC);
		Node mallocAddress = construction.newAddress(mallocEntity);
		Node sizeConst = construction.newConst(size, Mode.getLu());
		Node call = construction.newCall(construction.getCurrentMem(), mallocAddress, new Node[]{sizeConst},
			mallocEntity.getType());
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
		Node stdOutAddress = construction.newAddress(entityHelper.getEntity(StdLibEntity.STDOUT));
		Entity fflushEntity = entityHelper.getEntity(StdLibEntity.FFLUSH);
		Node fflushAddress = construction.newAddress(fflushEntity);
		Node stdOutLoad = construction.newLoad(construction.getCurrentMem(), stdOutAddress, Mode.getP());
		construction.setCurrentMem(construction.newProj(stdOutLoad, Mode.getM(), Load.pnM));
		Node stdOutResult = construction.newProj(stdOutLoad, Mode.getP(), Load.pnRes);
		var call = construction.newCall(construction.getCurrentMem(), fflushAddress, new Node[]{stdOutResult},
			fflushEntity.getType());
		construction.setCurrentMem(construction.newProj(call, Mode.getM(), Call.pnM));
		return construction.newBad(Mode.getANY());
	}

	private Node processSystemOutPrintln(Context context, SSystemOutPrintlnExpression expr) {
		Construction construction = context.construction();
		Entity printlnEntity = entityHelper.getEntity(StdLibEntity.PRINTLN);
		Node printlnAddress = construction.newAddress(printlnEntity);
		Node argument = processExpression(context, expr.argument());
		var call = construction.newCall(construction.getCurrentMem(), printlnAddress, new Node[]{argument},
			printlnEntity.getType());
		construction.setCurrentMem(construction.newProj(call, Mode.getM(), Call.pnM));
		return construction.newBad(Mode.getANY());
	}

	private Node processSystemOutWrite(Context context, SSystemOutWriteExpression expr) {
		Construction construction = context.construction();
		Entity putCharEntity = entityHelper.getEntity(StdLibEntity.PUTCHAR);
		Node putCharAddress = construction.newAddress(putCharEntity);
		Node argument = processExpression(context, expr.argument());
		var call = construction.newCall(construction.getCurrentMem(), putCharAddress, new Node[]{argument},
			putCharEntity.getType());
		construction.setCurrentMem(construction.newProj(call, Mode.getM(), Call.pnM));
		return construction.newBad(Mode.getANY());
	}

	private Node processThis(Context context) {
		return context.construction().getVariable(0, Mode.getP());
	}

	private Node processUnaryOperator(Context context, SUnaryOperatorExpression expr) {
		Construction construction = context.construction();
		return switch (expr.operator()) {
			case NEGATION -> construction.newMinus(processExpression(context, expr.expression()));
			case LOGICAL_NOT -> {
				JumpTarget jumpTarget = context.jumpTarget().orElseThrow(); // TODO exception
				Context invertedContext = context.withJumpTarget(jumpTarget.falseBlock(), jumpTarget.trueBlock());
				yield processCondition(invertedContext, expr.expression());

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
		Optional<JumpTarget> jumpTarget,
		Set<Block> returningBlocks
	) {

		public Context(Construction construction, SlotTable slotTable) {
			this(construction, slotTable, Optional.empty(), new HashSet<>());
		}

		public Context withJumpTarget(Block trueBlock, Block falseBlock) {
			return new Context(construction(), slotTable(), Optional.of(new JumpTarget(trueBlock, falseBlock)),
				returningBlocks());
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
