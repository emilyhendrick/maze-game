import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;

import javalib.impworld.World;
import javalib.impworld.WorldScene;
import javalib.worldimages.LineImage;
import javalib.worldimages.OutlineMode;
import javalib.worldimages.Posn;
import javalib.worldimages.RectangleImage;
import javalib.worldimages.TextImage;
import javalib.worldimages.WorldImage;
import tester.Tester;


class Cell {
  int cellSize; // width of the cell
  int x; // x value of the top left corner
  int y; // y value of the top left corner
  Color color; // fill color
  Cell cleft; // adjacent left cell
  Cell ctop; // adjacent top cell
  Cell cright; //  adjacent right cell
  Cell cbottom; // adjacent bottom cell
  Edge eleft; // left edge
  Edge etop; // top edge
  Edge eright; // right edge
  Edge ebottom; // bottom edge
  Posn posn; // center point

  Cell(int x, int y, int cellSize) {
    this.x = x;
    this.y = y;
    this.cellSize = cellSize;
    this.color = Color.white;
    this.posn = new Posn(this.x, this.y);
    this.cleft = null;
    this.cright = null;
    this.ctop = null;
    this.cbottom = null;
    this.eleft = null;
    this.eright = null;
    this.etop = null;
    this.ebottom = null;
  }

  // convenience constructor
  Cell(int x, int y, int cellSize, Color color) {
    this(x, y, cellSize);
    this.color = color;
  }

  // draws a single cell
  WorldImage drawCell() {
    return new RectangleImage(this.cellSize, this.cellSize, OutlineMode.SOLID, this.color);
  }

  // sets the color of the cell
  void changeColor(Color c) {
    this.color = c;
  }

  // fills top/bottom/left/right cell if
  Cell fillNextCell(String s) {
    if (s.equals("up") && this.etop == null && this.ctop != null) {
      this.ctop.color = Color.red;
      return this.ctop;
    }
    if (s.equals("down") && this.ebottom == null && this.cbottom != null) {
      this.cbottom.color = Color.red;
      return this.cbottom;
    }
    if (s.equals("left") && this.eleft == null && this.cleft != null) {
      this.cleft.color = Color.red;
      return this.cleft;
    }
    if (s.equals("right") && this.eright == null && this.cright != null) {
      this.cright.color = Color.red;
      return this.cright;
    }
    else {
      return this;
    }
  }
}

class Edge {

  Posn vertex1; // end point 1
  Posn vertex2; // end point 2
  int weight; // line weight


  Edge(Posn v1, Posn v2, int w) {
    this.vertex1 = v1;
    this.vertex2 = v2;
    this.weight = w;
  }

  // finds the center point of the edge
  Posn centerPoint() {
    return new Posn((this.vertex1.x + this.vertex2.x) / 2, (this.vertex1.y + this.vertex2.y) / 2);
  }

  boolean sameLocation(Posn one, Posn two) {
    return (this.vertex1.equals(one) && this.vertex2.equals(two)) || (this.vertex1.equals(two) 
        && this.vertex2.equals(one));
  }

}

class Maze extends World {

  int cellSize; // cell width
  int rows; // num rows on board
  int columns; // num columns on board
  int width; // of game board
  int height; // of game board
  Random r; 
  ArrayList<Posn> vertices; // all vertices on board
  ArrayList<Edge> allEdges; // all possible edges on board
  ArrayList<Edge> edgesInTree; // all edges that form the maze
  HashMap<Posn, Posn> representatives; // each vertex is a key, and its value is its representative
  ArrayList<Cell> allCells; // all cells on the board
  ArrayList<Cell> allVisitedCells; // all visited cells in search
  ArrayList<Cell> currentPath; // current manual search path
  boolean gameWon = false;
  Cell currentCell; // current cell in a manual maze


  Maze(int rows, int columns, Random r) {
    reset(rows, columns, r);
  }

  // reset the board
  void reset(int rows, int columns, Random r) {
    this.rows = rows;
    this.columns = columns;
    this.r = r;

    // prevents big boards from going off screen and tiny boards from being
    // impossible to read
    if (this.rows > 70 || this.columns > 40) {
      this.cellSize = 12;
    }
    else {
      this.cellSize = 20;
    }

    this.width = (this.rows * this.cellSize) + (2 * this.cellSize);
    this.height = (this.columns * this.cellSize) + (2 * this.cellSize);

    this.vertices = new ArrayList<Posn>();
    initVertices();

    this.representatives = new HashMap<Posn, Posn>(this.vertices.size());
    initRepresentatives();

    this.allEdges = new ArrayList<Edge>();
    initEdges();

    this.edgesInTree = calculateEdges();
    this.allCells = new ArrayList<Cell>();
    this.allVisitedCells = new ArrayList<Cell>();
    this.currentPath = new ArrayList<Cell>();
    initCells();
    this.currentCell = allCells.get(0);
  }

  // Handles user key inputs
  // "r" -> resets and creates a random maze
  // "b" -> performs and displays breadth-first search
  // "d" -> performs and displays depth-first search
  // "m" -> user will traverse maze manually
  // if manual:
  // "up" -> moves user up
  // "down" -> moves user down
  // "left" -> moves user left
  // "right" -> moves user right
  public void onKeyEvent(String key) {
    if (key.equals("r")) {
      // reset board
      reset(this.rows, this.columns, new Random());
    }
    if (key.equals("b")) {
      this.bfs();
    }
    if (key.equals("d")) {
      this.dfs();
    }
    if (key.equals("up") || key.equals("down") || key.equals("left") || key.equals("right")) {
      Cell oldCell = currentCell;
      currentCell = currentCell.fillNextCell(key);
      if(currentCell.equals(this.allCells.get(this.allCells.size() - 1))) {
        lastScene("");
      }
      if (currentCell != oldCell) {
        oldCell.color = Color.yellow;
      }
    }
  }

