#include "Generic.h"
extern const int delay_per_loop;
const byte After_Press_Start_APmode_Timer = 5; /*in seconds*/
/*The following 3 variables should'd been private inside the class*/
const byte Start_AP_Index_In_EEPROM = 101; 
const byte SSID_Buff_Size = 32; 
const byte Password_Buff_Size = 64;
const byte Max_IP_Buff_Size = 4;
const byte MAC_Buff_Size = 12;
const byte LocalOrInternet_Buff_Size = 1;
//#include <WiFiUdp.h>

WiFiServer AP_server(3551); /*interesting why this cannot be a private member*/
/* In the EEPROM or in the received buffer from the user, the info are like one of these: 
 * 7 -> Electrotel_Dirani -> trailor -> onlyforworkpls -> trailor -> header of static IP -> static IP -> trailor -> 
 *    header of gateway IP -> gateway IP -> trailor -> header of subnet -> subnet -> trailor -> mac -> trailor -> local_or_internet char -> trailor
 * 6 -> Electrotel_Dirani -> trailor -> header of static IP -> static IP -> trailor -> 
 *    header of gateway IP -> gateway IP -> trailor -> header of subnet -> subnet -> trailor -> mac -> trailor -> local_or_internet char -> trailor
 * 3 -> Electrotel_Dirani -> trailor -> onlyforworkpls -> trailor -> local_or_internet char -> trailor
 * 2 -> Electrotel_Dirani -> trailor -> local_or_internet char -> trailor
 * The local_or_internet char is either 'l' for local, 'i' for Internet, 'b' for both
 * At maximum the size of the whole buffer is 1 + 32 + 1 + 64 + 2 + 4 + 2 + 4 + 2 + 4 + 1 + 12 + 1 + 1 + 1 = 132 bytes.
 */
const byte Max_AP_Buffer_Size = 1 + SSID_Buff_Size + 1 + Password_Buff_Size + 2 + Max_IP_Buff_Size + 2 + Max_IP_Buff_Size + 2 + Max_IP_Buff_Size + 1 +
                              MAC_Buff_Size + 1 + LocalOrInternet_Buff_Size + 1; /*If more than a 255 (because in many places I used 'byte' and not 'int' to refer to the buffer) 
                                                  * which is the byte size then something must be changed in the code below!*/
                              /*should'd been public inside the class*/
      
const char trailor = 127; //should'd been public inside the class. 
/*I chose this char because it is really not a char, it's the delete command, so IT CANNOT BE ENTERED BY THE USER (in SSID and password). 
 * (And I think it has no representation as an escape character like \n or \t.)*/

//This extern "C" is to set the MAC address
extern "C" {
#include <user_interface.h> 
}

class AP_Op {
private:  
  byte buffer_info[ Max_AP_Buffer_Size ];
  /*The following are 6 dedicated varables used to connect to wifi's router*/
  byte determinant; //either 2, 3, 6, or 7. If '\n' then no network configuration is considered in EEPROM.
  char SSID_buff[ SSID_Buff_Size ];  
  char password_buff[ Password_Buff_Size ];
  byte local_IP_bytes[ Max_IP_Buff_Size ];
  byte gateway_bytes[ Max_IP_Buff_Size ];
  byte subnet_bytes[ Max_IP_Buff_Size ];
  byte MAC_bytes[ 6 ]; /*these are fed to wifi_set_macaddr. The value will be set from the MAC chars in the buffer.*/
  
//  WiFiUDP Udp;
  int server_port = 3551;
  
  WiFiClient AP_client;
  
  byte max_counter_to_start_APmode;
  byte counter_to_start_APmode;
  
  void resetEEPROM() {
    //This will close all opened sockets because all operations 
    Serial.println("Resetting the EEPROM");
    EEPROM.write(Start_AP_Index_In_EEPROM, trailor);
    EEPROM.commit();
  }

