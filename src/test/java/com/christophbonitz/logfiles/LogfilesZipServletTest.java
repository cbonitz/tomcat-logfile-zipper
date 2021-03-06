package com.christophbonitz.logfiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LogfilesZipServletTest {
	private static final Logger LOGGER = Logger.getLogger(LogfilesZipServlet.class.getName());
	
	private static final String CATALINA_BASE = "catalina.base";
	private static final String PACKAGE_WITH_ONE_FILE = "onefile";
	private static final String PACKAGE_WITH_TWO_FILES = "twofiles";
	private static final String PACKAGE_WITH_SUBDIRECTORIES = "directories";
	private static final String PACKAGE_WITHOUT_LOG_DIR = "nologs";
	
	private LogfilesZipServlet servlet;
	private ByteArrayOutputStream bos;
	
	@Before
	public void before() {
		servlet = new LogfilesZipServlet();
		bos = new ByteArrayOutputStream();
	}
	
	@Test
	public void testNonRootPath() throws ServletException, IOException {
		HttpServletResponse resp = getServletResponse();
		
		HttpServletRequest req = mock(HttpServletRequest.class);
		when(req.getRequestURI()).thenReturn("/logs/foo");
		when(req.getContextPath()).thenReturn("/logs");
		
		servlet.doGet(req, resp);
		
		verify(resp).sendError(eq(404), anyString());
	}
	
	@Test
	public void testOneFile() throws ServletException, IOException {
		setCatalinaBase(PACKAGE_WITH_ONE_FILE);
		
		HttpServletResponse resp = doGetMockResponse();
		
		verify(resp, never()).sendError(anyInt());
		ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		ZipInputStream zis = new ZipInputStream(bis);
		try {
			assertNextEntry(zis, "catalina.out", "catalina content");
			assertNull("Not just one entry (catalina.out)", zis.getNextEntry());
		} finally {
			IOUtils.closeQuietly(zis);
		}
	}
	
	@Test
	public void testTwoFiles() throws ServletException, IOException {
		setCatalinaBase(PACKAGE_WITH_TWO_FILES);
		
		HttpServletResponse resp = doGetMockResponse();
		
		verify(resp, never()).sendError(anyInt());
		ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		ZipInputStream zis = new ZipInputStream(bis);
		try {
			assertNextEntry(zis, "catalina.err", "catalina error content");
			assertNextEntry(zis, "catalina.out", "catalina content");
			assertNull("Not just two entries (catalina.err, catalina.out)", zis.getNextEntry());
		} finally {
			IOUtils.closeQuietly(zis);
		}
	}
	
	@Test
	public void testFilesInDirectories() throws ServletException, IOException {
		setCatalinaBase(PACKAGE_WITH_SUBDIRECTORIES);
		
		HttpServletResponse resp = doGetMockResponse();
		
		verify(resp, never()).sendError(anyInt());
		ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		ZipInputStream zis = new ZipInputStream(bis);
		try {
			assertNextEntry(zis, "catalina.out", "catalina content");
			assertNextEntry(zis, "subdir/custom.log", "customlog content");
			assertNull("Not just one entry", zis.getNextEntry());
			
		} finally {
			IOUtils.closeQuietly(zis);
		}
	}

	@Test
	public void testContentType() throws ServletException, IOException {
		setCatalinaBase(PACKAGE_WITH_ONE_FILE);
		
		HttpServletResponse resp = doGetMockResponse();
		
		verify(resp).setHeader("Content-Type", "application/octet-stream");
	}
	
	@Test
	public void testNoCatalinaBaseVariable() throws ServletException, IOException {
		ensureCatalinaBaseIsNotSet();
		
		HttpServletResponse resp = doGetMockResponse();
		
		verify(resp).sendError(eq(500), anyString());
	}
	
	@Test
	public void testNoCatalinaBaseDirectory() throws ServletException, IOException {
		// This directory doesn't exist
		System.setProperty(CATALINA_BASE, "/" + UUID.randomUUID());
		
		HttpServletResponse resp = doGetMockResponse();
		
		verify(resp).sendError(eq(500), anyString());
	}
	
	@Test
	public void testNoLogdir() throws ServletException, IOException {
		setCatalinaBase(PACKAGE_WITHOUT_LOG_DIR);
		
		HttpServletResponse resp = doGetMockResponse();
		
		verify(resp).sendError(eq(500), anyString());
	}
	
	@After
	public void after() {
		IOUtils.closeQuietly(bos);
	}

	private void setCatalinaBase(String subpackage) {
		URL catalinaTestUrl = LogfilesZipServletTest.class.getResource(String.format("./%s/", subpackage));
		System.setProperty(CATALINA_BASE, catalinaTestUrl.getPath());
		LOGGER.info("catalina base set to " + catalinaTestUrl.getPath());
	}

	private void ensureCatalinaBaseIsNotSet() {
		if (System.getProperty(CATALINA_BASE) != null) {
			System.getProperties().remove(CATALINA_BASE);
			LOGGER.info("unsetting " + CATALINA_BASE);
		}
	}

	private HttpServletResponse doGetMockResponse() throws IOException,
			ServletException {
		HttpServletResponse resp = getServletResponse();
		
		HttpServletRequest req = mock(HttpServletRequest.class);
		when(req.getRequestURI()).thenReturn("/logs/");
		when(req.getContextPath()).thenReturn("/logs");
		
		servlet.doGet(req, resp);
		
		return resp;
	}

	private HttpServletResponse getServletResponse() throws IOException {
		HttpServletResponse resp = mock(HttpServletResponse.class);
		ServletOutputStream os = new ServletOutputStream() {
			@Override
			public void write(int b) throws IOException {
				bos.write(b);
			}
		};
		when(resp.getOutputStream()).thenReturn(os);
		return resp;
	}

	private void assertNextEntry(ZipInputStream zis, String filename, String content) throws IOException {
		ZipEntry entry = zis.getNextEntry();
		assertEquals("Didn't find " + filename, filename, entry.getName());
		ByteArrayOutputStream eos = new ByteArrayOutputStream();
		IOUtils.copy(zis, eos);
		IOUtils.closeQuietly(eos);
		String receivedContent = new String(eos.toByteArray(), Charset.availableCharsets().get("UTF-8"));
		assertEquals(filename + " didn't contain the expected content.", content, receivedContent);
	}
}
