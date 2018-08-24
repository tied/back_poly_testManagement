package com.thed.zephyr.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IndexOutput;

/**
 * @author manjunath
 *
 */
public class UnZipDir {
	
	private static final int BUFFER_SIZE = 4096;
	
	private String zipFilePath;
	
	private Directory destDirectory;
	

	public UnZipDir(String zipFilePath, Directory destDirectory) {
		this.zipFilePath = zipFilePath;
		this.destDirectory = destDirectory;
	}

	public void unzip() throws IOException {
        try(ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry = zipIn.getNextEntry();
            while (entry != null) { //iterates over entries in the zip file
                if (!entry.isDirectory() && !destDirectory.fileExists(entry.getName())) {
                	IndexOutput indexOutput = destDirectory.createOutput(entry.getName());
                    extractFile(zipIn, indexOutput); //if the entry is a file, extracts it
                    indexOutput.close();
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }
    }
	
	private void extractFile(ZipInputStream zipIn, IndexOutput indexOutput) throws IOException {
		byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
        	indexOutput.writeBytes(bytesIn, 0, read);
        }
    }

}
