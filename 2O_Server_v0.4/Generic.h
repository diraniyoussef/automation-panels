#include <ESP8266WiFi.h>
#include <EEPROM.h>
#include "PCF_.h"

extern const int max_EEPROM;
const byte AP_setup_pin = 0; /*It's D0*/
extern boolean main_oper_vars_need_to_be_reset;

const int In_Pins_Number = 2;
const char in_pin[ In_Pins_Number ] = { '5' , '6' };

const int Out_Pins_Number = 2;
const char out_pin[ Out_Pins_Number ] = { '3' , '4' }; //Please consider the order of out_pin in coherence with the order of in_pin. I.e. the order is a mapping.

boolean in_pin_state[ In_Pins_Number ]; 
boolean an_in_pin_changed;

class General{
public:    
  static int getIntFromHexChar(char aChar) {
    switch (aChar) {
      case '0':
        return (0);
        break;
      case '1':
        return (1);
        break;
      case '2':
        return (2);
        break;
      case '3':
        return (3);
        break;
      case '4':
        return (4);
        break;
      case '5':
        return (5);
        break;
      case '6':
        return (6);
        break;
      case '7':
        return (7);
        break;
      case '8':
        return (8);
        break;
      case '9':
        return (9);
        break;
      case 'a':
        return (10);
        break;
      case 'A':
        return (10);
        break;
      case 'b':
        return (11);
        break;
      case 'B':
        return (11);
        break;
      case 'c':
        return (12);
        break;
      case 'C':
        return (12);
        break;
      case 'd':
        return (13);
        break;
      case 'D':
        return (13);
        break;
      case 'e':
        return (14);
        break;
      case 'E':
        return (14);
        break;        
      case 'f':
        return (15);
        break;
      case 'F':
        return (15);
        break;        
      default:        
        //must not enter here
        return(-1);    
        break;
    }
  }
    
  template <typename T>
  static int arrayIncludeElement( const T array[], int arrayLength, T element ) {
    for (int i = 0; i < arrayLength; i++) {
      if (array[i] == element) {
        return i;
      }
    }
    return -1;
  }

  static int getByteFromTwoHexChars(char higher_nibble_HexChar, char lower_nibble_HexChar ) {
    int higher_nibble = getIntFromHexChar( higher_nibble_HexChar );
    if( higher_nibble == -1 ) {
      return -1;
    }
    int lower_nibble = getIntFromHexChar( lower_nibble_HexChar );
    if( lower_nibble == -1 ) {
      return -1;
    }
    return( higher_nibble * 16 + lower_nibble );
  }

};

class PCF {

private:  
  byte in_pins_number;
  byte* in_pin; //Do NOT call delete on this pointer because it points to something on the stack.
  
