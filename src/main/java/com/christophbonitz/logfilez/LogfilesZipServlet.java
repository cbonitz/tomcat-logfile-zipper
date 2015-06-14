/* Copyright (C) 2015 Christoph Bonitz - Licensed under Apache License 2.0 */
package com.christophbonitz.logfilez;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

/**
 * Servlet that sends Tomcat log files as a zip.
 * Uses streams to avoid high memory usage.
 * Created temporary files so their size doesn't change during streaming. 
 */
@WebServlet("/")
public class LogfilesZipServlet extends HttpServlet {
	private static final long serialVersionUID = -438627221731395807L;
	private static final Logger LOGGER = LoggerFactory.getLogger(LogfilesZipServlet.class);
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		LOGGER.info("Logfiles requested");
		File basedir = new File(System.getProperty("catalina.base"), "logs");
		if (!basedir.exists()) {
			resp.sendError(500);
			return;
		}
		resp.setHeader("Content-Type", "application/octet-stream");
		resp.setHeader("content-disposition", "attachment; filename='logs.zip'");
		ServletOutputStream outputStream = resp.getOutputStream();
		ZipOutputStream zip = null;
		int count = 0;
		try {
			zip = new ZipOutputStream(outputStream);
			File[] logfiles = basedir.listFiles();
			count = logfiles.length;
			for (File logfile : logfiles) {
				zip.putNextEntry(new ZipEntry(logfile.getName()));
				LOGGER.debug("zipping {}", logfile.getName());
				File tmp = File.createTempFile("templog-", ".txt");
				LOGGER.trace("temp file {}", tmp.getAbsolutePath());
				try {
					// create a temp copy. streaming a file that changes its size 
					// will fail occasionally
					Files.copy(logfile, tmp);
					FileInputStream fileInputStream = new FileInputStream(tmp);
					try {
						ByteStreams.copy(fileInputStream, zip);
					} finally {
						closeQuietly(fileInputStream);
					}
				} finally {
					deleteIfExistsQuiet(tmp);
				}
				zip.closeEntry();
			}
		} finally {
			closeQuietly(zip);
			closeQuietly(outputStream);
		}
		LOGGER.info("zipped and served {} logfiles", count);
}

	private void deleteIfExistsQuiet(File file) {
		if (file != null && file.exists()) {
			try {
				file.delete();
				LOGGER.trace("deleting file {}", file.getAbsolutePath());
			} catch (Exception e) {
				LOGGER.error("Error deleting " + file.getAbsolutePath(), e);
			}
		}
	}

	private void closeQuietly(Closeable outputStream) {
		try {
			if (outputStream != null) {
				outputStream.close();
			}
		} catch (Exception e) {
			LOGGER.error("Error closing stream", e);
		}
	}
}