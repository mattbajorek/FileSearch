package com.example.filesearch;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileSearchApp {
	String path;
	String regex;
	String zipFileName;
	Pattern pattern;
	List<File> zipFiles = new ArrayList<File>();

	public static void main(String[] args) {
		FileSearchApp app = new FileSearchApp();
		
		switch( Math.min(args.length, 3) ) {
			case 0:
				System.out.println("USAGE: FileSearchApp path [regex] [zipfile]");
				return;
			case 3: app.setZipFileName(args[2]);
			case 2: app.setRegex(args[1]);	
			case 1: app.setPath(args[0]);
		}
		try {
			app.walkDirectory(app.getPath());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// File class
	public void walkDirectoryJava6(String path) throws IOException {
		File dir = new File(path);
		File[] files = dir.listFiles();
		
		for (File file : files) {
			if (file.isDirectory()) {
				walkDirectoryJava8(file.getAbsolutePath());
			} else {
				processFile(file);
			}
		}
	}
	
	public boolean searchFileJava6(File file) throws FileNotFoundException {
		boolean found = false;
		Scanner scanner = new Scanner(file, "UTF-8"); // throws exception
		while (scanner.hasNextLine()){
			found = searchText(scanner.nextLine());
			if (found) { break; }
		}
		scanner.close();
		return found;
	}
	
	public void zipFilesJava6() throws IOException {
		ZipOutputStream out = null;
		try {
			out = new ZipOutputStream(new FileOutputStream(getZipFileName()));
			File baseDir = new File(getPath());
			
			for (File file : zipFiles) {
				// fileName must be a relative path, not an absolute one.
				String fileName = getRelativeFilename(file, baseDir);
				
				ZipEntry zipEntry = new ZipEntry(fileName);
				zipEntry.setTime(file.lastModified());
				out.putNextEntry(zipEntry);
				
				int bufferSize = 2048;
				byte[] buffer = new byte[bufferSize];
				int len = 0;
				BufferedInputStream in = new BufferedInputStream(
						new FileInputStream(file), bufferSize);
				while((len = in.read(buffer, 0, bufferSize)) != -1) {
					out.write(buffer, 0, len);
				}
				in.close();
				
				out.closeEntry();
			}
		} finally {
			out.close();
		}
	}
	
	// NIO class
	public void walkDirectoryJava7(String path) throws IOException {
		Files.walkFileTree(Paths.get(path), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
				throws IOException {
					processFile(file.toFile());
					return FileVisitResult.CONTINUE;
				}
		});
	}
	
	public boolean searchFileJava7(File file) throws IOException {
		// List is an array that can be resized
		// <String> is a generic to catch errors, but does not need to be used
		List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8); // throws exception
		for (String line : lines) {
			if (searchText(line)) {
				return true;
			}
		}
		return false;
	}
	
	public void zipFilesJava7() throws IOException {
		try (ZipOutputStream out = 
				new ZipOutputStream(new FileOutputStream(getZipFileName())) ) {
			File baseDir = new File(getPath());
			
			for (File file : zipFiles) {
				// fileName must be a relative path, not an absolute one.
				String fileName = getRelativeFilename(file, baseDir);
				
				ZipEntry zipEntry = new ZipEntry(fileName);
				zipEntry.setTime(file.lastModified());
				out.putNextEntry(zipEntry);
				
				Files.copy(file.toPath(), out);
				
				out.closeEntry();
			}
		}
	}
	
	// Streams
	public void walkDirectoryJava8(String path) throws IOException {
		Files.walk(Paths.get(path))
			.forEach(f -> processFile(f.toFile()));
	}
	
	public boolean searchFileJava8(File file) throws IOException {
		return Files.lines(file.toPath(), StandardCharsets.UTF_8) // can remove StandardCharsets cause defaults to UTF_8
			.anyMatch(t -> searchText(t)); // stops and returns true first time any elements found
	}
	
	// Process file
	public void processFile(File file) {
		try {
			if (searchFile(file)) {
				addFileToZip(file);
			}
		} catch (IOException | UncheckedIOException e) {
			System.out.println("Error processing file: " + file + ": " + e);
		}
	}
	
	public void walkDirectory(String path) throws IOException {
		walkDirectoryJava8(path);
		zipFilesJava7();
	}
	
	public boolean searchFile(File file) throws IOException {
		return searchFileJava6(file);
	}
	
	public void addFileToZip(File file) {
		if (getZipFileName() != null) {
			zipFiles.add(file);
		}
	}
	
	// Search text in search file
	public boolean searchText(String text) {
		return (this.getRegex() == null ) ? true :
			this.pattern.matcher(text).matches(); // adds ^ before and $ after text (for full matching)
			// text.matches(this.getRegex()); // needs to be compiled every time so added pattern compile
			// text.toLowerCase().contains(this.getRegex().toLowerCase());
	}
	
	// Get relative filename
	public String getRelativeFilename(File file, File baseDir) {
		String fileName = file.getAbsolutePath().substring(
				baseDir.getAbsolutePath().length());
		
		// IMPORTANT: the ZipEntry file name must use "/", not "\".
		fileName = fileName.replace('\\', '/');
		
		while (fileName.startsWith("/")) {
			fileName = fileName.substring(1);
		}
		
		return fileName;
	}
	
	// Getters and setters
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getRegex() {
		return regex;
	}

	public void setRegex(String regex) {
		this.regex = regex;
		this.pattern = Pattern.compile(regex);
	}

	public String getZipFileName() {
		return zipFileName;
	}

	public void setZipFileName(String zipFileName) {
		this.zipFileName = zipFileName;
	}

}
