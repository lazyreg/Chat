package cn.rsvp.server.socket;

import cn.rsvp.datatype.ChatInfo;
import cn.rsvp.datatype.ConnectInfo;
import cn.rsvp.datatype.MessageInfo;
import cn.rsvp.server.bean.UserInfo;
import cn.rsvp.server.tomcat.TomcatTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Dai.Liangzhi (dlz@rsvptech.cn)
 * @since 2018/9/25
 */
public class SocketServerChannel {
  private Selector selector;

  //输入流列表集合
  private Map<String, UserInfo> users;
  private Map<Integer, List<String>> rooms;
  private ConcurrentLinkedQueue<Info> mQueue;

  public SocketServerChannel(int port) {
    try {
      users = new HashMap<>();
      rooms = new HashMap<>();
      mQueue = new ConcurrentLinkedQueue<>();

      ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
      serverSocketChannel.configureBlocking(false);
      serverSocketChannel.socket().bind(new InetSocketAddress(port));
      this.selector = Selector.open();
      serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

      new SelectorThread().start();
      TaskThread taskThread = new TaskThread();
      taskThread.setDaemon(true);
      taskThread.start();
    } catch (IOException e) {
      e.printStackTrace();
    }
    System.out.println(port + "端口服务已经启动...");
  }

  class SelectorThread extends Thread {
    @Override
    public void run() {
      System.out.println("thread 监听开启");
      try {
        int count =10;
        while (count>0) {
          System.out.println("---select---");
          selector.select();
          Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
          while (iterator.hasNext()) {
            SelectionKey key = iterator.next();
            iterator.remove();

            if (key.isAcceptable()) {
              handlerAccept(key);
            } else if (key.isReadable()) {
              handlerReader(key);
            }
          }
          count--;
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      System.out.println("thread 监听结束");
    }
  }

  class TaskThread extends Thread {
    @Override
    public void run() {
      while (true) {
        if (mQueue.isEmpty()) {
          try {
            Thread.sleep(50);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          continue;
        }

        Info key = mQueue.poll();
        processMsg(key.userid, key.messageInfo);
      }
    }
  }

  private void handlerAccept(SelectionKey key) {
    ServerSocketChannel sever = (ServerSocketChannel) key.channel();
    System.out.println("sever=" + sever.toString());

    SocketChannel channel = null;
    try {
      channel = sever.accept();
      if (channel == null) {
        return;
      }
      channel.configureBlocking(false);
      System.out.println("有客服端连接来了" + channel.toString());
      channel.register(this.selector, SelectionKey.OP_READ);

      System.out.println("channel=" + channel.toString());
      String userid = channel.toString();
      UserInfo userInfo = new UserInfo(userid, -1, null, channel);
      users.put(userid, userInfo);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void handlerReader(SelectionKey key) {
    SocketChannel socketChannel = (SocketChannel) key.channel();
    System.out.println("socket=" + socketChannel.toString());

    ByteBuffer buffer = ByteBuffer.allocate(1024);
    try {
      int n = socketChannel.read(buffer);
      System.out.println(n);
      if (n > 0) {
        byte[] data = buffer.array();
        String strMsg = new String(data, 0, n);
        if (strMsg != null) {
          System.out.println("服务端收到信息:" + strMsg);
          MessageInfo messageInfo = MessageInfo.parseJsonString(strMsg);
          if (messageInfo != null) {
            mQueue.offer(new Info(socketChannel.toString(), messageInfo));
            //processMsg(socketChannel.toString(), messageInfo);
          }
        }
        buffer.clear();
        //buffer.flip();
        //socketChannel.write(buffer);
      } else {
        System.out.println("client is close");
        key.cancel();
      }
    } catch (IOException e) {
      e.printStackTrace();
      try {
        socketChannel.close();
      } catch (IOException e1) {
        e1.printStackTrace();
      }
    }
  }

  class Info {
    String userid;
    MessageInfo messageInfo;

    Info(String userid, MessageInfo messageInfo) {
      this.messageInfo = messageInfo;
      this.userid = userid;
    }
  }

  private void processMsg(String userid, MessageInfo messageInfo) {
    String type = messageInfo.getType();
    if (type == null) {
      return;
    }

    UserInfo userInfo = users.get(userid);
    if (userInfo == null) {
      return;
    }

    if (type.equals("entry")) {
      ConnectInfo connectInfo = messageInfo.getConnect();
      String name = connectInfo.getName();
      int room = connectInfo.getRoom();

      int preRoom = userInfo.getRoom();
      if (preRoom > 0) {
        List roomList = rooms.get(preRoom);
        roomList.remove(userid);
      }

      userInfo.setName(name);
      userInfo.setRoom(room);
      List roomList = rooms.get(room);
      if (roomList == null) {
        roomList = new ArrayList();
        rooms.put(room, roomList);
      }
      roomList.add(userid);
    } else if (type.equals("chat")) {
      ChatInfo chatInfo = messageInfo.getChat();
      String sender = chatInfo.getFromUser();

      messageInfo.getTimestamp();
      SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      String content = df.format(new Date(messageInfo.getTimestamp())) + "\n";

      String toUser = chatInfo.getToUser();
      if (toUser == null) {
        content += sender + " 说：" + chatInfo.getContent() + "\n";

        int room = userInfo.getRoom();
        List<String> roomList = rooms.get(room);
        for (String id : roomList) {
          UserInfo user = users.get(id);
          SocketChannel channel = user.getChannel();
          try {
            ByteBuffer buf = ByteBuffer.allocate(200);
            buf.put(content.getBytes());
            buf.flip();
            while (buf.hasRemaining()) {
              int len = channel.write(buf);
              System.out.println("write len=" + len);
              System.out.println(new String(buf.array()));
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      } else {
        content += sender + " @ " + toUser + " 说：" + chatInfo.getContent() + "\n";

        SocketChannel channel = userInfo.getChannel();
        try {
          ByteBuffer buffer = ByteBuffer.allocate(200);
          buffer.put(content.getBytes());
          buffer.flip();
          while (buffer.hasRemaining()) {
            channel.write(buffer);
          }
        } catch (IOException e) {
          e.printStackTrace();
        }

        int room = userInfo.getRoom();
        List<String> roomList = rooms.get(room);
        for (String id : roomList) {
          UserInfo user = users.get(id);
          if (user.getName().equals(toUser)) {
            SocketChannel channel2 = user.getChannel();
            try {
              ByteBuffer buffer = ByteBuffer.allocate(200);
              buffer.put(content.getBytes());
              buffer.flip();
              while (buffer.hasRemaining()) {
                channel2.write(buffer);
              }
            } catch (IOException e) {
              e.printStackTrace();
            }
            break;
          }
        }
      }
    }

    System.out.println("chat process over");
  }
}
