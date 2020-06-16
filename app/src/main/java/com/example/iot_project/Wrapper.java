package com.example.iot_project;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

enum Direction{
    Up,
    Right,
    Down,
    Left
}

public class Wrapper implements Handler.Callback {

    public static final int MESSAGE_DIRECTION = 0;

    private Graph graph;
    private Direction cur_direction;

    public Wrapper() {
        CreateGraph();
        cur_direction = Direction.Up;
    }

    // TODO remember to invoke this method only when you have bluetooth connection
    void Start() throws Exception {
        StaticVars.hasReachTarget = false;
        if (!(StaticVars.startRow == StaticVars.finishRow && StaticVars.startCol == StaticVars.finishCol))
        {
            StartAlgo();
        }
        Log.e("-D-", "Finished running the algorithm");
    }

    // this function kicks in when we get an "event"
    // so DONE we need to switch this to handleMessage...
    @Override
    public boolean handleMessage(@NonNull Message msg)
    {
        if (!(msg.what == MESSAGE_DIRECTION)){
            Log.e("-E-", "Wrapper.handleMessage(): Unknown message received");
            graph.should_wait = false;
            return false;
        }
        int direction = msg.arg1;
        //StartCoroutine(Mover(n));
        int result = -1;
        RotateToDirection(direction);
        //TODO
        // that is the big one. _robotScript is the component that sends data to the arduino
        // result = _robotScript.Move(n);
        //  ==================  this is actually "move to the next black line"  ===================
        // ==============         and then busy-wait until the robot finished the move  ===========
        //  result is 1 if reached target, 0 if move was ok, -1 if obstacle detected, -2 out of bounds, -3 unexpected.
        if (result == 1)
        {
            StaticVars.hasReachTarget = true;
        }
        if (result == -1)
        {
            graph.addObstacle(StaticVars.curRow, StaticVars.curCol);
        }
        graph.should_wait = false;
        return true;
    }

    // n is the direction (is enum)
    private void RotateToDirection(int n){
        if (cur_direction.equals(n)){
            return;
        }
        // TODO rotation logic here:
        //  our current direction is cur_direction (nobody else changes this field).
        //  we need to call the robot's function that changes the direction, and update the field.
        //  ==============   and then busy-wait until the robot finished the move  ===========
    }

    private void CreateGraph()
    {
        String data = StaticVars.grid;
        String[] dataLines = data.split("\r\n" );
        int rows = dataLines.length;
        int cols = dataLines[0].length();
        String[][] dataForGraph = new String[rows][cols];
        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < cols; j++)
            {
                dataForGraph[i][j] = String.valueOf(dataLines[i].charAt(j));
            }
        }
        graph = new Graph(cols, rows, dataForGraph, new Handler(this));
    }

    private void StartAlgo() throws Exception {

        switch (StaticVars.algo)
        {
            case "BFS":
                graph.BFS(StaticVars.startRow, StaticVars.startCol, graph);
                break;
            case "DFS":
                graph.DFS(StaticVars.startRow, StaticVars.startCol, graph);
                break;
            default:
                throw new Exception("No Algo");
        }
    }
}

