package com.advancedsportstechnologies.scoreboardconfig;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 *  Application Welcome screen presenting the corporation logo, Scoreboard Configuration title,
 *  and a ubtton allowign the user to scan for Bluetooth devices at their convenience
 */
public class MainActivity extends AppCompatActivity {

    Button button;

    /**
     * Register the button when the app is opened
     * @param savedInstanceState    The saved state of the previous application run
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = findViewById(R.id.button);
    }

    /**
     * On button press, begin scanning for Bluetooth devices
     * @param view  A handle to the origin button
     */
    public void startScan(View view) {
        Intent intent = new Intent(getApplicationContext(), BluetoothFinderActivity.class);
        startActivity(intent);
    }
}