  int pins_index_min;
  int pins_index_max;
/*
  void toggle_pin_and_encode_EEPROM( int out_pin_index ) {
    boolean bit_status = pcf8574A.getBit( out_pin[ out_pin_index ] );
    if(! pcf8574A.isDataRead() ) { //not necessary since checked somewhere else and this method is not called if condition were not satisfied
      return;
    }
    if( bit_status ) {      
      pcf8574A.setBit( out_pin[ out_pin_index ] , false );
      encodeEEPROM( out_pin_symbol[ out_pin_index ] , 'F' );
    } else {
      pcf8574A.setBit( out_pin[ out_pin_index ] , true );
      encodeEEPROM( out_pin_symbol[ out_pin_index ] , 'T' );
    }
  }
*/
public:  
  static int absolute_max_pins_number;
/*  
  byte out_pins_number;
  byte* out_pin; //Do NOT call delete on this pointer because it points to something on the stack.
  char* out_pin_symbol; //Do NOT call delete on this pointer because it points to something on the stack.    
  PCF_ pcf8574A;
    
  PCF ( const uint8_t address_arg , const byte in_pins_number_arg , const byte* in_pin_arg , 
          const byte out_pins_number_arg , const byte* out_pin_arg , const char* out_pin_symbol_arg ) {    
    in_pins_number = (byte) in_pins_number_arg;    
    in_pin = (byte*) in_pin_arg;
    out_pins_number = (byte) out_pins_number_arg;
    out_pin = (byte*) out_pin_arg;
    out_pin_symbol = (char*) out_pin_symbol_arg;
    pcf8574A.begin( address_arg ); //whenever PCF8574A starts all its 8 pins will be high
    pcf8574A.setByte(0);//to lower its 8 pins.
  }

  byte symbolToPin( char out_pin_symbol_arg ) { //the in is a D... and the out is also a D...
    for ( int i = 0 ; i < out_pins_number ; i++ ) {
      if ( out_pin_symbol[i] == out_pin_symbol_arg ) {
        return out_pin[i];
      }
    }
    return 255;//shouldn't get here.
  }  

  void setEEPROM_PinRange() {
    pins_index_min = absolute_max_pins_number;    
    absolute_max_pins_number = absolute_max_pins_number + out_pins_number;
    pins_index_max = absolute_max_pins_number;
  }
  
  void decodeEEPROM() {                                          
    char pinChar; //e.g. 'b', 'c', ...
    char pinState; //'T' or 'F'
    boolean boolPinState;
    for ( int i = pins_index_min ; i < pins_index_max ; i++ ) {
      pinChar = (char) EEPROM.read(2*i);      
      pinState = (char) EEPROM.read(2*i + 1); 
      if(pinState == 'T') {
        boolPinState = true;
      } else {
        boolPinState = false;
      }
      Serial.printf("Reading %c%c from EEPROM during encoding\n", pinChar, pinState);
      if ( ! General::arrayIncludeElement( out_pin_symbol , out_pins_number , pinChar ) || !( pinState == 'F' || pinState == 'T' ) ) {
      //PLEASE BEWARE THAT I MADE A CHANGE ABOUT THE RETURN VALUE OF arrayIncludeElement
      
        //Whenever what I read in the EEEPROM is not valid, I make sure to free up to the REST of the EEPROM and then leave.
        Serial.printf("During EEPROM decoding, inconsistent reading from EEPROM happened so I will write '-' into EEPROM from place %d to the end\n", 2*i);
        for (int j = 2 * i ; j < 2 * pins_index_max ; j++) {
          EEPROM.write(j, '-'); //I free the EEPROM by putting a dumb char      
        }
        EEPROM.commit();
        break; 
      } else { //valid search.
        Serial.println("Got a valid search in decoding EEPROM");       
        pcf8574A.setBit ( symbolToPin( pinChar ) , boolPinState );  //I can test whether the PCF is connected or not, but since it's a best effort job then it's ok.
        //We know that pinchar is found for the first time at index 2*i, but is it repeated for any reason at another 2*j where j > i ? 
        // If it's repeated I will have to free it in 2*j place.        
        for (int j = 0 ; j < absolute_max_pins_number ; j++) {
          if ( j == i) {
            continue;
          }          
          if ( EEPROM.read(2*j) == pinChar ) {
            Serial.printf("During EEPROM decoding, found a duplicate of char %c at place %d\n", pinChar, 2*j);
            EEPROM.write( 2*j , '-');
            EEPROM.write( 2*j + 1 , '-');
            EEPROM.commit();
          }
        }
      }
    }          
  }

  boolean checkManualUpdate() {
    for( int i = 0 ; i < in_pins_number ; i++ ) {
      if ( ! pcf8574A.getBit( in_pin[i] ) ) { //this means that only when the pin is grounded then it's considered as as activated input.
        if(! pcf8574A.isDataRead() ) {
          return false;
        }
        //toggle corresponding out pin
        toggle_pin_and_encode_EEPROM( i );
      }
    }
    return true;
  }

  boolean updatePinAndEEPROM( char symbol, boolean bool_state ) {    
    if( General::arrayIncludeElement( out_pin_symbol , out_pins_number , symbol ) ) { 
    //PLEASE BEWARE THAT I MADE A CHANGE ABOUT THE RETURN VALUE OF arrayIncludeElement
      char state;
      if( bool_state ) {
        state = 'T';
      } else {
        state = 'F';
      }
      if( pcf8574A.setBit( symbolToPin( symbol ), bool_state )  ) { //not sure if this is necessary since we check in arrayIncludeElement beforehand
        encodeEEPROM( symbol, state );
      }
      return true;
    } else {
      return false;
    }
  }
  
  void encodeEEPROM( char out_pin_char, char state ) {
    Serial.printf("Now encoding %c%c into EEPROM\n", out_pin_char, state );
    int i; //i represents the pin index in EEPROM
    for( i = pins_index_min ; i < pins_index_max ; i++ ) {
      if ( EEPROM.read(2*i) == out_pin_char ) {
        Serial.printf("Found the pin in EEPROM during encoding... At position: %d\n", i);
        EEPROM.write(2*i + 1, state);
        EEPROM.commit();
        //once you update the EEPROM, your job is over.
        return;
      }        
    }
    if ( i == pins_index_max ) {//usually you won't reach here unless it's the first time.      
      Serial.printf("Pin %c was not found during EEPROM encoding.\n", out_pin_char);      
      for ( i = 2 * pins_index_min ; i < 2 * pins_index_max ; i++ ) {
        if( EEPROM.read(i) == '-' ) {
          Serial.printf("Allocating the pin for the first time in EEPROM during encoding... At the position of the first '-' which is position: %d\n", i);
          EEPROM.write( i , out_pin_char );
          EEPROM.write( i + 1 , state );
          EEPROM.commit();
          return;
        }
      }
    }
  }

  boolean addPinsToReport( char* message , int* last_length_ptr ) {
    //adding something like ObTOcTOdFOeT to *message in order to send a report.
    char char_symbol;
    boolean state;
    char state_of_pin;
    
    for( int i = 0 ; i < out_pins_number ; i++ ) { //please note that the real length of out_pin_symbol is String(out_pin_symbol).length() - 1      
      boolean state = pcf8574A.getBit( out_pin[i] );             
      if(! pcf8574A.isDataRead() ) {
        return false;
      }      
      if( state ) { //NodeMCU::getRealPinFromD(i) is the same as saying Di as of D1 or D2 or ...
        state_of_pin = 'T';
      } else {
        state_of_pin = 'F';
      }
      message[ *last_length_ptr ] = 'O';
      message[ *last_length_ptr + 1 ] = out_pin_symbol[i];
      message[ *last_length_ptr + 2 ] = state_of_pin;
      
      *last_length_ptr = *last_length_ptr + 3;  
    }
    
    return true;
  }
*/
};