  void runAsAP() {
    /*will restart if failed to run as AP, but normally should never fail*/
    IPAddress local_IP(172,17,15,30); 
    /*this follows the list of private IP addresses from 172.16.0.0 to 172.31.255.255
            * where subnet fixes the first 12 bits (10101100 for the first byte, and 0001xxxx for the second byte), so subnet has to be
            * 255.240.0.0 according to https://en.wikipedia.org/wiki/Private_network */
    //IPAddress gateway(172,18,19,200); //let the gateway be the same as the local_IP, but maybe it works if 192.168.4.251 was the same gateway for all modules.
    IPAddress gateway(172,17,15,30);
    IPAddress subnet(255,240,0,0);
    //IPAddress local_IP(192,168,1,1);
    //IPAddress gateway(192,168,1,1);
    //IPAddress subnet(255,255,255,0);
    WiFi.mode(WIFI_AP); //never used !
    if( WiFi.softAPConfig(local_IP, gateway, subnet) ) {  
      Serial.println("runAsAP()   Setting soft-AP ... ");      
      if( WiFi.softAP( "SetPanelNetConfig", "", 1, false, 1 ) ) {/*no password, channel is 1 (as the default value), SSID is not hidden, 
        * and the last 1 is the max_connection allowed to connect, which is what I really want*/        
      //if( WiFi.softAP( "SetPanelNetConfig" ) ) {/*no password*/
        Serial.println("runAsAP()      Soft-AP SSID broadcast success...");
        delay (50);//I added it just in case...      

//        Udp.begin( server_port );
        
        AP_server.begin();
        //I want a server socket
        AP_server.setNoDelay(true);//I think everything you send is directly sent without the need to wait for the buffer to be full
        Serial.println("runAsAP()      Server should be up and listening by now");
        return;
      }
    }
    /*normally should not restart.*/
    Serial.println("runAsAP()     Couldn't run as AP probably!!! So restarting.");
    NodeMCU::restartNodeMCU(); 
  }

  boolean getIncomingMessage() {
    Serial.printf("In AP mode, getIncomingMessage()\n");
    int connectedStationsNumber = WiFi.softAPgetStationNum();
    Serial.printf("In AP mode, connectedStationsNumber is %d\n", connectedStationsNumber);
    if (connectedStationsNumber > 0){
      //if( AP_client ) {//this should never be tested here because when it is not connected it returns false, so 
      //Serial.printf("In AP mode, AP_client is assumed as not null\n");
/*
      int packetSize = Udp.parsePacket();
      if (packetSize) {
        Serial.printf("UDP packet is received\n");
        //must check, in a separate function, that the received packet is from server...
        int len = Udp.read( buffer_info, Max_AP_Buffer_Size );
        if (len > 0) { //not needed ?
          buffer_info[len] = '\0'; //is this like a null character?
        }
        //now must check the content of the incoming packet
        Serial.printf("UDP packet content: %s\n", buffer_info);
        return true;
      }
*/  
      if( AP_client.connected() ) {
        Serial.printf("In AP mode, AP_client is connected\n");
        if( AP_client.available() ) {
          Serial.printf("In AP mode, an AP_client is available\n");
          int readCharNumber = AP_client.readBytesUntil('\0', (char*) buffer_info, Max_AP_Buffer_Size); //BTW the '\0' is not counted in the value of readCharNumber - tested
          if( readCharNumber != 0 ) {
            Serial.printf("In AP mode, readCharNumber is not 0, so buffer_info should be gotten by now\n");
            if( readCharNumber < Max_AP_Buffer_Size ) {
              buffer_info[ readCharNumber ] = '\0';
              Serial.printf("In AP mode, getIncomingMessage()   Read buff is %s\n", buffer_info);
            }
            Serial.printf("In AP mode, getIncomingMessage()   stopping AP_client\n");
            AP_client.stop();
            return true;
          } 
          Serial.printf("In AP mode, getIncomingMessage()   weird case! Anyway, stopping AP_client\n");
          AP_client.stop();          
        }
      } else {
        Serial.printf("In AP mode, getIncomingMessage()   no client is connected to the AP's server\n");
        AP_client = AP_server.available();
      }                 
      //}
    }
    return false;    
  }

  void stopAll_AP_Operations() {
    if( AP_client ) {
      if( AP_client.connected() ) {
        AP_client.stop();
      }
    }    
    NodeMCU::yieldAndDelay(50);
    AP_server.stop();
    NodeMCU::yieldAndDelay(100);
    WiFi.softAPdisconnect (true);
  }
  
