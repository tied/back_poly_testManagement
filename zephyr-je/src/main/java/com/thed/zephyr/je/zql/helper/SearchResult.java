package com.thed.zephyr.je.zql.helper;

import java.util.List;

import org.apache.lucene.document.Document;

public class SearchResult {
	
    private int start;
    private int max;
	private int total;
    private List<Document> documents;

    
    public SearchResult(int start, int total, List<Document> documents) {
		super();
		this.start = start;
		this.max = max;
		this.total = total;
		this.documents = documents;
	}

    
    /**
	 * @return the start
	 */
	public int getStart() {
		return start;
	}
	/**
	 * @param start the start to set
	 */
	public void setStart(int start) {
		this.start = start;
	}
	/**
	 * @return the max
	 */
	public int getMax() {
		return max;
	}
	/**
	 * @param max the max to set
	 */
	public void setMax(int max) {
		this.max = max;
	}
	/**
	 * @return the total
	 */
	public int getTotal() {
		return total;
	}
	/**
	 * @param total the total to set
	 */
	public void setTotal(int total) {
		this.total = total;
	}
	/**
	 * @return the documents
	 */
	public List<Document> getDocuments() {
		return documents;
	}
	/**
	 * @param documents the documents to set
	 */
	public void setDocuments(List<Document> documents) {
		this.documents = documents;
	}
 

}
