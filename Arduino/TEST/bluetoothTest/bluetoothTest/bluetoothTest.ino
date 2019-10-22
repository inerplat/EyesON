#include <SoftwareSerial.h>

SoftwareSerial bluetooth(10, 11);//RX,TX

unsigned char outputBuffer[100];
unsigned char inputBuffer[100];

void setup() {
  bluetooth.begin(9600);
  Serial.begin(9600);
}

void loop() {
  if(Serial.available())
    bluetooth.write(Serial.read());
   if(bluetooth.available())
    Serial.write(bluetooth.read());
}
