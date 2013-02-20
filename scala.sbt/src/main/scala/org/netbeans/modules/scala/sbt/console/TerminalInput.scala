package org.netbeans.modules.scala.sbt.console

import java.awt.event.KeyEvent
import java.util.Properties

abstract class TerminalInput {
  import TerminalInput._
  
  /** in vms mode, set by Terminal.VMS property */
  private var vms = false

  var vt52mode = false
  var keypadmode = false /* false - numeric, true - application */
  var output8bit = false
  
  var capslock = false
  var numlock = false
  
  var mouserpt = 0
  var mousebut = 0.toByte

  /* top row of numpad */
  private val PF1 = "\u001bOP"
  private val PF2 = "\u001bOQ"
  private val PF3 = "\u001bOR"
  private val PF4 = "\u001bOS"
  
  /* some more VT100 keys */
  private val Find = "\u001b[1~"
  private val Select = "\u001b[4~"
  private val Help = "\u001b[28~"
  private val Do = "\u001b[29~"

  private val Numpad = Array(
    "\u001bOp",
    "\u001bOq",
    "\u001bOr",
    "\u001bOs",
    "\u001bOt",
    "\u001bOu",
    "\u001bOv",
    "\u001bOw",
    "\u001bOx",
    "\u001bOy"
  )
  
  private val FunctionKey = Array[String](
    "",
    PF1,
    PF2,
    PF3,
    PF4,
    /* following are defined differently for vt220 / vt132 ... */
    "\u001b[15~",
    "\u001b[17~",
    "\u001b[18~",
    "\u001b[19~",
    "\u001b[20~",
    "\u001b[21~",
    "\u001b[23~",
    "\u001b[24~",
    "\u001b[25~",
    "\u001b[26~",
    Help,
    Do,
    "\u001b[31~",
    "\u001b[32~",
    "\u001b[33~",
    "\u001b[34~"
  )

  private val FunctionKeyShift = Array.fill[String](21)("")
  private val FunctionKeyAlt = Array.fill[String](21)("")
  private val FunctionKeyCtrl = Array.fill[String](21)("")
  FunctionKeyShift(15) = Find
  FunctionKeyShift(16) = Select

  /* the 3x2 keyblock on PC keyboards */
  private val Insert  = Array("\u001b[2~", "\u001b[2~", "\u001b[2~", "\u001b[2~")
  private val Remove  = Array("\u001b[3~", "\u001b[3~", "\u001b[3~", "\u001b[3~")
  private val KeyHome = Array("\u001b[H",  "\u001b[H",  "\u001b[H",  "\u001b[H")
  private val KeyEnd  = Array("\u001b[F",  "\u001b[F",  "\u001b[F",  "\u001b[F")
  private val NextScn = Array("\u001b[6~", "\u001b[6~", "\u001b[6~", "\u001b[6~")
  private val PrevScn = Array("\u001b[5~", "\u001b[5~", "\u001b[5~", "\u001b[5~")
  private val Escape  = Array("\u001b",    "\u001b",    "\u001b",    "\u001b")
  private val BackSpace = if (vms) 
    Array(
      "\u007f",       //  VMS other is delete
      "" + 10.toChar, //  VMS shift deletes word back
      "\u0018",       //  VMS control deletes line back
      "\u007f"        //  VMS other is delete
    ) 
  else Array("\b", "\b", "\b", "\b")

  
  private val KeyUp = Array.ofDim[String](4)
  private val KeyDown = Array.ofDim[String](4)
  private val KeyRight = Array.ofDim[String](4)
  private val KeyLeft = Array.ofDim[String](4)
  KeyUp(0)    = "\u001b[A"
  KeyDown(0)  = "\u001b[B"
  KeyRight(0) = "\u001b[C"
  KeyLeft(0)  = "\u001b[D"

  
  private val TabKey = Array[String](
    "\u0009",
    "\u001bOP\u0009",
    "",
    ""
  )
  
  private val NUMPlus = Array.ofDim[String](4)
  private val NUMDot = Array.ofDim[String](4)
  NUMPlus(0) = "+"
  NUMDot(0)  = "."

  private val KPMinus  = PF4
  private val KPComma  = "\u001bOl"
  private val KPPeriod = "\u001bOn"
  private val KPEnter  = "\u001bOM"

  
  /**
   * Direct access to writing data ...
   * @param b
   */
  def send(b: Array[Byte])

