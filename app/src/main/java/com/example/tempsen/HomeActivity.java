package com.example.tempsen;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    Button btnVoirRegions;
    TextView titleText, subtitleText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialisation des vues
        titleText = findViewById(R.id.titleText);
        subtitleText = findViewById(R.id.subtitleText);
        btnVoirRegions = findViewById(R.id.btnVoirRegions);

        // Action du bouton
        btnVoirRegions.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, MainActivity.class));
        });
    }
}