#include <TimerOne.h>

//---------------------------------------------------------------------------------------------------------
// port listings

// left motor
#define leftMotor 47            // clock signal output (yellow)
#define leftPreset 43           // enable              (green)
#define leftDir 45              // forward/reverse     (orange)
// right motor
#define rightMotor 53           // clock signal output (green)
#define rightPreset 49          // enable              (purple)
#define rightDir 51             // forward/reverse     (yellow)
// sensors
#define rightSensor 20          // right IR sensor
#define leftSensor 21           // left IR sensor
#define targetSensor 2          // inductance sensor
// switches
#define switch1 41
#define switch2 39
#define switch3 37
#define switch4 35
#define switch5 33
#define switch6 31
// pwm output
#define pwm 3                  // generates PWM signal for oscilator
//voltmeter
#define voltmeter 15
// DTMF
#define DTMFvalid 0
int DTMFdigits[5]= {
  29,27,23,22,25};


//---------------------------------------------------------------------------------------------------------
// global variables

// for DTMF data
int DTMFindex = 0;                // index for DTMF data
int DTMFdone = 0;                 // flag for the DTMF data
//char outData[120] = "*#*#*#*#A020B002C024B030C071A133B094*061*064*075*105*149*149*149*149*#";
int DTMFData[120];     
char outData[70];

// for communication
enum MoveType {                   // types of moves the buggy can carry out
  forwardM, backwardM, rightM, leftM, targetFM, targetNFM, targetFRM, targetNFRM};
char inData[30];                  // input string
char inChar;                      // input char
int byteIndex = 0;                // index for inData
# define sendDelay 50             // delay after sending data
MoveType moveType[60];            // stores the moves
int moveNum[60];                  // stores the 'number' of the move
int moveIndex = 0;                // index for the moves
boolean start = false;            // when this flags true movement starts

// for target interrogation
double mapper = 211.64;            // used to calculate the voltages from the analog input
int inputVolts = 10;              // the voltage our buggy supplies
#define RemovePrescale 7             // this is 111 in binary and is used to remove the prescale on timer3 used by pin3
#define fourKprescale 2         // sets PWM frequency to 4KHz  
#define readDelay 700             // delay between setting the switches and the voltage being valid
const float pi = 3.147;

// for movement
boolean rightRead, leftRead;      // saves the previous state of the sensors
int counter = 0;                  // counter used by the movement functions
#define moveDelay 110              // delay between moves
#define turnDelay 70              // delay between detacting line to being truly in line
#define forwardTurnTarget 20      // if counter reaches this value when lining up on a forward move assume it has moved tiles
#define forwardDelay 555          // delay between passing a line to being at the node when moving forward
#define delaySplitNum 5           // amount of times the above delay is split up to remain in line
#define baseSpeed 3600            // motor speed; range of 2000(fast) to 10000 (slow)
#define slowTurn 1100             // amount to slow down turns by compared to the baseSpeed


//---------------------------------------------------------------------------------------------------------
// brief setup:
//      setup the I/O
//      enable motors and switches
//      start timer (period in microseconds)
//      attatch interrupts
void setup()
{
  Serial.begin(9600);

  pinMode(rightSensor, INPUT);
  pinMode(leftSensor, INPUT);
  pinMode(targetSensor, INPUT);
  pinMode(rightMotor, OUTPUT);
  pinMode(rightDir, OUTPUT);
  pinMode(rightPreset, OUTPUT);
  pinMode(leftMotor, OUTPUT);
  pinMode(leftDir, OUTPUT);
  pinMode(leftPreset, OUTPUT);
  pinMode(switch1, OUTPUT);
  pinMode(switch2, OUTPUT);
  pinMode(switch3, OUTPUT);
  pinMode(switch4, OUTPUT);
  pinMode(switch5, OUTPUT);
  pinMode(switch6, OUTPUT);
  pinMode(pwm, OUTPUT);
  pinMode(voltmeter, INPUT);
  pinMode(DTMFvalid, INPUT);
  for(int index=0; index<5; index++)
  {
    pinMode(DTMFdigits[index], INPUT);
  }

  TCCR3B &= ~RemovePrescale;     // this operation (AND plus NOT),  set the three bits in TCCR3B to 0
  TCCR3B |= fourKprescale;       //this operation (OR), replaces the last three bits in TCCR2B with our new value 010

  digitalWrite(switch1, LOW);
  digitalWrite(switch2, LOW);
  digitalWrite(switch3, LOW);
  digitalWrite(switch4, LOW);
  digitalWrite(switch5, LOW);
  digitalWrite(switch6, LOW);

  digitalWrite(rightPreset, HIGH);
  digitalWrite(leftPreset, HIGH);

  Timer1.initialize(1000000);
  Timer1.attachInterrupt(interrupt);
  Timer1.stop();

  attachInterrupt(DTMFvalid, DTMFlisten, RISING);
}


