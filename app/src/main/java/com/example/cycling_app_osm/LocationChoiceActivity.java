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

public class LocationChoiceActivity extends AppCompatActivity {
    public int chooseCurrentLocation = -1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.location_choice);
        Button current_location_btn = findViewById(R.id.current_location_button);
        Button choose_location_btn = findViewById(R.id.choose_location_button);
        int tripChosen = getIntent().getIntExtra("tripChosen", -1);

        current_location_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseCurrentLocation = 0;
                startActivity(new Intent(LocationChoiceActivity.this, MainActivity.class)
                        .putExtra("chooseCurrentLocation", chooseCurrentLocation)
                        .putExtra("tripChosen", tripChosen));
            }
        });

        choose_location_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseCurrentLocation = 1;
                startActivity(new Intent(LocationChoiceActivity.this, ChooseParticularLocationActivity.class)
                        .putExtra("chooseCurrentLocation", chooseCurrentLocation)
                        .putExtra("tripChosen", tripChosen));
            }
        });
    }
}