  // EFFECT: creates all the vertices on the board
  // starts in the top left and adds them from top to bottom, then left to right
  void initVertices() {
    for (int x = 0; x < this.rows + 1; x++) {
      for (int y = 0; y < this.columns + 1; y++) {
        this.vertices.add(new Posn(x * this.cellSize, y * this.cellSize));
      }
    }
  }

  // EFFECT: assigns each vertex it's representative, which is initially itself
  void initRepresentatives() {
    for (Posn p : this.vertices) {
      this.representatives.put(p, p);
    }
  }

  // EFFECT: create all the edges in the board
  void initEdges() {
    for (Posn p : this.vertices) {

      // weights prioritize edges of the board (1), then randomly select edges in the
      // middle (2 - 4)

      // right border
      if (p.x == this.rows * this.cellSize && p.y < this.columns * this.cellSize) {
        this.allEdges.add(new Edge(p, this.vertices.get(this.vertices.indexOf(p) + 1), 1));
      }
      // top border
      else if (p.x < this.rows * this.cellSize && p.y == 0) {
        this.allEdges
        .add(new Edge(p, this.vertices.get(this.vertices.indexOf(p) + this.columns + 1), 1));
        this.allEdges
        .add(new Edge(p, this.vertices.get(this.vertices.indexOf(p) + 1), r.nextInt(5) + 2));
      }
      // left border
      else if (p.x == 0 && p.y < this.columns * this.cellSize) {
        this.allEdges.add(new Edge(p,
            this.vertices.get(this.vertices.indexOf(p) + this.columns + 1), r.nextInt(5) + 2));
        this.allEdges.add(new Edge(p, this.vertices.get(this.vertices.indexOf(p) + 1), 1));
      }
      // bottom border
      else if (p.x < this.rows * this.cellSize && p.y == this.columns * this.cellSize) {
        this.allEdges
        .add(new Edge(p, this.vertices.get(this.vertices.indexOf(p) + this.columns + 1), 1));
      }
      // middle spaces
      else if (p.x < this.rows * this.cellSize && p.y < this.columns * this.cellSize) {
        this.allEdges.add(new Edge(p,
            this.vertices.get(this.vertices.indexOf(p) + this.columns + 1), r.nextInt(5) + 2)); 
        this.allEdges
        .add(new Edge(p, this.vertices.get(this.vertices.indexOf(p) + 1), r.nextInt(5) + 2)); 
      }
    }
    ArrayUtils<Edge> u = new ArrayUtils<Edge>();
    u.quickSort(this.allEdges, new EdgeComparator());
  }

  // EFFECT: adds cells to the board
  void initCells() {
    // add all cells
    for (int i = 0; i < this.rows; i++) {
      for (int j = 0; j < this.columns; j++) {
        if (i == 0 && j == 0) {
          // starting tile
          this.allCells.add(new Cell(i, j, this.cellSize, Color.green));
        }
        else if (i == this.rows - 1 && j == this.columns - 1) {
          // finish tile
          this.allCells.add(new Cell(i, j, this.cellSize, Color.magenta));
        }
        else {
          this.allCells.add(new Cell(i, j, this.cellSize));
        }
      }
    }

    // link cells
    for (int i = 0; i < this.allCells.size(); i++) {
      Cell current = this.allCells.get(i);
      // only modify left of not left-most pieces
      if (current.x != 0) {
        current.cleft = this.allCells.get(i - this.columns);
      }
      // only modify right of not right-most pieces
      if (current.x != this.rows - 1) {
        current.cright = this.allCells.get(i + this.columns);
      }
      // only modify top of not top-most pieces
      if (current.y != 0) {
        current.ctop = this.allCells.get(i - 1);
      }
      // only modify bottom of not bottom-most pieces
      if (current.y != this.columns - 1) {
        current.cbottom = this.allCells.get(i + 1);
      }
    }
    initCellEdges();
  }

  // EFFECT: connects each cell with each edge
  // this information will be used in part two to find valid paths
  void initCellEdges() {
    for (Cell c : this.allCells)
      for (Edge e : this.edgesInTree) {
        int x = c.x * this.cellSize;
        int y = c.y * this.cellSize;
        int x1 = (c.x + 1) * this.cellSize;
        int y1 = (c.y + 1) * this.cellSize;
        if (e.sameLocation(new Posn(x, y), new Posn(x1, y))) { 
          c.etop = e;
        }
        if (e.sameLocation(new Posn(x, y), new Posn(x, y1))) { 
          c.eleft = e;
        }
        // the right edge
        if (e.sameLocation(new Posn(x1, y), new Posn(x1, y1))) { 
          c.eright = e;
        }
        // the left edge
        if (e.sameLocation(new Posn(x, y1), new Posn(x1, y1))) {
          c.ebottom = e;
        }
      }
  }

  // generates the walls of the maze using Kruskals Algorithm
  ArrayList<Edge> calculateEdges() {
    int numEdges = 0;
    int i = 0;
    ArrayList<Edge> countedEdges = new ArrayList<Edge>();
    while (numEdges < this.vertices.size() - 1) { // min. edges = vertices - 1
      Edge curr = this.allEdges.get(i);
      i++;
      if (find(curr.vertex1).equals(find(curr.vertex2))) {
        // if two elements have the same representative, they are already connected so
        // do nothing
      }
      else if (!countedEdges.contains(curr)) {
        countedEdges.add(curr);
        union(find(curr.vertex1), find(curr.vertex2));
        numEdges++;
      }
    }
    countedEdges.add(new Edge(new Posn(0, 0), new Posn(0, this.cellSize), 1));
    return countedEdges;
  }

