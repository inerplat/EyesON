#include <SoftwareSerial.h>
#include <Servo.h>

#define DATA_REQUEST '!'
#define EYES_DATA '@'
#define NOT_FOUND '#'
#define CONNECTION '$'
#define END_OF_DATA '~'
#define SLEEP_CHECK '%'
#define SLEEP_STATE_REQUEST '^'
#define buzzerPin 13
#define servoPin 8
#define buzzerDelay 100//ms
#define stepDelay 3//ms
#define servoDelay 1000//ms
#define bluetoothDelay 10000//ms

SoftwareSerial bluetooth(10, 11);//RX,TX

Servo servo;

int servoAngle = 0;

char outputBuffer[100];
char inputBuffer[100] = {1,};
int outputBufferIdx = 0;
int inputBufferIdx = 0;

float leftEye = 0, rightEye = 0;
float leftEyeWeightedAvg = 0.5, rightEyeWeightedAvg = 0.5;
const float lastDataWeight = 0.2;
const float avgRefVal = 0.5;

enum arduinostate{READY, SENDING, WAITING, PROCESSING, WARNING}arduinoState = READY;
enum alarmstate{OFF = '0', ON = '1'}alarmState = OFF;

volatile int timer0Count = 0;
const int stepmoterPin[4] = {4,5,6,7} ;
unsigned char stepmoterPinState = 0x77;
volatile int waitBluetooth = 0;

int delayLCM = 0x7fffffff;

int getLCM(int a, int b){
  int A = a, B = b;

  while(A * B != 0 && A != B){
    if(A > B){
      A %= B;
    }
    else{
      B %= A;
    }
  }
  
  if(B == 0){  
    return a/A*b;
  }
  else{
    return a/B*b;
  }
}

void stepMove(){
  int tmp = stepmoterPinState>>7;
  stepmoterPinState = (stepmoterPinState << 1)|tmp;
  for(int i = 0;i < 4;i++)
    digitalWrite(stepmoterPin[i], (stepmoterPinState>>i)&0x01);
}

void bluetoothConnection(){//bluetooth connection successful
  while(!bluetooth.available());
}

void sendData(String data){//output buffer setting
  int i, len = data.length();
  for(i = 0;i < len;i++){
    outputBuffer[i] = data[i];
  }
  outputBuffer[i] = END_OF_DATA;
  arduinoState = SENDING;
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

  servo.attach(servoPin);
  servo.write(30);
  
  delayLCM = getLCM(buzzerDelay,getLCM(stepDelay, servoDelay));
  
  bluetoothConnection();
  sendData((String)SLEEP_STATE_REQUEST);
  Serial.println("connection");
}

void loop() {
    switch(arduinoState){
    case SENDING://arduino->phone
      bluetoothSend();
      waitBluetooth = 0;
      arduinoState = WAITING;
     break;
    case WAITING://arduino<-phone
      bluetoothReceive();
      waitBluetooth = -1;
      arduinoState = PROCESSING;
    break;
    case PROCESSING:
      switch(inputBuffer[1]){
        case EYES_DATA:
/*          rightEye = ((String)inputBuffer).substring(2,5).toFloat()/100;
          leftEye = ((String)inputBuffer).substring(5,8).toFloat()/100;
          if(rightEye < rightEyeWeightedAvg * avgRefVal || leftEye < leftEyeWeightedAvg * avgRefVal){
          }
          else{
            rightEyeWeightedAvg = rightEye * lastDataWeight + rightEyeWeightedAvg * (1 - lastDataWeight);
            leftEyeWeightedAvg = leftEye * lastDataWeight + leftEyeWeightedAvg * (1 - lastDataWeight);
          }*/
        break;
        case NOT_FOUND:
        break;
        case SLEEP_CHECK:
          if(inputBuffer[2] == '1') {
            alarmState = ON;
          }
          else if(inputBuffer[2] == '0'){
            alarmState = OFF;
          }
        break;
      }
      sendData((String)SLEEP_STATE_REQUEST);
    break;
  }
}

ISR(TIMER0_COMPA_vect){
  TCNT0 = 0;
  if(waitBluetooth >= 0){
    waitBluetooth++;
  }
  if(waitBluetooth >= bluetoothDelay){
    waitBluetooth = -1;
    sendData((String)SLEEP_STATE_REQUEST);
  }
  if(alarmState == ON){
    if(timer0Count % buzzerDelay == 0){
      digitalWrite(buzzerPin, !digitalRead(buzzerPin));
    }
    if(timer0Count % stepDelay == 0){  
      stepMove();
    }
    if(timer0Count % servoDelay == 0){
      if(servoAngle == 0){
        servo.write(150);
        servoAngle = 1;
      }
      else{
        servo.write(30);
        servoAngle = 0;
      }
    }
  }
  else{
    digitalWrite(buzzerPin, LOW);
  }
  if(timer0Count % delayLCM == 0){
    timer0Count = 0;
  }
  timer0Count++;
}
