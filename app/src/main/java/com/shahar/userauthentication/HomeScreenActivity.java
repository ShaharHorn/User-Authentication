package com.shahar.userauthentication;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.net.URI;
import java.security.Provider;

public class HomeScreenActivity extends AppCompatActivity {
    private static final String TAG = "MAIN_ACTIVITY";
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e(TAG, "onCreate() >>");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);

        mAuth = FirebaseAuth.getInstance();
        Button signOutButton = (Button)findViewById(R.id.signOutButton) ;
        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAuth.signOut();
                finish();
                startActivity(new Intent(HomeScreenActivity.this,MainActivity.class ));

            }
        });

        displayUserInfo();

        Log.e(TAG, "onCreate() <<");
    }

    private void displayUserInfo() {
        Log.e(TAG, "displayUserInfo() >>");
        FirebaseUser currentUser = mAuth.getCurrentUser();
        TextView userName = findViewById(R.id.userNameTextView);
        TextView userEmail = findViewById(R.id.emailTextView);

        if (currentUser != null) {
            String displayName = currentUser.getDisplayName();
            String email = currentUser.getEmail();

            if (displayName != null) {
                userName.setText(displayName);
            }
            if (email != null) {
                userEmail.setText(email);
            }
        }

        Log.e(TAG, "displayUserInfo() <<");
    }

}