//---------------------------------------------------------------------------------------------------------
// brief loop:
//      check the 'start' flag,
//      when true carry out stored moves
void loop()
{

  if (start == true)
  {   
    for (int i = 0; i  != moveIndex; i++)
    {
      switch(moveType[i])
      {
      case forwardM:
        if (moveNum[i] != 0){
          forward(moveNum[i]);
        }
        break;
      case backwardM:            // no longer supported as it is never used
        break; 
      case rightM:
        right(moveNum[i]);
        break; 
      case leftM:
        left(moveNum[i]);
        break; 
      case targetFM:
        target(moveNum[i], false);
        break; 
      case targetNFM:
        targetProbe(moveNum[i], false);
        break;
      case targetFRM:
        target(moveNum[i], true);
        break;
      case targetNFRM:
        targetProbe(moveNum[i], true);
        break;
      }
    }
    start = false;
    Serial.println("c&");
  }
  else if (DTMFindex >= 70)
  {
    delay(4000);
    DTMF();
    DTMFindex = 0;
  }

  delay(30);
}


//---------------------------------------------------------------------------------------------------------
// brief readVolts:
//      read the value on the analog 'voltmeter' input
//      return this as a voltage
double readVolts()
{
  double volts = 0;
  delay(readDelay);
  volts =((double) analogRead(voltmeter)) / mapper;
  volts = (volts*(double)3) + (double) 0.242;
  return volts;
}


//---------------------------------------------------------------------------------------------------------
// brief setSwitches:
//      from the switch combination 'num' and 'reverse' flag:
//      set the switches to the corresponding combination,
//      take into account the orientation of the target
void setSwitches(int num, boolean reverse)
{
  switch(num)
  {
  case 0: //default

    digitalWrite(switch1, LOW);
    digitalWrite(switch2, LOW);
    digitalWrite(switch3, LOW);
    digitalWrite(switch4, LOW);
    digitalWrite(switch5, LOW);
    digitalWrite(switch6, LOW);
    break;

  case 1: // box 1 test1 box 2 test 2
    // 10V P1, read P2

    digitalWrite(switch1, LOW);
    digitalWrite(switch2, LOW);
    digitalWrite(switch3, LOW);
    digitalWrite(switch4, reverse ^ 0);
    digitalWrite(switch5, reverse ^ 1);
    digitalWrite(switch6, HIGH);
    break;

  case 2: // box3 test test 2 test R2
    // 
    digitalWrite(switch1, LOW);
    digitalWrite(switch2, LOW);
    digitalWrite(switch3, LOW);
    digitalWrite(switch4, reverse ^ 1);
    digitalWrite(switch5, reverse ^ 0);
    digitalWrite(switch6, HIGH);
    break;

  case 3: //box 4 test 1 ..

    digitalWrite(switch1, LOW);
    digitalWrite(switch2, HIGH);
    digitalWrite(switch3, HIGH);
    digitalWrite(switch4, reverse ^ 0);
    digitalWrite(switch5, reverse ^ 0);
    digitalWrite(switch6, HIGH);
    break;

  case 4: // box 4 test 2 ..


    digitalWrite(switch1, LOW);
    digitalWrite(switch2, HIGH);
    digitalWrite(switch3, HIGH);
    digitalWrite(switch4, reverse ^ 1);
    digitalWrite(switch5, reverse ^ 1);
    digitalWrite(switch6, HIGH);
    break;

  case 5: // box 5 test 1 

    digitalWrite(switch1, HIGH);
    digitalWrite(switch2, HIGH);
    digitalWrite(switch3, HIGH);
    digitalWrite(switch4, reverse ^ 1);
    digitalWrite(switch5, reverse ^ 1);
    digitalWrite(switch6, HIGH);
    break;

  case 6: // box 3 test 1 ->read rail voltage

    digitalWrite(switch1, LOW);
    digitalWrite(switch2, LOW);
    digitalWrite(switch3, LOW);
    digitalWrite(switch4, reverse ^ 1);
    digitalWrite(switch5, reverse ^ 1);
    digitalWrite(switch6, HIGH);
    break;

  case 7: // box 6 test 1

    digitalWrite(switch1, LOW);
    digitalWrite(switch2, LOW);
    digitalWrite(switch3, LOW);
    digitalWrite(switch4, reverse ^ 0);
    digitalWrite(switch5, reverse ^ 1);
    digitalWrite(switch6, LOW);
    break;

  case 8: // box 6 test 1

    digitalWrite(switch1, LOW);
    digitalWrite(switch2, HIGH);
    digitalWrite(switch3, HIGH);
    digitalWrite(switch4, reverse ^ 1);
    digitalWrite(switch5, reverse ^ 1);
    digitalWrite(switch6, LOW);
    break;

  case 9: // box 7 test 1

    digitalWrite(switch1, LOW);
    digitalWrite(switch2, LOW);
    digitalWrite(switch3, HIGH);
    digitalWrite(switch4, reverse ^ 1);
    digitalWrite(switch5, reverse ^ 1);
    digitalWrite(switch6, HIGH);
    break;
    
      case 10: // box 7 test 2

    digitalWrite(switch1, HIGH);
    digitalWrite(switch2, LOW);
    digitalWrite(switch3, LOW);
    digitalWrite(switch4, reverse ^ 0);
    digitalWrite(switch5, reverse ^ 1);
    digitalWrite(switch6, HIGH);
    break;

  }
  delay(readDelay);
}


