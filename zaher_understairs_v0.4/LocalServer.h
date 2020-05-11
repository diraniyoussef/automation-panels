#include "Remote.h"

class LocalMessageOp {
private:   
  //by the way another module belonging to the same owner can have its own set of mobiles.
  //const char* owner_id_buff = "Youssef_70853721:\0";
  //const char* mod_id_buff = "4:\0";
  static const int local_server_mob_Number = 4;
  const char* local_server_mob_i_Id_buff[ local_server_mob_Number ] = { "mob1:\0", "mob2:\0", "mob3:\0", "mob4:\0"}; //e.g.(mob_Id_buff + 2)[4] will the 'M' ???  

  char reading_buff[ Max_Reading_Buffer ];
  int readCharNumber;
  Operation oper; 
  boolean is_intelligible_message_context = false; //just to know if theRequest has been initialized.    
  WiFiClient* aClient;    

  void sendAck() {
    NodeMCU::yieldAndDelay(); //I think this delay may be useful...
    char* useful_message_buff = "ACK\0"; //Allocate pointer on the stack and point it to a static, read-only buffer containing ACK\0. So cannot delete.
    int total_message_length = strlen( (const char*) owner_id_buff ) + strlen( (const char*) mod_id_buff ) + 
                                strlen( (const char*) mob_id_buff) + strlen( (const char*) useful_message_buff ) + 1; //the + 1 is to add a \0 to it.
    char* totalMessage_buff = new char[ total_message_length ];
    strcpy( totalMessage_buff, (const char*) owner_id_buff );
    strcat( totalMessage_buff, (const char*) mod_id_buff );
    strcat( totalMessage_buff, (const char*) mob_id_buff );
    strcat( totalMessage_buff, (const char*) useful_message_buff );
    
    totalMessage_buff[ total_message_length - 1 ] = '\0';
    Serial.printf("sending an ack. Message is: %s\n", totalMessage_buff );
    
    total_message_length++;
    if( *aClient )
      if( aClient->connected() )
        aClient->write( (const uint8_t*)totalMessage_buff, total_message_length );

    delete[] totalMessage_buff;
  }
  
  boolean analyze(){ //this method returns true if the received message can be understood    
    Serial.printf("Analyzing local incoming message  %s of length %d\n", reading_buff, readCharNumber);
    is_intelligible_message_context = false;    
    the_request.pin = '\0'; //this will be the guide if the incoming message was only R? or O3T? e.g.    
    int lastColonOfIncomingMessage = RemoteServerMessageOp::getThirdOccurrenceOfCharInUsefulMessage( ':', reading_buff, readCharNumber);//useful characters in reading_buff are from 0 to readCharNumber - 1    
    if( lastColonOfIncomingMessage == -1 ) {
      Serial.println("couldn't find the third colon of  the incoming message");
      return false;
    }
    if( readCharNumber - 1 >= lastColonOfIncomingMessage + 2 ) { //the +2 is for the 2 chars R?
        //Because of using lastColonOfIncomingMessage then we must call sendAck() before analyze()
      if (reading_buff[lastColonOfIncomingMessage + 1] == 'R' && reading_buff[lastColonOfIncomingMessage + 2] == '?') { //a report request for pins status        
        is_intelligible_message_context = true;        
        //the_request.pin is still '\0'; 
        Serial.println("local incoming message is really an 'R?'");
        return (true);
      
      } else if( strcmp(Panel_Type, "2O") == 0 ) {
                
        //typically it's like mob1:O1T?
        if (readCharNumber - 1 >= lastColonOfIncomingMessage + 4){
          if (reading_buff[lastColonOfIncomingMessage + 1] == 'O'){
            //now making sure the incoming message indicates the pin clearly
            if ( General::arrayIncludeElement( out_pin , sizeof(out_pin) , reading_buff[lastColonOfIncomingMessage + 2] ) != -1            
              //|| General::arrayIncludeElement( PCF1.out_pin_symbol , PCF1.out_pins_number , reading_buff[ lastColonOfIncomingMessage + 2 ] ) != -1 //comment for PCF exclusion
            ) {
              if ( reading_buff[lastColonOfIncomingMessage + 3] == 'T' || reading_buff[lastColonOfIncomingMessage + 3] == 'F' ) {
                boolean actionType;
                if ( reading_buff[ lastColonOfIncomingMessage + 3 ] == 'T' ) {
                  actionType = true;
                } else {
                  actionType = false;
                }
                the_request.getPinAndAction( reading_buff[lastColonOfIncomingMessage + 2], actionType );
                is_intelligible_message_context = true;        
                return(true);
              } else {
                //Serial.println("Useful received local message doesn't state whether to turn the pin on or off.");
              }
            } else {
              //Serial.println("Useful received local message doesn't state the pin number.");
            }            
          } else {
            //Serial.println("Useful received local message doesn't start with R or O.");
          }
        } else {
          //Serial.println("Useful received local message is less than 3 chars and is not \"R?\"");
        }
      
      }
    } else {
      //something is wrong in the received message. But I won't feedback the user.
      //Serial.println("Useful received local message is less than 2 chars.");
    }
    return (false);
  }
  
