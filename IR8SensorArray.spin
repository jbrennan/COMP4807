{{
  IR8SensorArray.spin

  This code contains functions for using the 8-IR sensor array through the 74HC165 Shift Register.
  The 3 front sensors and 1 back sensor are powered by a single switch (#1 on the top-level black
  dip switch labeled "F_IR") while the 4 side sensors are powered by a second switch (#2 on the
  top-level black dip switch labeled "S_IR").   Make sure that these are on (i.e., down) in order
  to use the sensor.  If either group of sensors is not being used, the respective power switch 
  can be turned off to preserve battery power.  The sensors produce a binary signal of 1 if an
  object is located within either 10cm or 5cm (see diagram) from the sensor and 0 otherwise.  The
  sensors are numbered and located around the robot as follows:

                             ^             
                 \           |           / 
                  \          |          /
                10 \      10 |         / 10
                cm  \     cm |        /  cm
                     \       |       /
                      \      |      /
                    ___\_____|_____/___
                   |   [3]  [2]  [1]   |
          <--------+[4]    FRONT    [7]+-------->
              5cm  |                   |  5cm
                   |                   |
                   |                   |
                   |                   |
                   |                   |
                   |                   |
          10cm     |       BACK        |      10cm
  <----------------+[5]     [0]     [6]+---------------->
                   |_________|_________|
                             |
                             |5cm
                             |
                             v
}}


CON
  '!!!WARNING!!!    DO NOT CHANGE ANY OF THESE CONSTANTS
  PIN_IR_SENSE_LOAD  = 17       ' PIN connected to parallel load of the 74HC165 Shift Register
  PIN_IR_SENSE_CLOCK = 18       ' PIN connected to clock of the 74HC165 Shift Register
  PIN_IR_SENSE_DATA  = 19       ' PIN connected to output of the 74HC165 Shift Register

  
VAR
  byte  readings[8]           ' Stores the latest readings obtained from the sensor array

  
PUB Capture | i
  {{ Read all 8 sensors and store the data in the readings[] array.  }}
  
  dira[PIN_IR_SENSE_DATA]~       ' Make pin input
  dira[PIN_IR_SENSE_CLOCK]~~     ' Make pin output
  dira[PIN_IR_SENSE_LOAD]~~      ' Make pin output

  ' Reset the sensors
  repeat i from 0 to 7
    readings[7-i] := 1 

  ' Capture all 8 sensor readings
  outa[PIN_IR_SENSE_LOAD]~       ' Set pin low
  outa[PIN_IR_SENSE_LOAD]~~      ' Set pin high

  ' Now shift the register to get each value in turn
  readings[7] := 1 - ina[PIN_IR_SENSE_DATA]  
  repeat i from 1 to 7
    outa[PIN_IR_SENSE_CLOCK]~~   ' Set pin high
    outa[PIN_IR_SENSE_CLOCK]~    ' Set pin low
    readings[7-i] := 1 - ina[PIN_IR_SENSE_DATA]  

    
PUB Detect(i)
  {{ Call the Capture method once to read ann sensor values and then call this method to get the value of a specific sensor in the array (i.e., 0 through 7).
     Return 1 if the sensor detects something, otherwise return 0. }}
  result := readings[i]