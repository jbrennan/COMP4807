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
       

VAR
        long turningLeft
        long turningRight
        
        long turningCount

        long movingBackwards
        long backwardsCount

        long moveForwards
        long moveBackwards
        long will_turnLeft
        long will_turnRight

        long left, center, right

        long random_lr
        
        long arb_wander_left, arb_wander_right, arb_wander_difference
        long arb_avoid_left, arb_avoid_right
        long speed_left, speed_right

        long move_left_total, move_right_total
        long wandering_count

OBJ
        RBC: "RBC"
        Beeper: "Beeper"
        Servos: "ServoControl2"
        IRSensors: "IR8SensorArray"

PUB MAIN

  Beeper.Startup
  RBC.Init
  Servos.Start(SERVO_STOPPED_LEFT, SERVO_STOPPED_RIGHT, true, true, true, false)
  Servos.SetRightGripper(100)
  Servos.SetLeftGripper(190)
  Servos.SetHeadPitch(125)
  Beeper.OK
  
  
  arb_wander_left := NO
  arb_wander_right := NO
  arb_wander_difference := 0
  arb_avoid_left := 0
  arb_avoid_right := 0

  move_left_total := 0
  move_right_total := 0
  
  speed_left := 0
  speed_right := 0
  
  turningLeft := NO
  turningRight := NO
  turningCount := 0
  backwardsCount := 0
  wandering_count := 0
  random_lr := cnt     
  
  'the main runloop
  repeat

    ' see if we're already moving backwards first, in which case move backwards
    ' then make sure we turn!
    if (backwardsCount > 0)
      backwardsCount := backwardsCount - 1
      doMoveBackwards
      if (backwardsCount == 0)
        random_lr := random_lr?
        
        if (random_lr // 2 == 0)
          turningLeft := YES
          turningRight := NO
        else
          turningLeft := NO
          turningRight := YES
        turningCount := 2 * TURNING_AMOUNT
        nslog(string("done moving backwards... will turn next"))
      else
        next

             
    'see if we're already in a turn, in which case we should turn!
    if (turningCount > 0)
      if (turningCount == (2 * TURNING_AMOUNT))
        nslog(string("backwards -> turning"))
        if (turningLeft)
          nslog(string("turning left!"))
      turningCount := turningCount - 1
      'nslog(string("in the turning count loop"))
      doTurn
      next
                        
                
    moveForwards := YES
    moveBackwards := NO
    will_turnLeft := NO
    will_turnRight := NO
                
    left := NO
    center := NO
    right := NO

    do_collision_detect
    do_wander_decide
    do_arbitration
                
    'do the capture for the sensors first, then get the readings
    'IRSensors.capture
                
    'if (IRSensors.Detect(SENSOR_FRONT_LEFT))
     ' left := YES
      'Beeper.beep(BEEP_SHORT, BEEP_TONE_LEFT)
                        
    'if (IRSensors.Detect(SENSOR_FRONT_CENTER))
    '  center := YES
      'Beeper.beep(BEEP_SHORT, BEEP_TONE_CENTER)
                        
    'if (IRSensors.Detect(SENSOR_FRONT_RIGHT))
     ' right := YES
      'Beeper.beep(BEEP_SHORT, BEEP_TONE_RIGHT)
                
                
                'determine where to move
    'if (left AND center AND right)
     ' moveBackwards := YES
     ' moveForwards := NO
     ' turningLeft := NO
     ' turningRight := NO
      'Beeper.beep(BEEP_SHORT, 500)
      'Beeper.beep(2*BEEP_SHORT, 350)
'    elseif (left)
'      moveForwards := NO
'      moveBackwards := NO
'      turningLeft := NO
'      turningRight := YES
'    elseif (right)
'      moveForwards := NO
'      moveBackwards := NO
'      turningLeft := YES
'      turningRight := NO
'    elseif (center)
'      moveForwards := NO
'      moveBackwards := NO             
'                        'chosen by a random flip of a coin!
'      turningLeft := YES
'      turningRight := NO
'                        
'    else
'                        ' all clear, move forward!
'      moveForwards := YES
'      moveBackwards := NO
'      turningLeft := NO
'      turningRight := NO
'                
'    if (turningLeft OR turningRight)
'      turningCount := TURNING_AMOUNT
'      next
'
'    'check to see if we can move backwards or forwards
'    if (moveForwards)
'      doMoveForwards
'    elseif (moveBackwards)
'      nslog(string("should move backwards"))
'      backwardsCount := BACKWARDS_AMOUNT
'      next
                
                
                
        
        
 ' Beeper.Shutdown
  'do backing up/turning if needed
  'do collision detection
  'do wandering deciding
  'do arbitration
  '  determine if the robot needs to go into an avoidance left/right direction
  '  or if it needs to reverse and turn around
  '  If neither of those, then it's free to wander
  '  When setting wheel speeds, they shouldn't be set unless they have a different value
  '  unless the robot actually decides to wander in a new direction, his speeds shouldn't change

PUB do_arbitration

  if (arb_avoid_left AND arb_avoid_right)
    'need to reverse!
    nslog(string("should move backwards"))
    backwardsCount := BACKWARDS_AMOUNT

    
  move_left_total := ((arb_wander_left * PRIORITY_WANDER) + (arb_avoid_right * PRIORITY_COLLISION_AVOID))
  move_right_total := ((arb_wander_right * PRIORITY_WANDER) + (arb_avoid_left * PRIORITY_COLLISION_AVOID))
  
  
  if (move_left_total > move_right_total)
    'turn on LEFT motor i.e. TURN-RIGHT

    'now determine if we're wandering or avoiding
    if (arb_avoid_right)
      turningCount := TURNING_AMOUNT
      turningLeft := YES
      turningRight := NO

    else
      'we're not actually turning, so much as we are setting the wheel speeds to be different
      if (wandering_count > 0)
        'already wandering
      else
        wandering_count := WANDERING_AMOUNT
        'set the speed so it's wandering left
        set_wheel_speeds(15, 15 + arb_wander_difference)
      
    
  elseif (move_left_total < move_right_total)
    'turn on RIGHT motor

    if (arb_avoid_left)
      turningCount := TURNING_AMOUNT
      turningLeft := NO
      turningRight := YES
    else
       'we're not actually turning, so much as we are setting the wheel speeds to be different
      if (wandering_count > 0)
        'already wandering
      else
        wandering_count := WANDERING_AMOUNT
        'set the speed so it's wandering left
        set_wheel_speeds(15 + arb_wander_difference, 15)
    
  else
    ' turn on both motors
      'stay the course


PUB do_wander_decide
  
  if (wandering_count > 0)
    wandering_count := wandering_count - 1
    if (wandering_count == 0)
      arb_wander_left := NO
      arb_wander_right := NO
    return
  arb_wander_left := NO
  arb_wander_right := NO
  'nslog(string("will wander?"))
  if ((?random_lr) // 15 == 0)
    'only start wandering 1/20 of the time.... this will happen way too fast though!
    nslog(string("going to wander!"))
    if ((?random_lr) // 2 == 0)
      'go left
      arb_wander_left := YES
    else
      arb_wander_right := YES
    
    arb_wander_difference := (?random_lr) // 7 'the wandering wheel will go 0-6 FASTER than the other wheel.
  else
    'not going to wander
    arb_wander_left := NO
    arb_wander_right := NO


PUB do_collision_detect
  
  arb_avoid_left := NO
  arb_avoid_right := NO
  
  IRSensors.capture
                
  if (IRSensors.Detect(SENSOR_FRONT_LEFT))
    left := YES
    arb_avoid_left := YES
    'Beeper.beep(BEEP_SHORT, BEEP_TONE_LEFT)
                        
  if (IRSensors.Detect(SENSOR_FRONT_CENTER))
    center := YES
    'Beeper.beep(BEEP_SHORT, BEEP_TONE_CENTER)
                      
  if (IRSensors.Detect(SENSOR_FRONT_RIGHT))
    right := YES
    arb_avoid_right := YES
    'Beeper.beep(BEEP_SHORT, BEEP_TONE_RIGHT)


PUB set_wheel_speeds (new_left_speed, new_right_speed)
  
  'only actually change the speeds if the value is different
  if (new_left_speed <> speed_left)
    speed_left := new_left_speed
    Servos.SetLeftSpeed(new_left_speed)
  if (new_right_speed <> speed_right)
    speed_right := new_right_speed
    Servos.SetRightSpeed(new_right_speed)

PUB doMoveForwards

  'Servos.setSpeeds(10, 15)
  set_wheel_speeds(15, 15)

PUB doMoveBackwards
  'Servos.setSpeeds(-13, -13)
  set_wheel_speeds(-13, -13)
  nslog (string("do moving backwards"))
  'Beeper.beep(BEEP_SHORT, 900)
  'Beeper.beep(4*BEEP_SHORT, 350)
  'Servos.setSpeeds(-15, -15)

PUB doTurn
  if (turningLeft)
    nslog(string("lft"))
    turnLeft(0)
  else
    nslog(string("rght"))
    turnRight(0)

PUB turnLeft(stepAmount)
  'Servos.SetSpeeds(0, 17)
  set_wheel_speeds(0, 17)
        
PUB turnRight(stepAmount)
  'Servos.SetSpeeds(14, 0)
  set_wheel_speeds(14, 0)
        
        
        
PRI nslog(str) 'oh yeah, like it's a cocoa app....I can pretend, can't I?
  RBC.DebugStrCr(str)
         