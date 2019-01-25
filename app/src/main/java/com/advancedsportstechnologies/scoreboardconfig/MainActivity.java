package com.advancedsportstechnologies.scoreboardconfig;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

/**
 *  Application Welcome screen presenting the corporation logo, Scoreboard Configuration title,
 *  and a button allowing the user to scan for Bluetooth devices at their convenience
 */
public class MainActivity extends AppCompatActivity {

    /**
     * Register the button when the app is opened
     *
     * @param savedInstanceState The saved state of the previous application run
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(getApplicationContext(), BluetoothFinderActivity.class);
                        startActivity(intent);
                    }
                },
                1500
        );
    }


}