#include "InformLocal.h"

const int delay_per_loop = 750;
const int max_EEPROM = Start_AP_Index_In_EEPROM + Max_AP_Buffer_Size;
//D1 and D2 are naturally SCL and SDA
//D8 is reserved as connectFailureNotifierPin

int PCF::absolute_max_pins_number = Out_Pins_Number;
//************************************************This block is for PCF
/*
const uint8_t address_PCF = 0x38;
const byte in_pins_number_PCF1 = 1;
const byte in_pin_PCF1[ in_pins_number_PCF1 ] = {100}; //the order between the two arrays in_pin_PCF1 and out_pin_PCF1 matters.
                                                 // '100' is just a fake input. For the PCF it must not be between  0 and 7
const byte out_pins_number_PCF1 = 8; //I feel it's better to dedicate the whole PCF pins to be outputs or inputs, unless otherwise successfully tested.
const byte out_pin_PCF1[ out_pins_number_PCF1 ] = {0, 1, 2, 3, 4, 5, 6, 7};
const char out_pin_symbol_PCF1[ out_pins_number_PCF1 ] = {'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i'}; //the order between the two arrays out_pin_PCF1 and out_pin_symbol_PCF1 matters.

PCF PCF1( address_PCF , in_pins_number_PCF1 , in_pin_PCF1 , out_pins_number_PCF1 , out_pin_PCF1 , out_pin_symbol_PCF1 ); //needed to be instantiated before setup(). //comment for PCF exclusion
*/
//************************************************End of block.
boolean main_oper_vars_need_to_be_reset = true; /*Necessary. This won't be taken into effect unless NodeMCU connects to router*/
Remote remote; //this is about both server (receiving requests) and client (informing other entities) functionalities.
InformLocal inform_local;
LocalServer local_server;

AP_Op AP_op;
boolean first_time_switchToAPMode_lock = false;

void setupMainVars( boolean isInformRemote ) {
  NodeMCU::getInPinsState();
  if( AP_op.isLocalServer ) {
    local_server.serverSetup(); //to receive report requests from mobile.
  }
  if( AP_op.isInternetServer ) { 
    remote.setup( isInformRemote ); 
  }
  //inform_local.setup(); //no need to since the necessary info are not deleted
}

void stopMainOperationsAndDisconnect() {/*this involves stopping and resetting the needed things to normal in case we returned back to normal operation*/
  if( AP_op.isLocalServer ) {  
    local_server.stopOperations();
  }
  if( AP_op.isInternetServer ) { 
    remote.stopOperations();
  }
  //inform_local.stopOperations();
  if( WiFi.status()== WL_CONNECTED )
    WiFi.disconnect(true); /*normally the last among them*/
  Serial.println("Normal WIFI is now disconnected");
  NodeMCU::yieldAndDelay();
}

void setup() {
  Serial.begin(115200);  
  NodeMCU::setPins();
  NodeMCU::floatingRST(); //order is relevant
  
  NodeMCU::yieldAndDelay(50);
  NodeMCU::lowerAllOutPins(); //this is not to let any pin floating; because the idea is to make the NodeMCU pin HIGH in order to make it into effect.
  NodeMCU::yieldAndDelay(1);   

  //EEPROM stuff
  //PCF1.setEEPROM_PinRange(); //comment for PCF exclusion
  NodeMCU::beginEEPROM(); //This should be after all the PCFs have used setEEPROM_PinRange() so that PCF::absolute_max_pins_number has its correct value.     
  NodeMCU::decodePinsFromEEPROM(); //this should be before the decodeEEPROM() of the PCFs. It should be the first.  
  NodeMCU::yieldAndDelay(); //delaying like 2 seconds before setting pins of PCF8574A, assuming the PCD8574A is powered from the same power supply as the Node MCU's. 
                                /*We might need more delay time if the PCF was powered from another source.*/    
  //PCF1.decodeEEPROM();  //comment for PCF exclusion  
  AP_op.launch(); /*All embedded in this, the EEPROM connection might begin, NodeMCU might restart, and some important variables might be set*/
  TempHum tempHum;
}

