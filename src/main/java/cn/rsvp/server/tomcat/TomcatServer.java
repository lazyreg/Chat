package cn.rsvp.server.tomcat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Dai.Liangzhi (dlz@rsvptech.cn)
 * @since 2018/11/13
 */
public class TomcatServer {
  private int maxThread = 3;
  private int acceptCount = 2;
  private int minSpareThreads = 1;
  private int maxIdleThreads = 2;

  private List<TomcatThread> mIdleThread;
  private List<TomcatThread> mWorkerThread;
  private List<TomcatTask> mWait;

  private List<TomcatThread> mGCThread;

  private int workerCount = 0;
  private int waitCount = 0;

  private ServerSocket serverSocket;

  public TomcatServer(int port) {
    //初始化线程
    initThread();

    try {
      //创建服务器端套接字ServerSocket,端口监听
      serverSocket = new ServerSocket(port);

      //创建接受Socket读线程实例，并启动
      new AcceptSocketThread().start();
      new GcThread().start();
    } catch (IOException e) {
      e.printStackTrace();
    }
    System.out.println(port + "端口服务已经启动...");
  }

  /**
   * 初始化最小线程数
   */
  private void initThread() {
    mIdleThread = new ArrayList<>();
    mWorkerThread = new ArrayList<>();
    mGCThread = new ArrayList<>();
    mWait = new ArrayList<>();

    for (int i = 0; i < minSpareThreads; i++) {
      TomcatThread thread = new TomcatThread();
      mIdleThread.add(thread);
    }
  }

  /**
   * SocketServer线程
   */
  public class AcceptSocketThread extends Thread {
    public void run() {
      while (true) {
        try {
          if (workerCount >= maxThread && waitCount >= acceptCount) {
            Thread.sleep(50);
            continue;
          }

          System.out.println("socket wait");
          //接收一个客户端Socket对象
          Socket socket = serverSocket.accept();
          if (socket != null) {
            processSocket(socket);
          }
        } catch (IOException e) {
          e.printStackTrace();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * Tomcat线程
   */
  public class TomcatThread extends Thread {
    private TomcatTask tomcatTask;

    public void setTomcatTask(TomcatTask tomcatTask) {
      this.tomcatTask = tomcatTask;
    }

    @Override
    public void run() {
      try {
        tomcatTask.procss();
      } catch (Exception e) {
        e.printStackTrace();
      }

      mGCThread.add(this);
    }
  }

  /**
   * 回收线程
   */
  public class GcThread extends Thread {

    @Override
    public void run() {
      try {
        while (true) {
          if (mGCThread.size() == 0) {
            Thread.sleep(50);
            continue;
          }

          TomcatThread tomcatThread = mGCThread.remove(0);
          System.out.println("this thread=" + tomcatThread.toString() + "  GC");
          if (waitCount > 0) {
            TomcatTask tomcatTask = mWait.get(0);
            tomcatThread.setTomcatTask(tomcatTask);
            tomcatThread.start();
            waitCount--;
            System.out.println("waitCount=" + waitCount);
          } else {
            mWorkerThread.remove(tomcatThread);
            mIdleThread.add(tomcatThread);
            if (mIdleThread.size() > maxIdleThreads) {
              mIdleThread.remove(0);
            }
            workerCount--;
            System.out.println("workerCount=" + workerCount);
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * 处理socket
   *
   * @param socket
   */
  private void processSocket(Socket socket) {
    if (workerCount < maxThread) {
      TomcatTask tomcatTask = new TomcatTask(socket);
      if (mIdleThread.size() > 0) {
        TomcatThread thread = mIdleThread.get(0);
        thread.setTomcatTask(tomcatTask);
        thread.start();
        mIdleThread.remove(thread);
        mWorkerThread.add(thread);
      } else {
        TomcatThread thread = new TomcatThread();
        thread.setTomcatTask(tomcatTask);
        thread.start();
        mWorkerThread.add(thread);
      }
      workerCount++;
    } else if (waitCount < acceptCount) {
      TomcatTask tomcatTask = new TomcatTask(socket);
      mWait.add(tomcatTask);
      waitCount++;
    }

    System.out.println("waitCount=" + waitCount);
    System.out.println("workerCount=" + workerCount);
  }
}
