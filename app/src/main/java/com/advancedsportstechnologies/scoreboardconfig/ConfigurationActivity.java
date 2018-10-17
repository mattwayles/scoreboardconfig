package com.advancedsportstechnologies.scoreboardconfig;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

/**
 * This activity provides all of the required labels & textfields to actually configure the connected scoreboard.
 * If the phone sleeps while on this activity, the Bluetooth socket is disconnected and the user is returned
 * to the previous activity when resumed. This is to minimize client/server communication problems and save
 * data/battery life
 */
public class ConfigurationActivity extends AppCompatActivity {

    /**
     * Register all the view objects you'll need
     */
    private TextView statusView;
    private RadioGroup radioGroup;
    private EditText numGamesEditText;
    private LinearLayout gameScoreLayout;
    private SharedPreferences sharedPrefs;
    private EditText gamesNeededToWinEditText;
    EditText teamName1EditText;
    EditText teamName2EditText;

    private boolean paused = false;
    private final int MAX_SCORE = 99;

    /**
     * On activity creation, set up the environment
     * @param savedInstanceState    The previous application state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_selection);

        //Hide the keyboard when the background is touched
        LinearLayout background = findViewById(R.id.background);
        background.setOnTouchListener(hideKeyboard());

        //Initialize objects
        initializeViews();

        //Set intial text values from sharedPrefs
        setInitialValues();

        addTextChangedListeners();
    }

    public void startMatch(View view) {

        //Retrieve user input
        String numGames = numGamesEditText.getText().toString();
        String gamesToWin = gamesNeededToWinEditText.getText().toString();
        String team1Name = teamName1EditText.getText().toString();
        String team2Name = teamName2EditText.getText().toString();

        //Initialize error handling
        statusView.setTextColor(Color.RED);
        statusView.setAlpha(1);

        //Initialize Game Win Scores
        int[] gameScores = numGames.isEmpty() ? new int[0] : new int[Integer.parseInt(numGames)];

        //Validate input
        if (inputIsValid(numGames, gamesToWin, team1Name, team2Name, gameScores)) {

            //Retrieve scoreboard type
            String type = radioGroup.getCheckedRadioButtonId() == R.id.standardRadio ? "standard" : "switch";

            //Parse String gameScore values
            String gameScoreValueStr = parseGameScores(gameScores);

            //Add user input to Shared Preferences for retrieval on next run
            sharedPrefs.edit()
                    .putString("type", type)
                    .putString("team1Name", team1Name)
                    .putString("team2Name", team2Name)
                    .putString("numGames", numGames)
                    .putString("gameScores", gameScoreValueStr)
                    .putString("gamesToWin", gamesToWin)
                    .apply();


            //Formulate JSON message to send to server
            String message = "{" +
                    "type:" + type + ", " +
                    "numGames:" + numGames + ", " +
                    "gamesToWin:" + gamesToWin + ", " +
                    "gameScores:" + gameScoreValueStr + ", " +
                    "team1:" + team1Name + ", " +
                    "team2:" + team2Name + "}";

            //Write data to connected Bluetooth device
            statusView.setText(R.string.sent);
            statusView.setTextColor(Color.parseColor("#005A00"));
            statusView.animate().alpha(0).setDuration(5000);

            //TODO: Return error message if connection fails?

            BluetoothFinderActivity.writeToServer(message.length());
            BluetoothFinderActivity.writeToServer(message);
        }
    }

    /**
     * When app pauses, close the Bluetooth connection to save battery life
     */
    @Override
    protected void onPause() {
        super.onPause();

        BluetoothFinderActivity.closeSocket();
        paused = true;
    }

