package org.netbeans.modules.scala.console

import java.io.FilterOutputStream
import java.io.IOException
import java.io.OutputStream
import scala.collection.mutable.ArrayBuffer

class AnsiOutputStream(os: OutputStream) extends FilterOutputStream(os) {
  import Ansi._
  import AnsiOutputStream._
  
  private val options = new ArrayBuffer[Any]()
  private val buffer = Array.ofDim[Byte](MAX_ESCAPE_SEQUENCE_LENGTH)
  private var pos = 0
  private var startOfValue = 0
  private var state = LOOKING_FOR_FIRST_ESC_CHAR

  // TODO: implement to get perf boost: public void write(byte[] b, Int off, Int len)
	
  @throws(classOf[IOException])
  override 
  def write(data: Int) {
    state match {
      case LOOKING_FOR_FIRST_ESC_CHAR =>
        data match {
          case FIRST_ESC_CHAR =>
            buffer(pos) = data.toByte; pos += 1
            state = LOOKING_FOR_SECOND_ESC_CHAR
          case _ =>
            out.write(data)
        }
			
      case LOOKING_FOR_SECOND_ESC_CHAR =>
        buffer(pos) = data.toByte; pos += 1
        
        data match {
          case SECOND_ESC_CHAR =>
            state = LOOKING_FOR_NEXT_ARG
          case SECOND_OSC_CHAR =>
            state = LOOKING_FOR_OSC_COMMAND
          case _ =>
            reset(false)
        }
			
      case LOOKING_FOR_NEXT_ARG =>
        buffer(pos) = data.toByte; pos += 1
        
        data match {
          case '"' =>
            startOfValue = pos - 1
            state = LOOKING_FOR_STR_ARG_END
          case _ if '0' <= data && data <= '9' =>
            startOfValue = pos - 1
            state = LOOKING_FOR_INT_ARG_END		
          case ';' =>
            options += null
          case '?' =>
            options += '?'
          case '=' =>
            options += '='
          case _ =>
            reset(processEscapeCommand(options, data))
        }

      case LOOKING_FOR_INT_ARG_END =>
        buffer(pos) = data.toByte; pos += 1
        
        data match {
          case _ if '0' <= data && data <= '9' =>
          case _ =>
            val strValue = new String(buffer, startOfValue, (pos - 1) - startOfValue, "UTF-8")
            val value = new Integer(strValue)
            options += value
            data match {
              case ';' => 
                state = LOOKING_FOR_NEXT_ARG
              case _ =>
                reset(processEscapeCommand(options, data))
            }
        }
			
      case LOOKING_FOR_STR_ARG_END =>
        buffer(pos) = data.toByte; pos += 1
        
        data match {
          case '"' =>
          case _ =>
            val value = new String(buffer, startOfValue, (pos - 1) - startOfValue, "UTF-8")
            options += value
            data match {
              case ';' =>
                state = LOOKING_FOR_NEXT_ARG
              case _ =>
                reset(processEscapeCommand(options, data))
            }
        }
			
      case LOOKING_FOR_OSC_COMMAND =>
        buffer(pos) = data.toByte; pos += 1
        
        data match {
          case _ if '0' <= data && data <= '9' =>
            startOfValue = pos - 1
            state = LOOKING_FOR_OSC_COMMAND_END			
          case _ =>
            reset(false)
        }
		
      case LOOKING_FOR_OSC_COMMAND_END =>
        buffer(pos) = data.toByte; pos += 1
        
        data match {
          case ';' =>
            val strValue = new String(buffer, startOfValue, (pos - 1)-startOfValue, "UTF-8")
            val value = new Integer(strValue)
            options += value
            startOfValue = pos
            state = LOOKING_FOR_OSC_PARAM
          case _ if '0' <= data && data <= '9' =>
            // already pushed digit to buffer, just keep looking
          case _ =>
            // oops, did not expect this
            reset(false)
        }
			
      case LOOKING_FOR_OSC_PARAM =>
        buffer(pos) = data.toByte; pos += 1
        
        data match {
          case BEL =>
            val value = new String(buffer, startOfValue, (pos - 1) - startOfValue, "UTF-8")
            options += value
            reset(processOperatingSystemCommand(options))
          case FIRST_ESC_CHAR =>
            state = LOOKING_FOR_ST
          case _ =>
            // just keep looking while adding text
        }
			
      case LOOKING_FOR_ST =>
        buffer(pos) = data.toByte; pos += 1
        
        data match {
          case SECOND_ST_CHAR =>
            val value = new String(buffer, startOfValue, (pos - 2) - startOfValue, "UTF-8")
            options += value
            reset(processOperatingSystemCommand(options))
          case _ =>
            state = LOOKING_FOR_OSC_PARAM
        }
    }
		
    // Is it just too long?
    if (pos >= buffer.length) {
      reset(false)
    }
  }

