package org.jboss.pnc.buildagent.servlet;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class Upload extends HttpServlet {

  @Override
  protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    int fileSize = request.getContentLength();
    String fileDestination = request.getPathInfo();
    if (!fileDestination.startsWith("/")) {
      fileDestination = "/" + fileDestination;
    }

    int totalBytes = 0;
    try (ServletInputStream inputStream = request.getInputStream()) {
      try (FileOutputStream fileOutputStream = new FileOutputStream(fileDestination)) {
        byte[] bytes = new byte[1024];
        int read;
        while ((read = inputStream.read(bytes)) != -1) {
          fileOutputStream.write(bytes, 0, read);
          totalBytes += read;
        }
      }
    }

    if (totalBytes != fileSize) {
      response.sendError(500, "Did not received complete file!");
    } else {
      response.setStatus(200);
    }

  }
}
