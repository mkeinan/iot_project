package com.example.iot_project;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.iot_project.StaticVars;

public class SimulationActivity extends AppCompatActivity {

    Button runButton;
    Button backToMainButton;
    TextView mapText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simulation);
        InitUI();
    }

    private void InitUI() {
        Log.w("-D-", "CreateMapActivity.InitUI(): initializing UI");
        runButton = (Button) findViewById(R.id.button_run_simulation);
        backToMainButton = (Button) findViewById(R.id.button_go_back_to_main);
        mapText = (TextView) findViewById(R.id.text_simulation_map);

        backToMainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(mainIntent);
                finish();
            }
        });

        runButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runSimulation();
            }
        });

        paintMap();
    }

    public void paintMap(){

    }

    public void runSimulation(){

    }
}
