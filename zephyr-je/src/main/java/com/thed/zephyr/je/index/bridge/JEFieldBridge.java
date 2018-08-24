package com.thed.zephyr.je.index.bridge;

import org.apache.lucene.document.Document;


/**
 * Link between a java property and a Lucene Document
 * Usually a Java property will be linked to a Document Field.
 * 
 */
public interface JEFieldBridge {

	/**
	 * Manipulate the document to index the external entity given the value.
	 * <code>fieldName</code> is nullable, Bridge will handle if null
	 * <code>value</code> is not null
	 * <code>document</code> is not null
	 */
	void set(String fieldName,Object value,Document document);
}
