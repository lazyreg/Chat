package cn.rsvp.server;

import cn.rsvp.server.socket.SocketServerChannel;
import cn.rsvp.server.tomcat.TomcatServer;

/**
 * @author Dai.Liangzhi (dlz@rsvptech.cn)
 * @since 2018/9/25
 */
public class ChatServer {

  //主函数调用
  public static void main(String args[]) {
    //new SocketServer(2881);
    //new SocketServer(2882);
    //new SocketServer(2883);

    //SocketServerChannel channel = new SocketServerChannel(2881);
    //channel.listen();

    new TomcatServer(2881);
  }
}
