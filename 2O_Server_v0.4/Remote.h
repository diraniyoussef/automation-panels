#include "TempHum.h"
#include "espconn.h"
TempHum tempHum;

//it makes sense to define here the remote IP for both classes RemoteServer and InformEntity because they both use the same connection.
AsynchroClient remote_connection; //intermediate relay server

const char* owner_id_buff = "zaher's house:\0";
const char* mod_id_buff = "understairs_panel:\0"; //please select fine values
const char* Panel_Type = "2O"; //either "2I", "2O", or "TempHum"

class RemoteServerMessageOp {
private:       
  //the dummy part
  static constexpr char* dummy_buff = "dummy:\0";

  static const int mob_Number = 2; //Don't forget to add "dummy" in the intermediate server.
  //const char* mob_i_Id_buff[ mob_Number ] = { dummy_buff, "S4:\0", "TrekStor_Tab:\0", "S7_Edge:\0"};
  const char* mob_i_Id_buff[ mob_Number ] = { dummy_buff, "mob1:\0" };
  //const char* mob_i_Id_buff[ mob_Number ] = { dummy_buff }; //"dummy" thing: If I were to use dummy at all (because ...), and if I were to make sure it is connected to the intermediate relay server, then I would be using the ack mechanism, and the incoming ack will have "dummy" in its signature, then I will have to have "dummy" in mob_i_Id_buff array.
                                                                            // I won't use dummy here I'm using InformRemoteAPI which takes care of the connection reliability through its mechanism. (BUT THAT WASN'T ENOUGH SINCE WE NEED TO REGISTER AT THE INTERMEDIATE AT THE SOCKET SWITCH)
                                                                            // For the same reason I won't be sending Hi to intermediate because InformRemoteAPI takes care of connection reliablity through sending messages and receiving replies back (NOT AT SWITCHING TIME), and by these sent messages, this panel registers itself in the intermediate server.
  //const char* owner_id_buff = "Youssef_70853721:\0";
  //const char* mod_id_buff = "4:\0";

  char* reading_buff; //BTW, this is never changed in its own set of operations. Only changed when we have a new message in a new loop.
  int read_char_number = 0;
  Operation oper;
  boolean is_intelligible_message_context = false; //just to know if theRequest has been initialized.    

  static void sendPredefinedMessage( AsyncClient* async_client, char* useful_message_buff, const char* mob_id_buff ) {
    int total_message_length = strlen( (const char*) owner_id_buff ) + strlen( (const char*) mod_id_buff ) + 
                                strlen( (const char*) mob_id_buff ) + strlen( (const char*) useful_message_buff ) + 2; //the + 1 is to add a \0 to it.
    char* totalMessage_buff = new char[ total_message_length ];
    strcpy( totalMessage_buff, (const char*) owner_id_buff );
    strcat( totalMessage_buff, (const char*) mod_id_buff );
    strcat( totalMessage_buff, (const char*) mob_id_buff );
    strcat( totalMessage_buff, (const char*) useful_message_buff );

    totalMessage_buff[ total_message_length - 2 ] = '\\';
    totalMessage_buff[ total_message_length - 1 ] = '\0';

    sendMessageToAsync( async_client, totalMessage_buff );

    delete[] totalMessage_buff;
  }
  
public:
  
  
  Request the_request; //this is not null only if analyze() method is called.    
  char mob_id_buff[ Max_Reading_Buffer ];  

  /*
  static int getLastCharOccurenceOfMessage( char charToCheck, char* reading_buff, int searchUpToIndex ) {
    for ( int i = searchUpToIndex - 2 ; i >= 0 ; i-- ) { 
      if (reading_buff[i] == charToCheck) { 
        return (i);
      }
    }
    return(-1);
  }
  */

  void stopOperations() {
    if( reading_buff )
      reading_buff[0] = '\0';
    read_char_number = 0;
    is_intelligible_message_context = false;
    the_request.stopOperations(); /*not necessary since it'll be overwritten, but ok*/
    mob_id_buff[0] = '\0';    
  }
  
  //I find the following method better than getLastCharOccurenceOfMessage in case many messages are being buffered and received by async_client
  static int getThirdOccurrenceOfCharInUsefulMessage( char charToCheck, char* read_buff, int max_buff_index ) {
    int index = 0;
    int i;
    for( int j = 1 ; j <= 3 ; j++ ) {      
      for ( i = index ; i < max_buff_index ; i++ ) { 
        //Serial.printf("j= %d and i = %d\n", j , i );
        if( read_buff[i] == charToCheck ) {
          index = i + 1;
          //Serial.printf("found ':' at position %d\n", i);
          break;
        }
      }      
    }
    if( i == max_buff_index ) { //this protection is not really needed since we know the ":" will occur at least 3 times.
      return -1;
    } else {
      return i;
    }
  }
  
  void setup( ) {
    oper.setOperInfo( owner_id_buff, mob_Number, mob_i_Id_buff, mod_id_buff );
  }
  
  boolean getMessage( char* message_buff, int read_char_number ) {    
    if ( read_char_number > 0 ) {
      reading_buff = message_buff;
      reading_buff[ Max_Reading_Buffer - 1 ] = '\0';
      this->read_char_number = read_char_number;
      return true;
    } else {
      return false;
    }
  }

  void getRequesterId() {    
    mob_id_buff[0] = '\0';
    oper.getIdOfClient( mob_id_buff, reading_buff );
  }
  
  
  static void sendHiIntermediate( AsyncClient* async_client ) { //we won't be using this since we do have InformRemoteAPI - dummy thing is really 
    sendPredefinedMessage( async_client , "HI\0" , dummy_buff ); 
  }

