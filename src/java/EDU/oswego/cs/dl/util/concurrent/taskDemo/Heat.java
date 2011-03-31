/*
  Converted from heat.cilk, which is
    Copyright (c) 1996 Massachusetts Institute of Technology
  with the following notice:

   * Permission is hereby granted, free of charge, to any person obtaining
   * a copy of this software and associated documentation files (the
   * "Software"), to use, copy, modify, and distribute the Software without
   * restriction, provided the Software, including any modified copies made
   * under this license, is not distributed for a fee, subject to
   * the following conditions:
   * 
   * The above copyright notice and this permission notice shall be
   * included in all copies or substantial portions of the Software.
   * 
   * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
   * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
   * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
   * IN NO EVENT SHALL THE MASSACHUSETTS INSTITUTE OF TECHNOLOGY BE LIABLE
   * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
   * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
   * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
   * 
   * Except as contained in this notice, the name of the Massachusetts
   * Institute of Technology shall not be used in advertising or otherwise
   * to promote the sale, use or other dealings in this Software without
   * prior written authorization from the Massachusetts Institute of
   * Technology.
   *  
*/

import EDU.oswego.cs.dl.util.concurrent.*;

public class Heat {

  // Parameters
  static int nx;
  static int ny;
  static int nt;
  static int leafmaxcol;

  // the matrix representing the cells
  static double[][] newm;

  // alternating workspace matrix
  static double[][] oldm;


  public static void main(String[] args) {
    int procs = 1;
    int benchmark = 0;

    try {
      procs = Integer.parseInt(args[0]);
      benchmark = Integer.parseInt(args[1]);
    }
    catch (Exception e) {
      System.out.println("Usage: java Heat <threads> <0-4>");
      return;
    }

    switch (benchmark) {
    case 0:      /* cilk demo defaults */
      nx = 4096; ny = 512; nt = 100; leafmaxcol = 10; 
      break;
    case 1:      /* cilk short benchmark options */
      nx = 512; ny = 512; nt = 1; leafmaxcol = 10;
      break;
    case 2:      /* cilk standard benchmark options */
      nx = 4096; ny = 512; nt = 40; leafmaxcol = 10;
      break;
    case 3:      /* cilk long benchmark options */
      nx = 4096; ny = 1024; nt = 100; leafmaxcol = 1;
      break;
    case 4:      /* hood demo faults */
      nx = 1024; ny = 512; nt = 100; leafmaxcol = 16;
      break;
    default:
      System.out.println("Usage: java Heat <threads> <0-4>");
      return;
    }

    System.out.print("Parameters: ");
    System.out.print(" granularity = " + leafmaxcol);
    System.out.print(" rows = " + nx);
    System.out.print(" columns = " + ny);
    System.out.println(" steps = " + nt);

    
    oldm = new double[nx][ny];
    newm = new double[nx][ny];

    try {  
    
      FJTaskRunnerGroup g = new FJTaskRunnerGroup(procs);
      
      FJTask main = new FJTask() {
        public void run() {
          for (int timestep = 0; timestep <= nt; timestep++) {
            FJTask.invoke(new Compute(0, nx, timestep));
          }
        }
      };
      
      g.invoke(main);

      g.stats();

    }
    catch (InterruptedException ex) { return; }


  }


  // constants (at least for this demo)
  static final double xu = 0.0;
  static final double xo = 1.570796326794896558;
  static final double yu = 0.0;
  static final double yo = 1.570796326794896558;
  static final double tu = 0.0;
  static final double to = 0.0000001;

  static final double dx = (xo - xu) / (nx - 1);
  static final double dy = (yo - yu) / (ny - 1);
  static final double dt = (to - tu) / nt;	
  static final double dtdxsq = dt / (dx * dx);
  static final double dtdysq = dt / (dy * dy);


  // the function being applied across the cells
  static final double f(double x, double y) { 
    return Math.sin(x) * Math.sin(y); 
  }

  // random starting values

