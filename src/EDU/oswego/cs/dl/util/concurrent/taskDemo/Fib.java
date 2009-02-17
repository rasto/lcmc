import EDU.oswego.cs.dl.util.concurrent.*;


/**
 * Recursive task-based version of Fibonacci. Computes:
 * <pre>
 * Computes fibonacci(n) = fibonacci(n-1) + fibonacci(n-2);  for n> 1
 *          fibonacci(0) = 0; 
 *          fibonacci(1) = 1.       
 * </pre>
 **/

public class Fib extends FJTask {

  // Performance-tuning constant:
  static int sequentialThreshold = 0;
  
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
        System.out.println("Usage: java Fib <threads> <number> [<sequntialThreshold>]");
        return;
      }

      FJTaskRunnerGroup g = new FJTaskRunnerGroup(procs);
      Fib f = new Fib(num);
      g.invoke(f);
      g.stats();

      long result = f.getAnswer();
      System.out.println("Fib: Size: " + num + " Answer: " + result);
    }
    catch (InterruptedException ex) {}
  }


  // Initialized with argument; replaced with result
  volatile int number;

  Fib(int n) { number = n; }

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
      Fib f1 = new Fib(n - 1);
      Fib f2 = new Fib(n - 2);
      
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