  boolean analyze( boolean* is_ack_hi_received ){ 
    is_intelligible_message_context = false;    
    int lastColonOfIncomingMessage = getThirdOccurrenceOfCharInUsefulMessage( ':', reading_buff, read_char_number);
    
    if( read_char_number - 1 >= lastColonOfIncomingMessage + 2 ) { 
      if ( reading_buff[ lastColonOfIncomingMessage + 1 ] == 'R' && reading_buff[ lastColonOfIncomingMessage + 2 ] == '?' ) {
        is_intelligible_message_context = true;        
        the_request.pin = '\0'; 
        return (true);             
      } else if( strcmp(Panel_Type, "2O") ) {
        if (read_char_number - 1 >= lastColonOfIncomingMessage + 4){
          if( reading_buff[ lastColonOfIncomingMessage + 1 ] == 'O' ) {
            if ( General::arrayIncludeElement( out_pin , sizeof(out_pin) , reading_buff[lastColonOfIncomingMessage + 2] ) 
            //PLEASE BEWARE THAT I MADE A CHANGE ABOUT THE RETURN VALUE OF arrayIncludeElement
            
              //|| General::arrayIncludeElement( PCF1.out_pin_symbol , PCF1.out_pins_number , reading_buff[lastColonOfIncomingMessage + 2] ) //comment for PCF exclusion
            ) {
              if ( reading_buff[lastColonOfIncomingMessage + 3] == 'T' || reading_buff[lastColonOfIncomingMessage + 3] == 'F' ) {
                boolean actionType;
                if (reading_buff[lastColonOfIncomingMessage + 3] == 'T') {
                  actionType = true;
                } else {
                  actionType = false;
                }
                //Serial.printf("In Analyze, the pin is %c and the action is ...", reading_buff[lastColonOfIncomingMessage + 2] );
                the_request.getPinAndAction( reading_buff[ lastColonOfIncomingMessage + 2 ], actionType );
                is_intelligible_message_context = true;
                return(true);
              } else {
                //Serial.println("Useful received message doesn't state whether to turn the pin on or off.");
              }
            } else {
              //Serial.println("Useful received message doesn't state the pin number.");
            }
          } else {
            //Serial.println("Useful received message doesn't start with R or O.");
          }
        } else {
          //Serial.println("Useful received message is less than 3 chars and is not \"R?\"");
        }        
      }
      if ( read_char_number - 1 >= lastColonOfIncomingMessage + 3 ) { 
        if ( reading_buff[ lastColonOfIncomingMessage + 1 ] == 'A' && reading_buff[ lastColonOfIncomingMessage + 2 ] == 'C' && 
              reading_buff[ lastColonOfIncomingMessage + 3 ] == 'K' ) {
          //received an ack from intermediate (it's signed by dummy as the sender BTW)
          *is_ack_hi_received = true;
          Serial.println("In remote server: received an ack from Hi");
          return true;
          //not interested in returning true actually. Keeping that for report requests.
        }
      }
    } else {
      Serial.println("Useful received message is less than 2 chars.");
    }    
    return (false);
  }

  boolean isJustReport() {
    if( is_intelligible_message_context ){ //the protection is not necessary if the method is correctly used. I.e. we always enter here.
      return( the_request.isJustReport() );
    }
    return( true );
  }

  void updatePinAndEEPROM() { //it is bad to enter this method if the pcf was not connected.
    char symbol = the_request.pin;
    boolean state_bool = the_request.action;
    if( General::arrayIncludeElement( out_pin , sizeof(out_pin) , symbol ) ) {
    //PLEASE BEWARE THAT I MADE A CHANGE ABOUT THE RETURN VALUE OF arrayIncludeElement
    
      digitalWrite( NodeMCU::getRealPinFromD( General::getIntFromHexChar( symbol ) ), state_bool );
      NodeMCU::encodeEEPROM( symbol , state_bool );
    }     
    //else if( ! PCF1.updatePinAndEEPROM( symbol, state_bool ) ) {  //the return value of updatePinAndEEPROM method of PCF is just to know if the pin belongs to that particular PCF or not. //comment for PCF exclusion
      
    //  if( ! PCF2->updatePinAndEEPROM( symbol, state_bool ) ) {
    //    ...
    //  } 
      
    //}    //comment for PCF exclusion
  }
 


