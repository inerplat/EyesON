#include <SoftwareSerial.h>

#define DATA_REQUEST '!'
#define EYES_DATA '@'
#define NOT_FOUND '#'
#define CONNECTION '$'
#define END_OF_DATA '~'

SoftwareSerial bluetooth(10, 11);//RX,TX

char outputBuffer[100];
char inputBuffer[100] = {1,};
int outputBufferIdx = 0;
int inputBufferIdx = 0;

enum arduinostate{READY, SENDING, WAITING, PROCESSING, WARNING}arduinoState = READY;

void bluetoothConnection(){//bluetooth connection successful
  while(!bluetooth.available());
  bluetooth.read();
}

void sendData(String data){//output buffer setting
  int i;
  arduinoState = SENDING;
  for(i = 0;i < data.length();i++){
    outputBuffer[i] = data[i];
  }
  outputBuffer[i] = END_OF_DATA;
}

void bluetoothReceive(){
  inputBufferIdx = 1;
  while(inputBuffer[inputBufferIdx - 1] != END_OF_DATA){
    while(!bluetooth.available());
      inputBuffer[inputBufferIdx++] = bluetooth.read();
  }
  inputBuffer[inputBufferIdx] = '\0';
}

void bluetoothSend(){
  for(outputBufferIdx = 0; outputBuffer[outputBufferIdx] != END_OF_DATA; outputBufferIdx++){
    bluetooth.write(outputBuffer[outputBufferIdx]);
  }
  bluetooth.write(END_OF_DATA);
}

void setup() {
  bluetooth.begin(9600);
  Serial.begin(9600);
  
  bluetoothConnection();
  sendData((String)DATA_REQUEST);
  Serial.println("connection");
}

void loop() {
  switch(arduinoState){
    case SENDING://arduino->phone
      bluetoothSend();
      arduinoState = WAITING;
     break;
    case WAITING://arduino<-phone
      bluetoothReceive();
      arduinoState = PROCESSING;
    break;
    case PROCESSING:
      sendData((String)DATA_REQUEST);
    break;
    case WARNING:
    break;
  }
}
