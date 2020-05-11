#include "MessageOp.h"
#include "ESPAsyncTCP.h"

extern const int Max_Reading_Buffer;
extern const int delay_per_loop;

boolean toggle_connect_failure_notifier_pin;

class AsynchroClient {
  //example given by  me-no-dev in https://github.com/me-no-dev/ESPAsyncTCP/issues/18
  //https://github.com/me-no-dev/ESPAsyncTCP/blob/master/src/ESPAsyncTCP.cpp
  //https://arduino-esp8266.readthedocs.io/en/latest/faq/a02-my-esp-crashes.html#watchdog //this may help to resolve the crash
  //In case the crash continued, check this https://www.arduino.cc/reference/en/language/variables/utilities/progmem/
private:      
  //defining volatile locks  
  volatile boolean message_received1_lock = false; //I made this a lock for both clients
  volatile boolean message_received2_lock = false;  
  volatile boolean just_connected_1 = false;
  volatile boolean just_connected_2 = false;  

  int port1;  //3554; //3558;
  int port2;  //3555; //3559;
  
  int most_recent_client_connect_attempt_index = 2; //will be either 1 or 2. Maybe this is better be a union but ok.
  
  //defining counters
  
  int max_counter_to_manipulate_sockets; 
  int counter_to_manipulate_sockets = 0; 
  /*IMPORTANT********** 
   * It's better to make a dedicated counter_to_manipulate_sockets for client_1 and for client_2. This can prevent a weird problem
   * of a client that diconnects directly after just conneting, in effect this renews counter_to_manipulate_sockets for the same old still connected client.
   * This problem is not occuring much! Only at the beginning.
   */
  int max_counter_to_destroy_old_socket; //we destroy the old socket only when both sockets are connected.
  int counter_to_destroy_old_socket; 
  int max_counter_to_retry_socket_connection; 
  int counter_to_retry_socket_connection = 0; 
  int max_counter_to_suspect_disconnectivity;
  int counter_to_suspect_disconnectivity;
  int min_threshold_to_force_close_socket; 
  volatile boolean still_connecting_1; //tried to connect and onDisconnect hasn't triggered yet.
  volatile boolean still_connecting_2;
  volatile boolean async_client_1_disconnects_just_after_connecting;
  volatile boolean async_client_2_disconnects_just_after_connecting;  

  void createSocket1() {
    if( async_client_1.disconnected() ) {
      //async_client_1.stop(); //never do that, otherwise it will never connect.
      //NodeMCU::yieldAndDelay(50);
      counter_to_retry_socket_connection = max_counter_to_retry_socket_connection;
      most_recent_client_connect_attempt_index = 1;    
      still_connecting_1 = true;     
      async_client_1.connect( IP, port1 ); 
      Serial.printf("Now connecting to entity on port %d and IP %s\n", port1, IP); 
    } else {
      Serial.printf("async_client_1 of IP %s won't connect since it's not yet disconnected\n", IP);
    }    
  }

  void createSocket2() {         
    if( async_client_2.disconnected() ) {
      //async_client_2.stop(); //never do that, otherwise it will never connect.
      //NodeMCU::yieldAndDelay(50);
      counter_to_retry_socket_connection = max_counter_to_retry_socket_connection;
      most_recent_client_connect_attempt_index = 2;
      still_connecting_2 = true;    
      async_client_2.connect( IP, port2 ); 
      Serial.printf("Now connecting to entity on port %d and IP %s\n", port2, IP); 
    } else {
      Serial.printf("async_client_2 of IP %s won't connect since it's not yet disconnected\n", IP);
    }    
  }

