package com.example.iot_project;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class GetMapActivity extends AppCompatActivity {

    Button getMapButton;
    Button goBackToMainButton;
    Button goToAlgorithmButton;
    TextView mapText;
    Graph tryAlgorithmGraph;
    FirebaseFirestore db;
    private int nextMap = 0;
    private DocumentSnapshot mDocSnap = null;
    public static DocumentSnapshot mPublicDocSnap = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_map);
        db = FirebaseFirestore.getInstance();
        initUI();
    }

    private void initUI() {
        getMapButton = (Button) findViewById(R.id.button2);
        goBackToMainButton = (Button) findViewById(R.id.button3);
        goToAlgorithmButton = (Button) findViewById(R.id.goto_try_algorithm_button);
        mapText = findViewById(R.id.map_text);

        goBackToMainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(mainIntent);
                finish();
            }
        });

        getMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSomeMap();
            }
        });

        goToAlgorithmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    initGraph(mDocSnap);
                    tryAlgorithmGraph.BFS(0,0,mapText,tryAlgorithmGraph);
                    Toast.makeText(getApplicationContext(), "Running BFS...", Toast.LENGTH_LONG).show();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void initGraph(DocumentSnapshot mapDocument){
        Log.w("-D-", "Going to initialize the map for the algorithm...");

        List<String> curMapRows = (List<String>) mapDocument.get("rows");
        Integer rowsNum = mapDocument.getLong("height").intValue();
        Integer colsNum = mapDocument.getLong("width").intValue();

        String[][] mapDescription = new String[rowsNum][colsNum];

        for (int i = 0; i < rowsNum; i++) {
            for (int j = 0; j < colsNum; j++) {
                mapDescription[i][j] = Character.toString(curMapRows.get(i).charAt(j));
            }
        }
        //tryAlgorithmGraph = new Graph(rowsNum, colsNum, mapDescription);
        tryAlgorithmGraph = new Graph(rowsNum,colsNum,mapDescription);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mapText.setText(tryAlgorithmGraph.PrintGraph());
            }
        });
    }

    private void getSomeMap() {
        db.collection("maps")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d("-D-", document.getId() + " => " + document.getData());
                            }
                            mDocSnap = task.getResult().getDocuments().get(nextMap % task.getResult().size());
                            mPublicDocSnap = mDocSnap;
                            nextMap++;
                            List<String> curMapRows = (List<String>) mDocSnap.get("rows");
                            StringBuilder curMapText = new StringBuilder();
                            assert curMapRows != null;
                            for (String row : curMapRows) {
                                curMapText.append(row).append("\n");
                            }
                            mapText.setText(curMapText.toString());
                        } else {
                            Log.w("-D-", "Error getting documents.", task.getException());
                        }
                    }
                });
    }
    class Graph {
        private int columns;
        private int rows;
        private int tiles;
        private LinkedList<Integer> adj[]; //Adjacency Lists
        private String data[][];
        private Dictionary<Integer, Pair<Integer,Integer>> numberToCordMap;


        // Constructor
        Graph(int w, int h) {
            columns = w;
            rows = h;
            tiles = w*h;
            adj = new LinkedList[tiles];
            data = new String[h][w];
            for (int i = 0; i < tiles; ++i) {
                adj[i] = new LinkedList();
            }
            for (int j = 0; j < h; j++) {
                for (int k = 0; k < w; k++) {
                    data[j][k] = "O";
                }
            }
            fillTheMap();
        }

        public Graph(int columns, int rows, String[][] data) {
            this.columns = columns;
            this.rows = rows;
            this.data = data;
            this.tiles = rows*columns;
            this.adj = new LinkedList[tiles];
            for (int i = 0; i < tiles; ++i) {
                this.adj[i] = new LinkedList();
            }
            this.fillTheMap();
            this.CreateAdjacencies();
        }

        void fillTheMap(){
            numberToCordMap= new Hashtable<Integer, Pair<Integer,Integer>>();
            for (int i=0;i<rows;i++){
                for (int j=0;j<columns;j++){
                    numberToCordMap.put(i*columns+j, new Pair(i,j));
                }
            }
        }

        void CreateAdjacencies() {
            for (int i = 0; i < rows; ++i) {
                for (int j = 0; j < columns; j++) {
                    if (!data[i][j].equals("X")) {
                        CreateAdjacencies(i, j);
                    }
                }
            }
        }

        void CreateAdjacencies(int row, int col) {
            addEdge(row, col, row + 1, col);
            addEdge(row, col, row - 1, col);
            addEdge(row, col, row, col + 1);
            addEdge(row, col, row, col - 1);
        }

        // Function to add an edge into the graph
        void addEdge(int vx, int vy, int wx, int wy) {
            if (wx < 0 || wx >= rows || wy < 0 || wy >= columns) {
                return;
            }
            if (!data[vx][vy].equals("X") && !data[wx][wy].equals("X")) {
                adj[vx * columns + vy].add(wx * columns + wy);
            }
        }

        void addObstacle(int row, int col) {
            data[row][col] = "X";
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


        // prints BFS traversal from a given source s
        void BFS(final int x, final int y, final TextView textView, final Graph graph) throws InterruptedException {
            new Thread(){
                @Override
                public void run(){
                    int s = x * rows + y;
                    // Mark all the vertices as not visited(By default
                    // set as false)
                    boolean visited[] = new boolean[columns * rows];

                    // Create a queue for BFS
                    LinkedList<Integer> queue = new LinkedList<Integer>();

                    // Mark the current node as visited and enqueue it
                    visited[s] = true;
                    data[x][y] = "S";
                    queue.add(s);

                    while (queue.size() != 0) {
                        // Dequeue a vertex from queue and print it
                        s = queue.poll();
                        //System.out.print(s+" ");

                        // Get all adjacent vertices of the dequeued vertex s
                        // If a adjacent has not been visited, then mark it
                        // visited and enqueue it
                        Iterator<Integer> i = adj[s].listIterator();
                        while (i.hasNext()) {
                            int n = i.next();
                            if (!visited[n]) {
                                visited[n] = true;
                                Pair<Integer,Integer> current = numberToCordMap.get(n);
                                data[current.first][current.second] = "V";
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        textView.setText(graph.PrintGraph());
                                    }
                                });
                                queue.add(n);
                                SystemClock.sleep(2000);
                            }
                        }
                    }
                    // Thread finished run, ask whether to upload results to cloud:
                    createDialogUploadResultToCloud();
                }
            }.start();

        }
    }

    private void createDialogUploadResultToCloud(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
        // Add the buttons
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                Toast.makeText(getApplicationContext(),
                        "You make the world a better place ❤", Toast.LENGTH_LONG).show();
                uploadCurrentResultToCloud();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                Toast.makeText(getApplicationContext(),
                        "You have decided not to help in the fight against car accidents...", Toast.LENGTH_LONG).show();
            }
        });
        // Set other dialog properties
        builder.setMessage("Algorithm Simulation Finished");
        builder.setMessage("Can we upload your result to the cloud?");

        // Create the AlertDialog
        AlertDialog dialog = builder.create();
    }

    private void uploadCurrentResultToCloud() {
        Toast.makeText(getApplicationContext(),
                "PLEASE CHANGE ME SO I'LL UPLOAD THE RESULT", Toast.LENGTH_LONG).show();
    }
}
