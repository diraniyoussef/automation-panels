package com.youssefdirani.automation.ui;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.youssefdirani.automation.MainActivity;
import com.youssefdirani.automation.R;
import com.youssefdirani.automation.ServerConfig;
import com.youssefdirani.automation.SocketConnection;
import com.youssefdirani.automation.WiFiConnection;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

//import androidx.lifecycle.ViewModelProvider; //fine
//import androidx.lifecycle.ViewModelProviders; //deprecated so replaced by the fine one
//import androidx.lifecycle.ViewModelProvider; //fine
//import androidx.lifecycle.ViewModelProviders; //deprecated so replaced by the fine one

public class TempHumFragment extends Fragment { //in principle, this fragment represents the charcoal humidity and temperature panel.
    private String message;
    private String message_header;

    private SocketConnection localSocketConnection, internetSocketConnection;
    private Boolean isLocal, isInternet;

    private MainActivity activity;
    private View root;

    public void onPause() {
        super.onPause();
        if( localSocketConnection != null ) {
            localSocketConnection.destroyAllSockets();
        }
        if( internetSocketConnection != null ) {
            internetSocketConnection.destroyAllSockets();
        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_temphum, container, false);
        activity = (MainActivity) getActivity();
        return root;
    }
    private void messageServerWithWiFiCheck( SocketConnection socketConnection, boolean silent_wifi ) {
        if( WiFiConnection.wiFiValid( activity.getApplicationContext(), silent_wifi, activity.toasting, isLocal ) ) {
            try {
                Log.i("Youssef info..electr", "message is " + message);
                socketConnection.socketConnectionSetup(message);
            } catch (Exception e) {
                Log.i("Youssef info..electr", "Error in messageTheServer");
                e.printStackTrace();
            }
        }
    }