  boolean testValidityOfDeterminant() {
    return( determinant == 2 || determinant == 3 || determinant == 6 || determinant == 7 ); //according to structure of code, this should never happen
  }

  boolean checkNotExceedingMaxBuffSizeThenCopy( int first_index, int last_index, byte max_buff_size, char* destination_buff ) {
    int length_to_copy = last_index - first_index; /*last_index - 1 is the index of the last useful char 
    * and first_index - 1 is the length of the char which precedes the first useful char*/
    //Serial.printf("checkNotExceedingMaxBuffSizeThenCopy   length_to_copy is : %d\n", length_to_copy);
    if( length_to_copy > max_buff_size ) {
      return false;
    }
    strncpy( destination_buff , (const char*) ( buffer_info + first_index ), length_to_copy );
    if( length_to_copy < max_buff_size ) { /*we won't enter in case length_to_copy == SSID_Buff_Size*/
      destination_buff[ length_to_copy ] = '\0';
    }
    Serial.printf("checkNotExceedingMaxBuffSizeThenCopy   the variable is : %s\n", destination_buff);
    return true;
  }

  void updateFirstAndLastIndices( int* first_index, int* last_index ) { //there has to be max_search_interval argument to be passed
    *first_index = (*last_index) + 1; /*the index of the first useful char*/
    *last_index = *first_index;
    while( buffer_info[ *last_index ] != trailor && (*last_index) < Max_AP_Buffer_Size ) { /*we're sure we'll find a trailor*/
      //Serial.printf("updateFirstAndLastIndices   buffer_info char is : %c\n", buffer_info[ *last_index ] );
      (*last_index)++;
    }
  }

  boolean checkIP_HeaderThenCopy( int first_index, int last_index, byte* destination_buff ) {
    /*last_index - 1 is the index of the last useful char (so last_index is the index of the following trailor)
    * and first_index is the index of the first useful char, which is the header*/
    /*Max_IP_Buff_Size is 4 of course*/
    /*let's analyze the header*/
    byte header = buffer_info[ first_index ];
    for( int i = Max_IP_Buff_Size - 1 ; i >= 0 ; i-- ) {
      //Serial.printf("checkIP_HeaderThenCopy   header is %d\n", header ); 
      if( header & 1 != 0 ) {
        destination_buff[ i ] = 0;
      } else {
        last_index--;
        if( last_index == first_index ) {
          //this means that the IP bytes are less than the indicated number by the header       
          Serial.println("checkIP_HeaderThenCopy   position 1");   
          return false;
        }        
        destination_buff[ i ] = buffer_info[ last_index ];
        //Serial.printf("checkIP_HeaderThenCopy   'i' is %d and destination_buff[ i ] is %d\n", i , destination_buff[ i ] ); 
      }
      header = header >> 1; //this right shifts the header and places a 0 as the new MSbit.      
    }
    if( last_index != first_index + 1 ) {
      //this means that the IP bytes are more than the indicated number by the header          
      Serial.println("checkIP_HeaderThenCopy   position 2");   
      return false;
    }
    /*There is really no need for the following statements but I made it in order to make the code more exact (not loose)*/
    boolean all_bytes_are_non0 = true;
    for( int i = 0 ; i < Max_IP_Buff_Size ; i++ ) {
      if( destination_buff[ i ] == 0 ) {
        //Serial.println("checkIP_HeaderThenCopy   position 3");   
        all_bytes_are_non0 = false;
        break;
      }
    }
    if( header == 1 ) { //this means that it was 16 in the beginning, but after shifting 4 bits, it's now 1
      if( !all_bytes_are_non0 ) {
        Serial.println("checkIP_HeaderThenCopy   position 4");   
        return false;
      }
    } else if( header == 0 ) {//all of the 4 bytes must be different than 0 
      if( all_bytes_are_non0 ) {
        Serial.println("checkIP_HeaderThenCopy   position 5");   
        return false;
      }
    } else {
      Serial.println("checkIP_HeaderThenCopy   position 6");   
      return false; //no other value is allowed.
    }
    Serial.printf("checkIP_HeaderThenCopy   the IP bytes are : %d.%d.%d.%d\n", 
              destination_buff[0], destination_buff[1], destination_buff[2], destination_buff[3] );
    return true;
  }