  // finds the representative corresponding to the vertex
  Posn find(Posn vertex) {
    return this.representatives.get(vertex);
  }

  // EFFECT: updates an edges representative to the given representative
  void union(Posn value1, Posn value2) {
    if (this.representatives.get(value1).equals(value1)
        && this.representatives.get(value2).equals(value2)) { // if both keys point to themselves
      this.representatives.replace(value1, value2);
      for (Posn key : this.representatives.keySet()) {
        if (this.representatives.get(key).equals(value1)) {
          this.representatives.replace(key, value2);
        }
      }
    }
    else {
      union(find(value1), find(value2));
    }
  }

  // draws the board
  public WorldScene makeScene() {
    WorldScene ws = new WorldScene(this.width, this.height);
    for (Cell c : this.allCells) {
      ws.placeImageXY(c.drawCell(), c.x * this.cellSize + this.cellSize +
          this.cellSize / 2,
          c.y * this.cellSize + this.cellSize + this.cellSize / 2);
    }
    for (Edge e : this.edgesInTree) {
      Posn p = e.centerPoint();
      // draw horizontal line
      if (e.vertex1.y == e.vertex2.y) {
        ws.placeImageXY(new LineImage(new Posn(this.cellSize, 0), Color.black), p.x + this.cellSize,
            p.y + this.cellSize);
      }
      // draw vertical line
      else {
        ws.placeImageXY(new LineImage(new Posn(0, this.cellSize), Color.black), p.x + this.cellSize,
            p.y + this.cellSize);
      }
    }

    if(this.currentCell.equals(this.allCells.get(this.allCells.size() - 1))) {
      ws.placeImageXY(new TextImage("Game Over!", Color.black), 
          this.width / 2, this.height / 2);
      return ws;
    }
    else {
      return ws;
    }
  }

  // finds a solution to the maze using breadth first search
  // draws the solution
  ArrayList<Cell> bfs() {
    ArrayList<Cell> bfs = searchHelp(new Queue<Cell>());
    WorldScene ws = makeScene();
    for (Cell c : bfs) {
      c.changeColor(Color.yellow);
      ws.placeImageXY(c.drawCell(), c.x * this.cellSize + this.cellSize +
          this.cellSize / 2,
          c.y * this.cellSize + this.cellSize + this.cellSize / 2);
    }
    return bfs;
  }

  // finds a solution to the maze using depth first search
  // draws the solution
  ArrayList<Cell> dfs() {
    ArrayList<Cell> dfs = searchHelp(new Stack<Cell>());
    WorldScene ws = makeScene();
    for (Cell c : dfs) {
      c.changeColor(Color.yellow);
      ws.placeImageXY(c.drawCell(), c.x * this.cellSize + this.cellSize +
          this.cellSize / 2,
          c.y * this.cellSize + this.cellSize + this.cellSize / 2);
    }
    return dfs;
  }

  // executes the search for dfs and bfs
  ArrayList<Cell> searchHelp(ICollection<Cell> worklist) {
    ArrayDeque<Cell> alreadySeen = new ArrayDeque<Cell>();
    HashMap<Cell, Cell> cameFromCell = new HashMap<Cell, Cell>();
    Cell from = this.allCells.get(0);
    Cell to = this.allCells.get(this.allCells.size() - 1);

    // Initialize the worklist with the from vertex
    worklist.add(from);
    // As long as the worklist isn't empty...
    while (!worklist.isEmpty()) {
      Cell next = worklist.remove();
      if (next.equals(to)) {
        return reconstruct(cameFromCell, next, new ArrayList<Cell>()); // Success!
      }
      else if (alreadySeen.contains(next)) {
        // do nothing: we've already seen this one
      }
      else {
        if (next.ctop != null && next.etop == null) {
          worklist.add(next.ctop);
          cameFromCell.put(next, next.ctop);
        }
        if (next.cleft != null && next.eleft == null) {
          worklist.add(next.cleft);
          cameFromCell.put(next, next.cleft);
        }
        if (next.cright != null && next.eright == null) {
          worklist.add(next.cright);
          cameFromCell.put(next, next.cright);
        }
        if (next.cbottom != null && next.ebottom == null) {
          worklist.add(next.cbottom);
          cameFromCell.put(next, next.cbottom);
        }
        // add next to alreadySeen, since we're done with it
        alreadySeen.addFirst(next);
      }
    }
    // We haven't found the to vertex, and there are no more to try
    return null;
  }

  // returns the correct path from the search
  ArrayList<Cell> reconstruct(HashMap<Cell, Cell> cameFromCell, Cell end, ArrayList<Cell> path) {
    for (Cell c : cameFromCell.keySet()) {
      if (cameFromCell.get(c).equals(end)) {
        path.add(c);
        reconstruct(cameFromCell, c, path);
      }
    }
    return path;
  }

}

//Represents a mutable collection of items
interface ICollection<T> {
  // Is this collection empty?
  boolean isEmpty();

  // EFFECT: adds the item to the collection
  void add(T item);

  // Returns the first item of the collection
  // EFFECT: removes that first item
  T remove();
}

class Stack<T> implements ICollection<T> {
  ArrayDeque<T> contents;

  Stack() {
    this.contents = new ArrayDeque<T>();
  }

  // checks if the stack is empty
  public boolean isEmpty() {
    return this.contents.isEmpty();
  }

  // removes and returns the top item in the stack
  public T remove() {
    return this.contents.removeFirst();
  }

  // adds an item to the top of the stack
  public void add(T item) {
    this.contents.addFirst(item);
  }
}

