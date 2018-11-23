package cn.rsvp.client;

import cn.rsvp.datatype.MessageInfo;
import cn.rsvp.datatype.ChatInfo;
import cn.rsvp.datatype.ConnectInfo;
import org.apache.commons.lang3.StringUtils;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * @author Dai.Liangzhi (dlz@rsvptech.cn)
 * @since 2018/9/25
 */
public class ChatClient extends JFrame {
  private static final long serialVersionUID = 1L;
  Socket socket;
  GetMsgFromServer client;

  PrintWriter pWriter;
  BufferedReader bReader;
  JPanel panelSend;
  JScrollPane sPane;
  JTextArea txtContent;
  JLabel lblName, lblSend;
  JTextField txtName, txtSend;
  JButton btnSend;

  JPanel panelRoom;
  JButton btnRoom1, btnRoom2, btnRoom3, btnRoom4;
  JLabel txtRoom;

  private int curRoom = -1;

  public ChatClient() {
    super("聊天室");
    txtContent = new JTextArea();
    //设置文本域只读
    txtContent.setEditable(false);
    sPane = new JScrollPane(txtContent);

    lblName = new JLabel("昵称:");
    txtName = new JTextField(5);
    lblSend = new JLabel("发言:");
    txtSend = new JTextField(20);
    btnSend = new JButton("发送");

    panelSend = new JPanel();
    panelSend.add(lblName);
    panelSend.add(txtName);
    panelSend.add(lblSend);
    panelSend.add(txtSend);
    panelSend.add(btnSend);
    this.add(panelSend, BorderLayout.SOUTH);

    btnRoom1 = new JButton("聊天室1");
    btnRoom2 = new JButton("聊天室2");
    btnRoom3 = new JButton("聊天室3");
    txtRoom = new JLabel("当前聊天室:无");
    btnRoom4 = new JButton("退出");

    panelRoom = new JPanel();
    panelRoom.add(btnRoom1);
    panelRoom.add(btnRoom2);
    panelRoom.add(btnRoom3);
    panelRoom.add(txtRoom);
    panelRoom.add(btnRoom4);
    this.add(panelRoom, BorderLayout.NORTH);

    this.add(sPane);
    this.setSize(500, 300);
    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    //注册监听
    btnSend.addActionListener(new SendBtnListener());

    btnRoom1.addActionListener(new RoomBtnListener(1));
    btnRoom2.addActionListener(new RoomBtnListener(2));
    btnRoom3.addActionListener(new RoomBtnListener(3));
    btnRoom4.addActionListener(new RoomBtnListener(0));
    btnRoom4.setVisible(false);
  }

  //接受服务器读返回信息读线程
  class GetMsgFromServer extends Thread {
    @Override
    public void run() {
      try {
        while (this.isAlive()) {
          String strMsg = bReader.readLine();
          if (strMsg != null) {
            //在文本域中显示聊天信息
            txtContent.append(strMsg + "\n");
            txtContent.setCaretPosition(txtContent.getDocument().getLength());
          }
          Thread.sleep(50);
        }
      } catch (Exception e) {
        System.out.println("socket close");
        if (socket != null && !socket.isClosed()) {
          try {
            socket.close();
          } catch (IOException e1) {
            e1.printStackTrace();
          }
          socket = null;
        }
      }
    }
  }

  // 发送信息
  class SendBtnListener implements ActionListener {

    //@Override
    public void actionPerformed(ActionEvent e) {
      if (curRoom >= 1 && curRoom <= 3) {
        // 获取用户输入读文本
        String strName = txtName.getText();
        if (StringUtils.isEmpty(strName)) {
          return;
        }

        String strMsg = txtSend.getText();
        if (StringUtils.isEmpty(strMsg)) {
          return;
        }

        String toUser = null;
        if (strMsg.contains("@")) {
          int index = strMsg.indexOf("@");
          toUser = strMsg.substring(0, index);
          strMsg = strMsg.substring(index + 1);
          if (StringUtils.isEmpty(strMsg)) {
            return;
          }
        }

        MessageInfo messageInfo = new MessageInfo();
        ChatInfo chatInfo = new ChatInfo(toUser, strName, strMsg);
        messageInfo.setType("chat");
        messageInfo.setChat(chatInfo);
        messageInfo.setTimestamp(System.currentTimeMillis());
        String response = messageInfo.toJsonString();

        try {
          //通过输出流将数据发送给服务器
          pWriter.println(response);
          pWriter.flush();
          //清空文本框
          txtSend.setText("");
        } catch (Exception e1) {
          if (socket != null && !socket.isClosed()) {
            try {
              socket.close();
            } catch (IOException e2) {
              e2.printStackTrace();
            }
          }
          socket = null;
          e1.printStackTrace();
        }
      }
    }
  }

  // 选择聊天室
  class RoomBtnListener implements ActionListener {
    private int room;

    public RoomBtnListener(int room) {
      this.room = room;
    }

    //@Override
    public void actionPerformed(ActionEvent e) {
      if (curRoom == room) {
        return;
      }

      if (room == 0) {
        txtRoom.setText("当前聊天室:无");
        txtContent.setText("");
        btnRoom4.setVisible(false);
        if (socket != null) {
          try {
            socket.close();
          } catch (IOException e1) {
            e1.printStackTrace();
          }
          socket = null;
        }
        return;
      }

      curRoom = room;
      txtRoom.setText("当前聊天室:" + room);
      btnRoom4.setVisible(true);
      txtContent.setText("");

      if (socket == null) {
        try {
          //创建一个套接字
          socket = new Socket("127.0.0.1", 2881);
          //创建一个往套接字中写数据的管道，即输出流，给服务器发送信息
          pWriter = new PrintWriter(socket.getOutputStream());
          //创建一个聪套接字读数据的管道，即输入流，读服务器读返回信息
          bReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e1) {
          e1.printStackTrace();
        }

        new GetMsgFromServer().start();
      }

      String strName = txtName.getText();
      MessageInfo messageInfo = new MessageInfo();
      ConnectInfo connectInfo = new ConnectInfo(room, strName);
      messageInfo.setType("entry");
      messageInfo.setConnect(connectInfo);
      messageInfo.setTimestamp(System.currentTimeMillis());
      String response = messageInfo.toJsonString();

      //通过输出流将数据发送给服务器
      pWriter.println(response);
      pWriter.flush();
    }
  }

  public static void main(String[] args) {
    //创建聊天室客户端窗口实例，并显示
    new ChatClient().setVisible(true);
  }
}