//---------------------------------------------------------------------------------------------------------
// brief targetProbe:
//      from the box number 'num':
//      call the appropriate switch function(s)
//      read the voltage(s)
//      calculate the required values
//      transmit values to the PC for display
void targetProbe(int num, boolean reverse)
{
  switch(num)
  {
  case 1:
    {  
      setSwitches(1, reverse);              // set switches
      double volts = readVolts();           // read voltage
      Serial.println("Target1");
      Serial.print("*");
      Serial.print(volts);
      Serial.println("!00!V1");
      delay(sendDelay);
      break;
    }

  case 2:
    {
      double fudge1 = 0;
      double fudge2 = 0;
      double fudge3 = 0;

      if (reverse == false){
        fudge1 = 0.957;
        fudge2 = 0.024;
        fudge3 = 0.022;
      } 
      else {
        fudge1 = 1;
        fudge2 = 0.029;
        fudge3 = -0.04;
      }

      setSwitches(4, reverse);                      // set switches
      double volts = readVolts();                   // read voltage
      volts = volts*fudge1;
      double R2 = (1/((inputVolts / volts)-1));     // work out R1
      R2 = findE24(R2);

      setSwitches(1, reverse);                      // set switches
      double volts2 = readVolts();                  // read voltage
      double R1 = R2 * ((inputVolts/volts2)-1);      // work out R2
      R1 = findE24(R1);

      Serial.println("Target2");
      Serial.print("*");
      Serial.print(volts2);
      Serial.println("!04!V1");
      delay(sendDelay);
      Serial.print("*");
      Serial.print(R1);
      Serial.println("!05!R1");
      delay(sendDelay);
      Serial.print("*");
      Serial.print(volts);
      Serial.println("!06!V2");
      delay(sendDelay);
      Serial.print("*");
      Serial.print(R2);
      Serial.println("!07!R2");
      delay(sendDelay);
      break;
    }

  case 3:
    {
      double fudge1 = 0;
      double fudge2 = 0;
      double fudge3 = 0;
      double fudge4 = 0;
      
      if (reverse == false){
        fudge1 = 0.97;
        fudge2 = 0.56;
        fudge3 = 0.22;
        fudge4 = 1.2; 
      } 
      else {
        fudge1 = 1.02;
        fudge2 = 0;
        fudge3 = -0.04;
      }

      setSwitches(6, reverse);                    // set switches
      delay(100);
      double voltsRail = readVolts();             // read rail voltage
      voltsRail =  voltsRail * fudge1;
      
      setSwitches(2, reverse);                    // set switches
      double volts = readVolts();                 // read voltage
      double R2 = (voltsRail/volts) - 1 ;            // work out R2

      setSwitches(1, reverse);                    // set switches      
      double volts2 = readVolts();                 // read voltage
      double R3 =(R2/((inputVolts/volts2)-1));      // work out R3

      R2 = findE24(R2);
      R3 = findE24(R3);

      Serial.println("Target3");
      Serial.print("*");
      Serial.print(volts);
      Serial.println("!08!V1");
      delay(sendDelay);
      Serial.print("*");
      Serial.print(R2);
      Serial.println("!09!R2");
      delay(sendDelay);
      Serial.print("*");
      Serial.print(volts2);
      Serial.println("!10!V2");
      delay(sendDelay);
      Serial.print("*");
      Serial.print(R3);
      Serial.println("!11!R3");
      delay(sendDelay);
      break;
    }

  case 4:
    {
      double fudge1 = 0;
      double fudge2 = 0;
      double fudge3 = 0;
      double fudge4 = 0;

      if (reverse == false){
        fudge1 = 1;
        fudge2 = 0.953;
        fudge3 = 0;
        fudge4 = 0;
      } 
      else {
        fudge1 = 0.953;
        fudge2 = 1;
        fudge3 = 0.01;
        fudge4 = 0.01;
      }

      setSwitches(3, reverse);      // set switches
      double volts = readVolts();      // read voltage
      volts = fudge1 * volts;
      double R1 = (((volts/inputVolts)*2.2) - 1.2) / (1-(volts/inputVolts)) + fudge3;         // work out R1
      
      setSwitches(4, reverse);      // set switches
      double volts2 = readVolts();                                                            // read voltage
      volts2 = fudge2 * volts2;
      double R3 = (((volts2/inputVolts)*2.2) - 1.2) / (1-(volts2/inputVolts)) + fudge4 ;       // work out R3
      R1 = findE24(R1);
      R3 = findE24(R3);
      Serial.println("Target4");
      Serial.print("*");
      Serial.print(volts);
      Serial.println("!12!V1");
      delay(sendDelay);
      Serial.print("*");
      Serial.print(R1);
      Serial.println("!13!R1");
      delay(sendDelay);
      Serial.print("*");
      Serial.print(volts2);
      Serial.println("!14!V2");
      delay(sendDelay);
      Serial.print("*");
      Serial.print(R3);
      Serial.println("!15!R3");
      delay(sendDelay);
      break;
    }

  case 5://rms voltage at infinite frequency = 6.812 //max frequency =138.9kHz @ 0 duty cycle// low frequency = 1.773kHz at 240 no higher!
    // resolution is 571 Hz per dutycyle integer -> 0-240
    {
      double fudge1 = 0;
      double fudge2 = 0;
      if (reverse == false){
        fudge1 = 0.95;
        fudge2 = 0;
      } 
      else {
        fudge1 = 1;
        fudge2 = 0;
      }

      setSwitches(4, reverse);                          // set switches
      double volts = readVolts();                       // read voltage
      volts = fudge1 * volts;
      double R1 = 1/((10/ volts) - 1);       // work out R1

  
     setSwitches(10, reverse);                          // set switches
     
  analogWrite(pwm, 0);
  delay(50);
  double initVolts = readVolts();
  Serial.print("initV: ");
  Serial.println(initVolts);
  
  
  double targetValue = 10.142 - initVolts * 1.355;
      double difference;
      double leastDifference;
     
     Serial.print("target: ");
  Serial.println(targetValue);
     
      // run through all possible frequencies, givin our R value

        double F = 1/(2.0 * pi * R1 * 0.001 * 4.7);
      analogWrite(pwm, (138.9 - F)/0.571);
      double V1 = readVolts();
      delay(3);
      double V2 = readVolts();
      delay(3);
      double V3 = readVolts();
     double  averageV = (V1+V2+V3)/3;
     difference = averageV - targetValue;
      difference = abs(difference);
        leastDifference = difference;
       double C1 = 4.7;
     
     
      Serial.print("V1: ");
      Serial.println(averageV);
      Serial.println(F);

      F = 1/(2.0 * pi * R1 * 0.001 * 5.6);
      analogWrite(pwm, (138.9 - F)/0.571);
      V1 = readVolts();
      delay(3);
      V2 = readVolts();
      delay(3);
      V3 = readVolts();
      averageV = (V1+V2+V3)/3;
      Serial.print("V2: ");
      Serial.println(averageV);
      Serial.println(F);
      difference = averageV - targetValue;
      difference = abs(difference);
      if (difference < leastDifference){
        leastDifference = difference;
        C1 = 5.6;
      }

      F = 1/(2.0 * pi * R1 * 0.001 * 6.8);
      analogWrite(pwm, (138.9 - F)/0.571);
      V1 = readVolts();
      delay(3);
      V2 = readVolts();
      delay(3);
      V3 = readVolts();
      averageV = (V1+V2+V3)/3;
      Serial.print("V3: ");
      Serial.println(averageV);
      Serial.println(F);
      difference = averageV - targetValue;
      difference = abs(difference);
      if (difference < leastDifference){
        leastDifference = difference;
        C1 = 6.8;
      }

      F = 1/(2.0 * pi * R1 * 0.001 * 8.2);
      analogWrite(pwm, (138.9 - F)/0.571);
      V1 = readVolts();
      delay(3);
      V2 = readVolts();
      delay(3);
      V3 = readVolts();
      averageV = (V1+V2+V3)/3;
      Serial.print("V4: ");
      Serial.println(averageV);
      Serial.println(F);
      difference = averageV - targetValue;
      difference = abs(difference);
      if (difference < leastDifference){
        leastDifference = difference;
        C1 = 8.2;
      }

      F = 1/(2.0 * pi * R1 * 0.001 * 10);
      analogWrite(pwm, (138.9 - F)/0.571);
      V1 = readVolts();
      delay(3);
      V2 = readVolts();
      delay(3);
      V3 = readVolts();
      averageV = (V1+V2+V3)/3;
      Serial.print("V5: ");
      Serial.println(averageV);
      Serial.println(F);
      difference = averageV - targetValue;
      difference = abs(difference);
      if (difference < leastDifference){
        leastDifference = difference;
        C1 = 10;
      }

      F = 1/(2.0 * pi * R1 * 0.001 * 12);
      analogWrite(pwm, (138.9 - F)/0.571);
      V1 = readVolts();
      delay(3);
      V2 = readVolts();
      delay(3);
      V3 = readVolts();
      averageV = (V1+V2+V3)/3;
      Serial.print("V6: ");
      Serial.println(averageV);
      Serial.println(F);
      difference = averageV - targetValue;
      difference = abs(difference);
      if (difference < leastDifference){
        leastDifference = difference;
        C1 = 12;
      }


      F = 1/(2.0 * pi * R1 * 0.001 * 15);
      analogWrite(pwm, (138.9 - F)/0.571);
      V1 = readVolts();
      delay(3);
      V2 = readVolts();
      delay(3);
      V3 = readVolts();
      averageV = (V1+V2+V3)/3;
      Serial.print("V7: ");
      Serial.println(averageV);
      Serial.println(F);
      difference = averageV - targetValue;
      difference = abs(difference);
      if (difference < leastDifference){
        leastDifference = difference;
        C1 = 15;
      }

      F = 1/(2.0 * pi * R1 * 0.001 * C1);
      R1 = findE24(R1);

      Serial.println("Target5");
      Serial.print("*");
      Serial.print(volts);
      Serial.println("!20!V1");
      delay(sendDelay);
      Serial.print("*");
      Serial.print(R1);
      Serial.println("!16!R1");
      delay(sendDelay);
      Serial.print("*");
      Serial.print(C1);
      Serial.println("!17!F1");
      delay(sendDelay);
      Serial.print("*");
      Serial.print(F);
      Serial.println("!18!K1");
      delay(sendDelay);
      break;
    }

  case 6: //rms voltage at infinite frequency = 6.812 
    {
      double fudge1 = 0;
      double fudge2 = 0;
      double C1;

      if (reverse == false){
        fudge1 = 1.025;
        fudge2 = 0;
      } 
      else {
        fudge1 = 1.029;
        fudge2 = 0.17;
      }

      setSwitches(7, reverse);
      double volts = readVolts();
      volts = fudge1 * volts;
      double R1 = (inputVolts / volts) - 1;
      R1 = findE24(R1);

      double targetValue = 2.69*R1 - 0.49*R1*R1 + 0.724 - fudge2;
      double difference;
      double leastDifference;
      
      setSwitches(5, reverse);

      double F = 1/(2.0 * pi * R1 * 0.001 * 4.7);
      analogWrite(pwm, (138.9 - F)/0.571);
      double V1 = readVolts();
      delay(3);
      double V2 = readVolts();
      delay(3);
      double V3 = readVolts();
      double averageV = (V1+V2+V3)/3;
      difference = averageV - targetValue;
      difference = abs(difference);
      leastDifference = difference;
      C1 = 4.7;

      F = 1/(2.0 * pi * R1 * 0.001 * 5.6);
      analogWrite(pwm, (138.9 - F)/0.571);
      V1 = readVolts();
      delay(3);
      V2 = readVolts();
      delay(3);
      V3 = readVolts();
      averageV = (V1+V2+V3)/3;
      difference = averageV - targetValue;
      difference = abs(difference);
      if (difference < leastDifference){
        leastDifference = difference;
        C1 = 5.6;
      }

      F = 1/(2.0 * pi * R1 * 0.001 * 6.8);
      analogWrite(pwm, (138.9 - F)/0.571);
      V1 = readVolts();
      delay(3);
      V2 = readVolts();
      delay(3);
      V3 = readVolts();
      averageV = (V1+V2+V3)/3;
      difference = averageV - targetValue;
      difference = abs(difference);
      if (difference < leastDifference){
        leastDifference = difference;
        C1 = 6.8;
      }

      F = 1/(2.0 * pi * R1 * 0.001 * 8.2);
      analogWrite(pwm, (138.9 - F)/0.571);
      V1 = readVolts();
      delay(3);
      V2 = readVolts();
      delay(3);
      V3 = readVolts();
      averageV = (V1+V2+V3)/3;
      difference = averageV - targetValue;
      difference = abs(difference);
      if (difference < leastDifference){
        leastDifference = difference;
        C1 = 8.2;
      }

      F = 1/(2.0 * pi * R1 * 0.001 * 10);
      analogWrite(pwm, (138.9 - F)/0.571);
      V1 = readVolts();
      delay(3);
      V2 = readVolts();
      delay(3);
      V3 = readVolts();
      averageV = (V1+V2+V3)/3;
      difference = averageV - targetValue;
      difference = abs(difference);
      if (difference < leastDifference){
        leastDifference = difference;
        C1 = 10;
      }

      F = 1/(2.0 * pi * R1 * 0.001 * 12);
      analogWrite(pwm, (138.9 - F)/0.571);
      V1 = readVolts();
      delay(3);
      V2 = readVolts();
      delay(3);
      V3 = readVolts();
      averageV = (V1+V2+V3)/3;
      difference = averageV - targetValue;
      difference = abs(difference);
      if (difference < leastDifference){
        leastDifference = difference;
        C1 = 12;
      }

      F = 1/(2.0 * pi * R1 * 0.001 * 15);
      analogWrite(pwm, (138.9 - F)/0.571);
      V1 = readVolts();
      delay(3);
      V2 = readVolts();
      delay(3);
      V3 = readVolts();
      averageV = (V1+V2+V3)/3;
      difference = averageV - targetValue;
      difference = abs(difference);
      if (difference < leastDifference){
        leastDifference = difference;
        C1 = 15;
      }


      F = 1/(2.0 * pi * R1 * 0.001 * C1);


      Serial.println("Target6");
      delay(sendDelay);
      Serial.print("*");
      Serial.print(R1);
      Serial.println("!20!R1");
      delay(sendDelay);
      Serial.print("*");
      Serial.print(C1);
      Serial.println("!21!C1");
      delay(sendDelay);
      Serial.print("*");
      Serial.print(F);
      Serial.println("!22!F1");
      delay(sendDelay);
      break;
    }

  case 7:
    {

      double fudge1 = 0;
      double fudge2 = 0;
      double initVolts = 0;

      if (reverse == false){
        fudge1 = 125.48;
        fudge2 = 20.35;
      } 
      else {
        fudge1 = 125.48;
        fudge2 = 20.35;
      }

      setSwitches(9, reverse);
      double volts = readVolts();
      Serial.println(volts);
      double R1 = fudge1 * volts - fudge2;
      R1 = findE24(R1);


      double F = 17.575;
      double C1 = 82;
      
      Serial.println("Target7");
      Serial.print("*");
      Serial.print(R1);
      Serial.println("!24!R");
      delay(sendDelay);
      Serial.print("*");
      Serial.print(C1);
      Serial.println("!25!C");
      delay(sendDelay);
      Serial.print("*");
      Serial.print(F);
      Serial.println("!26!F");
      delay(sendDelay);
      break;
    }
  }

  //setSwitches(0, reverse);
}