  /**
   * Resets all state to continue with regular parsing
   * @param skipBuffer if current buffer should be skipped or written to out
   * @throws IOException
   */
  @throws(classOf[IOException])
  private def reset(skipBuffer: Boolean) {
    if (!skipBuffer) {
      out.write(buffer, 0, pos)
    }
    options.clear
    pos = 0
    startOfValue = 0
    state = LOOKING_FOR_FIRST_ESC_CHAR
  }
  
  /**
   * 
   * @param options
   * @param command
   * @return true if the escape command was processed.
   */
  @throws(classOf[IOException])
  private def processEscapeCommand(options: ArrayBuffer[Any], command: Int): Boolean = {
    try {
      command match {
        case 'A' =>
          processCursorUp(optionInt(options, 0, 1))
          true
        case 'B' =>
          processCursorDown(optionInt(options, 0, 1))
          true
        case 'C' =>
          processCursorRight(optionInt(options, 0, 1))
          true
        case 'D' =>
          processCursorLeft(optionInt(options, 0, 1))
          true
        case 'E' =>
          processCursorDownLine(optionInt(options, 0, 1))
          true
        case 'F' =>
          processCursorUpLine(optionInt(options, 0, 1))
          true
        case 'G' =>
          processCursorToColumn(optionInt(options, 0))
          true
        case 'H' | 'f' =>
          processCursorTo(optionInt(options, 0, 1), optionInt(options, 1, 1))
          true
        case 'J' =>
          processEraseScreen(optionInt(options, 0, 0))
          true
        case 'K' =>
          processEraseLine(optionInt(options, 0, 0))
          true
        case 'S' =>
          processScrollUp(optionInt(options, 0, 1))
          true
        case 'T' =>
          processScrollDown(optionInt(options, 0, 1))
          true
        case 'm' =>				
          // all options should be ints...
          var count = 0
          options foreach {
            case value: Int =>
              count += 1
              value match {
                case _ if 30 <= value && value <= 37 =>
                  processSetForegroundColor(value - 30)
                case _ if 40 <= value && value <= 47 =>
                  processSetBackgroundColor(value - 40)
                case 39 =>
                  processDefaultTextColor
                case 49 =>
                  processDefaultBackgroundColor
                case 0 =>
                  processAttributeRest
                case _ =>
                  processSetAttribute(value)
              }
            case null => 
              // ginore
            case _ => 
              throw new IllegalArgumentException()
          }

          if (count == 0) {
            processAttributeRest
          }
          true
        case 's' =>
          processSaveCursorPosition
          true
        case 'u' =>
          processRestoreCursorPosition
          true
        case _ =>
          if ('a' <= command && 'z' <= command) {
            processUnknownExtension(options, command)
            true
          } else if ('A' <= command && 'Z' <= command) {
            processUnknownExtension(options, command)
            true
          } else {
            false
          }
      }
    } catch {
      case ex: IllegalArgumentException => false
    }
  }

  /**
   * 
   * @param options
   * @return true if the operating system command was processed.
   */
  @throws(classOf[IOException])
  private def processOperatingSystemCommand(options: ArrayBuffer[Any]): Boolean = {
    val command = optionInt(options, 0)
    val label = options(1).asInstanceOf[String]
    // for command > 2 label could be composed (i.e. contain ';'), but we'll leave
    // it to processUnknownOperatingSystemCommand implementations to handle that
    try {
      command match {
        case 0 =>
          processChangeIconNameAndWindowTitle(label)
          true
        case 1 =>
          processChangeIconName(label)
          true
        case 2 =>
          processChangeWindowTitle(label)
          true
        case _ =>
          // not exactly unknown, but not supported through dedicated process methods:
          processUnknownOperatingSystemCommand(command, label)
          true
      }
    } catch {
      case ex: IllegalArgumentException => false
    }
  }
	
  @throws(classOf[IOException])
  protected def processRestoreCursorPosition() {}
  
  @throws(classOf[IOException])
  protected def processSaveCursorPosition() {}
  
  @throws(classOf[IOException])
  protected def processScrollDown(optionInt: Int) {}
  