class Queue<T> implements ICollection<T> {
  ArrayDeque<T> contents;

  Queue() {
    this.contents = new ArrayDeque<T>();
  }

  // checks if the queue is empty
  public boolean isEmpty() {
    return this.contents.isEmpty();
  }

  // removes and returns the first item in the queue
  public T remove() {
    return this.contents.removeFirst();
  }

  // adds an item to the end of the queue
  public void add(T item) {
    this.contents.addLast(item);
  }
}

class EdgeComparator implements Comparator<Edge> {

  @Override
  // compares the weights of two edges
  public int compare(Edge e1, Edge e2) {
    if (e1.weight > e2.weight) {
      return 1;
    }
    else if (e1.weight == e2.weight) {
      return 0;
    }
    else {
      return -1;
    }
  }

}

class ArrayUtils<T> {

  // EFFECT: Sorts the given ArrayList according to the given comparator
  public void quickSort(ArrayList<T> allEdges, Comparator<T> comp) {
    quicksortHelp(allEdges, comp, 0, allEdges.size());

  }

  // EFFECT: sorts the source array according to comp, in the range of indices
  // [loIdx, hiIdx)
  void quicksortHelp(ArrayList<T> source, Comparator<T> comp, int loIdx, int hiIdx) {
    if (loIdx >= hiIdx) {
      return;
    }
    T pivot = source.get(loIdx);
    int pivotIdx = partition(source, comp, loIdx, hiIdx, pivot);
    quicksortHelp(source, comp, loIdx, pivotIdx);
    quicksortHelp(source, comp, pivotIdx + 1, hiIdx);
  }

  // Returns the index where the pivot element ultimately ends up in the sorted
  // source
  // EFFECT: Modifies the source list in the range [loIdx, hiIdx) such that
  // all values to the left of the pivot are less than (or equal to) the pivot
  // and all values to the right of the pivot are greater than it
  int partition(ArrayList<T> source, Comparator<T> comp, int loIdx, int hiIdx, T pivot) {
    int curLo = loIdx;
    int curHi = hiIdx - 1;
    while (curLo < curHi) {
      while (curLo < hiIdx && comp.compare(source.get(curLo), pivot) <= 0) {
        curLo = curLo + 1;
      }
      while (curHi >= loIdx && comp.compare(source.get(curHi), pivot) > 0) {
        curHi = curHi - 1;
      }
      if (curLo < curHi) {
        swap(source, curLo, curHi);
      }
    }
    swap(source, loIdx, curHi);
    return curHi;
  }

  // EFFECT: swaps the values at the two given indexes
  void swap(ArrayList<T> list, int i, int j) {
    T temp = list.get(i);
    list.set(i, list.get(j));
    list.set(j, temp);
  }
}

// Test class
class ExampleMaze {


  Maze maze1;
  Maze maze2;
  Maze maze3;
  Edge edge1;
  Edge edge2;
  Edge edge3;
  ArrayList<Integer> list1;
  ArrayList<Integer> list1a;
  ArrayList<String> list2;
  ArrayList<String> list2a;
  Cell cell1;
  Cell cell2;
  Cell cell3;

  void init() {
    this.maze1 = new Maze(3, 3, new Random(5));
    this.maze2 = new Maze(2, 2, new Random(5));
    this.maze3 = new Maze(1, 2, new Random(5));
    this.edge1 = new Edge(new Posn(0, 0), new Posn(10, 10), 1);
    this.edge2 = new Edge(new Posn(10, 10), new Posn(20, 20), 2);
    this.edge3 = new Edge(new Posn(4, 4), new Posn(8, 12), 2);
    this.list1 = new ArrayList<Integer>(Arrays.asList(5, 2, 1, 3, 4));
    this.list1a = new ArrayList<Integer>(Arrays.asList(1, 2, 3, 4, 5));
    this.list2 = new ArrayList<String>(Arrays.asList("b", "d", "e", "a", "c"));
    this.list2a = new ArrayList<String>(Arrays.asList("a", "b", "c", "d", "e"));
    this.cell1 = new Cell(0, 0, 10, Color.white);
    this.cell2 = new Cell(10, 4, 12, Color.white);
    this.cell3 = new Cell(2, 3, 5, Color.black);
  }

  // run the maze game
  void testbigbang(Tester t) {
    Maze w = new Maze(10, 10, new Random());
    maze1.bigBang(maze1.width, maze1.height);
  }

  // Cell Tests ------------------------------------------------------------------

  void testDrawCell(Tester t) {
    init();
    t.checkExpect(this.cell1.drawCell(), 
        new RectangleImage(10, 10, OutlineMode.SOLID, Color.white));
    t.checkExpect(this.cell2.drawCell(), 
        new RectangleImage(12, 12, OutlineMode.SOLID, Color.white));
    t.checkExpect(this.cell3.drawCell(), 
        new RectangleImage(5, 5, OutlineMode.SOLID, Color.black));
  }

  void testChangeColor(Tester t) {
    init();
    this.cell1.changeColor(Color.red);
    t.checkExpect(this.cell1.color, Color.red);
    this.cell2.changeColor(Color.green);
    t.checkExpect(this.cell2.color, Color.green);
    this.cell3.changeColor(Color.black);
    t.checkExpect(this.cell3, this.cell3);
  }

  void testfillNextCell(Tester t) {
    init();
    this.maze1.allCells.get(0).fillNextCell("up");
    t.checkExpect(this.maze1.allCells.get(0).ctop, null);
    this.maze1.allCells.get(0).fillNextCell("down");
    t.checkExpect(this.maze1.allCells.get(0).cbottom.color, Color.red);
    this.maze1.allCells.get(0).fillNextCell("left");
    t.checkExpect(this.maze1.allCells.get(0).cleft, null);
    this.maze1.allCells.get(0).fillNextCell("right");
    t.checkExpect(this.maze1.allCells.get(0).cright.color, Color.red);
  }

