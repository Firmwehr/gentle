package com.github.firmwehr.gentle.firm.construction;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.debug.DebugInfoAllocate.AllocateElementType;
import com.github.firmwehr.gentle.debug.DebugInfoArrayAccessTarget.ArrayAccessTargetType;
import com.github.firmwehr.gentle.debug.DebugInfoCompare.CompareElementType;
import com.github.firmwehr.gentle.debug.DebugInfoCondToBoolBlock.CondToBoolBlockType;
import com.github.firmwehr.gentle.debug.DebugInfoFieldAccess.FieldAccessElementType;
import com.github.firmwehr.gentle.debug.DebugInfoIfBlock.IfBlockType;
import com.github.firmwehr.gentle.debug.DebugInfoImplicitMainReturn;
import com.github.firmwehr.gentle.debug.DebugInfoMethodInvocation.MethodInvocationElementType;
import com.github.firmwehr.gentle.debug.DebugInfoShortCircuitBlock.ShortCircuitBlockType;
import com.github.firmwehr.gentle.debug.DebugInfoWhileBlock.WhileBlockType;
import com.github.firmwehr.gentle.debug.DebugStore;
import com.github.firmwehr.gentle.debug.HasDebugInformation;
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
import firm.nodes.Const;
import firm.nodes.Div;
import firm.nodes.Load;
import firm.nodes.Mod;
import firm.nodes.Node;
import firm.nodes.Store;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static com.github.firmwehr.gentle.debug.FirmNodeMetadata.forAllocate;
import static com.github.firmwehr.gentle.debug.FirmNodeMetadata.forArrayAccessTarget;
import static com.github.firmwehr.gentle.debug.FirmNodeMetadata.forCompare;
import static com.github.firmwehr.gentle.debug.FirmNodeMetadata.forCondToBoolBlock;
import static com.github.firmwehr.gentle.debug.FirmNodeMetadata.forCondToBoolPhi;
import static com.github.firmwehr.gentle.debug.FirmNodeMetadata.forElement;
import static com.github.firmwehr.gentle.debug.FirmNodeMetadata.forFieldLoad;
import static com.github.firmwehr.gentle.debug.FirmNodeMetadata.forIfBlock;
import static com.github.firmwehr.gentle.debug.FirmNodeMetadata.forMethodInvocation;
import static com.github.firmwehr.gentle.debug.FirmNodeMetadata.forShortCircuitBlock;
import static com.github.firmwehr.gentle.debug.FirmNodeMetadata.forWhileBlock;

public class FirmGraphBuilder {

	private final TypeHelper typeHelper;
	private final EntityHelper entityHelper;
	private final DebugStore debugStore;

	private final Map<ConstNodeKey, Const> constCache = new HashMap<>();

	/**
	 * Wrapper around Const nodes to perform identity checks
	 */
	private record ConstNodeKey(
		int tarval,
		Mode mode
	) {
	}

	public FirmGraphBuilder(DebugStore debugStore) {
		this.debugStore = debugStore;
		this.typeHelper = new TypeHelper();
		this.entityHelper = new EntityHelper(typeHelper);
	}

	public void buildGraph(SProgram program) {
		layoutClasses(program.classes());
		for (SClassDeclaration declaration : program.classes().getAll()) {
			processClass(declaration);
		}
	}

	/**
	 * This method wraps around the {@link Construction#newConst(int, Mode)} call to perform Const node caching. While
	 * saving ressources, it's primary purpose is to reduce the cost of global value numbering in later stages. There
	 * are still cases in which new Const nodes are created and one should not really on pointer comparisons, when
	 * dealing with Const nodes.
	 *
	 * @param construction The construction instance in which the new Const node is to be created.
	 * @param value The value of the constant.
	 * @param mode The constant mode.
	 *
	 * @return A newly created Const node or an already created one from an earlier call.
	 */
	private Node newConst(Construction construction, int value, Mode mode) {
		return constCache.computeIfAbsent(new ConstNodeKey(value, mode),
			k -> (Const) construction.newConst(value, mode));
	}

	private void processClass(SClassDeclaration declaration) {
		for (SMethod method : declaration.methods().getAll()) {
			processMethod(method);
		}
	}

