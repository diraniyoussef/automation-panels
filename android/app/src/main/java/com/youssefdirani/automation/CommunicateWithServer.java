package com.youssefdirani.automation;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

import androidx.annotation.ColorInt;

class CommunicateWithServer {
    //This class has 2 tasks: 1) to listen to any incoming messages from server and update the switches accordingly.
    //                        2) to check if there's a connection and to create a new socket if the connection was broken.
    //                            so it actually fixes the connection.
    //Task 1 is taken care of by bufferThread
    //Task 2  is taken care of by TestAndFixConnection class. After that this TestAndFixConnection class creates a new socket,
    // it toggles the value of active_client_index from 1 to 0 or from 0 to 1.

    //The "communication" class instance is maintained and not renewed (unless through OnResume)
    private BufferThread[] bufferThread;
    private TestAndFixConnection testAndFixConnection;
    volatile DestroySocketTimer destroySocketTimer;
    volatile DelayToManipulateSockets delayToManipulateSockets;
    private volatile boolean isManipulateSocketsLocked = false;
    //*****
    //The testAndFixConnection thread class instance is frequently renewed (thus deleted) with each new order when it's
    // unlocked. Unlike the destroySocket thread class instance which persists as long as "communication" does.
    //testAndFixConnection will use (control) "destroySocket"
    //"communication" has no interest to control destroySocket.
    //*****

    //private volatile boolean silentToastAfterNewSocket = false;
    //    private ActionThread actionThread;
    private MainActivity activity;
    private volatile boolean receivedResponse = false;
    private Context applicationContext;
    private SocketConnection parentSocketConnection;
    private String panel_index;
    private String panel_name;
    private Toasting toasting;
    private int number_of_data_chunks;
    private boolean localNotInternet;
    private String owner_part;
    private String mob_part;
    private String mob_Id;

    CommunicateWithServer(Toasting toasting, MainActivity act, Context applicationContextParam, SocketConnection parentSocketConnection_arg,
                          String panel_index_arg, String panel_name_arg, int number_of_data_chunks,
                          boolean localNotInternet, String owner_part, String mob_part, String mob_Id) {
        this.localNotInternet = localNotInternet;
        this.owner_part = owner_part;
        this.mob_part = mob_part;
        this.mob_Id = mob_Id;
        this.toasting = toasting;
        this.number_of_data_chunks = number_of_data_chunks;
        activity = act;
        applicationContext = applicationContextParam;
        parentSocketConnection = parentSocketConnection_arg;
        panel_index = panel_index_arg;
        panel_name = panel_name_arg;
        bufferThread = new BufferThread[ SocketConnection.maxClientsNumber ];
        testAndFixConnection = new TestAndFixConnection();
        destroySocketTimer = new DestroySocketTimer();
        delayToManipulateSockets = new DelayToManipulateSockets();
    }