  // Edge Tests ------------------------------------------------------------------

  void testCenterPoint(Tester t) {
    init();
    t.checkExpect(this.edge1.centerPoint(), new Posn(5, 5));
    t.checkExpect(this.edge2.centerPoint(), new Posn(15, 15));
    t.checkExpect(this.edge3.centerPoint(), new Posn(6, 8));
  }

  void testSameLocation(Tester t) {
    init();
    t.checkExpect(this.edge1.sameLocation(new Posn(0, 0), new Posn(10, 10)), true);
    t.checkExpect(this.edge1.sameLocation(new Posn(10, 10), new Posn(0, 0)), true);
    t.checkExpect(this.edge1.sameLocation(new Posn(0, 0), new Posn(20, 10)), false);
  }

  // Maze Tests ------------------------------------------------------------------

  void testReset(Tester t) {
    init();
    Maze initial = this.maze1;
    t.checkExpect(this.maze1, initial);
    this.maze1.allCells.get(2).changeColor(Color.black); // modify board in some way
    this.maze1.reset(this.maze1.rows, this.maze1.columns, this.maze1.r); // reset
    t.checkExpect(this.maze1, initial); // board is the same as initial state

    Maze initial2 = this.maze2;
    this.maze2.allEdges.remove(3); // modify board in some way
    this.maze2.reset(this.maze2.rows, this.maze2.columns, this.maze2.r); // reset
    t.checkExpect(this.maze2, initial2); // board is the same as initial state
  }

  void testinitVertices(Tester t) {
    init();
    t.checkExpect(maze1.vertices.size(), 16);
    t.checkExpect(maze1.vertices.get(0), new Posn(0, 0));
    t.checkExpect(maze1.vertices.get(15), new Posn(60, 60));
    t.checkExpect(maze2.vertices.size(), 9);
    t.checkExpect(maze1.vertices.get(0), new Posn(0, 0));
    t.checkExpect(maze1.vertices.get(8), new Posn(40, 0));
  }

  void testinitRepresentatives(Tester t) {
    init();
    t.checkExpect(maze1.representatives.size(), 16);
    t.checkExpect(maze1.representatives.get(new Posn(0, 0)), new Posn(40, 40));
    t.checkExpect(maze1.representatives.get(new Posn(60, 60)), new Posn(40, 40));
    t.checkExpect(maze2.representatives.size(), 9);
    t.checkExpect(maze2.representatives.get(new Posn(0, 0)), new Posn(40, 40));
    t.checkExpect(maze2.representatives.get(new Posn(40, 0)), new Posn(40, 40));
  }

  void testinitEdges(Tester t) {
    init();
    t.checkExpect(maze1.allEdges.size(), 24);
    t.checkExpect(maze1.allEdges.get(0), new Edge(new Posn(60, 40), new Posn(60, 60), 1));
    t.checkExpect(maze1.allEdges.get(23), new Edge(new Posn(20, 40), new Posn(40, 40), 6));
    t.checkExpect(maze2.allEdges.size(), 12);
    t.checkExpect(maze2.allEdges.get(0), new Edge(new Posn(40, 20), new Posn(40, 40), 1));
    t.checkExpect(maze2.allEdges.get(11), new Edge(new Posn(20, 20), new Posn(40, 20), 6));
  }


  void testcalculateEdges(Tester t) {
    init();
    ArrayList<Edge> list = new ArrayList<Edge>();
    list.add(new Edge(new Posn(40, 20), new Posn(40, 40), 1));
    list.add(new Edge(new Posn(40, 0), new Posn(40, 20), 1));
    list.add(new Edge(new Posn(0, 20), new Posn(0, 40), 1));
    list.add(new Edge(new Posn(0, 40), new Posn(20, 40), 1));
    list.add(new Edge(new Posn(20, 0), new Posn(40, 0), 1));
    list.add(new Edge(new Posn(20, 40), new Posn(40, 40), 1));
    list.add(new Edge(new Posn(0, 0), new Posn(20, 0), 1));
    list.add(new Edge(new Posn(20, 20), new Posn(20, 40), 3));
    list.add(new Edge(new Posn(0, 0), new Posn(0, 20), 1));
    t.checkExpect(maze2.edgesInTree, list);
    ArrayList<Edge> list2 = new ArrayList<Edge>();
    list2.add(new Edge(new Posn(20, 20), new Posn(20, 40), 1));
    list2.add(new Edge(new Posn(20, 0), new Posn(20, 20), 1));
    list2.add(new Edge(new Posn(0, 20), new Posn(0, 40), 1));
    list2.add(new Edge(new Posn(0, 40), new Posn(20, 40), 1));
    list2.add(new Edge(new Posn(0, 0), new Posn(20, 0), 1));
    list2.add(new Edge(new Posn(0, 0), new Posn(0, 20), 1));
    t.checkExpect(this.maze3.edgesInTree, list2);
  }

  void testfind(Tester t) {
    init();
    t.checkExpect(this.maze1.find(new Posn(0, 0)), new Posn(40, 40));
    t.checkExpect(this.maze1.find(new Posn(20, 20)), new Posn(40, 40));
    t.checkExpect(this.maze2.find(new Posn(0, 0)), new Posn(40, 40));
  }

  void testUnion(Tester t) {
    init();
    // all edges should point to the same representative after being initialized
    t.checkExpect(this.maze1.representatives.get(this.maze1.vertices.get(0)), new Posn(40, 40));
    t.checkExpect(this.maze1.representatives.get(this.maze1.vertices.get(5)), new Posn(40, 40));
    t.checkExpect(this.maze1.representatives.get(this.maze1.vertices.get(11)), new Posn(40, 40));
  }