  boolean isJustReport() {
    if( is_intelligible_message_context ) { //the protection is not necessary if the method is correctly used.
      return( the_request.isJustReport() );
    }
    return( true );
  }
  

  void updatePinAndEEPROM() { //it is bad to enter this method if the pcf was not connected.
    char symbol = the_request.pin;
    boolean state_bool = the_request.action;
    if( General::arrayIncludeElement( out_pin , sizeof(out_pin) , symbol ) != -1 ) {
      Serial.println("pin is updated in LocalServer");
      NodeMCU::setOutPinStateAsConsidered( General::getIntFromHexChar( symbol ) , state_bool );
      NodeMCU::encodeEEPROM( symbol , state_bool );
    }
    //else if( ! PCF1.updatePinAndEEPROM( symbol , state_bool ) ) {  //the return value of updatePinAndEEPROM method of PCF is just to know if the pin belongs to that particular PCF or not. //comment for PCF exclusion
    //  if( ! PCF2->updatePinAndEEPROM( symbol, state_bool ) ) {
    //    ...
      //} 
    //} //comment for PCF exclusion
  }
  

public:        
  char mob_id_buff[ Max_Reading_Buffer ]; 
  Request the_request;   

  void setWiFiClient( WiFiClient* aClient ) {
    this->aClient = aClient;
    oper.setOperInfo( owner_id_buff, local_server_mob_Number, local_server_mob_i_Id_buff, mod_id_buff );
  }
  
  boolean getMessage() {    
    if( aClient->available() ) { //This will always be true, nevertheless it's good to protect it
      readCharNumber = aClient->readBytesUntil('\0', reading_buff, Max_Reading_Buffer); //BTW the '\0' is not counted in the value of readCharNumber - tested
      reading_buff[ Max_Reading_Buffer - 1 ] = '\0';
      //Serial.printf("Read char number of the incoming message is %d\n", readCharNumber);
      NodeMCU::yieldAndDelay();
      return true;               
    } else {
      return false;
    }
  }  

  void getIdOfClient() {    
    mob_id_buff[0] = '\0';
    //Serial.println("Trying to get the id of local incoming message");
    oper.getIdOfClient( mob_id_buff, reading_buff );
  }  

  void sendReport() { //Inside this method we take care of the case if the pcf was not connected.
    NodeMCU::yieldAndDelay(); //I think this delay may be useful...
    int last_length = strlen( (const char*) owner_id_buff ) + strlen( (const char*) mod_id_buff ) + strlen( (const char*) mob_id_buff );
       
    char* totalMessage_buff;
    if( strcmp( Panel_Type, "TempHum" ) == 0 ) {
      totalMessage_buff = new char[ last_length + TempHum::Humidity_Int_Size + 1 + TempHum::Temperature_Float_Size + 1 ];
      Serial.printf("Allocated size to totalMessage_buff is %d\n", last_length + TempHum::Humidity_Int_Size + 1 + TempHum::Temperature_Float_Size + 1 );
    } else {
      totalMessage_buff = new char[ last_length + 3 * PCF::absolute_max_pins_number + 1 ];
    }
    strcpy( totalMessage_buff, (const char*) owner_id_buff );
    strcat( totalMessage_buff, (const char*) mod_id_buff );
    strcat( totalMessage_buff, (const char*) mob_id_buff );

    if( strcmp( Panel_Type, "TempHum" ) == 0 ) {
      last_length = RemoteServerMessageOp::addTempAndHumToReport( totalMessage_buff, last_length );
    } else { //meaning if( strcmp(Panel_Type, "2O") == 0 )
      last_length = RemoteServerMessageOp::addPinsToReport( totalMessage_buff, last_length ); //nothing is special about RemoteServerMessageOp, it's just the addPinsToReport    
    }
       
    totalMessage_buff[ last_length ] = '\0'; //not needed since already made inside the previous command
    //last_length++; //is it necessary?
    Serial.printf("Responding to a local request with a report. Message is: %s\n", totalMessage_buff );

    if (*aClient) {
      if (aClient->connected()) {
        aClient->write( (const uint8_t*)totalMessage_buff, last_length );       
        //Serial.printf( "Sent message was like %s\n", String(usefulOutMessage_buff).c_str() );
      } else {
        //Serial.println("aClient is not connected.");
      }
    } else {
      //Serial.println("aClient is null.");
    }
    
    delete[] totalMessage_buff;
  }
  