class NodeMCU{  
private:
  const static byte connect_failure_notifier_pin = 8;    
  const static byte resetting_pin = 7;    
  
  static void toggle_D_pin_State(int D_pin) {
    if (digitalRead( getRealPinFromD( D_pin ) ) ){
      digitalWrite( getRealPinFromD( D_pin ), LOW );
    } else {
      digitalWrite( getRealPinFromD( D_pin ), HIGH );
    }
  }
/*  
  static void toggle_D_pin_State_AndWriteEEPROM(char symbol) {
    int real_pin = getRealPinFromD( General::getIntFromHexChar( symbol ) );
    if( digitalRead( real_pin ) ){
      digitalWrite( real_pin, LOW );
      encodeEEPROM( symbol, false);
    } else {
      digitalWrite( real_pin, HIGH );
      encodeEEPROM( symbol, true);
    }
  }  
  */  

  static void setBasicPinsBeforeRestart() {
    //0 - 1 - 1 for booting. And 0 - 0 - 1 for flashing. It's 15 - 0 - 2 sequence.    
    digitalWrite(D8, LOW);  //pin 15 
    //setConnectFailureNotifierPinLow(); //no need to because D8 is the notifier pin.
    digitalWrite(D3, HIGH); //pin 0
    digitalWrite(D4, HIGH); //pin 2
  }

  static void pulsingRST_Pin() {
    digitalWrite( getRealPinFromD( resetting_pin ), LOW ); //at this point RST will be connected to GND (according to PCB design)
    delay(100);
    digitalWrite( getRealPinFromD( resetting_pin ), HIGH ); //at this point it start HW resetting.
  }

