#define buzzerPin 13
#define buzzerDelay 100//ms
#define stepDelay 3//ms

volatile int timer0Count = 0;
const int stepmoterPin[4] = {4,5,6,7} ;
unsigned char stepmoterPinState = 0x77;

void stepMove(){
  int tmp = stepmoterPinState>>7;
  stepmoterPinState = (stepmoterPinState << 1)|tmp;
  for(int i = 0;i < 4;i++)
    digitalWrite(stepmoterPin[i], (stepmoterPinState>>i)&0x01);
}

void setup() {
  Serial.begin(9600);
  pinMode(buzzerPin, OUTPUT);
  for(int i = 0;i < 4;i++)
    pinMode(stepmoterPin[i], OUTPUT);
  TCCR0A = 0x02;//CTC mode
  TCCR0B = 0x03;//Clock / 64;
  OCR0A = 250;//1ms=OCR0A*64/16000000hz
  TCNT0 = 0;//Timer/Counter Register
  TIMSK0 = 0x02;//OCIE0A enable
}

void loop() {
  
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