    void setThreads(String message_param, boolean boolTestAndFixConnection) {//in case 2 seconds of no reply, you might want to
        //now we would start the bufferThread if it was not started already. When was it created? It was created
        //in CreateSocket class.
        Log.i("Youssef Communi...java", "Now entering 'communicate' with active_client_index as " +
                parentSocketConnection.active_client_index  + ". For panel index " + panel_index +
                " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex( parentSocketConnection.active_client_index ) );

        if (boolTestAndFixConnection) { //this variable boolTestAndFixConnection reflects the willingness to go to
            // class TestAndFixConnection. It will enter if the class thread was not doing anything there
            try {
                if (!testAndFixConnection.lock) { //it's enough to test once at a time, this is why I'm putting a lock
                    testAndFixConnection = null;
                    testAndFixConnection = new TestAndFixConnection(); //I have to create a new one because a thread cannot start twice or more; only once.
                    testAndFixConnection.lock = true;
                    Log.i("Youssef", "testAndFixConnection is renewed now." + " For panel index " + panel_index +
                            " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex( parentSocketConnection.active_client_index ));
                    receivedResponse = false;
                    Log.i("Youssef", "receivedResponse is set to false just before entering testAndFixConnection."
                            + " For panel index " + panel_index +
                            " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex( parentSocketConnection.active_client_index ));
                    testAndFixConnection.start();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.i("Youssef Communi...java", "TestAndFixConnection didn't enter while it should had!" +
                        " For panel index " + panel_index);
            }
        }
        //Note that it's ok (and maybe necessary) for printWriter and bufferedReader to be static fields (necessary because
        // I guess that printWriter and bufferedReader use the SAME specific resources) inside socketConnection as long as
        // they are accessed within UI thread not a separate thread, or else app may crash or behave wrongly if user did
        // multiple clicks. Where printWriter is initiated matters even though it starts a special thread to send a message.
        // It's not a stupid note.

        printMessage( message_param );
    }

    private void printMessage (String message_param) {
        if( parentSocketConnection.printWriter[ parentSocketConnection.active_client_index ] != null ) { //it's for protection but I think it shouldn't be null and enter here at the same time
            final String message = message_param;
            new Thread() { //Threading network operations is recommended in general.
                //It's good that this particular thread is anonymous so, by assumption, it works better now.
                public void run() {
                    try {//try-catch is always good. It can prevent a crash.
                        //perhaps waiting before writing is good?
                        Thread.sleep(100); //not bad at all I guess
                        parentSocketConnection.printWriter[parentSocketConnection.active_client_index].write(message); //message is something like "mob1: awake?"
                        parentSocketConnection.printWriter[parentSocketConnection.active_client_index].flush();
                        Log.i("Youssef Communi...java", "sent message should theoretically be: " + message
                                + " For panel index " + panel_index +
                                " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex( parentSocketConnection.active_client_index ));
                    } catch (Exception e) {//that could happen if the socket is null for some time... but I would have prevented
                        // sending a message then
                        e.printStackTrace();
                        Log.i("Youssef Communi...java", "Error in sending message to server or sleeping" +
                                ". For panel index " + panel_index +
                                " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex( parentSocketConnection.active_client_index ));
                    }
                }
            }.start();
        } else {
            Log.i("Youssef Communi...java", "printWriter is set to null!!!!!!!!!!!!!!" + " For panel index " + panel_index +
                    " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex( parentSocketConnection.active_client_index ));
        }
    }


    void null_bufferThread(int index){
        if (bufferThread[index]!= null) {
            bufferThread[index].end_bufferThread();
            bufferThread[index] = null;
            Log.i("Youssef Communi...java", "bufferThread " + index + " instance is now nulled." + " For panel index " + panel_index +
                    " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex( parentSocketConnection.active_client_index ));
        }
    }

    void renew_bufferThread(int index) {
        //bufferThread[index] = new BufferThread(SocketConnection.client[index], index); //this is for debugging only
        bufferThread[index] = new BufferThread( parentSocketConnection.client[index] );
        bufferThread[index].start();
        Log.i("Youssef Communi...java", "bufferThread (on server index " + index + "" +
                "of port " + parentSocketConnection.selectedServerConfig.getPortFromIndex( index ) +
                ") instance is now created and started."
                + " For panel index " + panel_index + ".");
    }

    class BufferThread extends Thread { //this class is like a ReaderControllerThread
        boolean killThread = false;
        BufferedReader bufferedReader;
        int bufferSize = 256;
        Socket client;
        WritingThread writingThread;
        //private int index;//this is for debugging only
        //BufferThread(Socket client_param, int index_param){ //this is for debugging only
        BufferThread(Socket client_param){
            //index = index_param; //this is for debugging only
            client = client_param;
            try{
                bufferedReader = new BufferedReader(new InputStreamReader(client.getInputStream()), bufferSize);
                Log.i("Youssef Communi...java", "A bufferedReader instance is effectively created now, for panel index " + panel_index +
                        ". We may still be on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex( parentSocketConnection.active_client_index ) +
                ". Yet a socket is created on the OTHER port by now.");
            } catch (Exception e) {
                e.printStackTrace();
                Log.i("Youssef Communi...java", "Error: creating bufferedReader." + " For panel index " + panel_index +
                        " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex( parentSocketConnection.active_client_index ));
            }
        }

        void end_bufferThread() {
            killThread = true;//VERY NECESSARY actually. Because making bufferThread[index] point to another instance is not sufficient to kill the thread, I guess.
            null_bufferedReader();
        }

        private void null_bufferedReader() {
            if (bufferedReader != null) {
                try{
                    bufferedReader.close(); //returns void
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i("Youssef Communi...java","Error in closing bufferedReader." + " For panel index " + panel_index +
                            " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex( parentSocketConnection.active_client_index ));
                }
                bufferedReader = null;
            }
        }

        @Override
        public void run() {
            Log.i("Youssef Communi...java", "bufferThread just started." + " For panel index " + panel_index );
                    //+ " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex( parentSocketConnection.active_client_index ));
            String s;
            try {
                while( !killThread ) {
                    if( bufferedReader != null ) {
                        //I'm supposing that bufferedReader could change reference outside without any harm...
                        //if (SocketConnection.bufferedReader.available() != 0) {//could that make a problem if bufferedReader was null?
                        if (bufferedReader.ready()) {
                            //Instead of the ready method you may use the read method and set the timeout of the socket
                            // using setSoTimeout. <- not sure of that, anyway never usse read() without preceding it with ready()

                            Log.i("Youssef Communi...java", "BufferThread being started");
                            int readCharAsInt;
                            int bufferCharIndex = 0;
                            s = "";
                            try {
                                readCharAsInt = bufferedReader.read();
                                //The following "while" block, as I believe, reads the whole buffer. So I don't think I need to flush it ever more!
                                while (readCharAsInt != -1 && bufferCharIndex < bufferSize) {
                                    //SocketConnection.client.getChannel().position(0);
                                    bufferCharIndex++;
                                    if (s.length() != 0 && (char) readCharAsInt == '\0') {
                                        writingThread = new WritingThread( s );
                                        writingThread.start();
                                        Log.i("Youssef Communi...java", "Receiving the message " + s +
                                                ". For panel index " + panel_index +
                                                " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex( parentSocketConnection.active_client_index ));
                                        writingThread.join();//in order to let the processing be done one by one.
                                        // because in multithreading there is no guaranteed succession without doing something.
                                        s = "";
                                    } else {
                                        //now discussing the non-validity of the previous "if"
                                        if( s.length() == 0 && ( (char) readCharAsInt == '\0') ) {
                                            s = "";
                                            break;
                                        }
                                        if ((char) readCharAsInt != '\0') { //whether "s" had already a length of 0 or had some characters
                                            s = s + (char) readCharAsInt;
                                        }
                                    }
                                    if( bufferedReader.ready() ) {
                                        readCharAsInt = bufferedReader.read();
                                    } else {
                                        if (s.length() == 0) {
                                            s = "";
                                            break;
                                        }
                                        writingThread = new WritingThread( s );
                                        writingThread.start();
                                        Log.i("Youssef Communi...java", "Receiving from panel the message " + s +
                                                ". For panel index " + panel_index +
                                                " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex( parentSocketConnection.active_client_index ));
                                        writingThread.join();
                                        s = "";
                                        break;
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.i("Youssef Communi...java", "Error: in reading from buffer. Maybe because the socket is dead. " +
                                        "The string was: " + s + ". For panel index " + panel_index +
                                        " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex( parentSocketConnection.active_client_index ));
                            }
                        }
                    }
                    try {
                        Thread.sleep(1000); //give it 80 ms rest
                    } catch (Exception e) {
                        //Thread.currentThread().interrupt(); //it's not good to interrupt it.
                        Log.i("Youssef Communi...java", "Error: in sleeping." + " For panel index " + panel_index +
                                " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex( parentSocketConnection.active_client_index ));
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) { //that could happen if the socket was null for some time...
                e.printStackTrace();
                Log.i("Youssef Communi...java", "Error: in receiving message from module." + " For panel index " + panel_index +
                        " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex( parentSocketConnection.active_client_index ));
            }
        }
    }

    class WritingThread extends Thread {
        private String incomingMessage;
        WritingThread( String s ){
            incomingMessage = s;
        }

        private boolean checkMessageValidity() {
            /*
            Do I need to check the validity of the message's owner, mob_id, and mod_id?
            It can be useful to prevent an unseen error in a code somewhere or maybe to harden (just a little) the sabotage of a hacker.
             */

            /* If the message started with a random dummy char for some reason which I don't know (I checked the NodeMCU and the string is going therefrom correctly),
            then I will handle it by checking if the assumed owner part of the message had the real owner name.
            But worse, for a bad luck and very low probability, if the message started with something like :Youssef_70853721:2:mob_...,
            or worst, ::::Youssef_708537:2:mob_..., or worst E:::Youssef_70853...
                         This is why I made the following search for the owner_part
            */
            int start_index = incomingMessage.indexOf(owner_part);
            if( start_index == -1 ) {
                return false;
            }
            if( start_index != 0 ) {
                incomingMessage = incomingMessage.substring( start_index );
            }

            String textToSearch = ":";
            int first_colon = incomingMessage.indexOf(textToSearch);
            String owner, mob, mod;

            if( first_colon == -1 ) {
                return false;
            }

            owner = incomingMessage.substring(0, first_colon);

            int next_colon = incomingMessage.indexOf(textToSearch, first_colon + 1); //indexOf is inherently protected, like in case the index was larger than the string's length
            if( next_colon == -1 ) {
                return false;
            }
            mod = incomingMessage.substring( first_colon + 1, next_colon );

            first_colon = next_colon;
            next_colon = incomingMessage.indexOf( textToSearch, first_colon + 1 );
            if( next_colon == -1 ) {
                return false;
            }
            mob = incomingMessage.substring( first_colon + 1, next_colon );

            return owner.equalsIgnoreCase(owner_part) && mob.equalsIgnoreCase(mob_part) &&
                    mod.equalsIgnoreCase( panel_index );
        }

        @Override
        public void run() {
            //analyse analyze the message to update the status of switches.
            Log.i("Youssef Communi...java","WritingThread just started." + " For panel index " + panel_index);
            String textToSearch;
            try {
                if( checkMessageValidity() ) { //checkMessageValidity() is about owner, mod, mob.
                    //The following is about the context of the message
                    int dataIndex = incomingMessage.lastIndexOf(":"); //last index is important...
                    // because what I process here is only this part server1:O0TO1TO2FO5TO6TO7T
                    // or server1:Ack
                    // not the part before the :
                    if (dataIndex != -1) {
                        dataIndex++;
                        int specificDataIndex;

                        textToSearch = "ACK";
                        specificDataIndex = incomingMessage.indexOf(textToSearch, dataIndex);
                        if (specificDataIndex != -1) {
                            //fine. It's an acknowledgment. Nothing to do here.
                            if( localNotInternet ) {
                                toasting.toast("Message received...", Toast.LENGTH_SHORT, false);
                            } else {
                                toasting.toast("Partial Acknowledge...", Toast.LENGTH_SHORT, false);
                            }

                            receivedResponse = true;
                            Log.i("Youssef Communi...java", "receivedResponse is set to true from Ack."
                                    + " For panel index " + panel_index +
                                    " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex(parentSocketConnection.active_client_index));
                            //break; //go out of the "for" loop.
                            return;
                        } else {
                            //Generic.toast(this_is_not_UI_thread, "Couldn't get information correctly from module.", Toast.LENGTH_SHORT, silentToast);
                            Log.i("Youssef Communi...java", "The message is not an ACK..." +
                                    incomingMessage + " from panel index " + panel_index +
                                    " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex(parentSocketConnection.active_client_index) +
                                    ". BTW if this is debugging, it could be that we have here more pins than the ones declared in the arduino code.");
                            //break; //break is not wrong but also commenting it is not wrong.
                        }

                        //not an ack, so hopefully maybe it's a humidity temperature report...
                        if( isHumidityTemperature( dataIndex ) ) {
                            return;
                        }
                        //not a humidity temperature report, so is it a report of an output maybe ?
                        if( isRelayReport( dataIndex ) ) {
                            return;
                        }
                    } else {
                        //Generic.toast(this_is_not_UI_thread, "Couldn't get information correctly from module. Please try later.", Toast.LENGTH_SHORT, silentToast);
                        Log.i("Youssef Communi...java", "Couldn't get information correctly from module because couldn't find a colon."
                                + " For panel index " + panel_index +
                                " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex(parentSocketConnection.active_client_index) );
                    }
                }
            }
            catch (Exception e){
                Log.i("Youssef Communi...java", "Error somewhere in string manipulation of read message from server. " +
                        "This is a catch block. Incoming message is: " + incomingMessage
                        + ". For panel index " + panel_index +
                        " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex( parentSocketConnection.active_client_index ) );
                e.printStackTrace();
                //Generic.toast(this_is_not_UI_thread, "Couldn't get information correctly from module.", Toast.LENGTH_SHORT, silentToast);
            }
            //when it finishes its job it must destroy itself immediately, and organise the list of the variable size array
            //which held that instance.
        }

        private boolean isRelayReport( int dataIndex ) {
            final int NUMBER_OF_POINTS = 2;
            final String[] outPin = {"3", "4"};

            String outputState;
            int specificDataIndex;
            for (int i = 0; i < NUMBER_OF_POINTS; i++) {
                String textToSearch = "O" + outPin[i];
                specificDataIndex = incomingMessage.indexOf(textToSearch, dataIndex);
                if (specificDataIndex != -1) {
                    outputState = Character.toString(incomingMessage.charAt(specificDataIndex + textToSearch.length()));
                    final int viewIndex = i;
                    if (outputState.equals("T")) {
                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                final ViewGroup parentOfAllButtons  = activity.findViewById(R.id.panel_control);
                                final TextView statusTextView = parentOfAllButtons.findViewWithTag( "textView" + outPin[viewIndex] );
                                statusTextView.setText("On");
                                statusTextView.setTextColor(Color.GREEN);
                                /*
                                Log.i("Youssef Communi...java", "output " + outPin[viewIndex] +
                                        " is set to true. For panel index " + panel_index +
                                        " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex(parentSocketConnection.active_client_index) );
                                 */
                            }
                        });
                        toasting.toast("Current state is received from panel  " +
                                panel_name + ".", Toast.LENGTH_SHORT, false);
                        receivedResponse = true;
                        Log.i("Youssef Communi...java", "receivedResponse is set to true at output " +
                                outPin[viewIndex]
                                + ", for panel index " + panel_index +
                                " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex(parentSocketConnection.active_client_index));
                    } else if (outputState.equals("F")) {
                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                final ViewGroup parentOfAllButtons  = activity.findViewById(R.id.panel_control);
                                final TextView statusTextView = parentOfAllButtons.findViewWithTag( "textView" + outPin[viewIndex] );
                                statusTextView.setText("Off");
                                statusTextView.setTextColor(Color.RED);
                                /*         Log.i("Youssef Communi...java", "something did change to False at output " +
                                        outPin[viewIndex]
                                        + ". For panel index " + panel_index +
                                        " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex(parentSocketConnection.active_client_index));
                        */
                            }
                        });
                        toasting.toast("Current state is received from panel " +
                                panel_name + ".", Toast.LENGTH_SHORT, false);
                        //Generic.toasting.toast("Current state is received.", Toast.LENGTH_SHORT, silentToastAfterNewSocket);
                        //silentToastAfterNewSocket = false;
                        receivedResponse = true;
                        Log.i("Youssef Communi...java", "receivedResponse is set to true at output pin " +
                                outPin[viewIndex]
                                + ". For panel index " + panel_index +
                                " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex(parentSocketConnection.active_client_index));
                    } else {
                        //Generic.toast(this_is_not_UI_thread, "Couldn't get information correctly from module.", Toast.LENGTH_SHORT, silentToast);
                        Log.i("Youssef Communi...java", "Couldn't get information correctly from module after not " +
                                "being true nor false, for panel index " + panel_index +
                                " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex(parentSocketConnection.active_client_index));
                    }
                } else {
                    Log.i("Youssef Communi...java", "wrong format. place ***");
                    return false;
                }
            }
            return false;
        }

        boolean isHumidityTemperature( int dataIndex ) {
            String humidity_str = "";
            final char trailor = 't';
            String textToSearch =  String.valueOf(trailor);
            for( int i = 0 ; i < number_of_data_chunks - 1 ; i++ ) { //it's 2, the first for humidity, tailed with 1 or more trailors, followed by the temperature, tailed with 1 or more null chars (which probably won't appear)
                int specificDataIndex = incomingMessage.indexOf( textToSearch, dataIndex );
                if (specificDataIndex == -1) {
                    Log.i("Youssef Comm", "wrong format");
                    return false;
                } else {
                    humidity_str = incomingMessage.substring(dataIndex, specificDataIndex);
                    /*Some more code should be added here in case number_of_data_chunks > 2*/
                }
            }
            if( number_of_data_chunks == 2 ) { //meaning humidity and temperature for now
                //specificDataIndex = incomingMessage.lastIndexOf( textToSearch, dataIndex ); //This resulted in a PROBLEM as dataIndex I guess is a backward index
                int specificDataIndex = incomingMessage.lastIndexOf( textToSearch );
                Log.i("Communi...java", "Youssef last trailor is found at " + specificDataIndex);
                if (specificDataIndex == -1) {
                    Log.i("Youssef Communi", "wrong format");
                    return false;
                } else {
                    final String temperature_str = incomingMessage.substring( specificDataIndex + 1);
                    final String humidity_str_final = humidity_str;
                    final TextView textView_humidity = activity.findViewById(R.id.text_humidity);
                    final TextView textView_temperature = activity.findViewById(R.id.text_temperature);
                    if( textView_humidity != null && textView_temperature != null ) {
                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                if (activity != null) {
                                    textView_humidity.setText(humidity_str_final);
                                    textView_temperature.setText(temperature_str);
                                    Log.i("Youssef Communi...java", "Values updated." +
                                            ". For panel index " + panel_index +
                                            " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex(parentSocketConnection.active_client_index));
                                }
                            }
                        });
                    }
                    receivedResponse = true;
                    toasting.toast("Current Temperature and humidity state is received from " +
                            panel_name + ".", Toast.LENGTH_SHORT, false);
                    return true;
                }
            } else {
                return false;
            }
        }
    }

    //this is the only thread among all created threads that can create a new socket.
    class TestAndFixConnection extends Thread {//it waits within 850 milliseconds to receive something from server.
        volatile boolean lock;

        TestAndFixConnection() {}

        @Override
        public void run() {
            //wait at most 2 or 3 seconds for receipt of message
            // if nothing is received
            // it creates a new socket without resending the message again,
            // and it will direct all new requests through the new socket
            // and kill the previous socket after some time like 1500 milliseconds.
            //When reaching 850 milliseconds and nothing is received, no need to disable switches since they may be sending
            // new orders through the previous socket.
            int iterationTimeout = 0;
            Log.i("Youssef Communi...java","TestAndFixConnection                                   " +
                    "thread has just started so starting the couting of " +
                    "readtimeout in testAndFixConnection." + " For panel index " + panel_index +
                    " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex( parentSocketConnection.active_client_index ));
            try {
                if (bufferThread[parentSocketConnection.active_client_index] != null)
                    while ((iterationTimeout < SocketConnection.read_waitingData.maxIterations) && !receivedResponse
                            && !bufferThread[parentSocketConnection.active_client_index].killThread) { //no need for !bufferThread[SocketConnection.active_client_index].killThread but anyway!
                        iterationTimeout = iterationTimeout + 1;
                        try {
                            Log.i("Youssef Communi...java", "testAndFixConnection                                   loop index: " + iterationTimeout
                                    + ". For panel index " + panel_index +
                                    " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex(parentSocketConnection.active_client_index));
                            Thread.sleep(SocketConnection.read_waitingData.divisionInterval);
                        } catch (Exception e) { //InterruptedException ex
                            e.printStackTrace();
                            Log.i("Youssef Communi...java", "TestAndFixConnection                   Error: Sleeping in testAndFixConnection." +
                                    " For panel index " + panel_index +
                                    " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex(parentSocketConnection.active_client_index));
                        }
                    }
                Log.i("Youssef Communi...java", "TestAndFixConnection                               receivedResponse value is " + String.valueOf(receivedResponse)
                        + "\n ending the counting of readtimeout in testAndFixConnection. "
                        + "is it less than 2.6 seconds?"
                        + " For panel index " + panel_index +
                        " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex(parentSocketConnection.active_client_index));
                if (iterationTimeout == SocketConnection.read_waitingData.maxIterations || bufferThread[parentSocketConnection.active_client_index] == null) {
                    Log.i("Youssef Communi...java", "testAndFixConnection           No, it's full 2.6 seconds! So if all timers are fine, creating a new socket."
                            + ". For panel index " + panel_index +
                            " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex(parentSocketConnection.active_client_index));
                    //the following method creates a new socket on the other port, and sets a timer to destroy the current socket.
                    if (!isManipulateSocketsLocked && !destroySocketTimer.isAwaitingToDestroy) {
                        toasting.toast("Enhancing connection for panel " + panel_name + "..."
                                , Toast.LENGTH_SHORT, false);
                        manipulateSocketsAndStartTiming(false);
                    } else {
                        if (isManipulateSocketsLocked) {
                            Log.i("Youssef Communi...java", "TestAndFixConnection                           " +
                                    "isManipulateSocketsLocked variable is true, so we won't create a new socket!" +
                                    " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex(parentSocketConnection.active_client_index));
                        }
                        if (destroySocketTimer.isAwaitingToDestroy) {
                            Log.i("Youssef Communi...java", "TestAndFixConnection                          " +
                                    "We're within the 8 seconds period to destroy the old socket, so we won't create a new socket!" +
                                    " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex(parentSocketConnection.active_client_index));
                        }
                    }

                } else {
                    Log.i("Youssef Communi...java", "TestAndFixConnection                      " +
                            "Yes, it took less than 2.6 seconds." + " For panel index " + panel_index +
                            " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex(parentSocketConnection.active_client_index));
                    //Generic.toasting.toast("Message received from module.", Toast.LENGTH_SHORT, silentToast);
                }
            } catch( Exception e) {
                Log.i("Youssef Communi...java", "TestAndFixConnection    weird exception! probably the active_client_index" +
                        "is like -1");
            }

            receivedResponse = false;
            Log.i("Youssef Communi...java","TestAndFixConnection                       " +
                    "receivedResponse is set to false when just about finishing testAndFixConnection."
            //        + " For panel index " + panel_index +
            //        " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex( parentSocketConnection.active_client_index )
            );
            //when all the work is done unlock it
            lock = false;
            //actionThread.enableSwitches(socketConnection2);

            //watch anything received from server to consider the validity of a connection.
            // This may be wrong with a very small percentage of error.
            //Messages won't be recorded in a special data structure like an array or a list. Not in this version.
            // If they were to be recorded then a monitoring thread has to be involved.
            //Also, if they were to be recorded then same messages has to be handled in a special way to know if we need to
            // omit some of them, or sign some of them... which further complicate things...
        }
    }

    private void manipulateSocketsAndStartTiming( boolean silentToast ) {
        isManipulateSocketsLocked = true;
        if( WiFiConnection.wiFiValid( applicationContext, silentToast, toasting, localNotInternet ) ) { //I think it's ok to access this static method by 2 different threads, e.g. here and through the button presses.
            //delete old socket after some time later
            if (destroySocketTimer.isAwaitingToDestroy) {
                //don't do anything... Because it's too early (too fast) that we want to make a new socket.
                //The other socket than SocketConnection.active_client_index is waiting to be dead.
                //It's an unusual behavior to enter here.
                //It might be due to the fact that the read timeout is small.
                // I thought about fixing it here by incrementing the read timeout,
                // then I changed my mind because fixing it based on awaitingToDestroySocket may not be
                // so meaningful, and it may impose a relatively unwanted large read timeout.
                // Anyway, it can be fixed by the following commented line.
                //SocketConnection.read_waitingData.incrementMaxIterations(300);
                Log.i("Youssef Communi...java", "manipulateSocketsAndStartTiming                       Unusual behavior. So incrementing the read timeout."
                        + " For panel index " + panel_index +
                        " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex( parentSocketConnection.active_client_index ));
                //At all cases I don't mind much about toggling on and on... Although it's better to commit
                // with one socket.
                //Still, if I want to have an optimum read timeout, then it must do a TRAINING WHILE KNOWING FOR SURE
                //THAT THE SOCKET IS STILL ON...
                //FOR NOW, I will check it manually to get the optimum read timeout. I don't know if the
                // physical conditions (like interference or barriers) on down layers may play a role...
            } else {
                //Now try to create a new socket...
                boolean successful_socket = parentSocketConnection.createNewSocket(
                        SocketConnection.nextClientIndex( parentSocketConnection.active_client_index ) );//create new socket
                //I would like to note that a very very rare case may happen as a critical race condition in the creation of 2 sockets.
                // I mean, when testAndFixConnection is trying to make a new socket on activeClientIndex 0 and socketConnectionSetup
                // is trying to make the socket on the same activeClientIndex 0 because e.g. the electricity went down.
                // ANYWAY, I guess and hope that this won't cause a problem because the newer socket would had already destroyed the slightly
                //previous one. So I won't bother...
                if (successful_socket) {
                    //silentToastAfterNewSocket = true; //this is to make the following toast apparent to the user.
                    //Generic.toasting.toast("Connection is better now.\nYou may continue or try again.", Toast.LENGTH_LONG, silentToast);
                    toasting.toast("Connection is better now for panel " + panel_name + ".",
                            Toast.LENGTH_LONG, silentToast);
                    //destroy the old socket after some time, because I want to keep listening to incoming messages on previous socket
                    destroySocketTimer.startTiming( parentSocketConnection.active_client_index );
                    delayToManipulateSockets.startTiming();

                    //now divert all newcoming orders to the new socket.
                    parentSocketConnection.active_client_index = SocketConnection.nextClientIndex(parentSocketConnection.active_client_index);
                    Log.i("Youssef Communi...java", "manipulateSocketsAndStartTiming          Now the newcoming orders should be diverted to " +
                            parentSocketConnection.active_client_index + ". For panel index " + panel_index +
                            " and port " + parentSocketConnection.selectedServerConfig.getPortFromIndex( parentSocketConnection.active_client_index) );
                    /* Now, if we're sending this to the intermediate server, then we would want to inform it with the owner and mob_id,
                     * so we will send a dummy message, but it's the signature that matters.
                     */
                    if( !localNotInternet ) {
                        printMessage(mob_Id + String.valueOf(parentSocketConnection.selectedServerConfig.getPanelIndex()) + ":" +
                                "mob_HI\0");/* message content part must include something, not just a null char - We're not interested
                                        * in getting a reply to this message although we might get it ! It's only
                                        * to let the intermediate server recognize the just made socket info. This practice is necessary for servers
                                        * for them to be known in case some client wanted to communicate with.
                                        * Here I'm not sure it's really necessary here, since we're a client now and no one will communicate with
                                        * our app. NEVERTHELESS, IN FUTURE CRITICAL APPLICATIONS WE WANT TO BE COMMUNICATED WITH !
                                        * WE MIGHT ALSO MAKE A DEAMON THREAD OR WHATEVER.
                                        */
                    }
                } else {
                    //silentToastAfterNewSocket = true; //this is to make the following toast apparent to the user.
                    Log.i("Youssef Communi...java", "manipulateSocketsAndStartTiming                                socket creation failed through testAndFi..."
                            + " For panel index " + panel_index + "."
                            + " We are still on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex( parentSocketConnection.active_client_index ));
//                    Generic.toasting.toast("You may continue or try again later for panel " +
//                            String.valueOf(panel_index + 1) + ".", Toast.LENGTH_LONG, silentToast);
                }
            }
        } /*else {
            don't do anything.
            Note that the bad case here is a false positive which is very potential... Further detailing needed...
            the right thing though is to kill all threads including
            socketConnection2.createSocketAsyncTaskInstance
        }*/
        isManipulateSocketsLocked = false;
    }

    class DelayToManipulateSockets {
        private Handler h = new Handler();

        Runnable manipulate = new Runnable() {
            public void run() {
                Log.i("Youssef Communi...java", "DelayToManipulateSockets                      2 minutes are now due, calling manipulateSockets."
                        + " For panel index " + panel_index +
                        " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex( parentSocketConnection.active_client_index ));
                if (!isManipulateSocketsLocked && !destroySocketTimer.isAwaitingToDestroy) {
                    manipulateSocketsAndStartTiming(true);
                    Log.i("Youssef Communi...java", "DelayToManipulateSockets                  finished calling manipulateSockets."
                            + " For panel index " + panel_index +
                            " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex( parentSocketConnection.active_client_index ));
                } else {
                    if( isManipulateSocketsLocked ) {
                        Log.i("Youssef Communi...java", "DelayToManipulateSockets               " +
                                "isManipulateSocketsLocked variable is true, so we won't create a new socket!" +
                                "We are still on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex( parentSocketConnection.active_client_index ));
                    }
                    if( destroySocketTimer.isAwaitingToDestroy ) {
                        Log.i("Youssef Communi...java", "DelayToManipulateSockets                  " +
                                "We're within the 8 seconds period to destroy the old socket, so we won't create a new socket!" +
                                "We are still on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex( parentSocketConnection.active_client_index ) );
                    }
                }
            }
        };

        void startTiming() {
            Log.i("Youssef Communi...java", "DelayToManipulateSockets                    " +
                    "Starting the 2 minutes timer" + " For panel index " + panel_index +
                    " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex( parentSocketConnection.active_client_index ) );
            h.removeCallbacks(manipulate);//this cancel is still important in case the program worked in an unpredictable manner (but may never be necessary in action)
                                            //i.e. this is a mechanism for the app to fix itself in case a critical race happened.
                                            // It's a good thing both sockets have only one instance of DelayToManipulateSocket
            //2 minutes means 120000. THIS DURATION SHOULD BE CAREFULLY CHOSEN AS SMALLER THAN THE SOCKET LIFETIME INDICATED IN NODEMCU.
            int timer = 120000;
            h.postDelayed(manipulate, timer);
        }

        void cancelTimer() {
            Log.i("Youssef Communi...java", "DelayToManipulateSockets                    " +
                    "Cancelling the timer.");
            h.removeCallbacks(manipulate);
        }
    }

    class DestroySocketTimer { //I made this an inner class of TestAndFixConnection because the volatile variable
        // awaitingToDestroySocket is defined in TestAndFixConnection
        volatile int clientIndex;
        volatile boolean isAwaitingToDestroy = false;
        private Handler h = new Handler();

        DestroySocketTimer() {
         /*   Log.i("Youssef Communi...java", "DestroySocketTimer                        " +
                    "Creating a new DestroySocket instance."  + " For panel index " + panel_index +
                    " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex( parentSocketConnection.active_client_index ));
                    */
        }

        Runnable destroy = new Runnable() {
            public void run() {
                Log.i("Youssef Communi...java", "DestroySocketTimer         8 seconds are now due, destroying the old socket" +
                        " of panel index " + panel_index +
                        " on port different than " + parentSocketConnection.selectedServerConfig.getPortFromIndex( parentSocketConnection.active_client_index ));
                parentSocketConnection.destroySocket(clientIndex);
                isAwaitingToDestroy = false;//unlocking...
            }
        };

        void startTiming(int clientIndex_param) {
            isAwaitingToDestroy = true; //locking...
            clientIndex = clientIndex_param;
//            h.removeCallbacks(destroy);
            /*this is like a timeout determined by how fast is the NodeMCU.
            If the NodeMCU takes a delay of 750 ms and if it had like 3 clients so it should bave like 3 x 750 ms = 3000 ms
             + the traveling time, say 1000 ms va et vient.
            such that the NodeMCU handles the requests from the older to the newest.
            There is another note concerning this timer; during this timer (i.e. isAwaitingToDestroy is true) it is not
            allowed to manipulate sockets since the newest socket is very fresh! So it's not so good to wait too long. Anyway,
            there is about 2.6 seconds discounted from the timer for the wrongly decision to be made to manipulate during the timer,
             this 2.6 seconds is the testAndFixConnection read timeout.
        */
            int timer = 8000;
            h.postDelayed(destroy, timer);
            Log.i("Youssef Communi...java", "DestroySocketTimer        " +
                    "Setting a delay so that after 8 seconds, the old socket should be destroyed"
                    + " on port " + parentSocketConnection.selectedServerConfig.getPortFromIndex( parentSocketConnection.active_client_index )
                    + " of panel index " + panel_index );
        }

        void cancelTimer() {
            Log.i("Youssef Communi...java", "DestroySocketTimer                            " +
                    "Cancelling timer.");
            h.removeCallbacks(destroy);
        }
    }
}