    private void messageServerThroughInternet( SocketConnection socketConnection ) {
        try {
            Log.i("Youssef info..electr", "message is " + message);
            socketConnection.socketConnectionSetup(message);
        } catch (Exception e) {
            Log.i("Youssef info..electr", "Error in messageTheServer");
            e.printStackTrace();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onResume() {
        super.onResume();
        final MainActivity activity = (MainActivity) getActivity();

        String panel_name;
        final String panel_type = "informing"; //either obeying or informing or empty. I do have a protection mechanism though, so it's ok if you forget it.. This variable is probably only used in ConfigPanel class.

        if (activity != null) {
            final String panel_index = "charcoal_humidity_panel";
            activity.panel_index = panel_index;
            panel_name = activity.getPanel_name();
            activity.panel_type = panel_type;

            message_header = activity.mob_Id + panel_index + ":";
            message = message_header + "R?\0";

            final SharedPreferences network_prefs = activity.getSharedPreferences(panel_index + "_networkConfig", 0);
            final String localPort1_key = "localPort1";
            final String localPort2_key = "localPort2";
            final String internetPort1_key = "internetPort1";
            final String internetPort2_key = "internetPort2";
            final String[] internetIP_key = {"internetIP0", "internetIP1", "internetIP2", "internetIP3"};

            int localPort1 = 11359, localPort2 = 11360, internetPort1 = 11359, internetPort2 = 11360;
            try {
                localPort1 = Integer.parseInt(network_prefs.getString(localPort1_key, String.valueOf( localPort1 ) ) ); //key MUST be the tag of the EditText
                localPort2 = Integer.parseInt(network_prefs.getString(localPort2_key, String.valueOf( localPort2  ) ) ); //key MUST be the tag of the EditText
                internetPort1 = Integer.parseInt(network_prefs.getString(internetPort1_key, String.valueOf( internetPort1 ) ) ); //key MUST be the tag of the EditText
                internetPort2 = Integer.parseInt(network_prefs.getString(internetPort2_key, String.valueOf( internetPort2 ) ) ); //key MUST be the tag of the EditText
            } catch (Exception e) {
                //The try catch block is necessary since Integer.parseInt to an empty string makes a fatal error
            }
            String[] internetIntermediateIP_portion = new String[4];
            internetIntermediateIP_portion[0] = network_prefs.getString( internetIP_key[0], "74" );
            internetIntermediateIP_portion[1] = network_prefs.getString( internetIP_key[1], "122" );
            internetIntermediateIP_portion[2] = network_prefs.getString( internetIP_key[2], "199" );
            internetIntermediateIP_portion[3] = network_prefs.getString( internetIP_key[3], "173" );
            /*Now, although it is weird to write the values just being read, but this is necessary for more consistency in the code.
            In particular, since we enter here before we enter ConfigPanel class, then the default value of the network preferences
            there is worthless, and this is the point. The goal is to have there the same initial values mentioned here. Automatically.
             */
            SharedPreferences.Editor prefs_editor = network_prefs.edit();
            prefs_editor.putString( localPort1_key, String.valueOf(localPort1) ).apply();
            prefs_editor.putString( localPort2_key, String.valueOf(localPort2) ).apply();
            prefs_editor.putString( internetPort1_key, String.valueOf(internetPort1) ).apply();
            prefs_editor.putString( internetPort2_key, String.valueOf(internetPort2) ).apply();
            prefs_editor.putString( internetIP_key[0], internetIntermediateIP_portion[0] ).apply();
            prefs_editor.putString( internetIP_key[1], internetIntermediateIP_portion[1] ).apply();
            prefs_editor.putString( internetIP_key[2], internetIntermediateIP_portion[2] ).apply();
            prefs_editor.putString( internetIP_key[3], internetIntermediateIP_portion[3] ).apply();

            final String localStaticIP = network_prefs.getString("staticIP", "192.168.1.215"); //"192.168.1.215" is a dummy value
            final String internetIntermediateIP = internetIntermediateIP_portion[0] + "." +
                    internetIntermediateIP_portion[1] + "." + internetIntermediateIP_portion[2] + "." +
                    internetIntermediateIP_portion[3];

            ServerConfig localServerConfig = new ServerConfig( panel_index, panel_name, localStaticIP, localPort1, localPort2 );
            ServerConfig internetServerConfig = new ServerConfig( panel_index, panel_name, internetIntermediateIP, internetPort1, internetPort2 );

            isLocal = network_prefs.getBoolean("local", true); //this info is written in ConfigPanel where we determine whether the panel supports local communication
            isInternet = network_prefs.getBoolean("internet", true); //same here.

            /*You might argue that isn't it better to put this code in MainActivity class instead of it being
             * redundant in each fragment ? Well you got a point in that.
             * Same goes for the refresh button.
             */
            if( activity.localInternet_toggleButton != null ) {
                if( !isLocal || !isInternet ) {
                    if( isLocal ) {
                        activity.localInternet_toggleButton.setChecked(false);
                        activity.localInternet_toggleButton.setVisibility(View.VISIBLE);
                        activity.localInternet_toggleButton.setEnabled(false);
                    } else if( isInternet ) {
                        activity.localInternet_toggleButton.setChecked(true);
                        activity.localInternet_toggleButton.setVisibility(View.VISIBLE);
                        activity.localInternet_toggleButton.setEnabled(false);
                    } else {
                        activity.localInternet_toggleButton.setVisibility(View.GONE);
                    }
                } else {
                    activity.localInternet_toggleButton.setChecked(false); //meaning local. I chose Local as a convention. Nothing special.
                    activity.localInternet_toggleButton.setEnabled(true);
                    activity.localInternet_toggleButton.setVisibility(View.VISIBLE);
                }
            }
            if( isLocal ) {
                localSocketConnection = new SocketConnection(activity.toasting, false, activity,
                        activity.getApplicationContext(), localServerConfig, 2,
                        true, activity.owner_part, activity.mob_part, activity.mob_Id);
            }
            if( isInternet ) {
                internetSocketConnection = new SocketConnection(activity.toasting, false, activity,
                        activity.getApplicationContext(), internetServerConfig, 2,
                        false, activity.owner_part, activity.mob_part, activity.mob_Id);
            }

            sendMessageAccordingToToggleButton( true );

            //Button refreshButton = activity.findViewById(R.id.buttonHumTemperature_refresh); //doesn't work when you go back to this fragment. And this is logical since the refresh button belongs to the fragment and not the activity
            Button refreshButton = root.findViewById(R.id.refresh_button);
            refreshButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    message = message_header + "R?\0";
                    sendMessageAccordingToToggleButton( false );
                    if( !isLocal && !isInternet ) {
                        activity.toasting.toast("يبدو أنّ إعدادات الإتصال (شبكة داخلية أو إنترنت) للوحة غير مناسبة !" +
                                        "\n" +
                                        "يرجى التحقق من " +
                                        getString( R.string.network_configuration ),
                                Toast.LENGTH_LONG);
                    }
                }
            });

        }

    }

    private void sendMessageAccordingToToggleButton( boolean silentWiFi ) {
        if( activity.localInternet_toggleButton != null &&
                activity.localInternet_toggleButton.getVisibility() == View.VISIBLE  &&
                activity.localInternet_toggleButton.isEnabled() ) { //user has the choice to select whichever he desires.
            if( !activity.localInternet_toggleButton.isChecked() ) {
                messageServerWithWiFiCheck( localSocketConnection, silentWiFi );
            } else {
                messageServerThroughInternet( internetSocketConnection );
            }
        } else { //isLocal and isInternet cannot be both true. localInternet_toggleButton won't be null.
            //BTW, must keep them loose, i.e. don't use "else if" because user might go to network configuration and unselect both.
            if( isLocal && activity.localInternet_toggleButton != null && !activity.localInternet_toggleButton.isChecked() ) {
                messageServerWithWiFiCheck( localSocketConnection, silentWiFi );
            }
            if( isInternet && activity.localInternet_toggleButton != null && activity.localInternet_toggleButton.isChecked() ) {
                messageServerThroughInternet( internetSocketConnection );
            }
        }
    }
}