  void setEvents1() {
    async_client_1.onConnect([this](void * arg, AsyncClient * client) {
      Serial.printf("onConnect triggered for port %d and IP %s. Success.\n", port1, IP);
      //async_client_1.onError(NULL, NULL);    //not sure if it's good to uncomment it
      just_connected_1 = true;           
    }, NULL);           
  
    async_client_1.onError([this](void * arg, AsyncClient * client, int error) { //may activate if an error happened even when connect(..., ...) runs.
      Serial.printf("Error number is %d for port %d and IP %s\n", error, port1, IP);
      //async_client_1.stop();
    }, NULL );

    async_client_1.onDisconnect([this](void * arg, AsyncClient * client) {      
      still_connecting_1 = false; //For it to stop connecting, onDisconnect will have to be triggered
      Serial.printf("out of Disconnect event for port %d and IP %s\n\n", port1, IP);
      if( just_connected_1 ) {
        just_connected_1 = false; 
        Serial.printf("Weird case for port 1 and IP %s!\n", IP);
        //async_client_1_disconnects_just_after_connecting = true;
      }
    }, NULL );

    async_client_1.onData([this]( void * arg, AsyncClient * c, void * data, size_t len ) { //I would rather avoid setting data to anything
      if( !message_received1_lock && len > 0 ) {
        message_received1_lock = true;        
        for( incoming_message1_index = 0 ; incoming_message1_index < max_incoming_messages_number ; incoming_message1_index++ ) {
          if( !message_in_processing_from_client_1[ incoming_message1_index ] ) {            
            Serial.printf("Inside onData for port 1 and IP %s of buffer index %d\n", IP, incoming_message1_index);            
            char* ch = (char *) data; //needed maybe
            byte considered_length = min( Max_Reading_Buffer , (const int) len );
            strncpy( (char*) buff_1[incoming_message1_index], (const char*) ch, considered_length ); //the const cast is probably very needed
            if( len < Max_Reading_Buffer ) {
              buff_1[ incoming_message1_index ][ len ] = '\0'; //maybe not necessary
            }            
            len_buff_1[ incoming_message1_index ] = considered_length;
            message_in_processing_from_client_1[ incoming_message1_index ] = true; //this is set to false after message analysis.
            break;
          }
        } 
        message_received1_lock = false;     
      }              
    }, NULL);       
  }

  void setEvents2() {
    
    async_client_2.onConnect([this](void * arg, AsyncClient * client) {            
      Serial.printf("onConnect triggered for port %d of IP %s. Success.\n", port2, IP);
      just_connected_2 = true;
    }, NULL);           
  
    async_client_2.onError([this](void * arg, AsyncClient * client, int error){ 
      Serial.printf("Error number is %d for port %d of IP %s\n", error, port2, IP);
      //async_client_2.stop();
    }, NULL );

    async_client_2.onDisconnect([this](void * arg, AsyncClient * client) {      
      still_connecting_2 = false; 
      Serial.printf("out of Disconnect event for port %d and IP %s\n\n", port2, IP);
      if( just_connected_2 ) {
        just_connected_2 = false; 
        Serial.printf("Weird case for port 2 of IP %s!\n", IP);
        //async_client_2_disconnects_just_after_connecting = true;
      }
    }, NULL );    

    async_client_2.onData([this]( void * arg, AsyncClient * c, void * data, size_t len ) { 
      if( !message_received2_lock && len > 0 ) {
        message_received2_lock = true;        
        for( incoming_message2_index = 0 ; incoming_message2_index < max_incoming_messages_number ; incoming_message2_index++ ) {
          if( !message_in_processing_from_client_2[ incoming_message2_index ] ) {
            message_in_processing_from_client_2[ incoming_message2_index ] = true; //this is set to false after message analysis.
            Serial.printf("Inside onData for port 2 and IP %s of buffer index %d\n", IP, incoming_message2_index);
            char* ch = (char *) data; //needed maybe
            byte considered_length = min( Max_Reading_Buffer , (const int) len );
            strncpy( (char*) buff_2[ incoming_message2_index ], (const char*) ch, considered_length ); //the const cast is probably very needed
            if( len < Max_Reading_Buffer ) {
              buff_2[ incoming_message2_index ][ len ] = '\0'; //maybe not necessary
            }
            len_buff_2[ incoming_message2_index ] = considered_length;
            break;
          }
        } 
        message_received2_lock = false;     
      }              
    }, NULL);  
  }

