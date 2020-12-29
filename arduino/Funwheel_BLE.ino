#include <Arduino.h>
#include "Adafruit_BLE.h"
#include "Adafruit_BluefruitLE_SPI.h"
#include "Adafruit_BluefruitLE_UART.h"
#include "BluefruitConfig.h"
#include <Adafruit_NeoPixel.h>
#include <SPI.h>

//Include libraries copied from VESC. These do not have to be used in order to connect to bluetooth.
//#include "VescUart.h"
//#include "datatypes.h"

//#define DEBUG 
#define SERIALIO Serial1
#define DEBUGSERIAL Serial // usb serial

#define FACTORYRESET_ENABLE        1

Adafruit_BluefruitLE_SPI ble(BLUEFRUIT_SPI_CS, BLUEFRUIT_SPI_IRQ, BLUEFRUIT_SPI_RST);


unsigned long count;

void print_serial(uint8_t* data, int len) {

  DEBUGSERIAL.print("Data to display: "); DEBUGSERIAL.println(len);

  for (int i = 0; i <= len; i++)
  {
    DEBUGSERIAL.print(data[i]);
    DEBUGSERIAL.print(" ");
  }
  DEBUGSERIAL.println("");
}
void BleUartRX(char data[], uint16_t len)
{
  //
  SERIALIO.write(data, len);
  Serial.println( F("[BLE UART RX]" ) );
  print_serial((uint8_t*)data,len);

  int num = ((int)data[1])<<8 | (int)data[2];
  Serial.print("Size of packet: ");
  Serial.print(num);
  Serial.print(" bytes\n");
}
void setup() {
  //Setup UART port
  SERIALIO.begin(19200);
  SetSerialPort(&SERIALIO);
  
  DEBUGSERIAL.begin(19200);

  #ifdef DEBUG
    //SEtup debug port
    SetDebugSerialPort((HardwareSerial*)&DEBUGSERIAL);
  #endif

  ble.begin(VERBOSE_MODE);

  ble.echo(false);
  //ble.sendCommandCheckOK( F("AT+GATTADDSERVICE=uuid=0x1234") );
  //ble.sendCommandWithIntReply( F("AT+GATTADDCHAR=UUID=0x2345,PROPERTIES=0x08,MIN_LEN=1,MAX_LEN=6,DATATYPE=string,DESCRIPTION=string,VALUE=abc"), &charid_string);
  //ble.sendCommandWithIntReply( F("AT+GATTADDCHAR=UUID=0x6789,PROPERTIES=0x08,MIN_LEN=4,MAX_LEN=4,DATATYPE=INTEGER,DESCRIPTION=number,VALUE=0"), &charid_number);

  ble.verbose(false);  // debug info is a little annoying after this point!
  ble.setMode(BLUEFRUIT_MODE_DATA);
  ble.setBleUartRxCallback(BleUartRX);
}

struct bldcMeasure measuredVal;

void loop() {
  ble.update(100);  

  uint8_t payload[256];
  int counter = 0;
  while (SERIALIO.available() && counter < 256) {
    payload[counter++] = SERIALIO.read();
  }
  if (counter) {
    ble.write(payload,counter);
    Serial.print( F("[UART RX]" ) );
    print_serial(payload,counter);
  }
}

