package cn.rsvp.server.tomcat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * @author Dai.Liangzhi (dlz@rsvptech.cn)
 * @since 2018/11/14
 */
public class TomcatTask {
  private Socket socket;

  public TomcatTask(Socket socket) {
    this.socket = socket;
  }

  public void procss() {
    try {
      BufferedReader bReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      PrintWriter printWriter = new PrintWriter(socket.getOutputStream());

      while (true) {
        if (socket == null || socket.isClosed()) {
          System.out.println("socket closed");
          break;
        }

        String strMsg = bReader.readLine();
        if (strMsg == null) {
          break;
        }
        printWriter.println(strMsg);
        printWriter.flush();
        System.out.println("this thread=" + Thread.currentThread().toString() + "  str=" + strMsg);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (socket != null) {
        try {
          socket.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }
}