  static int addTempAndHumToReport( char* totalMessage_buff, int last_length ) {
    /*Now adding the temperature as chars onto the array, then null char, then concatenating directly the humidity, then a null char */
    
    TempHumValue tempHumValue = tempHum.readValues();
    float temperature = tempHumValue.temperature;
    int humidity = tempHumValue.humidity;
    Serial.printf("temperature is %f and humidity is %d\n", temperature, humidity);    
    /*https://stackoverflow.com/questions/27651012/arduino-sprintf-float-not-formatting this is for converting a float onto a C-style string
    * or this if you like https://stackoverflow.com/questions/27651012/arduino-sprintf-float-not-formatting
    * As for converting an int onto a C-style string, you may use itoa(). Using sprintf() is not recommended according to 
    * https://forum.arduino.cc/index.php?topic=596935.0 or http://www.cplusplus.com/reference/cstdlib/itoa/
    * So here is the syntax for both :
    * dtostrf( float_or_double, total_number_of_chars_including_the_decimal_point , number_of_decimal_places, final_buffer_array_or_pointer ); 
    * As for the total_number_of_chars_including_the_decimal_point, it doesn't have to be increased by 1 to consider the final null char, and I
    * didn't increase it as it would write it to the right not to the left (a bit weird).
    * itoa( i, buffer, 10 );
    * 
    * The format is as follows :
    * humidity->(1 or more) trailors -> temperature -> 1 or more null chars
    * temperature is like -.- or --.- (from 0.0 to 99.9)
    * humidity is like --- (from 0 to 100)
    * 9->trailor->trailor->trailor->1->2->.->6->'\0'->... (no new trailors at the end. Just null chars)
    * or 
    * 2->6->trailor->trailor->0.7->'\0'->... (no new trailors at the end. Just null chars) etc..
    */
    if( humidity > 100 ) {
      humidity = 100;
    }
    if( humidity < 0 ) { //won't happen
      humidity = 0;
    }
    itoa( humidity, totalMessage_buff + last_length, 10 );//10 means decimal, 2 means binary, ... http://www.nongnu.org/avr-libc/user-manual/group__avr__stdlib.html#gaa571de9e773dde59b0550a5ca4bd2f00
    //Serial.printf(); //let's print the characters. 
    /*I think it automatically places a terminating string like a null char, but this is bad because I still have the humidity to add to the buffer to send.
     * So I think I will replace the null char by a trailor like 127 (defined in Setup.h) and if there is room left in the dedicated temperature chunk 
     * that is unfilled, I think I will fill it by trailors as well.
     */
    int number_of_filled_chars = 0; 
    if( humidity == 100 ) {
      number_of_filled_chars = 3;
      Serial.printf("humidity is equal to 100\n");
    }
    if( humidity < 100 ) {
      Serial.printf("humidity is smaller than 100\n");
      number_of_filled_chars = 2;
    }
    if( humidity < 10 ) {
      Serial.printf("humidity is smaller than 10\n");
      number_of_filled_chars = 1;
    }    
    
    const char trailor = 't'; //as for temperature. The trailor being 127 didn't work, maybe since this is a char array and not a byte array
    for( int i = last_length + number_of_filled_chars ; i < last_length + TempHum::Humidity_Int_Size + 1 ; i++ ) {
      totalMessage_buff[ i ] = trailor;
      Serial.printf("putting a trailor at i %d\n", i);
    }
    Serial.printf("Till now totalMessage_buff is: %s\n", totalMessage_buff );
    
    last_length = last_length + TempHum::Humidity_Int_Size + 1; 
    
    if( temperature > 99 ) { //won't be more than 60 as for the used sensor module.
       temperature = 99; //IDK if better to put 99f or 99.0 but anyway...
    }
    if( temperature < 0 ) { //won't happen
      temperature = 0;
    }
    for( int i = last_length ; i < last_length + TempHum::Temperature_Float_Size + 1 ; i++ ) {
      totalMessage_buff[ i ] = '\0'; //for technical reasons.
      Serial.printf("putting a null char at i %d\n", i);
    }
    dtostrf( temperature, TempHum::Temperature_Float_Size, TempHum::Temperature_Decimals, totalMessage_buff + last_length ); /*I believe 
    * the +1 to the TempHum::Temperature_Float_Size is not needed here but it's ok.
    */
    //I believe it must end with something like a null char.
    for( int i = last_length ; i < last_length + TempHum::Temperature_Float_Size ; i++ ) {      
      Serial.printf("Reading totalMessage_buff[ i ] after putting the temperature in it. At i %d , totalMessage_buff[ i ] is %c\n", i, totalMessage_buff[ i ] );
    }   
    
    last_length = last_length + TempHum::Temperature_Float_Size; //very wrong to add + 1
    
    return last_length;
  }
  
  static int addPinsToReport( char* totalMessage_buff, int last_length ) {
    int pins_to_report_about = sizeof( in_pin );
    for( int i = 0 ; i < pins_to_report_about ; i++ ) {
      totalMessage_buff[ last_length ] = 'O';
      char stateOfPin;
      int pinAsInt = General::getIntFromHexChar( in_pin[i] );
      if( NodeMCU::getInPinStateAsConsidered( pinAsInt ) ) { //NodeMCU::getRealPinFromD(i) is the same as saying Di as of D1 or D2 or ...
        stateOfPin = 'T';                       
      } else {
        stateOfPin = 'F';
      }
      totalMessage_buff[ last_length + 1 ] = in_pin[i];
      totalMessage_buff[ last_length + 2 ] = stateOfPin;
      last_length = last_length + 3;      
    }
    /* //comment for PCF exclusion
    if(! PCF1.addPinsToReport( totalMessage_buff , &last_length ) ) {//This also tests whether the PCF is connected.
      //Serial.println("message not sent since the PCF is disconnected");
      //don't send the message.
      return;
    }
    */
    return last_length;
  }

  static void sendMessageToAsync( AsyncClient* async_client, char* message ) {
    Serial.printf("Sending... Message  is: %s\n", message);
    if( async_client )
      //if( *async_client ) //does not compile. Unlike WiFiClient
        if( async_client->connected() )
          async_client->write( (const char*) message );
  }

  void sendReport( AsyncClient* newest_async_client ) {
    sendReport( newest_async_client, mob_id_buff );
  }
  
  static void sendReport( AsyncClient* newest_async_client, const char* destination_id_buff ) {    
    NodeMCU::yieldAndDelay(); //I think this delay may be useful...
    int last_length = strlen( (const char*) owner_id_buff ) + strlen( (const char*) mod_id_buff ) + strlen( (const char*) destination_id_buff );  //strlen still works fine with destination_id_buff  
    //char* totalMessage_buff = new char[ last_length + 3 * PCF::absolute_max_pins_number + 2 ]; //the + 2 is the '\\' and the '\0'
    //Serial.printf("Allocated size to totalMessage_buff is %d\n", last_length + TempHum::Humidity_Int_Size + 1 + TempHum::Temperature_Float_Size + 1  + 1);
    char* totalMessage_buff = new char[ last_length + TempHum::Humidity_Int_Size + 1 + TempHum::Temperature_Float_Size + 2 ]; //the + 2 is the '\\' and the '\0'
    strcpy( totalMessage_buff, (const char*) owner_id_buff );
    strcat( totalMessage_buff, (const char*) mod_id_buff );
    strcat( totalMessage_buff, (const char*) destination_id_buff );       
    
    if( strcmp( Panel_Type, "TempHum" ) ) {
      last_length = RemoteServerMessageOp::addTempAndHumToReport( totalMessage_buff, last_length );
    } else { //meaning if( strcmp(Panel_Type, "2O") )
      last_length = addPinsToReport( totalMessage_buff, last_length );
    }

    totalMessage_buff[ last_length ] = '\\';
    totalMessage_buff[ last_length + 1] = '\0';
    
    //totalMessage_buff[ last_length ] = '\0'; //does not work unfortunately I guess.
    sendMessageToAsync( newest_async_client, totalMessage_buff );

    delete[] totalMessage_buff;
  }  

