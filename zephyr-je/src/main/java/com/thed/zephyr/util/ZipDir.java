package com.thed.zephyr.util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

/**
 * @author manjunath
 *
 */
public class ZipDir extends SimpleFileVisitor<Path> {
	
	protected final Logger log = Logger.getLogger(ZipDir.class);

	private static ZipOutputStream zos;

	private Path sourceDirPath;
	
	private Path destinationPath;

	public ZipDir(Path sourceDirPath, Path destinationPath) {
		this.sourceDirPath = sourceDirPath;
		this.destinationPath = destinationPath;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
		Path targetFile = sourceDirPath.relativize(file);			
		zos.putNextEntry(new ZipEntry(targetFile.toString()));			
		try {
			byte[] bytes = Files.readAllBytes(file);
			zos.write(bytes, 0, bytes.length);
		} finally {
			zos.closeEntry();
		}
		return FileVisitResult.CONTINUE;
	}
	
	public void zipDir() throws IOException {
		long start = System.currentTimeMillis();
		if(destinationPath.toFile().exists()) {
			destinationPath.toFile().delete(); //In case if destination file is already present then delete it.
		} 
		try {
			zos = new ZipOutputStream(FileUtils.openOutputStream(destinationPath.toFile()));			
			Files.walkFileTree(sourceDirPath, this); //Walks through each file inside the directory to compress and zip it.			
			long end = System.currentTimeMillis();
			log.info("Zip compression is completed and took " + (end - start) / 1000 +  "s");
		} finally {
			if(Objects.nonNull(zos))
				zos.close();
		}
	}
}