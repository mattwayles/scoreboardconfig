package com.advancedsportstechnologies.scoreboardconfig;

import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class ControlActivity extends AppCompatActivity {

    private TextView teamOneNameLabel;
    private TextView teamTwoNameLabel;
    private String teamOneName;
    private String teamTwoName;
    private Vibrator vibe;

    private int team1GamesWon = 0;
    private int team2GamesWon = 0;
    private int team1Score = 0;
    private int team2Score = 0;
    private int currentGame = 0;
    private int currentGameScore;
    private int gamesToWin;
    private String gameScores;
    private boolean winByTwo;
    private String gameType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        teamOneNameLabel = findViewById(R.id.teamOneNameLabel);
        teamOneName = getIntent().getStringExtra("teamOneName");
        teamOneNameLabel.setText(teamOneName);

        teamTwoNameLabel = findViewById(R.id.teamTwoNameLabel);
        teamTwoName = getIntent().getStringExtra("teamTwoName");
        teamTwoNameLabel.setText(teamTwoName);

        gameType = getIntent().getStringExtra("type");
        winByTwo = getIntent().getBooleanExtra("winByTwo", false);
        gameScores = getIntent().getStringExtra("gameScores");
        gamesToWin = getIntent().getIntExtra("gamesToWin", 1);
        currentGameScore = Integer.parseInt(gameScores.split("-")[0]);
        vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

    }

    public void configure(View view) {
        vibe.vibrate(40);
        Intent intent = new Intent(getApplicationContext(), ConfigurationActivity.class);
        startActivity(intent);
    }

    public void reset(View view) {
        vibe.vibrate(40);
        team1Score = 0;
        team2Score = 0;
        team1GamesWon = 0;
        team2GamesWon = 0;
        currentGame = 0;
        sendMessage("reset");
    }

    public void swap(View view) {
        swap();
    }

    private void swap() {
        String temp = teamOneName;
        teamOneName = teamTwoName;
        teamTwoName = temp;

        int tempWon = team1GamesWon;
        team1GamesWon = team2GamesWon;
        team2GamesWon = tempWon;

        int tempScore = team1Score;
        team1Score = team2Score;
        team2Score = tempScore;

        teamOneNameLabel.setText(teamOneName);
        teamTwoNameLabel.setText(teamTwoName);
        vibe.vibrate(40);
    }

    public void teamOnePlus(View view) {
        if (team1Score < currentGameScore) {
            team1Score++;
            sendMessage("increase", teamOneName);
        }
    }

    public void teamOneMinus(View view) {
        team1Score--;
        sendMessage("decrease", teamOneName);
    }

    public void teamTwoPlus(View view) {
        if (team2Score < currentGameScore) {
            team2Score++;
            sendMessage("increase", teamTwoName);
        }
    }

    public void teamTwoMinus(View view) {
        team2Score--;
        sendMessage("decrease", teamTwoName);
    }

    private void sendMessage(String type, String name) {
        vibe.vibrate(80);
        String message = "{type: " + type + ", teamName: " + name + "}";
        BluetoothFinderActivity.writeToServer(message.length());
        BluetoothFinderActivity.writeToServer(message);

        if (gameType.equals("switch")) {
            checkScores();
        }
    }

    private void sendMessage(String type) {
        vibe.vibrate(80);
        String message = "{type: " + type + "}";
        BluetoothFinderActivity.writeToServer(message.length());
        BluetoothFinderActivity.writeToServer(message);
    }

    private void checkScores() {
        try {
            currentGameScore = Integer.parseInt(gameScores.split("-")[currentGame]);

            if (winByTwo) {
                if ((team1Score >= currentGameScore && team1Score > team2Score + 1) ||
                        team2Score >= currentGameScore && team2Score > team1Score + 1) {
                    if (team1Score >= currentGameScore && team1Score > team2Score + 1) {
                        team1GamesWon++;
                    } else {
                        team2GamesWon++;
                    }
                    currentGame += 1;
                    team1Score = 0;
                    team2Score = 0;
                    if (team1GamesWon < gamesToWin && team2GamesWon < gamesToWin) {
                        swap();
                    }
                }
            } else {
                if (team1Score == currentGameScore || team2Score == currentGameScore) {
                    if (team1Score == currentGameScore) {
                        team1GamesWon++;
                    } else
                        team2GamesWon++;
                    currentGame += 1;
                    team1Score = 0;
                    team2Score = 0;
                    if (team1GamesWon < gamesToWin && team2GamesWon < gamesToWin) {
                        swap();
                    }
                }
            }
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
    }
}
