package cn.rsvp.datatype;

import java.io.Serializable;

/**
 * @author Dai.Liangzhi (dlz@rsvptech.cn)
 * @since 2018/10/16
 */
public class ChatInfo implements Serializable {
  private String toUser;
  private String fromUser;
  private String content;

  public ChatInfo() {
  }

  public ChatInfo(String toUser, String fromUser, String content) {
    this.toUser = toUser;
    this.fromUser = fromUser;
    this.content = content;
  }

  public String getToUser() {
    return toUser;
  }

  public void setToUser(String toUser) {
    this.toUser = toUser;
  }

  public String getFromUser() {
    return fromUser;
  }

  public void setFromUser(String fromUser) {
    this.fromUser = fromUser;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }
}
