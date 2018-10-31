package cn.rsvp.datatype;

import com.alibaba.fastjson.JSON;

import java.io.Serializable;

/**
 * @author Dai.Liangzhi (dlz@rsvptech.cn)
 * @since 2018/10/16
 */
public class MessageInfo implements Serializable {
  private String type;
  private ChatInfo chat;
  private ConnectInfo connect;
  private long timestamp;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public ChatInfo getChat() {
    return chat;
  }

  public void setChat(ChatInfo chat) {
    this.chat = chat;
  }

  public ConnectInfo getConnect() {
    return connect;
  }

  public void setConnect(ConnectInfo connect) {
    this.connect = connect;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public String toJsonString() {
    return JSON.toJSONString(this);
  }

  public static MessageInfo parseJsonString(String json) {
    if (json == null) {
      return null;
    }

    MessageInfo result = JSON.parseObject(json, MessageInfo.class);
    return result;
  }
}

