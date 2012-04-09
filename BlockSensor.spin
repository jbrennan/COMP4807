{{
  BlockSensor.spin

  This code contains functions for using the sensor that detects the block at the
  front of the robot.   A binary 1 is returned if the sensor detects an object,
  otherwise 0 is returned.   The sensor is powered by a single switch (#1 on the
  bottom-level black dip switch labeled "Block").  Make sure that this is on (i.e.,
  down) in order to use the sensor.  If the sensor is not needed, you may turn off
  the power switch to preserve battery power.  
}}


CON
  '!!!WARNING!!!    DO NOT CHANGE THIS CONSTANT  
  PIN_BLOCK_DETECT = 16            ' PIN connected to block sensor

PUB Detect
  {{ Return 1 if the sensor detects something, otherwise return 0. }}

  dira[PIN_BLOCK_DETECT]~~       ' Set as output
  outa[PIN_BLOCK_DETECT]:=1      ' Set high
  dira[PIN_BLOCK_DETECT]~        ' Make pin input
  waitcnt(cnt + 100000)          ' Wait a bit
  return 1 - ina[PIN_BLOCK_DETECT]  