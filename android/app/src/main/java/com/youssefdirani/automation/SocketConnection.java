package com.youssefdirani.automation;

import android.content.Context;
import android.util.Log;

import android.widget.Toast;

import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SocketConnection { //some methods are not static in this class, this is why it is not made static.
    //My 2 sockets issue**********************************
    static int maxClientsNumber = 2; //this should be constant
    volatile Socket[] client = new Socket[maxClientsNumber];
    volatile int active_client_index = -1; //convention is that the value is either 0 or 1 and that it starts with -1
    volatile PrintWriter[] printWriter = new PrintWriter[maxClientsNumber];
    //****************************************************
    ServerConfig selectedServerConfig; //being protected instead of private is useful only in one place, but still is not so interesting.
    private static int i;
    private CommunicateWithServer communication;

    private volatile boolean newSocketIsCurrentlyUnderCreation = false;
    private volatile boolean socketCreationSuccessful;
    private Toasting toasting;
    static private PeriodicCheck_Data socket_waitingData = new PeriodicCheck_Data(50,1500);
    static PeriodicCheck_Data read_waitingData = new PeriodicCheck_Data(20,3000);

    private static boolean silentToast;

    private boolean localNotInternet;

    public SocketConnection(Toasting toasting, boolean isSilentToast, MainActivity act, Context applicationContextParam,
                     ServerConfig selectedServerConfig_arg, int number_of_data_chunks, boolean localNotInternet,
                            String owner_part, String mob_part, String mob_Id) { //constructor
        this.localNotInternet = localNotInternet;
        selectedServerConfig = selectedServerConfig_arg;
        communication = new CommunicateWithServer( toasting, act, applicationContextParam, this,
                selectedServerConfig.getPanelIndex(), selectedServerConfig.getPanelName(), number_of_data_chunks,
                localNotInternet, owner_part, mob_part, mob_Id );
        this.toasting = toasting;
        silentToast = isSilentToast;
    }

    //The purpose of this method is to message the server whenever we have a socket...
    public void socketConnectionSetup(String message) {
        if (active_client_index == -1) {//only the first time
            active_client_index = 0;
            //disableSwitches(); //user recognizes disabling so no need to toast.
            //if (createNewSocketAndEnableSwitches(active_client_index)) {
            if (createNewSocket(active_client_index)) {
                Log.i("Youssef Serv...java", "finished creating a new socket client 0." +
                        " For panel " + selectedServerConfig.getPanelName());
                communication.delayToManipulateSockets.startTiming();
                communication.setThreads(message, false);
            }
        } else {
            Log.i("Youssef Serv...java", "to initiate a new order without creating initially a new socket."
                    + " For panel " + selectedServerConfig.getPanelName());
            communication.setThreads(message, true);
        }
    }

    static int nextClientIndex(int i) {
        if (i < maxClientsNumber - 1){
            i++;
        } else { //i == maxClientsNumber - 1
            i = 0;
        }
        return(i);
    }

    void destroySocket(int client_index) { //No need to kill the bufferThread, bufferedReader will simply update with the new socket.
        //testAndFixConnection thread also isn't available to be killed in the first place!
        // Other threads just continue their work with no real harm and finish peacfully.
        try {
            communication.null_bufferThread(client_index); //interesting since I want to kill the thread.

            if (printWriter[client_index] != null){
                printWriter[client_index].close(); //returns void
                printWriter[client_index] = null;
            }
            if (client[client_index] != null) {
                if(client[client_index].isConnected()) //I added this line on 06062019
                    client[client_index].close(); //returns void    //this actually shows to the server since it's a proper closing of a socket.
                //client[client_index] = null;
            }
            Log.i("SocketConn...","Youssef/ Now socket " + client_index + " is destroyed." +
                    " For panel " + selectedServerConfig.getPanelName());
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("SocketConn...","Youssef/ Error in closing socket, printWriter, or reader................"
                    + " For panel " + selectedServerConfig.getPanelName());
        }
    }
/*
    void destroyAllSockets() {
        communication.delayToManipulateSockets.cancelTimer(); //usually there is a 2 minutes timer to manipulate the sockets, this should be cancelled.
        //it's like         communication.delayToManipulateSockets.cancelTimer();
        communication.destroySocketTimer.cancelTimer(); //there might be one socket waiting to be destroyed after sometime, so I will cancel the timer and close the socket
        //in the current method.
        destroySocket(0);
        destroySocket(1);
        active_client_index = -1;
    }
*/
    boolean createNewSocket(int client_index_param) {
        //createSocketAsyncTaskInstance.execute();
        socketCreationSuccessful = true;
        newSocketIsCurrentlyUnderCreation = true;
        //I made the object instance createSocketThreadInstance local to the createNewSocket method (it's pretty safe to me to make it local since the CreateSocket thread
        // will finish before this createNewSocket method ends -- even though I guess the current method doesn't end as long as there is a thread running from it.)
        CreateSocket createSocketThreadInstance = new CreateSocket(client_index_param);
        createSocketThreadInstance.start();
        //execution after AsyncTask goes to afterSocketCreation()
        //I'm joining the thread now... Maybe thread.join was a better choice. Anyway.
        int iterationTimeout = 0; //this variable can change, right?????????????????????????

        while ((iterationTimeout < socket_waitingData.maxIterations) && (newSocketIsCurrentlyUnderCreation)) {
            iterationTimeout = iterationTimeout + 1;
            try {
                Thread.sleep(socket_waitingData.divisionInterval);
            } catch (Exception e) {
                //Thread.currentThread().interrupt();
                e.printStackTrace();
                Log.i("Youssef Serv...java","Error: Sleeping in createNewSocket." + " For panel " + selectedServerConfig.getPanelName());
            }
        }
        if (iterationTimeout == socket_waitingData.maxIterations) { //wrongly it was before read_waitingData.maxIterations. Even now I'm not sure if it's needed
            //kill the asyncTask although not needed.
            newSocketIsCurrentlyUnderCreation = false;
            if( localNotInternet ) {
                toasting.toast("Connection failed presumably for panel " + selectedServerConfig.getPanelName() + "\n" +
                        "You may check if electricity is down on the module.", Toast.LENGTH_SHORT, silentToast);
            } else {
                toasting.toast("Couldn't connect to server.\n" +
                        " Please check Internet connection of the mobile.", Toast.LENGTH_LONG, silentToast);
                //Really, when connected to the internet through the data conection of the mobile operator, if  the signal is weak, it may not connect to the
                // server to establish a connected socket
            }
            if(client[client_index_param] != null) {
                try {
                    client[client_index_param].close();
                } catch (Exception e) {
                    //Thread.currentThread().interrupt();
                    e.printStackTrace();
                    Log.i("Youssef Serv...java", "Error: closing socket." + " For panel index " + selectedServerConfig.getPanelIndex());
                }
            }
            return (false);
        } else {
            if (socketCreationSuccessful){
                return (true);
            } else {
                if(client[client_index_param] != null) {
                    try {
                        client[client_index_param].close();
                    } catch (Exception e) {
                        //Thread.currentThread().interrupt();
                        e.printStackTrace();
                        Log.i("Youssef Serv...java", "Error: closing socket." + " For panel index " + selectedServerConfig.getPanelIndex());
                    }
                }
                return (false);
            }
        }
    }

    class CreateSocket extends Thread {
        int index;
        CreateSocket(int client_index_param){
            index = client_index_param;
        }

        @Override
        public void run() {
            try {
                i++; //only for debugging
                if( i == 10 ) {
                    i = 0;//I don't want i to grow indefinitely and potentially cause an overflow.
                }
                Log.i("Youssef Serv...java", "This is the " + i + "th time we enter in CreateSocket");
                //client = new Socket(wiFiConnection.chosenIPConfig.staticIP, port); //should be further developed.
                //client.connect(new InetSocketAddress(wiFiConnection.chosenIPConfig.staticIP, port), 1500);
                client[index] = new Socket();
                Log.i("SocketConnec...", "Youssef/ client[index " + index + "] is fine to connect to IP " +
                        selectedServerConfig.getStaticIP() + " on port " + selectedServerConfig.getPortFromIndex(index));
                //client.connect(new InetSocketAddress("192.168.4.201", port),1500);
                client[index].connect( new InetSocketAddress(selectedServerConfig.getStaticIP(),
                        selectedServerConfig.getPortFromIndex(index)), socket_waitingData.timeout );

                //client.setSoTimeout(0); //no need to set it to infinite since all it does, if it were not infinite, is to throw an exception; it does not affect the socket.
                Log.i("Youssef Serv...java", "Socket " + index + " " +
                        "is connected, for panel index " + selectedServerConfig.getPanelIndex() + " on port " +
                        "selectedServerConfig.getPortFromIndex(index)");

                printWriter[index] = new PrintWriter( client[index].getOutputStream() );
                Log.i("Youssef Serv...java", "New printWriter is made, for panel index " + selectedServerConfig.getPanelIndex());

                communication.renew_bufferThread(index);
                //Log.i("Youssef Serv...java", "New bufferThread is made." + " For panel  index " + selectedServerConfig.panel_index);

            } catch (Exception e) {//I hope this includes the IOException or the UnknownHostException because this will be thrown
                //in case the IP is wrong or the electricity on module is down.
                e.printStackTrace();
                if(client[index] != null) {
                    try {
                        client[index].close();
                    } catch (Exception exc) {
                        //Thread.currentThread().interrupt();
                        exc.printStackTrace();
                        Log.i("Youssef Serv...java", "Error: closing socket." + " For panel index " + selectedServerConfig.getPanelIndex());
                    }
                }
                //it's probably better to call      destroySocket(index);       but to be checked later.
                Log.i("Youssef Serv...java", "Exception is thrown. For panel index " + selectedServerConfig.getPanelIndex() +
                        " on port " + selectedServerConfig.getPortFromIndex(index));
                socketCreationSuccessful = false;
                //Now turn off the WiFi
/*
                if (WiFiConnection.isWiFiOn()) {
                    WiFiConnection.turnWiFiOff(); //this sometimes solves a problem..............
                }
                Generic.toasting.toast("Couldn't connect.\nPlease turn on the WiFi to refresh...", Toast.LENGTH_LONG, silentToast);
*/
                if ( localNotInternet ) {
                    toasting.toast("Couldn't connect to " + selectedServerConfig.getPanelName() +
                            "\nPlease try again later," +
                            "\nor check if electricity is down.", Toast.LENGTH_LONG, silentToast);
                } else {
                    toasting.toast("Couldn't connect to Server..." +
                            "\nPlease check if Internet connection is valid.", Toast.LENGTH_LONG, silentToast);
                }

                e.printStackTrace();
            }
            newSocketIsCurrentlyUnderCreation = false;
        }
    }
}
