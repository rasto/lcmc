// Jacobi iteration on a mesh. Based loosely on a Filaments demo

import EDU.oswego.cs.dl.util.concurrent.*;

public class Jacobi {

  static final int DEFAULT_LEAFCELLS = 1024;

  /** 
   * The maximum number of matrix cells 
   * at which to stop recursing down and instead directly update.
   **/

  static final double EPSILON = 0.001;  // convergence criterion

  public static void main(String[] args) {
    try {
      int procs;
      int n;
      int steps;
      int granularity = DEFAULT_LEAFCELLS;

      try {
        procs = Integer.parseInt(args[0]);
        n = Integer.parseInt(args[1]);
        steps = Integer.parseInt(args[2]);
        if (args.length > 3) granularity = Integer.parseInt(args[3]);
      }

      catch (Exception e) {
        System.out.println("Usage: java Jacobi <threads> <matrix size> <max steps> [<leafcells>]");
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

      Driver driver = new Driver(a, b, 1, n, 1, n, steps, granularity);

      FJTaskRunnerGroup g = new FJTaskRunnerGroup(procs);
      g.invoke(driver);
      g.stats();

    }
    catch (InterruptedException ex) {}
  }

  abstract static class MatrixTree extends FJTask {

    // maximum difference between old and new values
    volatile double maxDiff; 

  }


  static class LeafNode extends MatrixTree {
    final double[][] A; // matrix to get old values from
    final double[][] B; // matrix to put new values into

    // indices of current submatrix
    final int loRow;    final int hiRow;
    final int loCol;    final int hiCol;

    int steps = 0; // track even/odd steps

    LeafNode(double[][] A, double[][] B, 
             int loRow, int hiRow,
             int loCol, int hiCol) {
      this.A = A;   this.B = B;
      this.loRow = loRow; this.hiRow = hiRow;
      this.loCol = loCol; this.hiCol = hiCol;
    }

    public synchronized void run() { 
      
      boolean AtoB = (steps++ % 2) == 0;
      double[][] a = (AtoB)? A : B;
      double[][] b = (AtoB)? B : A;

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

      maxDiff = md;
    }
  }



  static class FourNode extends MatrixTree {
    final MatrixTree[] quads;

    FourNode(MatrixTree q1, MatrixTree q2, 
             MatrixTree q3, MatrixTree q4) {
      quads = new MatrixTree[] { q1, q2, q3, q4 };
    }

    public void run() { 
      coInvoke(quads);

      double md = quads[0].maxDiff;
      quads[0].reset(); 

      double m = quads[1].maxDiff;
      quads[1].reset(); 
      if (m > md) md = m;

      m = quads[2].maxDiff;
      quads[2].reset(); 
      if (m > md) md = m;

      m = quads[3].maxDiff;
      quads[3].reset(); 
      maxDiff =  (m > md)? m : md;
    }
  }
        

  static class TwoNode extends MatrixTree {
    final MatrixTree q1;
    final MatrixTree q2;

    TwoNode(MatrixTree q1, MatrixTree q2) {
      this.q1 = q1; this.q2 = q2;
    }

    public void run() { 
      FJTask.coInvoke(q1, q2);
      double m1 = q1.maxDiff;
      double m2 = q2.maxDiff;
      maxDiff = (m1 > m2)? m1: m2;
      q1.reset();
      q2.reset();
    }
       
  }
        

  static class Driver extends FJTask {
    final MatrixTree mat;
    final int steps;

    Driver(double[][] A, double[][] B, 
           int firstRow, int lastRow,
           int firstCol, int lastCol,
           int steps, int leafCells) {
      this.steps = steps;
      mat = build(A, B, firstRow, lastRow, firstCol, lastCol, leafCells);
    }

    MatrixTree build(double[][] a, double[][] b,
                     int lr, int hr, int lc, int hc, int gran) {
      int rows = (hr - lr + 1);
      int cols = (hc - lc + 1);

      int mr = (lr + hr) / 2; // midpoints
      int mc = (lc + hc) / 2;
      
      int hrows = (mr - lr + 1);
      int hcols = (mc - lc + 1);

      if (rows * cols <= gran) {
        return new LeafNode(a, b, lr, hr, lc, hc);
      }
      else if (hrows * hcols >= gran) {
        return new FourNode(build(a, b, lr,   mr, lc,   mc, gran),
                            build(a, b, lr,   mr, mc+1, hc, gran),
                            build(a, b, mr+1, hr, lc,   mc, gran),
                            build(a, b, mr+1, hr, mc+1, hc, gran));
      }
      else if (cols >= rows) {
        return new TwoNode(build(a, b, lr, hr, lc,   mc, gran),
                           build(a, b, lr, hr, mc+1, hc, gran));
      }
      else {
        return new TwoNode(build(a, b, lr,   mr, lc, hc, gran),
                           build(a, b, mr+1, hr, lc, hc, gran));
        
      }
    }


    public void run() {
      double md = 0.0;

      for (int i = 1; i <= steps; ++i) {
        invoke(mat);
        md = mat.maxDiff;
        if (md < EPSILON) { 
          System.out.println("Converged after " + i + " steps");
          return;
        }
        else
          mat.reset();
      }

      System.out.println("max diff after " + steps + " steps = " + md);
    }
  }


}