//---------------------------------------------------------------------------------------------------------
// brief findE24:
//      from the calculated resitance find the closest E24 resistor value
double findE24(double resistance)
{
  Serial.print("E24 in ");
  Serial.println(resistance);
  double prefVal = resistance;
  signed int scale;

  if(resistance < 0.01){
    prefVal = prefVal * 1000;
    scale = -3;
  } 
  else if (resistance < 0.1){
    prefVal = prefVal * 100;
    scale = -2;
  } 
  else if (resistance < 1){
    prefVal = prefVal * 10;
    scale = -1;
  } 
  else if (resistance < 10){
    prefVal = prefVal;
    scale = 0;
  }
  else if (resistance < 100){
    prefVal = prefVal / 10;
    scale = 1;
  }

  if(prefVal < 1.05){
    prefVal = 1.0;
  } 
  else if (prefVal < 1.15){
    prefVal = 1.1;
  } 
  else if (prefVal < 1.25){
    prefVal = 1.2;
  } 
  else if (prefVal < 1.4){
    prefVal = 1.3;
  } 
  else if (prefVal < 1.55){
    prefVal = 1.5;
  } 
  else if (prefVal < 1.7){
    prefVal = 1.6;
  } 
  else if (prefVal < 1.9){
    prefVal = 1.8;
  } 
  else if (prefVal < 2.1){
    prefVal = 2.0;
  } 
  else if (prefVal < 2.3){
    prefVal = 2.2;
  } 
  else if (prefVal < 2.55){
    prefVal = 2.4;
  } 
  else if (prefVal < 2.85){
    prefVal = 2.7;
  } 
  else if (prefVal < 3.15){
    prefVal = 3.0;
  } 
  else if (prefVal < 3.45){
    prefVal = 3.3;
  } 
  else if (prefVal < 3.75){
    prefVal = 3.6;
  } 
  else if (prefVal < 4.1){
    prefVal = 3.9;
  } 
  else if (prefVal < 4.4){
    prefVal = 4.3;
  } 
  else if (prefVal < 4.9){
    prefVal = 4.7;
  } 
  else if (prefVal < 5.35){
    prefVal = 5.1;
  } 
  else if (prefVal < 5.9){
    prefVal = 5.6;
  } 
  else if (prefVal < 6.5){
    prefVal = 6.2;
  } 
  else if (prefVal < 7.15){
    prefVal = 6.8;
  } 
  else if (prefVal < 7.85){
    prefVal = 7.5;
  } 
  else if (prefVal < 8.65){
    prefVal = 8.2;
  } 
  else if (prefVal < 9.55){
    prefVal = 9.1;
  }
  else {
    prefVal = 10; 
  }

  prefVal = prefVal * pow(10, scale);

  return prefVal;
}