  boolean checkMAC_BuffSizeThenCopy( int first_index, int last_index ) {
    int length_to_copy = last_index - first_index; /*last_index - 1 is the index of the last useful char 
    * and first_index - 1 is the length of the char which precedes the first useful char*/
    if( length_to_copy != MAC_Buff_Size ) {
      Serial.println("checkMAC_BuffSizeThenCopy()    size of MAC is not right");
      return false;
    }
    int byte_from_2_hex_chars;
    for( int i = 0 ; i < MAC_Buff_Size ; i++ ) {
      byte_from_2_hex_chars = General::getByteFromTwoHexChars( buffer_info[ first_index + i ] , buffer_info[ first_index + i + 1] ); /*buffer_info[ first_index + i ] is the higher nibble and buffer_info[ first_index + i + 1] is the lower nibble*/      
      if( byte_from_2_hex_chars == -1 ) {
        Serial.printf("checkMAC_BuffSizeThenCopy()    the %dth or the %dth char of the MAC chunk is not acceptable, they were %c and %c\n", i, i + 1,
                        buffer_info[ first_index + i ], buffer_info[ first_index + i + 1 ] );
        return false;
      }      
      MAC_bytes[ i / 2 ] = byte_from_2_hex_chars;
      i++;
    }
    return true;    
  }

  boolean acceptChar( byte buff_index, byte trailor_counter ) {
    return( ( buff_index < Max_AP_Buffer_Size ) && ( trailor_counter < determinant ) && ( buffer_info[ buff_index] != '\0' ) ); 
    /*there won't be a char gotten from the user
    * which is '\0' since this char would had truncated the incoming buffer (message)*/
  }

  boolean getLocalInternet( int last_index ) {
    last_index++;
    switch( buffer_info[ last_index ] ) {
      case 'l':
        isLocalServer = true;
        isInternetServer = false;
        break;
      case 'i':
        isLocalServer = false;
        isInternetServer = true;
        break;
      case 'b':
        isLocalServer = true;
        isInternetServer = true;
        break;
      default:
        return false;
        break;
    }         
    
    last_index++;
    if( buffer_info[ last_index ] != trailor ) {
      return false;
    }

    return true;
  }
    
