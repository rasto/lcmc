import EDU.oswego.cs.dl.util.concurrent.*;


/**
 * Sample program using Guassian Quadrature for numerical integration.
 * Inspired by a 
 * <A href="http://www.cs.uga.edu/~dkl/filaments/dist.html"> Filaments</A>
 * demo program.
 * 
 */

public class Integrate {


  public static void main(String[] args) {
    int procs;
    double start;
    double end;
    int exp = 5;

    try {
      procs = Integer.parseInt(args[0]);
      start = new Double(args[1]).doubleValue();
      end = new Double(args[2]).doubleValue();
      if (args.length > 3) exp = Integer.parseInt(args[3]);
    }
    catch (Exception e) {
      System.out.println("Usage: java Integrate <threads> <lower bound> <upper bound> <exponent>\n (for example 2 1 48 5).");
      return;
    }

    System.out.println("Integrating from " + start + " to " + end + " exponent: " + exp);

    Function f = new SampleFunction(exp);
    FJTaskRunnerGroup group = new FJTaskRunnerGroup(procs);
    Integrator integrator = new Integrator(f, 0.001, group);
    double result = integrator.integral(start, end);
    
    System.out.println("Answer = " + result);
    group.stats();

  }

  /*
    This is all set up as if it were part of a more serious
    framework, but is for now just a demo, with all
    classes declared as static within Integrate
  */

  /** A function to be integrated **/
  static interface Function {
    double compute(double x);
  }

  /**
   * Sample from filaments demo.
   * Computes (2*n-1)*(x^(2*n-1)) for all odd values
   **/
  static class SampleFunction implements Function {
    final int n;
    SampleFunction(int n) { this.n = n; }

    public double compute(double x)  {
      double power = x;
      double xsq = x * x;
      double val = power;
      double di = 1.0;
      for (int i = n - 1; i > 0; --i) {
        di += 2.0;
        power *= xsq;
        val += di * power;
      }
      return val;
    }
  }


  static class Integrator {
    final Function f;      // The function to integrate
    final double errorTolerance;
    final FJTaskRunnerGroup group;

    Integrator(Function f, double errorTolerance, FJTaskRunnerGroup group) {
      this.f = f;
      this.errorTolerance = errorTolerance;
      this.group = group;
    }

    double integral(double lowerBound, double upperBound) {
      double f_lower = f.compute(lowerBound);
      double f_upper = f.compute(upperBound);
      double initialArea = 0.5 * (upperBound-lowerBound) * (f_upper + f_lower);
      Quad q = new Quad(lowerBound, upperBound,
                        f_lower, f_upper,
                        initialArea);
      try {
        group.invoke(q);
        return q.area;
      }
      catch(InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new Error("Interrupted during computation");
      }
    }

    
    /** 
     * FJTask to recursively perform the quadrature.
     * Algorithm:
     *  Compute the area from lower bound to the center point of interval,
     *  and from the center point to the upper bound. If this
     *  differs from the value from lower to upper by more than
     *  the error tolerance, recurse on each half.
     **/
    class Quad extends FJTask {
      final double left;       // lower bound
      final double right;      // upper bound
      final double f_left;     // value of the function evaluated at left
      final double f_right;    // value of the function evaluated at right
    
      // Area initialized with original estimate from left to right.
      // It is replaced with refined value.
      volatile double area;
    
      Quad(double left, double right, 
           double f_left, double f_right, 
           double area) {
        this.left = left;
        this.right = right;
        this.f_left = f_left;
        this.f_right = f_right;
        this.area = area;
      }
      
      public void run() {
        double center = 0.5 * (left + right);
        double f_center = f.compute(center); 
        
        double leftArea  = 0.5 * (center - left)  * (f_left + f_center); 
        double rightArea = 0.5 * (right - center) * (f_center + f_right);
        double sum = leftArea + rightArea;
        
        double diff = sum - area;
        if (diff < 0) diff = -diff;
        
        if (diff >= errorTolerance) { 
          Quad q1 = new Quad(left,   center, f_left,   f_center, leftArea);
          Quad q2 = new Quad(center, right,  f_center, f_right,  rightArea);
          coInvoke(q1, q2);
          sum = q1.area + q2.area; 
        }
        
        area = sum;
      }
    }
  }

}

  
