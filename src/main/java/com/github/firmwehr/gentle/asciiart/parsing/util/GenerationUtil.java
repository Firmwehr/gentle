package com.github.firmwehr.gentle.asciiart.parsing.util;

import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;

import java.util.Optional;

public class GenerationUtil {

	public static CtInvocation<?> optionalEmpty(Factory factory) {
		CtTypeReference<Object> optionalReference = factory.Class().get(Optional.class).getReference();

		CtExecutableReference<Object> executableReference = factory.createExecutableReference();
		executableReference.setSimpleName("empty");
		executableReference.setStatic(true);
		executableReference.setDeclaringType(optionalReference);
		executableReference.setType(optionalReference);

		return factory.createInvocation(factory.createTypeAccess(optionalReference), executableReference);
	}

	public static CtInvocation<?> optional(Factory factory, CtExpression<?> value) {
		CtTypeReference<Object> optionalReference = factory.Class().get(Optional.class).getReference();

		CtExecutableReference<Object> executableReference = factory.createExecutableReference();
		executableReference.setSimpleName("of");
		executableReference.setStatic(true);
		executableReference.setDeclaringType(optionalReference);
		executableReference.setType(optionalReference);

		return factory.createInvocation(factory.createTypeAccess(optionalReference), executableReference, value);
	}
}
