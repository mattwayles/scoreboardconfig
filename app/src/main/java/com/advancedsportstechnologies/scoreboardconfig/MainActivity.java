package com.advancedsportstechnologies.scoreboardconfig;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = findViewById(R.id.button);
    }

    public void startScan(View view) {
        Intent intent = new Intent(getApplicationContext(), BluetoothFinderActivity.class);
        startActivity(intent);
    }
}
