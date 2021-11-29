package com.github.firmwehr.gentle.firm.construction;

import com.github.firmwehr.gentle.semantic.ast.LocalVariableDeclaration;
import com.github.firmwehr.gentle.semantic.ast.SField;
import com.github.firmwehr.gentle.semantic.ast.SMethod;
import com.github.firmwehr.gentle.semantic.ast.basictype.SIntType;
import com.github.firmwehr.gentle.semantic.ast.type.SVoidType;
import firm.ClassType;
import firm.Entity;
import firm.MethodType;
import firm.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * In firm most things identified by names (fields and methods in Gentle currently) are {@link Entity entities}. To use
 * our own field and method declarations in the firm graph we need to translate them to firm entities first.
 * <br>
 * <p>This class converts fields and methods to singleton firm entities,</p>
 */
public class EntityHelper {
	private final Map<SField, Entity> fieldEntities;
	private final Map<SMethod, Entity> methodEntities;
	private final TypeHelper typeHelper;

	public EntityHelper(TypeHelper typeHelper) {
		this.typeHelper = typeHelper;
		this.fieldEntities = new HashMap<>();
		this.methodEntities = new HashMap<>();
	}


	public void setFieldEntity(SField field) {
		ClassType ownerType = typeHelper.getClassType(field.classDecl());
		Type ownType = typeHelper.getType(field.type());
		fieldEntities.put(field, new Entity(ownerType, field.name().ident(), ownType));
	}

	public Entity getEntity(SField field) {
		return fieldEntities.get(field);
	}

	public Entity getEntity(StdLibEntity entity) {
		return entity.getEntity();
	}

	public Entity computeMethodEntity(SMethod method) {
		return this.methodEntities.computeIfAbsent(method, this::createMethodEntity);
	}

	private Entity createMethodEntity(SMethod method) {
		ClassType ownerType = typeHelper.getClassType(method.classDecl());
		List<Type> typesList = method.parameters()
			.stream()
			.map(LocalVariableDeclaration::type)
			.map(typeHelper::getType)
			.collect(Collectors.toCollection(ArrayList::new));

		if (!method.isStatic()) {
			typesList.add(0, typeHelper.getType(method.classDecl().type()));
		}
		Type[] types = typesList.toArray(Type[]::new);
		Type[] returnType = new Type[0];
		if (!(method.returnType() instanceof SVoidType)) {
			returnType = new Type[]{typeHelper.getType(method.returnType().asExprType())};
		}

		if (method.isStatic()) {
			returnType = new Type[]{typeHelper.getType(new SIntType())};
		}

		MethodType methodType = new MethodType(types, returnType);
		Entity entity = new Entity(ownerType, method.name().ident(), methodType);
		if (!method.isStatic()) {
			entity.setLdIdent(method.classDecl().name().ident() + "_" + method.name().ident());
		}
		return entity;
	}
}
