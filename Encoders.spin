{{
  Encoders.spin

  This code contains functions for using the WheelWatcher Encoders.  You must call
  Start in order to start the process that reads the encoders.   At any time, you
  can call GetLeftCount or GetRightCount to read each encoder.   ResetCounters should
  be called when you want to re-start the encoder values at 0. The encoders are
  powered by a single switch (#2 on the bottom-level black dip switch labeled "Encode").
  Make sure that this is on (i.e., down) in order to use the sensor.   If the sensor
  is not needed, you may turn off the power switch to preserve battery power.
}}

CON
  '!!!WARNING!!!    DO NOT CHANGE ANY OF THESE CONSTANTS
  PIN_LEFT_ENCODER_A = 27         ' Pin connected to Channel A of the left encoder 
  PIN_LEFT_ENCODER_B = 26         ' Pin connected to Channel B of the left encoder 
  PIN_RIGHT_ENCODER_A = 0         ' Pin connected to Channel A of the right encoder 
  PIN_RIGHT_ENCODER_B = 1         ' Pin connected to Channel B of the right encoder 


VAR
  long stack[16]
  byte leftA, leftB, rightA, rightB   ' binary reading from the left and right encoders signals  
  word leftCount, rightCount
  

PUB Start
  {{ Start the encoder cog to keep count of the encoder readings. }}
  
  dira[PIN_LEFT_ENCODER_A]~  
  dira[PIN_LEFT_ENCODER_B]~  
  dira[PIN_RIGHT_ENCODER_A]~  
  dira[PIN_RIGHT_ENCODER_B]~  
  cognew(Run, @stack)


PUB GetLeftCount
  {{ Return the count of the left encoder. }}
  return leftCount

  
PUB GetRightCount
  {{ Return the count of the right encoder. }}
  return rightCount

  
PUB ResetCounters
  {{ Reset the counters of the left and right encoders. }}
  leftCount := 0
  rightCount := 0
  leftA := ina[PIN_LEFT_ENCODER_A]
  leftB := ina[PIN_LEFT_ENCODER_B]
  rightA := ina[PIN_RIGHT_ENCODER_A]  
  rightB := ina[PIN_RIGHT_ENCODER_B]  


PRI Run | newVal1, newVal2
  { Read the encoders...forever. }
  
  leftA := ina[PIN_LEFT_ENCODER_A]
  leftB := ina[PIN_LEFT_ENCODER_B]
  rightA := ina[PIN_RIGHT_ENCODER_A]
  rightB := ina[PIN_RIGHT_ENCODER_B]
  repeat
    newVal1 := ina[PIN_LEFT_ENCODER_A]
    newVal2 := ina[PIN_LEFT_ENCODER_B]
    ifnot ((newVal1 == leftA) AND (newVal2 == leftB))
      leftA := newVal1
      leftB := newVal2
      leftCount++
    newVal1 := ina[PIN_RIGHT_ENCODER_A]
    newVal2 := ina[PIN_RIGHT_ENCODER_B]
    ifnot ((newVal1 == rightA)  AND (newVal2 == rightB))
      rightA := newVal1
      rightB := newVal2
      rightCount++
  