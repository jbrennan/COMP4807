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

  ROTATE_NONE = 0
  ROTATE_SMALL = 1
  'ROTATE_BIG = 2
  ROTATE_BACK_SMALL = 2
  'ROTATE_BACK_BIG = 4
  ROTATE_FULL = 3
                
  MOVE_NONE = 0
  MOVE_SMALL = 1
  STOP = 88
  RECORD = 89
  GO = 0
                
  ROTATION_I = 1
  MOVE = 2
  CONTROL = 3

  TURN_SMALL = 20
  MOVE_SMALL_COUNT = 50

  INVALID_READING = 111

VAR
  long current_state, edge_follow_left, edge_follow_right
  long detect_front, detect_right

  byte dataIn[7]
  byte out_packet[6]
  long cur_x, cur_y, cur_a, estimation_counter, pulse_left, pulse_right, turning_count, forward_count, turning_left
  byte turn_val, move_val
  long collecting_data, sonar_reading, ir_reading

OBJ
  RBC: "RBC"
  Beeper: "Beeper"
  Servos: "ServoControl2"
  IRSensors: "IR8SensorArray"
  Sonar: "PingSensor"
  Dirrs: "DirrsSensor"


PUB main

  'Beeper.Startup
  RBC.Init

  turning_count := 0
  forward_count := 0
  turning_left := NO
  collecting_data := NO

  turn_val := 0
  move_val := 0

'  RBC.ReceiveData(@dataIn)
 ' cur_x := dataIn[1]*256 + dataIn[2]
 ' cur_y := dataIn[3]*256 + dataIn[4]
 ' cur_a := dataIn[5]*256 + dataIn[6]

  Servos.Start(SERVO_STOPPED_LEFT, SERVO_STOPPED_RIGHT, true, true, true, false)
  Servos.SetRightGripper(100)
  Servos.SetLeftGripper(200)
 ' Servos.SetHeadPitch(125)
  Beeper.OK

  current_state := STATE_FOLLOW
  nslog(string("about to move"))
  repeat 'main loop

    

    'see if we're already in a turn, in which case we should turn!
    if (turning_count > 0)
      turning_count := turning_count - 1
      RBC.DebugLong(turning_count)
      nslog(string(" :in the turning count loop"))
      do_turn

      if (collecting_data == YES)
        'collect some data!
        do_collect_data
        if (turning_count < 1)
          collecting_data := NO
          nslog(string("DONE COLLECTING DATA!!!!!!!!!!!!"))
        send_data_to_pc
      
      next


    'move forward as needed
    if (forward_count > 0)
      forward_count := forward_count - 1
      nslog(string("in the moving loop"))
      do_move_forward
      next


    'wait and get data from the RBC
    nslog(string("waiting for planner data"))
    RBC.ReceiveData(@dataIn)
    nslog(string("got data from planner"))

    'if we're done, make sure we stop!!!
    if (dataIn[CONTROL] == STOP)
      set_wheel_speeds(0, 0)
      Beeper.Shutdown
      return
    
    'else keep going
    turn_val := dataIn[ROTATION_I]
    RBC.DebugLongCR(turn_val)
    
    collecting_data := NO 'reset this value
    
    case (turn_val)
      ROTATE_NONE:
        turning_count := 0
      ROTATE_SMALL:
        turning_count := TURN_SMALL
        turning_left := NO
        next
      ROTATE_BACK_SMALL:
        turning_count := TURN_SMALL
        turning_left := YES
        next
      ROTATE_FULL:
        turning_count := 6 * TURN_SMALL
        turning_left := NO
        collecting_data := YES
        nslog(string("WILL ROTATE FULL!!!!!!!!!!"))
        next


    move_val := dataIn[MOVE]
    case (move_val)
      MOVE_NONE:
        forward_count := 0
      MOVE_SMALL:
        forward_count := MOVE_SMALL_COUNT

    

PUB do_collect_data
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

PUB send_data_to_pc
  out_packet[0] := sonar_reading / 256
  out_packet[1] := sonar_reading // 256
  out_packet[2] := ir_reading / 256
  out_packet[3] := ir_reading // 256
  out_packet[4] := collecting_data / 256
  out_packet[5] := collecting_data // 256
  
  RBC.SendDataToPC(@out_packet, 6, RBC#OUTPUT_TO_LOG)



PUB do_moving_as_needed
  case (current_state)
    STATE_FOLLOW:
      'move forward
      set_speeds(WHEEL_SPEED_LEFT, WHEEL_SPEED_RIGHT)
    STATE_ALIGN:
      'turn right
      set_speeds(WHEEL_SPEED_LEFT + 5, -5)
    STATE_ORIENT:
      'turn left
      set_speeds(-3, WHEEL_SPEED_RIGHT + 8)


PUB do_move_forward
  set_wheel_speeds(WHEEL_SPEED_LEFT, WHEEL_SPEED_RIGHT)


PRI set_speeds(left, right)
  Servos.SetSpeeds(left, right)


PRI do_turn
  if (turning_left)
    nslog(string("lft"))
    turnLeft(0)
  else
    nslog(string("rght"))
    turnRight(0)


PUB turnLeft(stepAmount)
  'Servos.SetSpeeds(0, 17)
  set_wheel_speeds(-5, WHEEL_SPEED_RIGHT)
 
        
PUB turnRight(stepAmount)
  'Servos.SetSpeeds(14, 0)
  set_wheel_speeds(WHEEL_SPEED_LEFT, -5)


PUB set_wheel_speeds(left_s, right_s)
  Servos.SetSpeeds(left_s, right_s)



  'now write them out


PRI nslog(str)
  RBC.DebugStrCr(str)