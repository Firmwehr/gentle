package com.github.firmwehr.gentle.firm;

import com.github.firmwehr.gentle.semantic.ast.SClassDeclaration;
import com.github.firmwehr.gentle.semantic.ast.SField;
import firm.ClassType;
import firm.Entity;
import firm.Type;

import java.util.HashMap;
import java.util.Map;

public class EntityHelper {
	private final Map<SField, Entity> fieldEntities;
	private final TypeHelper typeHelper;

	public EntityHelper(TypeHelper typeHelper) {
		this.typeHelper = typeHelper;
		this.fieldEntities = new HashMap<>();
	}


	public void setFieldEntity(SField field, SClassDeclaration classDeclaration) {
		ClassType ownerType = typeHelper.getClassType(classDeclaration);
		Type ownType = typeHelper.getType(field.type());
		fieldEntities.put(field, new Entity(ownerType, field.name().ident(), ownType));
	}

	public Entity getEntity(SField field) {
		return fieldEntities.get(field);
	}
}
