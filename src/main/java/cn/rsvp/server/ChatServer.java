package cn.rsvp.server;

/**
 * @author Dai.Liangzhi (dlz@rsvptech.cn)
 * @since 2018/9/25
 */
public class ChatServer {

  //主函数调用
  public static void main(String args[]) {
    new SocketServer(2881);
    new SocketServer(2882);
    new SocketServer(2883);
  }
}