  @throws(classOf[IOException])
  protected def processScrollUp(optionInt: Int) {}

  @throws(classOf[IOException])
  protected def processEraseScreen(eraseOption: Int) {}

  @throws(classOf[IOException])
  protected def processEraseLine(eraseOption: Int) {}

  @throws(classOf[IOException])
  protected def processSetAttribute(attribute: Int) {}

  @throws(classOf[IOException])
  protected def processSetForegroundColor(color: Int) {}

  @throws(classOf[IOException])
  protected def processSetBackgroundColor(color: Int) {}
	
  @throws(classOf[IOException])
  protected def processDefaultTextColor() {}
	
  @throws(classOf[IOException])
  protected def processDefaultBackgroundColor() {}

  @throws(classOf[IOException])
  protected def processAttributeRest() {}

  @throws(classOf[IOException])
  protected def processCursorTo(row: Int, col: Int) {}

  @throws(classOf[IOException])
  protected def processCursorToColumn(x: Int) {}

  @throws(classOf[IOException])
  protected def processCursorUpLine(count: Int) {}

  @throws(classOf[IOException])
  protected def processCursorDownLine(count: Int) {
    var i = 0
    while (i < count) {
      out.write('\n')
      i += 1
    }
  }

  @throws(classOf[IOException])
  protected def processCursorLeft(count: Int) {}

  @throws(classOf[IOException])
  protected def processCursorRight(count: Int) {
    var i = 0
    while (i < count) {
      out.write(' ')
      i += 1
    }
  }

  @throws(classOf[IOException])
  protected def processCursorDown(count: Int) {}

  @throws(classOf[IOException])
  protected def processCursorUp(count: Int) {}
	
  protected def processUnknownExtension(options: ArrayBuffer[Any], command: Int) {}
	
  protected def processChangeIconNameAndWindowTitle(label: String) {
    processChangeIconName(label)
    processChangeWindowTitle(label)
  }

  protected def processChangeIconName(label: String) {}

  protected def processChangeWindowTitle(label: String) {}
	
  protected def processUnknownOperatingSystemCommand(command: Int, param: String) {}

  private def optionInt(options: ArrayBuffer[Any], index: Int): Int = {
    if (options.size > index) {
      options(index) match {
        case value: Int => value
        case _ => throw new IllegalArgumentException()
      }
    } else {
      throw new IllegalArgumentException()
    }
  }

  private def optionInt(options: ArrayBuffer[Any], index: Int, defaultValue: Int): Int = {
    if (options.size > index) {
      options(index) match {
        case value: Int => value
        case _ => defaultValue
      }
    } else {
      defaultValue
    }
  }
	
  override
  def close() {
    write(REST_CODE)
    flush
    super.close
  }
}

object AnsiOutputStream {
  import Ansi._
  
  private val REST_CODE = Array[Byte](FIRST_ESC_CHAR, SECOND_ESC_CHAR, 'm')
  private val MAX_ESCAPE_SEQUENCE_LENGTH = 100
  private val LOOKING_FOR_FIRST_ESC_CHAR = 0
  private val LOOKING_FOR_SECOND_ESC_CHAR = 1
  private val LOOKING_FOR_NEXT_ARG = 2
  private val LOOKING_FOR_STR_ARG_END = 3
  private val LOOKING_FOR_INT_ARG_END = 4
  private val LOOKING_FOR_OSC_COMMAND = 5
  private val LOOKING_FOR_OSC_COMMAND_END = 6
  private val LOOKING_FOR_OSC_PARAM = 7
  private val LOOKING_FOR_ST = 8
}

object Ansi {
  
  val FIRST_ESC_CHAR:  Byte = 27
  val SECOND_ESC_CHAR: Byte = '['
  val SECOND_OSC_CHAR: Byte = ']'
  val SECOND_ST_CHAR:  Byte = '\\'
  
  val BEL = 7
  val ERASE_SCREEN_TO_END = 0
  val ERASE_SCREEN_TO_BEGINING = 1
  val ERASE_SCREEN = 2
  val ERASE_LINE_TO_END = 0
  val ERASE_LINE_TO_BEGINING = 1
  val ERASE_LINE = 2
  
