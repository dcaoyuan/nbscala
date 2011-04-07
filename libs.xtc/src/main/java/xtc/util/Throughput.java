/*
 * xtc - The eXTensible Compiler
 * Copyright (C) 2005 Robert Grimm
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301,
 * USA.
 */
package xtc.util;

import java.io.BufferedReader;
import java.io.FileReader;

/**
 * A small utility program to compute a tool's throughput.  This
 * program takes as its input a text file, which contains one or more
 * &lt;file size in bytes, latency in seconds&gt; pairs.  Each pair is
 * specified on its own line, with the first number being a long
 * number, the second number being a double number, and the two
 * numbers being separated by a single space.
 *
 * @author Robert Grimm
 * @version $Revision: 1.4 $
 */
public class Throughput {

  /** Compute the throughput for the file specified on the command line. */
  public static void main(String[] args) {
    try {
      Statistics     sizes     = new Statistics();
      Statistics     latencies = new Statistics();
      BufferedReader in        = new BufferedReader(new FileReader(args[0]));

      for (String line = in.readLine(); null != line; line = in.readLine()) {
        int    idx     = line.indexOf(' ');
        long   size    = Long.parseLong(line.substring(0, idx));
        double latency = Double.parseDouble(line.substring(idx+1));

        sizes.add(size / 1024.0);
        latencies.add(latency);
      }

      double throughput = 1.0 / Statistics.fitSlope(sizes, latencies);

      System.out.println("Overall performance : " + Statistics.round(throughput)
                         + " KB/s");
    } catch (Exception x) {
      System.exit(1);
    }
    System.exit(0);
  }

}