  /**
   * Terminal is mouse-aware and requires (x,y) coordinates of
   * on the terminal (character coordinates) and the button clicked.
   * @param x
   * @param y
   * @param modifiers
   */
  def mousePressed(x: Int, y: Int, modifiers: Int) {
    if (mouserpt == 0)
      return

    val mods = modifiers
    mousebut = if ((mods & 16) == 16) 0
    else if ((mods & 8) == 8) 1
    else if ((mods & 4) == 4) 2  
    else 3

    val mousecode = if (mouserpt == 9) {	/* X10 Mouse */
      0x20 | mousebut
    }  else {		/* normal xterm mouse reporting */
      mousebut | 0x20 | ((mods & 7) << 2)
    }

    val b = Array[Byte](
      27,
      '[',
      'M',
      mousecode.toByte,
      (0x20 + x + 1).toByte,
      (0x20 + y + 1).toByte
    )

    send(b) // FIXME: writeSpecial here
  }

  /**
   * Terminal is mouse-aware and requires the coordinates and button
   * of the release.
   * @param x
   * @param y
   * @param modifiers
   */
  def mouseReleased(x: Int, y: Int, modifiers: Int) {
    if (mouserpt == 0)
      return

    /* problem is tht modifiers still have the released button set in them.
     Int mods = modifiers
     mousebut = 3
     if ((mods & 16)==16) mousebut=0
     if ((mods &  8)==8 ) mousebut=1
     if ((mods &  4)==4 ) mousebut=2
     */

    val mousecode = if (mouserpt == 9) {
      0x20 + mousebut	/* same as press? appears so. */
    } else {
      '#'
    }

    val b = Array[Byte](
      27,
      '[',
      'M',
      mousecode.toByte,
      (0x20 + x + 1).toByte,
      (0x20 + y + 1).toByte
    )
    
    send(b) // FIXME: writeSpecial here
    mousebut = 0
  }

  
  /**
   * Override the standard key codes used by the terminal emulation.
   * @param codes a properties object containing key code definitions
   */
  def setKeyCodes(codes: Properties) {
    var i = 0
    while (i < 10) {
      codes.getProperty("NUMPAD" + i) match {case null =>; case x => Numpad(i) = unEscape(x)}
      i += 1
    }
    i = 1
    while (i < 20) {
      codes.getProperty("F"  + i) match {case null =>; case x => FunctionKey(i) = unEscape(x)}
      codes.getProperty("SF" + i) match {case null =>; case x => FunctionKeyShift(i) = unEscape(x)}
      codes.getProperty("CF" + i) match {case null =>; case x => FunctionKeyCtrl(i) = unEscape(x)}
      codes.getProperty("AF" + i) match {case null =>; case x => FunctionKeyAlt(i) = unEscape(x)}
      i += 1
    }
    val prefixes = Array("", "S", "C", "A")
    i = 0
    while (i < 4) {
      codes.getProperty(prefixes(i) + "PGUP") match {case null =>; case x => PrevScn(i) = unEscape(x)}
      codes.getProperty(prefixes(i) + "PGDOWN") match {case null =>; case x => NextScn(i) = unEscape(x)}
      codes.getProperty(prefixes(i) + "END") match {case null =>; case x => KeyEnd(i) = unEscape(x)}
      codes.getProperty(prefixes(i) + "HOME") match {case null =>; case x => KeyHome(i) = unEscape(x)}
      codes.getProperty(prefixes(i) + "INSERT") match {case null =>; case x => Insert(i) = unEscape(x)}
      codes.getProperty(prefixes(i) + "REMOVE") match {case null =>; case x => Remove(i) = unEscape(x)}
      codes.getProperty(prefixes(i) + "UP") match {case null =>; case x => KeyUp(i) = unEscape(x)}
      codes.getProperty(prefixes(i) + "DOWN") match {case null =>; case x => KeyDown(i) = unEscape(x)}
      codes.getProperty(prefixes(i) + "LEFT") match {case null =>; case x => KeyLeft(i) = unEscape(x)}
      codes.getProperty(prefixes(i) + "RIGHT") match {case null =>; case x => KeyRight(i) = unEscape(x)}
      codes.getProperty(prefixes(i) + "ESCAPE") match {case null =>; case x => Escape(i) = unEscape(x)}
      codes.getProperty(prefixes(i) + "BACKSPACE") match {case null =>; case x => BackSpace(i) = unEscape(x)}
      codes.getProperty(prefixes(i) + "TAB") match {case null =>; case x => TabKey(i) = unEscape(x)}
      codes.getProperty(prefixes(i) + "NUMPLUS") match {case null =>; case x => NUMPlus(i) = unEscape(x)}
      codes.getProperty(prefixes(i) + "NUMDECIMAL") match {case null =>; case x => NUMDot(i) = unEscape(x)}
      i += 1
    }
  }

  
  /**
   * A small conveniance method thar converts the string to a byte array
   * for sending.
   * @param s the string to be sent
   */
  private def send(s: String): Boolean = {
    if (s == null) // aka the empty string.
      return true
    /* NOTE: getBytes() honours some locale, it *CONVERTS* the string.
     * However, we output only 7bit stuff towards the target, and *some*
     * 8 bit control codes. We must not mess up the latter, so we do hand
     * by hand copy.
     */

    val arr = Array.ofDim[Byte](s.length)
    var i = 0
    while (i < s.length) {
      arr(i) = s.charAt(i).toByte
      i += 1
    }
    send(arr)

    true
  }

