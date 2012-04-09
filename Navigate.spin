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
  RED = 185
  BLUE = 16
  GREEN = 16
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
  
  BLOCK_LEFT_SIDE = 55
  BLOCK_RIGHT_SIDE = 30

  ' byte indices
  ROTATION_INDEX = 1
  MOVE_INDEX = 2
  CONTROL_INDEX = 3
        
  ' Movement instructions
  MOVE_NONE = 0
  MOVE_SMALL = 1
        
  ' Rotation instructions
  ROTATE_NONE = 0
  ROTATE_SMALL = 1
  ROTATE_BACK_SMALL = 2
        
        
  ' Command instructions
  STAY_STILL = 0  ' We're still waiting for the team to finish... set a timer and then ask again
  ASK_AGAIN = 1   ' Transitioning states, just ask again on the next iteration through
  GO = 2          ' Some kind of movement command (move or rotate)
  SEEK_BLOCK = 3  ' The robot needs to go into seek mode and then pick up a block... collision detection too!
  DROP_BLOCK = 4  ' The robot just needs to drop the block
  ALL_DONE = 100  ' The robot has completed the tasks and should shut down.

   
  ' Status codes
  STATUS_COMMAND_REQUEST = 0
  STATUS_BLOCK_FOUND = 1
  
  TURN_COUNT_SMALL = 10
  TURN_SMALL = 5
  MOVE_SMALL_COUNT = 50
  STAY_STILL_COUNT = 100

  GOOD_CONFIDENCE = 20
  CAMERA_SENSITIVITY = 30


VAR
  long current_state, edge_follow_left, edge_follow_right
  long detect_front, detect_right

  byte dataIn[7]
  byte out_packet[6]
  long cur_x, cur_y, cur_a, estimation_counter, pulse_left, pulse_right, turning_count, forward_count, turning_left
  byte turn_val, move_val
  long collecting_data, sonar_reading, ir_reading
  long stay_still_counter

  long did_find_block

  long pc_move_command
  long pc_rotate_command
  long pc_control_command

OBJ
  RBC: "RBC"
  Beeper: "Beeper"
  Servos: "ServoControl2"
  IRSensors: "IR8SensorArray"
  Sonar: "PingSensor"
  Dirrs: "DirrsSensor"
  BlockSensor: "BlockSensor"
  Camera: "CMUCam"


PUB main

  'Beeper.Startup
  RBC.Init

  turning_count := 0
  forward_count := 0
  turning_left := NO
  did_find_block := NO
  

  turn_val := 0
  move_val := 0

'  RBC.ReceiveData(@dataIn)
 ' cur_x := dataIn[1]*256 + dataIn[2]
 ' cur_y := dataIn[3]*256 + dataIn[4]
 ' cur_a := dataIn[5]*256 + dataIn[6]

  Servos.Start(SERVO_STOPPED_LEFT - 5, SERVO_STOPPED_RIGHT, true, true, true, true)
  Servos.SetRightGripper(100)
  Servos.SetLeftGripper(200)
  Servos.SetHeadYaw(150)
 ' Servos.SetHeadPitch(125)

  Camera.Start
  Camera.SetTrackColor(RED, GREEN, BLUE, CAMERA_SENSITIVITY)
 
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
      
      next


    'move forward as needed
    if (forward_count > 0)
      forward_count := forward_count - 1
      nslog(string("in the moving loop"))
      do_move_forward
      next
    
    
    if (stay_still_counter > 0)
      stay_still_counter := stay_still_counter - 1
      nslog(string("staying still...."))
      next


    'wait and get data from the RBC
    nslog(string("waiting for planner data"))
    
    
    ' Stop the servos and then ask for a new command
    set_wheel_speeds(0, 0)
    ask_pc_for_instructions(did_find_block)
    
    ' Now process the response
    process_pc_instructions
    
    
    ' reset any control counters
    turning_count := 0
    forward_count := 0
    stay_still_counter := 0
    did_find_block := NO
    
    
    ' now deal with the instructions based on the control bit
    case (pc_control_command)
      
      STAY_STILL:
        stay_still_counter := STAY_STILL_COUNT
        next
      
      ASK_AGAIN:
        next
        
      GO:
        ' some kind of movement... need to check
        case (pc_rotate_command)
          ROTATE_NONE: 'do nothing important
          ROTATE_SMALL:
            turning_count := TURN_SMALL
            turning_left := NO
            next
          ROTATE_BACK_SMALL:
            turning_count := TURN_SMALL
            turning_left := YES
            next
        
        
        ' not rotation, so let's check movement then
        case (pc_move_command)
          MOVE_NONE:
            forward_count := 0
          MOVE_SMALL:
            forward_count := MOVE_SMALL_COUNT
            next 'ignoring the "REVERSE_BIG" command when we're all done?
        
        
      SEEK_BLOCK:
        do_seek_block ' enters its own run-loop where it doesn't break out until it's found a block and clasped the grippers
      DROP_BLOCK:
        do_drop_block ' enters its own run-loop where it moves and drops off a block and breaks out of it when it's done.
      ALL_DONE:
        set_wheel_speeds(0, 0)
        Beeper.Shutdown

    


