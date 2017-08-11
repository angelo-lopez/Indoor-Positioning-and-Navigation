package com.angelsoft.angelo_romel.indoorpositioning;

import android.*;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_WIFI_STATE) +
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.CHANGE_WIFI_STATE) +
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) +
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            Intent permissionIntent = new Intent(this, PermissionActivity.class);
            startActivity(permissionIntent);
            this.finish();
        }

        FloatingActionButton showMapFab = (FloatingActionButton) findViewById(R.id.show_map_fab);
        showMapFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent mapsOverlayIntent = new Intent(MainActivity.this, MapsOverlayActivity.class);
                startActivity(mapsOverlayIntent);
            }
        });
    }//end onCreate()
}