  protected def sendTelnetCommand(cmd: Byte) {
  }

  // ===================================================================
  // the actual terminal emulation code comes here:
  // ===================================================================

  /**
   * A small conveniance method thar converts a 7bit string to the 8bit
   * version depending on VT52/Output8Bit mode.
   *
   * @param s the string to be sent
   */
  private def sendSpecial(_s: String): Boolean = {
    var s = _s
    if (s == null) {
      return true
    }
    
    if (s.length >= 3 && s.charAt(0) == 27 && s.charAt(1) == 'O') {
      if (vt52mode) {
        if (s.charAt(2) >= 'P' && s.charAt(2) <= 'S') {
          s = "\u001b" + s.substring(2)  /* ESC x */
        } else {
          s = "\u001b?" + s.substring(2) /* ESC ? x */
        }
      } else {
        if (output8bit) {
          s = "\u008f" + s.substring(2)  /* SS3 x */
        } // else keep string as it is 
      }
    }
    
    if (s.length >= 3 && s.charAt(0) == 27 && s.charAt(1) == '[') {
      if (output8bit) {
        s = "\u009b" + s.substring(2) /* CSI ... */
      } // else keep 
    }
    
    send(s)
  }

  /**
   * main keytyping event handler...
   */
  def keyPressed(keyCode: Int, keyChar: Char, modifiers: Int) {
    val control = (modifiers & KEY_CONTROL) != 0
    val shift = (modifiers & KEY_SHIFT) != 0
    val alt = (modifiers & KEY_ALT) != 0

    var xind = 0
    var fmap = FunctionKey
    if (shift) {
      fmap = FunctionKeyShift
      xind = 1
    }
    if (control) {
      fmap = FunctionKeyCtrl
      xind = 2
    }
    if (alt) {
      fmap = FunctionKeyAlt
      xind = 3
    }

    keyCode match {
      case KeyEvent.VK_PAUSE =>
        if (shift || control) sendTelnetCommand(243.toByte)
      case KeyEvent.VK_F1 =>
        sendSpecial(fmap(1))
      case KeyEvent.VK_F2 =>
        sendSpecial(fmap(2))
      case KeyEvent.VK_F3 =>
        sendSpecial(fmap(3))
      case KeyEvent.VK_F4 =>
        sendSpecial(fmap(4))
      case KeyEvent.VK_F5 =>
        sendSpecial(fmap(5))
      case KeyEvent.VK_F6 =>
        sendSpecial(fmap(6))
      case KeyEvent.VK_F7 =>
        sendSpecial(fmap(7))
      case KeyEvent.VK_F8 =>
        sendSpecial(fmap(8))
      case KeyEvent.VK_F9 =>
        sendSpecial(fmap(9))
      case KeyEvent.VK_F10 =>
        sendSpecial(fmap(10))
      case KeyEvent.VK_F11 =>
        sendSpecial(fmap(11))
      case KeyEvent.VK_F12 =>
        sendSpecial(fmap(12))
      case KeyEvent.VK_UP =>
        sendSpecial(KeyUp(xind))
      case KeyEvent.VK_DOWN =>
        sendSpecial(KeyDown(xind))
      case KeyEvent.VK_LEFT =>
        sendSpecial(KeyLeft(xind))
      case KeyEvent.VK_RIGHT =>
        sendSpecial(KeyRight(xind))
      case KeyEvent.VK_PAGE_DOWN =>
        sendSpecial(NextScn(xind))
      case KeyEvent.VK_PAGE_UP =>
        sendSpecial(PrevScn(xind))
      case KeyEvent.VK_INSERT =>
        sendSpecial(Insert(xind))
      case KeyEvent.VK_DELETE =>
        sendSpecial(Remove(xind))
      case KeyEvent.VK_BACK_SPACE =>
        sendSpecial(BackSpace(xind))
//	if (localecho) {
//	  if (BackSpace(xind) == "\b") {
//	    putString("\b \b") // make the last Char 'deleted'
//	  } else {
//	    putString(BackSpace(xind)) // echo it
//	  }
//	}
      case KeyEvent.VK_HOME =>
        sendSpecial(KeyHome(xind))
      case KeyEvent.VK_END =>
        sendSpecial(KeyEnd(xind))
      case KeyEvent.VK_NUM_LOCK =>
        if (vms && control) {
          sendSpecial(PF1)
        }
        if (!control) {
          numlock = !numlock
        }
      case KeyEvent.VK_CAPS_LOCK =>
        capslock = !capslock
      case KeyEvent.VK_SHIFT | KeyEvent.VK_CONTROL | KeyEvent.VK_ALT =>
      case _ =>
    }
  }