    /**
     * When app opens back up after pausing, return to the previous page to restore Bluetooth connection
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (paused) {
            Intent intent = new Intent(getApplicationContext(), BluetoothFinderActivity.class);
            startActivity(intent);
            paused = false;
        }
    }

    /**
     * Attach allocated Java objects to the activity View elements to retrieve & modify data
     */
    private void initializeViews() {
        sharedPrefs = this.getSharedPreferences("com.advancedsportstechnologies.scoreboardconfig",
                Context.MODE_PRIVATE);

        radioGroup = findViewById(R.id.radioGroup);
        statusView = findViewById(R.id.statusView);
        gameScoreLayout = findViewById(R.id.gameScoresLayout);
        numGamesEditText = findViewById(R.id.numGamesEditText);
        teamName1EditText = findViewById(R.id.teamName1EditText);
        teamName2EditText = findViewById(R.id.teamName2EditText);
        gamesNeededToWinEditText = findViewById(R.id.gamesToWinEditText);
    }

    /**
     * If app has been used before, retrieve the values used last time
     */
    private void setInitialValues() {
        teamName1EditText.setText(sharedPrefs.getString("team1Name", ""));
        teamName2EditText.setText(sharedPrefs.getString("team2Name", ""));
        numGamesEditText.setText(sharedPrefs.getString("numGames", "3"));
        gamesNeededToWinEditText.setText(sharedPrefs.getString("gamesToWin", "2"));

        renderGameScoreEditViews();
    }

    /**
     * Add text change listeners to perform operations to protect against invalid input
     */
    private void addTextChangedListeners() {
        addGameNumTextChangedListener(numGamesEditText);
        addGameNumTextChangedListener(gamesNeededToWinEditText);

        addTextChangedListener(teamName1EditText);
        addTextChangedListener(teamName2EditText);
    }

    /**
     * Validate user input
     * @param numGames  The Number of Games user input
     * @param gamesToWin    The Games Needed To Win user input
     * @param team1Name The Team 1 name user input
     * @param team2Name The team 2 name user input
     * @param gameScores    The list of gameScores user input
     * @return  Boolean value indicating the validity of the input
     */
    private boolean inputIsValid(String numGames, String gamesToWin, String team1Name, String team2Name, int[] gameScores) {
        if (numGames.isEmpty() || Integer.parseInt(numGames) < 0 || Integer.parseInt(numGames) > 9) {
            numGamesEditText.setBackgroundColor(Color.parseColor("#fa8072"));
            statusView.setText(R.string.invalidNumGames);
            return false;
        }
        else if (gamesToWin.isEmpty()) {
            gamesNeededToWinEditText.setBackgroundColor(Color.parseColor("#fa8072"));
            statusView.setText(R.string.invalidGamesToWin);
            return false;
        }
        else if (Integer.parseInt(gamesToWin) > Integer.parseInt(numGames)) {
            numGamesEditText.setBackgroundColor(Color.parseColor("#fa8072"));
            statusView.setText(R.string.invalidGamesToWin2);
            return false;
        }
        else if (team1Name.isEmpty()) {
            teamName1EditText.setBackgroundColor(Color.parseColor("#fa8072"));
            statusView.setText(R.string.emptyTeam1Name);
            return false;
        }
        else if (team2Name.isEmpty()) {
            teamName2EditText.setBackgroundColor(Color.parseColor("#fa8072"));
            statusView.setText(R.string.emptyTeam2Name);
            return false;
        }
        else {
            for (int i = 0; i < gameScoreLayout.getChildCount(); i++) {
                EditText gameScoreEditText = (EditText) gameScoreLayout.getChildAt(i);
                if (gameScoreEditText.getText().toString().equals("")) {
                    gameScoreEditText.setBackgroundColor(Color.parseColor("#fa8072"));
                    statusView.setText(R.string.emptyGameScore);
                    return false;
                } else {
                    gameScores[i] = Integer.parseInt(gameScoreEditText.getText().toString());
                }
            }
        }
        return true;
    }

    /**
     * Parse the array of integers representing user-input game scores and return a /-separated string
     * @param gameScores    The int[] user input representing game scores
     * @return  A String of game scores separated by /
     */
    private String parseGameScores(int[] gameScores) {
        StringBuilder gameScoreBuilder = new StringBuilder();
        for (int i = 0; i < gameScores.length; i++) {
            if (i != gameScores.length - 1) {
                gameScoreBuilder.append(gameScores[i]).append("-");
            } else {
                gameScoreBuilder.append(gameScores[i]);
            }
        }
        return gameScoreBuilder.toString();
    }

