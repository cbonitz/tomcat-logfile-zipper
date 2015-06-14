package com.christophbonitz.logfilez;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

@WebServlet("/")
public class LogfilesZipServlet extends HttpServlet {
	private static final long serialVersionUID = -438627221731395807L;
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		PrintWriter writer = resp.getWriter();
		File basedir = new File(System.getProperty("catalina.base"), "logs");
		writer.printf(String.format("Log dir: %s\n", basedir.getAbsolutePath()));
		if (!basedir.exists()) {
			writer.println("oops");
		} else {
			File[] listFiles = basedir.listFiles();
			for (File file : listFiles) {
				writer.printf(String.format("[%s]\n", file.getName()));
				List<String> readLines = Files.readLines(file, Charsets.UTF_8);
				for (String line : readLines) {					
					writer.println(line);
				}
			}
		}
		writer.close();	}
}