  static const boolean out_as_considered = true; //true means ON is HIGH
  static boolean getOutPinStateAsConsidered( int D_pin ) {
    if( !out_as_considered ) {
      return ( !digitalRead( NodeMCU::getRealPinFromD( D_pin ) ) );
    } else {
      return ( digitalRead( NodeMCU::getRealPinFromD( D_pin ) ) ); /*the existence of "!" next to digitalRead is in parallel with that of 
      * setOutPinStateAsConsidered
      */
    }
  }
  
public:
  static void setPins() {
    pinMode(D5, INPUT);    
    pinMode(D6, INPUT);
    //pinMode(D10, INPUT);
    //pinMode(D0, INPUT);      
    pinMode( getRealPinFromD( AP_setup_pin ) , INPUT );   
    
    pinMode(D3, OUTPUT); //only for the sake of good restart  //this will be a LED that is controlled by the received report from the obeying panel.
    pinMode(D4, OUTPUT); //only for the sake of good restart  //this will be a LED that is controlled by the received report from the obeying panel.    
    //pinMode(D9, OUTPUT); 
    //pinMode(D10, OUTPUT);       
    pinMode( getRealPinFromD( connect_failure_notifier_pin ) , OUTPUT );   
    pinMode( getRealPinFromD( resetting_pin ) , OUTPUT );   /*I've chosen D7 as the resetting pin since it is 
    *(really?) automatically normally set to LOW when the NodeMCU is powered which is the preference for the PCB design*/
  }

  static void floatingRST() {
    digitalWrite( getRealPinFromD( resetting_pin ), HIGH );
  }

  static void HW_RST() {
    setBasicPinsBeforeRestart();//may not be necessary but ok.
    //Now pulsing the RST button
    pulsingRST_Pin();    
  }
  
  static void setConnectFailureNotifierPinHigh() {
    setOutPinStateAsOpposite( connect_failure_notifier_pin, true );
    //digitalWrite( getRealPinFromD( connect_failure_notifier_pin ), HIGH );
  }

  static void setConnectFailureNotifierPinLow() {
    setOutPinStateAsOpposite( connect_failure_notifier_pin, false );
    //digitalWrite( getRealPinFromD( connect_failure_notifier_pin ), LOW );
  }    

  static int beginEEPROM() {
    Serial.printf( "beginning the EEPROM up to %d\n", max_EEPROM );
    //EEPROM.begin( 2 * PCF::absolute_max_pins_number ); //e.g. 10T1F2T5T6F decoded like this: first byte is moduleId interpreted as a "byte" type, then 0T where 0 is char interpreted
                                                      // instead of a byte because pin 10 may one day be used which would be 'a'    
    //Since I introduced the ability to set SSID, password, static IP and MAC by the user, I had to extend the EEPROM range to a larger memory    
    EEPROM.begin( max_EEPROM );
  }

  static void setOutPinStateAsOpposite( int D_pin , boolean state_bool ) {/*always as opposite*/
    digitalWrite( getRealPinFromD( D_pin ), !state_bool );
  }
  
  static void lowerAllOutPins() {
    for( int i = 0 ; i < sizeof( out_pin ) ; i++ ) {
      //Serial.println("digitalWrite");
      //digitalWrite( getRealPinFromD( General::getIntFromHexChar( out_pin[i] ) ), LOW);
      setOutPinStateAsConsidered( General::getIntFromHexChar( out_pin[i] ) , false );
    }
  }

  static void getInPinsState() {
    for (int i = 0; i < sizeof( in_pin ); i++ ) {
      in_pin_state[i] = digitalRead( getRealPinFromD( General::getIntFromHexChar( in_pin[i] ) ) );
//      Serial.printf("In pin state is %d", in_pin_state[i] );
    }
  }

  static boolean getInPinStateAsConsidered( int D_pin ) {//I mean by "As Considered" that I may choose to consider the LOW as On state for the info. This is in accordance with PCB design
    return ( !digitalRead( NodeMCU::getRealPinFromD( D_pin ) ) );
  }