  void processMessage( boolean* is_ack_hi_received ) { //we have a message when we enter here
    getRequesterId();
    if( mob_id_buff[0] != '\0' ) {//this is for authentication
      if( analyze( is_ack_hi_received ) ) { //this is to check if message context is intelligible
        if( *is_ack_hi_received ) { //if has been set in analyze(..) call then it's an ack of HI.
          return;
        }
        if( strcmp(Panel_Type, "2O") ) {
          if( !isJustReport() ) {
            updatePinAndEEPROM();
          }
        }
      //The following will be commented if this panel won't get any report requests from any mobile app.    
        if( remote_connection.async_client_2.connected() ) {
          Serial.printf("async_client_2 of IP %s is connected, so sending a report through it\n", remote_connection.IP );
          sendReport( &remote_connection.async_client_2 );
        }
        if( remote_connection.async_client_1.connected() ) {
          Serial.printf("async_client_1 of IP %s is connected, so sending a report through it\n", remote_connection.IP );
          sendReport( &remote_connection.async_client_1 );
        }

      }            
    } else {
      Serial.println("Remote server: unagreed signature maybe, but only for Remote Server, not necessarily for Inform Remote !");
    }
  }
  
};

class InformEntity {
private:
  boolean* is_awaiting_ack; //this variable is only used in remote (not local). So we NEED TO TEST THIS each time we want to set or get it if it's null or not. //A side note: it might had been enough to have one is_awaiting_ack variable for all remote entities instead of one for each remote entity, but having one for each remote entity might present a tiny advantage and I will seek for it!
  int panels_number;  
  char** panel_id;
  int max_counter_to_await_report_receipt;
  AsynchroClient** asynchro_client;
  void (*sendReportMethod)( AsyncClient* , const char* ); //function pointer
  
  //message handling variables sendReportMethod = RemoteServ
  char* read_buff;
  int read_char_number = 0;
  char panel_id_buff[ Max_Reading_Buffer ]; //this will be the panel id of the received message which is one item of the panel_id char pointer array

  void sendReportAndSetCounter( int i ) {
    //We want to send through every possible async client. Best effort approach.
    if( asynchro_client[ i ]->async_client_1.connected() ) {
      Serial.printf("async_client_1 of IP %s is connected, so sending a report through it\n", asynchro_client[ i ]->IP );
      sendReportMethod( &asynchro_client[ i ]->async_client_1 , panel_id[ i ] );
      counter_to_await_report_receipt[ i ] = max_counter_to_await_report_receipt;
      if( is_awaiting_ack ) {
        is_awaiting_ack[ i ] = true;
      }
    }
    if( asynchro_client[ i ]->async_client_2.connected() ) {
      Serial.printf("async_client_2 of IP %s is connected, so sending a report through it\n", asynchro_client[ i ]->IP );
      sendReportMethod( &asynchro_client[ i ]->async_client_2 , panel_id[ i ] );
      counter_to_await_report_receipt[ i ] = max_counter_to_await_report_receipt;
      if( is_awaiting_ack ) {
        is_awaiting_ack[ i ] = true;
      }
    }
  }

  int getInformedEntityId( char* read_buff, int read_char_number ) {
    this->read_buff = read_buff;
    this->read_char_number = read_char_number;    
    panel_id_buff[0] = '\0';
    return oper.getIdOfClient( panel_id_buff , this->read_buff ); //BTW we won't change the second argument of getIdOfClient
  }

  void updateNodePinAndEEPROM( char symbol , boolean state_bool ) { //it is bad to enter this method if the pcf was not connected.
    //digitalWrite( NodeMCU::getRealPinFromD( General::getIntFromHexChar( symbol ) ), state_bool );
    NodeMCU::setOutPinStateAsConsidered( General::getIntFromHexChar( symbol ) , state_bool );
    NodeMCU::encodeEEPROM( symbol , state_bool );
  }

