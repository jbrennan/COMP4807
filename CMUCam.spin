{{
  CMUCam.spin

  This program contains code for using the CMUCam.   The code makes use of the
  FullDuplexSerial.spin object which requires an additional Cog.   For more
  details on the use of this camera, see the CMUCam documentation.  The camera
  is powered by a single switch (#5 on the top-level black dip switch labeled
  "CAM").  Make sure that this is on (i.e., down) in order to use the sensor.
  If the sensor is not needed, you SHOULD turn off the power switch to preserve
  battery power since the CMUCam uses up a LOT of current from the battery.    
}}


CON  
  '!!!WARNING!!!    DO NOT CHANGE ANY OF THESE CONSTANTS
  PIN_CAMERA_RX = 13               ' PIN that receives from the CMUCam
  PIN_CAMERA_TX = 14               ' PIN that sends commands to the CMUCam
  BAUD_RATE = 115_200             ' baud rate
  BAUD_MODE = 1                   ' non-inverted bits
  DATA_BITS = 8                   ' 8-bit data


VAR
  byte  red, green, blue                                      ' current r/g/b values from last ReadColor call
  byte  RMin, RMax, GMin, GMax, BMin, BMax                    ' min/max RGB values allowed for tracking
  byte  RTrack, GTrack, BTrack                                ' color currently being tracked                        
  byte  midX, midY, xTL, yTL, xBR, yBR, pixels, confidence    ' the data returned from a single track

  
OBJ
  SER:          "FullDuplexSerial"
  CONV:         "Numbers"
  

PUB Start
  {{ Initialize the CMUCam and set it into Poll Mode. }}
  
  waitcnt(cnt + 1_000_000)             ' Wait for CMUCam to be ready
       
  SER.Start(PIN_CAMERA_RX, PIN_CAMERA_TX, 0, BAUD_RATE)
  CONV.Init

  ' Put the camera into Poll mode
  SER.str(string("PM 1", 13))  
  GetAck

  ' Set the camera to Raw Mode 3 ... camera responds
  ' with a packet that starts with byte 255.
  SER.str(string("RM 3", 13))                           
  GetAck

  ' White Balance Smart Mode
  SER.str(string("CR 32 8", 13))
  GetAck

  ' Set to Full Window Mode
  SetFullWindow
  