  void testinitCells(Tester t) {
    init();
    t.checkExpect(this.maze1.allCells.get(0).x, 0);
    t.checkExpect(this.maze1.allCells.get(0).y, 0);
    t.checkExpect(this.maze1.allCells.get(0).color, Color.green);
    t.checkExpect(this.maze1.allCells.get(0).ctop, null);
    t.checkExpect(this.maze1.allCells.get(0).cbottom, this.maze1.allCells.get(1));
    t.checkExpect(this.maze1.allCells.get(0).cleft, null);
    t.checkExpect(this.maze1.allCells.get(0).cright, this.maze1.allCells.get(3));
    t.checkExpect(this.maze1.allCells.size(), 9);

    t.checkExpect(this.maze2.allCells.get(1).x, 0);
    t.checkExpect(this.maze2.allCells.get(1).y, 1);
    t.checkExpect(this.maze2.allCells.get(1).color, Color.white);
    t.checkExpect(this.maze2.allCells.get(1).ctop, this.maze2.allCells.get(0));
    t.checkExpect(this.maze2.allCells.get(1).cbottom, null);
    t.checkExpect(this.maze2.allCells.get(1).cleft, null);
    t.checkExpect(this.maze2.allCells.get(1).cright, this.maze2.allCells.get(3));
    t.checkExpect(this.maze2.allCells.size(), 4);

  }

  void testinitCellEdges(Tester t) {
    init();
    t.checkExpect(this.maze1.allCells.get(0).etop, new Edge(new Posn(0, 0), new Posn(20, 0), 1));
    t.checkExpect(this.maze1.allCells.get(0).eleft, new Edge(new Posn(0, 0), new Posn(0, 20), 1));
    t.checkExpect(this.maze1.allCells.get(0).ebottom, null);
    t.checkExpect(this.maze1.allCells.get(0).eright, null);
    t.checkExpect(this.maze2.allCells.get(3).etop, null);
    t.checkExpect(this.maze2.allCells.get(3).eleft, 
        new Edge(new Posn(20, 20), new Posn(20, 40), 3));
    t.checkExpect(this.maze2.allCells.get(3).ebottom, 
        new Edge(new Posn(20, 40), new Posn(40, 40), 1));
    t.checkExpect(this.maze2.allCells.get(3).eright, 
        new Edge(new Posn(40, 20), new Posn(40, 40), 1));
  }


  void testmakeScene(Tester t) {
    init();
    WorldScene ws = new WorldScene(80, 80);
    ws.placeImageXY(new RectangleImage(20, 20, OutlineMode.SOLID, Color.green), 30, 30);
    ws.placeImageXY(new RectangleImage(20, 20, OutlineMode.SOLID, Color.white), 30, 50);
    ws.placeImageXY(new RectangleImage(20, 20, OutlineMode.SOLID, Color.white), 50, 30);
    ws.placeImageXY(new RectangleImage(20, 20, OutlineMode.SOLID, Color.magenta), 50, 50);
    ws.placeImageXY(new LineImage(new Posn(0, 20), Color.black), 60, 50);
    ws.placeImageXY(new LineImage(new Posn(0, 20), Color.black), 60, 30);
    ws.placeImageXY(new LineImage(new Posn(0, 20), Color.black), 20, 50);
    ws.placeImageXY(new LineImage(new Posn(20, 0), Color.black), 30, 60);
    ws.placeImageXY(new LineImage(new Posn(20, 0), Color.black), 50, 20);
    ws.placeImageXY(new LineImage(new Posn(20, 0), Color.black), 50, 60);
    ws.placeImageXY(new LineImage(new Posn(20, 0), Color.black), 30, 20);
    ws.placeImageXY(new LineImage(new Posn(0, 20), Color.black), 40, 50);
    ws.placeImageXY(new LineImage(new Posn(0, 20), Color.black), 20, 30);
    t.checkExpect(this.maze2.makeScene(), ws);
    WorldScene ws2 = new WorldScene(60, 80);
    ws2.placeImageXY(new RectangleImage(20, 20, OutlineMode.SOLID, Color.green), 30, 30);
    ws2.placeImageXY(new RectangleImage(20, 20, OutlineMode.SOLID, Color.magenta), 30, 50);
    ws2.placeImageXY(new LineImage(new Posn(0, 20), Color.black), 40, 50);
    ws2.placeImageXY(new LineImage(new Posn(0, 20), Color.black), 40, 30);
    ws2.placeImageXY(new LineImage(new Posn(0, 20), Color.black), 20, 50);
    ws2.placeImageXY(new LineImage(new Posn(20, 0), Color.black), 30, 60);
    ws2.placeImageXY(new LineImage(new Posn(20, 0), Color.black), 30, 20);
    ws2.placeImageXY(new LineImage(new Posn(0, 20), Color.black), 20, 30);
    t.checkExpect(this.maze3.makeScene(), ws2);
  }

  void testCompare(Tester t) {
    init();
    EdgeComparator e = new EdgeComparator();
    t.checkExpect(e.compare(this.edge1, this.edge2), -1);
    t.checkExpect(e.compare(this.edge2, this.edge1), 1);
    t.checkExpect(e.compare(this.edge3, this.edge2), 0);
  }

  void testQuickSort(Tester t) {
    ArrayUtils<Integer> ui = new ArrayUtils<Integer>();
    ArrayUtils<String> us = new ArrayUtils<String>();
    init();
    t.checkExpect(list1, list1);
    ui.quickSort(list1, Comparator.naturalOrder());
    t.checkExpect(list1, list1a);
    t.checkExpect(list2, list2);
    us.quickSort(list2, Comparator.naturalOrder());
    t.checkExpect(list2, list2a);
  }

