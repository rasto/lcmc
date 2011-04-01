import EDU.oswego.cs.dl.util.concurrent.*;


/**
 * Callback version of Fibonacci. Computes:
 * <pre>
 * Computes fibonacci(n) = fibonacci(n-1) + fibonacci(n-2);  for n> 1
 *          fibonacci(0) = 0; 
 *          fibonacci(1) = 1.       
 * </pre>
 **/

public class FibVCB extends FJTask {

  // Performance-tuning constant:
  static int sequentialThreshold = 1;
  
  public static void main(String[] args) {
    try {
      int procs;
      int num;
      try {
        procs = Integer.parseInt(args[0]);
        num = Integer.parseInt(args[1]);
        if (args.length > 2) sequentialThreshold = Integer.parseInt(args[2]);
      }
      catch (Exception e) {
        System.out.println("Usage: java FibVCB <threads> <number> [<sequntialThreshold>]");
        return;
      }

      FJTaskRunnerGroup g = new FJTaskRunnerGroup(procs);
      FibVCB f = new FibVCB(num, null);
      g.invoke(f);
      g.stats();

      long result = f.getAnswer();
      System.out.println("FibVCB: Size: " + num + " Answer: " + result);
    }
    catch (InterruptedException ex) {}
  }


  volatile int number = 0;
  final FibVCB parent;            // callback target
  int callbacksExpected = 0; 
  volatile int callbacksReceived = 0;

  FibVCB(int n, FibVCB p) { number = n; parent = p; }

  // Callback method called from subtasks upon completion
  synchronized void addResult(int n) { 
    number += n;
    ++callbacksReceived;
  }

  synchronized int getAnswer() {
    if (!isDone()) throw new Error("Not yet computed");
    return number;
  }
  
  public void run() {  // same structure as join-based version
    int n = number;
    
    if (n <= 1) {
      // nothing
    }

    else if (n <= sequentialThreshold) { 
      number = seqFib(n);
    }

    else {
      // clear number so subtasks can fill in
      number = 0;

      // establish number of callbacks expected
      callbacksExpected = 2;


      new FibVCB(n - 1, this).fork();
      new FibVCB(n - 2, this).fork();

      // Wait for callbacks from children
      while (callbacksReceived < callbacksExpected) yield(); 
    }

    // Call back parent
    if (parent != null) parent.addResult(number); 
  }


  // Sequential version for arguments less than threshold
  static int seqFib(int n) {
    if (n <= 1) 
      return n;
    else 
      return seqFib(n-1) + seqFib(n-2);
  }

}