PUB SetFullWindow
  {{ Set the camera's image window to full size (i.e., 80x143). }}

  SER.str(string("SW", 13))                      
  GetAck


PUB SetConstrainedWindow(X_UpperLeft, Y_UpperLeft, X_BottomRight, Y_BottomRight)
  {{ Set the portion of the camera's image that you want to process.
     Maximum values are 1, 1, 80, 143. }}

  SER.str(string("SW "))                         
  SER.str(CONV.ToStr(X_UpperLeft, CONV#DEC))
  SER.tx(32)          
  SER.str(CONV.ToStr(Y_UpperLeft, CONV#DEC))
  SER.tx(32)          
  SER.str(CONV.ToStr(X_BottomRight, CONV#DEC))
  SER.tx(32)          
  SER.str(CONV.ToStr(Y_BottomRight, CONV#DEC))
  SER.tx(13)          
  GetAck

  
PUB ReadColor
  {{ Read the mean color value in terms of red, green and blue components.
     Use GetRed, GetGreen and GetBlue to extract the values. }}
  
  SER.str(string("GM"))
  SER.tx(13)          
  GetPacketStart
  SER.rxtime(100)  'ignore the "S" character in the reply (i.e., 83)
                             
  'Get the RGB values and ignore the deviation values   
  red := SER.rxtime(100)           ' Get & store the Red amount                                  
  green := SER.rxtime(100)         ' Get & store the Green amount                                 
  blue := SER.rxtime(100)          ' Get & store the Blue amount                                 
  SER.rxtime(100)                  ' Get & ignore the Red deviation                                 
  SER.rxtime(100)                  ' Get & ignore the Green deviation                                  
  SER.rxtime(100)                  ' Get & ignore the Blue deviation 
  waitcnt(cnt + 1_000)             ' Short delay, required for quick successive calls


PUB GetRed
  {{ Get the red value from the last call to ReadColor. }}
  return red

  
PUB GetGreen
  {{ Get the green value from the last call to ReadColor. }}
  return green

  
PUB GetBlue
  {{ Get the blue value from the last call to ReadColor. }}
  return blue


PUB SetTrackColor(r, g, b, sensitivity)
  {{ Set the color to be tracked currently. The sensitivity is the allowable +- range
     for each color component during tracking.  Call this before calling TrackColor.}}
  
  RTrack := r
  GTrack := g
  BTrack := b

  'Adjust the max/min ranges for each rgb component
  RMin := GMin := BMin := 16
  RMax := GMax := BMax := 240

  if (Rtrack => (RMin + sensitivity))
    RMin := Rtrack - sensitivity
  if (Gtrack => (GMin + sensitivity))
    GMin := Gtrack - sensitivity
  if (Btrack => (BMin + sensitivity))
    BMin := Btrack - sensitivity
  if (Rtrack =< (RMax - sensitivity))
    RMax := Rtrack + sensitivity
  if (Gtrack =< (GMax - sensitivity))
    GMax := Gtrack + sensitivity
  if (Btrack =< (BMax - sensitivity))
    BMax := Btrack + sensitivity

  'Do a single track so that the color to be tracked is stored in the camera
  SER.str(string("TC"))
  SER.str(CONV.ToStr(RMin, CONV#DEC))
  SER.tx(32)          
  SER.str(CONV.ToStr(RMax, CONV#DEC))
  SER.tx(32)          
  SER.str(CONV.ToStr(GMin, CONV#DEC))
  SER.tx(32)          
  SER.str(CONV.ToStr(GMax, CONV#DEC))
  SER.tx(32)          
  SER.str(CONV.ToStr(BMin, CONV#DEC))
  SER.tx(32)          
  SER.str(CONV.ToStr(BMax, CONV#DEC))
  SER.tx(13)          
  GetPacketStart
  GetTrackedData                ' read and discard this tracked data                           
  

PUB TrackColor
  {{ Track the color previously specified by the call to SetTrackColor. }}

  SER.str(string("TC"))
  SER.tx(13)          
  GetPacketStart
  GetTrackedData                                      


PUB GetCenterX
  {{ Return the x component of the blob's center of mass.  Call only after TrackColor. }}
  return midX
    
PUB GetCenterY
  {{ Return the y component of the blob's center of mass.  Call only after TrackColor. }}
  return midY
    
PUB GetTopLeftX
  {{ Return the x component of the blob bounding box's top left corner.   Call only after TrackColor. }}
  return xTL
    
PUB GetTopLeftY
  {{ Return the y component of the blob bounding box's top left corner.   Call only after TrackColor. }}
  return yTL
    
PUB GetBottomRightX
  {{ Return the x component of the blob bounding box's bottom right corner.   Call only after TrackColor. }}
  return xBR
    
PUB GetBottomRightY
  {{ Return the x component of the blob bounding box's bottom right corner.   Call only after TrackColor. }}
  return yBR
    
PUB GetPixels
  {{ Return the number of pixels in the tracked blob.  The actual value should be (pixels+4)/8.  Call only after TrackColor. }}
  return pixels
    
PUB GetConfidence
  {{ Return the confidence level of the track (i.e., 0 to 255 where 8 is poor & 50 is very good).  Call only after TrackColor. }}
  return confidence
  
    
PRI GetTrackedData
  { Get the data from the last track. }
  'Get the M-type packet back from the camera with the data in it
  SER.rxtime(100)  'ignore the "M" character in the reply (i.e., 77)
  midX := SER.rxtime(100)         ' Get x center of mass of the color blob                                  
  midY := SER.rxtime(100)         ' Get y center of mass of the color blob                                 
  xTL := SER.rxtime(100)          ' Get top left x of the blob's bounding box                               
  yTL := SER.rxtime(100)          ' Get top left y of the blob's bounding box                                
  xBR := SER.rxtime(100)          ' Get bottom right x of the blob's bounding box                                
  yBR := SER.rxtime(100)          ' Get bottom right y of the blob's bounding box                                
  pixels := SER.rxtime(100)       ' Get number of pixels in the traced blob                                 
  confidence := SER.rxtime(100)   ' Get confidence level of track                               
  SER.rxtime(100)                                       
  

PRI GetAck | in
  { Wait for the acknowledgement character 58. }
  repeat
    in := SER.rxtime(1000)
  until (in == 58)

PRI GetPacketStart | in
  { Wait for the packet start character 255. }
  repeat
    in := SER.rxtime(10000)
  until (in == 255)
  