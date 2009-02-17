import EDU.oswego.cs.dl.util.concurrent.*;
import java.net.*;
import java.io.*;

/**
 * Recursive task-based version of Fibonacci. Computes:
 * <pre>
 * Computes fibonacci(n) = fibonacci(n-1) + fibonacci(n-2);  for n> 1
 *          fibonacci(0) = 0; 
 *          fibonacci(1) = 1.       
 * </pre>
 **/

public class SFib extends FJTask {

  // Performance-tuning constant:
  static int sequentialThreshold = 0;
  
  public static void main(String[] args) {
    try {
      int procs;
      //      int num;
      try {
        procs = Integer.parseInt(args[0]);
        //        num = Integer.parseInt(args[1]);
        if (args.length > 2) sequentialThreshold = Integer.parseInt(args[2]);
      }
      catch (Exception e) {
        System.out.println("Usage: java SFib <threads> <number> [<sequntialThreshold>]");
        return;
      }

      FJTaskRunnerGroup group = new FJTaskRunnerGroup(procs);
      ServerSocket socket = new ServerSocket(1618);
      for (;;) {
         final Socket connection = socket.accept();
         group.execute(new Handler(connection));
      }

    }
    catch (Exception e) { e.printStackTrace(); }
  }

  static class Handler extends FJTask {
    final Socket s;
    Handler(Socket s) { this.s = s; }
    public void run() { 
      try {
        DataInputStream i = new DataInputStream(s.getInputStream());
        DataOutputStream o = new DataOutputStream(s.getOutputStream());
        int n = i.readInt();
        SFib f = new SFib(n);
        invoke(f);
        o.writeInt(f.getAnswer());
      }
      catch (Exception e) { e.printStackTrace(); }
    }
  }


  // Initialized with argument; replaced with result
  volatile int number;

  SFib(int n) { number = n; }

  int getAnswer() {
    if (!isDone()) throw new Error("Not yet computed");
    return number;
  }


  public void run() {
    int n = number;

    // Handle base cases:
    if (n <= 1) {
      // Do nothing: fib(0) = 0; fib(1) = 1
    }
    // Use sequential code for small problems:
    else if (n <= sequentialThreshold) {
      number = seqFib(n);
    }
    // Otherwise use recursive parallel decomposition:
    else {
      // Construct subtasks:
      SFib f1 = new SFib(n - 1);
      SFib f2 = new SFib(n - 2);
      
      // Run them in parallel:
      coInvoke(f1, f2);  
      
      // Combine results:
      number = f1.number + f2.number;
      // (We know numbers are ready, so directly access them.)
    }
  }

  // Sequential version for arguments less than threshold
  static int seqFib(int n) {
    if (n <= 1) 
      return n;
    else 
      return seqFib(n-1) + seqFib(n-2);
  }

}

