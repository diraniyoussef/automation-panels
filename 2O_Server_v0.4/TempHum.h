/* I'm advertisiing the youtuber whom I like.
 * I only got a few lines from his code. Anyway here is his attribute :
 * With the help of Ahmad Nejrabi for Robojax Video 
 * Permission granted to share this code given that this
 * note is kept with the code.
 * Disclaimer: this code is "AS IS" and for educational purpose only.
 * https://www.youtube.com/watch?v=1A4-6hDARQc&feature=youtu.be
 */


#include "AsynchroClient.h"
#include "DHT.h"
#define DHTTYPE DHT11
#define DHTPIN 2

DHT dht( DHTPIN, DHTTYPE ); //DHT11 is probably a special keyword inside "DHT.h" library. As for D4, it is the Rx1 pin for Serial1 comunication.

class TempHumValue {
public:
  float temperature;
  int humidity;
};

class TempHum {
private:

  
public:
  static const int Temperature_Float_Size = 4; /*reflecting the readable digits with the decimal point. 
    *tens-units-decimal_point-tenths so in all 4.*/
  static const int Temperature_Decimals = 1; /*that is tenths so it is 1*/  
  static const int Humidity_Int_Size = 3; /*from 0 (1 readable digit) to 100 (3 readable digits).*/

  TempHum() {    
    dht.begin();
  }
  
  TempHumValue readValues() {   
    TempHumValue tempHumValue;
    tempHumValue.humidity = (int) dht.readHumidity(); //returning the relative humidity which must be between 0 and 100
    tempHumValue.temperature = dht.readTemperature(); //returning in degree Celcius. Max value for DHT 11 module is 60 degrees.
    return tempHumValue;
  }

};
