package com.advancedsportstechnologies.scoreboardconfig;

import android.content.Context;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

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
    private ImageView swapImageView;
    private CheckBox winByTwoCheckbox;
    private Spinner themeSpinner;
    private Spinner matchTypeSpinner;
    private RadioGroup radioGroup;
    private LinearLayout gameScoreLayout;
    private SharedPreferences sharedPrefs;
    private EditText teamName1EditText;
    private EditText teamName2EditText;
    private List<String> matchTypes = new ArrayList<>();

    private boolean swap;
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

    public void configMatch(View view) {

        //Retrieve user input
        String team1Name = teamName1EditText.getText().toString();
        String team2Name = teamName2EditText.getText().toString();
        String theme = themeSpinner.getSelectedItem().toString().toLowerCase().replaceAll("\\s","");
        String matchType = matchTypeSpinner.getSelectedItem().toString();
        int numGames = matchType.equals("Single") ? 1 : Integer.parseInt(matchType.substring(matchType.length() - 1));
        boolean winByTwo = winByTwoCheckbox.isChecked();

        //Initialize error handling
        statusView.setTextColor(Color.RED);
        statusView.setAlpha(1);

        //Initialize Game Win Scores
        int[] gameScores = new int[numGames];

        //Validate input
        if (inputIsValid(team1Name, team2Name, gameScores)) {
            //Enable Update button
            swapImageView.setVisibility(View.VISIBLE);

            //Retrieve scoreboard type
            String type = radioGroup.getCheckedRadioButtonId() == R.id.standardRadio ? "standard" : "switch";

            //Parse String gameScore values
            String gameScoreValueStr = parseGameScores(gameScores);

            //Set Games to Win Value
            int gamesToWin = numGames == 1 ? 1 : numGames / 2 + 1;

            //Add user input to Shared Preferences for retrieval on next run
            sharedPrefs.edit()
                    .putString("type", type)
                    .putString("team1Name", team1Name)
                    .putString("team2Name", team2Name)
                    .putString("gameScores", gameScoreValueStr)
                    .putBoolean("winByTwo", winByTwo)
                    .putInt("numGames", matchTypeSpinner.getSelectedItemPosition())
                    .putInt("themePos", themeSpinner.getSelectedItemPosition())
                    .apply();

            //Formulate JSON message to send to server
            String message = "{" +
                    "type:" + type + ", " +
                    "theme:" + theme + ", " +
                    "numGames:" + numGames + ", " +
                    "gamesToWin:" + gamesToWin + ", " +
                    "gameScores:" + gameScoreValueStr + ", " +
                    "winByTwo:" + winByTwo + ", " +
                    "team1:" + team1Name + ", " +
                    "team2:" + team2Name + "}";

            System.out.println(message);

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
     * Swap team names when "Swap Teams" button is pressed
     */
    public void swapTeams(View view) {
        String editText = teamName1EditText.getText().toString();
        teamName1EditText.setText(teamName2EditText.getText());
        teamName2EditText.setText(editText);
        swap = !swap;
    }

    /**
     * Attach allocated Java objects to the activity View elements to retrieve & modify data
     */
    private void initializeViews() {
        sharedPrefs = this.getSharedPreferences("com.advancedsportstechnologies.scoreboardconfig",
                Context.MODE_PRIVATE);

        setMatchTypes();
        swapImageView = findViewById(R.id.swapImageView);
        radioGroup = findViewById(R.id.radioGroup);
        winByTwoCheckbox = findViewById(R.id.winByTwoCheckbox);
        statusView = findViewById(R.id.statusView);
        themeSpinner = findViewById(R.id.themeSpinner);
        matchTypeSpinner = findViewById(R.id.matchTypeSpinner);
        gameScoreLayout = findViewById(R.id.gameScoresLayout);
        teamName1EditText = findViewById(R.id.teamName1EditText);
        teamName2EditText = findViewById(R.id.teamName2EditText);
        ArrayAdapter<String> themeSpinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, getThemes());
        ArrayAdapter<String> matchTypeSpinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, matchTypes);

        themeSpinner.setAdapter(themeSpinnerAdapter);

        matchTypeSpinner.setAdapter(matchTypeSpinnerAdapter);
        matchTypeSpinner.setOnItemSelectedListener(matchTypeSelection());
    }

    /**
     * Set the list of available themes
     * @return  An ArrayList containing available themes
     */
    private ArrayList<String> getThemes() {
        //TODO: In future versions, the app should retrieve the available themes through the BT socket
        ArrayList<String> themes = new ArrayList<>();
        themes.add("Dark");
        themes.add("Glow");
        themes.add("Retro");
        themes.add("Sky Zone");
        themes.add("Traditional");

        return themes;
    }

    /**
     * Set the list of available match types
     */
    private void setMatchTypes() {
        matchTypes.add("Single");
        matchTypes.add("Best of 3");
        matchTypes.add("Best of 5");
        matchTypes.add("Best of 7");
        matchTypes.add("Best of 9");
    }

    /**
     * If app has been used before, retrieve the values used last time
     */
    private void setInitialValues() {
        teamName1EditText.setText(sharedPrefs.getString("team1Name", ""));
        teamName2EditText.setText(sharedPrefs.getString("team2Name", ""));
        themeSpinner.setSelection(sharedPrefs.getInt("themePos", 2));
        matchTypeSpinner.setSelection(sharedPrefs.getInt("numGames", 0));
        winByTwoCheckbox.setChecked(sharedPrefs.getBoolean("winByTwo", true));

        renderGameScoreEditViews(1);
    }

    /**
     * Add text change listeners to perform operations to protect against invalid input
     */
    private void addTextChangedListeners() {
        addTextChangedListener(teamName1EditText);
        addTextChangedListener(teamName2EditText);
    }

    /**
     * Validate user input
     * @param team1Name The Team 1 name user input
     * @param team2Name The team 2 name user input
     * @param gameScores    The list of gameScores user input
     * @return  Boolean value indicating the validity of the input
     */
    private boolean inputIsValid(String team1Name, String team2Name, int[] gameScores) {
        if (team1Name.isEmpty()) {
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
            gameScoreBuilder.append(gameScores[i]).append("-");
        }
        return gameScoreBuilder.toString();
    }

    /**
     * GameScoreEditViews are rendered based on the user input in the Number of Games text box.
     * This method takes the user input and renders the appropriate number of formatted EditText views.
     */
    private void renderGameScoreEditViews(int gameNum) {

        //Remove previous views
        gameScoreLayout.removeAllViews();

        for (int i = 0; i < gameNum; i++) {
            //Create the view
            EditText gameScoreEditText = new EditText(getApplicationContext());

            //Format view text properties
            String hint = "Game " + (i + 1);
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
            gameScoreEditText.setTextSize(18f);
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
     * When a user selects a match type, render the appropriate number of EditText Views
     * @return An OnItemSelectedListener for the MatchType spinner
     */
    private AdapterView.OnItemSelectedListener matchTypeSelection() {
        return new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (matchTypes.get(position).equals("Single")) {
                    renderGameScoreEditViews(1);
                } else {
                    String selection = matchTypes.get(position);
                    int gameNum = Integer.parseInt(selection.substring(selection.length() - 1));
                    renderGameScoreEditViews(gameNum);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        };
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
