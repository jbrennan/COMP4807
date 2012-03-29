{{
  RBC.spin

  This is the spin object used to allow the robot to communicate with the computer
  running the RBC program using Bluetooth
}}


CON
  'These constants are required for proper timing.
  'They should appear at the top of every cog program you write.
  _clkmode   = xtal1 + pll16x
  _xinfreq   = 5_000_000                      

  
CON
  RBC_BEGIN_DATA = 2
  RBC_BEGIN_DEBUG_DATA = 3
   
 'These are the types of messages (modes) that can be sent to the PC
CON
  RBC_DEBUG_OUTPUT = 1
  RBC_DEBUG_OUTPUT_CR = 2
  RBC_DEBUG_FILE = 3
  RBC_DEBUG_FILE_CR = 4
  RBC_PLANNER_DATA = 5
  RBC_ENABLE_INPUT = 7
  RBC_DISABLE_INPUT = 8
  RBC_ROBOT_BEGIN = 11
  RBC_START_CAMERA = 6
  RBC_CONNECTION_REQUEST = 10
  RBC_CONN_ACK = 8
  RBC_DEBUG_CLEAR = 9
  RBC_IMAGE_TRACE = 10
  RBC_ROBOT_DATA = 11
  RBC_IMAGE_CLEAR = 12
  RBC_IMAGE_COLOR = 13 

CON
  'Flags for SendDataToPc
  OUTPUT_TO_LOG = 1
  OUTPUT_TO_FILE = 2
  OUTPUT_TO_LOG_AND_FILE = 3
  OUTPUT_TO_NONE = 4

VAR
  byte dataIn[256]

OBJ 
  Bluetooth:    "EasyBluetooth"                    ' allows bluetooth communication
  Conv:         "Numbers" 
  
PUB Init | aByte
  'Wait for the computer to request for a connection
  Bluetooth.Init
  'Read bytes until a request connection byte is received

  repeat
    aByte := Bluetooth.GetByte
  until (aByte == RBC_CONNECTION_REQUEST)
  Bluetooth.sendByte(RBC_CONN_ACK)
  'Wait for begin command  
  repeat
    aByte := Bluetooth.GetByte
  until (aByte == RBC_ROBOT_BEGIN)

'Enable the input bar on the console
PUB EnableDebugInput
  Bluetooth.sendByte(RBC_BEGIN_DATA)
  Bluetooth.sendByte(RBC_ENABLE_INPUT)

'Disable the input bar on the console
PUB DisableDebugInput
  Bluetooth.sendByte(RBC_BEGIN_DATA)
  Bluetooth.sendByte(RBC_DISABLE_INPUT)

'Send the color of the tracked image
PUB SendTrackedColorToPc(red, green, blue)
  Bluetooth.sendByte(RBC_BEGIN_DATA)
  Bluetooth.sendByte(RBC_IMAGE_COLOR)
  Bluetooth.sendByte(3)
  ' Add a constant offset to the colors
  red := red + 80
  green := green + 70
  blue := blue + 60
  if (red > 255)
    red := 255
  if (green > 255)
    green := 255
  if (blue > 255)
    blue := 255
  Bluetooth.sendByte(red//256)
  Bluetooth.sendByte(green//256)
  Bluetooth.sendByte(blue//256)

'Send Image track data
PUB SendTrackedDataToPc(x1, y1, x2, y2)
  Bluetooth.sendByte(RBC_BEGIN_DATA)
  Bluetooth.sendByte(RBC_IMAGE_TRACE)
  Bluetooth.sendByte(4)
  Bluetooth.sendByte(x1//256)
  Bluetooth.sendByte(y1//256)
  Bluetooth.sendByte(x2//256)
  Bluetooth.sendByte(y2//256) 

'Wait for and received debug data from the PC
PUB ReceiveDebugData(dataReceived) | aByte
  'Read bytes until the the Begin Debug Data signal is received
  repeat
    aByte := Bluetooth.GetByte
  until (aByte == RBC_BEGIN_DEBUG_DATA)
  
  
  'Now get the actual data
  aByte := Bluetooth.GetByte
  bytefill(@dataIn, 0, aByte+1)
  dataIn[0] := aByte
  Bluetooth.getBytes(@dataIn+1, aByte)  
  bytemove(dataReceived, @dataIn, aByte+1)

'Receive data from the PC
'returns an array of bytes with the first element the size of the input data
'so the size of the array would be the value of the first element + 1
PUB ReceiveData(dataReceived) | aByte
  'Read bytes until the the Begin Data signal is received
  repeat
    aByte := Bluetooth.GetByte
  until (aByte == RBC_BEGIN_DATA)
  
  'Now get the actual data
  aByte := Bluetooth.GetByte
  bytefill(@dataIn, 0, aByte+1)
  dataIn[0] := aByte
  Bluetooth.getBytes(@dataIn+1, aByte)  
  bytemove(dataReceived, @dataIn, aByte+1)

'Send data to planner on the PC
PUB SendDataToPc(outData, count, flag)
  SendData(outData, count, RBC_PLANNER_DATA, flag)

'Reset the output image on the pc
PUB SendResetImage
  Bluetooth.SendByte(RBC_BEGIN_DATA)
  Bluetooth.SendByte(RBC_IMAGE_CLEAR)

'Send to robot at a specific station
PUB SendDataToRobot(station, outData, count)
  'Send the data
  Bluetooth.SendByte(RBC_BEGIN_DATA)
  Bluetooth.SendByte(RBC_ROBOT_DATA)
  Bluetooth.SendByte((count+1)//256)
  Bluetooth.SendByte(station//256)
  Bluetooth.SendBytes(outData, count)

'Clears the debug screen
PUB DebugClear
  Bluetooth.SendByte(RBC_BEGIN_DATA)
  Bluetooth.SendByte(RBC_DEBUG_CLEAR)

'Output a new line on the output screen
PUB DebugCR
  Bluetooth.SendByte(RBC_BEGIN_DATA)
  Bluetooth.SendByte(RBC_DEBUG_OUTPUT_CR)
  Bluetooth.SendByte(0)
  Bluetooth.SendByte(0)

'Send a char to the PC to display on the debug output screen
PUB DebugChar(output)
  SendData(output, 1, RBC_DEBUG_OUTPUT, -1)

'Send a char to the PC to display on the debug output screen with a carrage return
PUB DebugCharCR(output)
  SendData(output, 1, RBC_DEBUG_OUTPUT_CR, -1)

'Send a string to the PC to display on the debug output screen
PUB DebugStr(output)
  SendData(output, StrSize(output), RBC_DEBUG_OUTPUT, 0)   

'Send a string with a carriage return to display on the debug output screen
PUB DebugStrCR(output)
  SendData(output, StrSize(output), RBC_DEBUG_OUTPUT_CR, 0)
                                              
'Send a long to the PC to display on the debug output screen
PUB DebugLong(toSend)
  DebugStr(Conv.toStr(toSend, CONV#DEC)) 

'Send a long with a carriage return to display on the debug output screen
PUB DebugLongCR(toSend)
  DebugStrCR(Conv.toStr(toSend, CONV#DEC))

'Common function to send Data
PRI SendData(output, count, command, flag)
  Bluetooth.SendByte(RBC_BEGIN_DATA)
  Bluetooth.SendByte(command)
  if (flag > 0)
    Bluetooth.SendByte(flag)
  Bluetooth.SendByte(count//256)
  if (flag > -1)
    Bluetooth.SendBytes(output, count)
  else
    Bluetooth.SendByte(output)