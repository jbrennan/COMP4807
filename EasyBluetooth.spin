{{
  EasyBluetooth.spin

  This code contains functions for performing bluetooth communications from the robot's
  EasyBluetooth module to the PC's Bluetooth dongle.   
}}


CON
  _clkmode = xtal1 + pll16x     ' This is required for proper timing
  _xinfreq = 5_000_000          ' This is required for proper timing                           

  '!!!WARNING!!!    DO NOT CHANGE ANY OF THESE CONSTANTS 
  PIN_BlueRX = 4                ' input pin for receiving data into EasyBluetooth (i.e., receive from PC)
  PIN_BlueTX = 5                ' output pin for sending data over EasyBluetooth (i.e., sent to PC)
  BAUD_RATE = 9600              ' baud rate (untested at higher rate)
  BAUD_MODE = 1                 ' non-inverted bits
  DATA_BITS = 8                 ' 8-bit data

  
CON
  BUFFER_SIZE = 50              ' maximum number of bytes that can be received at any time

OBJ
  Serial:       "FullDuplexSerial"    
  
 
VAR
  byte  dataIn[BUFFER_SIZE+1]
  byte  connectionData[15]
  byte  tempString[3]

 
PUB Init | ptr, count, tempNum
  waitcnt(3_000_000 + cnt)
  Serial.start(PIN_BlueRX, PIN_BlueTX, 0, BAUD_RATE)
  Serial.rxflush               
               
  
PUB SendByte(ch) | t
  {{ Send a byte out the bluetooth. }}
  Serial.tx(ch)           

    
PUB SendBytes(byteArray, count)
  {{ Send count number of bytes out the bluetooth starting at byteArray[0]. }}

  repeat count
    SendByte(byte[byteArray++])

  
PUB GetByte: byteVal | x, br
  {{ Receive a byte from the bluetooth. }}
  byteVal := Serial.rx   

          
PUB GetBytes(byteArray, count) | numReceived
  {{ Receive a set of count bytes from the bluetooth to a maximum of BUFFER_SIZE. }}

  numReceived := 0
  'bytefill(@dataIn, 0, count+1)                            ' Fill string memory with 0's (null)
  dataIn[numReceived++] := GetByte                           ' get 1st byte
  'numReceived++                                            ' increment pointer
  repeat while (numReceived < count) AND (numReceived < BUFFER_SIZE)       ' repeat until count of BUFFER_SIZE reached 
      dataIn[numReceived++] := GetByte                      ' Store byte in array
      'numReceived++
  'dataIn[numReceived] := 0                                  ' set last character to null
'  byteMove(byteArray, @dataIn, numReceived + 1)             ' move into string pointer position
  byteMove(byteArray, @dataIn, numReceived)                  ' copy over into byte array