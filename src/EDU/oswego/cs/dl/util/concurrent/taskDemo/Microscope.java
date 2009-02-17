
import java.awt.*;
import javax.swing.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.event.*;
import EDU.oswego.cs.dl.util.concurrent.*;


/**
 * Microscope implements a version of the 7th Guest
 * game found looking in the Microscope in the laboratory.
 * See <a href="http://gee.cs.oswego.edu/dl/applets/micro.html">
 * Microscope</a> version for instructions.
 * <p>
 * The code has been mangled beyond recognition 
 * as a test of the FJTasks package.
 **/

public class Microscope extends JPanel {

  /*
   * If true, the move finder uses a repeatable evaluation
   * strategy, so all self-play games at same level have same outcome.
   * This is useful for testing purposes, but much less fun to watch.
   */

  static boolean DETERMINISTIC = false;

  // Command-line parameters

  static int nprocs;
  static int lookAheads = 3;
  static boolean autostart = false;


  public static void main(String[] args) {
    try {
      nprocs = Integer.parseInt(args[0]);
      if (args.length > 1) {
        autostart = true;
        lookAheads = Integer.parseInt(args[1]);
        DETERMINISTIC = true;
      }
    }
    catch (Exception e) {
      System.out.println("Usage: java Microscope <threads> [<level>]");
      return;
    }

    JFrame frame = new JFrame();
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {System.exit(0);}});

    Microscope t = new Microscope();
    frame.setSize(new Dimension(400, 400));
    frame.getContentPane().add(t);
    frame.setVisible(true);
    t.init();
  }

  // representations:

  Board board = new Board();        // The current board representation

  synchronized Board getBoard() { return board; }
  synchronized void  setBoard(Board b) { board = b; boardPanel.repaint(); }

  Player player = Player.Blue;      // current player (BLUE, GREEN)

  synchronized Player getPlayer() { return player; }
  synchronized void setPlayer(Player p) { player = p; }


  final AutoMover auto;                  // The move finder.
  final User user;                       // Mover for user moves
  Mover mover = null;    // the current Mover (always == auto or user or null)

  synchronized Mover getMover() { return mover; }
  synchronized void setMover(Mover m) { mover = m; }
  synchronized boolean isMoving() { return mover != null; }

  Vector history = new Vector();    // List of completed moves;

  boolean demoMode = true;
  synchronized boolean getDemoMode() { return demoMode; }
  synchronized void setDemoMode(boolean b) { demoMode = b; }
  synchronized boolean toggleDemoMode() { return demoMode = !demoMode; }

  final BoardPanel boardPanel = new BoardPanel();

  JLabel scoreLabel = new JLabel("Score:   0 ");
  JButton autoButton = new JButton(" Start ");
  JButton undoButton = new JButton("Undo");
  JButton modeButton = new JButton("Demo mode");
  JSlider levelSlider = new JSlider(JSlider.VERTICAL, 2, 6, lookAheads);

  public Microscope() {  
    auto = new AutoMover(this);
    user = new User(this);

    JPanel topPanel = new JPanel();
    autoButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (!isMoving()) {
          startMover(auto);
          autoButton.setText("Cancel");
        }
        else {
          stopMover();
          if (getDemoMode()) 
            autoButton.setText(" Start ");
          else
            autoButton.setText(" Find ");
        }
      }});

    modeButton.addActionListener(new ActionListener() {
      public synchronized void actionPerformed(ActionEvent e) {
        toggleDemoMode();
        updateStatus();

      }});

    undoButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        undo();
      }});
 
    levelSlider.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        setLevel(((JSlider)(e.getSource())).getValue());
      }});

    //    Dimension labDim = new Dimension(40, 16);
    Dimension labDim = new Dimension(72, 24);
    scoreLabel.setMinimumSize(labDim);
    scoreLabel.setPreferredSize(labDim);
    

    topPanel.add(autoButton);
    topPanel.add(modeButton);
    topPanel.add(undoButton);
    topPanel.add(scoreLabel);

    add(topPanel);
    

    levelSlider.setLabelTable(levelSlider.createStandardLabels(1));
    levelSlider.setPaintLabels(true);

    JPanel botPanel = new JPanel();

    botPanel.add(boardPanel);
    JPanel sliderPanel = new JPanel();
    sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.Y_AXIS));
    sliderPanel.add(levelSlider);
    sliderPanel.add(new JLabel("Level"));

    botPanel.add(sliderPanel);
    
    add(botPanel);
  }

  void initializeBoard() {
    board.reset();
    board.occupy(Player.Blue,   0,             0);
    board.occupy(Player.Blue,   Board.RANKS-1, Board.RANKS-1);
    board.occupy(Player.Green,  0,             Board.RANKS-1);
    board.occupy(Player.Green,  Board.RANKS-1, 0);
    setPlayer(Player.Blue);
    boardPanel.repaint();
  }

  public void init()  {
    initializeBoard();
    if (autostart) {
      try { Thread.sleep(1000); } catch(InterruptedException ex) { return; }
      startMover(auto);
    }
  }


  synchronized void setLevel(int l) {
    lookAheads = l;
    if (lookAheads <= 1) lookAheads = 2;
  }
    
    public int level () { return Microscope.lookAheads; }
    

  // process a move (called only from mover)

  public void move(Move m, Mover mvr) {
    if (mvr != mover || 
        m == null ||
        (mvr == user && !m.isLegal())) {
      setMover(null);
      if (mvr == auto && autostart) {
        auto.stats();        
        System.exit(0);
      }
    }
    else {
      m.commit();
      setBoard(m.board());
      setPlayer(m.player().opponent());

      history.addElement(m);

      if (mvr == auto && 
          getDemoMode() && 
          !m.isPass()) {
        if (getBoard().gameOver()) {
          if (autostart) {
            auto.stats();        
            System.exit(0);
          }
          else
            setMover(null);
        }
        else
          auto.startTurn(new Board(getBoard()), getPlayer());
      }
      else
        setMover(null);
    }
  }

  // start up a Mover
  void startMover(Mover m) {
    Mover mvr = getMover();
    if (mvr == null) {
      setMover(m);
      m.startTurn(new Board(getBoard()), player);
    }
  }

  // stop current Mover
  void stopMover() {
    Mover mvr = getMover();
    if (mvr != null) {
      setMover(null);
      mvr.cancel();
    }
  }
 

  // handle Undo button
  synchronized void undo() {
    if (mover == null) {
      if (history.size() > 1) {
        history.removeElementAt(history.size()-1);
        Move m = (Move)(history.lastElement());
        setPlayer(m.player().opponent());
        setBoard(m.board());
      }
      else if (history.size() == 1) {
        history.removeAllElements();
        initializeBoard();
      }
    }
  }

  // handle click on tile
  void userMove(int row, int col) {
    startMover(user);
    user.choose(row, col);
  }
  
  void updateStatus() { // normally called from board update
    Player p = getPlayer();
    int s = getBoard().score(p);
    scoreLabel.setForeground(displayColor(p));
    scoreLabel.setText("Score: " +  s);

    if (getDemoMode()) 
      modeButton.setText("Demo  mode");
    else {
      if (getPlayer().isBlue())
        modeButton.setText("Blue  turn");
      else
        modeButton.setText("Green turn");
    }

    if (!autostart) auto.stats();

  }


  static final int CELL_SIZE = 40; // size of a tile/cell 
  
  static final Color paleGreen = new Color(152, 251, 152);
  static final Color darkGreen = new Color(60, 179, 113);
    
  static final Color possibleMoveColor = Color.yellow;
    

  public static Color displayColor(Player pl) {
    if (pl.isBlue()) return Color.blue;
    else if (pl.isGreen()) return darkGreen;
    else return Color.white;
  }

  public static Color lightDisplayColor(Player pl) {
    if (pl.isBlue()) return Color.cyan;
    else if (pl.isGreen()) return paleGreen;
    else return Color.gray;
  }


  class BoardPanel extends Canvas implements MouseListener {
    
    BoardPanel() { 
      setSize(new Dimension(Board.RANKS * CELL_SIZE + 5, 
                            Board.RANKS * CELL_SIZE + 5));
      addMouseListener(BoardPanel.this);
    }
    
    public void paint(Graphics g) {
      
      Board b = getBoard();
      Player p = getPlayer();
      
      // the cells
      for (int row = 0; row < Board.RANKS; row++) {
        for (int col = 0; col < Board.RANKS; col++) {
          
          // Highlight selected tile and legal destinations
          if (user.placing()) {
            if (user.hasMovedFrom(row, col)) 
              g.setColor(lightDisplayColor(p));
            else if (user.canMoveTo(row, col))
              g.setColor(possibleMoveColor);
            else
              g.setColor(displayColor(b.occupant(row, col)));
          }
          
          else
            g.setColor(displayColor(b.occupant(row, col)));
          
          // tiles are just filled rectangles
          g.fillRect(row * CELL_SIZE, col * CELL_SIZE, CELL_SIZE, CELL_SIZE);
        }
      }
      
      // the grid over the cells
      g.setColor(Color.black);
      for ( int i = 0; i <= Board.RANKS; i++) {
        g.drawLine(0, i * CELL_SIZE, Board.RANKS * CELL_SIZE, i * CELL_SIZE);
        g.drawLine(i * CELL_SIZE, 0, i * CELL_SIZE, Board.RANKS * CELL_SIZE);
      }
      
      updateStatus();
    }
    
    public void mouseReleased(MouseEvent evt) {
      
      int x = evt.getX();
      int y = evt.getY();
      
      int row = x / CELL_SIZE;
      int col = y / CELL_SIZE;
      
      if (Board.inBounds(row, col)) { // cell selection
        userMove(row, col);
        repaint();
      }
    }
    
    public void mouseClicked(MouseEvent e) {}
    public void mousePressed(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
  }

  /**
   *  Player is just a glorified enumeration
   **/

  static final class Player {

    public static final int EMPTY = 0;
    public static final int BLUE = 1;
    public static final int GREEN = 2;
    public static final int ILLEGAL_PLAYER_VALUE = 3;
    
    public static final Player Empty   = new Player(EMPTY);
    public static final Player Blue    = new Player(BLUE);
    public static final Player Green   = new Player(GREEN);
    public static final Player Illegal = new Player(ILLEGAL_PLAYER_VALUE);
    
    /* private */ int code_;
    
    public Player(int code)       { code_ = code; }
    public Player(Player p)       { code_ = p.code_; }
    
    public boolean same(Player p) { return code_ == p.code_; }
    
    public boolean isEmpty()      { return code_ == EMPTY; }
    public boolean isBlue()       { return code_ == BLUE; }
    public boolean isGreen()      { return code_ == GREEN; }
    public boolean isLegal()      { return code_ <= GREEN; }
    
    public Player opponent() { 
      if (code_ == GREEN) return Blue;
      else if (code_ == BLUE) return Green;
      else return Illegal;
    }
    
  }
  
  /**
   *   Board configurations are represented by bit vectors.
   *   Since there are only 49 cells, the bits can be held in `longs',
   *   one for each player.
   * <p>
   * Boards are not immutable, but are never passed around across
   * threads (instead new ones are constructed), so don't
   * need any synch.
   **/
  
  static final class Board   {

    /* 
       First, some Constants and utilities that might as well be here
    */
    
    public static final int RANKS = 7;
    public static final int CELLS = RANKS * RANKS;
    
    static final long FULL = (1L << CELLS) - 1;
    
    // The finder uses a spare bit to remember whose move it is.
    static final long BLUEBIT = (1L << CELLS);
    
    // Bits representing the adjacent cells for every position
    static final long[] adjacentMasks = new long[CELLS];
    
    // bit pattern associated with each tile
    static final long[] cellBits = new long[CELLS];

    // locations of all cells reachable by a jump for every position
    static final byte[][] jumpDestinations = new byte[CELLS][];

    // initialize tables
    static {
      byte[] dests = new byte[CELLS];
      for (int j = 0; j < RANKS; ++j) {
        for (int i = 0; i < RANKS; ++i) {
          int k = i + j * RANKS;
          long nmask = 0;
          int jumpCount = 0;
          for (int c = j-2; c <= j+2; ++c) {
            for (int r = i-2; r <= i+2; ++r) {
              if (c >= 0 && c < RANKS &&
                  r >= 0 && r < RANKS) {
                int cellIndex = r + c * RANKS;
                if (r == i-2 || r == i+2 || c == j-2 || c == j+2) {
                  dests[jumpCount++] = (byte)cellIndex;
                }
                else if (!(r == i && c == j)) {
                  nmask |= 1L << cellIndex;
                }
              }
            }
          }
          adjacentMasks[k] = nmask;
          cellBits[k] = 1L << k;
          jumpDestinations[k] = new byte[jumpCount];
          for (int l = 0; l < jumpCount; ++l)
            jumpDestinations[k][l] = dests[l];

        }
      }

    }
    
    
    public static boolean inBounds(int row, int col) {
      return (0 <= row)  && (row < RANKS) && (0 <= col) && (col < RANKS);
    }
    
    // The representation
    
    long blue_;      // bit vector; true if occupied by blue
    long green_;     // same for green;
    
    // constructors and intializers:
    
    public Board()               { blue_ = 0L; green_ = 0L; }
    public Board(Board b)        { blue_ = b.blue_; green_ = b.green_; }
    public Board(long b, long g) { blue_ = b; green_ = g; }
    
    public void copyState(Board b) {
      blue_ = b.blue_; 
      green_ = b.green_; 
    }

    void reset() { 
      blue_ = 0L; green_ = 0L; 
    }
    
    long getBlue() { return blue_; }
    long getGreen() { return green_; }


    public Player occupant(int row, int col) {
      if ((0 <= row)  && (row < RANKS) && (0 <= col) && (col < RANKS)) {
        long m = 1L << (row + col * RANKS);
        if ((blue_ & m) != 0L) return Player.Blue;
        else if ((green_ &m) != 0L) return Player.Green;
        else return Player.Empty;
      }
      else
        return Player.Illegal;
    }
    
    
    // place a tile without taking opponent tiles
    
    public void occupy(Player player, int row, int col) {
      long m = 1L << (row + col * RANKS);
      long nm = ~m;
      if (player.code_ == Player.BLUE)  { 
        blue_ |= m;
        green_ &= nm;
      }
      else if (player.code_ == Player.GREEN) { 
        blue_ &=  nm;
        green_ |= m;
      }
      else { 
        blue_ &= nm;
        green_ &= nm;
      }
    }
    
    public void unoccupy(int row, int col) {
      long nm = ~(1L << (row + col * RANKS));
      blue_ &=  nm;
      green_ &= nm;
    }
    
    
    
    // place a tile, taking all adjacent tiles of opponent
    
    public void take(Player player, int row, int col) {
      int k =  (row + col * RANKS);
      long dest = 1L << k;
      long nbrMask = adjacentMasks[k];
      long sourceBlue = blue_;
      long sourceGreen = green_;
      if (player.code_ == Player.BLUE) {
        blue_ = sourceBlue | dest | (sourceGreen & nbrMask);
        green_ = sourceGreen & ~(sourceGreen & nbrMask);
      }
      else {
        blue_ = sourceBlue & ~(sourceBlue & nbrMask);
        green_ =  sourceGreen | dest | (sourceBlue & nbrMask);
      }
    }
    
    public boolean gameOver() {
      return 
        (((blue_ | green_) & FULL) == FULL) ||
        ((blue_ & ~BLUEBIT) == 0) ||
        ((green_ & ~BLUEBIT) == 0);
    }
    
    
    public int score(Player player) {
      if (player.isBlue()) {
        return score(blue_, green_);
      }
      else {
        return score(green_, blue_);
      }
    }
    
    static int score(long b, long g) {
      
      // much faster by splitting into ints
      // and using clever shift-based bit counter
      
      int lb = (int)(b & ((1L << 32) - 1));
      int hb = ((int)(b >>> 32)) & ((1 << (CELLS - 32)) - 1);

      lb -= (0xaaaaaaaa & lb) >>> 1;
      lb = (lb & 0x33333333) + ((lb >>> 2) & 0x33333333);
      lb = lb + (lb >>> 4) & 0x0f0f0f0f;
      lb += lb >>> 8;
      lb += lb >>> 16;

      hb -= (0xaaaaaaaa & hb) >>> 1;
      hb = (hb & 0x33333333) + ((hb >>> 2) & 0x33333333);
      hb = hb + (hb >>> 4) & 0x0f0f0f0f;
      hb += hb >>> 8;
      hb += hb >>> 16;

      hb = ((lb + hb) & 0xff);

      int lg = (int)(g & ((1L << 32) - 1));
      int hg = ((int)(g >>> 32)) & ((1 << (CELLS - 32)) - 1);

      lg -= (0xaaaaaaaa & lg) >>> 1;
      lg = (lg & 0x33333333) + ((lg >>> 2) & 0x33333333);
      lg = lg + (lg >>> 4) & 0x0f0f0f0f;
      lg += lg >>> 8;
      lg += lg >>> 16;

      hg -= (0xaaaaaaaa & hg) >>> 1;
      hg = (hg & 0x33333333) + ((hg >>> 2) & 0x33333333);
      hg = hg + (hg >>> 4) & 0x0f0f0f0f;
      hg += hg >>> 8;
      hg += hg >>> 16;

      return hb - ((lg + hg) & 0xff);
    }
    
    
    
    static int slowscore(long b, long g) {
      int score = 0;
      for (int l = 0; l < CELLS; ++l) {
        score += (int)(b & 1);
        b >>>= 1;
        score -= (int)(g & 1);
        g >>>= 1;
      }
      return score;
    }
   
    
  }
  
  /**
   * Moves represent transitions across Board states
   **/


  static final class Move  {

    static final int NO_VALUE = -1;     // row/col value if not yet set
    static final int PASS_VALUE = -2;   // special value for pass moves
    
    // utilities for classifying moves
    
    public static boolean twoFrom(int a, int b) { 
      return (a - b == 2) || (b - a == 2); 
    }
    
    public static boolean withinTwo(int a, int b) { 
      int diff = a - b; return -2 <= diff && diff <= 2;
    }
    
    // representations
    
    int fromRow;
    int fromCol;
    
    int toRow;
    int toCol;
    
    Player player_;
    Board board_;
    
    boolean committed = false; // true if board reflects move
    
    // constructors and intializers
    
    public Move(Player turn, Board board) { 
      fromRow = NO_VALUE; fromCol = NO_VALUE;
      toRow = NO_VALUE;   toCol = NO_VALUE;
      player_ = turn;
      board_ = board;
    }
    
    public Move(Player turn, Board board, boolean isCommitted) { 
      fromRow = NO_VALUE; fromCol = NO_VALUE;
      toRow = NO_VALUE;   toCol = NO_VALUE;
      player_ = turn;
      board_ = board;
      committed = isCommitted;
    }
    
    synchronized void reset() {
      fromRow = NO_VALUE;
      fromCol = NO_VALUE;
      toRow = NO_VALUE;
      toCol = NO_VALUE;
    }
    
    // setters:
    
    synchronized void player(Player p)       { player_ = p;  }
    synchronized void board(Board b)         { board_ = b;  }
    synchronized void from(int sr, int sc)   { fromRow = sr; fromCol = sc;  }
    synchronized void to(int dr, int dc)     { toRow = dr;   toCol = dc; }
   
    //  accessors:
    
    synchronized boolean isFrom(int r, int c) { 
      return fromRow== r && fromCol == c; 
    }
    synchronized boolean isTo(int r, int c)   { 
      return toRow == r && toCol == c; 
    }
    synchronized Board board() { 
      return board_; 
    }
    synchronized Player player() { 
      return player_; 
    }
    

    // status checks:
    
    synchronized boolean isPass() { // is this a `pass' move?
      return (toRow == PASS_VALUE || fromRow == PASS_VALUE);
    }
    
    synchronized boolean isJump() {
      return 
        (fromRow - toRow == 2) || (toRow - fromRow == 2) ||
        (fromCol - toCol == 2) || (toCol - fromCol == 2);
    }
    
    synchronized boolean hasFrom() { // is from set?
      return fromRow != NO_VALUE && fromCol != NO_VALUE;
    }
    
    synchronized boolean hasTo() { // is to set?
      return toRow != NO_VALUE && toCol != NO_VALUE;
    }
    
    
    synchronized boolean possibleTo(int r, int c) { // is (r, c) a legal `to'?
      return hasFrom() &&
        withinTwo(fromRow, r) &&
        withinTwo(fromCol, c) &&
        board_.occupant(r, c).isEmpty();
    }
    
    synchronized boolean isLegal() {
      if (isPass()) 
        return true;
      else if (!board_.occupant(toRow, toCol).isEmpty()) 
        return false;
      else if (!board_.occupant(fromRow, fromCol).same(player_)) 
        return false;
      else if (!(withinTwo(fromRow, toRow) && withinTwo(fromCol, toCol))) 
        return false;
      else
        return true;
    }
    
    synchronized void commit() { // update board to reflect move
      if (!committed) {
        committed = true;
        if (isLegal() && !isPass())  {
          if (isJump()) board_.occupy(Player.Empty, fromRow, fromCol);
          board_.take(player_, toRow, toCol);
        }
      }
    }
    
  }
  
  /**
   *  Mover is an abstract class to simplify code dealing with
   *  either user moves or auto moves.
   **/
  

  static abstract class Mover {
    
    // caller for move callbacks
    protected Microscope game;
    
    protected Mover(Microscope ap) { game = ap; }
    
    // start a turn as player on given board
    public abstract void startTurn(Board b, Player p);
    
    // cancel current partial move
    public abstract void cancel();
    
    // return true if move not yet ready
    public abstract boolean placing();
    
  }
  
  /**
   *  User builds moves via instructions/clicks by users
   **/

  static class User extends Mover {

    private Move current;
    
    public User(Microscope ap) { super(ap); current = null; }
    
    public synchronized void startTurn(Board b, Player p) {
      current = new Move(p, b);
    }
    
    public boolean placing() { 
      return current != null && current.hasFrom() && !current.hasTo(); 
    }
    
    public synchronized void cancel() { 
      if (current != null) {
        current.reset(); 
        current = null; 
      }
    }
    
    public synchronized void choose(int row, int col) {
      if (current != null) {
        if (row == Move.PASS_VALUE) {
          current.from(row, col);
          game.move(current, this);
          current = null;
        }
        else if (!current.hasFrom()) {
          if (current.board().occupant(row, col).same(current.player())) {
            current.from(row, col);
          }
        }
        else {
          current.to(row, col);
          game.move(current, this);
          current = null;
        }
      }
    }
    
    public synchronized boolean canMoveTo(int row, int col) {
      return placing() && current.possibleTo(row, col);
    }
    
    public synchronized boolean hasMovedFrom(int row, int col) {
      return current != null && current.isFrom(row, col);
    }
    
  }


  /**
   *     AutoMover constructs Finders that compute actual moves
   **/

  static class AutoMover extends Mover {

    FJTaskRunnerGroup group = null;
    boolean cancelled = false;
    RootFinder currentFinder = null;

    public AutoMover(Microscope ap) {
      super(ap);
    }
  
    
    public synchronized boolean placing() { 
      return currentFinder != null; 
    }

    synchronized void stopPlacing() { 
      currentFinder = null;
    }
    
    
    public synchronized void cancel() {
      if (placing())  { 
        currentFinder.cancel();
        stopPlacing();
      }
    }
    

    public synchronized void startTurn(Board board, Player player) {
      try {
        if (group == null) {
          group = new FJTaskRunnerGroup(Microscope.nprocs);
        }
        if (!placing()) {
          currentFinder = new RootFinder(board, player, 
                                         Microscope.lookAheads, this);
          group.execute(currentFinder);
        }
      }
      catch (InterruptedException ex) {
        stopPlacing();
      }
    }
    
    public void stats() {
      if (group != null) group.stats();
    }
   

    synchronized void relay(Move move) { // relay callback from finder
      if (placing()) {
        stopPlacing();
        game.move(move, this);
      }
    }
    
  }
  

  /**
   * Implements a classic all-possible-move search algorith using FJTasks.
   * The move finder is not all that smart. Among other possible
   * improvements, it could keep a cache of explored moves and
   * avoid repeating them. This would likely speed it up since
   * most expansions are duplicates of others. It could also be changed to
   * prune moves, although this is unlikely to work well without
   * better partial evaluation functions.
   **/
  
  static class Finder extends FJTask {

    static final int NOMOVE = Integer.MIN_VALUE;
    static final int LOSE   = NOMOVE+1;
    static final int WIN    = -LOSE;
    
    final long ours;     // bits for our tiles
    final long theirs;   // bits for opponent tiles
    final int level;     // current number of lookAheads
    final Finder next;   // Each Finder is placed in a linked list by parent

    // Assigned once; must be volatile since accessed by parents
    volatile int bestScore;

    Finder(long ours, long theirs, int level, Finder next) {
      this.ours = ours;
      this.theirs = theirs;
      this.level = level;
      this.next = next;
    }
    
    public final void run() {

      // Handle sure wins and losses here
      if ((ours & ~Board.BLUEBIT) == 0)  
        bestScore = LOSE;

      else if ((theirs & ~Board.BLUEBIT) == 0) 
        bestScore = WIN;

      else if (((ours | theirs) & Board.FULL) == Board.FULL) {
        int score = Board.score(ours, theirs);
        if (score > 0) bestScore = WIN;
        else if (score < 0) bestScore = LOSE;
        else bestScore = 0;
      }

      else 
        search();
    }
    

    final void search() {
      int best = NOMOVE;    // For direct evaluation when level == 1
      Finder forked = null; // list of forked subtasks when level > 1
      
      long open = ~(ours | theirs);  // currently empty cells
      long here = 1;                 // travserse through bits
      
      for (int k = 0; k < Board.CELLS; ++k, here <<= 1) {
        
        if ((here & ours) != 0) {
          /*
           * Step through possible destinations to find jumps for this tile
           */
          
          byte[] dests = Board.jumpDestinations[k];
          for (int j = 0; j < dests.length; ++j) {
            byte d = dests[j];
            long dest = 1L << d;

            if ( (dest & open) != 0) {
              long adjacent = Board.adjacentMasks[d];

              long nTheirs = theirs & ~adjacent;
              long nOurs = (ours & ~here) | dest | (theirs & adjacent);

              if (level > 1) 
                (forked = new Finder(nTheirs, nOurs, level-1, forked)).fork();

              else {
                int sc = Board.score(nOurs, nTheirs);
                if (sc > best) best = sc;
              }
            }
          }
        }

        else if ((here & open) != 0) {
          
          /*
           * If this cell is open, and is within 1 of one of our tiles,
           * it can be taken in some copy move.  It doesn't matter which
           * of the adjacent cells is considered to be source of copy
           * move 
           */
          
          long adjacent = Board.adjacentMasks[k];
          
          if ((ours & adjacent) != 0) {

            long nTheirs = theirs & ~adjacent;
            long nOurs = ours | here | (theirs & adjacent);

            if (level > 1) 
              (forked = new Finder(nTheirs, nOurs, level-1, forked)).fork();

            else {
              int sc = Board.score(nOurs, nTheirs);
              if (sc > best) best = sc;
            }
          }
        }
      }

      if (level > 1)
        collect(forked);
      else
        bestScore = best;
    }

    /**
     * Join all subtasks and evaluate moves. Default is sub-finder version.
     * Overridden in RootFinder
     **/

    void collect(Finder forked) {

      int best = NOMOVE;

      while (forked != null) {

        while (!forked.isDone()) { // interleave joins with cancel checks
          if (isDone()) {
            cancelAll(forked);
            return;
          }
          else 
            yield();
        }

        int score = -forked.bestScore; // negate opponent score
        
        if (score > best) {
          best = score;
          if (score >= WIN) {
            cancelAll(forked.next);
            break;
          }
        }
        forked = forked.next;
      }

      bestScore = best;
    }

    /**
     * Cancel all forked subtasks in list
     **/

    void cancelAll(Finder forked) {
      while (forked != null) {
        forked.cancel();
        forked = forked.next;
      }
    }

  }

  /**
   * Root Finder class -- wait out other finders and issue callback to game.
   **/

  static class RootFinder extends Finder {
    final AutoMover automover; 
    final Player player;

    RootFinder(Board board, Player p, int level, AutoMover automover) {
      super( (p.isBlue()? (board.getBlue()| Board.BLUEBIT) : board.getGreen()),
             (p.isBlue()? board.getGreen() : (board.getBlue()| Board.BLUEBIT)),
             level,
             null);

      this.player = p;
      this.automover = automover;
    }


    /**
     * This differs from default version by recording
     * and calling back with best move
     **/

    void collect(Finder forked) {

      int best = NOMOVE;
      Finder bestFinder = null;

      while (forked != null) {

        while (!forked.isDone()) {
          if (isDone()) {
            cancelAll(forked);
            return;
          }
          else 
            yield();
        }

          
        int score = -forked.bestScore; // negate opponent score
        
        if (bestFinder == null || score > best) {
          best = score;
          bestFinder = forked;
          if (score >= WIN) {
            cancelAll(forked.next);
            break;
          }
        }
        
        // Just for fun, introduce a little randomness via hashcodes
        
        else if (score == best &&
                 !Microscope.DETERMINISTIC &&
                 (System.identityHashCode(forked) > 
                  System.identityHashCode(bestFinder))) {
          bestFinder = forked;
        }
        
        forked = forked.next;
        
      }

      
      Move move = null;

      if (bestFinder != null) {
        /* 
           Even though accessed here,
           the ours and theirs vars of Finders do not
           need to be volatile because they are immutably
           established in constructors.
        */
        
        long nextOurs = bestFinder.theirs;
        long nextTheirs = bestFinder.ours;
        long blue = (player.isBlue())? nextOurs : nextTheirs;
        long green = (player.isBlue())? nextTheirs: nextOurs;
        move = new Move(player, new Board(blue, green), true);
      }
      
      automover.relay(move);
    }
  }

  
}