//---------------------------------------------------------------------------------------------------------
// brief DTMF:
//      transmit the DTMF data to the PC
void DTMF()
{
  int inIndex = 0;
  
  for(int index = 0; index != 70; index++)
  {
    switch(DTMFData[inIndex])
    {
    case 17:
      outData[index] = '1';
      break;
    case 18:
      outData[index] = '2';
      break;
    case 19:
      outData[index] = '3';
      break;
    case 20:
      outData[index] = '4';
      break;
    case 21:
      outData[index] = '5';
      break;
    case 22:
      outData[index] = '6';
      break;
    case 23:
      outData[index] = '7';
      break;
    case 24:
      outData[index] = '8';
      break;
    case 25:
      outData[index] = '9';
      break;  
    case 26:
      outData[index] = '0';
      break;  
    case 27:
      outData[index] = '*';
      break;       
    case 28:
      outData[index] = '#';
      break;       
    case 29:
      outData[index] = 'A';
      break;  
    case 30:
      outData[index] = 'B';
      break; 
    case 31:
      outData[index] = 'C'; 
      break; 
    case 16:
      outData[index] = 'D'; 
      break;        
    }
    
    if ((outData[index] == outData[index - 1]) && (index <= 9))
    {
      index--;
    }
    
    else if ((outData[index] == outData[index - 1]) && ((outData[index] == 'A') || (outData[index] == 'B') || (outData[index] == 'C') || (outData[index] == 'D') || (outData[index] == '*')  || (outData[index] == '#')))
    {
     index--; 
    }
    
    else if ((outData[index] == outData[index - 1]) && (index%4 == 2) && (outData[index] != '1') && (outData[index] != '0'))
    {
      index--;
    }
    
    while((index%4 == 0) && (outData[index] != 'A') && (outData[index] != 'B') && (outData[index] != 'C') && (outData[index] != 'D') && (outData[index] != '*')){
    {
      if ((outData[index] == outData[index - 1])) 
      {
        index--;
      }
      
      else if ((outData[index - 1] == outData[index - 2]))
      {
        outData[index - 1] = outData[index];
        index--;
      }
      
      else if ((outData[index - 2] == outData[index - 3]))
      {
        outData[index - 2] = outData[index - 1];
        outData[index - 1] = outData[index];
        index--;
      }
      
    }
    
    }

   inIndex++; 
  }
  Serial.print(outData); //sends data to the computer
  DTMFindex = 0;
}