  boolean checkBuffAndAssignConfigVariables() {
    /*This analyzes buffer_info assuming that it has all the needed stuff ALREADY. 
     * It assigns the 6 dedicated variables. In case a variable must not be assigned,
     * e.g. the local_IP_bytes (say the NodeMCU musn't have a static IP) then I will still account on determinant variable's value (1, 2, 4, or 5).
     * It should normally return true, but I made it possible to return false in case the buffer does not conform to an understandable format.
     * This method is used wither for the buffer gotten from the EEPROM or the one gotten from the user. 
     * Again it should return true, but this protection is not bad at all.
     */
    // checking the validity of the determinant
    determinant = buffer_info[0];
    if( !testValidityOfDeterminant() ) { //according to structure of code, this should never happen
      Serial.printf("checkBuffAndAssignConfigVariables()  The first byte of the buffer (which is %d) is not valid, so we will dismiss this buffer.\n", determinant);
      determinant = trailor;//here it converts from a char to a byte (it should be 10)      
      return false;
    }

    // checking that the number of trailors is the same as the determinant
    byte i = 1;
    byte trailor_counter = 0;
    while( acceptChar(  i , trailor_counter ) ) {    
      //Serial.printf("checkBuffAndAssignConfigVariables     char is : %d\n", buffer_info[ i ] );
      if( buffer_info[ i ] == trailor ) {
        //Serial.printf("checkBuffAndAssignConfigVariables     the index of the trailor is : %d\n", i );
        trailor_counter++;
      }
      i++;
    }
    if( trailor_counter != determinant ) {
      Serial.printf("checkBuffAndAssignConfigVariables()  Incorrect format of the buffer since the trailor_counter of the buffer "
        "(which is %d) is smaller than the determinant (which is %d).\n", trailor_counter, determinant);
      determinant = trailor;/*it may not be necessary but it's ok*/
      return false;
    }
    
    //getting the SSID (it's the first chunk of data)
    int first_index = 0;
    int last_index = 0;
    updateFirstAndLastIndices( &first_index, &last_index );
    //checking that there is at least 1 char in the first chunk of data
    if( first_index == last_index ) { //the case where 2 trailors are next to each other
      Serial.println("checkBuffAndAssignConfigVariables()    There isn't any char for the SSID");
      determinant = trailor;/*it may not be necessary but it's ok*/
      return false;
    }
    if( !checkNotExceedingMaxBuffSizeThenCopy( first_index, last_index, SSID_Buff_Size, (char*) SSID_buff ) ) {
      Serial.println("checkBuffAndAssignConfigVariables()    the supposed SSID is greater than the permitted size");
      determinant = trailor;/*it may not be necessary but it's ok*/
      return false;
    }    
    if( determinant == 2 ) {
      if( !getLocalInternet( last_index ) ) {
        return false;
      }
      return true;
    }
    
    //getting the second chunk of data
    updateFirstAndLastIndices( &first_index, &last_index );
    //checking that there is at least 1 char in this second chunk of data
    if( first_index == last_index ) { //the case where 2 trailors are next to each other
      Serial.println("checkBuffAndAssignConfigVariables()    There isn't any char for the second data");
      determinant = trailor;/*it may not be necessary but it's ok*/
      return false;
    }    
    //we know the determinant is not 1
    if( determinant == 3 || determinant == 7 ) {
      if( !checkNotExceedingMaxBuffSizeThenCopy( first_index, last_index, Password_Buff_Size, (char*) password_buff ) ) {
        Serial.println("checkBuffAndAssignConfigVariables()    the supposed Password is greater than the permitted size");
        determinant = trailor;/*it may not be necessary but it's ok*/
        return false;
      }
      if( determinant == 3 ) {
        if( !getLocalInternet( last_index ) ) {
          return false;
        }
        return true;
      }      
    } else if( determinant == 6 ) {
      if( !checkIP_HeaderThenCopy( first_index, last_index, local_IP_bytes ) ) {
        Serial.println("checkBuffAndAssignConfigVariables()    the supposed local IP is different than the specified size");
        determinant = trailor; /*it may not be necessary but it's ok*/
        return false;
      }      
    }
    //now the determinant is either 6 or 7
    //Getting the third chunk of data
    updateFirstAndLastIndices( &first_index, &last_index );
    if( first_index == last_index ) { //the case where 2 trailors are next to each other
      Serial.println("checkBuffAndAssignConfigVariables()    There isn't any char for the third data");
      determinant = trailor;/*it may not be necessary but it's ok*/
      return false;
    } 
    if( determinant == 6 ) {
      if( !checkIP_HeaderThenCopy( first_index, last_index, gateway_bytes ) ) {
        Serial.println("checkBuffAndAssignConfigVariables()    the supposed gateway IP is different than the specified size");
        determinant = trailor; /*it may not be necessary but it's ok*/
        return false;
      }      
    } else if( determinant == 7 ) {
      if( !checkIP_HeaderThenCopy( first_index, last_index, local_IP_bytes ) ) {
        Serial.println("checkBuffAndAssignConfigVariables()    the supposed local IP info aren't acceptable in decoding !");
        determinant = trailor; /*it may not be necessary but it's ok*/
        return false;
      }
    }

    //Getting the fourth chunk of data
    updateFirstAndLastIndices( &first_index, &last_index );
    if( first_index == last_index ) { //the case where 2 trailors are next to each other
      Serial.println("checkBuffAndAssignConfigVariables()    There isn't any char for the fourth data");
      determinant = trailor;/*it may not be necessary but it's ok*/
      return false;
    } 
    if( determinant == 6 ) {
      if( !checkIP_HeaderThenCopy( first_index, last_index, subnet_bytes ) ) {
        Serial.println("checkBuffAndAssignConfigVariables()    the supposed subnet IP is different than the specified size");
        determinant = trailor; /*it may not be necessary but it's ok*/
        return false;
      }
    } else if( determinant == 7 ) {
      if( !checkIP_HeaderThenCopy( first_index, last_index, gateway_bytes ) ) {
        Serial.println("checkBuffAndAssignConfigVariables()    the supposed gateway IP is different than the specified size");
        determinant = trailor; /*it may not be necessary but it's ok*/
        return false;
      } 
    }

    //Getting the fifth chunk of data
    updateFirstAndLastIndices( &first_index, &last_index );
    if( first_index == last_index ) { //the case where 2 trailors are next to each other
      Serial.println("checkBuffAndAssignConfigVariables()    There isn't any char for the fifth data");
      determinant = trailor;/*it may not be necessary but it's ok*/
      return false;
    } 
    if( determinant == 6 ) {
      if( !checkMAC_BuffSizeThenCopy( first_index, last_index ) ) {
        Serial.println("checkBuffAndAssignConfigVariables()    the supposed MAC is rejected");
        determinant = trailor; /*it may not be necessary but it's ok*/
        return false;
      }      
      if( !getLocalInternet( last_index ) ) {
        return false;
      }
      return true;
    } else if( determinant == 7 ) {
      if( !checkIP_HeaderThenCopy( first_index, last_index, subnet_bytes ) ) {
        Serial.println("checkBuffAndAssignConfigVariables()    the supposed gateway IP is different than the specified size");
        determinant = trailor; /*it may not be necessary but it's ok*/
        return false;
      } 
    }

    //Getting the sixth chunk of data
    updateFirstAndLastIndices( &first_index, &last_index );    
    if( first_index == last_index ) { //the case where 2 trailors are next to each other
      Serial.println("checkBuffAndAssignConfigVariables()    There isn't any char for the sixth data");
      determinant = trailor;/*it may not be necessary but it's ok*/
      return false;
    } 
    if( determinant == 7 ) { /*We know for sure determinant is 6 but it's ok*/
      if( !checkMAC_BuffSizeThenCopy( first_index, last_index ) ) {
        Serial.println("checkBuffAndAssignConfigVariables()    the supposed MAC is rejected");
        determinant = trailor; /*it may not be necessary but it's ok*/
        return false;
      } 
    }
    if( !getLocalInternet( last_index ) ) {
      return false;
    }
    return true;
  }

