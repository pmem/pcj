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

import lib.util.persistent.front.PersistentClass;

public class AnnotationTest {

   public static void main(String[] args) {
      Foo foo = new Foo();
      Bar bar = new Bar();
   }

   @PersistentClass
   public static final class Foo extends PersistentObject {
      private static final IntField I = new IntField();
      private static final ObjectType<Foo> TYPE = ObjectType.fromFields(Foo.class, I);

      public Foo() {
         super(TYPE);
      }

      public Foo(ObjectPointer<Foo> p) {
         super(p);
      }
   }

   @PersistentClass
   public static class Bar extends PersistentValue {
      private static final IntField I = new IntField();
      public static final ValueType TYPE = ValueType.fromFields(I);

      public Bar() {
         super(TYPE, Bar.class);
      }

      protected Bar(ValuePointer<Bar> p) {
         super(p);
      }
   }

   // @PersistentClass
   public static class Zoo<T extends PersistentObject> extends PersistentObject {
      private static final IntField I = new IntField();
      private static final ObjectType<Zoo> TYPE = ObjectType.fromFields(Zoo.class, I);

      public Zoo() {
         super(TYPE);
      }

      public Zoo(ObjectPointer<Zoo> p) {
         super(p);
      }
   }
}