  void testquicksortHelp(Tester t) {
    ArrayUtils<Integer> ui = new ArrayUtils<Integer>();
    ArrayUtils<String> us = new ArrayUtils<String>();
    init();
    t.checkExpect(list1, list1);
    ui.quicksortHelp(list1, Comparator.naturalOrder(), 0, 4);
    t.checkExpect(list1, new ArrayList<Integer>(Arrays.asList(1, 2, 3, 5, 4)));
    us.quicksortHelp(list2, Comparator.naturalOrder(), 0, 1);
    t.checkExpect(list2, new ArrayList<String>(Arrays.asList("b", "d", "e", "a", "c")));
  }

  void testPartition(Tester t) {
    ArrayUtils<Integer> ui = new ArrayUtils<Integer>();
    ArrayUtils<String> us = new ArrayUtils<String>();
    init();
    ui.partition(list1, Comparator.naturalOrder(), 0, 4, 3);
    t.checkExpect(list1, new ArrayList<Integer>(Arrays.asList(1, 2, 3, 5, 4)));
    ui.partition(list1a, Comparator.naturalOrder(), 0, 2, 1);
    t.checkExpect(list1a, new ArrayList<Integer>(Arrays.asList(1, 2, 3, 4, 5)));
    us.partition(list2, Comparator.naturalOrder(), 0, 4, "c");
    t.checkExpect(list2, new ArrayList<String>(Arrays.asList("a", "b", "e", "d", "c")));
    us.partition(list2a, Comparator.naturalOrder(), 0, 2, "b");
    t.checkExpect(list2a, new ArrayList<String>(Arrays.asList("b", "a", "c", "d", "e")));
  }

  void testSwap(Tester t) {
    ArrayUtils<Integer> u = new ArrayUtils<Integer>();
    ArrayList<Integer> list = new ArrayList<Integer>();
    list.add(1);
    list.add(2);
    list.add(3);
    u.swap(list, 0, 2);
    ArrayList<Integer> list2 = new ArrayList<Integer>();
    list2.add(3);
    list2.add(2);
    list2.add(1);
    t.checkExpect(list, list2);
    u.swap(list, 1, 2);
    ArrayList<Integer> list3 = new ArrayList<Integer>();
    list3.add(3);
    list3.add(1);
    list3.add(2);
    t.checkExpect(list, list3);
  }

  void testonKey(Tester t) {
    init();
    //    this.maze2.onKeyEvent("r");
    //    t.checkExpect(this.maze2, null);

    //    init();
    //    this.maze2.onKeyEvent("b");
    //    t.checkExpect(this.maze2.bfs(), null);

    init();
    Maze nochange = this.maze2;
    this.maze2.onKeyEvent("up"); // moving up fromt the top left corner
    t.checkExpect(this.maze2, nochange);

    Maze nochange2 = this.maze2;
    this.maze2.onKeyEvent("left"); // moving left fromt the top left corner
    t.checkExpect(this.maze2, nochange2);

    init(); // move right
    this.maze2.onKeyEvent("right"); 
    t.checkExpect(this.maze2.allCells.get(this.maze2.columns).color, Color.red);
    t.checkExpect(this.maze2.currentCell, this.maze2.allCells.get(this.maze2.columns));
    this.maze1.onKeyEvent("right"); 
    t.checkExpect(this.maze1.allCells.get(this.maze1.columns).color, Color.red);
    t.checkExpect(this.maze1.currentCell, this.maze1.allCells.get(this.maze1.columns));  

    init(); // move down then right
    this.maze2.onKeyEvent("down"); 
    t.checkExpect(this.maze2.allCells.get(1).color, Color.red);
    t.checkExpect(this.maze2.currentCell, this.maze2.allCells.get(1));  
    this.maze2.onKeyEvent("right"); 
    t.checkExpect(this.maze2.allCells.get(1).color, Color.red);
    t.checkExpect(this.maze2.allCells.get(this.maze2.columns + 1).color, Color.magenta);
    t.checkExpect(this.maze2.currentCell, this.maze2.allCells.get(1)); 

    init(); // move down then up
    this.maze2.onKeyEvent("down"); 
    t.checkExpect(this.maze2.allCells.get(1).color, Color.red);
    t.checkExpect(this.maze2.currentCell, this.maze2.allCells.get(1));  
    this.maze2.onKeyEvent("up"); 
    t.checkExpect(this.maze2.allCells.get(0).color, Color.red);
    t.checkExpect(this.maze2.allCells.get(1).color, Color.yellow);
    t.checkExpect(this.maze2.currentCell, this.maze2.allCells.get(0)); 

    init(); // move up
    this.maze2.onKeyEvent("up");  // moving nowhere
    t.checkExpect(this.maze2.allCells.get(0).color, Color.green);
    t.checkExpect(this.maze2.currentCell, this.maze2.allCells.get(0));  

    init(); // move left
    this.maze2.onKeyEvent("left"); // moving nowhere
    t.checkExpect(this.maze2.allCells.get(0).color, Color.green);
    t.checkExpect(this.maze2.currentCell, this.maze2.allCells.get(0));  

  }

