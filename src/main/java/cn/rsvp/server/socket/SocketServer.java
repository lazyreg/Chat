package cn.rsvp.server.socket;

import cn.rsvp.datatype.MessageInfo;
import cn.rsvp.datatype.ChatInfo;
import cn.rsvp.datatype.ConnectInfo;
import cn.rsvp.server.bean.UserInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @author Dai.Liangzhi (dlz@rsvptech.cn)
 * @since 2018/9/25
 */
public class SocketServer {
  //声明服务器端套接字ServerSocket
  ServerSocket serverSocket;

  //输入流列表集合
  List<UserInfo> users = new ArrayList<UserInfo>();

  AcceptSocketThread thread = null;

  public SocketServer(int port) {
    try {
      //创建服务器端套接字ServerSocket,端口监听
      serverSocket = new ServerSocket(port);

      //创建接受Socket读线程实例，并启动
      thread = new AcceptSocketThread();
      thread.start();
    } catch (IOException e) {
      e.printStackTrace();
    }
    System.out.println(port + "端口服务已经启动...");
  }

  // 关闭
  public void stop() {
    if (thread != null && thread.isAlive()) {
      thread.interrupt();
    }
    if (serverSocket != null) {
      try {
        serverSocket.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  //接收客户端Socket套接字线程
  class AcceptSocketThread extends Thread {
    public void run() {
      while (this.isAlive()) {
        try {
          //接收一个客户端Socket对象
          Socket socket = serverSocket.accept();
          //建立该客户端读通信管道
          if (socket != null) {
            GetMsgFromClient client = new GetMsgFromClient(socket);
            client.start();
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  //接收客户端读聊天信息读线程
  class GetMsgFromClient extends Thread {
    Socket socket;
    UserInfo userInfo;
    PrintWriter printWriter;

    public GetMsgFromClient(Socket socket) {
      this.socket = socket;
    }

    public void run() {
      try {
        BufferedReader bReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        printWriter = new PrintWriter(socket.getOutputStream());

        while (this.isAlive()) {
          if (socket == null || socket.isClosed()) {
            System.out.println("socket closed");
            break;
          }

          String strMsg = bReader.readLine();
          if (strMsg != null) {
            MessageInfo messageInfo = MessageInfo.parseJsonString(strMsg);
            if (messageInfo != null) {
              processMsg(messageInfo);
            }
          }

          System.out.println("str=" + strMsg);
        }
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        if (userInfo != null) {
          users.remove(userInfo);
        }

        if (socket != null) {
          try {
            socket.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    }

    private void processMsg(MessageInfo messageInfo) {
      String type = messageInfo.getType();
      if (type == null) {
        return;
      }

      if (type.equals("entry")) {
        ConnectInfo connectInfo = messageInfo.getConnect();
        String name = connectInfo.getName();
        int room = connectInfo.getRoom();
        if (userInfo == null) {
          String userid = UUID.randomUUID().toString();
          userInfo =null;// new UserInfo(userid, room, name, printWriter);
          users.add(userInfo);
        } else {
          userInfo.setRoom(room);
          userInfo.setName(name);
        }
      } else if (type.equals("chat")) {
        if (userInfo == null) {
          return;
        }
        ChatInfo chatInfo = messageInfo.getChat();
        String sender = chatInfo.getFromUser();

        messageInfo.getTimestamp();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String content = df.format(new Date(messageInfo.getTimestamp())) + "\n";

        List<UserInfo> userInfoList = new ArrayList<UserInfo>();
        String toUser = chatInfo.getToUser();
        if (toUser == null) {
          for (UserInfo user : users) {
            if (user.getRoom() == userInfo.getRoom()) {
              userInfoList.add(user);
            }
          }
          content += sender + " 说：" + chatInfo.getContent();
        } else {
          for (UserInfo user : users) {
            if (user.getName().equals(toUser) && user.getRoom() == userInfo.getRoom()) {
              userInfoList.add(user);
              if (userInfo != user) {
                userInfoList.add(userInfo);
              }
              content += sender + " @ " + toUser + " 说：" + chatInfo.getContent();
              break;
            }
          }
        }

        for (UserInfo user : userInfoList) {
          PrintWriter pw = null;//user.getPw();
          pw.println(content);
          pw.flush();
        }
      }
    }
  }
}
