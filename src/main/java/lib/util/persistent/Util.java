/* Copyright (C) 2017  Intel Corporation
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 only, as published by the Free Software Foundation.
 * This file has been designated as subject to the "Classpath"
 * exception as provided in the LICENSE file that accompanied
 * this code.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License version 2 for more details (a copy
 * is included in the LICENSE file that accompanied this code).
 *
 * You should have received a copy of the GNU General Public License
 * version 2 along with this program; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA  02110-1301, USA.
 */

package lib.util.persistent;

import java.util.Random;

public class Util {

    private static Random random = new Random(System.nanoTime());

    public static int randomInt(int max) {return random.nextInt(max);}

    public static void sleep(long ms) {
        try {Thread.sleep(ms);} catch (Exception e) {e.printStackTrace();}
    }

    public static void join(Thread[] ts) {
        try {
            for (int i = 0; i < ts.length; i++) ts[i].join();
        } 
        catch (InterruptedException ie) {ie.printStackTrace();}
    }

}