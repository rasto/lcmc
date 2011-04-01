// Adapted from a cilk demo

import EDU.oswego.cs.dl.util.concurrent.*;

class NQueens extends FJTask {

  static int boardSize;

  public static void main(String[] args) {
    try {
      int procs;
      try {
        procs = Integer.parseInt(args[0]);
        boardSize = Integer.parseInt(args[1]);
      }
      catch (Exception e) {
        System.out.println("Usage: java NQueens <threads> <boardSize>");
        return;
      }

      if (boardSize <= 3) {
        System.out.println("There is no solution for board size <= 3");
        return;
      }

                           
      FJTaskRunnerGroup g = new FJTaskRunnerGroup(procs);
      NQueens f = new NQueens(new int[0]);
      g.execute(f);

      int[] board = result.await();

      g.stats();

      System.out.print("Result:");

      for (int i = 0; i < board.length; ++i) {
        System.out.print(" " + board[i]);
      }
      System.out.println();

    }
    catch (InterruptedException ex) {}
  }

  /** 
   * Global variable holding the result of search.
   * FJTasks  check this to see if it is nonnull,
   * if so, returning early because a result has been found.
   * In a more serious program, we might use a fancier scheme
   * to reduce read/write pressure on this variable.
   **/

  static final class Result {
    private int[] board = null;

    synchronized int[] get() { return board; }

    synchronized void set(int[] b) {
      if (board == null) { board = b; notifyAll(); }
    }

    synchronized int[] await() throws InterruptedException {
      while (board == null) { wait(); }
      return board;
    }
  }

  static final Result result = new Result();

  // Boards are represented as arrays where each cell 
  // holds the column number of the queen in that row

  final int[] sofar;
  NQueens(int[] a) { this.sofar = a;  }

  public void run() {
    if (result.get() == null) { // check if already solved
      int row = sofar.length;

      if (row >= boardSize) // done
        result.set(sofar);
      else {
        for (int q = 0; q < boardSize; ++q) {
          
          // Check if can place queen in column q of next row
          boolean attacked = false;
          for (int i = 0; i < row; i++) {
            int p = sofar[i];
            if (q == p || q == p - (row - i) || q == p + (row - i)) {
              attacked = true;
              break;
            }
          }
          
          // Fork to explore moves from new configuration
          if (!attacked) { 
            int[] next = new int[row+1];
            for (int k = 0; k < row; ++k) next[k] = sofar[k];
            next[row] = q;
            new NQueens(next).fork();
          }
        }
      }
    }
  }

}
