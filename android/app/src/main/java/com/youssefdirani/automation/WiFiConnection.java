package com.youssefdirani.automation;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.widget.Toast;

public abstract class WiFiConnection {
    private static WifiManager wifiManager;

    /*
        static void turnWiFiOff() {
            wifiManager.setWifiEnabled(false);
        }

        static boolean isWiFiOn(){
            return(wifiManager.isWifiEnabled());
        }
    */
    public static boolean wiFiValid(Context applicationContext, boolean silentToast, Toasting toasting, boolean localNotInternet ) {
        if( !localNotInternet ) {
            return true;
        }
        wifiManager = (WifiManager) applicationContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        boolean isValid = false;
        boolean wiFiOff = ssidState(applicationContext);
        if (wiFiOff) {
            toasting.toast("Please turn on your WiFi...", Toast.LENGTH_SHORT, silentToast);
        } else {
            isValid = true;
        }
        return (isValid);
    }

    private static boolean ssidState(Context appContext) {
        //setDictionary();
        String currentSsid = getCurrentSsid(appContext);
        //I removed some of the old code. Anyway there must be a better mechanism to know if we're local or foreign.
        return( TextUtils.isEmpty(currentSsid) );
    }

    private static String getCurrentSsid(Context context) {
        //Context context = getApplicationContext();
        //it seems like getApplicationContext is the best parameter.
        //since later when context.getSystemService(Context.WIFI_SERVICE); is called, other contexts may cause memory leaks?!
        String ssid = null;
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo == null) {
            return null;
        }

        if (networkInfo.isConnected()) {
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
                if (connectionInfo != null) {
                    if (!TextUtils.isEmpty(connectionInfo.getSSID())) {
                        ssid = connectionInfo.getSSID(); //this returns extra quotes at the beginning and end of it.
                        ssid = ssid.replaceAll("^\"(.*)\"$", "$1"); //this removes the quotes
                    }
                }
            }
        }
        return ssid;
    }

}

