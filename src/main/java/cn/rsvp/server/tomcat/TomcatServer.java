package cn.rsvp.server.tomcat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Dai.Liangzhi (dlz@rsvptech.cn)
 * @since 2018/11/13
 */
public class TomcatServer {
  //参数值
  private int MAX_THREAD = 3;
  private int ACCEPT_COUNT = 2;
  private int MIN_SPARE_THREAD = 1;
  private int MAX_IDLE_THREAD = 2;

  //队列
  private ConcurrentLinkedQueue<TomcatThread> mIdleThread;
  private ConcurrentLinkedQueue<TomcatThread> mWorkerThread;
  private ConcurrentLinkedQueue<TomcatThread> mGCThread;
  private ConcurrentLinkedQueue<TomcatTask> mWait;

  //计数器
  private int workerCount = 0;
  private int waitCount = 0;

  //socketServer
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
    mIdleThread = new ConcurrentLinkedQueue<>();
    mWorkerThread = new ConcurrentLinkedQueue<>();
    mGCThread = new ConcurrentLinkedQueue<>();
    mWait = new ConcurrentLinkedQueue<>();

    for (int i = 0; i < MIN_SPARE_THREAD; i++) {
      TomcatThread thread = new TomcatThread();
      thread.start();
      mIdleThread.offer(thread);
    }
  }

  /**
   * SocketServer线程
   */
  public class AcceptSocketThread extends Thread {
    public void run() {
      while (true) {
        try {
          System.out.println("socket accept");
          //接收一个客户端Socket对象
          Socket socket = serverSocket.accept();
          if (socket != null) {
            processSocket(socket);
          }
        } catch (IOException e) {
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
    private boolean isStart;

    public TomcatThread() {
      tomcatTask = null;
      isStart = false;
    }

    public TomcatThread(TomcatTask tomcatTask, boolean isStart) {
      this.tomcatTask = tomcatTask;
      this.isStart = isStart;
    }

    public void setTomcatTask(TomcatTask tomcatTask) {
      this.tomcatTask = tomcatTask;
    }

    public void threadStart() {
      isStart = true;
    }

    @Override
    public void run() {
      while (true) {
        if (!isStart) {
          try {
            Thread.sleep(50);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          continue;
        }

        try {
          System.out.println("tomcatTask=" + tomcatTask.toString());
          tomcatTask.procss();
        } catch (Exception e) {
          e.printStackTrace();
        }
        mGCThread.add(this);
        isStart = false;
      }
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
          if (mGCThread.isEmpty()) {
            Thread.sleep(50);
            continue;
          }

          TomcatThread tomcatThread = mGCThread.poll();
          System.out.println("this thread=" + tomcatThread.toString() + "  GC");
          if (waitCount > 0) {
            TomcatTask tomcatTask = mWait.poll();
            tomcatThread.setTomcatTask(tomcatTask);
            tomcatThread.threadStart();
            waitCount--;
          } else {
            mWorkerThread.remove(tomcatThread);
            mIdleThread.offer(tomcatThread);
            if (mIdleThread.size() > MAX_IDLE_THREAD) {
              TomcatThread tThread = mIdleThread.poll();
              if (tThread != null) {
                tThread.interrupt();
              }
            }
            workerCount--;
          }
          System.out.println("waitCount=" + waitCount);
          System.out.println("workerCount=" + workerCount);
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
    if (workerCount >= MAX_THREAD && waitCount >= ACCEPT_COUNT) {
      try {
        socket.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
      return;
    }

    if (workerCount < MAX_THREAD) {
      TomcatTask tomcatTask = new TomcatTask(socket);
      if (mIdleThread.size() > 0) {
        TomcatThread thread = mIdleThread.poll();
        thread.setTomcatTask(tomcatTask);
        thread.threadStart();
        mWorkerThread.offer(thread);
      } else {
        TomcatThread thread = new TomcatThread(tomcatTask, true);
        thread.start();
        mWorkerThread.offer(thread);
      }
      workerCount++;
    } else if (waitCount < ACCEPT_COUNT) {
      TomcatTask tomcatTask = new TomcatTask(socket);
      mWait.offer(tomcatTask);
      waitCount++;
    }

    System.out.println("waitCount=" + waitCount);
    System.out.println("workerCount=" + workerCount);
  }
}