void loop() {
  if( AP_op.must_APmode_be_activated ) {
    /* must set connect_failure_notifier_pin to HIGH continuously, must setup panel to AP mode (and make it a server), then must wait for incoming 
     * client connection (initiated by the mobile app) to receive from it the SSID, password, (and optionally static IP and MAC address).
     * If nothing is received, it will keep entering here and waiting for something from the user.
     * Eventually, it will write the gotten info onto the EEPROM. And will test the connection to the router while in the AP_STA mode.
     */
    if( !first_time_switchToAPMode_lock ) {
      first_time_switchToAPMode_lock = true;
      stopMainOperationsAndDisconnect(); /*because of this method I made the lock here instead of inside Setup.h*/
      AP_op.firstTimeAP_Setup(); 
    }
    if( AP_op.checkIncomingMessageAndReturnToNormalOperation() ) { /*After this gets a (valid) message, it then, from inside,
      * connects to the router (given by the user).
      * It returns true if all was successful.
      * It is possible after getting the network configuration that the NodeMCU restarts
      * (from inside the getIncomingMessage() method) if it couldn't connect.
      */
      main_oper_vars_need_to_be_reset = true;
      first_time_switchToAPMode_lock = false;
    }
  } else {
    AP_op.check_APmode_pin();
    if( !AP_op.must_APmode_be_activated ) {/*if AP_op.must_APmode_be_activated has just been set to true
      * then all normal operations are stopped, e.g. all sockets are closed.
      */
      if( !NodeMCU::isConnectedToWiFi( !AP_op.is_APmode_button_pressed ) ) { /*Note that isConnectedToWiFi method actually tries to connect if not yet 
        * connected. I decided that : if APmode button is pressed and it's not yet connected, then I don't really want it to seriously try to connect*/ 
        if( !AP_op.is_APmode_button_pressed ) {
          stopMainOperationsAndDisconnect();
          NodeMCU::restartNodeMCU();
        }
      } else {
        /*Whenever we reach here, the main operation variables, all must be set up.
         *IS IT POSSIBLE IF THE WIFI WAS OFF FOR MOMENTS AND THE PANEL HASN'T RESTARTED THAT WE NEED TO RE-SET THE OPERATION VARIABLES???
         * I never noticed that though.
         * https://github.com/esp8266/Arduino/blob/master/tools/sdk/lwip/src/core/tcp.c
         * https://github.com/esp8266/Arduino/blob/master/libraries/ESP8266WiFi/src/WiFiServer.cpp
         * https://github.com/esp8266/Arduino/blob/master/libraries/ESP8266WiFi/src/WiFiServer.h
         * https://www.arduino.cc/en/Reference/WiFiServer
         * Anyway, I believe it is not a serious issue, since usually a router needs about a minute to be ready again after e.g. a power off of it, and since
         * this NodeMCU restarts within 11 seconds or so, then the restart resolves the issue.
         * 
         * If you want to implement it, then you need to test the return value (which is similar to a byte) of the
         * status() method of a WiFiServer object like server[0], i.e. server[0].status()
         * I based my reasoning on this piece of code
           uint8_t WiFiServer::status()  {
             if (!_pcb)
               return CLOSED;
             return _pcb->state;
           }
           Although that may be the best implementation but I chose to do the following
         */
        if( main_oper_vars_need_to_be_reset ) { /*This is true after any first-time successful connection to router. This will ocur at the beginning
                                                 * and at the exit of the AP mode when it happens*/
          main_oper_vars_need_to_be_reset = false;
          setupMainVars( false );
        }
        toggle_connect_failure_notifier_pin = false; /*I have to set it at the beginning of each loop. This variable may be changed in
                                                      * class AsynchroClient and class InformEntity
                                                       */        
      //  PCF1.pcf8574A.getByte(); //this is to evaluate the method "isDataRead()" //comment for PCF exclusion
      //  if(! PCF1.pcf8574A.isDataRead() ) { //comment for PCF exclusion
          //Serial.println("PCF is not properly connected to Node MCU");
          /*BTW later, make a special blinking for the connectFailureNotifierPin in case of an internal damage causing the PCF to be disconnected.*/
      //  } else { //comment for PCF exclusion
      //    NodeMCU::checkManualUpdate();    
      //    PCF1.checkManualUpdate();    //comment for PCF exclusion

          NodeMCU::checkIfInPinsChanged(); //this has to be before inform_local and inform_remote processing
          
          //informing registered local servers
          //inform_local.process(); //this mantains connections to every local server (preprocessing and postprocessing) and sends a report when an IN pin changes
          
          //run as remote server for incoming requests from remote clients (currently mobiles). Also it runs the InformRemote functionality if isInformRemote was true.
          if( AP_op.isInternetServer ) { 
            remote.process();
          }
          
          //run as local server for incoming requests from local clients (currently mobiles)
          if( AP_op.isLocalServer ) { 
            local_server.process();
          }
      
          if( toggle_connect_failure_notifier_pin ) {
            NodeMCU::toggleConnectFailureNotifierPin();
          } else {
            NodeMCU::setConnectFailureNotifierPinLow(); //Comment for debugging
          }    
          
        //} //comment for PCF exclusion
        
          /* //UNCOMMENT THIS IN CASE OF PCF
        int pcf_check_interval = 5; //each 5 ms refresh pcf.  
        for( int i = 0 ; i < floor( delay_per_loop / pcf_check_interval ) ; i++ ) { // 18 loops x 40 ms = 720 ms which is about 750 ms
          NodeMCU::yieldAndDelay( pcf_check_interval ); //40 ms (instead of 5 ms) is probably noticeable
          
          PCF1->getStateFromEEPROM_AndRefresh(0); //I don't care about getting the state from the EEPROM, but refresh is what I care about.
          */
      
          /*This is really needed because I don't know another way to tell if it's really connected or not.
          * For a critical application, shutting down a pin for 40 ms because the PCF8574A was like shaked is something BAD.
          * Please note the of the EEPROM were made of inputs (thus 8 input pins) then we may not need the getStateFromEEPROM_AndRefresh(...) method, plus its implementation 
          * would need t if all the pinsmodification.
          */
      }
    }
  }
  NodeMCU::yieldAndDelay( delay_per_loop ); //This is VERY IMPORTANT not only for the NodeMCU but also not to let the module cause flooding in messages to the requester (client).
                                // Flooding reports to the requester may cause the requester to execute the new report before the old one (in case of multi-threading)!

}
