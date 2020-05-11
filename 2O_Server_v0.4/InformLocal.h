#include "LocalServer.h"

class InformLocal { //If you want to disable this you may do it in the main sketch file.
private:
  static const int panels_number = 1;
  char* panel_IP[ panels_number ] = { "192.168.1.212" }; //{ "192.168.1.212" , "192.168.1.213" , "192.168.1.214" };
  const char* panel_id[ panels_number ] = { "2:\0" }; //{ "5:\0" , "6:\0" , "7:\0" };
  int counter_to_await_report_receipt[ panels_number ] = { 0 }; //{ 0 , 0 , 0 }; //-1 means not awaiting and 0 means to send right away
  AsynchroClient asynchro_client[ panels_number ];
  long time_to_await_for_report_receipt = 8000; //PLEASE let this smaller than max_counter_to_destroy_old_socket
  const AsynchroClient* asynchro_client_pointer_array[ panels_number ] = { &asynchro_client[0] }; //{ &asynchro_client[0] , &asynchro_client[1] , &asynchro_client[2] };
  InformEntity inform_local_entity;

  static void sendReport( AsyncClient* newest_async_client, const char* panel_id_i ) { 
    NodeMCU::yieldAndDelay(); //I think this delay may be useful...
    int last_length = strlen( (const char*) owner_id_buff ) + strlen( (const char*) mod_id_buff ) + strlen( (const char*) panel_id_i );
    char* totalMessage_buff = new char[ last_length + 3 * PCF::absolute_max_pins_number + 1 ]; //the + 1 is for the '\0'
    strcpy( totalMessage_buff, (const char*) owner_id_buff );
    strcat( totalMessage_buff, (const char*) mod_id_buff );
    strcat( totalMessage_buff, (const char*) panel_id_i );       
    
    last_length = RemoteServerMessageOp::addPinsToReport( totalMessage_buff, last_length ); //nothing is special about RemoteServerMessageOp, it's just the addPinsToReport
    totalMessage_buff[ last_length ] = '\0';

    RemoteServerMessageOp::sendMessageToAsync( newest_async_client, totalMessage_buff );    
    delete[] totalMessage_buff;
  }
  
public:
  
  void setup( ) {
    for( int i = 0 ; i < panels_number ; i++ ) {
      asynchro_client[i].setAsynchroClient( panel_IP[i] , 120 , 121 ); //usually all local clients listen on these two ports as being servers.
    }
    inform_local_entity.setup( panels_number, panel_id , counter_to_await_report_receipt , time_to_await_for_report_receipt , asynchro_client_pointer_array , 
                                sendReport );
  }  

  void stopOperations() {
    for( int i = 0 ; i < panels_number ; i++ ) {
      asynchro_client[i].stopOperations();
    }
    inform_local_entity.stopOperations();
  }

  void process() {
    Serial.println("Processing local entities.");
    for( int i = 0 ; i < panels_number ; i++ ) {
      asynchro_client[i].preProcess();
      
      //check incoming messages if any
      for( int incoming_message1_index = 0 ; incoming_message1_index < AsynchroClient::max_incoming_messages_number ; incoming_message1_index++ ) {    
        if( asynchro_client[i].message_in_processing_from_client_1[ incoming_message1_index ] ) { 
          Serial.printf("Checking incoming local message from async 1 of asynchro index %d\n", i);
          Serial.printf("So processing message of port 1 of buffer index %d\n", incoming_message1_index);
          inform_local_entity.checkIncomingMessageAndFixCounter( (char*) asynchro_client[i].buff_1[ incoming_message1_index ], 
                                                                  asynchro_client[i].len_buff_1[ incoming_message1_index ] );
                                                                  /* Not much important note,
                                                        * but anyway: inside this method "checkIncomingMessageAndFixCounter", we have 
                                                        * "getInformedEntityId". This latter works right but makes sort of a redundant task inside when it 
                                                        * comes to knowing the index of the particular mobile (sender) within the "mob_i_Id_buff".
                                                         */         
          asynchro_client[i].message_in_processing_from_client_1[ incoming_message1_index ] = false;
        }
      }

      for( int incoming_message2_index = 0 ; incoming_message2_index < AsynchroClient::max_incoming_messages_number ; incoming_message2_index++ ) {    
        if( asynchro_client[i].message_in_processing_from_client_2[ incoming_message2_index ] ) { 
          Serial.printf("Checking incoming local message from async 2 of asynchro index %d\n", i);
          Serial.printf("So processing message of port 2 of buffer index %d\n", incoming_message2_index);
          inform_local_entity.checkIncomingMessageAndFixCounter( (char*) asynchro_client[i].buff_2[ incoming_message2_index ], 
                                                                  asynchro_client[i].len_buff_2[ incoming_message2_index ] );                                                         
          asynchro_client[i].message_in_processing_from_client_2[ incoming_message2_index ] = false;
        }
      }
    }      

    //check In pins and send reports accordingly
    inform_local_entity.informAllIfInPinChanged();    
    //Say it was connected then disconnected then connected again, the following gives attention to the non-connected entities.
    inform_local_entity.adjustCounterIfDisconnected();
    //handle counters
    inform_local_entity.resendReport_and_adjustCounter();
             
    for( int i = 0 ; i < panels_number ; i++ ) {
      asynchro_client[i].postProcess();
    }
  }

};
