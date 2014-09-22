package jetbrains.buildServer.swabra.web;

import jetbrains.buildServer.messages.Status;
import jetbrains.buildServer.util.Dates;
import jetbrains.buildServer.web.util.CameFromSupport;
import jetbrains.buildServer.web.util.WebUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * User: vbedrosova
 * Date: 26.02.2010
 * Time: 13:12:22
 */
public class HandleForm {
  private String myUrl = "http://live.sysinternals.com/handle.exe";
  private boolean myRunning = false;
  private final List<String> myDownloadHandleMessages = new ArrayList<String>();
  private final CameFromSupport myCameFromSupport = new CameFromSupport();

  private int myErrors = 0;
  private final SimpleDateFormat myTimestampFormat = new SimpleDateFormat("HH:mm:ss");

  public String getUrl() {
    return myUrl;
  }

  public void setUrl(String url) {
    myUrl = url;
  }

  public boolean isRunning() {
    return myRunning;
  }

  public void setRunning(boolean running) {
    myRunning = running;
  }

  public List<String> getDownloadHandleMessages() {
    return myDownloadHandleMessages;
  }

  public void clearMessages() {
    myDownloadHandleMessages.clear();
    myErrors = 0;
  }

  public CameFromSupport getCameFromSupport() {
    return myCameFromSupport;
  }

  public void addMessage(String text, Status status) {
    boolean error = status.above(Status.WARNING);
    if (error) {
      myErrors++;
    }
    String errorId = error ? "id='errorNum:" + myErrors + "'" : "";
    final String escapedText = WebUtil.escapeXml(text).replace("\n", "<br/>");
    String className = "";
    if (error) {
      className = "errorMessage";
    }

    String finalText = escapedText;
    if (error) {
      finalText = "<span " + errorId + " class='" + className + "'>" + escapedText + "</span>";
    }

    myDownloadHandleMessages.add(timestamp() + finalText);
  }

  private String timestamp() {
    return "[" + myTimestampFormat.format(Dates.now()) + "]: ";
  }
}