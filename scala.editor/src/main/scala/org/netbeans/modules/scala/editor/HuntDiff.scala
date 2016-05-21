package org.netbeans.modules.scala.editor

import java.io.BufferedReader
import java.io.IOException
import java.io.Reader
import java.util.regex.Pattern
import scala.collection.mutable.ArrayBuffer

/**
 * http://en.wikipedia.org/wiki/Hunt%E2%80%93McIlroy_algorithm
 *
 */
object HuntDiff {
  private val spaces = Pattern.compile("(\\s+)")

  case class Options(ignoreLeadingAndtrailingWhitespace: Boolean, ignoreInnerWhitespace: Boolean, ignoreCase: Boolean)
  private case class Line(lineNo: Int, line: String) { val hash = line.hashCode }
  private case class Candidate(a: Int, b: Int, c: Candidate)
  private object LineComparator extends java.util.Comparator[Line] {
    def compare(l1: Line, l2: Line) = l1.line.compareTo(l2.line)
  }

  @throws(classOf[IOException])
  private def getLines(r: Reader): Array[String] = {
    val br = new BufferedReader(r)
    val lines = new ArrayBuffer[String]()
    var line: String = null
    while ({ line = br.readLine; line != null }) {
      lines += line
    }
    lines.toArray
  }

  def diff(r1: Reader, r2: Reader, options: Options): Array[Diff] = {
    diff(getLines(r1), getLines(r2), options)
  }

  /**
   * @param lines1 array of lines from the first source
   * @param lines2 array of lines from the second source
   * @param options additional paremeters for the diff algorithm
   * @return computed diff
   */
  def diff(lines1: Array[String], lines2: Array[String], options: Options): Array[Diff] = {
    val m = lines1.length
    val n = lines2.length
    val lines1_original = copy(lines1)
    val lines2_original = copy(lines2)
    applyDiffOptions(lines1, lines2, options)

    var l2s = Array.ofDim[Line](n + 1)
    // In l2s we have sorted lines of the second file <1, n>
    var i = 1
    while (i <= n) {
      l2s(i) = new Line(i, lines2(i - 1))
      i += 1
    }
    java.util.Arrays.sort(l2s, 1, n + 1, LineComparator)

    val equvalenceLines = Array.ofDim[Int](n + 1)
    val equivalence = Array.ofDim[Boolean](n + 1)
    i = 1
    while (i <= n) {
      val l = l2s(i)
      equvalenceLines(i) = l.lineNo
      equivalence(i) = (i == n) || !l.line.equals(l2s(i + 1).line) //((Line) l2s.get(i)).line)
      i += 1
    }
    equvalenceLines(0) = 0
    equivalence(0) = true
    val equivalenceAssoc = Array.ofDim[Int](m + 1)
    i = 1
    while (i <= m) {
      equivalenceAssoc(i) = findAssoc(lines1(i - 1), l2s, equivalence)
      i += 1
    }

    l2s = null
    val K = Array.ofDim[Candidate](math.min(m, n) + 2)
    K(0) = new Candidate(0, 0, null)
    K(1) = new Candidate(m + 1, n + 1, null)
    var k = 0
    i = 1
    while (i <= m) {
      if (equivalenceAssoc(i) != 0) {
        k = merge(K, k, i, equvalenceLines, equivalence, equivalenceAssoc(i))
      }
      i += 1
    }
    val J = Array.ofDim[Int](m + 2) // Initialized with zeros

    var c = K(k)
    while (c != null) {
      J(c.a) = c.b
      c = c.c
    }

    val differences = getDifferences(J, lines1_original, lines2_original)
    cleanup(differences)
    differences.toArray
  }

  private def copy(strings: Array[String]) = {
    val copy = Array.ofDim[String](strings.length)
    System.arraycopy(strings, 0, copy, 0, strings.length)
    copy
  }

