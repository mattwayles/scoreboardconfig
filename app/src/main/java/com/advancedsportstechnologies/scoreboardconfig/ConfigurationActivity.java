package com.advancedsportstechnologies.scoreboardconfig;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class ConfigurationActivity extends AppCompatActivity {

    static TextView connectionView;
    static Button startMatchButton;
    private boolean paused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_selection);

        connectionView = findViewById(R.id.connectionView);
        startMatchButton = findViewById(R.id.startMatchButton);

    }

    public void startMatch(View view) {
        EditText teamName1EditText = findViewById(R.id.teamName1EditText);
        EditText teamName2EditText = findViewById(R.id.teamName2EditText);
        String team1Name = teamName1EditText.getText().toString();
        String team2Name = teamName2EditText.getText().toString();

        String message = team1Name + "/" + team2Name;

        BluetoothFinderActivity.writeToServer(message);
        BluetoothFinderActivity.writeToServer("|");
    }

    @Override
    protected void onPause() {
        super.onPause();

        BluetoothFinderActivity.closeSocket();
        paused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (paused) {
            Intent intent = new Intent(getApplicationContext(), BluetoothFinderActivity.class);
            startActivity(intent);
            paused = false;
        }
    }
}
