//#define DEBUG_MODE
#include <SoftwareSerial.h>
#include <Servo.h>

#define DATA_REQUEST '!'
#define EYES_DATA '@'
#define NOT_FOUND '#'
#define CONNECTION '$'
#define END_OF_DATA '~'
#define SLEEP_CHECK '%'
#define SLEEP_STATE_REQUEST '^'
#define buzzerPin 13//11
#define servoPin 9
#define servoLeftAngle 150
#define servoRightAngle 50
#define buzzerDelay 100//ms
#define stepDelay 4//ms
#define servoDelay 1000//ms
#define bluetoothDelay 1000//ms
#define alarmResetDelay 20

SoftwareSerial bluetooth(2, 3);//RX,TX

Servo servo;

char outputBuffer[100];
char inputBuffer[100];
int outputBufferIdx = 0;
int inputBufferIdx = 0;

enum arduinostate{READY, SENDING, WAITING, PROCESSING, WARNING}arduinoState = READY;
enum alarmstate{OFF = '0', ON = '1'}alarmState = OFF;

volatile int timer0Count = 0;
const int stepmoterPin[4] = {5,6,7,8};
unsigned char stepmoterPinState = 0x77;
volatile int waitBluetooth = 0;

int servoMovingTime = -1;
enum servostate{STOP, MOVE, RESET}servoState = STOP;
int notFoundCnt = 0;

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

int charCheck(char ch){
  if(ch >= '0' && ch <= '9'){
    return 1;
  }
  if(ch == '!' || ch == '@' || ch == '#' || ch == '$' || ch == '%' || ch == '^' || ch == '~'){
    return 1;
  }
  return 0;
}

void stepMove(){
  int tmp = stepmoterPinState>>7;
  stepmoterPinState = (stepmoterPinState << 1)|tmp;
  for(int i = 0;i < 4;i++)
    digitalWrite(stepmoterPin[i], (stepmoterPinState>>i)&0x01);
}

void bluetoothConnection(){//bluetooth connection successful
  while(true){
    bluetoothReceive();
    if(inputBuffer[0] == CONNECTION){
      sendData((String)CONNECTION);
      return;
    }
  }
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
  inputBufferIdx = 0;
  waitBluetooth = 0;
  do{
    while(!bluetooth.available());
    inputBuffer[inputBufferIdx] = bluetooth.read();
    if(charCheck(inputBuffer[inputBufferIdx]) == 0){
      while(bluetooth.available()){
        bluetooth.read();
      }
      arduinoState = SENDING;
      return;
    }
  }while(inputBuffer[inputBufferIdx++] != END_OF_DATA);
  inputBuffer[inputBufferIdx] = '\0';
#ifdef DEBUG_MODE
  Serial.print("receive : ");
  Serial.println((String)inputBuffer);
#endif
}

void bluetoothSend(){
  if(waitBluetooth == -1){
    waitBluetooth = -2;
  }
  else if(waitBluetooth == -2){
    switch(outputBuffer[0]){
      case CONNECTION:
        arduinoState = PROCESSING;
      break;
      case SLEEP_STATE_REQUEST:
        arduinoState = WAITING;
      break;
    }
    return;
  }
  else {
    waitBluetooth = 0;
  }
  
  for(outputBufferIdx = 0; outputBuffer[outputBufferIdx] != END_OF_DATA; outputBufferIdx++){
    bluetooth.write(outputBuffer[outputBufferIdx]);
  }
  bluetooth.write(END_OF_DATA);

  switch(outputBuffer[0]){
    case CONNECTION:
      arduinoState = PROCESSING;
    break;
    case SLEEP_STATE_REQUEST:
      arduinoState = WAITING;
    break;
  }
#ifdef DEBUG_MODE
  Serial.print("send : ");
  Serial.println((String)outputBuffer);
#endif
}

void setup() {
  bluetooth.begin(9600);
#ifdef DEBUG_MODE
  Serial.begin(9600);
#endif
  pinMode(buzzerPin, OUTPUT);
  for(int i = 0;i < 4;i++)
    pinMode(stepmoterPin[i], OUTPUT);

  TCCR0A = 0x02;//CTC mode
  TCCR0B = 0x03;//Clock / 64;
  OCR0A = 250;//1ms=OCR0A*64/16000000hz
  TCNT0 = 0;//Timer/Counter Register
  TIMSK0 = 0x02;//OCIE0A enable
  
  delayLCM = getLCM(buzzerDelay,getLCM(stepDelay, servoDelay));
  
  servo.attach(servoPin);
  servo.write(servoLeftAngle);
  servoMovingTime = timer0Count;
  servoState = MOVE;
  
  bluetoothConnection();
  sendData((String)SLEEP_STATE_REQUEST);
}

void loop() {
#ifdef DEBUG_MODE
  Serial.print("arduino State : ");
  switch(arduinoState){
    case SENDING:
      Serial.println("SENDING");
    break;
    case WAITING:
      Serial.println("WAITING");
    break;     
    case PROCESSING:
      Serial.println("PROCESSING");
    break;
  }
#endif
    switch(arduinoState){
    case SENDING://arduino->phone
      bluetoothSend();
     break;
    case WAITING://arduino<-phone
      bluetoothReceive();
      arduinoState = PROCESSING;
    break;
    case PROCESSING:
      switch(inputBuffer[0]){
        case EYES_DATA:
        break;
        case NOT_FOUND:
          if(alarmState == ON){
            notFoundCnt++;
            if(notFoundCnt > alarmResetDelay){
              alarmState = OFF;
            }
          }
        break;
        case SLEEP_CHECK:
          switch(inputBuffer[1]){
            case '0'://normal
            break;
            case '1'://sleep
              alarmState = ON;
              notFoundCnt = 0;
            break;
            case '2'://wake up
              alarmState = OFF;
            break;
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
    alarmState = OFF;
    arduinoState = SENDING;
#ifdef DEBUG_MODE
    Serial.println("reconnect");
#endif
  }
  
  if(servoState != STOP && (timer0Count - servoMovingTime == 100 || timer0Count + delayLCM - servoMovingTime == 100)){
    servo.detach();
    if(servoState == MOVE){
      servoState = STOP;
    }
  }
  
  if(alarmState == ON){
    if(timer0Count % buzzerDelay == 0){
      digitalWrite(buzzerPin, !digitalRead(buzzerPin));
    }
    if(timer0Count % stepDelay == 0){  
      stepMove();
    }
    if(timer0Count % servoDelay == 200){
      servo.attach(servoPin);
      servo.write(servoLeftAngle);
      servoMovingTime = timer0Count;
      servoState = MOVE;
    }
    if(timer0Count % servoDelay == 0){
      servo.attach(servoPin);
      servo.write(servoRightAngle);
      servoMovingTime = timer0Count;
      servoState = MOVE;
    }
  }
  else{
    digitalWrite(buzzerPin, LOW);
    if(servoState != RESET){
      servo.write(servoLeftAngle);
      servoMovingTime = timer0Count;
      servoState = RESET;
    }
  }
  if(timer0Count % delayLCM == 0){
    timer0Count = 0;
  }
  timer0Count++;
}
