package cn.rsvp.server.bean;

import java.io.PrintWriter;
import java.nio.channels.SocketChannel;

/**
 * @author Dai.Liangzhi (dlz@rsvptech.cn)
 * @since 2018/10/16
 */
public class UserInfo {
  private String userid;
  private int room;
  private String name;
  private SocketChannel channel;

  public UserInfo(String userid, int room, String name, SocketChannel channel) {
    this.userid = userid;
    this.room = room;
    this.name = name;
    this.channel = channel;
  }

  public String getUserid() {
    return userid;
  }

  public int getRoom() {
    return room;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public SocketChannel getChannel() {
    return channel;
  }

  public void setRoom(int room) {
    this.room = room;
  }
}