  static void setOutPinStateAsConsidered( int D_pin , boolean state_bool ) {/*This is in accordance with PCB design.
    * These are used with the out pin D3 and D4. 
     */
    if( !out_as_considered ) {
      state_bool = !state_bool;
    } 
    digitalWrite( NodeMCU::getRealPinFromD( D_pin ), state_bool ); /*this is the only change to make... Usually !state_bool is the 
    * preferred change in terms of PCB design*/
  }

  static boolean isInOutIncoherent() { /*This is an extra check and I'm not sure if this can resolve a weird case once I've been through.
    * If this is meant to resolve something, then it will resolve it in case of just one reflector, but in case of many reflectors 
    * I'm not sure it will help. It will help for sure if we don't update the OUT pins (with the EEPROM) unless we receive the 
    * same values from all reflectors. Which is not yet implemented. It is now made as update for any received report.*/
    for (int i = 0; i < sizeof( in_pin ); i++ ) {
      if( getInPinStateAsConsidered ( General::getIntFromHexChar( in_pin[ i ]  ) ) != 
          getOutPinStateAsConsidered( General::getIntFromHexChar( out_pin[ i ] ) ) ) { /*instead of reading D3 and D4 through 
            * getOutPinStateAsConsidered, we could had read from EEPROM*/
        return true;
      }
    }
    return false;
  }
  
  static void checkIfInPinsChanged() {
    an_in_pin_changed = false;
    for( int i = 0 ; i < sizeof( in_pin ) ; i++ ) {
//      Serial.println("Checking in pin");    
      if( in_pin_state[i] != digitalRead( getRealPinFromD( General::getIntFromHexChar( in_pin[i] ) ) ) ) {
//        Serial.println("In pin has changed");
        an_in_pin_changed = true;
        in_pin_state[i] = digitalRead( getRealPinFromD( General::getIntFromHexChar( in_pin[i] ) ) );
      }      
    }
    /*
    //Only for testing purposes.......
    if( an_in_pin_changed )
      for( int i = 0 ; i < sizeof( in_pin ) ; i++ )
        digitalWrite( getRealPinFromD( General::getIntFromHexChar( out_pin[i] ) ) , digitalRead( getRealPinFromD( General::getIntFromHexChar( in_pin[i] ) ) ) );    
    //End of testing purposes
    */
  }
  
  static void toggleConnectFailureNotifierPin() {
    toggle_D_pin_State(connect_failure_notifier_pin);
  }  
  
  static boolean isConnectedToWiFi( boolean really_try_to_connect ) {
    /* The caller of this method usually directly restart the NodeMCU if this method returned false.
     * In this sense we may say that this method blocks the execution in case it was not connected.
     */
    int i = 0;    
    boolean notifier_pin_set_to_high = false;
    while( WiFi.status() != WL_CONNECTED && i < 5 ) { //waiting for 3 seconds. 
      i++;      
      if (i >= 3) {
        setConnectFailureNotifierPinHigh(); //Comment for debugging
        main_oper_vars_need_to_be_reset = true;
        notifier_pin_set_to_high = true;
      }
      if( really_try_to_connect ) {
        delay(750); //PLEASE DON'T WAIT LESS THAN THIS. LONG WAITING TIME TO CONNECT IS NECESSARY. 
      }
      Serial.print(".");
    }
    if( notifier_pin_set_to_high ) { //needed
      setConnectFailureNotifierPinLow(); //Comment for debugging
    }

    if (WiFi.status()!= WL_CONNECTED) {
      Serial.println("WiFi.status is not connected.");
      return(false);
    } else {
      return(true);
    }
  }
  
  static void restartNodeMCU() {
    setBasicPinsBeforeRestart();
    ESP.reset();//It actually works.
  }
  