  def keyReleased(keyCode: Int, keyChar: Char, modifiers: Int) {
    // ignore
  }

  /**
   * Handle key Typed events for the terminal, this will get
   * all normal key types, but no shift/alt/control/numlock.
   */
  def keyTyped(keyCode: Int, keyChar: Char, modifiers: Int) {
    val control = (modifiers & KEY_CONTROL) != 0
    val shift = (modifiers & KEY_SHIFT) != 0
    val alt = (modifiers & KEY_ALT) != 0

    if (keyChar == '\t') {
      if (shift) {
        send(TabKey(1))
      } else {
        if (control) {
          send(TabKey(2))
        } else {
          if (alt) {
            send(TabKey(3))
          } else {
            send(TabKey(0))
          }
        }
      }
      return
    }
    
    if (alt) {
      send("" + (keyChar | 0x80).toChar)
      return
    }

    if ((keyCode == KeyEvent.VK_ENTER || keyChar == 10) && !control) {
      send("\r")
      //if (localecho) putString("\r\n") // bad hack
      return
    }

    if (keyCode == 10 && !control) {
      System.out.println("Sending \\r")
      send("\r")
      return
    }

    // FIXME: on german PC keyboards you have to use Alt-Ctrl-q to get an @,
    // so we can't just use it here... will probably break some other VMS
    // codes.  -Marcus
    // if(((!vms && keyChar == '2') || keyChar == '@' || keyChar == ' ')
    //    && control)
    if ((!vms && keyChar == '2' || keyChar == ' ') && control)
      send("" + 0.toChar)

    if (vms) {
      if (keyChar == 127 && !control) {
        if (shift)
          sendSpecial(Insert(0))       //  VMS shift delete = insert
        else
          sendSpecial(Remove(0))       //  VMS delete = remove
        return
      } else if (control)
        keyChar match {
          case '0' =>
            sendSpecial(Numpad(0))
            return
          case '1' =>
            sendSpecial(Numpad(1))
            return
          case '2' =>
            sendSpecial(Numpad(2))
            return
          case '3' =>
            sendSpecial(Numpad(3))
            return
          case '4' =>
            sendSpecial(Numpad(4))
            return
          case '5' =>
            sendSpecial(Numpad(5))
            return
          case '6' =>
            sendSpecial(Numpad(6))
            return
          case '7' =>
            sendSpecial(Numpad(7))
            return
          case '8' =>
            sendSpecial(Numpad(8))
            return
          case '9' =>
            sendSpecial(Numpad(9))
            return
          case '.' =>
            sendSpecial(KPPeriod)
            return
          case '-' =>
          case 31 =>
            sendSpecial(KPMinus)
            return
          case '+' =>
            sendSpecial(KPComma)
            return
          case 10 =>
            sendSpecial(KPEnter)
            return
          case '/' =>
            sendSpecial(PF2)
            return
          case '*' =>
            sendSpecial(PF3)
            return
            /* NUMLOCK handled in keyPressed */
          case _ =>
        }
      /* Now what does this do and how did it get here. -Marcus
       if (shift && keyChar < 32) {
       write(PF1+(Char)(keyChar + 64))
       return
       }
       */
    }

    // FIXME: not used?
    var xind = 0
    var fmap = FunctionKey
    if (shift) {
      fmap = FunctionKeyShift
      xind = 1
    }
    if (control) {
      fmap = FunctionKeyCtrl
      xind = 2
    }
    if (alt) {
      fmap = FunctionKeyAlt
      xind = 3
    }

    if (keyCode == KeyEvent.VK_ESCAPE) {
      sendSpecial(Escape(xind))
      return
    }

    if ((modifiers & KEY_ACTION) != 0) {
      keyCode match {
        case KeyEvent.VK_NUMPAD0 =>
          sendSpecial(Numpad(0))
          return
        case KeyEvent.VK_NUMPAD1 =>
          sendSpecial(Numpad(1))
          return
        case KeyEvent.VK_NUMPAD2 =>
          sendSpecial(Numpad(2))
          return
        case KeyEvent.VK_NUMPAD3 =>
          sendSpecial(Numpad(3))
          return
        case KeyEvent.VK_NUMPAD4 =>
          sendSpecial(Numpad(4))
          return
        case KeyEvent.VK_NUMPAD5 =>
          sendSpecial(Numpad(5))
          return
        case KeyEvent.VK_NUMPAD6 =>
          sendSpecial(Numpad(6))
          return
        case KeyEvent.VK_NUMPAD7 =>
          sendSpecial(Numpad(7))
          return
        case KeyEvent.VK_NUMPAD8 =>
          sendSpecial(Numpad(8))
          return
        case KeyEvent.VK_NUMPAD9 =>
          sendSpecial(Numpad(9))
          return
        case KeyEvent.VK_DECIMAL =>
          sendSpecial(NUMDot(xind))
          return
        case KeyEvent.VK_ADD =>
          sendSpecial(NUMPlus(xind))
          return
      }
    }

    keyChar match {
      case 8 | 127 | '\r' | '\n' =>
      case _ =>
        send("" + keyChar)
    }
  }

  
  /**
   * Set the terminal id used to identify this terminal.
   * @param terminalID the id string
   */
  def setToScoansi {

    FunctionKey(1)  = "\u001b[M"
    FunctionKey(2)  = "\u001b[N"
    FunctionKey(3)  = "\u001b[O"
    FunctionKey(4)  = "\u001b[P"
    FunctionKey(5)  = "\u001b[Q"
    FunctionKey(6)  = "\u001b[R"
    FunctionKey(7)  = "\u001b[S"
    FunctionKey(8)  = "\u001b[T"
    FunctionKey(9)  = "\u001b[U"
    FunctionKey(10) = "\u001b[V"
    FunctionKey(11) = "\u001b[W"
    FunctionKey(12) = "\u001b[X"
    FunctionKey(13) = "\u001b[Y"
    FunctionKey(14) = "?"
    FunctionKey(15) = "\u001b[a"
    FunctionKey(16) = "\u001b[b"
    FunctionKey(17) = "\u001b[c"
    FunctionKey(18) = "\u001b[d"
    FunctionKey(19) = "\u001b[e"
    FunctionKey(20) = "\u001b[f"
    PrevScn(0) = "\u001b[I"
    PrevScn(1) = "\u001b[I"
    PrevScn(2) = "\u001b[I"
    PrevScn(3) = "\u001b[I"
    NextScn(0) = "\u001b[G"
    NextScn(1) = "\u001b[G"
    NextScn(2) = "\u001b[G"
    NextScn(3) = "\u001b[G"
    // more theoretically.
  }

}

