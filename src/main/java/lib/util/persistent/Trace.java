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

public class Trace {
    static boolean enable = false;
    static boolean disableOverride = false;
    static boolean indent = false;

    public static void enable(boolean e) {enable = e;}
    public static void disableOverride(boolean e) {disableOverride = e;}
    public static void useIndent(boolean e) {indent = e;}

    private static boolean isEnabled() {
        return (enable && !disableOverride);
    }

    public static void trace(String formatStr, Object... args) {
        trace(0, formatStr, args);
    }

    public static void trace(boolean override, String formatStr, Object... args) {
        trace(override, 0, formatStr, args);
    }

    public static void trace(long address, String formatStr, Object... args) {
        trace(enable, address, formatStr, args);
    }

    public static void trace(boolean override, long address, String formatStr, Object... args) {
        boolean enabled = disableOverride ? enable : override;
        if (enabled) {
            String message = String.format(formatStr, args);
            System.out.format("%s: %d " + message + "\n", threadInfo(), address);
        }
    }

    public static String threadInfo() {
      StringBuilder buff = new StringBuilder();
      long tid = Thread.currentThread().getId();
      if (indent) for (int i = 0; i < tid; i++) buff.append(" ");
      buff.append(tid);
      return buff.toString();
    }

    public static void stackTrace() {
      if (isEnabled()) new Exception().printStackTrace();
    }
}