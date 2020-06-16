package com.example.iot_project;

enum Direction{
    Up,
    Right,
    Down,
    Left
}

public class Wrapper {
    private Graph graph;
    private Direction cur_direction;

    public Wrapper() {
        CreateGraph();
        cur_direction = Direction.Up;
    }

    void Start() throws Exception {
        StaticVars.hasReachTarget = false;
        //TODO this is the arduino connection
        // graph.OnLocationChanged.AddListener(myAction);
        if (!(StaticVars.startRow == StaticVars.finishRow && StaticVars.startCol == StaticVars.finishCol))
        {
            StartAlgo();
        }
    }

    // this function kicks in when we get an "event"
    // so TODO we need to switch this to handleMessage...
    void Action(int n)
    {
        //StartCoroutine(Mover(n));
        int result=-1;
        //TODO
        // that is the big one. _robotScript is the component that sends data to the arduino
        // RotateToDirection(n);
        // result = _robotScript.Move(n);
        if (result == 1)
        {
            StaticVars.hasReachTarget = true;
        }
        if (result == -1)
        {
            graph.addObstacle(StaticVars.curRow, StaticVars.curCol);
        }
    }

    private void RotateToDirection(int n){
        if (cur_direction.equals(n)){
            return;
        }
        // TODO rotation logic here
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
        graph = new Graph(cols, rows, dataForGraph);
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

