#include <Wire.h> 
#include <math.h>

float RollRate, PitchRate, YawRate;

float AccX, AccY, AccZ;
float AngRoll, AngPitch;
float RollRateCalibrated, PitchRateCalibrated, YawRateCalibrated;
float KalmanRoll = 0;
float RollUncertainty = 10;
float KalmanPitch = 0;
float PitchUncertainty = 10;
//Kalman filter output initialization
float KalOut[] = {0,0};


uint32_t LoopTimer;

//Man This is wild
void kalman_filter(float* KalmanState, float* KalmanUncertainty,
                   float KalmanInput, float KalmanMeasurement) 
    {   
        //Predict Current State
        *KalmanState = *KalmanState + 0.004*KalmanInput;
        //Calculate the uncertainty of prediction
        *KalmanUncertainty = *KalmanUncertainty + 0.004 * 0.004 * 4 * 4;
        //Calculate Kalman gain from uncertanties
        float KalmanGain = *KalmanUncertainty * (1 /(1*(*KalmanUncertainty) + 1));
        //Update the predicted state
        *KalmanState = *KalmanState + KalmanGain * (KalmanMeasurement - *KalmanState);
        //Update uncertantity of predicted state
        *KalmanUncertainty = (1 - KalmanGain) * (*KalmanUncertainty);
        //Output
        //KalOut[0] = KalmanState;
        //KalOut[1] = KalmanUncertainty;
    }

void gyro_signals() {
    //Low Pass Filter
    Wire.beginTransmission(0x68);
    Wire.write(0x1A);
    Wire.write(0x05);
    Wire.endTransmission();
    //Congigure accelorometer outputs +/- 8g
    Wire.beginTransmission(0x68);
    Wire.write(0x1C);
    Wire.write(0x10); //Hex 10 is the binary representation of the data
    Wire.endTransmission();
    //Accelerometer measurements
    Wire.beginTransmission(0x68);
    Wire.write(0x3B);
    Wire.endTransmission();
    Wire.requestFrom(0x68, 6); //request 6 bytes
    int16_t AccXLSB = Wire.read() << 8 | Wire.read();
    int16_t AccYLSB = Wire.read() << 8 | Wire.read();
    int16_t AccZLSB = Wire.read() << 8 | Wire.read();
    //Configure Gyro outputs
    Wire.beginTransmission(0x68);
    Wire.write(0x1B);
    Wire.write(0x8);
    Wire.endTransmission();
    Wire.beginTransmission(0x68);
    Wire.write(0x43);
    Wire.endTransmission();
    //Gyro measurements
    Wire.requestFrom(0x68, 6); //request 6 bytes
    int16_t GyroX = Wire.read() << 8 | Wire.read();
    int16_t GyroY = Wire.read() << 8 | Wire.read();
    int16_t GyroZ = Wire.read() << 8 | Wire.read();
    RollRate = (float)GyroX/65.5;
    PitchRate = (float)GyroY/65.5;
    YawRate = (float)GyroZ/65.5;
    AccX = (float)AccXLSB/4096 - 0.05; //4096LSB per g
    AccY = (float)AccYLSB/4096;
    AccZ = (float)AccZLSB/4096 - 0.1;
    //Calculate Roll and Pitch
    AngRoll = atan(AccY/sqrt(AccX*AccX + AccZ*AccZ))/(M_PI/180);
    AngPitch = -atan(AccX/sqrt(AccY*AccY + AccZ*AccZ))/(M_PI/180);
}

void setup() {
    Serial.begin(57600);
    Wire.setClock(400000);
    Wire.begin();
    delay(250);
    Wire.beginTransmission(0x68); //Begins transmission to the GY-521 board
    Wire.write(0x6B);// PWR_MGMT_1 register
    Wire.write(0x00); //Wakes up MPU-6050
    Wire.endTransmission(true);

    //Calibrate Gyroscope
    delay(1000); //To give time to put it down
    for(int i = 0; i < 1000; i++) {
        gyro_signals();
        RollRateCalibrated += RollRate;
        PitchRateCalibrated += PitchRate;
        YawRateCalibrated += YawRate;
        delay(1);
    }
    RollRateCalibrated /= 1000;
    PitchRateCalibrated /= 1000;
    YawRateCalibrated /= 1000;
    LoopTimer = micros();
}

void loop() {
    gyro_signals();
    RollRate -= RollRateCalibrated;
    PitchRate -= PitchRateCalibrated;
    YawRate -= YawRateCalibrated;
    kalman_filter(&KalmanRoll, &RollUncertainty, RollRate, AngRoll);
    kalman_filter(&KalmanPitch, &PitchUncertainty, PitchRate, AngPitch);
    Serial.print(" Roll Angle [deg] ");
    Serial.print(KalmanRoll);
    Serial.print(" Pitch Angle [deg] ");
    Serial.print(KalmanPitch);
    Serial.print("\n");
    while (micros() - LoopTimer < 4000);
    LoopTimer=micros();
}