  void processMessage() {
    sendAck();
    if( analyze() ) {
      Serial.printf("analyze() in LocalServer is right. \n" );
      if( strcmp(Panel_Type, "2O") == 0 ) {
        if (!isJustReport()) {
          NodeMCU::yieldAndDelay(); //take a breath between two successive writings
          updatePinAndEEPROM();
        }
      }
      NodeMCU::yieldAndDelay();
      sendReport();
    } else {
      Serial.printf("analyze() in LocalServer is not right. \n" );
    }
  }
  
};

class LocalIncomingClient {
private:
  const int max_counter_to_assign_a_new_client = 3 * 60 * 1000;
    
public:
  int max_loop_counter_to_assign_a_new_client;
  WiFiClient theClient;
  LocalMessageOp m_and_op;

  LocalIncomingClient() {
    max_loop_counter_to_assign_a_new_client = floor( max_counter_to_assign_a_new_client / delay_per_loop );//it's like 250 times for a delay of 750 ms.     
    m_and_op.setWiFiClient( &theClient );
  } 
  
  int counter_to_assign_a_new_client = 0;
  
  void reset() {
    counter_to_assign_a_new_client = 0; 
    m_and_op.mob_id_buff[0] = '\0';
  }
};

class LocalServer {
private:
  static const int Max_Concurrent_Clients = 4;
  static const int Local_Server_Number = 2; //This will remain 2 in my convention. Never changed
  LocalIncomingClient client_[ Max_Concurrent_Clients ][ Local_Server_Number ];
  WiFiServer server[Local_Server_Number] = {
    WiFiServer(11359), //120 - 3552
    WiFiServer(11360)  //121 - 3553
  };
  
  void stopAllClientsOnPort( int port_i ) {
    LocalIncomingClient* theClientObj;
    WiFiClient* theClient;
    for( int client_i = 0 ; client_i < Max_Concurrent_Clients ; client_i++ ) {
      theClientObj = &client_[client_i][port_i]; //for reading simplicity.
      theClient = &theClientObj->theClient;
      theClientObj->reset();
      if( *theClient ) {
        if( theClient->connected() ) {
          client_[client_i][port_i].theClient.flush();
          NodeMCU::yieldAndDelay();
          client_[client_i][port_i].theClient.stop();        
        }
      }
      //client_[client_i][port_i].just_connected = false;
    }
    NodeMCU::yieldAndDelay(50);
  }
  
public:
  void serverSetup() {          
    for ( int i = 0; i < Local_Server_Number; i++ ) {
      server[i].begin();
      server[i].setNoDelay(true); //I think everything you send is directly sent without the need to wait for the buffer to be full
      NodeMCU::yieldAndDelay();
    }
  }

  void stopOperations() {    
    for( int port_i = 0 ; port_i < Local_Server_Number ; port_i++ ) {
      stopAllClientsOnPort( port_i );
      server[ port_i ].stop(); /*tested without problem if we enter this without beginning the server beforehand. Although it won't happen anyway.*/
      NodeMCU::yieldAndDelay(200);
    }
  }
  
