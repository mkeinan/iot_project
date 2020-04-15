package com.example.iot_project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

class FirebaseActivity extends AppCompatActivity {
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;

    @Override
    protected void onCreate(Bundle onSavedInstanceState) {
        super.onCreate(onSavedInstanceState);
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        if(mFirebaseUser == null) {
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        }
    }

    protected View.OnClickListener getLogoutListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logout();
            }
        };
    }

    private void logout() {
        mFirebaseAuth = FirebaseAuth.getInstance();
//        if (mFirebaseAuth == null) {
//            return;
//        }
        mFirebaseAuth.signOut();
        Toast.makeText(this, "Logged out of Firebase", Toast.LENGTH_LONG).show();
    }
}
