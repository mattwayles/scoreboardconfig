package com.advancedsportstechnologies.scoreboardconfig;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 *  Application Welcome screen presenting the corporation logo, Scoreboard Configuration title,
 *  and a button allowing the user to scan for Bluetooth devices at their convenience
 */
public class MainActivity extends AppCompatActivity {
    private TextView statusView;
    private Button button;
    private boolean connecting = false;
    private boolean connected = false;
    private ProgressBar loading;
    private BluetoothAdapter bluetoothAdapter;

    /**
     * This UUID is used to communicate with the scoreboard
     */
    private static final UUID SCOREBOARD_UUID = UUID.fromString("04c6093b-0000-1000-8000-00805f9b34fb");

    private final String SCOREBOARD_HOSTNAME = "scoreboard";
    private final String DEBUG_HOSTNAME = "DESKTOP-TORE5J3";
    private final String DEBUG_HOSTNAME2 = "TAPSCCXTG7H2";

    /**
     * Register the button when the app is opened
     *
     * @param savedInstanceState The saved state of the previous application run
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Get components from activity
        statusView = findViewById(R.id.statusView);
        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(getApplicationContext(), BluetoothFinderActivity.class);
                        startActivity(intent);
                    }
                },
                3000
        );
    }


}