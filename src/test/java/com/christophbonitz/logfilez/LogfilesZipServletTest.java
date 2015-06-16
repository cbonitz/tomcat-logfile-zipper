package com.christophbonitz.logfilez;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.mockito.Mockito;

public class LogfilesZipServletTest {
	
	LogfilesZipServlet servlet = new LogfilesZipServlet();
	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	Logger LOGGER = Logger.getLogger(LogfilesZipServlet.class.getName());
	
	@Test
	public void testDoGet() throws ServletException, IOException {
		HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
		HttpServletResponse resp = getServletResponse();
		URL catalinaTestUrl = LogfilesZipServletTest.class.getResource(".");
		System.setProperty("catalina.base", catalinaTestUrl.getPath());
		LOGGER.info("catalina base set to " + catalinaTestUrl.getPath());
		
		servlet.doGet(req, resp);
		
		Mockito.verify(resp, Mockito.never()).sendError(Mockito.anyInt());
		ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		ZipInputStream zis = new ZipInputStream(bis);
		assertNextMethod(zis, "catalina.out", "catalina content");
	}

	private void assertNextMethod(ZipInputStream zis, String filename, String content) throws IOException {
		ZipEntry entry = zis.getNextEntry();
		assertEquals("Didn't find " + filename, filename, entry.getName());
		ByteArrayOutputStream eos = new ByteArrayOutputStream();
		int len = 0;
		byte[] buffer = new byte[4096];
		while ((len = zis.read(buffer)) > 0) {
			eos.write(buffer, 0, len);
		}
		String gotContent = new String(eos.toByteArray(), Charset.availableCharsets().get("UTF-8"));
		assertEquals(filename + " didn't contain the expected content.", content, gotContent);
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
