package com.thed.zephyr.je.index.bridge;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * specifies a given bridge implementation
 *
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.TYPE )
@Documented
public @interface ClassBridge {

	/**
	 * User supplied class to manipulate document in
	 * whatever mysterious ways they wish to.
	 */
	public Class<?>[] impl() default void.class;
}