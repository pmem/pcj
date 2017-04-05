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

package examples.misc;

import java.lang.reflect.Method;
import java.util.Arrays;

public class Runner {
    public static void main(String[] args) throws Exception {
        int nArgs = args.length;
        if (nArgs < 3) {
            System.out.println("args: <target class with main method> <args to that main method, if any> <nThreads> <nRuns each thread> ");
            System.exit(-1);
        }
        String targetName = args[0];
        int nThreads = Integer.parseInt(args[nArgs - 2]);
        int nRuns = Integer.parseInt(args[nArgs - 1]);
        Class<?> targetClass = Class.forName(targetName);
        Method mainMethod = targetClass.getDeclaredMethod("main", String[].class);
        final String[] callArgs = Arrays.copyOfRange(args, 1, nArgs - 2);
        Thread[] ts = new Thread[nThreads];
        for (int i = 0; i < nThreads; i++) {
            ts[i] = new Thread(() -> {
                try {
                    for (int n = 0; n < nRuns; n++) {
                        System.out.println(String.format("\n======================== ThreadId %d run #%d ========================", Thread.currentThread().getId(), n + 1));
                        mainMethod.invoke(null, (Object)callArgs);
                    }
                } 
                catch (IllegalAccessException e) {e.printStackTrace(); throw new RuntimeException(e.getMessage());}
                catch (java.lang.reflect.InvocationTargetException e) {e.printStackTrace(); throw new RuntimeException(e.getMessage());}
            });
            ts[i].start();
            System.out.println("started thread " + ts[i].getId());
        }
        try {
            for (Thread t : ts) {
                t.join();
                System.out.println("joined thread " + t.getId());
            }
        } catch (InterruptedException e) {e.printStackTrace();}
    }
}