	private void processMethod(SMethod method) {
		SlotTable slotTable = SlotTable.forMethod(method);

		Entity entity = this.entityHelper.computeMethodEntity(method);

		// wipe const node cache, since nodes are only valid in their own graph
		constCache.clear();

		Graph currentGraph = new Graph(entity, slotTable.size());
		Construction construction = new Construction(currentGraph);

		if (!method.isStatic()) {
			Node argsTuple = currentGraph.getArgs();

			// the implicit receiver parameter is at pos 0 and needs to be handled separately
			Node thisProj = construction.newProj(argsTuple, typeHelper.getMode(method.classDecl().type()), 0);
			construction.setVariable(0, thisProj);

			List<LocalVariableDeclaration> parameters = method.parameters();
			for (LocalVariableDeclaration parameter : parameters) {
				var index = slotTable.computeIndex(parameter);

				Node proj = construction.newProj(argsTuple, typeHelper.getMode(parameter.type()), index);
				construction.setVariable(index, proj);
				debugStore.putMetadata(proj, forElement(parameter));
			}
		}

		processMethodBody(new Context(construction, slotTable, method), method);

		construction.finish();

		GraphDumper.dumpGraph(currentGraph, "after-mature");
	}

	private void processMethodBody(Context context, SMethod method) {
		List<SStatement> body = method.body();
		processBlock(context, body);

		if (!method.isStatic() && !(method.returnType() instanceof SVoidType)) {
			return;
		}

		if (!context.isReturning(context.construction().getCurrentBlock())) {
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

		debugStore.putMetadata(header, forWhileBlock(whileStatement, WhileBlockType.HEADER));
		debugStore.putMetadata(body, forWhileBlock(whileStatement, WhileBlockType.BODY));
		debugStore.putMetadata(after, forWhileBlock(whileStatement, WhileBlockType.AFTER));
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

		debugStore.putMetadata(afterBlock, forIfBlock(ifStatement, IfBlockType.AFTER));
		debugStore.putMetadata(trueBlock, forIfBlock(ifStatement, IfBlockType.TRUE));
		debugStore.putMetadata(falseBlock, forIfBlock(ifStatement, IfBlockType.FALSE));
	}

	private void processReturn(Context context, SReturnStatement returnStatement) {
		Node[] returnValues = new Node[0];
		if (returnStatement.returnValue().isPresent()) {
			returnValues = new Node[]{processValueExpression(context, returnStatement.returnValue().get())};
		}
		if (context.currentMethod().isStatic()) {
			Node zeroReturn = processValueExpression(context, new SIntegerValueExpression(0, SourceSpan.dummy()));
			debugStore.putMetadata(zeroReturn, forElement(new DebugInfoImplicitMainReturn()));
			returnValues = new Node[]{zeroReturn};
		}
		Construction construction = context.construction();
		// Arguments need to be evaluated first so memory chain is built correctly
		Node returnNode = construction.newReturn(construction.getCurrentMem(), returnValues);
		construction.getGraph().getEndBlock().addPred(returnNode);
		context.setReturns(construction.getCurrentBlock());

		debugStore.putMetadata(returnNode, forElement(returnStatement));
	}

	private void processLogicalExpression(Context context, SExpression expression, JumpTarget jumpTarget) {
		switch (expression) {
			case SBinaryOperatorExpression expr -> processLogicalBinaryOperator(context, expr, jumpTarget);
			case SUnaryOperatorExpression expr -> processLogicalUnaryOperator(context, expr, jumpTarget);
			default -> booleanToJump(context, processValueExpression(context, expression), jumpTarget, expression);
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
			case SNullExpression nullExpression -> processNull(context, nullExpression);
			case SSystemInReadExpression ignored -> processSystemInRead(context);
			case SSystemOutFlushExpression ignored -> processSystemOutFlush(context);
			case SSystemOutPrintlnExpression expr -> processSystemOutPrintln(context, expr);
			case SSystemOutWriteExpression expr -> processSystemOutWrite(context, expr);
			case SThisExpression thisExpression -> processThis(context, thisExpression);
			case SUnaryOperatorExpression expr -> processUnaryOperator(context, expr);
		};
	}

	private void booleanToJump(Context context, Node node, JumpTarget jumpTarget, SExpression source) {
		Preconditions.checkArgument(node.getMode().isInt(), "Expected boolean literal (Bu), got " + node);

		Node right = newConst(context.construction(), 0, Mode.getBu());
		JumpTarget invertedTarget = new JumpTarget(jumpTarget.falseBlock(), jumpTarget.trueBlock());

		processRelation(context, node, right, Relation.Equal, invertedTarget, source);
	}

	private Node processArrayAccess(Context context, SArrayAccessExpression expr) {
		Construction construction = context.construction();
		Node target = computeArrayAccessTarget(context, expr);
		Mode innerMode = typeHelper.getMode(expr.type());
		// Arguments need to be evaluated first so memory chain is built correctly
		Node loadNode = construction.newLoad(construction.getCurrentMem(), target, innerMode);
		construction.setCurrentMem(construction.newProj(loadNode, Mode.getM(), Load.pnM));
		Node resultProj = construction.newProj(loadNode, innerMode, Load.pnRes);

		debugStore.putMetadata(target, forElement(expr));
		debugStore.putMetadata(resultProj, forElement(expr));

		return resultProj;
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
			case LOGICAL_OR -> condToBool(context, target -> processLogicalOr(context, expr, target), expr);
			case LOGICAL_AND -> condToBool(context, target -> processLogicalAnd(context, expr, target), expr);
			case EQUAL -> condToBool(context, target -> processRelation(context, expr, Relation.Equal, target), expr);
			case NOT_EQUAL -> condToBool(context,
				target -> processRelation(context, expr, Relation.UnorderedLessGreater, target), expr);
			case LESS_THAN -> condToBool(context, target -> processRelation(context, expr, Relation.Less, target),
				expr);
			case LESS_OR_EQUAL -> condToBool(context,
				target -> processRelation(context, expr, Relation.LessEqual, target), expr);
			case GREATER_THAN -> condToBool(context, target -> processRelation(context, expr, Relation.Greater,
					target),
				expr);
			case GREATER_OR_EQUAL -> condToBool(context,
				target -> processRelation(context, expr, Relation.GreaterEqual, target), expr);
			case ADD -> construction.newAdd(processValueExpression(context, expr.lhs()),
				processValueExpression(context, expr.rhs()));
			case SUBTRACT -> construction.newSub(processValueExpression(context, expr.lhs()),
				processValueExpression(context, expr.rhs()));
			case MULTIPLY -> construction.newMul(processValueExpression(context, expr.lhs()),
				processValueExpression(context, expr.rhs()));
			case DIVIDE -> {
				Node lhs = processValueExpression(context, expr.lhs());
				Node rhs = processValueExpression(context, expr.rhs());

				// Arguments need to be evaluated first so memory chain is built correctly
				Node divNode = construction.newDiv(construction.getCurrentMem(), lhs, rhs,
					binding_ircons.op_pin_state.op_pin_state_pinned);
				construction.setCurrentMem(construction.newProj(divNode, Mode.getM(), Div.pnM));
				yield construction.newProj(divNode, Mode.getIs(), Div.pnRes);
			}
			case MODULO -> {
				Node left = processValueExpression(context, expr.lhs());
				Node right = processValueExpression(context, expr.rhs());
				// Arguments need to be evaluated first so memory chain is built correctly
				Node modNode = construction.newMod(construction.getCurrentMem(), left, right,
					binding_ircons.op_pin_state.op_pin_state_pinned);
				construction.setCurrentMem(construction.newProj(modNode, Mode.getM(), Mod.pnM));
				yield construction.newProj(modNode, Mode.getIs(), Mod.pnRes);
			}
		};
	}