  boolean connectToRouterFromEEPROM_OrRestart() {
    /*Here we fetch the network configuration from EEPROM if existing, then we assign them to the corresponding private variables
     * through a special method checkBuffAndAssignConfigVariables that reads well the determinant (the first byte).
     * Next we connect.
     */
    determinant = (byte) EEPROM.read( Start_AP_Index_In_EEPROM );
    Serial.printf("connectToRouterFromEEPROM_OrRestart()    just after reading first char from EEPROM which is %d\n", determinant);
    if( !testValidityOfDeterminant() ) {
      Serial.printf("connectToRouterFromEEPROM_OrRestart()  The first byte of the buffer (which is %d) is not valid, so we will dismiss this buffer.\n", determinant);
      determinant = trailor;//here it converts from a char to a byte (it should be 10). I don't think this is important at all.      
      return false;
    }
    Serial.printf("connectToRouterFromEEPROM_OrRestart()    position 1. Now reading the chars :\n");
    byte i = 0; /*since the network config part of the EEPROM will be copied onto the buffer and then processed in a general
    * method specific for processing the network configuration buffers (also those originating from the user, then I have to take even the 
    * determinant again. so it starts from 0 and not from 1.
    */
    byte trailor_counter = 0;
    while( true ) {
      buffer_info[ i ] = (byte) EEPROM.read( Start_AP_Index_In_EEPROM  + i );
      //Serial.printf("%d", buffer_info[ i ] );
      if( buffer_info[ i ] == trailor ) {
        trailor_counter++;
      }
      if( !acceptChar( i , trailor_counter ) ) {
        break;
      }
      i++;
    }
    /* //this is commented because it will be made again in checkBuffAndAssignConfigVariables() below call.
    if( trailor_counter != determinant ) {
      ...
    }
    */
    Serial.printf("connectToRouterFromEEPROM_OrRestart()    position 2\n");
    //we really should return false if trailor_counter != determinant but I left this check to be done in checkBuffAndAssignConfigVariables()
    if( !checkBuffAndAssignConfigVariables() ) {
      resetEEPROM(); /*very logical*/
      return false;
    }
    Serial.printf("connectToRouterFromEEPROM_OrRestart()    position 3\n");

    //Now we're ready to connect
    connectToRouterOrRestart();
    Serial.printf("connectToRouterFromEEPROM_OrRestart()    position 4\n");
    return true;
  }
  
public:
  boolean must_APmode_be_activated = false; 
  boolean is_APmode_button_pressed = false;