  void testBFS(Tester t) {
    init();
    WorldScene ws = this.maze2.makeScene();
    ws.placeImageXY(new RectangleImage(20, 20, OutlineMode.SOLID, Color.yellow), 50, 30);
    t.checkExpect(this.maze2.bfs(), ws);
    WorldScene ws2 = this.maze3.makeScene();
    ws2.placeImageXY(new RectangleImage(20, 20, OutlineMode.SOLID, Color.yellow), 30, 30);
    t.checkExpect(this.maze3.bfs(), ws2);
    WorldScene ws3 = this.maze1.makeScene();
    ws3.placeImageXY(new RectangleImage(20, 20, OutlineMode.SOLID, Color.yellow), 70, 50);
    ws3.placeImageXY(new RectangleImage(20, 20, OutlineMode.SOLID, Color.yellow), 70, 30);
    ws3.placeImageXY(new RectangleImage(20, 20, OutlineMode.SOLID, Color.yellow), 50, 30);
    t.checkExpect(this.maze1.bfs(), ws3);

  }

  void testDFS(Tester t) {
    init();
    WorldScene ws = this.maze2.makeScene();
    ws.placeImageXY(new RectangleImage(20, 20, OutlineMode.SOLID, Color.yellow), 50, 30);
    t.checkExpect(this.maze2.dfs(), ws);
    WorldScene ws2 = this.maze3.makeScene();
    ws2.placeImageXY(new RectangleImage(20, 20, OutlineMode.SOLID, Color.yellow), 30, 30);
    t.checkExpect(this.maze3.dfs(), ws2);
    WorldScene ws3 = this.maze1.makeScene();
    ws3.placeImageXY(new RectangleImage(20, 20, OutlineMode.SOLID, Color.yellow), 70, 50);
    ws3.placeImageXY(new RectangleImage(20, 20, OutlineMode.SOLID, Color.yellow), 70, 30);
    ws3.placeImageXY(new RectangleImage(20, 20, OutlineMode.SOLID, Color.yellow), 50, 30);
    t.checkExpect(this.maze1.dfs(), ws3);
  }

  void testSearchHelp(Tester t) {
    init();
    //dfs
    ArrayList<Cell> path1 = new ArrayList<Cell>();
    path1.add(this.maze2.allCells.get(2));
    t.checkExpect(this.maze2.searchHelp(new Stack<Cell>()), path1);
    ArrayList<Cell> path2 = new ArrayList<Cell>();
    path2.add(this.maze3.allCells.get(0));
    t.checkExpect(this.maze3.searchHelp(new Stack<Cell>()), path2);
    ArrayList<Cell> path5 = new ArrayList<Cell>();
    path5.add(this.maze1.allCells.get(7));
    path5.add(this.maze1.allCells.get(6));
    path5.add(this.maze1.allCells.get(3));
    t.checkExpect(this.maze1.searchHelp(new Stack<Cell>()), path5);

    // bfs
    ArrayList<Cell> path3 = new ArrayList<Cell>();
    path3.add(this.maze2.allCells.get(2));
    t.checkExpect(this.maze2.searchHelp(new Queue<Cell>()), path3);
    ArrayList<Cell> path4 = new ArrayList<Cell>();
    path4.add(this.maze3.allCells.get(0));
    t.checkExpect(this.maze3.searchHelp(new Queue<Cell>()), path4);
    ArrayList<Cell> path6 = new ArrayList<Cell>();
    path6.add(this.maze1.allCells.get(7));
    path6.add(this.maze1.allCells.get(6));
    path6.add(this.maze1.allCells.get(3));
    t.checkExpect(this.maze1.searchHelp(new Queue<Cell>()), path6);
  }

  void testReconstruct(Tester t) {
    init();
    HashMap<Cell, Cell> map = new HashMap<Cell, Cell>();
    map.put(this.maze2.allCells.get(0), this.maze2.allCells.get(1));
    map.put(this.maze2.allCells.get(1), this.maze2.allCells.get(2));
    map.put(this.maze2.allCells.get(2), this.maze2.allCells.get(2));
    ArrayList<Cell> path = new ArrayList<Cell>();
    path.add(this.maze2.allCells.get(0));
    path.add(this.maze2.allCells.get(1));
    path.add(this.maze2.allCells.get(2));
    t.checkExpect(this.maze2.reconstruct(map, this.maze2.allCells.get(0), 
        new ArrayList<Cell>()), new ArrayList<Cell>());
    ArrayList<Cell> list = new ArrayList<Cell>();
    list.add(this.maze2.allCells.get(0));
    t.checkExpect(this.maze2.reconstruct(map, this.maze2.allCells.get(1), 
        new ArrayList<Cell>()), list);
  }

  void testIsEmpty(Tester t) {
    Stack<Integer> s = new Stack<Integer>();
    t.checkExpect(s.isEmpty(), true);
    s.add(1);
    t.checkExpect(s.isEmpty(), false);

    Queue<Integer> q = new Queue<Integer>();
    t.checkExpect(q.isEmpty(), true);
    q.add(1);
    t.checkExpect(q.isEmpty(), false);
  }

  void testRemove(Tester t) {
    Stack<Integer> s = new Stack<Integer>();
    s.add(1);
    s.add(2);
    Queue<Integer> q = new Queue<Integer>();
    q.add(1);
    q.add(2);

    t.checkExpect(s.remove(), 2);
    t.checkExpect(s.remove(), 1);

    t.checkExpect(q.remove(), 1);
    t.checkExpect(q.remove(), 2);
  }

  void testAdd(Tester t) {
    Stack<Integer> s = new Stack<Integer>();
    Queue<Integer> q = new Queue<Integer>();

    s.add(1);
    s.add(2);
    ArrayDeque<Integer> result = new ArrayDeque<Integer>();
    result.add(2);
    result.add(1);
    t.checkExpect(s.contents, result);

    q.add(1);
    q.add(2);
    ArrayDeque<Integer> result2 = new ArrayDeque<Integer>();
    result2.add(1);
    result2.add(2);
    t.checkExpect(q.contents, result2);
  }

}
