package com.advancedsportstechnologies.scoreboardconfig;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.Arrays;

public class ConfigurationActivity extends AppCompatActivity {

    static TextView connectionView;
    static Button startMatchButton;
    RadioGroup radioGroup;
    TextView errorView;
    LinearLayout gameScoreLayout;
    EditText numGamesEditText;
    private boolean paused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_selection);

        connectionView = findViewById(R.id.connectionView);
        startMatchButton = findViewById(R.id.startMatchButton);
        radioGroup = findViewById(R.id.radioGroup);
        errorView = findViewById(R.id.errorView);
        gameScoreLayout = findViewById(R.id.gameScoresLayout);
        numGamesEditText = findViewById(R.id.numGamesEditText);
        addGameNumTextChangedListener(numGamesEditText);
    }

    public void startMatch(View view) {

        EditText teamName1EditText = findViewById(R.id.teamName1EditText);
        EditText teamName2EditText = findViewById(R.id.teamName2EditText);

        addTextChangedListener(teamName1EditText);
        addTextChangedListener(teamName2EditText);

        String numGames = numGamesEditText.getText().toString();
        String team1Name = teamName1EditText.getText().toString();
        String team2Name = teamName2EditText.getText().toString();


        boolean error = false;
        errorView.setTextColor(Color.RED);

        int[] gameScores = numGames.isEmpty() ? new int[0] : new int[Integer.parseInt(numGames)];
        if (numGames.isEmpty() || Integer.parseInt(numGames) < 0 || Integer.parseInt(numGames) > 9) {
            numGamesEditText.setBackgroundColor(Color.parseColor("#fa8072"));
            errorView.setText(R.string.invalidNumGames);
            error = true;
        }
        else if (team1Name.isEmpty()) {
            teamName1EditText.setBackgroundColor(Color.parseColor("#fa8072"));
            errorView.setText(R.string.emptyTeam1Name);error = true;
        }
        else if (team2Name.isEmpty()) {
            teamName2EditText.setBackgroundColor(Color.parseColor("#fa8072"));
            errorView.setText(R.string.emptyTeam2Name);
            error = true;
        }
        else {
            for (int i = 0; i < gameScoreLayout.getChildCount(); i++) {
                EditText gameScoreEditText = (EditText) gameScoreLayout.getChildAt(i);
                if (gameScoreEditText.getText().toString().equals("")) {
                    teamName2EditText.setBackgroundColor(Color.parseColor("#fa8072"));
                    errorView.setText(R.string.emptyGameScore);
                    error = true;
                    break;
                } else {
                    gameScores[i] = Integer.parseInt(gameScoreEditText.getText().toString());
                }
            }
        }

        if (!error) {
            String type = radioGroup.getCheckedRadioButtonId() == R.id.standardRadio ? "standard" : "switch";

            String message = "{'" +
                    "type':'" + type + "', " +
                    "'numGames':'" + numGames + "', " +
                    "'gameScores':'" + Arrays.toString(gameScores) + "', " +
                    "'team1':'" + team1Name + "', " +
                    "'team2':'" + team2Name + "'}";

            errorView.setText(R.string.sent);
            errorView.setTextColor(Color.parseColor("#228B22"));
            //TODO: Error from server???
            BluetoothFinderActivity.writeToServer(message.length());
            BluetoothFinderActivity.writeToServer(message);
        }
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) this.getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(
                this.getCurrentFocus().getWindowToken(), 0);
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

    private void addGameNumTextChangedListener(final EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                editText.setBackgroundColor(Color.TRANSPARENT);

                if (s.length() > 1) {
                    s = s.subSequence(0, 1);
                    editText.setText(s);
                }

                gameScoreLayout.removeAllViews();
                int gameNum = s.toString().isEmpty() ? 0 : Integer.parseInt(s.toString());
                for (int i = 0; i < gameNum; i++) {
                    EditText gameScoreEditText = new EditText(getApplicationContext());
                    String hint = "Game " + (i + 1) + " Win Score";
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

                    int pxLeftMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 56, getResources().getDisplayMetrics());
                    int pxTopMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());

                    lp.setMargins(pxLeftMargin, pxTopMargin, 0, 0);
                    gameScoreEditText.setLayoutParams(lp);
                    gameScoreEditText.setHint(hint);
                    gameScoreEditText.setHintTextColor(Color.GRAY);
                    gameScoreEditText.setTextColor(Color.BLACK);
                    gameScoreEditText.setTextSize(10f);
                    gameScoreEditText.setGravity(Gravity.CENTER_HORIZONTAL);
                    gameScoreEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
                    gameScoreLayout.addView(gameScoreEditText);
                }
            }
        });
    }

    private void addTextChangedListener(final EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                editText.setBackgroundColor(Color.TRANSPARENT);
            }
        });
    }
}