	private Node condToBool(Context context, Consumer<JumpTarget> processInner, HasDebugInformation source) {
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
		Node phi = construction.newPhi(
			new Node[]{newConst(construction, 0, Mode.getBu()), newConst(construction, 1, Mode.getBu())},
			Mode.getBu());

		debugStore.putMetadata(afterBlock, forCondToBoolBlock(source, CondToBoolBlockType.AFTER));
		debugStore.putMetadata(trueBlock, forCondToBoolBlock(source, CondToBoolBlockType.TRUE));
		debugStore.putMetadata(falseBlock, forCondToBoolBlock(source, CondToBoolBlockType.FALSE));
		debugStore.putMetadata(phi, forCondToBoolPhi(source));

		return phi;
	}

	private void assertNotReturning(Context context, Block block) {
		if (context.isReturning(block)) {
			throw new InternalCompilerException("block " + block + " should not return but is marked as returning");
		}
	}

	private void processAssignment(Context context, SBinaryOperatorExpression expr, JumpTarget jumpTarget) {
		Node rhs = processAssignment(context, expr);
		booleanToJump(context, rhs, jumpTarget, expr);
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
				// Arguments need to be evaluated first so memory chain is built correctly
				Node storeNode = construction.newStore(construction.getCurrentMem(), member, rhs);
				construction.setCurrentMem(construction.newProj(storeNode, Mode.getM(), Store.pnM));
				yield rhs;
			}
			case SArrayAccessExpression arrayAccess -> {
				Node target = computeArrayAccessTarget(context, arrayAccess);
				Node rhs = processValueExpression(context, expr.rhs());
				// Arguments need to be evaluated first so memory chain is built correctly
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
		Node typeSizeNode = newConst(construction, innerType.getSize(), Mode.getLs());
		Node offsetNode = construction.newMul(construction.newConv(indexNode, Mode.getLs()), typeSizeNode);
		Node addressAdd = construction.newAdd(arrayNode, offsetNode);
		Node addressConv = construction.newConv(addressAdd, Mode.getP());

		debugStore.putMetadata(typeSizeNode, forArrayAccessTarget(expr, ArrayAccessTargetType.TYPE_SIZE));
		debugStore.putMetadata(offsetNode, forArrayAccessTarget(expr, ArrayAccessTargetType.OFFSET));
		debugStore.putMetadata(addressAdd, forArrayAccessTarget(expr, ArrayAccessTargetType.RESULT_COMPUTATION));
		debugStore.putMetadata(addressConv, forArrayAccessTarget(expr, ArrayAccessTargetType.RESULT_CONVERSION));

		return addressConv;
	}

