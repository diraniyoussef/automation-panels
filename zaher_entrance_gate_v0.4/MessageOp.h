#include "Setup.h"
//Actually this only serves RemoteServer.h and LocalServer.h

const int Max_Reading_Buffer = 60; //must be less than a byte size (255) or else some variable might be in risk

class Request {
public:
  char pin = '\0'; //this will be the guide if the incoming message was only R? or O3T? e.g.
  boolean action;  
  void getPinAndAction(char pin, boolean action) {
    this->pin = pin;
    this->action = action; 
  }
  boolean isJustReport() {
    return( pin == '\0' );
  }
  void stopOperations() { /*not needed I guess but ok*/
    pin = '\0';
    action = false;
  }
};

class Operation {
public:  

  void setOperInfo( const char* owner_id_buff, const int mob_Number , const char** mob_i_Id_buff, const char* mod_id_buff ) {
    this->owner_id_buff = (char*) owner_id_buff;
    this->mob_Number = (int) mob_Number;
    this->mob_i_Id_buff = (char**) mob_i_Id_buff;
    this->mod_id_buff = (char*) mod_id_buff;
  }
  
  int getIdOfClient( char* mob_id_arg, char* reading_buff ) { //BTW we don't change reading_buff
    //Resetting stuff
    mob_id_arg[0] = '\0'; //this is usually set before calling this method
    int index_of_matched_mob = -1; //this can be a good indication for whether there was a matching mob_id and the owner with the mod were correctly matched too.
                                   //Although we use the first char of reading_buff
    firstColon = 0;
    //Done resetting
    
    this->reading_buff = reading_buff;
    char owner_id_of_message[ Max_Reading_Buffer ];
    owner_id_of_message[0] = '\0';
    getOwnerId( owner_id_of_message, reading_buff ); //reading_buff is input and owner_id_of_message is output
    if ( owner_id_of_message[0] == '\0' || strcmp( owner_id_buff, (const char*) owner_id_of_message ) != 0 ) { //I think the || is redundant
      //Serial.println("leaving in block 1");
      return -1;
    }

    char temporary_mob_Id_of_message [ Max_Reading_Buffer ];
    temporary_mob_Id_of_message[0] = '\0';
    getMobIdOfMessage( temporary_mob_Id_of_message, reading_buff ); 
    if ( temporary_mob_Id_of_message[0] == '\0' ) {
      //Serial.println("leaving in block 2");
      return -1;     
    }
    boolean mob_Id_of_message_is_valid = false;   
    for ( int i = 0; i < mob_Number; i++ ) {
      if( strcmp( mob_i_Id_buff[ i ], (const char*) temporary_mob_Id_of_message ) == 0 ) {
        mob_Id_of_message_is_valid = true;
        index_of_matched_mob = i; 
        break;
      }
    }
    if( !mob_Id_of_message_is_valid ) {
      ///Serial.println("leaving in block 3");
      return -1;
    }
  
    char mod_Id_of_message[ Max_Reading_Buffer ];
    mod_Id_of_message[0] = '\0';
    getModIdOfMessage( mod_Id_of_message , reading_buff );
    
    if ( mod_Id_of_message[0] == '\0' || strcmp( mod_id_buff, (const char*) mod_Id_of_message ) != 0 ) { //e.g. if it didn't find the second ':'
      //Serial.println("leaving in block 4");
      return -1;   
    }
    strcpy( mob_id_arg, temporary_mob_Id_of_message );
    mob_id_arg[ strlen( (const char*) temporary_mob_Id_of_message ) ] = '\0';/*the char we are replacing now corresponds to the size of the useful word and 
      not to Max_Reading_Buffer, because an additional '\0' is added in getMobIdOfMessage(...) where temporary_mob_Id_of_message has been defined.
    */
    //Serial.printf( "mob id seems to be: %s\n", mob_id_arg.c_str() );
    return index_of_matched_mob;  
  }  

private:
  char* owner_id_buff;
  int firstColon;
  char* reading_buff;  
  int mob_Number;
  char** mob_i_Id_buff;
  char* mod_id_buff;
  
  void getOwnerId( char* owner_id_of_message, char* message_buff ) { //owner_id must be '\0' terminated    
    /*In case, for a dummy reason, the message has gotten a weird set of characters (usually when it happens it's justone character) at the beginning,
     * so I won't be searching for the colon (at first), instead I'll be searching for the owner_id_buff as a whole. After that I can get the colon index.
     */
     /*
    int j = 0;     
    for( int i = 0; i < strlen( (const char*) owner_id_buff ) ; i++) {
      while( message_buff[j] != owner_id_buff[i] && j < strlen( message_buff ) ) {
        j++;
      }
    }*/
    char * ptr_of_first_occurrence;
    ptr_of_first_occurrence = strstr( message_buff , owner_id_buff );
    //Serial.printf( "first occurrence of the owner in the message was at %s\n", ptr_of_first_occurrence );
    if( !ptr_of_first_occurrence ) {
      Serial.printf( "Owner wasn't found in message\n" );
      firstColon = -1;
      return;
    } else {
      firstColon = (ptr_of_first_occurrence - message_buff) + strlen( owner_id_buff ) - 1;
      //Serial.printf( "first colon is at %d\n", firstColon );
      strcpy( owner_id_of_message, (const char*) owner_id_buff );      
      owner_id_of_message[ firstColon + 1 ] = '\0';
    }
    
    /*
    firstColon = getFirstCharOccurenceStartingFromIndex( 0, ':', message_buff );
    if ( firstColon > 0 ) { 
      strncpy( owner_id_of_message, (const char*)message_buff, firstColon + 1 );       
      owner_id_of_message[ firstColon + 1 ] = '\0';
    }
    */
  }
  
  int getFirstCharOccurenceStartingFromIndex( int index, char charToCheck, char* s_buff ) {
    if ( s_buff[0] == '\0' ) {
      return (-1);
    }
    for (int i = index ; i < strlen( (const char*) s_buff ) ; i++) { //strlen won't be more than Max_Reading_Buffer, the size of s_buff, by declaration
      if ( s_buff[i] == charToCheck ) {
        return (i);
      }
    }
    return(-1);
  }
  
  void getMobIdOfMessage( char* mob_id_buff, char* s_buff ) { //return value would be like "mob_S4:"
    if ( s_buff[0] == '\0') {
      return;
    }
    int secondColon = getFirstCharOccurenceStartingFromIndex( firstColon + 1, ':', s_buff );
    if( secondColon == -1 )
      return;
    if( secondColon > firstColon + 1 ) {
      strncpy( mob_id_buff, (const char*)(s_buff + firstColon + 1), secondColon - firstColon );
      mob_id_buff[ secondColon - firstColon ] = '\0';
    }
    firstColon = secondColon;
  }

  void getModIdOfMessage( char* mod_id_buff, char* s_buff ) {
    getMobIdOfMessage( mod_id_buff, s_buff );
  }

};