  void process() {
    boolean clientToBeCreated = false;
    LocalIncomingClient* theClientObj;
    WiFiClient* theClient;
    
    for( int port_i = 0 ; port_i < Local_Server_Number ; port_i++ ) {
      for( int client_i = 0 ; client_i < Max_Concurrent_Clients ; client_i++ ) {
        theClientObj = &client_[client_i][port_i]; //for reading simplicity. Operator precedence is respected.
        theClient = &theClientObj->theClient;
        //Serial.printf("client_i = %d of port_i = %d\n", client_i, port_i);
        clientToBeCreated = false;
  
        //This "if" block checks if the client is to be nulled, and increments the client's freeing-resource-counter.
        if( !(*theClient) ) { //theClient is already instantiated when clients was. (Be carful with this. If never connected before, it returns false...)
          clientToBeCreated = true;
//          Serial.printf("client_i = %d of port_i = %d was never connected.\n", client_i, port_i);
        } else {
          if ( !theClient->connected() ) { //I think there is not a real need for this test, because it may be the same test as if( !(*theClient) ). Anyway it's ok
            //not sure if wise to do ...->theClient.stop() //I think it's not useful at all.
            clientToBeCreated = true;      
//            Serial.printf("client_i = %d of port_i = %d was once connected but not now.\n", client_i, port_i);     
          } else {
//            Serial.printf("client_i = %d of port_i = %d is now connected.\n", client_i, port_i);
            //Serial.println("It's connected again");
            theClientObj->counter_to_assign_a_new_client++;
            if ( theClientObj->counter_to_assign_a_new_client == theClientObj->max_loop_counter_to_assign_a_new_client ) {               
              if ( theClientObj->m_and_op.mob_id_buff[0] != '\0' ) { //was never processed before              
                theClientObj->m_and_op.sendReport();
              }
//              Serial.printf("client_i = %d of port_i = %d will be stopped to free resources.\n", client_i, port_i);
              theClient->stop();//hopefully that when destroying it from the server, the client would be notified in his Android code
              clientToBeCreated = true;
            }
          }
        }
  
        if ( clientToBeCreated ) {
          //resetting stuff related to m_and_op field is up to that class itself.
          theClientObj->reset();          
          //It is ok not to delete m_and_op because all its member variables can be reused just fine.
          
          //Now trying to get a new client.
          *theClient = server[port_i].available(); 
          NodeMCU::yieldAndDelay();              
        }
      } //end for
    } //end for
    
    //NodeMCU::yieldAndDelay(50);
    //Now listen to clients and process the ones that sent something     
    for(int port_i = 0; port_i < Local_Server_Number; port_i++) {
      for(int client_i = 0; client_i < Max_Concurrent_Clients; client_i++) {
        theClientObj = &client_[client_i][port_i]; //for reading simplicity.
        theClient = &theClientObj->theClient;
        //Serial.printf("client_i = %d of port_i = %d\n", client_i, port_i);    
        
        if( *theClient ) {
          if( theClient->connected() ) {
//            Serial.printf("client_i = %d of port_i = %d is always connected.\n", client_i, port_i);
            if( theClientObj->m_and_op.getMessage() ) {
              //process received message
//              Serial.printf("A message is received from client_i = %d of port_i = %d\n.", client_i, port_i);
              boolean processClient = false;
              if ( theClientObj->m_and_op.mob_id_buff[0] == '\0' ) { //was never processed before
                theClientObj->m_and_op.getIdOfClient( ); //this also checks if the received message is valid in terms of signature
                
                boolean client_to_be_deleted = false;
                if ( theClientObj->m_and_op.mob_id_buff[0] == '\0' ) { //this happens if the method setIdOfClient found that the mobile does not belong to the owner's family                
                  //Serial.println("local client to be deleted after mob id not being recognized");
//                  Serial.printf("id failure for message so client will be deleted/freed.\n");
                  client_to_be_deleted = true;
                } else { 
  /*                if ( clients->idPreExist( port_i, id ) ){ //it should not exist unless the user somehow installed the app on another mobile.
                                                              // This is not effective as a security measure since the mobile app will enhance the connection 
                                                              //  by going to the other server (the other port) so the 2 mobiles (with the same id) will function
                                                              // simultaneously on 2 servers.
                    client_to_be_deleted = true;
                  } else { 
                    */
                    //accepted as a client
                    processClient = true;                             
  //                }
                }
                if ( client_to_be_deleted ) {
                  //Serial.println("this local socket's message was not suitable to come from a member of the family");
                  theClient->stop();
                  theClientObj->reset();
                }
              } else {
                processClient = true;
              }
              
              if ( processClient ) { 
                //Serial.println("this local socket is to be processed");
                theClientObj->m_and_op.processMessage();
              }
              NodeMCU::yieldAndDelay(5);
            } else {
              //Serial.println("Nothing available from local client.");
            }          
          } else {
            //Serial.println("Client was once connected but not now.\n");      
          }
        } else {
          //Serial.println("Client was never connected.\n");      
        }
      }
    }

  }
};
