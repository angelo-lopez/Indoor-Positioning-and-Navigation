package com.angelsoft.angelo_romel.indoorpositioning;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class SearchActivity extends AppCompatActivity {

    private SharedPreferences pref;
    private EditText roomTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        pref = PreferenceManager.getDefaultSharedPreferences(this);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.nav_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                roomTextView = (EditText)findViewById(R.id.room_edittext);
                if(roomTextView.length() != 0) {
                    pref.edit().putString("destinationId", roomTextView.getText().toString().trim()).apply();
                    finish();
                }
                else {
                    Toast.makeText(SearchActivity.this, getString(R.string.no_destination_specified),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        FloatingActionButton toiletFab = (FloatingActionButton) findViewById(R.id.toilet_fab);
        toiletFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pref.edit().putString("destinationId", "toilet").apply();
                finish();
            }
        });

        FloatingActionButton stairFab = (FloatingActionButton) findViewById(R.id.staircase_fab);
        stairFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pref.edit().putString("destinationId", "staircase").apply();
                finish();
            }
        });

        FloatingActionButton liftFab = (FloatingActionButton) findViewById(R.id.lift_fab);
        liftFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pref.edit().putString("destinationId", "lift").apply();
                finish();
            }
        });

        FloatingActionButton emergerncyExitFab = (FloatingActionButton) findViewById(R.id.emergency_exit_fab);
        emergerncyExitFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pref.edit().putString("destinationId", "emergency exit").apply();
                finish();
            }
        });
    }

}