//---------------------------------------------------------------------------------------------------------
// brief DTMFlisten:
//      upon DTMF interrupt save the DTMF data
void DTMFlisten()
{
  DTMFData[DTMFindex] = 0;
  for(int index = 0 ;index < 5 ;index ++ )
  {
    DTMFData[DTMFindex] += digitalRead(DTMFdigits[index]) << index;        
  }
  DTMFindex += 1;
}


//---------------------------------------------------------------------------------------------------------
// brief right, left and forward:
//      turn right by 'turnNum' * 90 degrees
//      turn left by 'turnNum' * 90 degrees
//      or move forward 'moveNum' spaces
//      use calls to the motor control functions,
//      sensors, and counters to acheive this
void right(int turnNum)
{
  rotateRight();
  for(int i = 0; i < turnNum; i++)
  {
    checkSensors();
    while(rightRead == digitalRead(rightSensor))
    {
      delay(10); 
    }
    Serial.println("r&");
  }
  delay(turnDelay);
  if (digitalRead(leftSensor) == digitalRead(rightSensor))
  {                                                          // check against overshoot
    rotateLeft(); 
    while(digitalRead(leftSensor) == digitalRead(rightSensor))
    {
      delay(7);
    }
  }
  stopBuggy();
  delay(moveDelay);
}

