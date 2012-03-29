CON
  _clkmode = xtal1 + pll16x     ' This is required for proper timing
  _xinfreq = 5_000_000          ' This is required for proper timing
  SERVO_STOPPED_LEFT = 750
  SERVO_STOPPED_RIGHT = 750
  
  LEFT_GRIPPER_CLOSED = 208
  LEFT_GRIPPER_MID = 175
  LEFT_GRIPPER_OPEN = 158
  
  RIGHT_GRIPPER_CLOSED = 101
  RIGHT_GRIPPER_MID = 134
  RIGHT_GRIPPER_OPEN = 155
  
  HEAD_TILT_DOWN = 110
  HEAD_TILT_MID = 126
  HEAD_TILT_UP = 143
  
  HEAD_TWIST_LEFT = 81
  HEAD_TWIST_MID = 146
  HEAD_TWIST_RIGHT = 215
  
  SENSOR_FRONT_LEFT = 3
  SENSOR_FRONT_CENTER = 2
  SENSOR_FRONT_RIGHT = 1

  SENSOR_SIDE_FRONT_RIGHT = 7
  SENSOR_SIDE_FRONT_LEFT = 4
  SENSOR_SIDE_BACK_RIGHT = 6
  SENSOR_SIDE_BACK_LEFT = 5
  
  BEEP_SHORT = 250
  BEEP_TONE_LEFT = 1000
  BEEP_TONE_CENTER =3000
  BEEP_TONE_RIGHT = 5000
  
  YES = 1
  NO = 0
  
  HALF_TURNING_AMOUNT = 150
  TURNING_AMOUNT = 310
  BACKWARDS_AMOUNT = 175
  WANDERING_AMOUNT = 500
  
  ' Arbitration
  PRIORITY_WANDER = 1
  PRIORITY_PERFORM_TASK = 2
  PRIORITY_COLLISION_AVOID = 3
  
  STATE_ORIENT = 0
  STATE_FOLLOW = 1
  STATE_ALIGN  = 2

  WHEEL_SPEED_LEFT = 17
  WHEEL_SPEED_RIGHT = 17
  
  INVALID_READING = 111
  
VAR
  long current_state, edge_follow_left, edge_follow_right
  long detect_front, detect_right
  byte out_packet[4]
  long sonar_reading, ir_reading, fudge 
  
OBJ
  RBC: "RBC"
  Beeper: "Beeper"
  Servos: "ServoControl2"
  IRSensors: "IR8SensorArray"
  Sonar: "PingSensor"
  Dirrs : "DirrsSensor"


PUB main
  
  
  RBC.Init
  
  Servos.Start(SERVO_STOPPED_LEFT, SERVO_STOPPED_RIGHT, true, true, true, true)
  Servos.SetRightGripper(100)
  Servos.SetLeftGripper(200)
  'Servos.SetHeadPitch(125)
  Servos.SetHeadYaw(160)
  Beeper.OK
  
  
  current_state := STATE_FOLLOW
  
  repeat 'main loop

    do_moving_as_needed
    
    edge_follow_left := 0
    edge_follow_right := 0
    
    sonar_reading := 0
    ir_reading := 0
    
    
    sonar_reading := Sonar.DistanceCM
    if (sonar_reading < 0)
      'too close
      sonar_reading := INVALID_READING
    else
      'it was valid!
      
    ir_reading := Dirrs.DistanceCM
    if (ir_reading < 2)
      'too close
      ir_reading := INVALID_READING
    else
      'it was valid
    
    
    ' send the data to the PC
    send_readings_to_pc
    


    'reset the sensor readings
    detect_front := NO
    detect_right := NO
    
    IRSensors.capture

    if (IRSensors.Detect(SENSOR_FRONT_RIGHT) OR IRSensors.Detect(SENSOR_FRONT_CENTER) OR IRSensors.Detect(SENSOR_FRONT_LEFT))' OR (sonar_reading < 6))
      detect_front := YES
    if (IRSensors.Detect(SENSOR_SIDE_FRONT_RIGHT) OR IRSensors.Detect(SENSOR_SIDE_BACK_RIGHT))'sonar_reading > 6 AND sonar_reading < 10)'IRSensors.Detect(SENSOR_SIDE_BACK_RIGHT))
      detect_right := YES

    
    case (current_state)
      STATE_FOLLOW:
        if (detect_right == NO)
          current_state := STATE_ALIGN
          'move just a bit forward first
          'fudge := 100
          'repeat while (fudge > 0)
          '  set_speeds(WHEEL_SPEED_LEFT, WHEEL_SPEED_RIGHT)
           ' RBC.DebugStrCR(string("fudge"))
           ' fudge -= 1
            
        if (detect_front == YES)
          current_state := STATE_ORIENT
      
      STATE_ALIGN:
        edge_follow_right := YES
        if (detect_right)
          current_state := STATE_FOLLOW
        if (detect_front)
          current_state := STATE_ORIENT
      
      STATE_ORIENT:
        edge_follow_left := YES
        if (detect_front == NO)
          current_state := STATE_FOLLOW
    
    
PRI send_readings_to_pc
  
  out_packet[0] := sonar_reading / 256
  out_packet[1] := sonar_reading // 256
  out_packet[2] := ir_reading / 256
  out_packet[3] := ir_reading // 256
  
  RBC.SendDataToPC(@out_packet, 4, RBC#OUTPUT_TO_LOG)
 ' RBC.DebugLongCR(ir_reading)
  
  
  
PUB do_moving_as_needed
  case (current_state)
    STATE_FOLLOW:
      'move forward
      set_speeds(WHEEL_SPEED_LEFT, WHEEL_SPEED_RIGHT)
    STATE_ALIGN:
      'turn right
      set_speeds(WHEEL_SPEED_LEFT + 5, 2)
    STATE_ORIENT:
      'turn left
      set_speeds(-3, WHEEL_SPEED_RIGHT + 8)

PRI set_speeds(left, right)
  Servos.SetSpeeds(left, right)