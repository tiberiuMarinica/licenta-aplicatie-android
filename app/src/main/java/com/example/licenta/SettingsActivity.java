package com.example.licenta;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if(findViewById(R.id.fragment_container) != null){
            if(savedInstanceState != null){
                return;
            }

            getFragmentManager().beginTransaction().add(R.id.fragment_container, new SettingsFragment()).commit();
        }
    }
}
