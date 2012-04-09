{{
  ServoCalibration.spin

  This code allows you to fine-tune all servo motors.  The wheel servos are operated
  by sending pulses to move them either forwards or backwards.   The pulses are sent
  by the code as a number near 750.  The wheel servos have benn adjusted so that a
  value of around 750 keeps them at the stopped position ... although this varies from
  robot to robot.   For each robot, you will need to determine the proper left and
  right servo "stopped values" that are required by ServoControl.spin.  The program also
  allows you to determine the maximum and minumum extremity values for the left and
  right grippers as well as the head's pitch and yaw.

  Run the program by connecting the USB cable and running the Parallax Serial Terminal
  application.   Press a number or letter corresponding to the menu shown to increase
  or decrease the value of an individual servo.   For the wheel servos, you will notice
  a range of about 4 or 5 values for each servo that do not cause the motors to move.
  Remember this range (write it down) and use the median of the range as your stopped
  servo value.   Do each servo separately.   Use these values wherever your code calls
  the Start function of a ServoControl.spin object.   Also, make the increases and
  decreases to align the grippers and head to the positions that you want and take note
  of the values (i.e., write them down to be used in your program later).   Note that
  all robots will have slightly different values for each servo.  Here are the values
  that you will typically want to determine (some typical values are shown but may
  differ on your robot):
  
    LEFT_GRIPPER_CLOSED = 215    'make sure no interference with right closed gripper
    LEFT_GRIPPER_STRAIGHT = 173  'make sure outward edge aligned with left wheel
    LEFT_GRIPPER_WIDEST = 147    'make sure no interference with left wheel
    RIGHT_GRIPPER_CLOSED = 104   'make sure no interference with left closed gripper
    RIGHT_GRIPPER_STRAIGHT = 147 'make sure outward edge aligned with right wheel
    RIGHT_GRIPPER_WIDEST = 163   'make sure no interference with right wheel
    PITCH_DOWNMOST = 95          'make sure 1cm gap from head to top board
    PITCH_HORIZONTAL = 143       'make sure to get nice and horizontal
    PITCH_UPMOST = 175           'make sure 0.5cm gap between head and bluetooth
    YAW_LEFTMOST = 60            'will not be able to get fully sideways
    YAW_STRAIGHT = 148           'make sure to set straight ahead with pitch horizontal
    YAW_RIGHTMOST = 215          'will not be able to get fully sideways


  The GRIPPER_WIDEST values are the values that causes the gripper to open the
  widest (make sure that it does not go too close to the wheels).   For the
  left servo gripper, the widest open value will actually be the smallest of
  your values wheras it will be the largest value for the right gripper.  Note
  that when choosing the PITCH_DOWNMOST value for the head, allow a centimetre
  between the head plate and the top board so that when yawing the head while
  in the downmost position it will not scrape against the board.   Also ensure
  that the UPMOST position maintains 1/2 CM or so from the Bluetooth card at
  the back of the robot.   Lastly, be aware that the YAW motor does not provide
  180 degrees rotation.  Therefore, you will not be able to get the robot to
  look perpendicular to its body on either side.
}}


CON
    _clkmode = xtal1 + pll16x
    _xinfreq = 5_000_000 

    
OBJ
  RBC:        "RBC"
  Beeper:     "Beeper"
  Servos:     "ServoControl"


VAR
  long  leftGripperValue, rightGripperValue, headPitchValue, headYawValue, leftValue, rightValue, s, v
  byte dataIn[5]    
  
  
PUB Main

  Beeper.Startup
  RBC.Init                      ' Set up bluetooth and wait for Play button to be pressed on PC 
  Beeper.OK
  
  leftValue := 750
  rightValue := 750
  leftGripperValue := Servos#LEFT_GRIPPER_MID
  rightGripperValue := Servos#RIGHT_GRIPPER_MID 
  headPitchValue := Servos#PITCH_MID 
  headYawValue := Servos#YAW_MID 

  Servos.Start(750, 750, true, true, true, true)
  Servos.SetSpeeds(leftValue-750, 750-rightValue)   
  Servos.SetLeftGripper(leftGripperValue)
  Servos.SetRightGripper(rightGripperValue)
  Servos.SetHeadPitch(headPitchValue)
  Servos.SetHeadYaw(headYawValue)               
  
  repeat

    'Wait for the 4-byte command from the PC               
    RBC.ReceiveData(@dataIn)

    'First element in the array is the size of the data that has been sent to the robot
    s := dataIn[1]                                      ' This is the servo number
    v := dataIn[2]*256 + dataIn[3]                      ' This is the servo value
    
    case (s)
        1:
          leftValue := v
          RBC.DebugStr(String("Adjusting Left Wheel Servo to "))  
        2:
          rightValue := v
          RBC.DebugStr(String("Adjusting Right Wheel Servo to ")) 
        3:
          leftGripperValue := v
          RBC.DebugStr(String("Adjusting Left Gripper Servo to ")) 
        4:
          rightGripperValue := v
          RBC.DebugStr(String("Adjusting Right Gripper Servo to ")) 
        5:
          headPitchValue := v
          RBC.DebugStr(String("Adjusting Head Pitch Servo to ")) 
        6:
          headYawValue := v 
          RBC.DebugStr(String("Adjusting Head Yaw Servo to "))
    RBC.DebugLongCR(v) 
                                
    Servos.SetSpeeds(leftValue-750, 750-rightValue)
    Servos.SetLeftGripper(leftGripperValue)
    Servos.SetRightGripper(rightGripperValue)
    Servos.SetHeadPitch(headPitchValue)
    Servos.SetHeadYaw(headYawValue)       
                                   
           