	private void processLogicalOr(Context context, SBinaryOperatorExpression expr, JumpTarget jumpTarget) {
		Block falseBlock = context.construction().newBlock();
		JumpTarget newJumpTarget = new JumpTarget(jumpTarget.trueBlock(), falseBlock);
		processLogicalExpression(context, expr.lhs(), newJumpTarget);
		falseBlock.mature();
		context.construction().setCurrentBlock(falseBlock);
		processLogicalExpression(context, expr.rhs(), jumpTarget);

		debugStore.putMetadata(falseBlock, forShortCircuitBlock(expr, ShortCircuitBlockType.OR));
	}

	private void processLogicalAnd(Context context, SBinaryOperatorExpression expr, JumpTarget jumpTarget) {
		Block trueBlock = context.construction().newBlock();
		JumpTarget newJumpTarget = new JumpTarget(trueBlock, jumpTarget.falseBlock());
		processLogicalExpression(context, expr.lhs(), newJumpTarget);
		trueBlock.mature();
		context.construction().setCurrentBlock(trueBlock);
		processLogicalExpression(context, expr.rhs(), jumpTarget);

		debugStore.putMetadata(trueBlock, forShortCircuitBlock(expr, ShortCircuitBlockType.AND));
	}

	private void processRelation(
		Context context, SBinaryOperatorExpression expr, Relation relation, JumpTarget target
	) {
		Node left = processValueExpression(context, expr.lhs());
		Node right = processValueExpression(context, expr.rhs());
		processRelation(context, left, right, relation, target, expr);
	}