  boolean analyze( int entity_index ) {
    boolean is_intelligible_report = false;    
    int colon_index = RemoteServerMessageOp::getThirdOccurrenceOfCharInUsefulMessage( ':', read_buff, read_char_number );
    Serial.printf("Analyzing in Inform. Received message is %s\n", read_buff);
    Serial.printf("Analyzing in Inform. read_char_number - 1 is %d, and colon_index + 4 is %d\n", read_char_number - 1, colon_index + 4);
    Serial.printf("Analyzing in Inform. colon_index is %d\n", colon_index);
    //Serial.println("BTW, it is weird that read_char_number - 1 is not = to colon_index + 4," 
      //        " even though the message as I read it indicates that they should! For some reason, read_char_number is added 1 on its own!"); 
      /*
       * Anyway this doesn't hurt since there may be for any stupid reason an additional char at the beginning so the >= in the following "if"
       * is just fine.
       */
    if( read_char_number - 1 >= colon_index + 3 * In_Pins_Number ) {
      int test_index = colon_index;
      for( int i = 0 ; i < In_Pins_Number ; i++ ) {
        if( read_buff[ test_index + 1 ] == 'O') {
          is_intelligible_report = true;
          //Serial.printf("Analyzing, 'O' is fine\n");
          int pin_index = General::arrayIncludeElement( (char*) in_pin , In_Pins_Number , read_buff[ test_index + 2 ] );
          /*
          if ( General::arrayIncludeElement( (char*) in_pin , In_Pins_Number , read_buff[ test_index + 2 ] ) 
          //PLEASE BEWARE THAT I MADE A CHANGE ABOUT THE RETURN VALUE OF arrayIncludeElement
          
            //|| General::arrayIncludeElement( PCF1.out_pin_symbol , PCF1.out_pins_number , read_buff[ last_colon_of_incoming_message + 2 ] ) //comment for PCF exclusion
            ) {
          */
          if( pin_index != -1 ) {
            //Serial.printf("Analyzing, Pin %c is fine\n", read_buff[ test_index + 2 ] );
            if ( read_buff[ test_index + 3 ] == 'T' || read_buff[ test_index + 3 ] == 'F' ) {
              boolean actionType;
              Serial.printf("Analyzing, Action type is fine and it is: \n", read_buff[ test_index + 3 ] );
              if ( read_buff[ test_index + 3 ] == 'T' ) {
                actionType = true;
              } else {                
                actionType = false;
              }
              updateNodePinAndEEPROM( out_pin[ pin_index ] , actionType );
              //Now we need to check if this particular in-report pin state is the same as the current pin state
              if( actionType != NodeMCU::getInPinStateAsConsidered( General::getIntFromHexChar( read_buff[ test_index + 2 ] ) ) ) {
                Serial.println("Read pin doesn't hold the same state as in the received message");
                is_intelligible_report = false;
                break; //have to break once it's false
              }
              test_index = test_index + 3;
            } else {
              //Serial.println("Useful received local message doesn't state whether to turn the pin on or off.");
              is_intelligible_report = false;
              break; //have to break once it's false
            }
          } else {
            is_intelligible_report = false;
            break; //have to break once it's false
            //Serial.println("Useful received local message doesn't state the pin number.");
          }            
        } else {
          is_intelligible_report = false;
          break; //have to break once it's false
        }
      }
    } else {
      //Serial.println("Useful received local message is less than the usual report's size");
    }     
    if( is_awaiting_ack ) { 
      if( is_intelligible_report ) {
        Serial.println("As we received a report from the Inform Remote then we're no longer awaiting an ack");
        is_awaiting_ack[ entity_index ] = false;
      } else {
        if( read_char_number - 1 >= colon_index + 3 ) { //Please note that *is_intelligible_message_received_ptr is already set to false elsewhere
          if( read_buff[ colon_index + 1 ] == 'A' && read_buff[ colon_index + 2 ] == 'C' && read_buff[ colon_index + 3 ] == 'K' ) {
                is_awaiting_ack[ entity_index ] = false;
                Serial.println("As we received an ack from the Inform Remote then we're no longer awaiting an ack");
          }
        }
      }
    }
    return (is_intelligible_report);
  }

public:    
  int* counter_to_await_report_receipt;
  
  //message handling variables  
  Operation oper;  

  void stopOperations() {
    for( int i = 0 ; i < panels_number ; i++ ) {
      if( is_awaiting_ack )
        is_awaiting_ack[ i ] = false;
      if( counter_to_await_report_receipt )
        counter_to_await_report_receipt[ i ] = 0;
    }
    if( read_buff )
      read_buff[0] = '\0';
    read_char_number = 0;
    panel_id_buff[0] = '\0';    
  }

  void setup( int panels_number, const char** panel_id, int* counter_to_await_report_receipt, long max_awaiting_time, 
              const AsynchroClient** asynchro_client, void sendReportMethod( AsyncClient* , const char* ), boolean* is_awaiting_ack ) {
    this->is_awaiting_ack = is_awaiting_ack;
    setup( panels_number, panel_id, counter_to_await_report_receipt, max_awaiting_time, asynchro_client, sendReportMethod );
  }
  
  void setup( int panels_number, const char** panel_id, int* counter_to_await_report_receipt, long max_awaiting_time, 
                const AsynchroClient** asynchro_client, void sendReportMethod( AsyncClient* , const char* ) ) {
    this->panels_number = panels_number;
    this->panel_id = (char**) panel_id;
    this->counter_to_await_report_receipt = counter_to_await_report_receipt;
    max_counter_to_await_report_receipt = floor( max_awaiting_time / delay_per_loop ); //All in all, the 5000 still means 5000 instead of 4000  
    this->asynchro_client = (AsynchroClient**) asynchro_client;
    this->sendReportMethod = sendReportMethod;
    
    oper.setOperInfo( owner_id_buff , panels_number , (const char**) this->panel_id , mod_id_buff );
  }
  
  //preprocesssing and postprocessing remote_connection is up to RemoteServer
  void informAllIfInPinChanged() {
    boolean inform_all = false;
    //Sending report because an IN pin has changed              
    if( an_in_pin_changed ) {      
      Serial.println("since an in pin has changed, so informing all entities");
      inform_all = true;
    } else if( NodeMCU::isInOutIncoherent() ) {
      for( int i = 0 ; i < panels_number ; i++ ) {
        if( counter_to_await_report_receipt[ i ] == -1 ) {
          Serial.println("Absolutely weird case of incoherence");
          inform_all = true;
          break;
        }        
      }
    }
    if( inform_all ) {
      for( int i = 0 ; i < panels_number ; i++ ) {
        sendReportAndSetCounter( i );
      }
    }
  }