  void fixAutonomousDisconnectedSocket() {//a socket has disconnected alone after being connected. Weird ugly case. Perhaps caused by a bad or busy port.
    if( ( last_connected_index == 1 && !async_client_1.connected() ) || ( last_connected_index == 2 && !async_client_2.connected() ) ) {
      counter_to_manipulate_sockets = 0; /*
      * The necessary thing is to fix the value of last_connected_index. And this is made later (after the call to this function) normally.
      * 
      * Since this should had never happened even if the reflector was shut down, I don't know what 
      * could fix this issue and re-establish connection. Anyway I will attempt to quickly reconnect again. I will use
      * counter_to_manipulate_sockets instead of rushSocketSwitch()
      * 
      * Our condition in the 'if' is smarter than using just_connected_2 (thus the onConnect event) or relying on onDisconect event.
      * 
      * Another isssue: Is it better to put this in preProcess? I mean could this be useful in case it was disconnected 
      * directly right after connection, and say we wanted to send something to the reflector? Not really 
      * a problem since we send to ALL connected servers. (We also listen to all servers).
       */
    }
  }

public:
  static const byte max_incoming_messages_number = 4;
  volatile boolean message_in_processing_from_client_1[ max_incoming_messages_number ]; //This is an interesting lock not to override some messages without analyzing them. 
  volatile boolean message_in_processing_from_client_2[ max_incoming_messages_number ]; //Also these 2 locks must be initially set to false, which is the case.
  volatile boolean last_connected_index = 0;
  volatile char buff_1[ max_incoming_messages_number ][ Max_Reading_Buffer ];
  volatile int len_buff_1[ max_incoming_messages_number ];
  volatile char buff_2[ max_incoming_messages_number ][ Max_Reading_Buffer ];
  volatile int len_buff_2[ max_incoming_messages_number ];
  AsyncClient async_client_1;
  AsyncClient async_client_2;
  boolean both_ports_not_working;
  char* IP; //= "91.240.81.106"; for public IP  //this field "IP" is made public just for debugging
  volatile byte incoming_message1_index;  
  volatile byte incoming_message2_index;  
    
  AsynchroClient( ) {
    //following 2 counters are in effect once we're connected
    max_counter_to_manipulate_sockets = floor( 2 * 60 * 1000 / delay_per_loop ); //that is to switch to a new socket
    counter_to_manipulate_sockets = 0;
    max_counter_to_destroy_old_socket = floor( 10000 / delay_per_loop ); //that is after having a different new socket assigned 
                                                                        //PLEASE make this > time_to_await_for_report_receipt
    counter_to_destroy_old_socket = max_counter_to_destroy_old_socket;
    //next counter is in effect once we're not connected
    max_counter_to_retry_socket_connection = floor( 7000 / delay_per_loop ); //logically this must be > timeout to create a socket
    counter_to_retry_socket_connection = 0;    
    min_threshold_to_force_close_socket = floor( 0.25 * max_counter_to_retry_socket_connection ); //While retry_socket_connection is counting down, when it reaches this min_threshold_to_force_close_socket value then it forces the connection attempt to stop.
    Serial.printf("min_threshold_to_force_close_socket is %d\n", min_threshold_to_force_close_socket );
    counter_to_suspect_disconnectivity = 0;
    both_ports_not_working = false;
    max_counter_to_suspect_disconnectivity = 2; /* This means "max_counter_to_suspect_disconnectivity - 1" unsuccessful connection attempt(s) 
    * to a particular sepcific port while the other port is connected, to assume there is no real connection.
    * Why "max_counter_to_suspect_disconnectivity - 1" and not simply "max_counter_to_suspect_disconnectivity"? It has to do with the placement in the code.
    * Usually this is made in case our panel thinks it is connected but is not actually.
    * Say our NodeMCU thinks it is connected on port 1 e.g.. After some time it will try to switch on port 2. It won't be able to do that. So, 
    * counter_to_suspect_disconnectivity must count that. Then it will retry socket connection on port 2 again, then it fails then 
    * counter_to_suspect_disconnectivity will count that too. Then after a few counts we decide to close the connection on port 1 because we think 
    * that the our server is not actually connected.
     */
        
    setEvents1();
    setEvents2();
  }

