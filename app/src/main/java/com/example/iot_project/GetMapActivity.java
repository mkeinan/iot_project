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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GetMapActivity extends AppCompatActivity {

    Button getMapButton;
    Button goBackToMainButton;
    Button goToAlgorithmButton;
    TextView mapText;
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
                initGraph(mDocSnap);
                Intent chooseParamsIntent = new Intent(getApplicationContext(), ChooseParamsActivity.class);
                startActivity(chooseParamsIntent);
                finish();
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
        StaticVars.numRows = rowsNum;
        StaticVars.numCols = colsNum;
        StaticVars.grid = new Graph(StaticVars.numRows,  StaticVars.numCols, mapDescription, null).PrintGraph();
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
}