object TerminalInput {
  
  val KEY_CONTROL = 0x01
  val KEY_SHIFT   = 0x02
  val KEY_ALT     = 0x04
  val KEY_ACTION  = 0x08
  
  /**
   * Replace escape code characters (backslash + identifier) with their
   * respective codes.
   * @param tmp the string to be parsed
   * @return a unescaped string
   */
  def unEscape(tmp: String): String = {
    var idx = 0
    var oldidx = 0
    var cmd = ""
    while ({idx = tmp.indexOf('\\', oldidx); idx >= 0} && {idx += 1; idx <= tmp.length}) {
      cmd += tmp.substring(oldidx, idx - 1)
      if (idx == tmp.length()) return cmd
      
      tmp.charAt(idx) match {
        case 'b' =>
          cmd += "\b"
        case 'e' =>
          cmd += "\u001b"
        case 'n' =>
          cmd += "\n"
        case 'r' =>
          cmd += "\r"
        case 't' =>
          cmd += "\t"
        case 'v' =>
          cmd += "\u000b"
        case 'a' =>
          cmd += "\u0012"
        case _  =>
          if ((tmp.charAt(idx) >= '0') && (tmp.charAt(idx) <= '9')) {
            var i = idx
            var break = false
            while (i < tmp.length) {
              if ((tmp.charAt(i) < '0') || (tmp.charAt(i) > '9')) {
                break = true
              }
              i += 1
            }
            cmd += Integer.parseInt(tmp.substring(idx, i)).toChar
            idx = i - 1
          } else {
            cmd += tmp.substring(idx, {idx += 1; idx})
          }
      }
      oldidx = {idx += 1; idx}
    }
    if (oldidx <= tmp.length()) cmd += tmp.substring(oldidx)
    
    cmd
  }
  
  
}
