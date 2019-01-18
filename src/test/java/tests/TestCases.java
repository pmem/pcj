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

import lib.util.persistent.*;
import lib.util.persistent.types.*;
import lib.xpersistent.*;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Arrays;
import java.util.ArrayList;
import lib.util.persistent.spi.PersistentMemoryProvider;
import lib.util.persistent.front.PersistentClass;
import static lib.util.persistent.Util.*;

public class TestCases {
    static boolean verbose = false;

    public static void main(String[] args) {
        verbose = true;
        run();
    }

    public static boolean run() {
        System.out.println("****************TestCases******************************");
        return testCases();
    }

    public static boolean testCases() {
        try {
            PersistentMemoryProvider.getDefaultProvider().getHeap().open();

            PersistentString ods = new PersistentString("test string");
            if (verbose) { System.out.println("ods = " + ods); }
            ObjectDirectory.put("ods", ods);
            PersistentString ods1 = ObjectDirectory.get("ods", PersistentString.class);
            if (verbose) { System.out.println("ods1 = " + ods1); }
            ods = null;
            System.gc();

            ObjectDirectory.remove("ods", PersistentString.class);
            System.gc();

            PersistentLong pl1 = new PersistentLong(789);
            ObjectDirectory.put("pl1", pl1);
            if (verbose) { System.out.println(pl1); }
            pl1 = null;
            System.gc();
            PersistentLong pl1p = ObjectDirectory.get("pl1", PersistentLong.class);
            if (verbose) { System.out.println(pl1p); }

            PersistentLong pl2 = new PersistentLong(890);
            if (verbose) { System.out.println(pl2); }
            pl2 = null;
            System.gc();

            PersistentByteArray ba = new PersistentByteArray(100);
            ba.set(50, (byte)10);
            byte b = ba.get(50);
            if (verbose) { System.out.println("ba.get(50), should be 10 -> " + b); }
            ba = null; System.gc();

            if (verbose) { System.out.println("new PersistentString1"); }
            PersistentString sx1 = new PersistentString("xxxxxx");
            if (verbose) { System.out.println("created PersistentStringX " + sx1 + ", len = " + sx1.length()); }
            sx1 = null;
            System.gc();

            Point p1 = new Point(12.5, 15.5);
            if (verbose) { System.out.println(p1); }

            PersistentFloatArray fa = new PersistentFloatArray(100);
            for (int i = 0; i < 100; i++) assert(fa.get(i) == 0.0f);
            if (verbose) { System.out.println("Tested init of FloatArray"); }

            if (verbose) { System.out.println("new PersistentString2"); }
            PersistentString sx2 = new PersistentString("xxxxxx");
            if (verbose) { System.out.println("created PersistentStringX " + sx2 + ", len = " + sx2.length()); }

            PersistentDoubleArray da = new PersistentDoubleArray(100);
            da.set(50, 12.5d);
            double d = da.get(50);
            if (verbose) { System.out.println("da.get(50), should be 12.5 -> " + d); }

            if (verbose) { System.out.println("new PersistentLongArray(100)"); }
            PersistentLongArray la = new PersistentLongArray(100);
            la.set(50, 10);
            long l = la.get(50);
            if (verbose) { System.out.println("la.get(50), should be 10 -> " + l); }

            if (verbose) { System.out.println("new PersistentImmutableLongArray(100)"); }
            PersistentImmutableLongArray ila = new PersistentImmutableLongArray(new long[] {10, 20, 30, 40, 50, 60, 70});
            long il = ila.get(4);
            if (verbose) { System.out.println("ila.get(4), should be 50 -> " + il); }

            if (verbose) { System.out.println("new PersistentString"); }
            PersistentString s = new PersistentString("hello");
            if (verbose) { System.out.println("created PersistentString " + s + ", len = " + s.length()); }

            Mixed m = new Mixed(new PersistentString("dog"), 13);
            if (verbose) { System.out.println("m.s() = " + m.s()); }
            if (verbose) { System.out.println("m.x() = " + m.x()); }

            Mixed1 mx = new Mixed1(new Point(12.3, 23.4), new PersistentString("dog"), 13);
            if (verbose) { System.out.println("mx.p() = " + mx.p()); }
            if (verbose) { System.out.println("mx.s() = " + mx.s()); }
            if (verbose) { System.out.println("mx.x() = " + mx.x()); }

            PersistentString s1 = new PersistentString("hello");
            PersistentString s2 = new PersistentString("world");
            PersistentTuple2<PersistentString, PersistentString> t = new PersistentTuple2<>(s1, s2);
            if (verbose) { System.out.println("t._1 = " + t._1()); }
            if (verbose) { System.out.println("t._2 = " + t._2()); }

            PersistentLong pl = new PersistentLong(9090);
            if (verbose) { System.out.println("pl.toLong() = " + pl.longValue()); }

            Mixed m1 = new Mixed(new PersistentString("dog"), 13);
            PersistentTuple2<Mixed, PersistentLong> t1 = new PersistentTuple2<>(m, pl);
            if (verbose) { System.out.println("t1._1() = " + t1._1()); }
            if (verbose) { System.out.println("t1._2() = " + t1._2()); }
            if (verbose) { System.out.println("t1._1().s() = " + t1._1().s()); }
            if (verbose) { System.out.println("t1._1().x() = " + t1._1().x()); }
            if (verbose) { System.out.println("t1._2().toLong() = " + t1._2().longValue()); }

            PersistentDouble pd = new PersistentDouble(123.45d);
            if (verbose) { System.out.println("pd.toDouble() = " + pd.doubleValue()); }

            PersistentArray<PersistentObject> oa = new PersistentArray<>(m, t1);
            if (verbose) { System.out.println("oa.get(0) = " + oa.get(0)); }
            if (verbose) { System.out.println("oa.get(1) = " + oa.get(1)); }

            PersistentArray<Mixed> ma = new PersistentArray<>(m, m);
            if (verbose) { System.out.println("ma.get(0) = " + ma.get(0)); }
            if (verbose) { System.out.println("ma.get(1) = " + ma.get(1)); }

            PersistentArray<PersistentArray<Mixed>> maa = new PersistentArray<PersistentArray<Mixed>>(ma, ma);
            if (verbose) { System.out.println("maa.getClass(): "+maa.getClass()); }
            if (verbose) { System.out.println("maa.get(0) = " + maa.get(0)); }
            if (verbose) { System.out.println("maa.get(1) = " + maa.get(1)); }

            ExtendedMix em = new ExtendedMix(s1, 100, s2, 200);
            if (verbose) { System.out.println("em.s() = " + em.s()); }
            if (verbose) { System.out.println("em.x() = " + em.x()); }
            if (verbose) { System.out.println("em.es() = " + em.es()); }
            if (verbose) { System.out.println("em.ei() = " + em.ei()); }

            A aa = new A(12, 12345);
            if (verbose) { System.out.println("aa = " + aa); }

            B ab = new B(23, 34534, new PersistentString("hello"));
            if (verbose) { System.out.println("ab = " + ab); }

            C ac = new C(23, 34534, new PersistentString("hello"), 987923, new PersistentString("world"));
            if (verbose) { System.out.println("ac = " + ac); }

            Point p = new Point(12.5, 15.5);
            if (verbose) { System.out.println(p); }

            PersistentString xx = new PersistentString("hello");
            if (verbose) { System.out.println(xx); }

            Employee bob = new Employee(new PersistentString("Bob"), 12378);
            if (verbose) { System.out.println(bob); }

            Employee1 bob1 = new Employee1(new PersistentString("Bob"), 12378);
            if (verbose) { System.out.println(bob1); }

            Employee2 bob2 = new Employee2(new PersistentString("Bob"), 12378, "persistent", "Java");
            if (verbose) { System.out.println(bob2); }

            Point px = new Point(35.5, 36.5);
            if (verbose) { System.out.println(px); }

            T tt = new T(new PersistentString("hello"));
            if (verbose) { System.out.println(tt); }

            PersistentTuple3<Employee1, Point, Employee2> tup3 = new PersistentTuple3<>(bob1, px, bob2);
            if (verbose) { System.out.println("tup3 contents: " + tup3._1() + ", " + tup3._2() + ", " + tup3._3()); }

            PersistentImmutableTuple2<Employee1, Point> tup2 = new PersistentImmutableTuple2<>(bob1, px);
            if (verbose) { System.out.println("tup2 contents: " + tup2._1() + ", " + tup2._2()); }

            PersistentArray<PersistentString> parr = new PersistentArray<>(10);
            parr.set(0, new PersistentString("Lei"));
            parr.set(1, new PersistentString("Steve"));
            parr.set(2, new PersistentString("Soji"));
            parr.set(3, new PersistentString("Vamsi"));
            PersistentArray<PersistentString> parr2 = PersistentArrays.copyOfRange(parr, 1, 4);


            for (int i=0; i<parr.length(); i++){
                try{
                    if (verbose) { System.out.println("parr1: Element at index "+i+" is "+parr.get(i).toString()); }
                } catch (NullPointerException e){
                    if (verbose) { System.out.println("parr1: Element at index "+i+" is NULL"); }
                }
            }

            for (int i=0; i<parr2.length(); i++){
                try{
                    if (verbose) { System.out.println("parr2: Element at index "+i+" is "+parr2.get(i).toString()); }
                } catch (NullPointerException e){
                    if (verbose) { System.out.println("parr2: Element at index "+i+" is NULL"); }
                }
            }
            // test identity and equals methods
            assert(tup2.is(tup2));
            assert(!tup2.is(tup3));
            assert(tup2.equals(tup2));
            assert(!tup2.equals(tup3));

            assert(InnerStatic.getId() == 567);
            if (verbose) { System.out.println("InnerStatic.getId() = " + InnerStatic.getId()); }

            SelfRef sr0 = new SelfRef(13, null, persistent("hello"));
            SelfRef sr1 = new SelfRef(12, sr0, persistent("world"));
            if (verbose) { System.out.println(sr1); }

            // test PersistentLong
            PersistentLong tpl1 = new PersistentLong(12345);
            PersistentLong tpl2 = new PersistentLong(12345);
            assert(tpl1.hashCode() == tpl2.hashCode());
            assert(tpl1.longValue() ==  tpl2.longValue());

            // test identity
            PersistentString is1 = new PersistentString("world");
            PersistentTuple2<PersistentString, PersistentString> it1 = new PersistentTuple2<>(is1, is1);
            ObjectDirectory.put("is1", is1);
            PersistentHashMap<PersistentLong, PersistentString> im1 = new PersistentHashMap<>();
            im1.put(new PersistentLong(12345), is1);
            PersistentArray<PersistentString> ipa1 = new PersistentArray<>(2);
            ipa1.set(0, is1);
            ipa1.set(1, is1);
            assert(is1 == it1._1());
            assert(is1 == it1._2());
            assert(is1 == ObjectDirectory.get("is1", PersistentString.class));
            assert(is1 == im1.get(new PersistentLong(12345)));
            assert(ipa1.get(0) == is1);
            assert(ipa1.get(1) == is1);

            //test PersistentSkipListMap
            if (verbose) { System.out.println("****************Testing PersistentSkipListMap ****************"); }
            if (verbose) { System.out.println("****************Testing insertion****************"); }

            PersistentSkipListMap<PersistentInteger, PersistentString> map = new PersistentSkipListMap<>();

            map.clear();
            assert(map.size() == 0);
            PersistentInteger key = new PersistentInteger(1);
            PersistentString val = new PersistentString("world");
            assert(map.get(key) == null);
            PersistentString out = map.put(key, val);
            assert(out == null);
            assert(map.get(key) != null);
            assert(map.get(key).toString().equals("world"));
            assert(map.size() == 1);
            PersistentString val2 = new PersistentString("javad");
            out = map.put(key, val2);
            assert(out.toString().equals("world"));
            map.clear();

            if (verbose) { System.out.println("****************Testing removal****************"); }
            assert(map.size() == 0);
            key = new PersistentInteger(1);
            val = new PersistentString("world");
            map.put(key, val);
            assert(map.size() == 1);
            PersistentInteger key2 = new PersistentInteger(1);
            out = map.remove(key2);
            assert(out.toString().equals("world"));
            assert(map.size() == 0);
            assert(map.isEmpty() == true);
            map.put(key2, val);



            //test PersistentLinkedQueue
            if (verbose) { System.out.println("****************Testing PersistentLinkedQueue ****************"); }
            PersistentLinkedQueue<PersistentInteger> plq = new PersistentLinkedQueue<>();
            assert(plq.size() == 0);
            for(int i = 0; i < 10; i++) plq.add(new PersistentInteger(i));
            assert(plq.size() == 10);
            assert(plq.poll().toString().equals("0"));
            for(int i = 1; i <=5 ; i++) plq.poll();
            assert(plq.size() == 4);
            assert(plq.contains(new PersistentInteger(8)));
            plq.remove(new PersistentInteger(8));
            assert(!plq.contains(new PersistentInteger(8)) && plq.size() == 3);
            plq.clear();
            assert(plq.size() == 0);

            ArrayList<PersistentInteger> al = new ArrayList<>();
            for(int i = -5; i < 0; i++) al.add(new PersistentInteger(i));
            PersistentLinkedQueue<PersistentInteger> plq2 = new PersistentLinkedQueue<>(al);
            assert(plq2.peek().toString().equals("-5"));

            @SuppressWarnings("unchecked")
            PersistentLinkedQueue<PersistentInteger> plqPmem = ObjectDirectory.get("plq", PersistentLinkedQueue.class);
            if(plqPmem == null) {
                //plq2.clear();
                ConcurrentLinkedQueue<PersistentInteger> clq = new ConcurrentLinkedQueue<>();
                for(int i = 0; i <= 5; i++) clq.add(new PersistentInteger(i));
                plq2.addAll(clq);
                assert(plq2.contains(new PersistentInteger(5)));

                ObjectDirectory.put("plq", plq2);
                if (verbose) { System.out.println("PersistentLinkedQueue doesn't exist...saving to pmem"); }
                if (verbose) { System.out.println(plq2); }
            }
            else{
                if (verbose) { System.out.println("Retrieving PersistentLinkedQueue from pmem"); }
                if (verbose) { System.out.println(plqPmem); }
                assert(plqPmem.peek().toString().equals("-5") && plqPmem.size() == 11);
            }

            //test PersistentLinkedQueue
            if (verbose) { System.out.println("****************Testing PersistentLinkedList ****************"); }

            PersistentLinkedList<PersistentInteger> list = new PersistentLinkedList<>();
            for(int i = 0; i < 5; i++) list.add(new PersistentInteger(i));
            assert(list.get(2).toString().equals("2"));
            list.set(2, new PersistentInteger(22));
            assert(list.get(2).toString().equals("22"));
            list.insert(0, new PersistentInteger(-1));
            list.insert(3, new PersistentInteger(2));
            list.insert(list.size(), new PersistentInteger(9999));
            assert(list.get(0).toString().equals("-1"));
            assert(list.get(list.size()-1).toString().equals("9999"));
            list.remove(4);
            list.remove(0);
            list.remove(list.size()-1);
            assert(list.size() == 5);
            list.clear();
            assert(list.size() == 0);

            @SuppressWarnings("unchecked")
            PersistentLinkedList<PersistentInteger> pllPmem = ObjectDirectory.get("testcases_persistent_linked_list", PersistentLinkedList.class);
            if(pllPmem == null) {

                for(int i = 1; i <= 5; i++) list.add(new PersistentInteger(i));
                ObjectDirectory.put("testcases_persistent_linked_list", list);
                if (verbose) { System.out.println("PersistentLinkedList doesn't exist...saving to pmem"); }
                if (verbose) { System.out.println(list); }
            }
            else{
                if (verbose) { System.out.println("Retrieving PersistentLinkedList from pmem"); }
                if (verbose) { System.out.println(pllPmem); }
                assert(pllPmem.get(pllPmem.size()-1).toString().equals("5") && pllPmem.size() == 5);
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @PersistentClass
    public final static class Point extends PersistentObject {
        // metadata
        private static final DoubleField X = new DoubleField();
        private static final DoubleField Y = new DoubleField();
        private static final ObjectType<Point> TYPE = ObjectType.withFields(Point.class, X, Y);

        // constructor
        public Point(double x, double y) {
            super(TYPE);
            setX(x);
            setY(y);
        }

        //reconstuctor
        private Point(ObjectPointer<Point> p) {super(p);}

        // accessors
        public void setX(double x) {setDoubleField(X, x);}
        public void setY(double y) {setDoubleField(Y, y);}
        public double getX() {return getDoubleField(X);}
        public double getY() {return getDoubleField(Y);}

        public String toString() {return "Point(" + getX() + ", " + getY() + ")";}
    }

    @PersistentClass
    public static class Mixed extends PersistentObject {
        private static final StringField S = new StringField();
        private static final IntField X = new IntField();
        private static final ObjectType<Mixed> TYPE = ObjectType.withFields(Mixed.class, S, X);

        // constructor
        public Mixed(PersistentString s, int x) {
            this(TYPE, s, x);
        }

        // subclassing constructor
        protected Mixed(ObjectType<? extends Mixed> type, PersistentString s, int x) {
            super(type);
            s(s);
            x(x);
        }

        // reconstructor
        protected Mixed(ObjectPointer<? extends Mixed> p) {super(p);}

        // getters and setters
        public PersistentString s() {
            return getObjectField(S);
        }

        public int x() {
            return getIntField(X);
        }

        public void s(PersistentString s) {
            setObjectField(S, s);
        }

        public void x(int x) {
            setIntField(X, x);
        }

        public String toString() {
            return "Mixed(" + s() + ", " + x() + ")";
        }
    }

    @PersistentClass
    public static class ExtendedMix extends Mixed {
        private static final StringField ES = new StringField();
        private static final IntField EI = new IntField();
        private static final ObjectType<ExtendedMix> TYPE = ObjectType.extendClassWith(ExtendedMix.class, Mixed.class, ES, EI);

        public ExtendedMix(PersistentString s, int x, PersistentString es, int ei) {
            super(TYPE, s, x);
            es(es);
            ei(ei);
        }

        protected ExtendedMix(ObjectPointer<? extends ExtendedMix> p) {super(p);}

        public PersistentString es() {
            return getObjectField(ES);
        }

        public int ei() {
            return getIntField(EI);
        }

        public void es(PersistentString es) {
            setObjectField(ES, es);
        }

        public void ei(int ex) {
            setIntField(EI, ex);
        }
    }

    @PersistentClass
    public static class Mixed1 extends PersistentObject {
        private static final ObjectField<Point> P = new ObjectField<>(Point.class);
        private static final StringField S = new StringField();
        private static final IntField X = new IntField();
        public static final ObjectType<Mixed1> TYPE = ObjectType.withFields(Mixed1.class, P, S, X);

        // constructor
        public Mixed1(Point p, PersistentString s, int x) {
            this(TYPE, p, s, x);
        }

        // subclassing constructor
        protected Mixed1(ObjectType<? extends Mixed1> type, Point p, PersistentString s, int x) {
            super(type);
            p(p);
            s(s);
            x(x);
        }

        // reconstructor
        protected Mixed1(ObjectPointer<? extends Mixed1> p) {super(p);}

        // getters and setters
        public Point p() {
            return getObjectField(P);
        }

        public PersistentString s() {
            return getObjectField(S);
        }

        public int x() {
            return getIntField(X);
        }

        public void p(Point p) {
            setObjectField(P, p);
        }

        public void s(PersistentString s) {
            setObjectField(S, s);
        }

        public void x(int x) {
            setIntField(X, x);
        }

        public String toString() {
            return "Mixed1(" + s() + ", " + x() + ")";
        }
    }

    @PersistentClass
    public static class A extends PersistentObject {
        private static final IntField X = new IntField();
        private static final LongField Y = new LongField();
        public static final ObjectType<A> TYPE = ObjectType.withFields(A.class, X, Y);

        public A(int x, long y) {
            this(TYPE, x, y);
        }

        protected A(ObjectType<? extends A> type, int x, long y) {
            super(type);
            x(x);
            y(y);
        }

        protected A(ObjectPointer<? extends A> p) {super(p);}

        public void x(int x) {setIntField(X, x);}
        public void y(long y) {setLongField(Y, y);}
        public int x() {return getIntField(X);}
        public long y() {return getLongField(Y);}

        public String toString() {return "A(" + x() + ", " + y() + ")";}
    }

    @PersistentClass
    public static class B extends A {
        private static final StringField S = new StringField();
        public static final ObjectType<B> TYPE = A.TYPE.extendWith(B.class, S);

        public B(int x, long y, PersistentString s) {
            this(TYPE, x, y, s);
        }

        protected B(ObjectType<? extends B> type, int x, long y, PersistentString s) {
            super(type, x, y);
            s(s);
        }

        protected B(ObjectPointer<? extends B> p) {super(p);}

        public PersistentString s() {return getObjectField(S);}
        public void s(PersistentString s) {setObjectField(S, s);}

        public String toString() {return "B(" + x() + ", " + y() + ", " + s() + ")";}
    }

    @PersistentClass
    public final static class C extends B {
        private static final LongField ID = new LongField();
        private static final StringField NAME = new StringField();
        private static final ObjectType<C> TYPE = B.TYPE.extendWith(C.class, ID, NAME);

        public C(int x, long y, PersistentString s, long id, PersistentString name) {
            super(TYPE, x, y, s);
            setId(id);
            setName(name);
        }

        private C(ObjectPointer<C> p) {super(p);}

        public long getId() {return getLongField(ID);}
        public void setId(long id) {setLongField(ID, id);}
        public PersistentString getName() {return getObjectField(NAME);}
        public void setName(PersistentString name) {setObjectField(NAME, name);}

        public String toString() {return "C(" + x() + ", " + y() + ", " + s() + ", " + getId() + ", " + getName() + ")";}
    }

    @PersistentClass
    public static class Employee extends PersistentObject {
        private static final StringField NAME = new StringField();
        private static final LongField ID = new LongField();
        public static final ObjectType<Employee> TYPE = ObjectType.withFields(Employee.class, NAME, ID);

        public Employee(PersistentString name, long id) {
            this(TYPE, name, id);
        }

        protected Employee(ObjectType<? extends Employee> type, PersistentString name, long id) {
            super(type);
            name(name);
            id(id);
        }

        protected Employee(ObjectPointer<? extends Employee> p) {super(p);}

        public PersistentString name() {return getObjectField(NAME);}
        public void name(PersistentString name) {setObjectField(NAME, name);}
        public long id() {return getLongField(ID);}
        public void id(long id) {setLongField(ID, id);}
        public String toString() {return "Employee(" + name() + ", " + id() + ")";}
    }

    @PersistentClass
    public static class Employee1 extends PersistentObject {
        private static final StringField NAME = new StringField();
        private static final ObjectField<Employee> EMP = new ObjectField<>(Employee.class);
        private static final LongField ID = new LongField();
        public static final ObjectType<Employee1> TYPE = ObjectType.withFields(Employee1.class, NAME, EMP, ID);

        public Employee1(PersistentString name, long id) {
            this(TYPE, name, id);
        }

        public Employee1(ObjectType<? extends Employee1> type, PersistentString name, long id) {
            super(type);
            setName(name);
            setId(id);
            setEmployee(new Employee(new PersistentString("PreBob"), 928347));
        }

        protected Employee1(ObjectPointer<? extends Employee1> p) {super(p);}

        public PersistentString getName() {return getObjectField(NAME);}
        public void setName(PersistentString name) {setObjectField(NAME, name);}
        public Employee getEmployee() {return getObjectField(EMP);}
        public void setEmployee(Employee e) {setObjectField(EMP, e);}
        public long getId() {return getLongField(ID);}
        public void setId(long id) {setLongField(ID, id);}
        public String toString() {return "Employee1(" + getName() + ", " + getId() + ", " + getEmployee() + ")";}
    }

    @PersistentClass
    public static class Employee2 extends Employee1 {
        private static final ObjectField<PersistentArray> ACCTS = new ObjectField<>(PersistentArray.class);
        public static final ObjectType<Employee2> TYPE = Employee1.TYPE.extendWith(Employee2.class, ACCTS);

        public Employee2(PersistentString name, long id, String... accounts) {
            super(TYPE, name, id);
            PersistentArray<PersistentString> a = new PersistentArray<>(accounts.length);
            for (int i = 0; i < a.length(); i++) a.set(i, new PersistentString(accounts[i]));
            setObjectField(ACCTS, a);
            setName(name);
            setId(id);
            setEmployee(new Employee(new PersistentString("PreBob"), 928347));
        }

        protected Employee2(ObjectPointer<? extends Employee2> p) {super(p);}

        public PersistentArray accounts() {
            return getObjectField(ACCTS);
        }

        public String toString() {
            return "Employee2(" + getName() + ", " + getId() + ", " + getEmployee() + ", " + accounts().get(0) + ", " + accounts().get(1) + ")";}
    }

    @PersistentClass
    public static class T extends PersistentObject {
        private static final StringField NAME = new StringField();
        public static final ObjectType<T> TYPE = ObjectType.withFields(T.class, NAME);

        public T(PersistentString name) {
            super(TYPE);
            setName(name);
        }

        protected T(ObjectPointer<? extends T> p) {super(p);}

        public PersistentString getName() {
            return getObjectField(NAME);
        }

        public void setName(PersistentString name) {
            setObjectField(NAME, name);
        }

        public String toString() {
            return "T(" + getName() + ")";
        }
    }

    @PersistentClass
    public static final class InnerStatic extends PersistentObject {
        private static Statics statics;
        private static final IntField F = new IntField();
        private static final ObjectType<InnerStatic> TYPE = ObjectType.withFields(InnerStatic.class, F);

        static {
            statics = ObjectDirectory.get("InnerStatic_Statics", Statics.class);
            if (statics == null) {
                ObjectDirectory.put("InnerStatic_Statics", statics = new Statics());
                statics.setName(persistent("some name"));
                statics.setId(567);
            }
        }

        public InnerStatic() {
            super(TYPE);
        }

        public static PersistentString getName() {return statics.getName();}
        public static void setName(PersistentString name) {statics.setName(name);}
        public static int getId() {return statics.getId();}
        public static void setId(int id) {statics.setId(id);}
        public int getF() {return getIntField(F);}

        private InnerStatic(ObjectPointer<InnerStatic> p) {super(p);}

        public static final class Statics extends PersistentObject {
            private static final StringField STATIC_NAME = new StringField();
            private static final IntField STATIC_ID = new IntField();
            public static final ObjectType<Statics> TYPE = ObjectType.withFields(Statics.class, STATIC_NAME, STATIC_ID);

            public Statics() {super(TYPE);}

            private Statics(ObjectPointer<InnerStatic> p) {super(p);}

            public PersistentString getName() {return getObjectField(STATIC_NAME);}
            public void setName(PersistentString value) {setObjectField(STATIC_NAME, value);}
            public int getId() {return getIntField(STATIC_ID);}
            public void setId(int value) {setIntField(STATIC_ID, value);}

        }
    }

    // ValueType experiment

    // @PersistentClass
    // public static class DecoratedKey extends PersistentValue {
    //     private static final IntField CATEGORY = new IntField();
    //     private static final LongField OFFSET = new LongField();
    //     public static final ValueType TYPE = ValueType.fromFields(CATEGORY, OFFSET);

    //     public DecoratedKey(int category, long offset) {
    //         super(TYPE, DecoratedKey.class);
    //         setIntField(CATEGORY, category);
    //         setLongField(OFFSET, offset);
    //     }

    //     protected DecoratedKey(ValuePointer<DecoratedKey> p) {super(p);}

    //     public String toString() {
    //         return String.format("DecoratedKey(%d, %d)", getIntField(CATEGORY), getLongField(OFFSET));
    //     }
    // }

    // @PersistentClass
    // public static final class VTE extends PersistentObject {
    //     private static final StringField NAME = new StringField();
    //     private static final ValueField<Long256> VEC = ValueField.forClass(Long256.class);
    //     private static final ValueField<DecoratedKey> KEY = ValueField.forClass(DecoratedKey.class);
    //     private static final ObjectField<PersistentValueArray> ARR_KEYS = new ObjectField<>(PersistentValueArray.class);
    //     private static final ObjectType<VTE> TYPE = ObjectType.withFields(VTE.class, NAME, VEC, KEY, ARR_KEYS);

    //     public VTE() {
    //         super(TYPE);
    //         setObjectField(NAME, persistent("Bob"));
    //         setValueField(VEC, new Long256(10, 20, 30, 40));
    //         setValueField(KEY, new DecoratedKey(77, 1234567890));
    //         PersistentValueArray<DecoratedKey> ka = new PersistentValueArray<>(DecoratedKey.class, 100);
    //         ka.set(10, new DecoratedKey(130, 3497850));
    //         setObjectField(ARR_KEYS, ka);
    //     }

    //     private VTE(ObjectPointer<VTE> p) {super(p);}

    //     @SuppressWarnings("unchecked")
    //     public String toString() {
    //         PersistentValueArray<DecoratedKey> ka = getObjectField(ARR_KEYS);
    //         return String.format("VTE(%s, %s, %s, %s)", getObjectField(NAME), getValueField(VEC), getValueField(KEY), ka.get(10, DecoratedKey.class));
    //     }
    // }

    // Self-referencing type
    @PersistentClass
    public static class SelfRef extends PersistentObject {
        private static final IntField I = new IntField();
        private static final ObjectField<SelfRef> SR = new ObjectField<>(SelfRef.class);
        private static final StringField S = new StringField();
        public static final ObjectType<SelfRef> TYPE = ObjectType.withFields(SelfRef.class, I, SR, S);

        public SelfRef(int i, SelfRef sr, PersistentString s) {
            super(TYPE);
            setIntField(I, i);
            setObjectField(SR, sr);
            setObjectField(S, s);
        }

        protected SelfRef(ObjectPointer<? extends SelfRef> p) {super(p);}

        public String toString() {
            return String.format("SelfRef(%d, %s, %s)", getIntField(I), getObjectField(SR), getObjectField(S));
        }
    }
}
