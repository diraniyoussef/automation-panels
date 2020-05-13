package com.youssefdirani.automation;

import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;

import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
//import androidx.fragment.app.Fragment;
//import androidx.fragment.app.FragmentTransaction;
import androidx.drawerlayout.widget.DrawerLayout;
//import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import android.view.Menu;
import android.widget.ToggleButton;

import com.google.android.material.navigation.NavigationView;
//import com.youssefdirani.temphum_v04.ui.temphum.TempHumFragment;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    final public Toasting toasting = new Toasting( this );
    public String panel_index, panel_name, panel_type;

    private Toolbar toolbar;
    public String getPanel_name() {
        panel_name = toolbar.getTitle().toString();
        return panel_name;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        */
         //I commented these just to remove the menu on the top left
        DrawerLayout drawer = findViewById(R.id.drawer_layout);

        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_temphum, R.id.nav_waterheater, R.id.nav_gate,
                R.id.nav_understairs, R.id.nav_share, R.id.nav_send)
                .setDrawerLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        /* //this is working (tested and worked). This is not needed.
        NavDestination currentDestination = navController.getCurrentDestination();
        if( currentDestination != null ) {
            int navControllerDestinationId = currentDestination.getId();
            if ( navControllerDestinationId == R.id.nav_temphum) {
                Log.i("MainAct..", "Youssef, navControllerDestinationId is well known and reachable");
            } else {
                Log.i("MainAct..", "Youssef, navControllerDestinationId is not reachable");
            }
        } else {
            Log.i("MainAct..", "Youssef, navControllerDestinationId is null");
        }
        */
    }

    public ToggleButton localInternet_toggleButton;
//    public TempHumFragment shownFragmentLayout; //if you want to control the fragment class here from.

    protected void onResume() {
        super.onResume();
        /*You might argue that do I need to put this toggle button here instead of in the fragment ?
        * Well, it's also very logical to put it in each fragment instead of here.
        * Same question would go with the refresh button.
         */
        //Log.i("MainAct...", "Youssef/ Entering resume");//clearly only entered once even when navigating through fragment layouts

        localInternet_toggleButton = findViewById(R.id.toggleButton_internet_local);
        updateToggleButtonShape( localInternet_toggleButton );
        localInternet_toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("MainAct", "Youssef/ inside tb setonclickListener");
                updateToggleButtonShape( localInternet_toggleButton );
            }
        });
        /*
        //Log.i("MainAct...", "Youssef/ fragments size is " + getSupportFragmentManager().getFragments().size() ); //working and value is 1
        TempHumFragment fragment_tempHum = (TempHumFragment) getSupportFragmentManager().getFragments().get(0); //ok
        Fragment fragment_tempHum = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if( fragment_tempHum != null ) {
            Log.i("MainAct...", "Youssef/ fragment accessible and its tag is " + fragment_tempHum.getTag() );
        } else {
            Log.i("MainAct...", "Youssef/ fragment is null");
        }
        Log.i("MainAct...", "Youssef/ Id of nav_host_fragment is " +
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment).getId() );
        */

    }

    private void updateToggleButtonShape( ToggleButton tb ) {
        if( tb == null ) return; //just protection
        if(tb.isChecked()) { //internet
            tb.setTextColor( getTextColorPrimary() );
            Log.i("HumTemp MainAct", "trying local connection");
        } else {//local
            tb.setTextColor(Color.parseColor("#D81B60"));
            Log.i("HumTemp MainAct", "trying internet connection");
        }
    }

    private int getTextColorPrimary() { //getting text_color_primary. I could had simply searched for the color number, but it's ok.
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = this.getTheme();
        theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        TypedArray arr = this.obtainStyledAttributes(typedValue.data, new int[]{
                android.R.attr.textColorPrimary
        });
        int primaryColor = arr.getColor(0, -1);
        arr.recycle();
        return primaryColor;
    }


    //public MutableLiveData<String> localNotInternet = new MutableLiveData<>(); //this is to propagate the value to the fragment
    public String owner_part = "zaher_house";
    //String mob_part = "S7_Edge";
    //String mob_part = "S4";
    public String mob_part = "mob1"; //usr2 is kept in the store
    //String mob_part = "Mom_Tab";
    public String mob_Id = owner_part + ":" + mob_part + ":";

    @Override
    public boolean onCreateOptionsMenu(Menu menu) { //This is for the menu on the top right I guess
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.about_app:
                startActivity(new Intent(getApplicationContext(), AppDescription.class));
                return true;
            case R.id.configuration:
                final Intent intent = new Intent();
                final String other_panel_index = "-1"; //I can make it any value but -1 is preferred
                intent.putExtra("panelName", panel_name );
                intent.putExtra("panelIndex", panel_index ); //panel index will be used to set the static IP.
                //if the panel index corresponds to this "other panel" we won't assign then any static IP.
                intent.putExtra("panelType", panel_type );
                intent.putExtra("otherPanelIndex", other_panel_index ); /*used to compare the panel index with
                    this extra panel index. This is not relevant anymore. I'm only keeping it for compatibility but not used.*/
                intent.setClass(getApplicationContext(), ConfigPanel.class);
                startActivity(intent);

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();

     //   return false; //I added this
    }

    @Override
    public void onBackPressed()
    {
        finish();
        super.onBackPressed();  // optional depending on your needs
    }
}

