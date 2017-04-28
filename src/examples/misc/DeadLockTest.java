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

import lib.util.persistent.*;
import lib.util.persistent.types.*;

public class DeadLockTest {

    static PersistentFoo foo = new PersistentFoo(new PersistentString("hello"));

    public static void main(String[] args) {
        foo.method1(new PersistentString("world"));
        foo.method2(new PersistentString("world"));
    }

    static class PersistentFoo extends PersistentObject {
        private static final ObjectField<PersistentString> STR = new StringField();
        private static final ObjectType<PersistentFoo> TYPE = ObjectType.fromFields(PersistentFoo.class, STR);

        public PersistentFoo(PersistentString str) {
            super(TYPE);
            setObjectField(STR, str);
        }

        public synchronized void method1(PersistentString str) {
            System.out.println("Thread " + Thread.currentThread().getId() + " in method1");
            setObjectField(STR, str);
            System.out.println("Thread " + Thread.currentThread().getId() + " end in method1");
        }

        public void method2(PersistentString str) {
            System.out.println("Thread " + Thread.currentThread().getId() + " in method2");
            Transaction.run(() -> {
                method1(str);
            }, this, this);
            System.out.println("Thread " + Thread.currentThread().getId() + " end in method2");
        }
    }
}
