package cn.rsvp.server.bean;

import java.io.PrintWriter;

/**
 * @author Dai.Liangzhi (dlz@rsvptech.cn)
 * @since 2018/10/16
 */
public class UserInfo {
  private String userid;
  private int room;
  private String name;
  private PrintWriter pw;

  public UserInfo(String userid, int room, String name, PrintWriter pw) {
    this.userid = userid;
    this.room = room;
    this.name = name;
    this.pw = pw;
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

  public PrintWriter getPw() {
    return pw;
  }

  public void setRoom(int room) {
    this.room = room;
  }
}
