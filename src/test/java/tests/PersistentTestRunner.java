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

package tests;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.io.File;
import lib.util.persistent.spi.PersistentMemoryProvider;

public class PersistentTestRunner {
    public static void main(String[] args) {
    PersistentMemoryProvider.getDefaultProvider().getHeap().open();
        File directory = new File("src/test/java/tests/");
        File[] listOfFiles = directory.listFiles();
        ArrayList<Class<?>> testClasses = new ArrayList<>();
        for (int i = 0; i < listOfFiles.length; i++) {
            String fileName = listOfFiles[i].getName();
            if (!fileName.endsWith(".java")) continue;
            if (fileName.equals("PersistentTestRunner.java")) continue;
            String className = fileName.split("\\.")[0];
            try {
                testClasses.add(Class.forName("tests." + className));
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
        int testCount = testClasses.size(), passCount = 0;
        for (Class<?> testClass : testClasses) {
            Boolean ret = false;
            try {
                ret = (Boolean)(testClass.getDeclaredMethod("run").invoke(null));
                if (ret) passCount++;
            } catch (InvocationTargetException e) {
                System.out.println(testClass.getName() + " tests failed.");
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
        System.out.println(passCount + "/" + testCount + " tests passed.");
    }
}