  //This section is to handle the counters of the entities we sent a report to but still awaiting a valid report from.    
  void checkIncomingMessageAndFixCounter( char* message_buff, int message_length ) {
    Serial.println("process incoming message from the informed entity, which may be a valid report");
    Serial.printf("incoming message was %s and its length was %d\n", message_buff, message_length);
    int entity_index = getInformedEntityId( message_buff, message_length ); /*the return value is 
     * the panel index according to our panel_id char array.
     * Also this method checks is the signature of the received message is valid.
     */
    Serial.printf("entity_index (or panel_index, but don't misconcept with panel id)" 
                  " which is known from the received message is %d\n", entity_index );
     
    if( entity_index != -1 ) { /*another valid check is if panel_id_buff[0] != '\0'
      * Anyway, this entity_index check means that the sender of this message is one of our informed (or able to be informed) panels.
       */
//      if( counter_to_await_report_receipt[ entity_index ] >= 0 ) { 
        /*
        *  This means that this particular panel has been informed by us and we're still awaiting a report from it so
        *  the content of the message is worth of being checked.
         */
        
        if( analyze( entity_index ) ) { //if true then we have received a report that reflects our current pins status
          Serial.println("A valid report is well received!");
          counter_to_await_report_receipt[ entity_index ] = -1; /*declaring that we're no longer awaiting a receipt of this entity
          * As we've received a valid report from it.
           */          
        } else {
        //  sendReportAndSetCounter( entity_index ); 
        /*sendReportAndSetCounter have to be commented in this block since we do receive sometimes ack which will bring us here ! And 
                                                      * it's unfair to send a new report in this case.
                                                      */
        }
        
/*
      } else {
        Serial.printf("counter_to_await_report_receipt was %d\n", counter_to_await_report_receipt[ entity_index ] );
      }
*/
    } else {
      Serial.println("Inform - Wrong signature!");
    }
    //I'm afraid that the buffer must be big enough so  that I don't have to process (or worst, read) one message per loop. This might waste 
    // valid received report messages. And incoming report messages must be maybe not ending with a \0.
    // I may find myself having to omit the \0 from the whole system in case a message was received from a requester in the same buffer as the 
    // incoming report.
    //check Max_Reading_Buffer
    // A trick can be made in the format (by concatenating messages or at least the sending mobs, whatever) probably in the relay intermediate server.
    //What are the consequences of ignoring such issue? Nothing really except the blinking LED in our panel.
    // This problem mainly appears in the intermediate server.
  }  

  void adjustCounterIfDisconnected() {
    for( int i = 0 ; i < panels_number ; i++ ) {
      if( asynchro_client[ i ]->last_connected_index == 0 ) {
        counter_to_await_report_receipt[ i ] = 1;
      }
    }
  }

  void resendReport_and_adjustCounter() {
    /*Now send report to entities which have counter_to_await_report_receipt == 0, 
     * if found valid to send at least one report then flag the Notifier pin to blink.
     * Also descrease counter if awaiting a report, the panels of which counter_to_await_report_receipt is not -1
     */
    for( int i = 0 ; i < panels_number ; i++ ) {
      if( counter_to_await_report_receipt[ i ] == 0 ) {//meaning that it has been decreasing some time ago (thus we're awaiting from it and never received a valid report)        
        Serial.printf("Inform: Toggeling notifier pin of IP %s because didn't receive a valid report from informed entity (or this is the first time)\n", asynchro_client[ i ]->IP);        
        if( !is_awaiting_ack ) { //this is left to local entities
          Serial.println("Rushing the connection of the Inform Local"); //it's really local not remote
          asynchro_client[ i ]->rushSocketSwitch(); /*One of 2 possibilities:
            *Either there is no connection so trying with the other port is fine,
            * or our reflected message has been well received by the reflector but they are stuck in the buffer and not yet received.
           */
        } else {
          if( is_awaiting_ack[ i ] ) {
            asynchro_client[ i ]->rushSocketSwitch();
            Serial.println("Rushing the internet connection of the Inform Remote");
          }
        }
        sendReportAndSetCounter( i );
      } else if( counter_to_await_report_receipt[i] > 0 ) {
        counter_to_await_report_receipt[ i ]--;
      }
      if( counter_to_await_report_receipt[ i ] != -1 ) {  //then we leave it as we're not awaiting anything from it.
        toggle_connect_failure_notifier_pin = true;
      }
    }    
  }
  
};

class InformRemoteAPI {
private:  
/*
  static const int panels_number = 5;
  int counter_to_await_report_receipt[ panels_number ] = { 0, 0, 0, 0, 0 }; //-1 means not awaiting anything. 0 means to send right away.
  const char* panel_id[ panels_number ] = { "3:\0", "2:\0", "5:\0", "6:\0", "7:\0" }; //BTW there is no problem in these panels being also RemoteServer's mobiles.
  const AsynchroClient* asynchro_client[ panels_number ] = { &remote_connection, &remote_connection, &remote_connection, &remote_connection, &remote_connection };
*/
  static const int panels_number = 1;
  int counter_to_await_report_receipt[ panels_number ] = { 0 }; 
  const char* panel_id[ panels_number ] = { "13:\0" }; 
  const AsynchroClient* asynchro_client[ panels_number ] = { &remote_connection };
  boolean is_awaiting_ack[ panels_number ];
  
  long time_to_await_for_report_receipt = 8000; //PLEASE let this smaller than max_counter_to_destroy_old_socket
  
public:  
  void setup( InformEntity* inform_remote ) {
    inform_remote->setup( panels_number, panel_id, counter_to_await_report_receipt, time_to_await_for_report_receipt, asynchro_client,
                          RemoteServerMessageOp::sendReport, is_awaiting_ack );
  }
};

class Remote {
private:  
  char remote_server_IP[15]; //it needs to be alive all the time because it's used in different scopes.
  RemoteServerMessageOp remote_server_message_op;
  InformRemoteAPI inform_remote_setter;
  InformEntity inform_remote;
  boolean isInformRemote = false;

  //Now to control the sendHi and its ack
  int counter_to_ack_hi;
  int max_counter_to_ack_hi;
  boolean is_ack_hi_received_1, is_ack_hi_received_2;
  char last_just_connected;
  char temporary_just_connected;
  
