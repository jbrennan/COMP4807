{{
  Beeper.spin - This code contains functions for using the Beeper. 
}}


CON
  '!!!WARNING!!!    DO NOT CHANGE ANY OF THESE CONSTANTS
  _clkmode   = xtal1 + pll16x     ' This is required for proper timing
  _xinfreq   = 5_000_000          ' This is required for proper timing
  PIN_BEEPER = 15                 ' PIN connected to beeper                 


VAR
  long temp    


PUB Startup
  {{ Make a "Starting Up" sound. }}
  Beep(25, 3000)
  Beep(50, 3300)
  Beep(75, 3600)
  Beep(50, 3900)
  Beep(25, 4200)


PUB Shutdown
  {{ Make a "Shutting Down" sound. }}
  Beep(25, 4200)
  Beep(50, 3900)
  Beep(75, 3600)
  Beep(50, 3300)
  Beep(25, 3000)


PUB Ok
  {{ Make an "Ok" sound. }}
  Beep(100, 2500)
  Beep(100, 3000)


PUB Error
  {{ Make an "Error" sound. }}
  Beep(200, 4000)
  Beep(200, 3000)
  Beep(200, 2000)


PUB Beep(Duration, Frequency) 
  {{ Plays frequency (hz) for specified duration (mSec). }}
  
  ' Set the tone
  Update(Frequency)

  ' Pause for "Duration" milliseconds (400 = Minimum waitcnt value to prevent lock-up) 
  temp := Duration * (clkfreq / 1_000) -2300 #> 400
  waitcnt(temp + cnt)

  ' Stop the tone
  Update(0)


PRI Update(freq)
  { Updates the counter module and return the value of cnt at the start of the signal.}

  if freq == 0                                         ' freq = 0 turns off square wave
    waitpeq(0, |< PIN_BEEPER, 0)                       ' Wait for low signal
    dira[PIN_BEEPER]~ 
    ctra := 0                                          ' Set CTRA to 0
         
  temp := PIN_BEEPER                                   ' CTRA[8..0] := pin
  temp += (%00100 << 26)                               ' CTRA[30..26] := %00100
  ctra := temp                                         ' Copy temp to CTRA

  'Calculate the frequency
  repeat 33                                    
    frqa <<= 1
    if freq => clkfreq
      freq -= clkfreq
      frqa++        
    freq <<= 1
  
  phsa := 0                                            ' Clear PHSA (start cycle low)
  dira[PIN_BEEPER]~~                                   ' Make pin output
  result := cnt                                        ' Return the start time