/* Copyright (C) 2015 Christoph Bonitz - Licensed under Apache License 2.0 */
package com.christophbonitz.logfilez;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet that sends Tomcat log files as a zip.
 * Uses streams to avoid high memory usage.
 * Created temporary files so their size doesn't change during streaming. 
 */
@WebServlet("/")
public class LogfilesZipServlet extends HttpServlet {
	private static final long serialVersionUID = -438627221731395807L;
	private static final Logger LOGGER = Logger.getLogger(LogfilesZipServlet.class.getName());
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		LOGGER.info("Logfiles requested");
		String catalinaBase = System.getProperty("catalina.base");
		if (catalinaBase == null) {
			resp.sendError(500, "catalina.base not exist");
			return;
		}
		File basedir = new File(catalinaBase, "logs");
		if (!basedir.exists()) {
			resp.sendError(500, "logs directory does not exist");
			return;
		}
		resp.setHeader("Content-Type", "application/octet-stream");
		resp.setHeader("content-disposition", "attachment; filename=logs.zip");
		ServletOutputStream outputStream = resp.getOutputStream();
		ZipOutputStream zip = null;
		int count = 0;
		try {
			zip = new ZipOutputStream(outputStream);
			count = zip("", basedir, zip);
		} finally {
			closeQuietly(zip);
			closeQuietly(outputStream);
		}
		LOGGER.info("zipped and served " + count + " logfiles");
	}

	/**
	 * Recursively zip log directory
	 * @param prefix prefix, can be empty
	 * @param directory directory
	 * @param zip a zip output stream
	 * @return number of zipped files
	 * @throws IOException
	 */
	private int zip(String prefix, File directory, ZipOutputStream zip) throws IOException {
		File[] logfiles = directory.listFiles();
		int count = 0;
		for (File logfile : logfiles) {
			if (!logfile.isFile()) {
				if (logfile.isDirectory()) {
					// zip recursively, count files
					count += zip(prefix + logfile.getName() + "/", logfile, zip);
				}
				continue;
			}
			zip.putNextEntry(new ZipEntry(prefix + logfile.getName()));
			LOGGER.info("zipping " + logfile.getName());
			File tmp = File.createTempFile("templog-", ".txt");
			LOGGER.log(Level.FINE, "temp file " + tmp.getAbsolutePath());
			try {
				// create a temp copy. streaming a file that changes its size 
				// will fail occasionally
				Files.copy(logfile.toPath(), tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
				FileInputStream fileInputStream = new FileInputStream(tmp);
				try {
					copy(zip, fileInputStream);
				} finally {
					closeQuietly(fileInputStream);
				}
			} finally {
				deleteIfExistsQuiet(tmp);
			}
			zip.closeEntry();
			count++;
		}
		return count;
	}

	/**
	 * Copy contents of in to out via a buffer. Imitates Guava's Files.copy
	 * @param out
	 * @param in
	 * @throws IOException
	 */
	private void copy(OutputStream out, InputStream in)
			throws IOException {
		byte[] buffer = new byte[4096];
		int readBytes = in.read(buffer);
		while (readBytes != -1) {
			out.write(buffer, 0, readBytes);
			readBytes = in.read(buffer);
		}
	}

	/**
	 * Deletes file if it exists, throwing now Exceptions
	 * @param file
	 */
	private void deleteIfExistsQuiet(File file) {
		if (file != null && file.exists()) {
			try {
				file.delete();
				LOGGER.log(Level.FINE, "deleting file " + file.getAbsolutePath());
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Error deleting " + file.getAbsolutePath(), e);
			}
		}
	}

	/**
	 * Close a stream, and don't throw on errors. Imitaltes IOUtils' closeQuietly
	 * @param outputStream
	 */
	private void closeQuietly(Closeable outputStream) {
		try {
			if (outputStream != null) {
				outputStream.close();
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error closing stream", e);
		}
	}
}