  private def applyDiffOptions(lines1: Array[String], lines2: Array[String], options: Options) {
    if (options.ignoreLeadingAndtrailingWhitespace && options.ignoreInnerWhitespace) {
      var i = 0
      while (i < lines1.length) {
        lines1(i) = spaces.matcher(lines1(i)).replaceAll("")
        i += 1
      }
      i = 0
      while (i < lines2.length) {
        lines2(i) = spaces.matcher(lines2(i)).replaceAll("")
        i += 1
      }
    } else if (options.ignoreLeadingAndtrailingWhitespace) {
      var i = 0
      while (i < lines1.length) {
        lines1(i) = lines1(i).trim
        i += 1
      }
      i = 0
      while (i < lines2.length) {
        lines2(i) = lines2(i).trim
        i += 1
      }
    } else if (options.ignoreInnerWhitespace) {
      var i = 0
      while (i < lines1.length) {
        replaceInnerSpaces(lines1, i)
        i += 1
      }
      i = 0
      while (i < lines2.length) {
        replaceInnerSpaces(lines2, i)
        i += 1
      }
    }
    if (options.ignoreCase) {
      var i = 0
      while (i < lines1.length) {
        lines1(i) = lines1(i).toUpperCase
        i += 1
      }
      i = 0
      while (i < lines2.length) {
        lines2(i) = lines2(i).toUpperCase
        i += 1
      }
    }
  }

  private def replaceInnerSpaces(strings: Array[String], idx: Int) {
    val m = spaces.matcher(strings(idx))
    val sb = new StringBuffer()
    while (m.find) {
      if (m.start == 0 || m.end == strings(idx).length) {
        m.appendReplacement(sb, "$1")
      } else {
        m.appendReplacement(sb, "")
      }
    }
    m.appendTail(sb)
    strings(idx) = sb.toString
  }

  private def findAssoc(line1: String, l2s: Array[Line], equivalence: Array[Boolean]): Int = {
    var idx = binarySearch(l2s, line1, 1, l2s.length - 1)
    if (idx < 1) {
      0
    } else {
      var lastGoodIdx = 0
      while (idx >= 1 && l2s(idx).line.equals(line1)) {
        if (equivalence(idx - 1)) {
          lastGoodIdx = idx
        }
        idx -= 1
      }
      lastGoodIdx
    }
  }

  private def binarySearch(L: Array[Line], key: String, _low: Int, _high: Int): Int = {
    var low = _low
    var high = _high
    while (low <= high) {
      val mid = (low + high) >> 1
      val midVal = L(mid).line
      val comparison = midVal.compareTo(key)
      if (comparison < 0) {
        low = mid + 1
      } else if (comparison > 0) {
        high = mid - 1
      } else {
        return mid
      }
    }

    -(low + 1)
  }

  private def binarySearch(K: Array[Candidate], key: Int, _low: Int, _high: Int): Int = {
    var low = _low
    var high = _high
    while (low <= high) {
      val mid = (low + high) >> 1
      val midVal = K(mid).b
      if (midVal < key) {
        low = mid + 1
      } else if (midVal > key) {
        high = mid - 1
      } else {
        return mid
      }
    }

    -(low + 1)
  }

  private def merge(K: Array[Candidate], _k: Int, i: Int, equvalenceLines: Array[Int], equivalence: Array[Boolean], _p: Int): Int = {
    var k = _k
    var p = _p
    var r = 0
    var c = K(0)
    var break = false
    do {
      val j = equvalenceLines(p)
      var s = binarySearch(K, j, r, k)
      if (s >= 0) {
        // j was found in K[]
        s = k + 1
      } else {
        s = -s - 2
        if (s < r || s > k) s = k + 1
      }
      if (s <= k) {
        if (K(s + 1).b > j) {
          val newc = Candidate(i, j, K(s))
          K(r) = c
          r = s + 1
          c = newc
        }
        if (s == k) {
          K(k + 2) = K(k + 1)
          k += 1
          break = true
        }
      }
      if (!break) {
        if (equivalence(p)) {
          break = true
        } else {
          p += 1
        }
      }
    } while (!break)
    K(r) = c

    k
  }

