import EDU.oswego.cs.dl.util.concurrent.*;
import java.util.*;

public class BufferTasks extends FJTask {

  static int niters = 1024 * 64;
  static int[] pairs = {1, 2, 4, 8, 16, 32, 64};
  static int[] sizes = { 1024, 64, 1 };

  public static void main(String[] args) {
    try {
      int procs;
      try {
        procs = Integer.parseInt(args[0]);
      }
      catch (Exception e) {
        System.out.println("Usage: java BufferTasks <threads>");
        return;
      }

      System.out.print("pairs:");
      for (int p = 0; p < pairs.length; ++p) 
        System.out.print("\t" + pairs[p]);
      System.out.print("\n");


      FJTaskRunnerGroup g = new FJTaskRunnerGroup(procs);
      g.invoke(new BufferTasks());
    }
    catch (InterruptedException ex) {}
  }

  public void run() {
    for (int s = 0; s < sizes.length; ++s) {
      System.out.println("cap: " + sizes[s]);

      for (int p = 0; p < pairs.length; ++p) {

        buffer = new Buffer(sizes[s]);
        int npairs = pairs[p];
        int iters = niters / npairs;

        long startTime = System.currentTimeMillis();
        setCallbackCount(npairs * 2);

        for (int k = 0; k < npairs; ++k) {
          new Producer(iters).fork();
          new Consumer(iters).fork();
        }
        
        while (!checkDone()) yield();

        long now = System.currentTimeMillis();
        long time = now - startTime;
        long tpi = (time * 1000) / (npairs * niters);
        System.out.print("\t" + tpi);

      }

      System.out.print("\n");

      getFJTaskRunnerGroup().stats();
    }
  }


  /**
   * Keep track of callbacks so that test driver knows when
   * to terminate
   **/
  int callbackCount;
  synchronized void notifyDone() { --callbackCount; }
  synchronized void setCallbackCount(int c) { callbackCount = c;  }
  synchronized boolean checkDone() { return callbackCount == 0;  }

  /** The shared buffer **/
  Buffer buffer;

  class Producer extends FJTask {
    final int iters;
    Producer(int n) { iters = n;  }
    
    public void run() {
      for (int n = iters; n > 0; --n) {
        // If cannot continue, create a new task to
        // take our place, and start it.
        if (!buffer.offer(new Integer(n))) { // Doesn't matter what's put in
          yield();
          new Producer(n).start();
          return;
        }
      }
      notifyDone();
    }
  }


  class Consumer extends FJTask {
    final int iters;

    Consumer(int n) { iters = n;  }

    public void run() {
      for (int n = iters; n > 0; --n) {
        // If cannot continue, create a new task to
        // take our place, and start it.
        if (buffer.poll() == null) {
          yield();
          new Consumer(n).start();
          return;
        }
      }
      notifyDone();
    }
  }


  static class Buffer {

    protected Object[]  array_;      // the elements
    protected int putPtr_ = 0;       // circular indices
    protected int takePtr_ = 0;     
    
    final NonBlockingSemaphore putPermits;
    final NonBlockingSemaphore takePermits;
    
    public Buffer(int capacity){
      putPermits = new NonBlockingSemaphore(capacity);
      takePermits = new NonBlockingSemaphore(0);
      array_ = new Object[capacity];
    }
    
    public boolean offer(Object x){
      if (!putPermits.attempt()) return false;
      synchronized(this) {
        array_[putPtr_] = x;
        if (++putPtr_ == array_.length) putPtr_ = 0;
      }
      takePermits.release();
      return true;
    }
    
    public Object poll() {
      if (!takePermits.attempt()) return null;
      Object x;
      synchronized(this) {
        x = array_[takePtr_];
        array_[takePtr_] = null;
        if (++takePtr_ == array_.length) takePtr_ = 0;
      }
      putPermits.release();
      return x;
    }
    
  }


  static class NonBlockingSemaphore {
    private long permits_;
    
    public NonBlockingSemaphore(long initialPermits) {  
      permits_ = initialPermits; 
    }
    
    public synchronized boolean attempt() {
      if (permits_ > 0) {
        --permits_;
        return true;
      }
      else
        return false;
    }
    
    public synchronized void release() {
      ++permits_;
    }
  }
}



