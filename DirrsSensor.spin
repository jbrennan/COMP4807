{{
  DirrsSensor.spin

  This code contains functions for using the DIRRS+ IR range finder.
  The code returns the distance from the sensor in CM.  The sensor is
  powered by a single switch (#6 on the top-level black dip switch
  labeled "DIRS").  Make sure that this is on (i.e., down) in order to
  use the sensor.  If the sensor is not needed, you may turn off
  the power switch to preserve battery power.  The code here was
  extracted and adapted from (Chris Savage & Jeff Martin)'s Ping.spin
  code from the Parallax Propeller Object Exchange.   
}}


CON
  '!!!WARNING!!!    DO NOT CHANGE ANY OF THESE CONSTANTS
  _clkmode   = xtal1 + pll16x     ' This is required for proper timing
  _xinfreq   = 5_000_000          ' This is required for proper timing
  PIN_DIRRS = 11                  ' PIN connected to DIRRS+ sensor
  BAUD_RATE = 4800                ' baud rate
  BAUD_MODE = 1                   ' non-inverted bits
  DATA_BITS = 8                   ' 8-bit data


PUB DistanceCM | aByte
  {{ Return the distance (in centimeters) to the object in front of the DIRRS+.
     -1 is returned if no object is detected (usually more than 80cm away).
      0 is returned if the object is between 7 to 10cm away.
      Invalid data is returned if the object is less than 7cm away. }}

  'Wait for the header byte
  repeat
    aByte := GetByte
  until (aByte == %10101010)

  'Get the actual integer data (10% was added for more accurate readings)
  result := (((GetByte - "0")*110) + ((GetByte - "0")*11) + (GetByte - "0")) / 10

  'Adjust for some of the invalid data ranges
  if (result == 0)   ' sensor returns 0 when no object detected
    result := -1
  elseif (result => 80)  ' sensor returns large invalid data between 7cm and 10cm
    result := 0


PRI GetByte: byteVal | x, br
  {{ Received a byte from the bluetooth. }}
  
  br := 1_000_000 / BAUD_RATE                         ' Calculate bit rate
  waitpeq(BAUD_MODE << PIN_DIRRS, |< PIN_DIRRS, 0)    ' Wait for idle
  waitpne(BAUD_MODE << PIN_DIRRS, |< PIN_DIRRS, 0)    ' Wait for Start bit
  PauseUs(br*100/90)                                  ' Pause to be centered in 1st bit time
  byteVal := ina[PIN_DIRRS]                           ' Read LSB
  {if BAUD_MODE == 1}
    repeat x from 1 to DATA_BITS-1                    ' Number of bits - 1
      PauseUs(br-70)                                  ' Wait until center of next bit
      byteVal := byteVal | (ina[PIN_DIRRS] << x)      ' Read next bit, shift and store
  {else
    repeat x from 1 to DATA_BITS-1                    ' Number of bits - 1
      PauseUs(br-70)                                  ' Wait until center of next bit
      byteVal := byteVal | ((ina[PIN_Dirrs]^1)<< x)   ' Read next bit, shift and store }

          
PRI PauseUs(duration) | clkCycles, uS
  { Pause for the given number of micro seconds.  Smallest value is 20 at clkfreq = 80Mhz.  Largest value is around 50 seconds at 80Mhz. }

  uS := clkfreq / 1_000_000               ' compute microsecond value 
  clkCycles := duration * uS #> 400       ' duration * clk cycles for us (400=Minimum waitcnt value to prevent lock-up )
                                          ' - inst. time, min cntMin 
  waitcnt(clkcycles + cnt)                ' wait until clk gets there
  