  void rushSocketSwitch() {
    /*This is useful if one of them is connected and the other is not.
    * In case both were not connected then our asynchro_client handles it alone.
    * In case both are connected, one of them would had just been connected and it's a matter of time (counter_to_destroy_old_socket actually)
    * for the older socket to be closed. So we can set counter_to_destroy_old_socket to 0 to speed things a little.
    * Third case: one is connected and the other not connected and we have not yet tried to connect to (because I don't want 2 consecutive attempts to connect).
     */
     
    /*      
    if( async_client_1.connected() && async_client_2.connected() ) {
      counter_to_destroy_old_socket = 0; //Destroy the old socket and 
      counter_to_manipulate_sockets = 0; // when the old socket is destroyed make a new one in its place.
    } else
    //I commented this part because it doesn't solve really anything I beleive.
    */
    //PLEASE NOTE that for this rush idea to hold right, time_to_await_for_report_receipt, which is the period to re-ask again to rush 
    // (as well as max_counter_to_destroy_old_socket probably too) must be > time for the message to arrive.
    // Also, having max_counter_to_destroy_old_socket probably > time_to_await_for_report_receipt is a good thing because this gives a breath in order not to
    // rush again if we have just made a new socket.
    if( ( async_client_1.connected() && !async_client_2.connected() && most_recent_client_connect_attempt_index != 2 ) ||
        ( async_client_2.connected() && !async_client_1.connected() && most_recent_client_connect_attempt_index != 1 ) ) { 
            /*in case any of the conditions is not met then this means that our asynchro_client is in the phase of creating a new socket, 
             * so we won't intervene.  
             * 
             * Involving most_recent_client_connect_attempt_index is probably useless, but very logical.
             * 
             * We didn't use counter_to_retry_socket_connection because this is internal to our system (on a lower level that we must not go into and which
             * our asynchro_client handles automatically and healthily)
             */
      counter_to_manipulate_sockets = 0;
    }
    /* What if e.g. socket 1 was connected and 2 was not connected and say it disconnected in a weird manner after being connected, then 
     *  fixAutonomousDisconnectedSocket() handles it and no need to think about it here.
     *  
     *  What if e.g. socket 1 was connected and 2 was not connected, and socket 1 was like ruined because of port being busy or something like that,
     *  in this case it is up to counter_to_suspect_disconnectivity to handle it (if I activated it).
     */
  }

  void setAsynchroClient( char* IP, int port1, int port2 ) { //this should had been in the constructor but I made it here for pure programmatic reasons.
    this->IP = IP;
    this->port1 = port1;
    this->port2 = port2;
  }

  void preProcess( ) { //For local servers, I'm not interested in sending anything to them the moment they're just being connected.
                       // This is why we don't care in knowing if an async_client has just been connected or not.
    char just_connected;
    preProcess( &just_connected );
  }

  void preProcess( char* just_connected ) { //this is connectivity check 1

    if( async_client_1_disconnects_just_after_connecting ){
      async_client_1_disconnects_just_after_connecting = false;
      //async_client_1.stop(); //although it has just been disconnected but I hope this can help.
    }
    if( async_client_2_disconnects_just_after_connecting ){
      async_client_2_disconnects_just_after_connecting = false;
      //async_client_2.stop(); //although it has just been disconnected but I hope this can help.
    }
    
    //strcpy( just_connected, "\0\0\0\0" ); //it is our convention that just_connected is an array of size 4
    *just_connected = '\0'; //I made just_connected a one char pointer
    
    if( just_connected_1 || just_connected_2 ) {
      //resetting counters
      counter_to_manipulate_sockets = max_counter_to_manipulate_sockets;
      counter_to_destroy_old_socket = max_counter_to_destroy_old_socket;
      counter_to_retry_socket_connection = 0;  
      counter_to_suspect_disconnectivity = max_counter_to_suspect_disconnectivity; /*This is the right place to put this counter in. It may be put harmlessly (and as a preventive measure) somewhere else but I don't think it's necessary really. So I'm satisfied here.*/
      both_ports_not_working = false;
      //Serial.println("Inside PreProcess - counters set in preConnectivity");
    }
/*
    boolean both_connected = false;
    if( just_connected_1 && just_connected_2 ) {
      both_connected = true;
    }  
*/        
    if( just_connected_1 ) {
      //messageAndOperation_1.sendHiIntermediate(); //the idea is that the intermediate server knows the id of this NodeMCU and registers a dedicated Printer there.
      //strcpy( just_connected , "1" );
      *just_connected = '1';
      if( async_client_1.connected() ) { 
        Serial.printf("Evident that async_client_1 of %s is connected\n", IP);
      } else {
        Serial.printf("Evidency is broken!!! async_client_1 of %s is not connected\n", IP);
        //In reality counter_to_manipulate_sockets must now be set again to 0. That is to attempt right away to reconnect to the lost socket. But will it be able to connect? no. So we may be needing another solution..
      }
      just_connected_1 = false;
      last_connected_index = 1;
    }
    
    if( just_connected_2 ) {
      //messageAndOperation_2.sendHiIntermediate(); 
      //strcpy( just_connected , "2" );
      *just_connected = '2';
      if( async_client_2.connected() ) { 
        Serial.printf("Evident that async_client_2 of %s is connected\n", IP);
      } else {
        Serial.printf("Evidency is broken!!! async_client_2 of %s is not connected\n", IP);
      }
      just_connected_2 = false;
      last_connected_index = 2;
    }
/*
    if( both_connected ) {
      //it must never be both
      strcpy( just_connected , "both" );
    }
*/      
  }
  