	private void processRelation(
		Context context, Node left, Node right, Relation relation, JumpTarget jumpTarget, SExpression source
	) {
		Construction construction = context.construction();
		Node cmp = construction.newCmp(left, right, relation);
		Node cond = construction.newCond(cmp);
		Node trueProj = construction.newProj(cond, Mode.getX(), Cond.pnTrue);
		Node falseProj = construction.newProj(cond, Mode.getX(), Cond.pnFalse);
		jumpTarget.trueBlock().addPred(trueProj);
		jumpTarget.falseBlock().addPred(falseProj);

		debugStore.putMetadata(cmp, forCompare(source, CompareElementType.COMPARE));
		debugStore.putMetadata(cond, forCompare(source, CompareElementType.COND));
		debugStore.putMetadata(trueProj, forCompare(source, CompareElementType.TRUE_PROJ));
		debugStore.putMetadata(falseProj, forCompare(source, CompareElementType.FALSE_PROJ));
	}

	private Node processBooleanValue(Context context, SBooleanValueExpression expr) {
		Node resultConst = newConst(context.construction(), expr.value() ? 1 : 0, Mode.getBu());

		debugStore.putMetadata(resultConst, forElement(expr));

		return resultConst;
	}

	private Node processFieldAccess(Context context, SFieldAccessExpression expr) {
		Construction construction = context.construction();
		Node exprNode = processValueExpression(context, expr.expression());
		Node member = construction.newMember(exprNode, entityHelper.getEntity(expr.field()));
		Mode mode = typeHelper.getMode(expr.type());
		// Arguments need to be evaluated first so memory chain is built correctly
		Node load = construction.newLoad(construction.getCurrentMem(), member, mode);
		construction.setCurrentMem(construction.newProj(load, Mode.getM(), Load.pnM));

		Node loadResProj = construction.newProj(load, mode, Load.pnRes);

		debugStore.putMetadata(member, forFieldLoad(expr, FieldAccessElementType.MEMBER));
		debugStore.putMetadata(load, forFieldLoad(expr, FieldAccessElementType.LOAD));
		debugStore.putMetadata(loadResProj, forFieldLoad(expr, FieldAccessElementType.LOAD_RESULT));

		return loadResProj;
	}

	private Node processIntegerValue(Context context, SIntegerValueExpression expr) {
		Node result = newConst(context.construction(), expr.value(), Mode.getIs());

		debugStore.putMetadata(result, forElement(expr));

		return result;
	}

	private Node processLocalVariable(Context context, SLocalVariableExpression expr) {
		LocalVariableDeclaration variable = expr.localVariable();

		int index = context.slotTable().computeIndex(variable);
		Mode mode = typeHelper.getMode(expr.localVariable().type());
		Node variableLoad = context.construction().getVariable(index, mode);

		debugStore.putMetadata(variableLoad, forElement(expr));

		return variableLoad;
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
		// Arguments need to be evaluated first so memory chain is built correctly
		Node call = construction.newCall(construction.getCurrentMem(), address, fArguments, methodEntity.getType());
		Node resultsProj = construction.newProj(call, Mode.getT(), Call.pnTResult);
		construction.setCurrentMem(construction.newProj(call, Mode.getM(), Call.pnM));

		debugStore.putMetadata(address, forMethodInvocation(expr, MethodInvocationElementType.ADDRESS));
		debugStore.putMetadata(call, forMethodInvocation(expr, MethodInvocationElementType.CALL));
		debugStore.putMetadata(resultsProj, forMethodInvocation(expr, MethodInvocationElementType.RESULT_PROJ));

		if (method.returnType() instanceof SVoidType) {
			return construction.newBad(Mode.getANY());
		}
		// 0 as we only have one return element
		Node resultProj = construction.newProj(resultsProj, typeHelper.getMode(method.returnType().asExprType()), 0);

		debugStore.putMetadata(resultProj, forMethodInvocation(expr, MethodInvocationElementType.RESULT_PROJ));

		return resultProj;
	}

	private Node processNewArray(Context context, SNewArrayExpression expr) {
		Type type = typeHelper.getType(expr.type()
			.withDecrementedLevel()
			.orElseThrow(
				() -> new InternalCompilerException("array type " + expr.type() + " could not be decremented")));

		int typeSize = type.getSize();
		Construction construction = context.construction();

		Node memberCount = construction.newConv(processValueExpression(context, expr.size()), Mode.getLu());

		return allocateMemory(construction, typeSize, memberCount, expr);
	}

