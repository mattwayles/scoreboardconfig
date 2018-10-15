package com.advancedsportstechnologies.scoreboardconfig;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

public class ConfigurationActivity extends AppCompatActivity {

    static TextView connectionView;
    static Button startMatchButton;
    RadioGroup radioGroup;
    private boolean paused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_selection);

        connectionView = findViewById(R.id.connectionView);
        startMatchButton = findViewById(R.id.startMatchButton);
        radioGroup = findViewById(R.id.radioGroup);

    }

    public void startMatch(View view) {
        EditText teamName1EditText = findViewById(R.id.teamName1EditText);
        EditText teamName2EditText = findViewById(R.id.teamName2EditText);

        String team1Name = teamName1EditText.getText().toString();
        String team2Name = teamName2EditText.getText().toString();


        //TODO: IF either is empty, throw an error!

        String type = radioGroup.getCheckedRadioButtonId() == R.id.standardRadio ? "standard" : "switch";

        String message = "{'type':'" + type + "', 'team1':'" + team1Name + "', 'team2':'" + team2Name + "'}";



        BluetoothFinderActivity.writeToServer(message.length());
        BluetoothFinderActivity.writeToServer(message);
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