  val ATTRIBUTE_INTENSITY_BOLD   = 1  // Intensity: Bold 	
  val ATTRIBUTE_INTENSITY_FAINT  = 2  // Intensity; Faint 	not widely supported
  val ATTRIBUTE_ITALIC           = 3  // Italic; on 	not widely supported. Sometimes treated as inverse.
  val ATTRIBUTE_UNDERLINE        = 4  // Underline; Single 	
  val ATTRIBUTE_BLINK_SLOW       = 5  // Blink; Slow 	less than 150 per minute
  val ATTRIBUTE_BLINK_FAST       = 6  // Blink; Rapid 	MS-DOS ANSI.SYS; 150 per minute or more
  val ATTRIBUTE_NEGATIVE_ON      = 7  // Image; Negative 	inverse or reverse; swap foreground and background
  val ATTRIBUTE_CONCEAL_ON       = 8  // Conceal on
  val ATTRIBUTE_UNDERLINE_DOUBLE = 21 // Underline; Double 	not widely supported
  val ATTRIBUTE_INTENSITY_NORMAL = 22 // Intensity; Normal 	not bold and not faint
  val ATTRIBUTE_UNDERLINE_OFF    = 24 // Underline; None 	
  val ATTRIBUTE_BLINK_OFF        = 25 // Blink; off 	
  val ATTRIBUTE_NEGATIVE_Off     = 27 // Image; Positive 	
  val ATTRIBUTE_CONCEAL_OFF      = 28 // Reveal 	conceal off

  val BLACK 	= 0
  val RED 	= 1
  val GREEN 	= 2
  val YELLOW 	= 3
  val BLUE 	= 4
  val MAGENTA 	= 5
  val CYAN 	= 6
  val WHITE 	= 7

  trait Attr

  abstract class Color(val value: Int, name: String) extends Attr {
    def fg = value + 30
    def bg = value + 40
    def fgBright = value + 90
    def bgBright = value + 100

    override 
    def toString = name
  }
  
  object Color {
    case object BLACK   extends Color(0, "BLACK")
    case object RED     extends Color(1, "RED")
    case object GREEN   extends Color(2, "GREEN")
    case object YELLOW  extends Color(3, "YELLOW")
    case object BLUE    extends Color(4, "BLUE")
    case object MAGENTA extends Color(5, "MAGENTA")
    case object CYAN    extends Color(6, "CYAN")
    case object WHITE   extends Color(7,"WHITE")
    case object DEFAULT extends Color(9,"DEFAULT")
  }
  
  abstract class Attribute(val value: Int, name: String) extends Attr {
    override
    def toString= name
  }
  
  object Attribute {
    case object RESET		   extends Attribute( 0, "RESET")
    case object INTENSITY_BOLD	   extends Attribute( 1, "INTENSITY_BOLD")
    case object INTENSITY_FAINT	   extends Attribute( 2, "INTENSITY_FAINT")
    case object ITALIC		   extends Attribute( 3, "ITALIC_ON")
    case object UNDERLINE	   extends Attribute( 4, "UNDERLINE_ON")
    case object BLINK_SLOW	   extends Attribute( 5, "BLINK_SLOW")
    case object BLINK_FAST	   extends Attribute( 6, "BLINK_FAST")
    case object NEGATIVE_ON	   extends Attribute( 7, "NEGATIVE_ON")
    case object CONCEAL_ON	   extends Attribute( 8, "CONCEAL_ON")
    case object STRIKETHROUGH_ON   extends Attribute( 9, "STRIKETHROUGH_ON")
    case object UNDERLINE_DOUBLE   extends Attribute(21, "UNDERLINE_DOUBLE")
    case object INTENSITY_BOLD_OFF extends Attribute(22, "INTENSITY_BOLD_OFF")
    case object ITALIC_OFF	   extends Attribute(23, "ITALIC_OFF")
    case object UNDERLINE_OFF	   extends Attribute(24, "UNDERLINE_OFF")
    case object BLINK_OFF	   extends Attribute(25, "BLINK_OFF")
    case object NEGATIVE_OFF	   extends Attribute(27, "NEGATIVE_OFF")
    case object CONCEAL_OFF	   extends Attribute(28, "CONCEAL_OFF")
    case object STRIKETHROUGH_OFF  extends Attribute(29, "STRIKETHROUGH_OFF")
  }

  abstract class Erase(val value: Int, name: String) extends Attr {
    override
    def toString = name
  }
  
  object Erase {
    case object FORWARD  extends Erase(0, "FORWARD")
    case object BACKWARD extends Erase(1, "BACKWARD")
    case object ALL      extends Erase(2, "ALL")
  }
  
  
  abstract class Code(a: Attr, name: String, background: Boolean) {
    def this(n: Attr, name: String) = this(n, name, false)
    