  static final double randa(double x, double t) { 
    return 0.0; 
  }
  static final double randb(double x, double t) { 
    return Math.exp(-2*t) * Math.sin(x); 
  }
  static final double randc(double y, double t) { 
    return 0.0; 
  }
  static final double randd(double y, double t) { 
    return Math.exp(-2*t) * Math.sin(y); 
  }
  static final double solu(double x, double y, double t) { 
    return Math.exp(-2*t) * Math.sin(x) * Math.sin(y); 
  }




  static final class Compute extends FJTask {

    final int lb;
    final int ub;
    final int time;

    Compute(int lowerBound, int upperBound, int timestep) {
      lb = lowerBound;
      ub = upperBound;
      time = timestep;
    }
     
    public void run() {
      if (ub - lb > leafmaxcol) {
        int mid = (lb + ub) / 2;
        coInvoke(new Compute(lb, mid, time),
                 new Compute(mid, ub, time));
      }
      else if (time == 0)     // if first pass, initialize cells
        init();
      else if (time %2 != 0)  // alternate new/old
        compstripe(newm, oldm);
      else
        compstripe(oldm, newm);
    }


    /** Update all cells **/
    final void compstripe(double[][] newMat, double[][] oldMat) {

      // manually mangled to reduce array indexing

      final int llb = (lb == 0)  ? 1 : lb;
      final int lub = (ub == nx) ? nx - 1 : ub;

      double[] west;
      double[] row = oldMat[llb-1];
      double[] east = oldMat[llb];

      for (int a = llb; a < lub; a++) {

        west = row;
        row =  east;
        east = oldMat[a+1];

        double prev;
        double cell = row[0];
        double next = row[1];

        double[] nv = newMat[a];

        for (int b = 1; b < ny-1; b++) {

          prev = cell;
          cell = next;
          double twoc = 2 * cell;
          next = row[b+1];

          nv[b] = cell
            + dtdysq * (prev    - twoc + next)
            + dtdxsq * (east[b] - twoc + west[b]);

        }
      }

      edges(newMat, llb, lub,  tu + time * dt);
    }


    // the original version from cilk
    final void origcompstripe(double[][] newMat, double[][] oldMat) {
      
      final int llb = (lb == 0)  ? 1 : lb;
      final int lub = (ub == nx) ? nx - 1 : ub;

      for (int a = llb; a < lub; a++) {
        for (int b = 1; b < ny-1; b++) {
          double cell = oldMat[a][b];
          double twoc = 2 * cell;
          newMat[a][b] = cell
            + dtdxsq * (oldMat[a+1][b] - twoc + oldMat[a-1][b])
            + dtdysq * (oldMat[a][b+1] - twoc + oldMat[a][b-1]);

        }
      }

      edges(newMat, llb, lub,  tu + time * dt);
    }


    /** Initialize all cells **/
    final void init() {
      final int llb = (lb == 0) ? 1 : lb;
      final int lub = (ub == nx) ? nx - 1 : ub;

      for (int a = llb; a < lub; a++) {	/* inner nodes */
        double[] ov = oldm[a];
        double x = xu + a * dx;
        double y = yu;
        for (int b = 1; b < ny-1; b++) {
          y += dy;
          ov[b] = f(x, y);
        }
      }

      edges(oldm, llb, lub, 0);

    }

    /** Fill in edges with boundary values **/
    final void edges(double [][] m, int llb, int lub, double t) {

      for (int a = llb; a < lub; a++) {
        double[] v = m[a];
        double x = xu + a * dx;
        v[0] = randa(x, t);
        v[ny-1] = randb(x, t);
      }

      if (lb == 0) {
        double[] v = m[0];
        double y = yu;
        for (int b = 0; b < ny; b++) {
          y += dy;
          v[b] = randc(y, t);
        }
      }

      if (ub == nx) {
        double[] v = m[nx - 1]; 
        double y = yu;
        for (int b = 0; b < ny; b++) {
          y += dy;
          v[b] = randd(y, t);
        }
      }
    }
  }
}
