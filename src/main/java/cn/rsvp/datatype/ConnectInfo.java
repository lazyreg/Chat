package cn.rsvp.datatype;

import java.io.Serializable;

/**
 * @author Dai.Liangzhi (dlz@rsvptech.cn)
 * @since 2018/10/16
 */
public class ConnectInfo implements Serializable {
  private int room;
  private String name;

  public ConnectInfo() {
  }

  public ConnectInfo(int room, String name) {
    this.room = room;
    this.name = name;
  }

  public int getRoom() {
    return room;
  }

  public void setRoom(int room) {
    this.room = room;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
