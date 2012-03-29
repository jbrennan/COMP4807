{{
  ServoControl.spin

  This code contains functions for controlling all of the robot's servo motors
  including the wheels, grippers and head.   The wheel servos are assumed to be
  continuous rotation servos while the grippers and head are assumed to be
  standard servos whose position can be set directly.  All servos obtain their
  power from either the 5V regulator or directly from the power source.  By
  default, the jumper on the bottom-level board is set to supply 5V to all servos
  from the regulator.   A power switch is located under the bottom-level board
  to enable or disable power to all motors ... so if your robot is not moving,
  check this switch to make sure that it is turned on. 

  You must call the Start function once to start the cog that controls the
  servos.   You need to provide the initial values that the wheel servos need in
  order to be in their stopped position.   These values vary from servo to
  servo, so you should run ServoCalibration.spin to determine them. 
}}


CON
  '!!!WARNING!!!    DO NOT CHANGE ANY OF THESE CONSTANTS
  _clkmode = xtal1 + pll16x     ' This is required for proper timing
  _xinfreq = 5_000_000          ' This is required for proper timing
  PIN_LEFT_SERVO = 24            'PIN connected to left wheel servo
  PIN_RIGHT_SERVO = 3            'PIN connected to right wheel servo
  PIN_HEAD_PITCH_SERVO = 21       'PIN connected to head pitch servo
  PIN_HEAD_YAW_SERVO = 20         'PIN connected to head yaw servo
  PIN_LEFT_GRIPPER_SERVO = 25     'PIN connected to left gripper servo
  PIN_RIGHT_GRIPPER_SERVO = 2     'PIN connected to right gripper servo

  ' Maximum and Minimum servo ranges for the head and grippers
  ' THESE VALUES WILL VARY SLIGHTLY FROM ROBOT TO ROBOT
  LEFT_GRIPPER_MIN = 215
  LEFT_GRIPPER_MID = 170
  LEFT_GRIPPER_MAX = 140
  RIGHT_GRIPPER_MIN = 104
  RIGHT_GRIPPER_MID = 150
  RIGHT_GRIPPER_MAX = 181
  PITCH_MIN = 95
  PITCH_MID = 137
  PITCH_MAX = 170 
  YAW_MIN = 61
  YAW_MID = 146
  YAW_MAX = 225
  
  ADJUSTMENT = 0.01
  

VAR
  byte  headPitch               ' Pitch value for head servo              
  byte  headYaw                 ' Yaw value for head servo               
  byte  leftGripper             ' value for left gripper servo              
  byte  rightGripper            ' value for right gripper servo               
  long  stoppedLeftValue        ' servo value that represent a stopped state of left servo wheel
  long  stoppedRightValue       ' servo value that represent a stopped state of right servo wheel
  long  leftSpeed               ' current speed of left servo
  long  rightSpeed              ' current speed of right servo
  
  long currentLeftSpeed
  long currentRightSpeed
  long desiredLeftSpeed
  long desiredRightSpeed

  long temp
  
  long  stack[32]               ' some stack space fof the new cog process                         
  byte  cog                     ' ID of cog that is controlling the servos
  byte  enableWheels            ' set to TRUE if wheels will be used, enables/disables pulses to servos
  byte  enableGrippers          ' set to TRUE if grippers will be used, enables/disables pulses to servos
  byte  enablePitch             ' set to TRUE if head pitch will be used, enables/disables pulses to servos
  byte  enableYaw               ' set to TRUE if head yaw will be used, enables/disables pulses to servos

OBJ
  F: "Float32Full"
  RBC: "RBC"
  Beeper: "Beeper"