  void preProcess() { //this is called from loop()        
    remote_connection.preProcess( &temporary_just_connected );
    if( temporary_just_connected == '1' || temporary_just_connected == '2' ) {
      last_just_connected = temporary_just_connected;
      counter_to_ack_hi = max_counter_to_ack_hi;
      Serial.println("Awaiting ack after sending Hi to intermediate");
    }
    
    if( temporary_just_connected == '1' ) { /*send Hi to intermediate. 
                                                * BTW we could have put this in AsynchroClient
                                                * (and frankly this may have been better) but for pure technical reasons we didn't.
                                                * Even if technically possible, we wouldn't do that is that AsynchroClient class is also used 
                                                * in InformLocal class and sendHiToServer and this is not needed there.
                                                */
      RemoteServerMessageOp::sendHiIntermediate( &remote_connection.async_client_1 );      
    } else if( temporary_just_connected == '2' ) { //also send Hi to intermediate.
      RemoteServerMessageOp::sendHiIntermediate( &remote_connection.async_client_2 );
    } 
    
  }

  void postProcess() {
    remote_connection.postProcess();
  }

  void preSetAckHi() {
    is_ack_hi_received_1 = false;
    is_ack_hi_received_2 = false;    
  }

  void processAckHi() {
    if( counter_to_ack_hi > 0 ) {      
      if( ( last_just_connected == '1' && is_ack_hi_received_1 ) || ( last_just_connected == '2' && is_ack_hi_received_2 ) ) {
        counter_to_ack_hi = -1;
        Serial.println("Ack of HI is received");
      } else {
        counter_to_ack_hi--;      
        Serial.println("Awaiting for ack of HI");
      }
    }
    if( counter_to_ack_hi == 0 ) {
      remote_connection.rushSocketSwitch();
      counter_to_ack_hi = max_counter_to_ack_hi; //we keep trying to establish a connection.
      Serial.println("Rushing socket connection since ack of HI is received");
    }
    if( counter_to_ack_hi != -1 ) {
      toggle_connect_failure_notifier_pin = true;    //it's very logical
    }
  }

  int getDigitsNumber( int num ) {
    if( num < 10 ) return 1;
    if( num < 100 ) return 2;
    return 3;
  }

  void writeByteToRemoteServerIP( int IP_byte, int &last_index ) {//this is the reference to last_index (not pointer, nor address)
    itoa( IP_byte, (char*) (remote_server_IP + last_index), 10 ); //10 means decimal
    last_index += getDigitsNumber( IP_byte );
    remote_server_IP[ last_index ] = '.';
    last_index++;
  }

  void settingRemoteServerIP( IPAddress &IPtofind ) {
    //Serial.printf( "So gotten IP is %d.%d.%d.%d\n", IPtofind[0], IPtofind[1], IPtofind[2], IPtofind[3] );
    //starting with the first byte (upper most)
    int last_index = 0;
    writeByteToRemoteServerIP( IPtofind[0], last_index );
    //Serial.printf("The remote IP after first byte is %s\n", remote_server_IP );
    //now the second byte
    writeByteToRemoteServerIP( IPtofind[1], last_index );
    //Serial.printf("The remote IP after second byte is %s\n", remote_server_IP );
    //now the third byte
    writeByteToRemoteServerIP( IPtofind[2], last_index );
    //Serial.printf("The remote IP after third byte is %s\n", remote_server_IP );
    //now the fourth byte (the lower most)
    itoa( IPtofind[3], (char*) (remote_server_IP + last_index), 10 );
    last_index += getDigitsNumber( IPtofind[3] );
    remote_server_IP[ last_index ] = '\0';
    //now we've gotten the C-string buffer
    Serial.printf("The remote IP is %s\n", remote_server_IP );
  }