PUB ask_pc_for_instructions (found_a_block)
  
  if (found_a_block == YES)
    out_packet[0] := STATUS_BLOCK_FOUND / 256
    out_packet[1] := STATUS_BLOCK_FOUND // 256
  else
    out_packet[0] := STATUS_COMMAND_REQUEST / 256
    out_packet[1] := STATUS_COMMAND_REQUEST // 256
  
  RBC.SendDataToPC(@out_packet, 6, RBC#OUTPUT_TO_LOG)


PUB process_pc_instructions
  
  RBC.ReceiveData(@dataIn)
  
  ' Does this need to be offset???
  
  pc_move_command := dataIn[MOVE_INDEX]
  pc_rotate_command := dataIn[ROTATION_INDEX]
  pc_control_command := dataIn[CONTROL_INDEX]
  


PRI do_seek_block | found_block, cam_x, temp_holder, temp_con

  Servos.SetHeadPitch(HEAD_TILT_DOWN)
  open_grippers
  found_block := NO
  cam_x := 0
  temp_holder := 99
  temp_con := 98
  
  nslog(string("GOING TO SEEK A BLOCK"))
  repeat until (found_block == YES)
    
    ' try looking for a block
    temp_holder := BlockSensor.Detect
    
    RBC.DebugLongCR(temp_holder)
    if (BlockSensor.Detect)
      close_grippers
      found_block := YES
      nslog(string("YESSSS"))
      quit
    
    nslog(string("gmmmmmm"))
    ' see if we can find where a block is using the camera
    Camera.TrackColor
    temp_con := Camera.GetConfidence
    nslog(string("confidence"))
    RBC.DebugLongCR(temp_con)
    
    if (temp_con > GOOD_CONFIDENCE)
      cam_x := Camera.GetCenterX
      nslog(string("SEE A BLOCK SOMEWHERE"))
      RBC.DebugLongCR(cam_x)
      
      
      if (cam_x > BLOCK_LEFT_SIDE)
        nslog(string("turn left"))
        do_left_turn
      elseif (cam_x < BLOCK_RIGHT_SIDE)
        nslog(string("to the right"))
        do_right_turn
      else
        do_slow_forward
    
    else
      ' don't see the block... keep moving forward. it might appear
      do_slow_forward
        

  set_wheel_speeds(0, 0)
  nslog(string("out of the loop"))
  close_grippers 
  Servos.SetHeadPitch(HEAD_TILT_MID)
  did_find_block := YES ' so this can be sent to the PC on the next iteration 


PRI do_drop_block | back_count
  nslog(string("dropping block and backing up"))
  ' drop the block, back up a bit, and then close the grippers
  open_grippers
  back_count := 15
  repeat until (back_count == 0)
    back_count := back_count - 1
    set_wheel_speeds(-8, -8)
  
  set_wheel_speeds(0, 0)
  close_grippers


PRI open_grippers
  Servos.SetLeftGripper(LEFT_GRIPPER_OPEN)
  Servos.SetRightGripper(RIGHT_GRIPPER_OPEN)
  

PRI close_grippers
  Servos.SetLeftGripper(LEFT_GRIPPER_CLOSED)
  Servos.SetRightGripper(RIGHT_GRIPPER_CLOSED)
'moving functions for the block seeking


PRI do_left_turn | i_turn_count
  
  
  i_turn_count := TURN_COUNT_SMALL + 6
  repeat until (i_turn_count == 0)
    i_turn_count := i_turn_count - 1
    turnLeft
    nslog(string("L"))
  
  ' reset the speeds so the robot isn't still turning :)
 ' set_wheel_speeds(0, 0)



PRI do_right_turn | i_turn_count

  i_turn_count := TURN_COUNT_SMALL + 6
  repeat until (i_turn_count == 0)
    i_turn_count := i_turn_count - 1
    turnRight
    nslog(string("R"))
  
  ' reset the speeds so the robot isn't still turning :)
  'set_wheel_speeds(0, 0)

PRI do_slow_forward | i_move_count
  
  i_move_count := 10
  repeat until (i_move_count == 0)
    set_wheel_speeds(8, 8)
    nslog(string("m"))
    i_move_count := i_move_count - 1
  
  ' stop him again
 ' set_wheel_speeds(0, 0)


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
    turnLeft
  else
    nslog(string("rght"))
    turnRight


PUB turnLeft
  'Servos.SetSpeeds(0, 17)
  set_wheel_speeds(-5, WHEEL_SPEED_RIGHT)
 
        
PUB turnRight
  'Servos.SetSpeeds(14, 0)
  set_wheel_speeds(WHEEL_SPEED_LEFT, -5)


PUB set_wheel_speeds(left_s, right_s)
  Servos.SetSpeeds(left_s, right_s)



  'now write them out


PRI nslog(str)
  RBC.DebugStrCr(str)