package com.example.iot_project;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Pair;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Graph {
    private int columns;
    private int rows;
    private int tiles;
    private boolean obstacleHandle;
    private LinkedList<Integer>[] adj; //Adjacency Lists
    private String[][] data;
    private Dictionary<Integer, Pair<Integer,Integer>> numberToCordMap;

    private Handler myHandler;
    public boolean should_wait = false;

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

    public String[][] getData() {
        return data;
    }

    String PrintGraph() {
        String res = "";
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                res += data[row][col];
            }
            res += "\n";
        }
        return res;
    }

    public Graph(int columns, int rows, String[][] data, Handler handler)
    {
        this.myHandler = handler;
        this.obstacleHandle = false;
        this.columns = columns;
        this.rows = rows;
        this.data = data;
        this.tiles = rows * columns;
        this.adj = new LinkedList[tiles];
        for (int i = 0; i < tiles; ++i)
        {
            this.adj[i] = new LinkedList();
        }
        this.fillTheMap();
        this.CreateAdjacencies();
    }

    void fillTheMap()
    {
        numberToCordMap= new Hashtable<Integer, Pair<Integer,Integer>>();
        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < columns; j++)
            {
                numberToCordMap.put(i*columns+j, new Pair(i,j));
            }
        }
    }

    void CreateAdjacencies()
    {
        for (int i = 0; i < rows; ++i)
        {
            for (int j = 0; j < columns; j++)
            {
                if (data[i][j] != "X")
                {
                    CreateAdjacencies(i, j);
                }
            }
        }
    }
    void CreateAdjacencies(int row, int col)
    {
        addEdge(row, col, row + 1, col);
        addEdge(row, col, row - 1, col);
        addEdge(row, col, row, col + 1);
        addEdge(row, col, row, col - 1);
    }

    void RemoveAdjacencies(int row, int col)
    {
        for (LinkedList<Integer> item : adj)
        {
            if (item.contains(row * columns + col)){
                item.remove(item.indexOf(row * columns + col));
            }
        }
        adj[row * columns + col].clear();
    }

    void addEdge(int vx, int vy, int wx, int wy)
    {
        if (wx < 0 || wx >= rows || wy < 0 || wy >= columns)
        {
            return;
        }
        if (!data[vx][vy].equals("X") && !data[wx][wy].equals("X"))
        {
            adj[vx * columns + vy].add(wx * columns + wy);
        }
    }

    public void addObstacle(int row, int col)
    {
        obstacleHandle = true;
        data[row][col] = "X";
        RemoveAdjacencies(row, col);
    }

    public int cost[];

    public void ASTAR(int x ,int y, Graph graph){

        Log.w("-I-", "Graph.ASTAR: starting to run..");

        int s = x * columns + y;
        // Mark all the vertices as not visited(By default
        // set as false)
        boolean closed[] = new boolean[columns * rows];
        for (int i = 0; i < columns * rows; i++)
        {
            closed[i] = false;
        }
        cost = new int[columns * rows];
        for (int j = 0; j < columns * rows; j++)
        {
            cost[j] = 0;
        }

        // Create a priority queue for BFS
        //PriorityQueue<Integer> queue = new PriorityQueue<Integer>();
        PriorityQueue<Integer> queue = new PriorityQueue<Integer>(11, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return evaluationFunction(o1)-evaluationFunction(o2);
            }
        });

        // Mark the current node as visited and enqueue it
        closed[s] = true;
        data[x][y] = "V";
        queue.addAll(adj[s]);
        UpdateCurrentLocation(x, y);

        while (queue.size() != 0)
        {
            Iterator<Integer> iter = queue.iterator();
            while (iter.hasNext()){
                int mapIndex = iter.next();
                Log.e("-DD-", "index " + mapIndex + "  eval = " + evaluationFunction(mapIndex));
            }
            // re-organize the queue...?

            // Dequeue a vertex from queue and print it
            s = queue.poll();
            Log.e("-DD-", "index received from the queue: " + s);
            if (StaticVars.hasReachTarget)
            {
                return;
            }
            Pair<Integer,Integer> current = numberToCordMap.get(s);
            Integer cur_x = current.first;
            Integer cur_y = current.second;
            MoveToTarget(cur_x, cur_y);
            if (!(data[cur_x][cur_y].equals("X")))
            {
                data[cur_x][cur_y] = "V";
            }
            closed[s] = true;

            // Get all adjacent vertices of the dequeued vertex s
            // If a adjacent has not been visited, then mark it
            // visited and enqueue it
            Iterator<Integer> i = adj[s].listIterator();
            while (i.hasNext())
            {
                int n = i.next();
                if (!closed[n] && !queue.contains(n))
                {
                    cost[n]=cost[s]+1;
                    queue.add(n);
                }
                StaticVars.grid = PrintGraph();
                StaticVars.printStatusToLog();
            }
        }
    }

    private int evaluationFunction(int index){
        Pair<Integer,Integer> current = numberToCordMap.get(index);
        int heuristicScore=0;

        heuristicScore+=ManhattanHeuristic(current.first,current.second);

        return (cost[index]/2) + heuristicScore;
    }

    private int ManhattanHeuristic(Integer row, Integer col) {
        return Math.abs(row-StaticVars.finishRow) + Math.abs(col-StaticVars.finishCol);
    }

    public void DFS(int x,int y, Graph graph)
    {
        // Mark all the vertices as not visited
        boolean[] visited = new boolean[columns * rows];
        for (int i = 0; i < columns * rows; i++)
        {
            visited[i] = false;
        }

        // Call the recursive helper function
        // to print DFS traversal
        DFSUtil(x,y, visited);
    }

    void DFSUtil(int x,int y, boolean[] visited)
    {
        // Mark the current node as visited
        // and print it
        int s = x * columns + y;
        visited[s] = true;
        MoveToTarget(x, y);
        if (data[x][y].equals("X"))
        {
            return;
        }
        data[x][y] = "V";

        // Recur for all the vertices
        // adjacent to this vertex
        LinkedList<Integer> vList = new LinkedList<Integer>(adj[s]);
        for (Integer n : vList)
        {
            if (StaticVars.hasReachTarget)
            {
                return;
            }
            if (!visited[n])
            {
                StaticVars.grid = PrintGraph();
                StaticVars.printStatusToLog();
                Pair<Integer,Integer> current = numberToCordMap.get(n);
                DFSUtil(current.first,current.second, visited);
            }
        }
    }

    public void BFS(int x,  int y, Graph graph)
    {
        int s = x * columns + y;
        // Mark all the vertices as not visited(By default
        // set as false)
        boolean visited[] = new boolean[columns * rows];
        for (int i = 0; i < columns * rows; i++)
        {
            visited[i] = false;
        }

        // Create a queue for BFS
        LinkedList<Integer> queue = new LinkedList<Integer>();

        // Mark the current node as visited and enqueue it
        visited[s] = true;
        data[x][y] = "V";
        queue.add(s);
        UpdateCurrentLocation(x, y);

        while (queue.size() != 0)
        {
            // Dequeue a vertex from queue and print it
            s = queue.poll();

            // Get all adjacent vertices of the dequeued vertex s
            // If a adjacent has not been visited, then mark it
            // visited and enqueue it
            Iterator<Integer> i = adj[s].listIterator();
            while (i.hasNext())
            {
                if (StaticVars.hasReachTarget)
                {
                    return;
                }
                int n = i.next();
                if (!visited[n])
                {
                    Pair<Integer,Integer> current = numberToCordMap.get(n);
                    Integer cur_x = current.first;
                    Integer cur_y = current.second;
                    MoveToTarget(cur_x, cur_y);
                    if (!(data[cur_x][cur_y].equals("X")))
                    {
                        data[cur_x][cur_y] = "V";
                    }
                    visited[n] = true;

                }
                queue.add(n);
                StaticVars.grid = PrintGraph();
                StaticVars.printStatusToLog();
            }
        }
    }

    private void MoveToTarget(Integer x_target, Integer y_target) {
        Log.w("-D-", "Graph.MoveToTarget(): moving to (" + x_target + ", " + y_target + ")");
        CalculateMoveList(x_target, y_target);
        int prev_x = 0, prev_y = 0;
        List<Pair<Integer, Integer>> moveList = StaticVars.backtrackMinSol;
        for (Pair<Integer, Integer> item : moveList)
        {
            if (StaticVars.hasReachTarget)
            {
                Log.w("-D-", "Graph.MoveToTarget(): Target has been reached... I'm not going to do the move");
                return;
            }
            prev_x = StaticVars.curRow;
            prev_y = StaticVars.curCol;
            if (item.first>StaticVars.curRow && item.second == StaticVars.curCol)
            {
                //move Down
                UpdateCurrentLocation(item.first, item.second);
                should_wait = true;
                Message directionMsg = myHandler.obtainMessage(
                        Wrapper.MESSAGE_DIRECTION, 2, -1, null);  // send a message to the Wrapper
                directionMsg.sendToTarget();
                Log.w("-W-", "Graph.MoveToTarget(): going to busy-wait. TID = " + Thread.currentThread().getId());
                while (should_wait){
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e){
                        Log.e("-W-", "Graph.MoveToTarget(): InterruptedException ", e);
                    }
                }
                CheckForObstacle(prev_x, prev_y);
                continue;
            }
            if (item.first < StaticVars.curRow && item.second == StaticVars.curCol)
            {
                //move Up
                UpdateCurrentLocation(item.first, item.second);
                should_wait = true;
                Message directionMsg = myHandler.obtainMessage(
                        Wrapper.MESSAGE_DIRECTION, 0, -1, null);  // send a message to the Wrapper
                directionMsg.sendToTarget();
                Log.w("-W-", "Graph.MoveToTarget(): going to busy-wait. TID = " + Thread.currentThread().getId());
                while (should_wait){
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e){
                        Log.e("-W-", "Graph.MoveToTarget(): InterruptedException ", e);
                    }
                }
                CheckForObstacle(prev_x, prev_y);
                continue;
            }
            if (item.first == StaticVars.curRow && item.second > StaticVars.curCol)
            {
                //move Right
                UpdateCurrentLocation(item.first, item.second);
                should_wait = true;
                Message directionMsg = myHandler.obtainMessage(
                        Wrapper.MESSAGE_DIRECTION, 1, -1, null);  // send a message to the Wrapper
                directionMsg.sendToTarget();
                Log.w("-W-", "Graph.MoveToTarget(): going to busy-wait. TID = " + Thread.currentThread().getId());
                while (should_wait){
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e){
                        Log.e("-W-", "Graph.MoveToTarget(): InterruptedException ", e);
                    }
                }
                CheckForObstacle(prev_x, prev_y);
                continue;
            }
            if (item.first == StaticVars.curRow && item.second < StaticVars.curCol)
            {
                //move Left
                UpdateCurrentLocation(item.first, item.second);
                should_wait = true;
                Message directionMsg = myHandler.obtainMessage(
                        Wrapper.MESSAGE_DIRECTION, 3, -1, null);  // send a message to the Wrapper
                directionMsg.sendToTarget();
                Log.w("-W-", "Graph.MoveToTarget(): going to busy-wait. TID = " + Thread.currentThread().getId());
                while (should_wait){
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e){
                        Log.e("-W-", "Graph.MoveToTarget(): InterruptedException ", e);
                    }
                }
                CheckForObstacle(prev_x, prev_y);
                continue;
            }
        }
    }

    private void CalculateMoveList(Integer x_target, Integer y_target) {
        String[][] new_data = data;
        int[][] sol = new int[rows][columns];
        List<Pair<Integer, Integer>> sol2 = new ArrayList<>();
        StaticVars.backtrackMinCount = Integer.MAX_VALUE;
        StaticVars.backtrackMinSol = new ArrayList<>();
        solveMazeUtil(new_data, StaticVars.curRow, StaticVars.curCol,x_target,y_target ,sol,sol2,0);
    }

    private void solveMazeUtil(String[][] maze, int x, int y,int x_target,int y_target, int[][] sol, List<Pair<Integer, Integer>> sol2, int count)
    {
        if (x == x_target && y == y_target)
        {
            sol[x][y] = 1;
            Pair<Integer, Integer> entry = new Pair<>(x, y);
            sol2.add(entry);
            if (count < StaticVars.backtrackMinCount)
            {
                StaticVars.backtrackMinCount = count;
                StaticVars.backtrackMinSol = new ArrayList<Pair<Integer, Integer>>(sol2);
            }
            sol[x][y] = 0;
            sol2.remove(entry);
            return;
        }
        // Check if maze[x][y] is valid
        if (isSafe(maze, x, y, sol) == true)
        {
            // mark x, y as part of solution path
            sol[x][y] = 1;
            Pair<Integer, Integer> entry = new Pair<>(x, y);
            sol2.add(entry);

            solveMazeUtil(maze, x + 1, y, x_target, y_target, sol, sol2, count + 1);
            solveMazeUtil(maze, x - 1, y, x_target, y_target, sol, sol2, count + 1);
            solveMazeUtil(maze, x, y+1, x_target, y_target, sol, sol2, count + 1);
            solveMazeUtil(maze, x , y-1, x_target, y_target, sol, sol2, count + 1);

            sol[x][y] = 0;
            sol2.remove(entry);
        }
    }

    private boolean isSafe(String[][] maze, int x, int y,int[][] sol)
    {
        if (x >= 0 && x < rows && y >= 0 && y < columns && maze[x][y].equals("V") && sol[x][y] == 0) {
            return true;
        }
        return false;
    }

    private void CheckForObstacle(int prev_x, int prev_y) {
        if (obstacleHandle)
        {
            UpdateCurrentLocation(prev_x, prev_y);
            obstacleHandle = false;
        }
    }

    private void UpdateCurrentLocation(int x, int y) {
        StaticVars.curRow = x;
        StaticVars.curCol = y;
    }
}

