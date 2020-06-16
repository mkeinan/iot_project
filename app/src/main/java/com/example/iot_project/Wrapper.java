package com.example.iot_project;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

public class Wrapper extends Thread implements Handler.Callback {

    public interface Direction {
        public static final int Up = 0;
        public static final int Right = 1;
        public static final int Down = 2;
        public static final int Left = 3;
    }

    public static final int MESSAGE_DIRECTION = 0;

    public Graph graph;
    private int cur_direction;

    public SimulationActivity.Robot myRobot;  // points to THE SAME Robot as the field of SimulationActivity

    public boolean shouldBusyWait = false;

    public Wrapper(SimulationActivity.Robot robot) {
        myRobot = robot;
        CreateGraph();
        cur_direction = Direction.Up;
    }

    // TODO remember to invoke this method only when you have bluetooth connection
    public void run() {
        StaticVars.hasReachTarget = false;
        if (!(StaticVars.startRow == StaticVars.finishRow && StaticVars.startCol == StaticVars.finishCol))
        {
            try {
                StartAlgo();
            } catch (Exception e){
                Log.e("-E-", "Wrapper.run(): Exception occurred, killing the Wrapper");
                return;
            }
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

        boolean res = myRobot.doCommand(SimulationActivity.Commands.MOVE_TO_BLACK_LINE);
        if (res){
            result = 0;
        } else {
            result = -1;
        }

        if (StaticVars.curRow==StaticVars.finishRow && StaticVars.curCol == StaticVars.finishCol){
            result = 1;
        }

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
        if (cur_direction == n){
            return;
        }
        // TODO rotation logic here:
        //  our current direction is cur_direction (nobody else changes this field).
        //  we need to call the robot's function that changes the direction, and update the field.
        //  ==============   and then busy-wait until the robot finished the move  ===========

        boolean res = true;

        if (cur_direction == Direction.Up){
            if (n == Direction.Left){
                res &= myRobot.doCommand(SimulationActivity.Commands.TURN_90_LEFT);
            }
            if (n == Direction.Right){
                res &= myRobot.doCommand(SimulationActivity.Commands.TURN_90_RIGHT);
            }
            if (n == Direction.Down){
                res &= myRobot.doCommand(SimulationActivity.Commands.TURN_90_RIGHT);
                res &= myRobot.doCommand(SimulationActivity.Commands.TURN_90_RIGHT);
            }
        }
        if (cur_direction == Direction.Left){
            if (n == Direction.Down){
                res &= myRobot.doCommand(SimulationActivity.Commands.TURN_90_LEFT);
            }
            if (n == Direction.Up){
                res &= myRobot.doCommand(SimulationActivity.Commands.TURN_90_RIGHT);
            }
            if (n == Direction.Right){
                res &= myRobot.doCommand(SimulationActivity.Commands.TURN_90_RIGHT);
                res &= myRobot.doCommand(SimulationActivity.Commands.TURN_90_RIGHT);
            }
        }
        if (cur_direction == Direction.Down){
            if (n == Direction.Right){
                res &= myRobot.doCommand(SimulationActivity.Commands.TURN_90_LEFT);
            }
            if (n == Direction.Left){
                res &= myRobot.doCommand(SimulationActivity.Commands.TURN_90_RIGHT);
            }
            if (n == Direction.Up){
                res &= myRobot.doCommand(SimulationActivity.Commands.TURN_90_RIGHT);
                res &= myRobot.doCommand(SimulationActivity.Commands.TURN_90_RIGHT);
            }
        }
        if (cur_direction == Direction.Right){
            if (n == Direction.Up){
                res &= myRobot.doCommand(SimulationActivity.Commands.TURN_90_LEFT);
            }
            if (n == Direction.Down){
                res &= myRobot.doCommand(SimulationActivity.Commands.TURN_90_RIGHT);
            }
            if (n == Direction.Left){
                res &= myRobot.doCommand(SimulationActivity.Commands.TURN_90_RIGHT);
                res &= myRobot.doCommand(SimulationActivity.Commands.TURN_90_RIGHT);
            }
        }
        if (!res){
            Log.e("-E-", "Wrapper.RotateToDirection(): something about the vehicle is horribly wrong");
        }
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

