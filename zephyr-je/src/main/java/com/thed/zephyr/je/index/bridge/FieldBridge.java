package com.thed.zephyr.je.index.bridge;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * specifies a given field bridge implementation
 *
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( {ElementType.FIELD, ElementType.METHOD} )
@Documented
public @interface FieldBridge {
	//default to embed @FieldBridge in @Field
	public Class impl() default void.class;
}
