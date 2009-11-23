package jetbrains.buildServer.swabra;

import jetbrains.buildServer.vcs.VcsException;

import java.text.DateFormat;
import java.text.ParseException;

/**
 * User: vbedrosova
 * Date: 09.09.2009
 * Time: 16:58:13
 */
public class Test {
  public static void main(String args[]) {
    try {
      System.out.println(DateFormat.getDateTimeInstance().parse("2007.4.9 21:30:54"));
      System.out.println(DateFormat.getInstance().format(DateFormat.getDateTimeInstance().parse("2007.4.9 21:30:54")));
    } catch (ParseException e) {
      e.printStackTrace();
    }
  }
}