//---------------------------------------------------------------------------------------------------------
void left(int turnNum)
{
  rotateLeft();
  for(int i = 0; i < turnNum; i++)
  {
    checkSensors();

    while(leftRead == digitalRead(leftSensor))
    {
      delay(10); 
    }
    Serial.println("l&");
  }
  delay(turnDelay);
  if (digitalRead(leftSensor) == digitalRead(rightSensor))
  {                                                          // check against overshoot
    rotateRight();
    while(digitalRead(leftSensor) == digitalRead(rightSensor))
    {
      delay(7);
    }
  }
  stopBuggy();
  delay(moveDelay);
}

//---------------------------------------------------------------------------------------------------------
void forward(int moveNum)
{
  checkSensors();
  int i = 0;
  while(i < moveNum)  
  {
    counter = 0;
    goForward();
    if (digitalRead(leftSensor) != digitalRead(rightSensor))
    {                                                        // if on a line check if the tiles changed and continue
      if (rightRead != digitalRead(rightSensor))
      {
        checkSensors();
        Serial.println("f&");
        i++;
      }
      delay(60);
    }


    // RIGHT

    else if((digitalRead(leftSensor) == leftRead)&&(digitalRead(rightSensor) == leftRead))
    {                                                        // else if both sensors are on the left, turn right
    delay(50);
      rotateRight();
      while ((digitalRead(rightSensor) == leftRead)&&(counter != forwardTurnTarget)) 
      {
        delay(4);
        counter++;                                           // increment counter while turing
      }
      if (counter == forwardTurnTarget)                      // if counter reached target we're on the next tile, turn left
      {
        rotateLeft();
        counter = 0;
        while ((digitalRead(leftSensor) == leftRead)&&(counter != (forwardTurnTarget + 5)))
        {
          delay(3);

        }
      }

      delay(turnDelay); 
      stopBuggy();
      Serial.println(counter);
      delay(100);
    }

    // LEFT

    else if((digitalRead(rightSensor) == rightRead)&&(digitalRead(leftSensor) == rightRead))
    {                                                       // else if both sensors are on the right, turn left
    delay(50);
      rotateLeft();
      while ((digitalRead(leftSensor) == rightRead)&&(counter != forwardTurnTarget)) 
      {
        delay(4);
        counter++;                                          // increment counter while turing
      }
      if (counter == forwardTurnTarget)
      {                                                    // if counter reached target we're on the next tile, turn right
        rotateRight();
        counter = 0;
        while ((digitalRead(rightSensor) == rightRead)&&(counter != forwardTurnTarget + 5)) 
        {
          delay(3);

        }
      }

      delay(turnDelay);
      stopBuggy();
      Serial.println(counter);
      delay(100);
    }
  }
  for(int j = 0; j != delaySplitNum; j++)
  {                                                         // carry on going forward and lining up until at the node
    goForward();
    delay(forwardDelay/delaySplitNum);
    lineUp();
  }
}


//---------------------------------------------------------------------------------------------------------
// brief target:
//      approach taget
//      call targetProbe function
//      reverse back to the node
void target(int targetNum, boolean reverse)
{
  checkSensors();
  goForward();
  delay(400);
  stopBuggy();
  delay(moveDelay);
  targetProbe(targetNum, reverse);
  goBackward();
  delay(470);
  stopBuggy();
  Serial.println("t!!");
  delay(moveDelay);
}