    Code.nameToValue += name -> this
    
    //
    // TODO: Find a better way to keep Code in sync with Color/Attribute/Erase
    //
    
    def isColor = a.isInstanceOf[Ansi.Color]
    def getColor = a.asInstanceOf[Ansi.Color]

    def isAttribute = a.isInstanceOf[Attribute]
    def getAttribute = a.asInstanceOf[Attribute]

    def isBackground = background
    
  }
  
  object Code {
    private var nameToValue = Map[String, Code]()
    
    def valueOf(name: String): Code = {
      nameToValue.getOrElse(name, null)
    }

    // Colors
    case object BLACK extends Code(Color.BLACK, "BLACK")
    case object RED extends Code(Color.RED, "RED")
    case object GREEN extends Code(Color.GREEN, "GREEN")
    case object YELLOW extends Code(Color.YELLOW, "YELLOW")
    case object BLUE extends Code(Color.BLUE, "BLUE")
    case object MAGENTA extends Code(Color.MAGENTA, "MAGENTA")
    case object CYAN extends Code(Color.CYAN, "CYAN")
    case object WHITE extends Code(Color.WHITE, "WHITE")

    // Foreground Colors
    case object FG_BLACK extends Code(Color.BLACK, "FG_BLACK", false)
    case object FG_RED extends Code(Color.RED, "FG_RED", false)
    case object FG_GREEN extends Code(Color.GREEN, "FG_GREEN", false)
    case object FG_YELLOW extends Code(Color.YELLOW, "FG_YELLOW", false)
    case object FG_BLUE extends Code(Color.BLUE, "FG_BLUE", false)
    case object FG_MAGENTA extends Code(Color.MAGENTA, "FG_MAGENTA", false)
    case object FG_CYAN extends Code(Color.CYAN, "FG_CYAN", false)
    case object FG_WHITE extends Code(Color.WHITE, "FG_WHITE", false)

    // Background Colors
    case object BG_BLACK extends Code(Color.BLACK, "BG_BLACK", true)
    case object BG_RED extends Code(Color.RED, "BG_RED", true)
    case object BG_GREEN extends Code(Color.GREEN, "BG_GREEN", true)
    case object BG_YELLOW extends Code(Color.YELLOW, "BG_YELLOW", true)
    case object BG_BLUE extends Code(Color.BLUE, "BG_BLUE", true)
    case object BG_MAGENTA extends Code(Color.MAGENTA, "BG_MAGENTA", true)
    case object BG_CYAN extends Code(Color.CYAN, "BG_CYAN", true)
    case object BG_WHITE extends Code(Color.WHITE, "BG_WHITE", true)

    // Attributes
    case object RESET extends Code(Attribute.RESET, "RESET")
    case object INTENSITY_BOLD extends Code(Attribute.INTENSITY_BOLD, "INTENSITY_BOLD")
    case object INTENSITY_FAINT extends Code(Attribute.INTENSITY_FAINT, "INTENSITY_FAINT")
    case object ITALIC extends Code(Attribute.ITALIC, "ITALIC")
    case object UNDERLINE extends Code(Attribute.UNDERLINE, "UNDERLINE")
    case object BLINK_SLOW extends Code(Attribute.BLINK_SLOW, "BLINK_SLOW")
    case object BLINK_FAST extends Code(Attribute.BLINK_FAST, "BLINK_FAST")
    case object BLINK_OFF extends Code(Attribute.BLINK_OFF, "BLINK_OFF")
    case object NEGATIVE_ON extends Code(Attribute.NEGATIVE_ON, "NEGATIVE_ON")
    case object NEGATIVE_OFF extends Code(Attribute.NEGATIVE_OFF, "NEGATIVE_OFF")
    case object CONCEAL_ON extends Code(Attribute.CONCEAL_ON, "CONCEAL_ON")
    case object CONCEAL_OFF extends Code(Attribute.CONCEAL_OFF, "CONCEAL_OFF")
    case object UNDERLINE_DOUBLE extends Code(Attribute.UNDERLINE_DOUBLE, "UNDERLINE_DOUBLE")
    case object UNDERLINE_OFF extends Code(Attribute.UNDERLINE_OFF, "UNDERLINE_OFF")

    // Aliases
    case object BOLD extends Code(Attribute.INTENSITY_BOLD, "BOLD")
    case object FAINT extends Code(Attribute.INTENSITY_FAINT, "FAINT")
  }
}