PUB Start(leftServoStoppedValue, rightServoStoppedValue, useWheels, useGrippers, usePitch, useYaw)
  {{ Start a cog to control the servos.
    The parameters indicate the values that must be sent to the servos to stop them.
    These are obtained by running the ServoCalibration.spin program. }}

  ' Initially, the servos are off
 ' RBC.Init
  F.start
  stoppedLeftValue := leftServoStoppedValue       
  stoppedRightValue := rightServoStoppedValue
  leftGripper := LEFT_GRIPPER_MID  
  rightGripper := RIGHT_GRIPPER_MID  
  headPitch := PITCH_MID  
  headYaw := YAW_MID
  enableWheels := useWheels
  enableGrippers := useGrippers
  enablePitch := usePitch
  enableYaw := useYaw
  
  leftSpeed := 0       
  rightSpeed := 0
  currentLeftSpeed := 0.0
  currentRightSpeed := 0.0
  desiredLeftSpeed := 0.0
  desiredRightSpeed := 0.0  
  result := (cog := cognew(Run, @stack) + 1) > 0  'return true iff new cog has been created OK

  
PUB SetLeftSpeed(speed)
  {{ Set the speed of the left servo. }}
  'leftSpeed := speed
  desiredLeftSpeed := speed

  
PUB SetRightSpeed(speed)
  {{ Set the speed of the right servo. }}
  'rightSpeed := speed
  desiredRightSpeed := speed
   

PUB SetSpeeds(lSpeed, rSpeed)
  {{ Set the speeds of both servos. }}
  'leftSpeed := lSpeed
  'rightSpeed := rSpeed
  SetLeftSpeed(lSpeed)
  SetRightSpeed(rSpeed)


PUB SetHeadPitch(value)
  {{ Set the pitch of the head servo. }}
  headPitch := value

  
PUB SetHeadYaw(value)
  {{ Set the yaw of the head servo. }}
  headYaw := value


PUB SetLeftGripper(value)
  {{ Set the position of the left gripper servo. }}
  leftGripper := value

  
PUB SetRightGripper(value)
  {{ Set the position of the right gripper servo. }}
  rightGripper := value


PUB Stop
  {{ Stop the cog that was controlling the servos. }}
  if cog > 0
    cogstop(cog-1)

    
PRI Run | i
  ' Set pin directions to output
  if (enableWheels)
     dira[PIN_LEFT_SERVO]~~                                       
     dira[PIN_RIGHT_SERVO]~~                                        
  if (enablePitch)
     dira[PIN_HEAD_PITCH_SERVO]~~ 
  if (enableYaw)
     dira[PIN_HEAD_YAW_SERVO]~~ 
  if (enableGrippers)
     dira[PIN_LEFT_GRIPPER_SERVO]~~ 
     dira[PIN_RIGHT_GRIPPER_SERVO]~~ 

  repeat
    if (enableWheels)
       adjustSpeeds
       MoveWheels
    if (enablePitch)
       MovePitch
    if (enableYaw)
       MoveYaw
    if (enableGrippers)
       MoveGrippers                       
    waitcnt(1600000 + cnt )             


PRI adjustSpeeds
  RBC.DebugStrCr(string("adjust!"))
  if (F.FMax(currentLeftSpeed, desiredLeftSpeed) == currentLeftSpeed)
    currentLeftSpeed := F.FSub(currentLeftSpeed, ADJUSTMENT)
    Beeper.Beep(500, 2000)
    Beeper.Beep(500, 1000)
  elseif (F.FMin(currentLeftSpeed, desiredLeftSpeed) == desiredLeftSpeed)
    currentLeftSpeed := F.FAdd(currentLeftSpeed, ADJUSTMENT)
    Beeper.Beep(500, 1000)
    Beeper.Beep(500, 2200)
    Beeper.Beep(500, 2500)
    Beeper.Beep(500, 2800)
    Beeper.Beep(500, 2200)
  
  if (F.FMax(currentRightSpeed, desiredRightSpeed) == currentRightSpeed)
    currentRightSpeed := F.FSub(currentRightSpeed, ADJUSTMENT)
  elseif (F.FMin(currentRightSpeed, desiredRightSpeed) == desiredRightSpeed)
    currentRightSpeed := F.FAdd(currentRightSpeed, ADJUSTMENT)