//---------------------------------------------------------------------------------------------------------
// brief lineUp:
//      re-align the buggy if needed
void lineUp()
{
  if((digitalRead(rightSensor) == rightRead)&&(digitalRead(leftSensor) == rightRead))
  {
    rotateLeft();
    while (digitalRead(leftSensor) == rightRead)
    {
      delay(5);
    }
  }
  else if((digitalRead(rightSensor) == leftRead)&&(digitalRead(leftSensor) == leftRead))
  {
    rotateRight();
    while (digitalRead(rightSensor) == leftRead)
    {
      delay(5);
    }
  }
  delay(40);

  stopBuggy();
}


//---------------------------------------------------------------------------------------------------------
// brief checkSensors:
//      save the state of the sensor inputs
void checkSensors()
{
  leftRead = digitalRead(leftSensor);
  rightRead = digitalRead(rightSensor);
}


//---------------------------------------------------------------------------------------------------------
// brief goForward, goBackward, rotateRight, rotateLeft and stopBuggy:
//      set the motor control pins
//      and start the timer
//      to control the  movement of the buggy
void goForward()
{
  digitalWrite(leftDir,LOW); 
  digitalWrite(rightDir,HIGH);
  Timer1.setPeriod(baseSpeed);
}

void goBackward()
{
  digitalWrite(leftDir,HIGH);
  digitalWrite(rightDir,LOW);
  Timer1.setPeriod(baseSpeed);
}

void stopBuggy()
{
  Timer1.stop(); 
  digitalWrite(leftMotor, LOW);
  digitalWrite(rightMotor, LOW);
}

void rotateLeft()
{
  digitalWrite(leftDir,HIGH);
  digitalWrite(rightDir,HIGH);
  Timer1.setPeriod(baseSpeed + slowTurn);
}

void rotateRight()
{
  digitalWrite(leftDir,LOW);
  digitalWrite(rightDir,LOW);
  Timer1.setPeriod(baseSpeed + slowTurn);
}


//---------------------------------------------------------------------------------------------------------
// brief interrupt:
//      upon timer match:
//      invert the values on the motor clock input, causing the wheels to turn
void interrupt()
{
  digitalWrite(rightMotor, !digitalRead(rightMotor));
  digitalWrite(leftMotor, !digitalRead(leftMotor));  
}


//---------------------------------------------------------------------------------------------------------
// brief serial event:
//      upon serial event:
//      read the data
//      process it and carry out the appropriate action
void serialEvent() 
{
  while (Serial.available() > 0)     // data avaliable
  {
    if (byteIndex < 29)
    {
      inChar = Serial.read();        // read character
      inData[byteIndex] = inChar;    // store it
      byteIndex++;                   // increment index
      inData[byteIndex] = '\0';      // null terminates the string
    }
  }
  // case 1: 'rst' is received; confirms connection and resets moves---------------------------------------
  if ((inData[byteIndex - 3] == 'r')&&(inData[byteIndex - 2] == 's') && (inData[byteIndex - 1] == 't')) 
  {
    byteIndex = 0;  
    moveIndex = 0;
    Serial.println("ok");
    DTMFindex = 0;
    delay(sendDelay);
  }
  // case 2: '**' is received; preceeding bytes are the next move------------------------------------------
  else if ((inData[byteIndex - 2] == '*')&&(inData[byteIndex - 1] == '*')) 
  {
    switch (inData[byteIndex - 4])
    {
    case 'a':
      moveType[moveIndex] = forwardM;
      break;
    case 'b':
      moveType[moveIndex] = backwardM;
      break;
    case'c':
      moveType[moveIndex] = rightM;
      break;
    case'd':
      moveType[moveIndex] = leftM;
      break;
    case'e':
      moveType[moveIndex] = targetFM;
      break;
    case'f':
      moveType[moveIndex] = targetNFM;
      break;
    case 'g':
      moveType[moveIndex] = targetFRM;
      break;
    case 'h':
      moveType[moveIndex] = targetNFRM;
      break;
    }
    moveNum[moveIndex] = ((int)inData[byteIndex - 3]) - 48;
    moveIndex++; 
    byteIndex = 0;
    Serial.println("ok");
    delay(sendDelay);
  }
  // case 3: 'go' is received; all moves sent, movement can start------------------------------------------
  else if ((inData[byteIndex - 2] == 'g')&&(inData[byteIndex - 1] == 'o')) 
  {
    byteIndex = 0;  
    start = true;
    Serial.println("go");
    delay(sendDelay);
  }
  // case 4: 'dtmf' is received; send dtmf code------------------------------------------------------------
  else if ((inData[byteIndex - 4] == 'd')&&(inData[byteIndex - 3] == 't')&&(inData[byteIndex - 2] == 'm')&&(inData[byteIndex - 1] == 'f')) 
  {
    byteIndex = 0;
    Serial.print(outData);
    Serial.println("jj");
  }
}






