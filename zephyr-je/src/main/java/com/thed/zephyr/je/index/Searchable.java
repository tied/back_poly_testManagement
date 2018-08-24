package com.thed.zephyr.je.index;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Used to tag methods as being full-text searchable within a 
 * Lucene index.  
 * <pre>public interface Post extends Entity {
 *     &#064;Searchable
 *     public String getComment();
 * }
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Searchable {}