  boolean isLocalServer = true; //this variable is used in the main sketch file
  boolean isInternetServer = true; //this variable is used in the main sketch file

  void check_APmode_pin() {/*only accessed when not in AP mode*/
    if( NodeMCU::getInPinStateAsConsidered( AP_setup_pin ) ) { /*user is asking to enter AP mode, e.g. he's pressing a special button for that*/
      is_APmode_button_pressed = true;
      counter_to_start_APmode--;
      if( counter_to_start_APmode == 0 ) {
        //counter_to_start_APmode = max_counter_to_start_APmode; //check note below
        //is_APmode_button_pressed = false;        //check note below
        /*It's not wrong to set counter_to_start_APmode and is_APmode_button_pressed here, 
         * but I prefer not to put it here because I may enter AP mode from another place
         * so I prefer to put it in a sort of final one place.
         */
        must_APmode_be_activated = true;/*this line is the whole point of this method*/
      }
    } else if( counter_to_start_APmode != max_counter_to_start_APmode ) {/*the long pressing has been cancelled.*/
      counter_to_start_APmode = max_counter_to_start_APmode; //necessary here
      must_APmode_be_activated = false;
      is_APmode_button_pressed = false;
    }    
  }  
  
  void launch() {
    max_counter_to_start_APmode = floor( After_Press_Start_APmode_Timer * 1000 / delay_per_loop );    
    counter_to_start_APmode = max_counter_to_start_APmode;
    /*Now the process of trying to connect to router or entering AP mode, etc.*/
    check_APmode_pin(); 
    if( !is_APmode_button_pressed ) { /*This check is necessary here; the user wants to change network configuration right after 
                                       * NodeMCU startup without falling into a restart (i.e. not being able to change network config)*/
      /*Aside note: in a weird case, it is possible that this block will be bypassed by a user pressing and then the user 
       * cancels his intention to set network configurations, so what happens then?
       * It won't be able to connect to router since the connection had never yet begun
       * (regardless whether the network configuration is good or bad) 
       * and so it will restart.
       */ 
      if( !connectToRouterFromEEPROM_OrRestart() ) { /*If the info in the EEPROM are correct in format but the NodeMCU isn't able to connect 
                                                      * then NodeMCU will restart.*/
        /*This metod returns true in case of successful connection to router*/
        /*This method returns false in case the info in the network configuration in the EEPROM was not valid in format*/
        must_APmode_be_activated = true; /*This is a strict lock to never do anything unless the network configuration are set with a good format*/
      }
    }
  }

  void firstTimeAP_Setup() {
    //setting first-time-things to AP mode
    Serial.println("firstTimeAP_Setup()");      
    resetEEPROM();
    NodeMCU::setConnectFailureNotifierPinHigh();
    //DONE setting first-time-things to AP mode
    runAsAP();     
  }

  boolean checkIncomingMessageAndReturnToNormalOperation() {
    //getting the message (and putting it inside buffer_info char buffer
    if( !getIncomingMessage() ) {
      return false;
    }
    
    if( !checkBuffAndAssignConfigVariables() ) {
      return false;
    }
    /*when we reach here, then we've got successful info from user so we want to return all back to normal*/
    stopAll_AP_Operations(); /*like closing opened socket with user*/
    writeNetworkConfigVariablesToEEPROM(); /*same for this method*/
    connectToRouterOrRestart(); /*this method is only called when the user network configuration variables are all set (thus has been accepted)*/    
    //now it is connected to router, so setting first-time-things back to normal operation
    NodeMCU::setConnectFailureNotifierPinLow();
    counter_to_start_APmode = max_counter_to_start_APmode;
    is_APmode_button_pressed = false;
    must_APmode_be_activated = false;
    return true;    
  }  