  private def getDifferences(J: Array[Int], lines1: Array[String], lines2: Array[String]): ArrayBuffer[Diff] = {
    val differences = new ArrayBuffer[Diff]()
    val n = lines1.length
    val m = lines2.length
    var start1 = 1
    var start2 = 1
    var break = false
    do {
      while (start1 <= n && J(start1) == start2) {
        start1 += 1
        start2 += 1
      }
      if (start1 > n) {
        break = true
      } else {
        if (J(start1) < start2) { // There's something extra in the first file
          var end1 = start1 + 1
          val deletedText = new StringBuffer()
          deletedText.append(lines1(start1 - 1)).append('\n')
          while (end1 <= n && J(end1) < start2) {
            val line = lines1(end1 - 1)
            deletedText.append(line).append('\n')
            end1 += 1
          }
          differences += Diff(Diff.DELETE, start1, end1 - 1, start2 - 1, 0, deletedText.toString, null)
          start1 = end1
        } else { // There's something extra in the second file
          val end2 = J(start1)
          val addedText = new StringBuffer()
          var i = start2
          while (i < end2) {
            val line = lines2(i - 1)
            addedText.append(line).append('\n')
            i += 1
          }
          differences += Diff(Diff.ADD, start1 - 1, 0, start2, end2 - 1, null, addedText.toString)
          start2 = end2
        }
      }
    } while (start1 <= n && !break)

    if (start2 <= m) { // There's something extra at the end of the second file
      var end2 = start2 + 1
      val addedText = new StringBuilder()
      addedText.append(lines2(start2 - 1)).append('\n')
      while (end2 <= m) {
        val line = lines2(end2 - 1)
        addedText.append(line).append('\n')
        end2 += 1
      }
      differences += Diff(Diff.ADD, n, 0, start2, m, null, addedText.toString)
    }

    differences
  }

  private def cleanup(diffs: ArrayBuffer[Diff]) {
    var last: Diff = null
    var i = 0
    while (i < diffs.size) {
      var diff = diffs(i)
      if (last != null) {
        if (diff.tpe == Diff.ADD && last.tpe == Diff.DELETE ||
          diff.tpe == Diff.DELETE && last.tpe == Diff.ADD) {

          val (add, del) = diff.tpe match {
            case Diff.ADD => (diff, last)
            case _        => (last, diff)
          }
          val d1f1l1 = add.firstStart - (del.firstEnd - del.firstStart)
          val d2f1l1 = del.firstStart
          if (d1f1l1 == d2f1l1) {
            val newDiff = Diff(
              Diff.CHANGE,
              d1f1l1, del.firstEnd, add.secondStart, add.secondEnd,
              del.firstText, add.secondText)
            diffs(i - 1) = newDiff
            diffs.remove(i)
            i -= 1
            diff = newDiff
          }
        }
      }
      last = diff
      i += 1
    }
  }

}

/**
 * @param type The type of the difference. Must be one of the <a href="#DELETE">DELETE</a>,
 *             <a href="#ADD">ADD</a> or <a href="#CHANGE">CHANGE</a>
 * @param firstStart The line number on which the difference starts in the first file.
 * @param firstEnd The line number on which the difference ends in the first file.
 * @param secondStart The line number on which the difference starts in the second file.
 * @param secondEnd The line number on which the difference ends in the second file.
 * @param firstText The text content of the difference in the first file.
 * @param secondText The text content of the difference in the second file.
 * @param firstLineDiffs The list of differences on lines in the first file.
 *                    The list contains instances of {@link Difference.Part}.
 *                    Can be <code>null</code> when there are no line differences.
 * @param secondLineDiffs The list of differences on lines in the second file.
 *                    The list contains instances of {@link Difference.Part}.
 *                    Can be <code>null</code> when there are no line differences.
 */
case class Diff(tpe: Int, firstStart: Int, firstEnd: Int, secondStart: Int, secondEnd: Int,
                firstText: String = null, secondText: String = null, firstLineDiffs: Array[Diff.Part] = null, secondLineDiffs: Array[Diff.Part] = null)

object Diff {
  /** Delete type of difference - a portion of a file was removed in the other */
  val DELETE = 0

  /** Add type of difference - a portion of a file was added in the other */
  val ADD = 1

  /** Change type of difference - a portion of a file was changed in the other */
  val CHANGE = 2

  /**
   * This class represents a difference on a single line.
   * @param type The type of the difference. Must be one of the {<a href="#DELETE">DELETE</a>,
   *             <a href="#ADD">ADD</a> or <a href="#CHANGE">CHANGE</a>
   * @param line The line number
   * @param pos1 The position on which the difference starts on this line.
   * @param pos2 The position on which the difference ends on this line.
   * @param text The text content of the difference.
   */
  case class Part(tpe: Int, line: Int, pos1: Int, pos2: Int, text: String)
}