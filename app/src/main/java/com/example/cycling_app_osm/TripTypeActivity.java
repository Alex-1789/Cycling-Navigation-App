package com.example.cycling_app_osm;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class TripTypeActivity extends AppCompatActivity {
    public int tripChosen = -1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.trip_type_choice);
        Button sightseeing_btn = findViewById(R.id.sightseeing_button);
        Button urban_btn = findViewById(R.id.urban_button);

        sightseeing_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               tripChosen = 0;
               startActivity(new Intent(TripTypeActivity.this, LocationChoiceActivity.class)
                       .putExtra("tripChosen", tripChosen));
            }
        });

        urban_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tripChosen = 1;
                startActivity(new Intent(TripTypeActivity.this, LocationChoiceActivity.class)
                        .putExtra("tripChosen", tripChosen));
            }
        });
    }
}