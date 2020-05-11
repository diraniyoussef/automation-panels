package com.youssefdirani.automation;

import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;

class PeriodicCheck_Data{
    int timeout; //in ms
    int divisionInterval;
    int maxIterations;
    PeriodicCheck_Data(int divisionInterval_arg, int timeout_arg){
        divisionInterval = divisionInterval_arg;
        timeout = timeout_arg;
        maxIterations = (timeout/divisionInterval) + 1;
    }
}


/*
class ActionThread{ //Instance info are set only at initialization time, not at a later time again.
    private Activity activity;

    public ActionThread (Activity act){
        activity = act;
    }

    protected void enableSwitches(SocketConnection aSocketConnection){
        final SocketConnection a_socket_connection = aSocketConnection;
        activity.runOnUiThread(new Runnable() {
            public void run() {
                a_socket_connection.enableSwitches();
            }
        });
    }
    protected void disableSwitches(SocketConnection aSocketConnection){
        final SocketConnection a_socket_connection = aSocketConnection;
        activity.runOnUiThread(new Runnable() {
            public void run() {
                a_socket_connection.disableSwitches();
            }
        });
    }
}
*/

class PrefsTextWatcher implements TextWatcher //this tracks the made edit char by char, which is not the best behavior really.
{
    //protected static SharedPreferences prefs;
/*    private Button mainActi_P1_Button, mainActi_P2_Button, mainActi_P3_Button;
    private EditText mainActi_P1_EditText, mainActi_P2_EditText, mainActi_P3_EditText;
*/
    private transient EditText editText = null;
    private String keyString;
    private SharedPreferences prefs;

    PrefsTextWatcher(SharedPreferences prefs_arg, EditText editText, String keyString)
    //PrefsTextWatcher(SharedPreferences prefs_arg, EditText editText, String keyString, Button mainActi_P1_Button, Button mainActi_P2_Button, Button mainActi_P3_Button) //constructor
            //Preferences is a class I made just to make it easy to access mPrefs.
    {
        super();
/*
        this.mainActi_P1_Button = mainActi_P1_Button;
        this.mainActi_P2_Button = mainActi_P2_Button;
        this.mainActi_P3_Button = mainActi_P3_Button;
*/
        prefs = prefs_arg;
        this.editText = editText;
        this.keyString = keyString;
    }

    @Override
    public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
    {

    }

    @Override
    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
    {

    }

    @Override
    public void afterTextChanged(Editable arg0)
    {
        //save in the respective shared preferences.
        SharedPreferences.Editor mEditor = prefs.edit();
        mEditor.putString( keyString, editText.getText().toString() ).apply();
        Log.i("Youssef -Generic.java", "change made to preferences.");
        /*
        //manage behavior for buttons and edit texts in MainActivity
        if (editText.equals(mainActi_P1_EditText)){
            mainActi_P1_Button.setText(editText.getText().toString());
            mainActi_P1_Button.setVisibility(View.VISIBLE);
            Log.i("Youssef -Generic.java", "PrefsTextWatcher - showing again button 1.");
        }
        if (editText.equals(mainActi_P2_EditText)){
            mainActi_P2_Button.setText(editText.getText().toString());
            mainActi_P2_Button.setVisibility(View.VISIBLE);
        }
        if (editText.equals(mainActi_P3_EditText)){
            mainActi_P3_Button.setText(editText.getText().toString());
            mainActi_P3_Button.setVisibility(View.VISIBLE);
        }
        */
    }
}