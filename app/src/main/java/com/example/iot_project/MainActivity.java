package com.example.iot_project;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends FirebaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button logout = (Button) findViewById(R.id.logout_button);
        logout.setOnClickListener(getLogoutListener());

        Button goToGetMapButton = (Button) findViewById(R.id.button_go_to_maps);
        goToGetMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent getMapIntent = new Intent(getApplicationContext(), GetMapActivity.class);
                startActivity(getMapIntent);
                finish();
            }
        });
    }
}
