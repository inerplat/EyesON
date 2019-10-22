#include <SoftwareSerial.h>

#define DATA_REQUEST '!'
#define EYES_DATA '@'
#define NOT_FOUND '#'
#define CONNECTION '$'
#define END_OF_DATA '~'
#define buzzerPin 13
#define buzzerDelay 100//ms
#define stepDelay 3//ms

SoftwareSerial bluetooth(10, 11);//RX,TX

char outputBuffer[100];
char inputBuffer[100] = {1,};
int outputBufferIdx = 0;
int inputBufferIdx = 0;

enum arduinostate{READY, SENDING, WAITING, PROCESSING, WARNING}arduinoState = READY;

volatile int timer0Count = 0;
const int stepmoterPin[4] = {4,5,6,7} ;
unsigned char stepmoterPinState = 0x77;

void stepMove(){
  int tmp = stepmoterPinState>>7;
  stepmoterPinState = (stepmoterPinState << 1)|tmp;
  for(int i = 0;i < 4;i++)
    digitalWrite(stepmoterPin[i], (stepmoterPinState>>i)&0x01);
}

void bluetoothConnection(){//bluetooth connection successful
  while(!bluetooth.available());
  bluetooth.read();
}

void sendData(String data){//output buffer setting
  int i, len = data.length();
  arduinoState = SENDING;
  for(i = 0;i < len;i++){
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
  
  pinMode(buzzerPin, OUTPUT);
  for(int i = 0;i < 4;i++)
    pinMode(stepmoterPin[i], OUTPUT);
    
  TCCR0A = 0x02;//CTC mode
  TCCR0B = 0x03;//Clock / 64;
  OCR0A = 250;//1ms=OCR0A*64/16000000hz
  TCNT0 = 0;//Timer/Counter Register
  TIMSK0 = 0x02;//OCIE0A enable
  
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

ISR(TIMER0_COMPA_vect){
  TCNT0 = 0;
  if(timer0Count % buzzerDelay == 0){
    digitalWrite(buzzerPin, !digitalRead(buzzerPin));
  }
  if(timer0Count % stepDelay == 0){  
    stepMove();
  }
  if(timer0Count % (buzzerDelay * stepDelay) == 0){
    timer0Count = 0;
  }
  timer0Count++;
}