  void findRemoteServerIP() {    
    /*Now we want to find the IP of the intermediate server.
    * Once I tried the following and it worked :
    * I haven't used WiFi.hostByName on the pre-allocated DNS. Instead, I have set the primary DNS to 8.8.8.8 and if it failed I have 
    * reset the primary DNS to 8.8.4.4. I have never set the secondary DNS to anything.
    * Weird enough, it didn't work when I set 8.8.4.4 and then reset it, in case of failure, to 8.8.8.8 (again secondary was not set at all).
    */
    //char remote_server_IP[15]; //declared with a more alive scope
    char* remote_server_domain_name = "dirani.jvmhost.net"; //P.S. : DO NOT PUT http:// this made a serious problem ! It was never resolved !
    IPAddress IPtofind;
    WiFi.hostByName( remote_server_domain_name, IPtofind );
    if( IPtofind[0] == 0 && IPtofind[1] == 0 && IPtofind[2] == 0 && IPtofind[3] == 0 ) {/*this block has the power to fix the issue for 
      *some reason.
      *Start with 8.8.8.8 primary and 8.8.4.4 secondary and don't start with the opposite - it makes a difference for some odd reason.
      */          
      IPAddress DNS1_IP( 8, 8, 8, 8 );
      IPAddress DNS2_IP( 8, 8, 4, 4 );    
      espconn_dns_setserver(0, DNS1_IP);    
      espconn_dns_setserver(1, DNS2_IP); //I don't know if the secondary matters but I put it anyway 
      //WiFi.dnsIP(0).printTo(Serial);
      //WiFi.dnsIP(1).printTo(Serial);
      WiFi.hostByName( remote_server_domain_name, IPtofind );        
      Serial.printf( "Gotten IP from 8.8.8.8 as primary is %d.%d.%d.%d\n", IPtofind[0], IPtofind[1], IPtofind[2], IPtofind[3] );
      if( IPtofind[0] == 0 && IPtofind[1] == 0 && IPtofind[2] == 0 && IPtofind[3] == 0 ) {   
        //we probably won't enter here.   
        IPAddress DNS3_IP( 216,146,35,35 );
        IPAddress DNS4_IP( 216,146,36,36 ); //for more DNS's check https://en.wikipedia.org/wiki/Public_recursive_name_server
        espconn_dns_setserver(0, DNS3_IP);    
        espconn_dns_setserver(1, DNS4_IP);
        WiFi.hostByName( remote_server_domain_name, IPtofind );        
        Serial.printf( "Gotten IP from 216.146.35.35 as primary is %d.%d.%d.%d\n", IPtofind[0], IPtofind[1], IPtofind[2], IPtofind[3] );
      }
      /*If you're really concerned about not changing the pre-allocated DNS, for some reason, you may get the primary and secondary DNS
      * and then after you get remote_server_IP you may set the old allocated DNS IPs back to the NodeMCU. This way, this act will be 
      * seemless.
      */
    }    
    
    if( IPtofind[0] == 0 && IPtofind[1] == 0 && IPtofind[2] == 0 && IPtofind[3] == 0 ) {
      NodeMCU::HW_RST(); //this should solve it. Actually this could had solved it since the beginning.
    }
    
    //now converting the IP from IPAddress onto a C-buffer string inside remote_server_IP
    settingRemoteServerIP( IPtofind );
    //After we've got the IP now let's set remote_connection
  }
  
public:
  void setup( boolean isInformRemote ) {
    //remote_connection.setAsynchroClient( "91.240.81.106" , 3558 , 3559 ); //this is only setting, not connecting yet
    //remote_connection.setAsynchroClient( "91.240.81.106" , 11359 , 11360 ); //this is only setting, not connecting yet
    //remote_connection.setAsynchroClient( "dirani.jvmhost.net" , 11360 , 11359 );
    //remote_connection.setAsynchroClient( "173.243.120.250" , 11360 , 11359 );
    //findRemoteServerIP(); //this is to evaluate remote_server_IP    
    //remote_connection.setAsynchroClient( (char*) remote_server_IP , 11360 , 11359 );    
//    remote_connection.setAsynchroClient( "74.122.199.173" , 11359 , 11360 );    
    remote_connection.setAsynchroClient( "192.168.1.21" , 11359 , 11360 );    
    remote_server_message_op.setup( );
    this->isInformRemote = isInformRemote;
    
    //Now managing inform_remote
    if( isInformRemote ) {
      inform_remote_setter.setup( &inform_remote );
    }
    
    //Now for the sendHi and receiving the ack
    max_counter_to_ack_hi = floor( 8000 / delay_per_loop );
    counter_to_ack_hi = -1; //don't ask to rush right away
  }  

  void stopOperations() {
    remote_connection.stopOperations();
    if( isInformRemote ) {
      inform_remote.stopOperations();
    }
    remote_server_message_op.stopOperations();
    counter_to_ack_hi = -1;
    preSetAckHi();
    last_just_connected = '\0'; /*not necessary*/
    temporary_just_connected = '\0'; /*not necessary*/
  }

  void process() {    
    preProcess(); /*This will handle the preprocessing of the asynchro_client to the intermediate server. 
                    * It is obligatory to sendHiToIntermediate and this is implemented (and only needed) in RemoteServerMessageOp. 
                    * It is not needed in InformRemote because the purpose of sending "Hi" is to let the intermediate server 
                    * relay an incoming message to this NodeMCU.
                    */
    Serial.println("Processing remote");
    preSetAckHi();
        
    for( int incoming_message1_index = 0 ; incoming_message1_index < AsynchroClient::max_incoming_messages_number ; incoming_message1_index++ ) {
      if( remote_connection.message_in_processing_from_client_1[ incoming_message1_index ] ) { 
        if( remote_server_message_op.getMessage( (char*) remote_connection.buff_1[ incoming_message1_index ], 
                                                  remote_connection.len_buff_1[ incoming_message1_index ] ) ) { //should normally be always true
          Serial.printf("Processing remote message of port 1 of buffer index %d where message is %s\n", incoming_message1_index,
                            remote_connection.buff_1[ incoming_message1_index ]);
          
          remote_server_message_op.processMessage( &is_ack_hi_received_1 ); //This usually checks incoming report requests or if Hi ack is received.
                                                                            // For this ordering panel, it's only useful for Hi ack
          //Now we check if the message has to do with InformEntity
          if( isInformRemote ) {
            inform_remote.checkIncomingMessageAndFixCounter( (char*) remote_connection.buff_1[ incoming_message1_index ], 
                                                              remote_connection.len_buff_1[ incoming_message1_index ] );
          }
        }
        remote_connection.message_in_processing_from_client_1[ incoming_message1_index ] = false;
      }
    }
    
    for( int incoming_message2_index = 0 ; incoming_message2_index < AsynchroClient::max_incoming_messages_number ; incoming_message2_index++ ) {    
      if( remote_connection.message_in_processing_from_client_2[ incoming_message2_index ] ) { 
        if( remote_server_message_op.getMessage( (char*) remote_connection.buff_2[ incoming_message2_index ], 
                                                  remote_connection.len_buff_2[ incoming_message2_index ] ) ) { //should normally be always true
          Serial.printf("Processing remote message of port 2 of buffer index %d where message is %s\n", incoming_message2_index,
                            remote_connection.buff_2[ incoming_message2_index ]);
          remote_server_message_op.processMessage( &is_ack_hi_received_2 );          
          //Now we check if the message has to do with InformEntity
          if( isInformRemote ) {
            inform_remote.checkIncomingMessageAndFixCounter( (char*) remote_connection.buff_2[ incoming_message2_index ], 
                                                              remote_connection.len_buff_2[ incoming_message2_index ] );
          }
        }
        remote_connection.message_in_processing_from_client_2[ incoming_message2_index ] = false;
      }
    }

    processAckHi();    

    if( isInformRemote ) {
      inform_remote.informAllIfInPinChanged();
      inform_remote.adjustCounterIfDisconnected();
      inform_remote.resendReport_and_adjustCounter();   //we might rush in here.
    }
    
    postProcess();
  }

};
