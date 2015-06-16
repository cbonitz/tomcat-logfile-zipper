package com.christophbonitz.logfilez;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

public class LogfilesZipServletTest {
	
	LogfilesZipServlet servlet = new LogfilesZipServlet();
	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	Logger LOGGER = Logger.getLogger(LogfilesZipServlet.class.getName());
	
	@Test
	public void testDoGet() throws ServletException, IOException {
		HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
		HttpServletResponse resp = getServletResponse();
		setPath();
		
		servlet.doGet(req, resp);
		
		Mockito.verify(resp, Mockito.never()).sendError(Mockito.anyInt());
		ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		ZipInputStream zis = new ZipInputStream(bis);
		try {
			assertNextEntry(zis, "catalina.out", "catalina content");
			assertNull("Not just one entry", zis.getNextEntry());
		} finally {
			IOUtils.closeQuietly(zis);
		}
	}

	@Test
	public void testContentType() throws ServletException, IOException {
		HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
		HttpServletResponse resp = getServletResponse();
		setPath();
		
		servlet.doGet(req, resp);
		
		Mockito.verify(resp).setHeader("Content-Type", "application/octet-stream");
	}
	
	@Test
	public void testNoCatalinaBase() throws ServletException, IOException {
		HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
		HttpServletResponse resp = getServletResponse();
		
		servlet.doGet(req, resp);
		
		Mockito.verify(resp).sendError(Matchers.eq(500), Matchers.anyString());
	}
	
	@Test
	public void testNoLogdir() throws ServletException, IOException {
		HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
		HttpServletResponse resp = getServletResponse();
		System.setProperty("catalina.base", "/" + UUID.randomUUID());
		
		servlet.doGet(req, resp);
		
		Mockito.verify(resp).sendError(Matchers.eq(500), Matchers.anyString());
	}
	
	@After
	public void after() {
		IOUtils.closeQuietly(bos);
		// if set during test, we need to unset it.
		System.getProperties().remove("catalina.base");
	}

	private void setPath() {
		URL catalinaTestUrl = LogfilesZipServletTest.class.getResource(".");
		System.setProperty("catalina.base", catalinaTestUrl.getPath());
		LOGGER.info("catalina base set to " + catalinaTestUrl.getPath());
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

	private HttpServletResponse getServletResponse() throws IOException {
		HttpServletResponse resp = Mockito.mock(HttpServletResponse.class);
		ServletOutputStream os = new ServletOutputStream() {
			@Override
			public void write(int b) throws IOException {
				bos.write(b);
			}
		};
		Mockito.when(resp.getOutputStream()).thenReturn(os);
		return resp;
	}
}
