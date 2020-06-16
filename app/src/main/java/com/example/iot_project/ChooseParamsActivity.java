package com.example.iot_project;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.iot_project.StaticVars;

public class ChooseParamsActivity extends AppCompatActivity {

    Button continueButton;
    EditText startRowText;
    EditText startColText;
    EditText finishRowText;
    EditText finishColText;
    EditText chooseAlgoText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_params);
        InitUI();
    }

    private void InitUI() {
        Log.w("-D-", "CreateMapActivity.InitUI(): initializing UI");
        continueButton = (Button) findViewById(R.id.button_choose_params_continue);
        startRowText = (EditText) findViewById(R.id.choose_start_row);
        startColText = (EditText) findViewById(R.id.choose_start_col);
        finishRowText = (EditText) findViewById(R.id.choose_finish_row);
        finishColText = (EditText) findViewById(R.id.choose_finish_col);
        chooseAlgoText = (EditText) findViewById(R.id.choose_algo);

        startRowText.setSelection(0,StaticVars.numRows - 1);
        startColText.setSelection(0,StaticVars.numCols - 1);
        finishRowText.setSelection(0,StaticVars.numRows - 1);
        finishColText.setSelection(0,StaticVars.numCols - 1);

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                storeParams();
                Intent simulationIntent = new Intent(getApplicationContext(), SimulationActivity.class);
                startActivity(simulationIntent);
                finish();
            }
        });
    }

    private void storeParams() {

        // start row and col:
        String startRowStr = startRowText.getText().toString();
        String startColStr = startColText.getText().toString();
        int startRow = Integer.parseInt(startRowStr);
        int startCol = Integer.parseInt(startColStr);
        StaticVars.startRow = startRow;
        StaticVars.startCol = startCol;

        // finish row and col
        String finishRowStr = startRowText.getText().toString();
        String finishColStr = startColText.getText().toString();
        int finishRow = Integer.parseInt(finishRowStr);
        int finishCol = Integer.parseInt(finishColStr);
        StaticVars.finishRow = finishRow;
        StaticVars.finishCol = finishCol;

        // algorithm
        StaticVars.algo = chooseAlgoText.getText().toString();
    }
}
