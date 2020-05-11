package com.youssefdirani.automation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

//import java.io.IOException;
//import java.io.PrintWriter;

public class ConfigPanel extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    private static final int IP_Portions_Number = 4;
    private static final int MAC_Portions_Number = 6;
    private EditText SSID_EditText, Password_EditText, localPort1_EditText,
            localPort2_EditText, internetPort1_EditText, internetPort2_EditText;
    private EditText[] obeyingIP0_EditText = new EditText[ IP_Portions_Number ],
            obeyingIP1_EditText = new EditText[ IP_Portions_Number ],
            IP_EditText = new EditText[ IP_Portions_Number ],
            gatewayIP_EditText = new EditText[ IP_Portions_Number ],
            subnet_EditText = new EditText[ IP_Portions_Number ],
            internetIP_EditText = new EditText[ IP_Portions_Number ],
            Mac_EditText = new EditText[ MAC_Portions_Number ];
    private Editable editable;
    private EditText editText;
    private volatile String obeyingIP_message = "";
    private String panel_index, panel_name, original_panel_index; //original_panel_index is made because of "unrelate mobile to panel" thing. It's a way to solve things.
    //volatile private byte sent_messages_number = 0;
    volatile private byte max_sent_messages_number = 0;

    private SharedPreferences network_prefs;
    private SharedPreferences.Editor prefs_editor;

    @Override
    protected void onStart(){
        super.onStart();
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onResume() {
        super.onResume();

        final Bundle bundle = getIntent().getExtras();
        if( bundle == null ) {
            finish();
            return;
        }
        panel_index = bundle.getString("panelIndex");
        if( panel_index == null || //by mistake if happened.
                panel_index.equals( "" ) ) { //by mistake if happened.
            panel_index = "";
        }
        original_panel_index = panel_index;
        //Log.i("Youssef ConfigPanel", "now about to setContentView");
        setContentView(R.layout.config_panel);
        obeyingIP0_EditText[0] = findViewById(R.id.Config_editText_obey0IP0);
        obeyingIP0_EditText[1] = findViewById(R.id.Config_editText_obey0IP1);
        obeyingIP0_EditText[2] = findViewById(R.id.Config_editText_obey0IP2);
        obeyingIP0_EditText[3] = findViewById(R.id.Config_editText_obey0IP3);
        obeyingIP1_EditText[0] = findViewById(R.id.Config_editText_obey1IP0);
        obeyingIP1_EditText[1] = findViewById(R.id.Config_editText_obey1IP1);
        obeyingIP1_EditText[2] = findViewById(R.id.Config_editText_obey1IP2);
        obeyingIP1_EditText[3] = findViewById(R.id.Config_editText_obey1IP3);
        SSID_EditText = findViewById(R.id.Config_editText_SSID);
        Password_EditText = findViewById(R.id.Config_editText_Password);
        IP_EditText[0] = findViewById(R.id.Config_editText_staticIP0);
        IP_EditText[1] = findViewById(R.id.Config_editText_staticIP1);
        IP_EditText[2] = findViewById(R.id.Config_editText_staticIP2);
        IP_EditText[3] = findViewById(R.id.Config_editText_staticIP3);
        gatewayIP_EditText[0] = findViewById(R.id.Config_editText_gatewayIP0);
        gatewayIP_EditText[1] = findViewById(R.id.Config_editText_gatewayIP1);
        gatewayIP_EditText[2] = findViewById(R.id.Config_editText_gatewayIP2);
        gatewayIP_EditText[3] = findViewById(R.id.Config_editText_gatewayIP3);
        subnet_EditText[0] = findViewById(R.id.Config_editText_subnet0);
        subnet_EditText[1] = findViewById(R.id.Config_editText_subnet1);
        subnet_EditText[2] = findViewById(R.id.Config_editText_subnet2);
        subnet_EditText[3] = findViewById(R.id.Config_editText_subnet3);
        Mac_EditText[0] = findViewById(R.id.Config_editText_MAC0);
        Mac_EditText[1] = findViewById(R.id.Config_editText_MAC1);
        Mac_EditText[2] = findViewById(R.id.Config_editText_MAC2);
        Mac_EditText[3] = findViewById(R.id.Config_editText_MAC3);
        Mac_EditText[4] = findViewById(R.id.Config_editText_MAC4);
        Mac_EditText[5] = findViewById(R.id.Config_editText_MAC5);
        //Now admin section
        internetIP_EditText[0] = findViewById(R.id.Config_editText_internetIP0);
        internetIP_EditText[1] = findViewById(R.id.Config_editText_internetIP1);
        internetIP_EditText[2] = findViewById(R.id.Config_editText_internetIP2);
        internetIP_EditText[3] = findViewById(R.id.Config_editText_internetIP3);
        localPort1_EditText = findViewById(R.id.Config_editText_localPort1);
        localPort2_EditText = findViewById(R.id.Config_editText_localPort2);
        internetPort1_EditText = findViewById(R.id.Config_editText_internetPort1);
        internetPort2_EditText = findViewById(R.id.Config_editText_internetPort2);

        panel_name = bundle.getString("panelName");
        setTitleWithPanelName();

        final String panel_type = bundle.getString("panelType");
        String tell_user_type_of_panel = "";
        if( panel_type == null ) { //needed !
            tell_user_type_of_panel = "";
        } else {
            if( panel_type.equals("informing") ) {
                tell_user_type_of_panel = "Please be noted that you should be now standing in front of an " +
                        "informing panel. I.e. one which has either sensor(s), button(s) or contact(s) " +
                        "but not relays.<br/><br/>";
            } else if( panel_type.equals("obeying") ) {
                tell_user_type_of_panel = "Please be noted that you should be now standing in front of a " +
                        "listening-and-executing panel. I.e. one which has relay(s).<br/><br/>";
                final LinearLayout informing_panel_layout = findViewById(R.id.Config_Panel_Informing_Local_Section);
                informing_panel_layout.setVisibility(View.GONE);
            }
        }

        final TextView overviewExplanation_textView = findViewById(R.id.Config_textView_userGuide_overviewExplanation);
        overviewExplanation_textView.setText( Html.fromHtml(tell_user_type_of_panel + "Please press (long press) " +
                "the configuration button on the panel " +
                "until the yellow notification light indicator is totally continuously ON.<br/>" +
                "\t\t\tThe panel will now broadcast its own WIFI SSID named e.g. 'SetPanelNetConfig'.<br/>" +
                "Now, in order to be able to communicate with the panel, <b>please do connect your mobile phone to the " +
                "panel's broadcast WIFI SSID just mentioned</b> (don't put any password).<br/>" +
                "\t\t\tAfter that, fill the form below with the desired values then after you finish, please press the 'Set' button.<br/>" +
                " (P.S.: In case some text boxes you do not wish to fill, just leave them blank.)<br/>" +
                "\t\t\tFinally, wait some time until the yellow notification light indicator stops being continuously ON and" +
                " the WiFi of your phone will automatically disconnect from the panel's broadcast SSID (your phone's WiFi will " +
                "probably notify you about that).<br/>" +
                "And by now, congratulations! You have successfully saved the network configuration onto the panel.<br/>" +
                "You may go back to normal operation.") );

        final TextView admin_textView = findViewById( R.id.Config_textView_userGuide_Admin );
        admin_textView.setText("1) Just keep them filled anyway :\n" +
                "Since this is intended for me only, I haven't made restrictions control, e.g. " +
                "logically speaking, local ports must not be filled when the static IP is not filled (nevertheless, " +
                "I have kept them filled and that's fine), and internet ports " +
                "and IP must absolutely be filled when the static IP section is not filled. When I say 'static IP', I " +
                "also mean the 'Local Network' and 'Internet' checkboxes.\n" +
                "When 'unrelate mobile to panel' is checked, the values don't really matter then.\n\n" +
                "2) Note also that the entered values here do not configure the panel (for now). They only " +
                "configure the mobile panels' fragments\n\n" +
                "3) Usually, when you want to make use of this section, 'unrelate mobile to panel' must be " +
                "unchecked beforehand");

        final TextView obeyingIPs_textView = findViewById( R.id.Config_textView_userGuide_obeyingIPs );
        obeyingIPs_textView.setText("This section is intended only for a panel that is " +
                "'local' and 'informing' at the same time. I.e. a panel that transmits its info locally (not through Internet) " +
                "to at least one other panel.\n In this case, it is mandatory to fill as many fields as there are reflectors " +
                "to this particular informing local panel.\n" +
                "If this panel is not 'local' and 'informing' panel then please skip this section and leave the " +
                "'Reflector Static IP(s)' field blank. (By 'reflector' we mean a listening-and-executing panel thus usually" +
                "having relay/s.)\n"+
                "Particularly saying, it is the IP(s) of the reflector panel(s) that is to be entered here.\n"+
                "(Although this process will be automatic in some later versions of the system, currently we have to enter " +
                "the reflector panel IP(s) manually.)\n"+
                "Please note that the fields below (if already filled) are not necessarily " +
                "recommended values. Feel free to change them as suitable.");

        final TextView unmemorizeInMobile_textView = findViewById( R.id.Config_textView_userGuide_UnmemorizeInMobile );
        unmemorizeInMobile_textView.setText("If this panel is not within the menu, then please check this box.\n" +
                "If these configurations are intended for the panel shown before then keep it unchecked.");

        final TextView description_router_textView = findViewById(R.id.Config_textView_userGuide_router);
        description_router_textView.setText("This section is obligatory. It refers solely to this panel.\n" +
                "\t\t\tPlease enter the router's SSID and password." +
                " (P.S: if no password exists, please leave blank.)");

        final TextView userGuide_staticAllocation_textView = findViewById(R.id.Config_textView_userGuide_staticAllocation);
        userGuide_staticAllocation_textView.setText("The following below text boxes are only for static allocation of " +
                "this particular panel.\n" +
                "If you want your router to automatically assign an IP for the panel, please do empty all below fields.\n" +
                "Otherwise, do fill them all.");

        final TextView userGuide_localOrInternet_textView = findViewById(R.id.Config_textView_userGuide_localOrInternet);
        userGuide_localOrInternet_textView.setText( "We assign here whether the panel (not your mobile app) is able " +
                "to communicate locally, through Internet, or in both means." );


        final LinearLayout overviewExplanation_LinearLayout = findViewById(R.id.Config_LinearLayout_overviewExplanation);
        final Button overviewExplanation_button = findViewById( R.id.Config_button_overviewExplanation );
        overviewExplanation_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                expand( overviewExplanation_LinearLayout );
                overviewExplanation_button.setEnabled( false );
            }
        });
        final Button obeyingIPsExpansion_button = findViewById( R.id.Config_button_obeyingIPsExpansion );
        final LinearLayout obeyingIPs_LinearLayout = findViewById(R.id.Config_LinearLayout_obeyingIPsTexts);

        obeyingIPsExpansion_button.setOnClickListener( new View.OnClickListener() {
            public void onClick( View view ) {
                expand( obeyingIPs_LinearLayout );
                obeyingIPsExpansion_button.setEnabled( false );
            }
        });

        final Toasting toasting = new Toasting( this );

        final Button adminExpansion_button = findViewById( R.id.Config_button_AdminExpansion );
        final LinearLayout admin_LinearLayout = findViewById(R.id.Config_LinearLayout_AdminTexts);

        adminExpansion_button.setOnClickListener( new View.OnClickListener() {
            public void onClick( View view ) {
                //usually I want to hide this section as it contains the IP of the server.
                AlertDialog.Builder builder = new AlertDialog.Builder(ConfigPanel.this);
                builder.setTitle("Password");
                // Set up the input
                final EditText input = new EditText(ConfigPanel.this);
                // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                builder.setView(input);
                // Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if( input.getText().toString().equals("Shmegevod0") ) {
                            expand(admin_LinearLayout);
                            adminExpansion_button.setEnabled(false);
                        } else {
                            toasting.toast("Wrong password. Please contact the manufacturer +961/70/853721", Toast.LENGTH_LONG, false);
                        }
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
            }
        });

        final CheckBox unmemorizeInMobile_checkBox = findViewById(R.id.checkBox_unmemorizeInMobile);
        unmemorizeInMobile_checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if (isChecked) {
                    setTitle( "Network Configuration - General Panel" );
                    panel_index = "";
                    setSharedPreferencesVariables();
                } else {
                    setTitleWithPanelName();
                    panel_index = original_panel_index;
                    setSharedPreferencesVariables();
                }
            }
        });

        /*User must be now connected to the panel's SSID. I can make a check whether the WIFI is on or not,
        * and whether the mobile is connected to the specified SSID or not, but later.*/
        /*Once the "Set" button is pressed, we must :
        * 1) check the validity of the entered data. We will also instruct the user to fix the error.
        * 2) connects to a predefined socket and sends the info.*/
        setSharedPreferencesVariables();

        final CheckBox local_checkBox = findViewById(R.id.checkBox_local);
        Log.i("Config...", "Youssef/ local checkbox prefs is " + network_prefs.getBoolean( "local", true ));
        local_checkBox.setChecked( network_prefs.getBoolean( "local", true ) ); //didn't use local_checkBox.getTag().toString() instead of "local"
        local_checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                //prefs_editor.putBoolean( local_checkBox.getTag().toString(), isChecked ).apply(); //maybe it's better to  use getTag() but no need to complicate things
                prefs_editor.putBoolean( "local", isChecked ).apply();
            }
        });

        final CheckBox internet_checkBox = findViewById(R.id.checkBox_internet);
        Log.i("Config...", "Youssef/ internet checkbox prefs is " + network_prefs.getBoolean( "internet", true ));
        internet_checkBox.setChecked( network_prefs.getBoolean( "internet", true ) ); //didn't use internet_checkBox.getTag().toString()
        internet_checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                //prefs_editor.putBoolean( internet_checkBox.getTag().toString(), isChecked ).apply();//maybe it's better to  use getTag() but no need to complicate things
                prefs_editor.putBoolean( "internet", isChecked ).apply();
            }
        });

        //The default values here in this admin section are worthless. It takes the default value from the caller fragment
        localPort1_EditText.setText(network_prefs.getString( localPort1_EditText.getTag().toString(), "11359") );
        localPort2_EditText.setText(network_prefs.getString( localPort2_EditText.getTag().toString(), "11360") );
        internetPort1_EditText.setText(network_prefs.getString( internetPort1_EditText.getTag().toString(), "11359") );
        internetPort2_EditText.setText(network_prefs.getString( internetPort2_EditText.getTag().toString(), "11360") );


        SSID_EditText.setText( network_prefs.getString( SSID_EditText.getTag().toString(), "MySSID") );
        Password_EditText.setText( network_prefs.getString( Password_EditText.getTag().toString(), "MyPassword") );
        obeyingIP0_EditText[0].setText( network_prefs.getString( obeyingIP0_EditText[0].getTag().toString(), "") );
        obeyingIP0_EditText[1].setText( network_prefs.getString( obeyingIP0_EditText[1].getTag().toString(), "") );
        obeyingIP0_EditText[2].setText( network_prefs.getString( obeyingIP0_EditText[2].getTag().toString(), "") );
        obeyingIP0_EditText[3].setText( network_prefs.getString( obeyingIP0_EditText[3].getTag().toString(), "") );
        obeyingIP1_EditText[0].setText( network_prefs.getString( obeyingIP1_EditText[0].getTag().toString(), "") );
        obeyingIP1_EditText[1].setText( network_prefs.getString( obeyingIP1_EditText[1].getTag().toString(), "") );
        obeyingIP1_EditText[2].setText( network_prefs.getString( obeyingIP1_EditText[2].getTag().toString(), "") );
        obeyingIP1_EditText[3].setText( network_prefs.getString( obeyingIP1_EditText[3].getTag().toString(), "") );
        IP_EditText[0].setText( network_prefs.getString( IP_EditText[0].getTag().toString(), "") );
        IP_EditText[1].setText( network_prefs.getString( IP_EditText[1].getTag().toString(), "") );
        IP_EditText[2].setText( network_prefs.getString( IP_EditText[2].getTag().toString(), "") );
        IP_EditText[3].setText( network_prefs.getString( IP_EditText[3].getTag().toString(), "") );
        gatewayIP_EditText[0].setText( network_prefs.getString( gatewayIP_EditText[0].getTag().toString(), "") );
        gatewayIP_EditText[1].setText( network_prefs.getString( gatewayIP_EditText[1].getTag().toString(), "") );
        gatewayIP_EditText[2].setText( network_prefs.getString( gatewayIP_EditText[2].getTag().toString(), "") );
        gatewayIP_EditText[3].setText( network_prefs.getString( gatewayIP_EditText[3].getTag().toString(), "") );
        subnet_EditText[0].setText( network_prefs.getString( subnet_EditText[0].getTag().toString(), "") );
        subnet_EditText[1].setText( network_prefs.getString( subnet_EditText[1].getTag().toString(), "") );
        subnet_EditText[2].setText( network_prefs.getString( subnet_EditText[2].getTag().toString(), "") );
        subnet_EditText[3].setText( network_prefs.getString( subnet_EditText[3].getTag().toString(), "") );
        Mac_EditText[0].setText( network_prefs.getString( Mac_EditText[0].getTag().toString(), "") );
        Mac_EditText[1].setText( network_prefs.getString( Mac_EditText[1].getTag().toString(), "") );
        Mac_EditText[2].setText( network_prefs.getString( Mac_EditText[2].getTag().toString(), "") );
        Mac_EditText[3].setText( network_prefs.getString( Mac_EditText[3].getTag().toString(), "") );
        Mac_EditText[4].setText( network_prefs.getString( Mac_EditText[4].getTag().toString(), "") );
        Mac_EditText[5].setText( network_prefs.getString( Mac_EditText[5].getTag().toString(), "") );
        //Now the admin. BTW, default values here are worthless; we got now real values, based on the fact that we enter the fragment and putStrings there before we reach here
        internetIP_EditText[0].setText( network_prefs.getString( internetIP_EditText[0].getTag().toString(), "") );
        internetIP_EditText[1].setText( network_prefs.getString( internetIP_EditText[1].getTag().toString(), "") );
        internetIP_EditText[2].setText( network_prefs.getString( internetIP_EditText[2].getTag().toString(), "") );
        internetIP_EditText[3].setText( network_prefs.getString( internetIP_EditText[3].getTag().toString(), "") );

        //obeyingIP_message (this section has to be after getting texts from shared preferences)
        final String obeying0IP0 = obeyingIP0_EditText[0].getText().toString();
        final String obeying0IP1 = obeyingIP0_EditText[1].getText().toString();
        final String obeying0IP2 = obeyingIP0_EditText[2].getText().toString();
        final String obeying0IP3 = obeyingIP0_EditText[3].getText().toString();
        final String obeying1IP0 = obeyingIP1_EditText[0].getText().toString();
        final String obeying1IP1 = obeyingIP1_EditText[1].getText().toString();
        final String obeying1IP2 = obeyingIP1_EditText[2].getText().toString();
        final String obeying1IP3 = obeyingIP1_EditText[3].getText().toString();

        if( !( obeying0IP0.equals("") && obeying0IP1.equals("") && obeying0IP2.equals("") && obeying0IP3.equals("") &&
                obeying1IP0.equals("") && obeying1IP1.equals("") && obeying1IP2.equals("") && obeying1IP3.equals("") ) ) {
            //expandObeyingIPSection.showText(); //dramatically bad
            obeyingIPsExpansion_button.setEnabled( false );
            obeyingIPs_LinearLayout.setVisibility(View.VISIBLE);
        }

        final TextView rectifyUserInput = findViewById(R.id.Config_warnUser_textView);

        final LinearLayout warnUser_layout = findViewById( R.id.Config_warnUser_layout);
        final ScrollView scrollView = findViewById( R.id.Config_scroll);
        final Button warnUser_OkButton = findViewById( R.id.Config_warnUser_buttonOk );

        class prefsForFutureSession {
            private EditText eT;
            private prefsForFutureSession( EditText eT_arg ) {
                eT = eT_arg;
                //Log.i("Youssef Config", "First Text watcher" + eT.getTag().toString());
                android.text.TextWatcher textWatcher = new TextWatcher() {
                    @Override
                    public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                    }

                    @Override
                    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                    }

                    @Override
                    public void afterTextChanged(Editable ed) {
                        prefs_editor.putString(eT.getTag().toString(), eT.getText().toString()).apply();
                        //Log.i("Youssef Config", "First Text watcher" + eT.getTag().toString());
                    }
                };
                eT.addTextChangedListener(textWatcher);
            }
        }

        class checkUserIP {
            private EditText eT;
            private checkUserIP( EditText eT_arg ) {
                eT = eT_arg;
                /*
                    if(s == null) { //compiler says that this is always false
                        return;
                    }*/
                //e.g. 06 instead of just 6
                /*
                    if(warnUser_OkButton.requestFocus()) {
                        //    Log.i("Youssef Config", "ok button got focus");
                    } else {
                        //  Log.i("Youssef Config", "ok button didn't get focus");
                    }
                    */
                //the following 2 lines is to fix a weird problem of stealing focus
                TextWatcher IP_TextWatcher = new TextWatcher() {
                    @Override
                    public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                    }

                    @Override
                    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                    }

                    @Override
                    public void afterTextChanged(Editable ed) {
                        String s = ed.toString();
                    /*
                    if(s == null) { //compiler says that this is always false
                        return;
                    }*/
                        if (!s.equals("")) {
                            editable = ed;
                            editText = eT;
                            if (s.length() > 1 && s.charAt(0) == '0') { //e.g. 06 instead of just 6
                                rectifyUserInput.setText("Do you mean '0' ?");
                                showNotification();
                                return;
                            }

                            int IP;
                            try {
                                IP = Integer.parseInt(s);
                            } catch (NumberFormatException e) {
                                IP = -1;
                            }
                            if (IP < 0 || IP > 255) {
                                rectifyUserInput.setText("Please enter valid number between 0 and 255");
                                showNotification();
                                return;
                            }
                        }
                        prefs_editor.putString(eT.getTag().toString(), eT.getText().toString()).apply();
                    }

                    private void showNotification() {
                        warnUser_layout.setVisibility(View.VISIBLE);
                        scrollView.setAlpha((float) 0.2);
                        warnUser_OkButton.requestFocus();
                    /*
                    if(warnUser_OkButton.requestFocus()) {
                        //    Log.i("Youssef Config", "ok button got focus");
                    } else {
                        //  Log.i("Youssef Config", "ok button didn't get focus");
                    }
                    */
                        hideKeyboard();
                        //the following 2 lines is to fix a weird problem of stealing focus
                        scrollView.setFocusable(false);
                        scrollView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
                    }
                };
                eT.addTextChangedListener(IP_TextWatcher);
            }
        }

        class checkUserMAC {
            private EditText eT;
            private checkUserMAC( EditText eT_arg ) {
                eT = eT_arg;
                //please note that by its nature, this method is for char by char usage
                /*
                    if(s == null) { //compiler says that this is always false
                        return;
                    }*/
                /*
                            if(warnUser_OkButton.requestFocus()) {
                                //    Log.i("Youssef Config", "ok button got focus");
                            } else {
                                //  Log.i("Youssef Config", "ok button didn't get focus");
                            }
                            */
                //the following 2 lines is to fix a weird problem of stealing focus
                TextWatcher MAC_TextWatcher = new TextWatcher() {
                    @Override
                    public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                    }

                    @Override
                    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                    }

                    @Override
                    public void afterTextChanged(Editable ed) { //please note that by its nature, this method is for char by char usage
                        String s = ed.toString();
                    /*
                    if(s == null) { //compiler says that this is always false
                        return;
                    }*/
                        if (!s.equals("")) {
                            boolean char1IsHex, char2IsHex = true;
                            char1IsHex = checkIfHex(s.charAt(0));
                            if (s.length() == 2) {
                                char2IsHex = checkIfHex(s.charAt(1));
                            }
                            if (!char1IsHex || !char2IsHex) {
                                editable = ed;
                                editText = eT;
                                rectifyUserInput.setText("Please enter only hexadecimals (from 0 to F)");
                                warnUser_layout.setVisibility(View.VISIBLE);
                                scrollView.setAlpha((float) 0.2);
                                warnUser_OkButton.requestFocus();
                            /*
                            if(warnUser_OkButton.requestFocus()) {
                                //    Log.i("Youssef Config", "ok button got focus");
                            } else {
                                //  Log.i("Youssef Config", "ok button didn't get focus");
                            }
                            */
                                hideKeyboard();
                                //the following 2 lines is to fix a weird problem of stealing focus
                                scrollView.setFocusable(false);
                                scrollView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
                                return;
                            }
                        }
                        prefs_editor.putString(eT.getTag().toString(), eT.getText().toString()).apply();
                    }
                };
                eT.addTextChangedListener(MAC_TextWatcher);
            }

            private boolean checkIfHex( char c ) {
                return ( c == '0' || c == '1' || c == '2' || c == '3' || c == '4' || c == '5' || c == '6' || c == '7' || c == '8' ||
                        c == '9' || c == 'a' || c == 'b' || c == 'c' || c == 'd' || c == 'e' || c == 'f' || c == 'A' ||
                        c == 'B' || c == 'C' || c == 'D' || c == 'E' || c == 'F' );
            }

        }

        new checkUserIP(obeyingIP0_EditText[0]);
        new checkUserIP(obeyingIP0_EditText[1]);
        new checkUserIP(obeyingIP0_EditText[2]);
        new checkUserIP(obeyingIP0_EditText[3]);
        new checkUserIP(obeyingIP1_EditText[0]);
        new checkUserIP(obeyingIP1_EditText[1]);
        new checkUserIP(obeyingIP1_EditText[2]);
        new checkUserIP(obeyingIP1_EditText[3]);
        new checkUserIP(IP_EditText[0]);
        new checkUserIP(IP_EditText[1]);
        new checkUserIP(IP_EditText[2]);
        new checkUserIP(IP_EditText[3]);
        new checkUserIP(gatewayIP_EditText[0]);
        new checkUserIP(gatewayIP_EditText[1]);
        new checkUserIP(gatewayIP_EditText[2]);
        new checkUserIP(gatewayIP_EditText[3]);
        new checkUserIP(subnet_EditText[0]);
        new checkUserIP(subnet_EditText[1]);
        new checkUserIP(subnet_EditText[2]);
        new checkUserIP(subnet_EditText[3]);
        new checkUserMAC(Mac_EditText[0]);
        new checkUserMAC(Mac_EditText[1]);
        new checkUserMAC(Mac_EditText[2]);
        new checkUserMAC(Mac_EditText[3]);
        new checkUserMAC(Mac_EditText[4]);
        new checkUserMAC(Mac_EditText[5]);
        new checkUserIP(internetIP_EditText[0]);
        new checkUserIP(internetIP_EditText[1]);
        new checkUserIP(internetIP_EditText[2]);
        new checkUserIP(internetIP_EditText[3]);
        new prefsForFutureSession(SSID_EditText);
        new prefsForFutureSession(Password_EditText);
        new prefsForFutureSession(localPort1_EditText);
        new prefsForFutureSession(localPort2_EditText);
        new prefsForFutureSession(internetPort1_EditText);
        new prefsForFutureSession(internetPort2_EditText);
        //new prefsForFutureSession(IP_EditText[0]); //It's not wrong to add another TextWatcher to this editText
        //but we restrained from doing that because we don't want to save faulty data to prefs

        final int SSID_Buff_Size = 32;
        final int Password_Buff_Size = 64;
        final int Max_IP_Buff_Size = 4;
        final int MAC_Buff_Size = 12;
        final int Local_Or_Internet_Size = 1; //'l' for local, 'i' for Internet, 'b' for both
        final int Max_AP_Buffer_Size = 1 + SSID_Buff_Size + 1 + Password_Buff_Size + 1 + 1 + Max_IP_Buff_Size + 1
                + 1 + Max_IP_Buff_Size + 1 + 1 + Max_IP_Buff_Size + 1 + MAC_Buff_Size + 1 + Local_Or_Internet_Size + 1 + 1; /*You may see the format
                * of the message to be sent in the description of the method isAllTextsValid()
                * It is like this :
                * determinant then SSID then trailor then password then trailor then header of static IP then the static IP
                * then trailor then header of gateway IP then the gateway IP then trailor then header of subnet then subnet then
                 * trailor then 12 MAC characters then trailor then null char*/

        final byte[] message_byte = new byte[ Max_AP_Buffer_Size ]; /*while this is declared as final but the value of
        *every element of the array can yet be changed. check https://www.geeksforgeeks.org/final-arrays-in-java/ */

        class ConnectToPanel extends Thread { //it's a little weird that the class is inside a method. Anyway, there isn't even a warning.
            private int AP_port;
            private final Socket client = new Socket();
            private static final int NetworkConf_Port = 3551;
            private static final int obeyIP_Port = 3552;//it was 3552, but that's not a problem now

            private PrintWriter printWriter;
            private OutputStream outputStream;

            private boolean still_sending = false;
            private boolean still_waiting = false;

            private final Handler waitThenAllowUserToSendAgain_Handler = new Handler();
            private final Runnable runnable = new Runnable() {
                public void run() {
                    Log.i("Youssef ConfigPan.", "Delaying 6 sec stopped. User may now press again on 'Set' button");
                    still_waiting = false;
                }
            };
            /*
            private final Handler waitThenSendSecondMessage_Handler = new Handler();
            private final Runnable runnable1 = new Runnable() {
                public void run() {
                    Log.i("Youssef ConfigPan.", "Delaying 0.5 sec stopped. Will try to send second message now");
                    try {
                        outputStream.close();
                        outputStream = null;
                        outputStream = client.getOutputStream();
                        outputStream.write(message_byte);
                        outputStream.flush();
                        Log.i("Youssef ConfigPan.", "Must had succeeded sending second message by now");
                    } catch( Exception e) {
                        e.printStackTrace();
                        Log.i("Youssef ConfigPanel", "Failed sending second message.");
                    }
                }
            };
             */

            ConnectToPanel( int AP_port ) {
                this.AP_port = AP_port;
            }

            @Override
            public void run() {
                try {
                    //sent_messages_number++;
/*
                    if( sent_messages_number >= max_sent_messages_number ) { /*It's really == but I made it >= as a
                     * protection just in case.
                     * Anyway, here we're ASSUMING (and I suppose it is the case) that the max_sent_messages_number
                     * refers to the messages we are interested in sending, which are :
                     * - the obeying IP and the network config of the panel if max_sent_messages_number was 2
                     * - or just the network config of the panel if max_sent_messages_number was 1

                        still_sending = true;
                    }
*/
                    still_sending = true;

                    if (printWriter != null){
                        Log.i("Youssef ConfigPanel", "Closing printWriter.");
                        printWriter.close(); //returns void
                        printWriter = null;
                    }
                    if (outputStream != null) {
                        Log.i("Youssef ConfigPanel", "Closing outputStream.");
                        outputStream.close();
                        outputStream = null;
                    }
                    //if (client != null) { //always true
                    if( client.isConnected() ) {
                        Log.i("Youssef ConfigPanel", "Closing client.");
                        client.close(); //I prefer to start a new connection in case it was actually disconnected
                        //Please do not make client null.
                    }
/*I don't think it's really fine to close and then connect right away! What happens is that the NodeMCU actually is
* instructed to directly close the socket after receiving the message so I guess the socket here gets closed as well.*/

                    //}
                    //if( !client.isConnected()  ) { //mainly sent_messages_number will also be 0
                    Log.i("Youssef ConfigPanel", "Right before Connect attempt. Port is " + AP_port);
                    /*this follows the list of private IP addresses from 172.16.0.0 to 172.31.255.255
                     * where subnet fixes the first 12 bits (10101100 for the first byte, and 0001xxxx for the second byte), so subnet has to be
                     * 255.240.0.0 according to https://en.wikipedia.org/wiki/Private_network*/
                    String AP_IP = "172.17.15.30";
                    //String AP_IP = "192.168.1.1";
                    client.connect( new InetSocketAddress( AP_IP, AP_port ) );
/*                  //I had an attempt to make UDP communication, but I didn't have ? luckily
                    DatagramSocket ds = null;
                    byte[] ipAddr = new byte[]{ (byte) 192, (byte) 168,(byte) 1, (byte) 1};
                    InetAddress addr = InetAddress.getByAddress(ipAddr);
                    ds = new DatagramSocket(AP_port);ngIP message should be sent by
                    dp = new DatagramPacket(Message.getBytes(), Message.getBytes().length, addr, 50000);
                    ds.setBroadcast(true);
                    ds.send(dp);
*/
                    Log.i("Youssef ConfigPanel", "Connected to AP.");
                    //}

                    //if( printWriter == null && max_sent_messages_number == 2 ) {
                    outputStream = client.getOutputStream();
                    if( AP_port == obeyIP_Port ) {
                        printWriter = new PrintWriter( outputStream );
                        //now sending the message
                        printWriter.write( obeyingIP_message );
                        printWriter.flush();
                        Log.i("Youssef ConfigPanel", "ObeyingIP message should be sent by now. And it is");
                        //waitThenSendSecondMessage_Handler.postDelayed(runnable1, 500);
                    } else if( AP_port == NetworkConf_Port ) {
                        Log.i("ConfigPanel","Youssef/ First byte of the sent message is : " + message_byte[0] );
                        outputStream.write( message_byte );
                        outputStream.flush();
                    }

                    Log.i("Youssef ConfigPan.", "delaying a few seconds to prevent user from re-pressing on 'Set' button");

                    /*
                    if( sent_messages_number >= max_sent_messages_number ) { /*It's really == but I made it >= as a
                     * protection just in case.*/
                    still_waiting = true;

//                        sent_messages_number = 0;
                    waitThenAllowUserToSendAgain_Handler.postDelayed(runnable, 6000);
//                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i("Youssef ConfigPanel", "Caught exception in connection to panel.");
                }
                still_sending = false;
            }
        }

        class AfterSetButtonPressed {
            private final byte trailor = 127; /*same trailor used in panels to be configured.
                 * Please don't use a trailor of '\0' as this would truncate the message to be sent. And don't use as well any char
                 * having ASCII from 1 to 15 as this would confuse with the 'header' byte (review encodeHeaderAndInsertIPsAndTrailor method for more details)*/
            private String static_IP = "";
            private int index_to_fill_message_byte;
            private void encodeHeaderAndInsertIPsAndTrailor( byte first_byte, byte second_byte,
                                                             byte third_byte, byte fourth_byte ) {
                /*first_byte is considered the MSByte and fourth_byte is considered the LSByte.
                 * This method will be used for static IP, gateway IP, or subnet. We need a header because when we decided to use
                 * a byte array to be sent to NodeMCU, we had fallen into the problem of sending a 0 (or '\0') byte which actually
                 * truncates the sent message and causes the message to be non-useful.
                  * So before the 4 IP bytes we need a special header byte that will point to where the 0 byte of the
                  * IP bytes exists (if existent).
                  * Description of this header byte :
                  * The LSBit corresponds to the LSByte, thus it corresponds to fourth_byte
                   * The bit next to it corresponds to third_byte
                   * the bit next to it corresponds to second_byte
                   * And the MSBit corresponds to first_byte.
                   * As you can see, this header byte can range anywhere from 1 to 15. (By the way, this is the reason that led me
                   * to omit trailor from being '\n' or 10, and that is to avoid confusion with this Header byte.)
                    * But what do we do if non of the 4 IP bytes is 0? In this case I decided to use the following sequence 00010000
                    * which is simply 16 to refer to this case. (We cannot use 0 (or '\0') because this would truncate the message.)
                    * I would like to note that this method cooperates with index_to_fill_message_byte which refers to where
                    * this header byte must be put in the message_byte array.
                    * I also decided that a zero IP byte be omitted and will not be considered in message_byte array, e.g.
                     * in case of 4 non-0 IP bytes, we'll put header then 4 bytes (and then trailor of course)
                     * in case of one 0 IP byte, we'll put header then the 3 non-0 IP bytes (and then trailor of course) - of course,
                     * sequence (order) of non-0 IP bytes matters.
                      * in case of two 0 IP bytes, we'll put header then the 2 non-0 IP bytes (and then trailor of course) - of course,
                     * sequence (order) of non-0 IP bytes matters.
                     * in case of three 0 IP bytes, we'll put header then the non-0 IP byte (and then trailor of course)
                     * in case of four 0 IP bytes, we'll put header which is 16 (and then trailor of course)
                 */
                byte header = 0;
                byte[] IP_bytes = new byte[ Max_IP_Buff_Size ];
                IP_bytes[0] = first_byte;
                IP_bytes[1] = second_byte;
                IP_bytes[2] = third_byte;
                IP_bytes[3] = fourth_byte;
                final int index_to_fill_byte = index_to_fill_message_byte;
                for( int i = 0; i < Max_IP_Buff_Size; i++ ) {
                    header <<= 1; //left shift with 0
                    if( IP_bytes[i] == 0 ) {
                        header++; //this makes the newly left shifted bit '1' instead of '0'
                    } else {
                        index_to_fill_message_byte++;
                        message_byte[index_to_fill_message_byte] = IP_bytes[i];
                    }
                }
                if( header == 0 ) {
                    header = 16;
                }
                message_byte[index_to_fill_byte] = header;

                index_to_fill_message_byte++;
                message_byte[index_to_fill_message_byte] = trailor;
            }

            private boolean isAllTextsValid() {
                /*What is this method useful for?
                * In case of a non-valid user entry, we will notify (toast - and not using warnUser_layout) the user about it here.
                *static_IP will also be assigned in this method. As well as setting the "message" variable.
                *The old days :
                * "message" will be something like 1SSID->trailor or 2SSID->trailor->Password->trailor or
                * 5SSID->trailor->header->StaticIP->trailor->header->GatewayIP->trailor->header->Subnet->trailor->MAC->trailor or
                * 6SSID->trailor->Password->trailor->header->StaticIP->trailor->header->GatewayIP->trailor->header->Subnet->trailor->MAC->trailor
                * Now :
                * I have added to the end of this message either l\n (for local), i\n (for internet), or b\n (for both)
                * "message" will be something like 2SSID->trailor or 3SSID->trailor->Password->trailor or
                * 6SSID->trailor->header->StaticIP->trailor->header->GatewayIP->trailor->header->Subnet->trailor->
                * MAC->trailor->local_or_internet_or_both->trailor or
                * 7SSID->trailor->Password->trailor->header->StaticIP->trailor->header->GatewayIP->trailor->header->Subnet->trailor->
                * MAC->trailor->local_or_internet_or_both->trailor

                * * This method also sends the obeying IP (if filled at all, and if filled correctly) of 1 (in this version) obeying panel.
                * Thus if the obeying IP is filled, it prepares 2 messages, one for the obeying IP and another for the network configuration
                  * of the particular panel the mobile app connects to, and this method will also set the value of
                  * max_sent_messages_number to 2. If the obeying IP is not filled, then this method prepares only one message and
                  * max_sent_messages_number will be 1.
                */

                static_IP = "";
                //message = "";

                String SSID = SSID_EditText.getText().toString();
                if(SSID.equals("")) {
                    toasting.toast("Please enter the SSID the panel must connect to.", Toast.LENGTH_LONG, false);
                    SSID_EditText.requestFocus();
                    return false;
                }

                final String obeying0IP0 = obeyingIP0_EditText[0].getText().toString();
                final String obeying0IP1 = obeyingIP0_EditText[1].getText().toString();
                final String obeying0IP2 = obeyingIP0_EditText[2].getText().toString();
                final String obeying0IP3 = obeyingIP0_EditText[3].getText().toString();
                final String obeying1IP0 = obeyingIP1_EditText[0].getText().toString();
                final String obeying1IP1 = obeyingIP1_EditText[1].getText().toString();
                final String obeying1IP2 = obeyingIP1_EditText[2].getText().toString();
                final String obeying1IP3 = obeyingIP1_EditText[3].getText().toString();

                max_sent_messages_number = 0;
                obeyingIP_message = "";
                byte reflectors_number_of_informing_local_panel = 0;

                final boolean no_obeyingIP = obeying0IP0.equals("") && obeying0IP1.equals("") &&
                        obeying0IP2.equals("") && obeying0IP3.equals("") &&
                        obeying1IP0.equals("") && obeying1IP1.equals("") &&
                        obeying1IP2.equals("") && obeying1IP3.equals(""); //which is suitable to a non-informing local panel

                if( no_obeyingIP ) {
                    max_sent_messages_number = 1;
                } else {
                    if( obeying0IP0.equals("") || obeying0IP1.equals("") || obeying0IP2.equals("") || obeying0IP3.equals("") ) {
                        if (obeying0IP0.equals("")) {
                            obeyingIP0_EditText[0].requestFocus();
                        } else if (obeying0IP1.equals("")) {
                            obeyingIP0_EditText[1].requestFocus();
                        } else if (obeying0IP2.equals("")) {
                            obeyingIP0_EditText[2].requestFocus();
                        } else { //meaning if (obeying0IP3.equals("")) which is always true
                            obeyingIP0_EditText[3].requestFocus();
                        }
                        toasting.toast("Usually the fields of the static items must all be filled.",
                                Toast.LENGTH_LONG, false);
                        return false;
                    } else {
                        reflectors_number_of_informing_local_panel = 1;
                        if( !( obeying1IP0.equals("") && obeying1IP1.equals("") && obeying1IP2.equals("") && obeying1IP3.equals("") ) ) {
                            if( obeying1IP0.equals("") || obeying1IP1.equals("") || obeying1IP2.equals("") || obeying1IP3.equals("") ) {
                                if( obeying1IP0.equals("") ) {
                                    obeyingIP1_EditText[0].requestFocus();
                                } else if( obeying1IP1.equals("") ) {
                                    obeyingIP1_EditText[1].requestFocus();
                                } else if( obeying1IP2.equals("") ) {
                                    obeyingIP1_EditText[2].requestFocus();
                                } else { //meaning if( obeying1IP3.equals("") ) which is always true
                                    obeyingIP1_EditText[3].requestFocus();
                                }
                                toasting.toast("Usually the fields of the static items must all be filled.",
                                        Toast.LENGTH_LONG, false);
                                return false;
                            } else {
                                reflectors_number_of_informing_local_panel = 2;
                            }
                        }
                    }

                    if( obeying0IP0.equals("0") ) {
                        toasting.toast("The static IP does not usually start with 0.", Toast.LENGTH_LONG, false);
                        obeyingIP0_EditText[0].requestFocus();
                        return false;
                    }
                    if( reflectors_number_of_informing_local_panel != 1 ) {
                        if (obeying1IP0.equals("0")) {
                            toasting.toast("The static IP does not usually start with 0.", Toast.LENGTH_LONG, false);
                            obeyingIP1_EditText[0].requestFocus();
                            return false;
                        }
                    }
                    //All 1's IP is probably rejected as well
                    if( obeying0IP0.equals("255") && obeying0IP1.equals("255") && obeying0IP2.equals("255") &&
                            obeying0IP3.equals("255") ) {
                        toasting.toast("The static IP is not usually all ones.", Toast.LENGTH_LONG, false);
                        obeyingIP0_EditText[0].requestFocus();
                        return false;
                    }
                    if( reflectors_number_of_informing_local_panel != 1 ) {
                        if (obeying1IP0.equals("255") && obeying1IP1.equals("255") && obeying1IP2.equals("255") &&
                                obeying1IP3.equals("255")) {
                            toasting.toast("The static IP is not usually all ones.", Toast.LENGTH_LONG, false);
                            obeyingIP1_EditText[0].requestFocus();
                            return false;
                        }
                    }
                    max_sent_messages_number = 2;
                    //now preparing the obeying IP message that will be sent separately
                    if( reflectors_number_of_informing_local_panel == 1 ) {
                        obeyingIP_message = "obey" + "1" + obeying0IP0 + "." + obeying0IP1 + "." + obeying0IP2 + "." + obeying0IP3 + "\\";
                    } else {
                        obeyingIP_message = "obey" + "2" + obeying0IP0 + "." + obeying0IP1 + "." + obeying0IP2 + "." + obeying0IP3 +
                                "\\" + obeying1IP0 + "." + obeying1IP1 + "." + obeying1IP2 + "." + obeying1IP3 + "\\";
                    }
                    //obey is like a header, 1 means one obeying panel, '\' is a trailor
                }

                final String password = Password_EditText.getText().toString();
                final String IP0 = IP_EditText[0].getText().toString();
                final String IP1 = IP_EditText[1].getText().toString();
                final String IP2 = IP_EditText[2].getText().toString();
                final String IP3 = IP_EditText[3].getText().toString();
                final String gatewayIP0 = gatewayIP_EditText[0].getText().toString();
                final String gatewayIP1 = gatewayIP_EditText[1].getText().toString();
                final String gatewayIP2 = gatewayIP_EditText[2].getText().toString();
                final String gatewayIP3 = gatewayIP_EditText[3].getText().toString();
                final String subnet0 = subnet_EditText[0].getText().toString();
                final String subnet1 = subnet_EditText[1].getText().toString();
                final String subnet2 = subnet_EditText[2].getText().toString();
                final String subnet3 = subnet_EditText[3].getText().toString();
                final String mac0 = Mac_EditText[0].getText().toString();
                final String mac1 = Mac_EditText[1].getText().toString();
                final String mac2 = Mac_EditText[2].getText().toString();
                final String mac3 = Mac_EditText[3].getText().toString();
                final String mac4 = Mac_EditText[4].getText().toString();
                final String mac5 = Mac_EditText[5].getText().toString();

                final boolean case1Or2 = IP0.equals("") && IP1.equals("") && IP2.equals("") && IP3.equals("") && gatewayIP0.equals("") &&
                        gatewayIP1.equals("") && gatewayIP2.equals("") && gatewayIP3.equals("") && subnet0.equals("") &&
                        subnet1.equals("") && subnet2.equals("") && subnet3.equals("") && mac0.equals("") &&
                        mac1.equals("") && mac2.equals("") && mac3.equals("") && mac4.equals("") && mac5.equals("");

                if( case1Or2 ) {
                    /*Concerning the obeyingIP, if it's not case1Or2 then we will be performing some more checks for the obeying
                         * IP to be valid. Particularly comparing it with the subnet.*/
                    if(password.equals("")) {
                        //message = "1" + SSID + trailor;
                        index_to_fill_message_byte = 0;
                        message_byte[index_to_fill_message_byte] = 2;
                        index_to_fill_message_byte++;
                        for(int i = 0; i < SSID.length(); i++ )
                            message_byte[ index_to_fill_message_byte + i ] = (byte) SSID.charAt(i);
                        index_to_fill_message_byte = index_to_fill_message_byte + SSID.length();
                        message_byte[index_to_fill_message_byte] = trailor;
                    } else {
                        //message = "2" + SSID + trailor + password + trailor;
                        index_to_fill_message_byte = 0;
                        message_byte[index_to_fill_message_byte] = 3;
                        index_to_fill_message_byte++;
                        for(int i = 0; i < SSID.length(); i++ )
                            message_byte[ index_to_fill_message_byte + i ] = (byte) SSID.charAt(i);
                        index_to_fill_message_byte = index_to_fill_message_byte + SSID.length();
                        message_byte[index_to_fill_message_byte] = trailor;
                        index_to_fill_message_byte++;
                        for(int i = 0; i < password.length(); i++ )
                            message_byte[ index_to_fill_message_byte + i ] = (byte) password.charAt(i);
                        index_to_fill_message_byte = index_to_fill_message_byte + password.length();
                        message_byte[index_to_fill_message_byte] = trailor;
                    }

                    if( local_checkBox.isChecked() ) {
                        local_checkBox.requestFocus(); //doesn't work
                        toasting.toast("It looks like you want this panel to communicate locally.\n" +
                                "In this case you must fill the static IP configurations.", Toast.LENGTH_LONG, false);
                        return false;
                    }

                    //here we were almost going to return true...
                } else {
                    if(IP0.equals("") || IP1.equals("") || IP2.equals("") || IP3.equals("") || gatewayIP0.equals("") ||
                            gatewayIP1.equals("") || gatewayIP2.equals("") || gatewayIP3.equals("") || subnet0.equals("") ||
                            subnet1.equals("") || subnet2.equals("") || subnet3.equals("") || mac0.equals("") ||
                            mac1.equals("") || mac2.equals("") || mac3.equals("") || mac4.equals("") || mac5.equals("")) {
                        if(IP0.equals("")) {
                            IP_EditText[0].requestFocus();
                        } else if(IP1.equals("")){
                            IP_EditText[1].requestFocus();
                        } else if(IP2.equals("")){
                            IP_EditText[2].requestFocus();
                        } else if(IP3.equals("")){
                            IP_EditText[3].requestFocus();
                        } else if(gatewayIP0.equals("")){
                            gatewayIP_EditText[0].requestFocus();
                        } else if(gatewayIP1.equals("")){
                            gatewayIP_EditText[1].requestFocus();
                        } else if(gatewayIP2.equals("")){
                            gatewayIP_EditText[2].requestFocus();
                        } else if(gatewayIP3.equals("")){
                            gatewayIP_EditText[3].requestFocus();
                        } else if(subnet0.equals("")){
                            subnet_EditText[0].requestFocus();
                        } else if(subnet1.equals("")){
                            subnet_EditText[1].requestFocus();
                        } else if(subnet2.equals("")){
                            subnet_EditText[2].requestFocus();
                        } else if(subnet3.equals("")){
                            subnet_EditText[3].requestFocus();
                        } else if(mac0.equals("")){
                            Mac_EditText[0].requestFocus();
                        } else if(mac1.equals("")){
                            Mac_EditText[1].requestFocus();
                        } else if(mac2.equals("")){
                            Mac_EditText[2].requestFocus();
                        } else if(mac3.equals("")){
                            Mac_EditText[3].requestFocus();
                        } else if(mac4.equals("")){
                            Mac_EditText[4].requestFocus();
                        } else if(mac5.equals("")){
                            Mac_EditText[5].requestFocus();
                        }
                        toasting.toast("Usually the fields of the static items must all be filled.",
                                Toast.LENGTH_LONG, false);
                        return false;
                    }
                    if (IP0.equals("0")) {
                        toasting.toast("The static IP does not usually start with 0.", Toast.LENGTH_LONG, false);
                        IP_EditText[0].requestFocus();
                        return false;
                    }
                    //All 1's IP is probably rejected as well
                    if( IP0.equals("255") && IP1.equals("255") && IP2.equals("255") && IP3.equals("255") ) {
                        toasting.toast("The static IP is not usually all ones.", Toast.LENGTH_LONG, false);
                        IP_EditText[0].requestFocus();
                        return false;
                    }

                    if (gatewayIP0.equals("0")) {
                        toasting.toast("The gateway IP does not usually start with 0.", Toast.LENGTH_LONG, false);
                        gatewayIP_EditText[0].requestFocus();
                        return false;
                    }
                    if( gatewayIP0.equals("255") && gatewayIP1.equals("255") && gatewayIP2.equals("255") && gatewayIP3.equals("255") ) {
                        toasting.toast("The gateway IP is not usually all ones.", Toast.LENGTH_LONG, false);
                        gatewayIP_EditText[0].requestFocus();
                        return false;
                    }

                    /*
                    if (subnet0.equals("0")) {
                        toasting.toast("The subnet mask does not usually start with 0.", Toast.LENGTH_LONG, false);
                        subnet_EditText[0].requestFocus();
                        return false;
                    }
                    */
                    if(subnet0.equals("0") && (!subnet1.equals("0") || !subnet2.equals("0") || !subnet3.equals("0"))) {
                        toasting.toast("In subnet, following a 0 byte, there are non-0 bytes.", Toast.LENGTH_LONG, false);
                        subnet_EditText[0].requestFocus();
                        return false;
                    }
                    if(subnet1.equals("0") && (!subnet2.equals("0") || !subnet3.equals("0"))) {
                        toasting.toast("In subnet, following a 0 byte, there are non-0 bytes.", Toast.LENGTH_LONG, false);
                        subnet_EditText[1].requestFocus();
                        return false;
                    }
                    if(subnet2.equals("0") && !subnet3.equals("0")) {
                        toasting.toast("In subnet, following a 0 byte, there are non-0 bytes.", Toast.LENGTH_LONG, false);
                        subnet_EditText[2].requestFocus();
                        return false;
                    }
                    if(!( subnet0.equals("0") || subnet0.equals("128") || subnet0.equals("192") || subnet0.equals("224") ||
                            subnet0.equals("240") || subnet0.equals("248") || subnet0.equals("252") || subnet0.equals("254")
                            || subnet0.equals("255") )) {
                        toasting.toast("A subnet byte is usually one of the following :\n" +
                                "255, 254, 252, 248, 240, 224, 192, 128, or 0 (if followed by zeros).", Toast.LENGTH_LONG, false);
                        subnet_EditText[0].requestFocus();
                        return false;
                    }
                    if(!( subnet1.equals("0") || subnet1.equals("128") || subnet1.equals("192") || subnet1.equals("224") ||
                            subnet1.equals("240") || subnet1.equals("248") || subnet1.equals("252") || subnet1.equals("254")
                            || subnet1.equals("255") )) {
                        toasting.toast("A subnet byte is usually one of the following :\n" +
                                "255, 254, 252, 248, 240, 224, 192, 128, or 0 (if followed by zeros).", Toast.LENGTH_LONG, false);
                        subnet_EditText[1].requestFocus();
                        return false;
                    }
                    if(!(subnet2.equals("0") || subnet2.equals("128") || subnet2.equals("192") || subnet2.equals("224") ||
                            subnet2.equals("240") || subnet2.equals("248") || subnet2.equals("252") || subnet2.equals("254")
                            || subnet2.equals("255") )) {
                        toasting.toast("A subnet byte is usually one of the following :\n" +
                                "255, 254, 252, 248, 240, 224, 192, 128, or 0 (if followed by zeros).", Toast.LENGTH_LONG, false);
                        subnet_EditText[2].requestFocus();
                        return false;
                    }
                    if(!(subnet3.equals("0") || subnet3.equals("128") || subnet3.equals("192") || subnet3.equals("224") ||
                            subnet3.equals("240") || subnet3.equals("248") || subnet3.equals("252") || subnet3.equals("254")
                            || subnet3.equals("255") )) {
                        toasting.toast("A subnet byte is usually one of the following :\n" +
                                "255, 254, 252, 248, 240, 224, 192, 128, or 0.", Toast.LENGTH_LONG, false);
                        subnet_EditText[3].requestFocus();
                        return false;
                    }
                    if(!subnet0.equals("255") && !subnet0.equals("0") && !subnet1.equals("0") ) {
                        toasting.toast("In subnet masks, a non-full byte is usually followed by 0 bytes.",
                                Toast.LENGTH_LONG, false);
                        subnet_EditText[1].requestFocus();
                        return false;
                    }
                    if(!subnet1.equals("255") && !subnet1.equals("0") && !subnet2.equals("0") ) {
                        toasting.toast("In subnet masks, a non-full byte is usually followed by 0 bytes.",
                                Toast.LENGTH_LONG, false);
                        subnet_EditText[2].requestFocus();
                        return false;
                    }
                    if(!subnet2.equals("255") && !subnet2.equals("0") && !subnet3.equals("0") ) {
                        toasting.toast("In subnet masks, a non-full byte is usually followed by 0 bytes.",
                                Toast.LENGTH_LONG, false);
                        subnet_EditText[3].requestFocus();
                        return false;
                    }
                    /*Now bitwise multiplying the subnet with the gateway IP and then multiplying it back with the static IP
                    * and checking whether the results are the same.
                    */
                    if( bitwiseMultiply_loose( Integer.parseInt( subnet0 ), Integer.parseInt( IP0 ) ) !=
                            bitwiseMultiply_loose( Integer.parseInt( subnet0 ), Integer.parseInt( gatewayIP0 ) ) ) {
                        toasting.toast("The subnet mask does not seem to be compatible with the static and gateway IPs.",
                                Toast.LENGTH_LONG, false);
                        subnet_EditText[0].requestFocus();
                        return false;
                    }
                    if( bitwiseMultiply_loose( Integer.parseInt( subnet1 ), Integer.parseInt( IP1 ) ) !=
                            bitwiseMultiply_loose( Integer.parseInt( subnet1 ), Integer.parseInt( gatewayIP1 ) ) ) {
                        toasting.toast("The subnet mask does not seem to be compatible with the static and gateway IPs.",
                                Toast.LENGTH_LONG, false);
                        subnet_EditText[1].requestFocus();
                        return false;
                    }
                    if( bitwiseMultiply_loose( Integer.parseInt( subnet2 ), Integer.parseInt( IP2 ) ) !=
                            bitwiseMultiply_loose( Integer.parseInt( subnet2 ), Integer.parseInt( gatewayIP2 ) ) ) {
                        toasting.toast("The subnet mask does not seem to be compatible with the static and gateway IPs.",
                                Toast.LENGTH_LONG, false);
                        subnet_EditText[2].requestFocus();
                        return false;
                    }
                    if( bitwiseMultiply_loose( Integer.parseInt( subnet3 ), Integer.parseInt( IP3 ) ) !=
                            bitwiseMultiply_loose( Integer.parseInt( subnet3 ), Integer.parseInt( gatewayIP3 ) ) ) {
                        toasting.toast("The subnet mask does not seem to be compatible with the static and gateway IPs.",
                                Toast.LENGTH_LONG, false);
                        subnet_EditText[3].requestFocus();
                        return false;
                    }
                    if( max_sent_messages_number == 2 ) {
                        if( IP0.equals(obeying0IP0) && IP1.equals(obeying0IP1) && IP2.equals(obeying0IP2) && IP3.equals(obeying0IP3) ) {
                            toasting.toast("The static IP of this panel and that of a reflector panel" +
                                    " cannot be the same!", Toast.LENGTH_LONG, false);
                            obeyingIP0_EditText[3].requestFocus();
                            return false;
                        }
                        /*
                        //I commented because it must be allowed that a panel communicates
                        // locally with another panel on a different network.
                        if (bitwiseMultiply_loose(Integer.parseInt(subnet0), Integer.parseInt(obeying0IP0)) !=
                                bitwiseMultiply_loose(Integer.parseInt(subnet0), Integer.parseInt(gatewayIP0))) {
                            toasting.toast("The subnet mask does not seem to be compatible with a reflector static IP " +
                                    "and gateway IP", Toast.LENGTH_LONG, false);
                            subnet_EditText[0].requestFocus();
                            return false;
                        }
                        if (bitwiseMultiply_loose(Integer.parseInt(subnet1), Integer.parseInt(obeying0IP1)) !=
                                bitwiseMultiply_loose(Integer.parseInt(subnet1), Integer.parseInt(gatewayIP1))) {
                            toasting.toast("The subnet mask does not seem to be compatible with a reflector static IP " +
                                    "and gateway IP", Toast.LENGTH_LONG, false);
                            subnet_EditText[1].requestFocus();
                            return false;
                        }
                        if (bitwiseMultiply_loose(Integer.parseInt(subnet2), Integer.parseInt(obeying0IP2)) !=
                                bitwiseMultiply_loose(Integer.parseInt(subnet2), Integer.parseInt(gatewayIP2))) {
                            toasting.toast("The subnet mask does not seem to be compatible with a reflector static IP " +
                                    "and gateway IP", Toast.LENGTH_LONG, false);
                            subnet_EditText[2].requestFocus();
                            return false;
                        }
                        if (bitwiseMultiply_loose(Integer.parseInt(subnet3), Integer.parseInt(obeying0IP3)) !=
                                bitwiseMultiply_loose(Integer.parseInt(subnet3), Integer.parseInt(gatewayIP3))) {
                            toasting.toast("The subnet mask does not seem to be compatible with a reflector static IP " +
                                    "and gateway IP", Toast.LENGTH_LONG, false);
                            subnet_EditText[3].requestFocus();
                            return false;
                        }
                        */
                        if( reflectors_number_of_informing_local_panel != 1 ) {
                            if( IP0.equals(obeying1IP0) && IP1.equals(obeying1IP1) &&
                                    IP2.equals(obeying1IP2) && IP3.equals(obeying1IP3) ) {
                                toasting.toast("The static IP of this panel and that of a reflector panel" +
                                        " cannot be the same!", Toast.LENGTH_LONG, false);
                                obeyingIP1_EditText[3].requestFocus();
                                return false;
                            }
                            if( obeying0IP0.equals(obeying1IP0) && obeying0IP1.equals(obeying1IP1) &&
                                    obeying0IP2.equals(obeying1IP2) && obeying0IP3.equals(obeying1IP3) ) {
                                toasting.toast("The static IP of 2 reflector panels" +
                                        " cannot be the same!", Toast.LENGTH_LONG, false);
                                obeyingIP1_EditText[3].requestFocus();
                                return false;
                            }
                            /*
                            //I commented because it must be allowed that a panel communicates
                            // locally with another panel on a different network.
                            if (bitwiseMultiply_loose(Integer.parseInt(subnet0), Integer.parseInt(obeying1IP0)) !=
                                    bitwiseMultiply_loose(Integer.parseInt(subnet0), Integer.parseInt(gatewayIP0))) {
                                toasting.toast("The subnet mask does not seem to be compatible with a reflector static IP " +
                                        "and gateway IP", Toast.LENGTH_LONG, false);
                                subnet_EditText[0].requestFocus();
                                return false;
                            }
                            if (bitwiseMultiply_loose(Integer.parseInt(subnet1), Integer.parseInt(obeying1IP1)) !=
                                    bitwiseMultiply_loose(Integer.parseInt(subnet1), Integer.parseInt(gatewayIP1))) {
                                toasting.toast("The subnet mask does not seem to be compatible with a reflector static IP " +
                                        "and gateway IP", Toast.LENGTH_LONG, false);
                                subnet_EditText[1].requestFocus();
                                return false;
                            }
                            if (bitwiseMultiply_loose(Integer.parseInt(subnet2), Integer.parseInt(obeying1IP2)) !=
                                    bitwiseMultiply_loose(Integer.parseInt(subnet2), Integer.parseInt(gatewayIP2))) {
                                toasting.toast("The subnet mask does not seem to be compatible with a reflector static IP " +
                                        "and gateway IP", Toast.LENGTH_LONG, false);
                                subnet_EditText[2].requestFocus();
                                return false;
                            }
                            if (bitwiseMultiply_loose(Integer.parseInt(subnet3), Integer.parseInt(obeying1IP3)) !=
                                    bitwiseMultiply_loose(Integer.parseInt(subnet3), Integer.parseInt(gatewayIP3))) {
                                toasting.toast("The subnet mask does not seem to be compatible with a reflector static IP " +
                                        "and gateway IP", Toast.LENGTH_LONG, false);
                                subnet_EditText[3].requestFocus();
                                return false;
                            }
                            */
                        }
                    }

                    if( mac0.length() != 2 ) {
                        toasting.toast("Please make it 2 characters wide " +
                                "e.g. 3a, 0a, 00, bb, or 49.", Toast.LENGTH_LONG, false);
                        Mac_EditText[0].requestFocus();
                        return false;
                    }
                    if( mac1.length() != 2 ) {
                        toasting.toast("Please make it 2 characters wide " +
                                "e.g. 3a, 0a, 00, bb, or 49.", Toast.LENGTH_LONG, false);
                        Mac_EditText[1].requestFocus();
                        return false;
                    }
                    if( mac2.length() != 2 ) {
                        toasting.toast("Please make it 2 characters wide " +
                                "e.g. 3a, 0a, 00, bb, or 49.", Toast.LENGTH_LONG, false);
                        Mac_EditText[2].requestFocus();
                        return false;
                    }
                    if( mac3.length() != 2 ) {
                        toasting.toast("Please make it 2 characters wide " +
                                "e.g. 3a, 0a, 00, bb, or 49.", Toast.LENGTH_LONG, false);
                        Mac_EditText[3].requestFocus();
                        return false;
                    }
                    if( mac4.length() != 2 ) {
                        toasting.toast("Please make it 2 characters wide " +
                                "e.g. 3a, 0a, 00, bb, or 49.", Toast.LENGTH_LONG, false);
                        Mac_EditText[4].requestFocus();
                        return false;
                    }
                    if( mac5.length() != 2 ) {
                        toasting.toast("Please make it 2 characters wide " +
                                "e.g. 3a, 0a, 00, bb, or 49.", Toast.LENGTH_LONG, false);
                        Mac_EditText[5].requestFocus();
                        return false;
                    }
                    //the following is to prepare the message of the network configuration of this panel, which is mandatory
                    static_IP = IP0 + "." + IP1 + "." + IP2 + "." + IP3;
                    if(password.equals("")) {
                        /*
                        message = "5" + SSID +
                                trailor +
                                getTwoBytes( Short.parseShort(IP0), Short.parseShort(IP1) ) +
                                getTwoBytes( Short.parseShort(IP2), Short.parseShort(IP3) ) +
                                trailor +
                                getTwoBytes( Short.parseShort(gatewayIP0), Short.parseShort(gatewayIP1) ) +
                                getTwoBytes( Short.parseShort(gatewayIP2), Short.parseShort(gatewayIP3) ) +
                                trailor +
                                getTwoBytes( Short.parseShort(subnet0), Short.parseShort(subnet1) ) +
                                getTwoBytes( Short.parseShort(subnet2), Short.parseShort(subnet3) ) +
                                trailor +
                                mac0 + mac1 + mac2 + mac3 + mac4 + mac5 +
                                trailor;
                        */
                        /*
                    *the problem with the followed strategy is that some values entered by the user might actually be 0,
                    * and a byte which is 00000000 is '\0' will let the buffer cut itself and by consequence
                    * not the whole of it will be sent to the panel...
                    */
                        index_to_fill_message_byte = 0;
                        message_byte[index_to_fill_message_byte] = 6;
                        index_to_fill_message_byte++;
                        for(int i = 0; i < SSID.length(); i++ )
                            message_byte[ index_to_fill_message_byte + i ] = (byte) SSID.charAt(i);
                        index_to_fill_message_byte = index_to_fill_message_byte + SSID.length();
                        message_byte[index_to_fill_message_byte] = trailor;
                        index_to_fill_message_byte++;
                        encodeHeaderAndInsertIPsAndTrailor( (byte) Short.parseShort( IP0 ),
                                (byte) Short.parseShort( IP1 ), (byte) Short.parseShort( IP2 ), (byte) Short.parseShort( IP3 ) );
                        /*cannot use Byte.parseByte(IP0) because
                        IP0 might be above 127 and Byte is between -128 and 127, so it might throw an error*/
                        /*Please note that the decimal representation of (byte) Short.parseShort(IP0) will probably (didn't try it) be
                        * a negative number here but it won't be negative in arduino; it'll be the correct value.*/
                        index_to_fill_message_byte++;
                        encodeHeaderAndInsertIPsAndTrailor( (byte) Short.parseShort( gatewayIP0 ),
                                (byte) Short.parseShort( gatewayIP1 ), (byte) Short.parseShort( gatewayIP2 ),
                                (byte) Short.parseShort( gatewayIP3 ) );
                        index_to_fill_message_byte++;
                        encodeHeaderAndInsertIPsAndTrailor( (byte) Short.parseShort( subnet0 ),
                                (byte) Short.parseShort( subnet1 ), (byte) Short.parseShort( subnet2 ),
                                (byte) Short.parseShort( subnet3 ) );
                        index_to_fill_message_byte++;
                        message_byte[index_to_fill_message_byte] = (byte) mac0.charAt(0);
                        index_to_fill_message_byte++;
                        message_byte[index_to_fill_message_byte] = (byte) mac0.charAt(1);
                        index_to_fill_message_byte++;
                        message_byte[index_to_fill_message_byte] = (byte) mac1.charAt(0);
                        index_to_fill_message_byte++;
                        message_byte[index_to_fill_message_byte] = (byte) mac1.charAt(1);
                        index_to_fill_message_byte++;
                        message_byte[index_to_fill_message_byte] = (byte) mac2.charAt(0);
                        index_to_fill_message_byte++;
                        message_byte[index_to_fill_message_byte] = (byte) mac2.charAt(1);
                        index_to_fill_message_byte++;
                        message_byte[index_to_fill_message_byte] = (byte) mac3.charAt(0);
                        index_to_fill_message_byte++;
                        message_byte[index_to_fill_message_byte] = (byte) mac3.charAt(1);
                        index_to_fill_message_byte++;
                        message_byte[index_to_fill_message_byte] = (byte) mac4.charAt(0);
                        index_to_fill_message_byte++;
                        message_byte[index_to_fill_message_byte] = (byte) mac4.charAt(1);
                        index_to_fill_message_byte++;
                        message_byte[index_to_fill_message_byte] = (byte) mac5.charAt(0);
                        index_to_fill_message_byte++;
                        message_byte[index_to_fill_message_byte] = (byte) mac5.charAt(1);
                        index_to_fill_message_byte++;
                        message_byte[index_to_fill_message_byte] = trailor;
                    } else {
                      /*
                        message = "6" + SSID +
                                trailor +
                                password +
                                trailor +
                                getTwoBytes( Short.parseShort(IP0), Short.parseShort(IP1) ) +
                                getTwoBytes( Short.parseShort(IP2), Short.parseShort(IP3) ) +
                                trailor +
                                getTwoBytes( Short.parseShort(gatewayIP0), Short.parseShort(gatewayIP1) ) +
                                getTwoBytes( Short.parseShort(gatewayIP2), Short.parseShort(gatewayIP3) ) +
                                trailor +
                                getTwoBytes( Short.parseShort(subnet0), Short.parseShort(subnet1) ) +
                                getTwoBytes( Short.parseShort(subnet2), Short.parseShort(subnet3) ) +
                                trailor +
                                mac0 + mac1 + mac2 + mac3 + mac4 + mac5 +
                                trailor;
                        //nor this is suitable...
                        message = "6" + SSID +
                                trailor +
                                password +
                                trailor +
                                (byte) Short.parseShort(IP0) + (byte) Short.parseShort(IP1) +
                                (byte) Short.parseShort(IP2) + (byte) Short.parseShort(IP3) +
                                trailor +
                                (byte) Short.parseShort(gatewayIP0) + (byte) Short.parseShort(gatewayIP1) +
                                (byte) Short.parseShort(gatewayIP2) + (byte) Short.parseShort(gatewayIP3) +
                                trailor +
                                (byte) Short.parseShort(subnet0) + (byte) Short.parseShort(subnet1) +
                                (byte) Short.parseShort(subnet2) + (byte) Short.parseShort(subnet3) +
                                trailor +
                                mac0 + mac1 + mac2 + mac3 + mac4 + mac5 +
                                trailor;
                      */
                        index_to_fill_message_byte = 0;
                        message_byte[index_to_fill_message_byte] = 7;
                        index_to_fill_message_byte++;
                        for(int i = 0; i < SSID.length(); i++ )
                            message_byte[ index_to_fill_message_byte + i ] = (byte) SSID.charAt(i);
                        index_to_fill_message_byte = index_to_fill_message_byte + SSID.length();
                        message_byte[index_to_fill_message_byte] = trailor;
                        index_to_fill_message_byte++;
                        for(int i = 0; i < password.length(); i++ )
                            message_byte[ index_to_fill_message_byte + i ] = (byte) password.charAt(i);
                        index_to_fill_message_byte = index_to_fill_message_byte + password.length();
                        message_byte[index_to_fill_message_byte] = trailor;
                        index_to_fill_message_byte++;
                        encodeHeaderAndInsertIPsAndTrailor( (byte) Short.parseShort( IP0 ),
                                (byte) Short.parseShort( IP1 ), (byte) Short.parseShort( IP2 ), (byte) Short.parseShort( IP3 ) );
                        index_to_fill_message_byte++;
                        encodeHeaderAndInsertIPsAndTrailor( (byte) Short.parseShort( gatewayIP0 ),
                                (byte) Short.parseShort( gatewayIP1 ), (byte) Short.parseShort( gatewayIP2 ),
                                (byte) Short.parseShort( gatewayIP3 ) );
                        index_to_fill_message_byte++;
                        encodeHeaderAndInsertIPsAndTrailor( (byte) Short.parseShort( subnet0 ),
                                (byte) Short.parseShort( subnet1 ), (byte) Short.parseShort( subnet2 ),
                                (byte) Short.parseShort( subnet3 ) );
                        index_to_fill_message_byte++;
                        message_byte[index_to_fill_message_byte] = (byte) mac0.charAt(0);
                        index_to_fill_message_byte++;
                        message_byte[index_to_fill_message_byte] = (byte) mac0.charAt(1);
                        index_to_fill_message_byte++;
                        message_byte[index_to_fill_message_byte] = (byte) mac1.charAt(0);
                        index_to_fill_message_byte++;
                        message_byte[index_to_fill_message_byte] = (byte) mac1.charAt(1);
                        index_to_fill_message_byte++;
                        message_byte[index_to_fill_message_byte] = (byte) mac2.charAt(0);
                        index_to_fill_message_byte++;
                        message_byte[index_to_fill_message_byte] = (byte) mac2.charAt(1);
                        index_to_fill_message_byte++;
                        message_byte[index_to_fill_message_byte] = (byte) mac3.charAt(0);
                        index_to_fill_message_byte++;
                        message_byte[index_to_fill_message_byte] = (byte) mac3.charAt(1);
                        index_to_fill_message_byte++;
                        message_byte[index_to_fill_message_byte] = (byte) mac4.charAt(0);
                        index_to_fill_message_byte++;
                        message_byte[index_to_fill_message_byte] = (byte) mac4.charAt(1);
                        index_to_fill_message_byte++;
                        message_byte[index_to_fill_message_byte] = (byte) mac5.charAt(0);
                        index_to_fill_message_byte++;
                        message_byte[index_to_fill_message_byte] = (byte) mac5.charAt(1);
                        index_to_fill_message_byte++;
                        message_byte[index_to_fill_message_byte] = trailor;
                    }
                    //Log.i("Config...", "Youssef/ " + message);
                    /*message in Log.i will appear falsely represented, but don't be worried about it, in short data types,
                    * all numbers like 1000010010101010 starting with 1 are considered negative and complemented by 2 and you'll
                    * see the '-' sign.
                    * and all numbers like 0000010010101010 won't be complemented by 2.
                    * What's important is the bits that are sent and interpreted by Arduino.
                    */

                }
                if( !local_checkBox.isChecked() && !internet_checkBox.isChecked() ) {
                    local_checkBox.requestFocus(); //not working for some reason
                    toasting.toast("Please make sure you have selected how the panel communicates with the system.\n" +
                            "You must select at least one of the 2 boxes : \"Local Network\" or \"Internet\".",
                            Toast.LENGTH_LONG, false);
                    return false;
                }

                //here comes the info of the local or internet...
                index_to_fill_message_byte++;
                if( local_checkBox.isChecked() && !internet_checkBox.isChecked() ) {
                    message_byte[index_to_fill_message_byte] = 'l';
                } else if( !local_checkBox.isChecked() && internet_checkBox.isChecked() ) {
                    message_byte[index_to_fill_message_byte] = 'i';
                } else if( local_checkBox.isChecked() && internet_checkBox.isChecked() ) {
                    message_byte[index_to_fill_message_byte] = 'b';
                }

                index_to_fill_message_byte++;
                message_byte[index_to_fill_message_byte] = trailor;

                index_to_fill_message_byte++;
                message_byte[index_to_fill_message_byte] = '\0';

                return true;
            }

            private int bitwiseMultiply_loose( int n1, int n2 ) { /*I named it loose because it's a not bitwise and really,
                * but it does the job right since we use it in comparing only.
                * You would obtain something like 100011110, while in reality you should omit the MSB and LSB and flip the remaining
                * in between, thus the real result is 11110000.
                */
                int a = n1;
                int b = n2;
                int result = 1;
                while( (b != 0) && (a != 0) ) /*at most this will be entered 8 times, no more; because n1 and n2 are actually 8 bits size each of
                                Short format*/
                {
                    if( (b & 1) != 0 && (a & 1) != 0 ) {
                        result = result + 1;
                    }
                    //Log.i("Youssef config.j", "looping in bitwise and result was : " + result);
                    result = result << 1; //one time left shifted and placing 0 as the least significant bit
                    a = a >>> 1;
                    b = b >>> 1; //one time right and placing 0 (as well) as the most significant bit
                }
                Log.i("Youssef config.j", "result of bitwise and is " + result);
                return result;
            }
            /*
            private String getTwoBytes( Short higher_byte, Short lower_byte ) { /*what is meant by this method is to have
            * precisely 2 bytes.
                higher_byte = (short) (higher_byte << 8); //the first byte of this shifted Short is all zeros
                return String.valueOf( higher_byte + lower_byte ); //even "bitwise or" which is | works fine as well
            }
            */
            private AfterSetButtonPressed() { //it's really interesting how this can be private and still be used outside!

                final Button set_button = findViewById( R.id.Config_Set_button );

                set_button.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        if( isAllTextsValid() ) {/*isAllTextsValid() also sets the message(s) to be sent*/
                            /*I have to declare the following 2 variables here since ta thread can only be started once
                            * */
                            ConnectToPanel connectToPanel_obeyIP =  new ConnectToPanel( ConnectToPanel.obeyIP_Port );
                            ConnectToPanel connectToPanel_networkConf =  new ConnectToPanel( ConnectToPanel.NetworkConf_Port );
                            if( connectToPanel_networkConf.still_sending || connectToPanel_networkConf.still_waiting ||
                                    connectToPanel_obeyIP.still_sending || connectToPanel_obeyIP.still_waiting ) {
                                toasting.toast("Please wait a few seconds...", Toast.LENGTH_LONG, false);
                                return;
                            }
                            /*We have checked whether the values of the text boxes are valid. If ok and if there's an IP and if it was a panel
                             * that this mobile app will communicate to locally, then we will save the info in a shared prefs and will
                             * send the network configuration in a best effort manner to the panel. (We are not getting an acknowledge from
                             * the panel whether info were saved correctly or not. And this is not a big problem; user can always try again.)
                             */
                            /* If max_sent_messages_number is 2 then the 2 messages are set and ready by now
                             */

                            final String other_panel_index = bundle.getString("otherPanelIndex"); //This is no longer needed and not used anymore, but left only for compatibility reasons

                            /*
                            if( //!static_IP.equals("") && //I want to allow to set the static IP to nothing. E.g. panel had a static IP and now it doesn't.
                                    !panel_index.equals( "" ) && //This may be made on purpose, e.g. unmemorizeInMobile_checkBox is checked.
                                    !panel_index.equals( other_panel_index ) && //should never happen; I don't rely onother_panel_index anymore.
                                    !unmemorizeInMobile_checkBox.isChecked() ) //that is perfectly fine now.
                                    {
                                    //value of static_IP is determined in isAllTextsValid()
                                prefs_editor.putString("staticIP", static_IP).apply();
                            }
                        */
                            //value of static_IP is determined in isAllTextsValid()
                            if( !panel_index.equals( other_panel_index ) ) { //which is now the case after I cancelled the other_panel_index
                                prefs_editor.putString("staticIP", static_IP).apply();
                            }

                            connectToPanel_networkConf.start();

                            if( max_sent_messages_number == 2 ) {
                                connectToPanel_obeyIP.start();
                            }
                            //connectToPanel_networkConf = null; //not needed for now
                            //connectToPanel_obeyIP = null; //not needed for now
                        }
                    }
                });

            }
        }

        new AfterSetButtonPressed();

        final View backToWelcome_Button = findViewById(R.id.button_backToMainFromConfig);
        backToWelcome_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //startActivity(new Intent(getApplicationContext(), readButton.WelcomeActivity.class));
                finish();
            }
        });

    }

    void setSharedPreferencesVariables() {
        network_prefs = getSharedPreferences(panel_index + "_networkConfig", 0); /*this shared preference
         *not only serves for static IPs gotten in activities of panels, but also it serves to remember the last
         *entered values of edit texts by the user*/
        prefs_editor = network_prefs.edit();
    }

    void setTitleWithPanelName() {
        setTitle( "Network Configuration - " + panel_name );
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        try {
            imm.hideSoftInputFromWindow(getWindow().getCurrentFocus().getWindowToken(), 0);
        } catch (Exception e) {
            Log.i("Youssef Conf..", "exception thrown in hideKeyboard()");
        }
    }
    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        try{
            imm.showSoftInput(getWindow().getCurrentFocus(), 0);
        } catch (Exception e) {
            Log.i("Youssef Conf..", "exception thrown in showKeyboard()");
        }
    }
    /*
    private boolean isKeyboardHidden(){
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        return imm.isActive();
    }
    */

    /*The following method is to hide the keyboard when the user presses outside of an EditText.
     *It also solves a problem for the OK button dedicated to rectify the user's input (like IP and MAC)
     */
    @Override
    public boolean dispatchTouchEvent( MotionEvent event ) {

        View v = getCurrentFocus(); //from this I understand that this event method is triggered before moving to a new focus
        boolean ret = super.dispatchTouchEvent(event);
        if( v == null ) {
            return ret;
        }

        int[] scrcoords = new int[2];
        v.getLocationOnScreen( scrcoords );
        float x = event.getRawX() + v.getLeft() - scrcoords[0];
        float y = event.getRawY() + v.getTop() - scrcoords[1];

        /*This block is made because warnUser_OkButton onClick listener never worked! So I relied on this trick. Focus was not a problem though! The problem was the click lisener.
            Focus used to escape the OK button to the scrollView, then these 2 lines in TextWatcher fixed it
            scrollView.setFocusable(false);
            scrollView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            So again, focus always wanted to escape after it was correctly acquired by the button.
        */
        final LinearLayout warnUser_layout = findViewById( R.id.Config_warnUser_layout );
        final ScrollView scrollView = findViewById( R.id.Config_scroll );
        final Button warnUser_OkButton = findViewById( R.id.Config_warnUser_buttonOk );
        if( v == warnUser_OkButton ) {
            if (event.getAction() == MotionEvent.ACTION_UP && (x >= v.getLeft() && x <= v.getRight() && y >= v.getTop() && y <= v.getBottom())) {
                //Log.i("Youssef Config.java", "onClick event is on!!");
                warnUser_layout.setVisibility(View.GONE);
                scrollView.setAlpha( 1 );
                if( editable != null ) {
                    editable.clear();
                }
                scrollView.setFocusable(true);
                scrollView.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
                if( editText != null){
                    editText.requestFocus();
                }
                showKeyboard();
            }
        }

        //This block is made to clear focus of EditTexts
        if( v instanceof EditText) {
            View w = getCurrentFocus();
            //Log.d("Activity", "Touch event "+event.getRawX()+","+event.getRawY()+" "+x+","+y+" rect "+w.getLeft()+","+w.getTop()+","+w.getRight()+","+w.getBottom()+" coords "+scrcoords[0]+","+scrcoords[1]);
            try {
                if (event.getAction() == MotionEvent.ACTION_UP && (x < w.getLeft() || x >= w.getRight() || y < w.getTop() || y > w.getBottom())) {
                    // hideKeyboard();
                    //you can make the cursor disappear by putting the following below without the need
                    //to struggle with xml file...
                    SSID_EditText.clearFocus();
                    Password_EditText.clearFocus();
                    localPort1_EditText.clearFocus();
                    localPort2_EditText.clearFocus();
                    internetPort1_EditText.clearFocus();
                    internetPort2_EditText.clearFocus();
                    for (int i = 0; i < IP_Portions_Number; i++) {
                        obeyingIP0_EditText[i].clearFocus();
                        obeyingIP1_EditText[i].clearFocus();
                        IP_EditText[i].clearFocus();
                        gatewayIP_EditText[i].clearFocus();
                        subnet_EditText[i].clearFocus();
                        internetIP_EditText[i].clearFocus();
                    }
                    for (int i = 0; i < MAC_Portions_Number; i++)
                        Mac_EditText[i].clearFocus();

                }
            } catch (Exception e) {
                Log.i("Config...", "Youssef/ NPE was thrown, caused by w.getLeft() ");
            }
        }
        return ret;
    }

    public static void expand(final View v) {
        int matchParentMeasureSpec = View.MeasureSpec.makeMeasureSpec(((View) v.getParent()).getWidth(), View.MeasureSpec.EXACTLY);
        int wrapContentMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        v.measure(matchParentMeasureSpec, wrapContentMeasureSpec);
        final int targetHeight = v.getMeasuredHeight();

        // Older versions of android (pre API 21) cancel animations for views with a height of 0.
        v.getLayoutParams().height = 1;
        v.setVisibility(View.VISIBLE);
        Animation a = new Animation()
        {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                v.getLayoutParams().height = interpolatedTime == 1
                        ? ViewGroup.LayoutParams.WRAP_CONTENT
                        : (int)(targetHeight * interpolatedTime);
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        // Expansion speed of 1dp/ms
        a.setDuration((int)(targetHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }
    /*
    protected void onSaveInstanceState(Bundle extra) {
        super.onSaveInstanceState(extra);
        extra.putString("text", "your text here");
    }
    */
}
