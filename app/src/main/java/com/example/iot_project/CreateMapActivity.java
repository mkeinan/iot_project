package com.example.iot_project;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.iot_project.StaticVars;

public class CreateMapActivity extends AppCompatActivity {

    Button continueButton;
    Button defaultButton;
    EditText numRowsText;
    EditText numColsText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_map);
        InitUI();
    }

    private void InitUI() {
        Log.w("-D-", "CreateMapActivity.InitUI(): initializing UI");
        continueButton = (Button) findViewById(R.id.button_create_map_continue);
        defaultButton = (Button) findViewById(R.id.button_default_rows_cols);
        numRowsText = (EditText) findViewById(R.id.choose_num_rows);
        numColsText = (EditText) findViewById(R.id.choose_num_cols);

        numRowsText.setSelection(2,9);
        numRowsText.setSelection(2,9);

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                storeMap();
                Intent chooseParamsIntent = new Intent(getApplicationContext(), ChooseParamsActivity.class);
                startActivity(chooseParamsIntent);
                finish();
            }
        });

        defaultButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setDefaultValues();
            }
        });
    }

    private void storeMap() {
        String numRowsStr = numRowsText.getText().toString();
        String numColsStr = numColsText.getText().toString();
        int numRows = Integer.parseInt(numRowsStr);
        int numCols = Integer.parseInt(numColsStr);
        StaticVars.numRows = numRows;
        StaticVars.numCols = numCols;

        String[][] mapDescription = new String[StaticVars.numRows][StaticVars.numCols];
        for (int i = 0; i < StaticVars.numRows; i++) {
            for (int j = 0; j < StaticVars.numCols; j++) {
                mapDescription[i][j] = "O";
            }
        }
        StaticVars.grid = new Graph(StaticVars.numCols,StaticVars.numRows ,mapDescription, null).PrintGraph();
    }

    private void setDefaultValues(){
        numRowsText.setText("3");
        numColsText.setText("2");
    }
}