  void postProcess() {  //this is connectivity check 2
    /*Mechanism: 
     * client_2 is firstly connected at the beginning
     * if client_1 is already connected and it's time to make a new socket with client_2 and we will keep trying with client_2 until it connects 
     */
    //Serial.printf( "%d %d %d %d %d\n", message_received, just_connected_1, just_connected_2, message_in_processing_from_client_1, 
    //                                                                                          message_in_processing_from_client_2 );
    //fixAutonomousDisconnectedSocket(); //this call must be before the following processing, since it relies on the value of last_connected_index.
    if( !async_client_1.connected() ) { 
      if( !async_client_2.connected() ) {
        last_connected_index = 0;
        Serial.printf("toggling notifier pin since panel of %s is not connected to entity\n", IP);
        toggle_connect_failure_notifier_pin = true;
        if( counter_to_retry_socket_connection > 0 ) {
          counter_to_retry_socket_connection--; //a socket is/was connecting (it may succeed or fail)
          if( counter_to_retry_socket_connection == min_threshold_to_force_close_socket ) {
            if( most_recent_client_connect_attempt_index == 2 && still_connecting_2 && !async_client_2.connected() ) {
              still_connecting_2 = false; /*maybe not needed since it'll be set in onDisconnect when it'll be triggered a moment later, but it's better anyway.
              *Another possible reason why this may not be needed for is that this condition will not account anymore on still_connected_2 since 
              *it will not hold to be true because counter_to_retry_socket_connection is continuing the count down. 
               */
              Serial.printf("both clients of %s are not connected. client 2 took too long in attempt to connect. Stopping now the connection attempt.\n", IP);
              //async_client_2.stop(); //I dislike using stop(). I have overcome it through a change in createSocket1() and createSocket2()
              
            } else {
              if( !async_client_1.connected() && still_connecting_1 ) {
                still_connecting_1 = false;
                Serial.printf("both clients of %s are not connected. client 1 took too long in attempt to connect. Stopping now the connection attempt.\n", IP);
                //async_client_1.stop();
              }
            }
          }
        } else {    
          Serial.printf("both clients of %s are not connected. So connecting.\n", IP);
          if( most_recent_client_connect_attempt_index == 1 ) { //it's alternating now between client 1 and 2. So we try with the 2 ports not just with port 3555.
            createSocket2();
          } else {
            createSocket1();
          }
        }
      } else {
        last_connected_index = 2;
        if( counter_to_manipulate_sockets > 0 ) {
          counter_to_manipulate_sockets--;                
        } else { //counter_to_manipulate_sockets is now 0 and it will remain 0 until a new socket is created successfully and onConnect is triggered
          //it is totally not wise to destroy the current_socket because it's the only one left
          if( counter_to_retry_socket_connection > 0 ) {
            counter_to_retry_socket_connection--; //a socket is/was connecting (it may succeed or fail)            
            if( counter_to_retry_socket_connection == min_threshold_to_force_close_socket ) {
              if( !async_client_1.connected() && still_connecting_1 ) { 
                still_connecting_1 = false;
                Serial.printf("client 2 connected. client 1 not connected. client 1 of %s being weird. Stopping client 1 connection attempt\n", IP);
                //async_client_1.stop();
              }
            }
          } else {             
//            counter_to_suspect_disconnectivity--; 
              /*To know why I commented this please look at the method implementation.
               */
            createSocket1();
          }
        }
      }   
    } else { //The following commentary is for clarification
      /*client_1 is connected*/      
      if( !async_client_2.connected() || async_client_2.disconnected() ) { //AFAIB disconnected() is included in the 'or'ed !connected() so we may drop the '|| disconnected()'
        /* We ask ourselves: Is it time to try to connect to port 2 ? */
        last_connected_index = 1;
        if( counter_to_manipulate_sockets > 0 ) {
          counter_to_manipulate_sockets--;                
        } else { 
          /* yes it is the time. Have we tried to connect client_2 before? */          
          if( counter_to_retry_socket_connection > 0 ) {
            counter_to_retry_socket_connection--; //a socket is/was connecting (it may succeed or fail)
            if( counter_to_retry_socket_connection == min_threshold_to_force_close_socket ) {
              if( !async_client_2.connected() && still_connecting_2 ) { 
                still_connecting_2 = false;
                Serial.printf("client 1 connected. client 2 not connected. client 2 of %s being weird. Stopping client 2 connection attempt\n", IP);
                //async_client_2.stop();
              }
            }
          } else {             
//            counter_to_suspect_disconnectivity--;
            createSocket2();     
          }
        }
      } else {
        /* Both clients are connected, so we need to know which one to ask to terminate after counter_to_destroy_old_socket has timed out */          
        if( counter_to_destroy_old_socket > 0 ) {
          counter_to_destroy_old_socket--;
        } else {
          if( most_recent_client_connect_attempt_index == 1 ) { //this seems safe enough because they're both connected.
            if( !message_received2_lock ) {
              if( !async_client_2.disconnecting() ) { //to ensure we enter only once. And it works fine, i.e. we usually enter here.
                Serial.printf("2 clients connected, stopping client_2 of %s\n", IP);
                async_client_2.stop(); 
              }
            }
          } else {
            if( !message_received1_lock ) {              
              if( !async_client_1.disconnecting() ) {
                Serial.printf("2 clients connected, stopping client_1 of %s\n", IP);  
                async_client_1.stop();   
              }           
            }
          }
        }        
      }      
    }
    if( counter_to_suspect_disconnectivity == 0 ) { /*one socket is not connected and had tried to connect unsuccessfully while the other (old) socket 
      * is already connected, 
      * so here we close the old socket to remain with 2 empty sockets.
      * Actually this "if" will be true when counter_to_manipulate_sockets == 0
      * Anyway, IS THIS A GOOD IDEA? Not really. Because 
      *   sometimes you fail to connect to a new socket but the old one is very fine! So disconnecting
      * the working (old) socket is not probably wise. But finally, won't the reflector disconnect our panel? True, it will disconnect there from
      * and, to our convenience, this will be reflected here too.
      * As for the case of real disconnectivity caused by a malfunctioning reflector (like being shut down), then
      * the normal mechanism of counter_to_retry_socket_connection is enough and should work fine.
      * In addition, staying away from forcing stopping a socket is a good thing I guess.
      */
      if( !( async_client_1.connected() && async_client_2.connected() ) ) { //just for compatibility with reasoning. It's already true. 
        if( last_connected_index == 1 ) {
          Serial.printf("After suspecting the disconnectivity, closing the port 1 of %s\n", IP);
          async_client_1.stop();   
          /*Is the following "if" needed? I guess not. Can it be useful? yes.
           * Is it harmful? I guess not.
           * Will fixAutonomousDisconnectedSocket() make a rush? Actually we are already in the rush state!
           */
          if( async_client_2.connected() ) {//usually it's not connected, but this sort of care is necessary to let the program fix itself in case of new modifications.
            last_connected_index = 2;
          } else { 
            last_connected_index = 0;
          }
        } else if( last_connected_index == 2 ) {
          Serial.printf("After suspecting the disconnectivity, closing the port 2 of %s\n", IP);
          async_client_2.stop();
          if( async_client_1.connected() ) {
            last_connected_index = 1;
          } else { 
            last_connected_index = 0;
          }
        }
        both_ports_not_working = true;
      }
      counter_to_suspect_disconnectivity = max_counter_to_suspect_disconnectivity;
    }   
  }