    /**
     * GameScoreEditViews are rendered based on the user input in the Number of Games text box.
     * This method takes the user input and renders the appropriate number of formatted EditText views.
     */
    private void renderGameScoreEditViews() {

        //Remove previous views
        gameScoreLayout.removeAllViews();

        //Retrieve number of views to be created
        int gameNum = numGamesEditText.getText().toString().isEmpty() ? 0 : Integer.parseInt(numGamesEditText.getText().toString());

        for (int i = 0; i < gameNum; i++) {
            //Create the view
            EditText gameScoreEditText = new EditText(getApplicationContext());

            //Format view text properties
            String hint = "Game " + (i + 1) + " Win Score";
            String scoreStr = sharedPrefs.getString("gameScores", "");
            String[] scores = scoreStr != null && !scoreStr.isEmpty() ? scoreStr.split("-") : null;
            if (scores != null){
                if (i < scores.length) {
                    gameScoreEditText.setText(scores[i]);
                }
            }

            //Format view layout parameters
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);

            int pxLeftMargin = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 56, getResources().getDisplayMetrics());
            int pxTopMargin = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());

            lp.setMargins(pxLeftMargin, pxTopMargin, 0, 0);

            //Set view properties
            gameScoreEditText.setHint(hint);
            gameScoreEditText.setTextSize(10f);
            gameScoreEditText.setLayoutParams(lp);
            gameScoreEditText.setTextColor(Color.BLACK);
            gameScoreEditText.setHintTextColor(Color.GRAY);
            gameScoreEditText.setGravity(Gravity.CENTER_HORIZONTAL);
            gameScoreEditText.setInputType(InputType.TYPE_CLASS_NUMBER);

            //Add Text Listeners to view
            addGameScoreTextChangeListener(gameScoreEditText);
            //Add view to layout
            gameScoreLayout.addView(gameScoreEditText);
        }
    }

    /**
     * GameScoreViews must contain a value lower than MAX_SCORE. On text change, validate that the
     * value is within the requried parameters
     * @param editText The view being modified
     */
    private void addGameScoreTextChangeListener(final EditText editText) {
    editText.addTextChangedListener(new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            int gameScore = s.toString().isEmpty() ? 0 : Integer.parseInt(s.toString());
            if (gameScore > MAX_SCORE) {
                s = s.subSequence(0, s.length() - 1);
                editText.setText(s);
            }
            editText.setTextColor(Color.BLACK);
        }

        //Not used, but required to be here
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        //Not used, but required to be here
        @Override
        public void afterTextChanged(Editable s) { }
    });
    }


    /**
     * When the user enters text into the Number Of Games textbox, render the appropriate number of EditText Views
     *
     * Number of Games can hold a single, numerical character.
     *
     * @param editText  The EditText view holding the Number of Games variable
     */
    private void addGameNumTextChangedListener(final EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                editText.setBackgroundColor(Color.TRANSPARENT);

                if (s.length() > 1) {
                    s = s.subSequence(0, 1);
                    editText.setText(s);
                }

                if (editText.getId() == R.id.numGamesEditText) {
                    renderGameScoreEditViews();
                }
            }

            //Not used, but required to be here
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            //Not used, but required to be here
            @Override
            public void afterTextChanged(Editable s) { }


        });
    }

    /**
     * For all other editText views, reset their background color on change to clear error colors
     * @param editText  The editText being edited
     */
    private void addTextChangedListener(final EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                editText.setBackgroundColor(Color.TRANSPARENT);
                if (s.toString().contains(",")) {
                    editText.setText(s.toString().replace(",",""));
                }
                else if (s.toString().contains(":")) {
                    editText.setText(s.toString().replace(":",""));
                }
            }

            //Not used, but required to be here
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            //Not used, but required to be here
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Hide the keyboard when the background is pressed
     * @return  An onTouchListener to apply to the LinearLayout
     */
    private View.OnTouchListener hideKeyboard() {
        return new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (getCurrentFocus() != null) {
                    imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                }
                return true;
            }
        };
    }
}
