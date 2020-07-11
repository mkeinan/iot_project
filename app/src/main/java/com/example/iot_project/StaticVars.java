package com.example.iot_project;

import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class StaticVars {
    public static int numRows;
    public static int numCols;

    public static int startRow;
    public static int startCol;

    public static int finishRow;
    public static int finishCol;

    public static int curRow;
    public static int curCol;

    public static String direction;

    public static String grid;
    public static String algo;

    public static int backtrackMinCount;
    public static List<Pair<Integer, Integer>> backtrackMinSol;

    public static boolean hasReachTarget;

    public static void printStatusToLog(){
        Log.w("-D-", "StaticVars: numRows=" + numRows + "numCols=" + numCols +
                " startRow=" + startRow + " startCol=" + startCol + " finishRow=" +
                finishRow + " finishCol=" + finishCol + " curRow=" + curRow + "curCol=" + curCol +
                " grid=" + grid + " algo=" + algo + " backtrackMinCount=" + backtrackMinCount);
    }

    public static void resetStatus(){
        numRows = 0;
        numCols = 0;
        startRow = 0;
        startCol = 0;
        finishRow = 0;
        finishCol = 0;
        curRow = 0;
        curCol = 0;
        direction = "up";
        grid = "?";
        algo = "None";
        backtrackMinCount = 0;
        backtrackMinSol = new ArrayList<>();
        hasReachTarget = false;
    }
}