  void stopOperations() {
    message_received1_lock = false;
    message_received2_lock = false;  
    just_connected_1 = false;
    just_connected_2 = false;
    most_recent_client_connect_attempt_index = 2;
    counter_to_manipulate_sockets = 0;
    counter_to_destroy_old_socket = max_counter_to_destroy_old_socket;
    counter_to_retry_socket_connection = 0;    
    counter_to_suspect_disconnectivity = 0;
    both_ports_not_working = false;
    max_counter_to_suspect_disconnectivity = 2; 
    still_connecting_1 = false; 
    still_connecting_2 = false;
    async_client_1_disconnects_just_after_connecting = false;
    async_client_2_disconnects_just_after_connecting = false;    
    for( int i = 0 ; i < max_incoming_messages_number ; i++ ) {/*maybe not necessary but I made it anyway*/
      buff_1[ i ][ 0 ] = '\0';
      len_buff_1[ i ] = 0;
      buff_2[ i ][ 0 ] = '\0';
      len_buff_2[ i ] = 0;
    }
    if( last_connected_index == 1 ) {
      async_client_1.stop();/*perhaps the stop() is the most useful thing to do*/
      NodeMCU::yieldAndDelay(100);   
    }
    if( last_connected_index == 2 ) {
      async_client_2.stop();    
      NodeMCU::yieldAndDelay(100); 
    }  
    both_ports_not_working = false;
    last_connected_index = 0;
  }
  
};
