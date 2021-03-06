/**
 * Copyright (C) 2000 - 2013 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of the GPL, you may
 * redistribute this Program in connection with Free/Libre Open Source Software ("FLOSS")
 * applications as described in Silverpeas's FLOSS exception. You should have received a copy of the
 * text describing the FLOSS exception, and it is also available here:
 * "http://www.silverpeas.org/docs/core/legal/floss_exception.html"
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */
package com.silverpeas.servlets.upload;

import com.silverpeas.util.StringUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Sample servlet to upload file in an ajax way.
 *
 * @author ehugonnet
 */
public class AjaxFileUploadServlet extends HttpServlet {

  private static final long serialVersionUID = -557782586447656336L;
  private static final String UPLOAD_DIRECTORY = "UPLOAD_DIRECTORY";
  private static final String WHITE_LIST = "WHITE_LIST";
  private static final String FILE_UPLOAD_STATS = "FILE_UPLOAD_STATS";
  private static final String FILE_UPLOAD_PATHS = "FILE_UPLOAD_PATHS";
  private static String uploadDir;
  private static String whiteList;

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    uploadDir = config.getInitParameter(UPLOAD_DIRECTORY);
    whiteList = config.getInitParameter(WHITE_LIST);
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    HttpSession session = request.getSession();

    if ("status".equals(request.getParameter("q"))) {
      doStatus(session, response);
    } else {
      doFileUpload(session, request, response);
    }
  }

  /**
   * Do the effective upload of files.
   *
   * @param session the HttpSession
   * @param request the multpart request
   * @param response the response
   * @throws IOException
   */
  @SuppressWarnings("unchecked")
  private void doFileUpload(HttpSession session, HttpServletRequest request,
      HttpServletResponse response) throws IOException {
    try {
      FileUploadListener listener = new FileUploadListener(request.getContentLength());
      session.setAttribute(FILE_UPLOAD_STATS, listener.getFileUploadStats());
      FileItemFactory factory = new MonitoringFileItemFactory(listener);
      ServletFileUpload upload = new ServletFileUpload(factory);
      List<FileItem> items = (List<FileItem>) upload.parseRequest(request);
      boolean hasError = false;
      String errorMessage = "";
      List<String> paths = new ArrayList<String>(items.size());
      session.setAttribute(FILE_UPLOAD_PATHS, paths);
      for (FileItem fileItem : items) {
        if (!fileItem.isFormField() && fileItem.getSize() > 0L) {
          String filename = fileItem.getName();
          if (filename.indexOf('/') >= 0) {
            filename = filename.substring(filename.lastIndexOf('/') + 1);
          }
          if (filename.indexOf('\\') >= 0) {
            filename = filename.substring(filename.lastIndexOf('\\') + 1);
          }
          if (!isInWhiteList(filename)) {
            hasError = true;
            errorMessage += "The file " + filename
                + " isn't uploaded! Only HTML files can be uploaded<br/>";
          } else {
            filename = System.currentTimeMillis() + "-" + filename;
            File targetDirectory = new File(uploadDir, fileItem.getFieldName());
            targetDirectory.mkdirs();
            File uploadedFile = new File(targetDirectory, filename);
            OutputStream out = null;
            try {
              out = new FileOutputStream(uploadedFile);
              IOUtils.copy(fileItem.getInputStream(), out);
              paths.add(uploadedFile.getParentFile().getName() + '/' + uploadedFile.getName());
            } finally {
              IOUtils.closeQuietly(out);
            }
            fileItem.delete();
          }
        }
      }
      String uploadedFiles = getUploadedFilePaths(paths);
      if (!hasError) {
        sendCompleteResponse(response, uploadedFiles, null);
      } else {
        sendCompleteResponse(response, uploadedFiles, errorMessage);
      }
    } catch (Exception e) {
      Logger.getLogger(getClass().getSimpleName()).log(Level.WARNING, e.getMessage());
      sendCompleteResponse(response, "[]",
          "Could not process uploaded file. Please see log for details.");
    }
  }

  /**
   * Return the current status of the upload.
   *
   * @param session the HttpSession.
   * @param response where the status is to be written.
   * @throws IOException
   */
  private void doStatus(HttpSession session, HttpServletResponse response) throws IOException {
    // Make sure the status response is not cached by the browser
    response.addHeader("Expires", "0");
    response.addHeader("Cache-Control", "no-store, no-cache, must-revalidate");
    response.addHeader("Cache-Control", "post-check=0, pre-check=0");
    response.addHeader("Pragma", "no-cache");

    FileUploadListener.FileUploadStats fileUploadStats
        = (FileUploadListener.FileUploadStats) session.getAttribute(FILE_UPLOAD_STATS);
    if (fileUploadStats != null) {
      long bytesProcessed = fileUploadStats.getBytesRead();
      long sizeTotal = fileUploadStats.getTotalSize();
      long percentComplete = (long) Math.floor(((double) bytesProcessed / (double) sizeTotal)
          * 100.0);
      response.getWriter().println("<b>Upload Status:</b><br/>");

      if (fileUploadStats.getBytesRead() != fileUploadStats.getTotalSize()) {
        response.getWriter().println(
            "<div class=\"prog-border\"><div class=\"prog-bar\" style=\"width: " + percentComplete
            + "%;\"></div></div>");
      } else {
        response
            .getWriter()
            .println(
                "<div class=\"prog-border\"><div class=\"prog-bar\" style=\"width: 100%;\"></div></div>");
        response.getWriter().println("Complete.<br/>");
      }
    }
    if (fileUploadStats != null && fileUploadStats.getBytesRead() == fileUploadStats.getTotalSize()) {
      List<String> paths = (List<String>) session.getAttribute(FILE_UPLOAD_PATHS);
      String uploadedFilePaths = getUploadedFilePaths(paths);
      response.getWriter().println(
          "<b>Upload complete.</b>");
      response.getWriter().println(
          "<script type='text/javascript'>window.parent.stop('', " + uploadedFilePaths
          + "); stop('', " + uploadedFilePaths + ");</script>");
    }
  }

  /**
   * Send the response after the upload.
   *
   * @param response the HttpServletResponse.
   * @param paths the paths to the uploaded files.
   * @param message the error message.
   * @throws IOException
   */
  private void sendCompleteResponse(HttpServletResponse response, String paths, String message)
      throws IOException {
    if (message == null) {
      response
          .getOutputStream()
          .print(
              "<html><head><script type='text/javascript'>function killUpdate() { window.parent.stop('', "
              + paths + " ); }</script></head><body onload='killUpdate()'></body></html>");
    } else {
      response
          .getOutputStream()
          .print(
              "<html><head><script type='text/javascript'>function killUpdate() { window.parent.stop('"
              + message + "', ''); }</script></head><body onload='killUpdate()'></body></html>");
    }
  }

  /**
   * Compute a javascript array from the uploaded file paths
   *
   * @param session the HttpSession.
   */
  @SuppressWarnings("unchecked")
  private String getUploadedFilePaths(List<String> paths) throws IOException {
    if (paths == null) {
      paths = new ArrayList<String>();
    }
    ObjectMapper mapper = new ObjectMapper();
    return mapper.writeValueAsString(paths);
  }

  private List<String> getWhiteList() {
    if (StringUtil.isDefined(whiteList)) {
      return Arrays.asList(whiteList.split(" "));
    }
    return Arrays.asList();
  }

  private boolean isInWhiteList(String filename) {
    List<String> whileList = getWhiteList();
    String extension = FilenameUtils.getExtension(filename).toLowerCase();
    return whileList.contains(extension);
  }
}
