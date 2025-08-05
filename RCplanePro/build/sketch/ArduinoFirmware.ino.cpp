#include <Arduino.h>
#line 1 "C:\\Users\\Paul\\Desktop\\RCplanePro\\ArduinoFirmware\\ArduinoFirmware.ino"
#include <Wire.h> 

const int MPU_ADDR = 0x69;

int16_t accelerometer_x, accelerometer_y, accelerometer_z;
int16_t gyro_x, gyro_y, gyro_z;

char tmp_str[7];

#line 10 "C:\\Users\\Paul\\Desktop\\RCplanePro\\ArduinoFirmware\\ArduinoFirmware.ino"
char * convert_int16_to_str(int16_t i);
#line 15 "C:\\Users\\Paul\\Desktop\\RCplanePro\\ArduinoFirmware\\ArduinoFirmware.ino"
void setup();
#line 10 "C:\\Users\\Paul\\Desktop\\RCplanePro\\ArduinoFirmware\\ArduinoFirmware.ino"
char* convert_int16_to_str(int16_t i) {
    sprintf(tmp_str, "%6d", i);
    return tmp_str;
}

void setup() {
    Serial.begin(9600);
    Wire.begin();
    Wire.beginTransmission(MPU_ADDR); //Begins transmission to the GY-521 board
    Wire.write(0x69);// PWR_MGMT_1 register
    Wire.write(0); //Wakes up MPU-6050
    Wire.endTransmission(true);
    Wire.beginTransmission(0x68);
    Wire.write(0x75);
    Wire.endTransmission(false);
    Wire.requestFrom(0x69, 1, true);
    byte who = Wire.read();
    Serial.println(who, HEX);   // should print 0x68
}

// void loop() {
//     Wire.beginTransmission(MPU_ADDR);
//     Wire.write(0x3B);
//     Wire.endTransmission(false);
//     Wire.requestFrom(MPU_ADDR, 7*2, true);

//     accelerometer_x = Wire.read()<<8 | Wire.read();
//     accelerometer_y = Wire.read()<<8 | Wire.read();
//     accelerometer_z = Wire.read()<<8 | Wire.read();
//     gyro_x = Wire.read()<<16 | Wire.read();
//     gyro_y = Wire.read()<<8 | Wire.read();
//     gyro_z = Wire.read()<<8 | Wire.read();

//     Serial.print("aX = "); Serial.print(convert_int16_to_str(accelerometer_x));
//     Serial.print("aY = "); Serial.print(convert_int16_to_str(accelerometer_y));
//     Serial.print("aZ = "); Serial.print(convert_int16_to_str(accelerometer_z));
//     Serial.print("gX = "); Serial.print(convert_int16_to_str(gyro_x));
//     Serial.print("gY = "); Serial.print(convert_int16_to_str(gyro_y));
//     Serial.print("gZ = "); Serial.print(convert_int16_to_str(gyro_z));

//     delay(1000);
// }