  boolean has_EEPROM_been_flushed() {
    //will check the SSID
    char first_char = (char) EEPROM.read(Start_AP_Index_In_EEPROM);    
    return( first_char == trailor );
  }

  void writeNetworkConfigVariablesToEEPROM() {
    /* The buffer is assumed to be well set, so we only have to write it to EEPROM.
     * I account on my specific format to know how many chars to write. I didn't account on something like ending with a null char.
     */
    byte i = 0;
    byte trailor_counter = 0;
    //Serial.println("writeNetworkConfigVariablesToEEPROM     about to write in EEPROM");
    while( acceptChar( i , trailor_counter ) ) { /*we start by writing the determinant, then the SSID, etc...*/
      EEPROM.write( Start_AP_Index_In_EEPROM + i , buffer_info[ i ] );      
      //Serial.printf("writing.");
      if( buffer_info[ i ] == trailor ) {
        trailor_counter++;
      }
      i++;
    }    
    EEPROM.commit();
    //Serial.println("");
    //Serial.println("done writing.");
  }

  void connectToRouterOrRestart() { //this is ("only" - I believe) called on success of reading the buffer containing the network configuration.
    /*The following 2 lines are to set the mac address if the original mac address was considered invalid for static IP allocation by the router.
     * uint8_t mac[6] {0x2C, 0x3A, 0xE8, 0x40,0x31, 0xB6}; //Actually, this is the meant MAC 2C:3A:E8:40:31:BA. ??
     * It is the second nibble that matters being 2, 6, A, or E (really?). But is it the second nibble from last or beginning?? 
     * It looks like from last  B2:31:40:E8:3A:2C or B6-... or BA-... or BE-... 
     */    
    /* 
    if( determinant == trailor ) {
      //do nothing as it will reastart at the end
    }
    */
    if ( WiFi.mode(WIFI_STA) ) {   //please follow this order: WiFi.config, then WiFi.mode(WIFI_STA) then WiFi.begin. https://github.com/esp8266/Arduino/issues/2371      
      if( determinant == 2 ) {
        WiFi.begin( SSID_buff );
      }
      if( determinant == 3 ) {
        WiFi.begin( SSID_buff, password_buff );
      }
      if( determinant == 6 || determinant == 7 ) {
        if( wifi_set_macaddr(STATION_IF, MAC_bytes) ) {
          IPAddress local_IP( local_IP_bytes[0], local_IP_bytes[1], local_IP_bytes[2], local_IP_bytes[3] );  //This will change for each NodeMCU
          IPAddress gateway( gateway_bytes[0], gateway_bytes[1], gateway_bytes[2], gateway_bytes[3] );       //This is gotten from the user local network configuration
          IPAddress subnet( subnet_bytes[0], subnet_bytes[1], subnet_bytes[2], subnet_bytes[3] );      //This is gotten from the user local network configuration
          if ( WiFi.config(local_IP, gateway, gateway, subnet) ) {//this allocates a static IP.  //comment for debugging in case this NodeMCU will use the DHCP of the local network router     
            if( determinant == 6 ) {
              WiFi.begin( SSID_buff );
            } else if( determinant == 7 ) {
              WiFi.begin( SSID_buff, password_buff ); //Nothing would happen if we executed this twice - tested.
            }
          } else {
            Serial.println("connectToRouterOrRestart() error when configuring the 3 IPs");          
          }          
        } else {//mac value wasn't set for some reason
          Serial.println("connectToRouterOrRestart() mac value wasn't set");          
        }
      }
    } else {
      Serial.println("Couldn't make WiFi.mode an STA mode.");  
    }
    if( NodeMCU::isConnectedToWiFi( true ) ) { //checks if connected. This actually waits some time before returning false.
      Serial.println("connected to Router..."); 
      return;
    }
    Serial.println("Restarting as not being connected..."); 
    NodeMCU::restartNodeMCU();
  }

};