PRI MoveWheels | clkCycles
  ' Move the left wheel
  temp := F.FSub(F.FMul(F.FAdd(F.FFloat(stoppedLeftValue), currentLeftSpeed), 160.0), 1250)
  'clkCycles := ((stoppedLeftValue + currentLeftSpeed) * 160 - 1250) #> 400           ' duration * 160 clk cycles (i.e., 160 = 2us) ' - inst. time, min cntMin
  clkCycles := F.FRound(temp) #> 400
  !outa[PIN_LEFT_SERVO]                                            ' set to opposite state
  waitcnt(clkCycles + cnt)                              ' wait until clk gets there 
  !outa[PIN_LEFT_SERVO]                                            ' return to orig. state                   
  ' Cause a 20ms pause.  The 1597700 was calculated as clkCycles as follows:
      'ms:= clkfreq / 1_000                    ' Clock cycles for 1 ms
      'clkCycles := 20 * ms{-2300} #> 400      ' duration * clk cycles for ms - inst. time, min cntMin
  'waitcnt(1600000 + cnt )

  ' Move the right wheel
  temp := F.FSub(F.FMul(F.FSub(F.FFloat(stoppedRightValue), currentRightSpeed), 160.0), 1250)                         
  'clkCycles := ((stoppedRightValue - currentRightSpeed) * 160 - 1250) #> 400           ' duration * 160 clk cycles (i.e., 160 = 2us) ' - inst. time, min cntMin
  clkCycles := F.Fround(temp) #> 400
  !outa[PIN_RIGHT_SERVO]                                            ' set to opposite state
  waitcnt(clkCycles + cnt)                              ' wait until clk gets there 
  !outa[PIN_RIGHT_SERVO]                                            ' return to orig. state                   
  ' Cause a 20ms pause.  The 1597700 was calculated as clkCycles as follows:
      'ms:= clkfreq / 1_000                    ' Clock cycles for 1 ms
      'clkCycles := 20 * ms{-2300} #> 400      ' duration * clk cycles for ms - inst. time, min cntMin
  'waitcnt(1600000 + cnt )                        

    
PRI MovePitch   
  outa[PIN_HEAD_PITCH_SERVO]~~                  'Set "Pin" High
  waitcnt((clkfreq/100_000)*headPitch+cnt)    'Wait for the specifed position (units = 10 microseconds)
  outa[PIN_HEAD_PITCH_SERVO]~                   'Set "Pin" Low
  'waitcnt(clkfreq/100+cnt)                    'Wait 10ms between pulses   

    
PRI MoveYaw   
  outa[PIN_HEAD_YAW_SERVO]~~                    'Set "Pin" High
  waitcnt((clkfreq/100_000)*headYaw+cnt)      'Wait for the specifed position (units = 10 microseconds)
  outa[PIN_HEAD_YAW_SERVO]~                     'Set "Pin" Low
  'waitcnt(clkfreq/100+cnt)                    'Wait 10ms between pulses   

    
PRI MoveGrippers   
  outa[PIN_LEFT_GRIPPER_SERVO]~~                'Set "Pin" High
  waitcnt((clkfreq/100_000)*leftGripper+cnt)  'Wait for the specifed position (units = 10 microseconds)
  outa[PIN_LEFT_GRIPPER_SERVO]~                 'Set "Pin" Low
  'waitcnt(clkfreq/100+cnt)                    'Wait 10ms between pulses
      
  outa[PIN_RIGHT_GRIPPER_SERVO]~~               'Set "Pin" High
  waitcnt((clkfreq/100_000)*rightGripper+cnt) 'Wait for the specifed position (units = 10 microseconds)
  outa[PIN_RIGHT_GRIPPER_SERVO]~                'Set "Pin" Low
  'waitcnt(clkfreq/100+cnt)                    'Wait 10ms between pulses   