  static void decodePinsFromEEPROM() {    //I believe everything in the EEPROM is of type "byte". https://www.arduino.cc/en/Reference/EEPROMWrite        
    //determining the state of various pins
    /*The EEPROM is something like "4T5F8T9TbTcFdFeF".*/
    char pinChar; //e.g. '0', '1', ...
    char pinState; //'T' or 'F'
    boolean boolPinState;
    for( int i = 0 ; i < sizeof(out_pin) ; i++ ) {
      pinChar = (char) EEPROM.read(2*i);      
      pinState = (char) EEPROM.read(2*i + 1); 
      if(pinState == 'T') {
        boolPinState = true;
      } else {
        boolPinState = false;
      }
      if ( ( General::arrayIncludeElement( out_pin, sizeof(out_pin), pinChar ) == -1 ) || !( pinState == 'F' || pinState == 'T' ) ) {
        //Whenever what I read in the EEEPROM is not valid, I make sure to free up to the REST of the EEPROM and then leave.
        Serial.printf("During EEPROM decoding, inconsistent reading from EEPROM happened so I will write '-' into EEPROM from place %d to the end\n", 2*i);
        for (int j = 2 * i; j < 2 * sizeof(out_pin); j++) {
          EEPROM.write(j, '-'); //I free the EEPROM by putting a dumb char      
        }
        EEPROM.commit();
        break; 
      } else { //valid search.
        //digitalWrite( getRealPinFromD( General::getIntFromHexChar( pinChar ) ), boolPinState );
        setOutPinStateAsConsidered( General::getIntFromHexChar( pinChar ) , boolPinState ); //we're concerned with D3 and D4
        //We know that pinchar is found for the first time at index 2*i, but is it repeated for any reason at another 2*j where j > i ? 
        // If it's repeated I will have to free it in 2*j place.        
        for( int j = i + 1 ; j < PCF::absolute_max_pins_number; j++ ) {
          if ( EEPROM.read(2*j) == pinChar ) {
            Serial.printf("During EEPROM decoding, found a duplicate of char %c at place %d\n", pinChar, 2*j);
            EEPROM.write(2*j, '-');
            EEPROM.write(2*j + 1, '-');
            EEPROM.commit();
          }
        }
      }
    }        
  }

  static void encodeEEPROM(char pin, boolean action) {
    char state;
    if (action) {
      state = 'T';
    } else {
      state = 'F';
    }
    int i; //i represents the pinIndexInEEPROM
    for (i = 0; i < sizeof(out_pin); i++) {
      if (EEPROM.read(2*i) == pin) {
        Serial.printf("Found the pin in EEPROM during encoding... At position: %d\n", i);
        EEPROM.write(2*i + 1, state);
        EEPROM.commit();
        //once you update the EEPROM, your job is over.
        return;
      }        
    }
    if (i == sizeof(out_pin)) {//usually you won't reach here unless it's the first time.      
      //i = General::getIndexOfArrayElement( out_pin, sizeof(out_pin), pin );      
      Serial.printf("Pin %c was not found during EEPROM encoding.\n", pin);      
      for (i = 0; i < 2 * sizeof(out_pin); i++) {
        if (EEPROM.read(i) == '-') {
          Serial.printf("Allocating the pin for the first time in EEPROM during encoding... At the position of the first '-' which is position: %d\n", i);
          EEPROM.write(i, pin);
          EEPROM.write(i + 1, state);
          EEPROM.commit();
          return;
        }
      }
    }   
  }
  
  
  static int getRealPinFromD(int D_pin){
     switch (D_pin) {
      case 0:
        return (16);
        break;
      case 1:
        return (5);
        break;
      case 2:
        return (4);
        break;
      case 3:
        return (0);
        break;
      case 4:
        return (2);
        break;
      case 5:
        return (14);
        break;
      case 6:
        return (12);
        break;
      case 7:
        return (13);
        break;
      case 8:
        return (15);
        break;
      case 9:
        return (3);
        break;
      case 10:
        return (1);
        break;
      default:
        return (16);//this should never be used.
        // statements
        break;
     }
  }

  //this is recommended. Check this issue https://github.com/esp8266/Arduino/issues/1017
  static void yieldAndDelay( int delayTime ) {
    yield();
    delay(delayTime);
  }
  static void yieldAndDelay(){
    yieldAndDelay(33);
  }
  /*
  static void checkManualUpdate(){
    for ( int i = 0 ; i < sizeof(in_pin) ; i++ ) {
      if( digitalRead( getRealPinFromD( General::getIntFromHexChar( in_pin[i] ) ) ) ) {
        toggle_D_pin_State_AndWriteEEPROM( out_pin[i] );
      }        
    }    
  }
    */
};
