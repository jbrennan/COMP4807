{{
  PingSensor.spin

  This code contains functions for using the ping sensor (i.e., sonar).
  The code returns the distance from the sensor in CM.  The sensor is
  powered by a single switch (#6 on the top-level black dip switch
  labeled "SNR").  Make sure that this is on (i.e., down) in order to
  use the sensor.  If the sensor is not needed, you may turn off
  the power switch to preserve battery power.  The code here was
  extracted and adapted from (Chris Savage & Jeff Martin)'s Ping.spin
  code from the Parallax Propeller Object Exchange. 
}}


CON
  '!!!WARNING!!!    DO NOT CHANGE THIS CONSTANT  
  PIN_SONAR = 12                  ' PIN connected to ping sensor (DO NOT CHANGE) 


PUB DistanceCM | microseconds, cnt1, cnt2
  {{ Return the distance (in centimeters) to the object in front of the Sonar.
     The data may be invalid when the object is less than 2cm from the sensor. }}
                                                                                 
  outa[PIN_SONAR]~                                                ' Clear I/O Pin
  dira[PIN_SONAR]~~                                               ' Make Pin Output
  outa[PIN_SONAR]~~                                               ' Set I/O Pin
  outa[PIN_SONAR]~                                                ' Clear I/O Pin (> 2 µs pulse)
  dira[PIN_SONAR]~                                                ' Make I/O Pin Input
  waitpne(0, |< PIN_SONAR, 0)                                     ' Wait For Pin To Go HIGH
  cnt1 := cnt                                                     ' Store Current Counter Value
  waitpeq(0, |< PIN_SONAR, 0)                                     ' Wait For Pin To Go LOW 
  cnt2 := cnt                                                     ' Store New Counter Value
  microseconds := (||(cnt1 - cnt2) / (clkfreq / 1_000_000)) >> 1  ' Return Time in µs
  result := microseconds * 10_000 / 29_034 / 10                   ' Convert to Centimeters