	private Node processNewObject(Context context, SNewObjectExpression expr) {
		Construction construction = context.construction();
		ClassType type = typeHelper.getClassType(expr.classDecl());

		return allocateMemory(construction, type.getSize(), newConst(construction, 1, Mode.getLu()), expr);
	}

	private Node allocateMemory(Construction construction, int typeSize, Node memberCount, SExpression source) {
		Entity allocateEntity = entityHelper.getEntity(StdLibEntity.ALLOCATE);
		Node allocateAddress = construction.newAddress(allocateEntity);

		Node typeSizeConst = newConst(construction, typeSize, Mode.getLu());
		Node[] arguments = {memberCount, typeSizeConst};
		// Arguments need to be evaluated first so memory chain is built correctly
		Node call =
			construction.newCall(construction.getCurrentMem(), allocateAddress, arguments, allocateEntity.getType());

		construction.setCurrentMem(construction.newProj(call, Mode.getM(), Call.pnM));
		Node resultsProj = construction.newProj(call, Mode.getT(), Call.pnTResult);
		Node resultProj = construction.newProj(resultsProj, Mode.getP(), 0);

		debugStore.putMetadata(allocateAddress, forAllocate(source, AllocateElementType.ALLOCATE_ADDRESS));
		debugStore.putMetadata(typeSizeConst, forAllocate(source, AllocateElementType.TYPE_SIZE));
		debugStore.putMetadata(call, forAllocate(source, AllocateElementType.CALL));
		debugStore.putMetadata(resultsProj, forAllocate(source, AllocateElementType.RESULTS_PROJ));
		debugStore.putMetadata(resultProj, forAllocate(source, AllocateElementType.RESULT_PROJ));

		return resultProj;
	}

	private Node processNull(Context context, SNullExpression nullExpression) {
		Node result = newConst(context.construction(), 0, Mode.getP());

		debugStore.putMetadata(result, forElement(nullExpression));

		return result;
	}

	private Node processSystemInRead(Context context) {
		Construction construction = context.construction();
		Entity getCharEntity = entityHelper.getEntity(StdLibEntity.GETCHAR);
		Node getCharAddress = construction.newAddress(getCharEntity);
		// Arguments need to be evaluated first so memory chain is built correctly
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
		// Arguments need to be evaluated first so memory chain is built correctly
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

		// Arguments need to be evaluated first so memory chain is built correctly
		var call = construction.newCall(construction.getCurrentMem(), putCharAddress, new Node[]{argumentNode},
			putCharEntity.getType());

		construction.setCurrentMem(construction.newProj(call, Mode.getM(), Call.pnM));

		return construction.newBad(Mode.getANY());
	}

	private Node processThis(Context context, SThisExpression thisExpression) {
		Node result = context.construction().getVariable(0, Mode.getP());

		debugStore.putMetadata(result, forElement(thisExpression));

		return result;
	}

	private void processLogicalUnaryOperator(Context context, SUnaryOperatorExpression expr, JumpTarget jumpTarget) {
		Construction construction = context.construction();
		switch (expr.operator()) {
			case NEGATION -> {
				Node minus = construction.newMinus(processValueExpression(context, expr.expression()));

				debugStore.putMetadata(minus, forElement(expr));
			}
			case LOGICAL_NOT -> {
				JumpTarget invertedJumpTarget = new JumpTarget(jumpTarget.falseBlock(), jumpTarget.trueBlock());
				processLogicalExpression(context, expr.expression(), invertedJumpTarget);
			}
		}
	}

	private Node processUnaryOperator(Context context, SUnaryOperatorExpression expr) {
		Construction construction = context.construction();
		return switch (expr.operator()) {
			case NEGATION -> {
				Node node = construction.newMinus(processValueExpression(context, expr.expression()));

				debugStore.putMetadata(node, forElement(expr));

				yield node;
			}
			case LOGICAL_NOT -> {
				// !b => (b == false)
				Node innerExpr = processValueExpression(context, expr.expression());
				Node constFalse = newConst(construction, 0, Mode.getBu());
				yield condToBool(context,
					target -> processRelation(context, innerExpr, constFalse, Relation.Equal, target, expr), expr);
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
