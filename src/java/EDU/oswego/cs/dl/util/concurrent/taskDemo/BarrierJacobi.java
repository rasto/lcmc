// Barrier version of Jacobi iteration

import EDU.oswego.cs.dl.util.concurrent.*;

public class BarrierJacobi {

  static final int DEFAULT_GRANULARITY = 128;

  /** 
   * The maximum submatrix length (both row-wise and column-wise)
   * for any Segment
   **/

  static int granularity = DEFAULT_GRANULARITY;

  static final double EPSILON = 0.001;  // convergence criterion


  public static void main(String[] args) {
    try {
      int n;
      int steps;
      try {
        n = Integer.parseInt(args[0]);
        steps = Integer.parseInt(args[1]);
        if (args.length > 2) granularity = Integer.parseInt(args[2]);
      }

      catch (Exception e) {
        System.out.println("Usage: java BarrierJacobi <matrix size> <max steps> [<granularity>]");
        return;
      }

      // allocate enough space for edges

      double[][] a = new double[n+2][n+2];
      double[][] b = new double[n+2][n+2];


      // Simple initialization for demo. Fill all edges with 1's.
      // (All interiors are already default-initialized to zero.)

      for (int k = 0; k < n+2; ++k) {
        a[k][0] = 1.0;
        a[k][n+1] = 1.0;
        a[0][k] = 1.0;
        a[n+1][k] = 1.0;

        b[k][0] = 1.0;
        b[k][n+1] = 1.0;
        b[0][k] = 1.0;
        b[n+1][k] = 1.0;
      }

      long startTime = System.currentTimeMillis();

      new Driver(a, b, 1, n, 1, n, steps).compute();

      long time = System.currentTimeMillis() - startTime;
      double secs = ((double)time) / 1000.0;

      System.out.println("Compute Time: " + secs);

    }
    catch (InterruptedException ex) {}
  }

  static class Segment implements Runnable {

    double[][] A; // matrix to get old values from
    double[][] B; // matrix to put new values into

    // indices of current submatrix
    final int loRow;   
    final int hiRow;
    final int loCol;
    final int hiCol;
    final int steps;
    final CyclicBarrier barrier;

    final Segment[] allSegments;

    volatile double maxDiff; // maximum difference between old and new values
    volatile boolean converged = false;

    Segment(double[][] A, double[][] B, 
            int loRow, int hiRow,
            int loCol, int hiCol,
            int steps,
            CyclicBarrier barrier,
            Segment[] allSegments) {
      this.A = A;   this.B = B;
      this.loRow = loRow; this.hiRow = hiRow;
      this.loCol = loCol; this.hiCol = hiCol;
      this.steps = steps;
      this.barrier = barrier;
      this.allSegments = allSegments;
    }

    void convergenceCheck(int step) {
      for (int i = 0; i < allSegments.length; ++i) 
        if (allSegments[i].maxDiff > EPSILON) return;

      System.out.println("Converged after " + step + " steps");

      for (int i = 0; i < allSegments.length; ++i) 
        allSegments[i].converged = true;
    }


    public void run() {
      try {
        double[][] a = A;
        double[][] b = B;
        
        for (int i = 1; i <= steps && !converged; ++i) {
          maxDiff = update(a, b);

          int index = barrier.barrier();
          if (index == 0) convergenceCheck(i);
          barrier.barrier();

          double[][] tmp = a; a = b; b = tmp;
        }
      }
      catch(Exception ex) { 
        return;
      }
    }

    double update(double[][] a, double[][] b) {
      double md = 0.0; // local for computing max diff

      for (int i = loRow; i <= hiRow; ++i) {
        for (int j = loCol; j <= hiCol; ++j) {
          double v = 0.25 * (a[i-1][j] + a[i][j-1] +
                             a[i+1][j] + a[i][j+1]);
          b[i][j] = v;

          double diff = v - a[i][j];
          if (diff < 0) diff = -diff;
          if (diff > md) md = diff;
        }
      }

      return md;
    }
        
  }
        

  static class Driver { 
    double[][] A; // matrix to get old values from
    double[][] B; // matrix to put new values into

    final int loRow;   // indices of current submatrix
    final int hiRow;
    final int loCol;
    final int hiCol;
    final int steps;

    Driver(double[][] mat1, double[][] mat2, 
           int firstRow, int lastRow,
           int firstCol, int lastCol,
           int steps) {
      
      this.A = mat1;   this.B = mat2;
      this.loRow = firstRow; this.hiRow = lastRow;
      this.loCol = firstCol; this.hiCol = lastCol;
      this.steps = steps;
    }

    public void compute() throws InterruptedException {

      int rows = hiRow - loRow + 1;
      int cols = hiCol - loCol + 1;
      int rblocks = rows / granularity;
      int cblocks = cols / granularity;

      int n = rblocks * cblocks;

      System.out.println("Using " + n + " segments (threads)");

      Segment[] segs = new Segment[n];
      Thread[] threads = new Thread[n];
      CyclicBarrier barrier = new CyclicBarrier(n);

      int k = 0;
      for (int i = 0; i < rblocks; ++i) {
        int lr = loRow + i * granularity;
        int hr = lr + granularity;
        if (i == rblocks-1) hr = hiRow;
        
        for (int j = 0; j < cblocks; ++j) {
          int lc = loCol + j * granularity;
          int hc = lc + granularity;
          if (j == cblocks-1) hc = hiCol;

          segs[k] = new Segment(A, B, lr, hr, lc, hc, steps, barrier, segs);
          threads[k] = new Thread(segs[k]);
          ++k;
        }
      }

      for (k = 0; k < n; ++k) threads[k].start();

      for (k = 0; k < n; ++k) threads[k].join();

      double maxd = 0;
      for (k = 0; k < n; ++k) {
        double md = segs[k].maxDiff;
        if (md > maxd) maxd = md;
      }

      System.out.println("Max diff after " + steps + " steps = " + maxd);


    }
  }


}
