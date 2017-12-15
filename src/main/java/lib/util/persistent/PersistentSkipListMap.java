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

import lib.util.persistent.types.*;
import lib.util.persistent.types.Types;
import lib.util.persistent.types.ArrayType;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;
import java.util.AbstractMap;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.NavigableSet;
import java.util.List;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.ArrayList;
import java.util.Set;
import java.util.AbstractCollection;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;
import lib.util.persistent.front.PersistentClass;
import static lib.util.persistent.Util.*;

public class PersistentSkipListMap<K extends AnyPersistent, V extends AnyPersistent> extends PersistentObject implements ConcurrentNavigableMap<K,V>, PersistentSortedMap<K,V> {
    private KeySet<K> keySet;
    private EntrySet<K,V> entrySet;
    private Values<V> values;
    private ConcurrentNavigableMap<K,V> descendingMap;

    final Comparator<? super K> comparator;
    Comparator<? super K> sister_comparator;

    private static Statics statics;
    private static final FinalObjectField<PersistentAtomicReference> HEAD = new FinalObjectField<>(PersistentAtomicReference.class);
    private static final FinalObjectField<AnyPersistent> BASE_HEADER = new FinalObjectField<>();
    public static final ObjectType<PersistentSkipListMap> TYPE = ObjectType.fromFields(PersistentSkipListMap.class, HEAD, BASE_HEADER);

    static {
        statics = ObjectDirectory.get("PersistentSkipListMap_statics", Statics.class);
        if (statics == null)
            ObjectDirectory.put("PersistentSkipListMap_statics", statics = new Statics());
    }

    public static final class Statics extends PersistentObject {
        private static final FinalObjectField<PersistentLong> BASE_HEADER = new FinalObjectField<>();
        public static final ObjectType<Statics> TYPE = ObjectType.fromFields(Statics.class, BASE_HEADER);

        public Statics() { 
            super(TYPE, (PersistentObject obj) -> { 
            obj.initObjectField(BASE_HEADER, new PersistentLong(8627078645895051609L));
            });
        }

        public Statics (ObjectPointer<Statics> p) { super(p); }

        public AnyPersistent baseHeader() { return getObjectField(BASE_HEADER); }
    }   

    public static AnyPersistent baseHeader() { return statics.baseHeader(); }

    @SuppressWarnings("unchecked")
    public HeadIndex<K,V> head() { return (HeadIndex<K,V>)getObjectField(HEAD).get(); }

    @SuppressWarnings("unchecked")
    void head(HeadIndex<K,V> head) { getObjectField(HEAD).set(head); }

    private void initialize() {
        keySet = null;
        entrySet = null;
        values = null;
        descendingMap = null;
        head(new HeadIndex<K,V>(new Node<K,V>(null, baseHeader(), null),null, null, 1));
    }

    @SuppressWarnings("unchecked")
    private boolean casHead(HeadIndex<K,V> cmp, HeadIndex<K,V> val) {
        return getObjectField(HEAD).compareAndSet(cmp, val);
    }

    /* ---------------- Nodes -------------- */

    @PersistentClass
    public static final class Node<K extends AnyPersistent,V extends AnyPersistent> extends PersistentObject {
        private static final FinalObjectField<AnyPersistent> KEY = new FinalObjectField<>();
        private static final ObjectField<AnyPersistent> VALUE = new ObjectField<>();
        private static final ObjectField<Node> NEXT = new ObjectField<>(Node.class);
        private static final FinalBooleanField MARKER = new FinalBooleanField();
        private static final ObjectType<Node> TYPE = ObjectType.fromFields(Node.class, KEY, VALUE, NEXT, MARKER);
        private boolean hasNullValue = false;


        //Creates regular node
        @SuppressWarnings("unchecked")
        Node (K key, AnyPersistent value, Node<K,V> next) {
            super(TYPE, (Node obj) -> {
                obj.initObjectField(KEY, key);
                obj.initBooleanField(MARKER, false);
            });
                setObjectField(VALUE, value);
                setObjectField(NEXT, next);
        }

        //Creates new marker node.
        @SuppressWarnings("unchecked")
        Node (Node<K,V> next) {
            super(TYPE, (Node obj) -> {
                obj.initObjectField(KEY, null);
                obj.initBooleanField(MARKER, true);
            });
                setObjectField(VALUE, this);
                setObjectField(NEXT, next);
        }

        public Node (ObjectPointer<Node> p) { 
                super(p); 
            //Transaction.run( ()->{}, ()-> {
                hasNullValue = (getObjectField(KEY) != null && getObjectField(VALUE) == null);
            //});
        } 
    
        @SuppressWarnings("unchecked")
        K key(){ return (K) getObjectField(KEY); }

        @SuppressWarnings("unchecked")
        Node<K,V> next() { return (Node<K,V>)getObjectField(NEXT); }

        AnyPersistent value(){ return getObjectField(VALUE); }

        @SuppressWarnings("unchecked")
        private void next(Node<K,V> n){ setObjectField(NEXT, n); }

        @SuppressWarnings("unchecked")
        private boolean compareAndSetNext(AnyPersistent expect, Node<K,V> update) {
        return Util.synchronizedBlock(this, () -> {
		    if(next() != expect) return false; 
		    setObjectField(NEXT, update);
		    return true;
        });
	    }

        @SuppressWarnings("unchecked")
        private boolean compareAndSetValue(AnyPersistent expect, AnyPersistent update) {
        return Util.synchronizedBlock(this, () -> {
		    if(value() != expect) return false; 
		    setObjectField(VALUE, update);
		    return true;
        });
	    }

        @SuppressWarnings("unchecked")
        boolean casValue(AnyPersistent cmp, AnyPersistent val) {
            if (val != null) return compareAndSetValue(cmp, val);
            else {
                 Box<Boolean> ret = new Box<>();
                 Transaction.run(() -> {
                    ret.set(compareAndSetValue(cmp, val));
                    }, () -> {
                    if (ret.get()) hasNullValue = true;
                    }
                );
                 return ret.get();
            }
        }

        @SuppressWarnings("unchecked")
        boolean casNext(Node<K,V> cmp, Node<K,V> val) {
            return compareAndSetNext(cmp, val);
        }

        boolean hasNullValue() {
            return hasNullValue;
        }

        boolean isMarker() {
            //return value() == this;
            return getBooleanField(MARKER);
        }

        boolean isBaseHeader() {
            return value() == baseHeader();
        }

        boolean appendMarker(Node<K,V> f) {
            return casNext(f, new Node<K,V>(f));
        }

        void helpDelete(Node<K,V> b, Node<K,V> f) {
            /*
             * Rechecking links and then doing only one of the
             * help-out stages per call tends to minimize CAS
             * interference among helping threads.
             */
            if (f == next() && this == b.next()) {
                if (f == null || f.value() != f) // not already marked
                    casNext(f, new Node<K,V>(f));
                else
                    b.casNext(this, f.next());
            }
        }

        V getValidValue() {
            AnyPersistent v = value();
            if (v == this || v == baseHeader())
                return null;
            @SuppressWarnings("unchecked") V vv = (V)v;
            return vv;
        }

        AbstractMap.SimpleImmutableEntry<K,V> createSnapshot() {
            AnyPersistent v = value();
            if (v == null || v == this || v == baseHeader())
                return null;
            @SuppressWarnings("unchecked") V vv = (V)v;
            return new AbstractMap.SimpleImmutableEntry<K,V>(key(), vv);
        }

    }

    /* ---------------- Indexing -------------- */
    public static class Index<K extends AnyPersistent, V extends AnyPersistent> extends PersistentImmutableObject {
        private static final FinalObjectField<Node> NODE = new FinalObjectField<>(Node.class);
        private static final FinalObjectField<Index> DOWN = new FinalObjectField<>(Index.class);
        private static final FinalObjectField<PersistentAtomicReference> RIGHT = new FinalObjectField<>(PersistentAtomicReference.class);
        public static final ObjectType<Index> TYPE = ObjectType.fromFields(Index.class, NODE, DOWN, RIGHT);

        @SuppressWarnings("unchecked")
        Index(Node<K,V> node, Index<K,V> down, Index<K,V> right) {
            this(TYPE, node, down, right);
        }

        @SuppressWarnings("unchecked")
        public Index(ObjectPointer<? extends Index> p) { super(p); }

        @SuppressWarnings("unchecked")
        Index(ObjectType<? extends Index> type, Node<K,V> node, Index<K,V> down, Index<K,V> right) {
            this(TYPE, node, down, right, null);
        }

        @SuppressWarnings("unchecked")
        <T extends Index> Index(ObjectType<T> type, Node<K,V> node, Index<K,V> down, Index<K,V> right, Consumer<T> initializer) {
            super(type, (Index obj) -> {
                obj.initObjectField(NODE, node);
                obj.initObjectField(DOWN, down);
                obj.initObjectField(RIGHT, new PersistentAtomicReference(right));
				if (initializer != null) initializer.accept((T)obj);
            });
        }	

        @SuppressWarnings("unchecked")
        Node<K,V> node() { return (Node<K,V>) getObjectField(NODE); }
        @SuppressWarnings("unchecked")
        Index <K,V> down() { return (Index<K,V>)getObjectField(DOWN); }
        @SuppressWarnings("unchecked")
        Index<K,V> right() { return (Index<K,V>)getObjectField(RIGHT).get(); }
        @SuppressWarnings("unchecked")
        void right(Index<K,V> right) { getObjectField(RIGHT).set(right); }

        @SuppressWarnings("unchecked")
        final boolean casRight(Index<K,V> cmp, Index<K,V> val) {
            return getObjectField(RIGHT).compareAndSet(cmp, val);
        }

        final boolean indexesDeletedNode() {
            return node().value() == null;
        }

        final boolean link(Index<K,V> succ, Index<K,V> newSucc) {
            Node<K,V> n = node();
            newSucc.right(succ);
            return n.value() != null && casRight(succ, newSucc);
        }

        final boolean unlink(Index<K,V> succ) {
            return node().value() != null && casRight(succ, succ.right());
        }

    }

    /* ---------------- Head nodes -------------- */

    public static final class HeadIndex<K extends AnyPersistent,V extends AnyPersistent> extends Index<K,V> {
        private static final IntField LEVEL = new IntField();
        private static final ObjectType TYPE = Index.TYPE.extendWith(HeadIndex.class, LEVEL);
 
        @SuppressWarnings("unchecked")
        HeadIndex(Node<K,V> node, Index<K,V> down, Index<K,V> right, int level) {
            //super(TYPE, node, down, right);
            super(TYPE, node, down, right, (HeadIndex obj) -> {
                obj.initIntField(LEVEL, level);
            });
        }
    
        public HeadIndex(ObjectPointer<HeadIndex> p) { super(p); }

        public int level() { return getIntField(LEVEL); }
    }

    /* ---------------- Comparison utilities -------------- */

    @SuppressWarnings({"unchecked", "rawtypes"})
    static final int cpr(Comparator c, Object x, AnyPersistent y) {
        return (c != null) ? c.compare(x, y) : ((Comparable)x).compareTo(y);
    }

    /* ---------------- Traversal -------------- */

    private Node<K,V> findPredecessor(Object key, Comparator<? super K> cmp) {
        if (key == null)
            throw new NullPointerException(); // don't postpone errors
        for (;;) {
            for (Index<K,V> q = head(), r = q.right(), d;;) {
                if (r != null) {
                    Node<K,V> n = r.node();
                    K k = n.key();
                    if (n.hasNullValue()) {
                    //if (n.value() == null) {
                        if (!q.unlink(r))
                            break;           // restart
                        r = q.right();         // reread r
                        continue;
                    }
                    if (cpr(cmp, key, k) > 0) {
                        q = r;
                        r = r.right();
                        continue;
                    }
                }
                if ((d = q.down()) == null)
                    return q.node();
                q = d;
                r = d.right();
            }
        }
    }

    private Node<K,V> findNode(AnyPersistent key) {
        if (key == null)
            throw new NullPointerException(); // don't postpone errors
        Comparator<? super K> cmp = comparator;
        outer: for (;;) {
            for (Node<K,V> b = findPredecessor(key, cmp), n = b.next();;) {
                AnyPersistent v; int c;
                if (n == null)
                    break outer;
                Node<K,V> f = n.next();
                if (n != b.next())                // inconsistent read
                    break;
                if ((v = n.value()) == null) {    // n is deleted
                    //n.helpDelete(b, f);
                    break;
                }
                if (b.value() == null || v == n)  // b is deleted
                    break;
                if ((c = cpr(cmp, key, n.key())) == 0)
                    return n;
                if (c < 0)
                    break outer;
                b = n;
                n = f;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked") 
    private V doGet(Object key) {
        if (key == null)
            throw new NullPointerException();
        Comparator<? super K> cmp = comparator;
        if (!(key instanceof AnyPersistent)) { 
            if (sister_comparator == null) {
                sister_comparator = (Object x, Object y) -> {
                    return (((ComparableWith)y).compareWith(x) * -1); //flip back
                };
            }
            cmp = sister_comparator;
        }
        outer: for (;;) {
            for (Node<K,V> b = findPredecessor(key, cmp), n = b.next();;) {
                AnyPersistent v; int c;
                if (n == null)
                    break outer;
                Node<K,V> f = n.next();
                if (n != b.next())                // inconsistent read
                    break;
                //if ((v = n.value()) == null) {    // n is deleted
                if (n.hasNullValue()) {    // n is deleted
                    //n.helpDelete(b, f);
                    break;
                }
                if (b.hasNullValue() || n.isMarker())  // b is deleted
                //if (b.value() == null || (v = n.value()) == n)
                    break;
                if ((c = cpr(cmp, key, n.key())) == 0) {
                    @SuppressWarnings("unchecked") V vv = (V)(n.value());
                    return vv;
                }
                if (c < 0)
                    break outer;
                b = n;
                n = f;
            }
        }
        return null;
    }

    public String toString() {
        Iterator<Entry<K,V>> i = entrySet().iterator();
        if (! i.hasNext())
            return "{}";

        StringBuilder sb = new StringBuilder();
        sb.append('{');
        for (;;) {
            Entry<K,V> e = i.next();
            K key = e.getKey();
            V value = e.getValue();
            sb.append(key   == this ? "(this Map)" : key);
            sb.append('=');
            sb.append(value == this ? "(this Map)" : value);
            if (! i.hasNext())
                return sb.append('}').toString();
            sb.append(',').append(' ');
        }
    }

    /* ---------------- Insertion -------------- */

    private V doPut(K key, V value, boolean onlyIfAbsent) {
        Node<K,V> z;             // added node
        if (key == null)
            throw new NullPointerException();
        Comparator<? super K> cmp = comparator;
        outer: for (;;) {
            for (Node<K,V> b = findPredecessor(key, cmp), n = b.next();;) {

                if (n != null) {
                    AnyPersistent v; int c;
                    Node<K,V> f = n.next();
                    if (n != b.next())               // inconsistent read
                        break;
                    //if ((v = n.value()) == null) {   // n is deleted
                    if (n.hasNullValue()) {   // n is deleted
                        //n.helpDelete(b, f);
                        break;
                    }
                    if (b.value() == null || n.isMarker()) // b is deleted
                    //if (b.value() == null || v == n)
                        break;
                    if ((c = cpr(cmp, key, n.key())) > 0) {
                        b = n;
                        n = f;
                        continue;
                    }
                    if (c == 0) {
                        v = n.value();
                        if (onlyIfAbsent || n.casValue(v, value)) {
                            @SuppressWarnings("unchecked") V vv = (V)v;
                            return vv;
                        }
                        break; // restart if lost race to replace value
                    }
                    // else c < 0; fall through
                }

                z = new Node<K,V>(key, value, n);
                if (!b.casNext(n, z))
                    break;         // restart if lost race to append to b
                 break outer;
             }
        }

        //int rnd = ThreadLocalRandom.current().nextSecondarySeed();
        int rnd = new Random().nextInt();
        if ((rnd & 0x80000001) == 0) { // test highest and lowest bits
            int level = 1, max;
            while (((rnd >>>= 1) & 1) != 0)
                ++level;
            Index<K,V> idx = null;
            HeadIndex<K,V> h = head();
            if (level <= (max = h.level())) {
                for (int i = 1; i <= level; ++i)
                    idx = new Index<K,V>(z, idx, null);
            }
            else { // try to grow by one level
                level = max + 1; // hold in array and later pick the one to use
                @SuppressWarnings("unchecked")Index<K,V>[] idxs =
                    (Index<K,V>[])new Index<?,?>[level+1];
                for (int i = 1; i <= level; ++i)
                    idxs[i] = idx = new Index<K,V>(z, idx, null);
                for (;;) {
                    h = head();
                    int oldLevel = h.level();
                    if (level <= oldLevel) // lost race to add level
                        break;
                    HeadIndex<K,V> newh = h;
                    Node<K,V> oldbase = h.node();
                    for (int j = oldLevel+1; j <= level; ++j)
                        newh = new HeadIndex<K,V>(oldbase, newh, idxs[j], j);
                    if (casHead(h, newh)) {
                        h = newh;
                        idx = idxs[level = oldLevel];
                        break;
                    }
                }
            }
            // find insertion points and splice in
            splice: for (int insertionLevel = level;;) {
                int j = h.level();
                for (Index<K,V> q = h, r = q.right(), t = idx;;) {
                    if (q == null || t == null)
                        break splice;
                    if (r != null) {
                        Node<K,V> n = r.node();
                        // compare before deletion check avoids needing recheck
                        int c = cpr(cmp, key, n.key());
                        //if (n.value() == null) {
                        if (n.hasNullValue()) {
                            if (!q.unlink(r))
                                break;
                            r = q.right();
                            continue;
                        }
                        if (c > 0) {
                            q = r;
                            r = r.right();
                            continue;
                        }
                    }
        
                    if (j == insertionLevel) {
                        if (!q.link(r, t))
                            break; // restart
                        //if (t.node().value() == null) {
                        if (t.node().hasNullValue()) {
                            findNode(key);
                            break splice;
                        }

                    if (--insertionLevel == 0)
                        break splice;
                    }
        
                    if (--j >= insertionLevel && j < level)
                        t = t.down();
                    q = q.down();
                    r = q.right();
                }
            }
        }
        return null;
    }

    /* ---------------- Deletion -------------- */

    final V doRemove(AnyPersistent key, AnyPersistent value) {
        if (key == null)
            throw new NullPointerException();
        Comparator<? super K> cmp = comparator;
        outer: for (;;) {
            for (Node<K,V> b = findPredecessor(key, cmp), n = b.next();;) {
                AnyPersistent v; int c;
                if (n == null)
                    break outer;
                Node<K,V> f = n.next();
                if (n != b.next())                    // inconsistent read
                    break;
                if ((v = n.value()) == null) {        // n is deleted
                //if (n.hasNullValue()) {        // n is deleted
                    //n.helpDelete(b, f);
                    break;
                }
                //if (b.hasNullValue() || n.isMarker())      // b is deleted
                if (b.value() == null || v == n)
                    break;
                if ((c = cpr(cmp, key, n.key())) < 0)
                    break outer;
                if (c > 0) {
                    b = n;
                    n = f;
                    continue;
                }
                //v = n.value();
                if (value != null && !value.equals(v))
                    break outer;
                if (!n.casValue(v, null))
                    break;
                if (!n.appendMarker(f) || !b.casNext(n, f))
                    findNode(key);                  // retry via findNode
                else {
                    findPredecessor(key, cmp);      // clean index
                    if (head().right() == null)
                        tryReduceLevel();
                }
                @SuppressWarnings("unchecked") V vv = (V)v;
                return vv;
            }
        }
        return null;
    }

    private void tryReduceLevel() {
        HeadIndex<K,V> h = head();
        HeadIndex<K,V> d;
        HeadIndex<K,V> e;
        if (h.level() > 3 &&
            (d = (HeadIndex<K,V>)h.down()) != null &&
            (e = (HeadIndex<K,V>)d.down()) != null &&
            e.right() == null &&
            d.right() == null &&
            h.right() == null &&
            casHead(h, d) && // try to set
            h.right() != null) // recheck
            casHead(d, h);   // try to backout
    }

    /* ---------------- Finding and removing first element -------------- */

    final Node<K,V> findFirst() {
        for (Node<K,V> b, n;;) {
            if ((n = (b = head().node()).next()) == null)
                return null;
            if (!n.hasNullValue())
                return n;
            //n.helpDelete(b, n.next());
        }
    }

    private Map.Entry<K,V> doRemoveFirstEntry() {
        for (Node<K,V> b, n;;) {
            if ((n = (b = head().node()).next()) == null)
                return null;
            Node<K,V> f = n.next();
            if (n != b.next())
                continue;
            AnyPersistent v = n.value();
            if (v == null) {
                //n.helpDelete(b, f);
                continue;
            }
            if (!n.casValue(v, null))
                continue;
            if (!n.appendMarker(f) || !b.casNext(n, f))
                findFirst(); // retry
            clearIndexToFirst();
            @SuppressWarnings("unchecked") V vv = (V)v;
            return new AbstractMap.SimpleImmutableEntry<K,V>(n.key(), vv);
        }
    }

    private void clearIndexToFirst() {
        for (;;) {
            for (Index<K,V> q = head();;) {
                Index<K,V> r = q.right();
                if (r != null && r.indexesDeletedNode() && !q.unlink(r))
                    break;
                if ((q = q.down()) == null) {
                    if (head().right() == null)
                        tryReduceLevel();
                    return;
                }
            }
        }
    }

    private Map.Entry<K,V> doRemoveLastEntry() {
        for (;;) {
            Node<K,V> b = findPredecessorOfLast();
            Node<K,V> n = b.next();
            if (n == null) {
                if (b.isBaseHeader())               // empty
                    return null;
                else
                    continue; // all b's successors are deleted; retry
            }
            for (;;) {
                Node<K,V> f = n.next();
                if (n != b.next())                    // inconsistent read
                    break;
                AnyPersistent v = n.value();
                if (v == null) {                    // n is deleted
                    //n.helpDelete(b, f);
                    break;
                }
                if (b.value() == null || v == n)      // b is deleted
                    break;
                if (f != null) {
                    b = n;
                    n = f;
                    continue;
                }
                if (!n.casValue(v, null))
                    break;
                K key = n.key();
                if (!n.appendMarker(f) || !b.casNext(n, f))
                    findNode(key);                  // retry via findNode
                else {                              // clean index
                    findPredecessor(key, comparator);
                    if (head().right() == null)
                        tryReduceLevel();
                }
                @SuppressWarnings("unchecked") V vv = (V)v;
                return new AbstractMap.SimpleImmutableEntry<K,V>(key, vv);
            }
        }
    }

    /* ---------------- Finding and removing last element -------------- */

    final Node<K,V> findLast() {
        /*
         * findPredecessor can't be used to traverse index level
         * because this doesn't use comparisons.  So traversals of
         * both levels are folded together.
         */
        Index<K,V> q = head();
        for (;;) {
            Index<K,V> d, r;
            if ((r = q.right()) != null) {
                if (r.indexesDeletedNode()) {
                    q.unlink(r);
                    q = head(); // restart
                }
                else
                    q = r;
            } else if ((d = q.down()) != null) {
                q = d;
            } else {
                for (Node<K,V> b = q.node(), n = b.next();;) {
                    if (n == null)
                        return b.isBaseHeader() ? null : b;
                    Node<K,V> f = n.next();            // inconsistent read
                    if (n != b.next())
                        break;
                    AnyPersistent v = n.value();
                    if (v == null) {                 // n is deleted
                        //n.helpDelete(b, f);
                        break;
                    }
                    if (b.value() == null || v == n)      // b is deleted
                        break;
                    b = n;
                    n = f;
                }
                q = head(); // restart
            }
        }
    }

    private Node<K,V> findPredecessorOfLast() {
        for (;;) {
            for (Index<K,V> q = head();;) {
                Index<K,V> d, r;
                if ((r = q.right()) != null) {
                    if (r.indexesDeletedNode()) {
                        q.unlink(r);
                        break;    // must restart
                    }
                    // proceed as far across as possible without overshooting
                    if (r.node().next() != null) {
                        q = r;
                        continue;
                    }
                }
                if ((d = q.down()) != null)
                    q = d;
                else
                    return q.node();
            }
        }
    }

    /* ---------------- Relational operations -------------- */

    private static final int EQ = 1;
    private static final int LT = 2;
    private static final int GT = 0; // Actually checked as !LT

    final Node<K,V> findNear(K key, int rel, Comparator<? super K> cmp) {
        if (key == null)
            throw new NullPointerException();
        for (;;) {
            for (Node<K,V> b = findPredecessor(key, cmp), n = b.next();;) {
                AnyPersistent v;
                if (n == null)
                    return ((rel & LT) == 0 || b.isBaseHeader()) ? null : b;
                Node<K,V> f = n.next();
                if (n != b.next())                  // inconsistent read
                    break;
                if ((v = n.value()) == null) {      // n is deleted
                    //n.helpDelete(b, f);
                    break;
                }
                if (b.value() == null || v == n)      // b is deleted
                    break;
                int c = cpr(cmp, key, n.key());
                if ((c == 0 && (rel & EQ) != 0) ||
                    (c <  0 && (rel & LT) == 0))
                    return n;
                if ( c <= 0 && (rel & LT) != 0)
                    return b.isBaseHeader() ? null : b;
                b = n;
                n = f;
            }
        }
    }

    final AbstractMap.SimpleImmutableEntry<K,V> getNear(K key, int rel) {
        Comparator<? super K> cmp = comparator;
        for (;;) {
            Node<K,V> n = findNear(key, rel, cmp);
            if (n == null)
                return null;
            AbstractMap.SimpleImmutableEntry<K,V> e = n.createSnapshot();
            if (e != null)
                return e;
        }
    }


    /* ---------------- Constructors -------------- */

    /**
     * Constructs a new, empty map, sorted according to the
     * {@linkplain Comparable natural ordering} of the keys.
     */
    public PersistentSkipListMap() {
        this(TYPE);
    }

    protected PersistentSkipListMap(ObjectType<? extends PersistentSkipListMap> type) {
        super(type, (PersistentSkipListMap obj) -> {
        obj.initObjectField(HEAD, new PersistentAtomicReference<HeadIndex<K,V>>());
        });
        this.comparator = null;
        initialize();
    }

    public PersistentSkipListMap(ObjectPointer<? extends PersistentSkipListMap> p) {
        super(p);
        this.comparator = null;
    } 

    /**
     * Constructs a new, empty map, sorted according to the specified
     * comparator.
     *
     * @param comparator the comparator that will be used to order this map.
     *        If {@code null}, the {@linkplain Comparable natural
     *        ordering} of the keys will be used.
     */
    public PersistentSkipListMap(Comparator<? super K> comparator) {
        super(TYPE, (PersistentSkipListMap obj) -> {
            obj.initObjectField(HEAD, new PersistentAtomicReference<HeadIndex<K,V>>());
        });
        this.comparator = comparator;
        initialize();
    }

    /**
     * Constructs a new map containing the same mappings as the given map,
     * sorted according to the {@linkplain Comparable natural ordering} of
     * the keys.
     *
     * @param  m the map whose mappings are to be placed in this map
     * @throws ClassCastException if the keys in {@code m} are not
     *         {@link Comparable}, or are not mutually comparable
     * @throws NullPointerException if the specified map or any of its keys
     *         or values are null
     */
    public PersistentSkipListMap(Map<? extends K, ? extends V> m) {
        super(TYPE, (PersistentSkipListMap obj) -> {
            obj.initObjectField(HEAD, new PersistentAtomicReference<HeadIndex<K,V>>());
        });
        this.comparator = null;
        initialize();
        putAll(m);
    }

    /**
     * Constructs a new map containing the same mappings and using the
     * same ordering as the specified sorted map.
     *
     * @param m the sorted map whose mappings are to be placed in this
     *        map, and whose comparator is to be used to sort this map
     * @throws NullPointerException if the specified sorted map or any of
     *         its keys or values are null
     */
    public PersistentSkipListMap(SortedMap<K, ? extends V> m) {
        super(TYPE, (PersistentSkipListMap obj) -> {
            obj.initObjectField(HEAD, new PersistentAtomicReference<HeadIndex<K,V>>());
        });
        this.comparator = m.comparator();
        initialize();
        buildFromSorted(m);
    }

    /**
     * Returns a shallow copy of this {@code PersistentSkipListMap}
     * instance. (The keys and values themselves are not cloned.)
     *
     * @return a shallow copy of this map
     */
   /* public PersistentSkipListMap<K,V> clone() {
        try {
            @SuppressWarnings("unchecked")
            PersistentSkipListMap<K,V> clone =
                (PersistentSkipListMap<K,V>) super.clone();
            clone.initialize();
            clone.buildFromSorted(this);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }*/

    private void buildFromSorted(SortedMap<K, ? extends V> map) {
        if (map == null)
            throw new NullPointerException();

        HeadIndex<K,V> h = head();
        Node<K,V> basepred = h.node();

        // Track the current rightmost node at each level. Uses an
        // ArrayList to avoid committing to initial or maximum level.
        ArrayList<Index<K,V>> preds = new ArrayList<Index<K,V>>();

        // initialize
        for (int i = 0; i <= h.level(); ++i)
            preds.add(null);
        Index<K,V> q = h;
        for (int i = h.level(); i > 0; --i) {
            preds.set(i, q);
            q = q.down();
        }

        Iterator<? extends Map.Entry<? extends K, ? extends V>> it =
            map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<? extends K, ? extends V> e = it.next();
            int rnd = ThreadLocalRandom.current().nextInt();
            int j = 0;
            if ((rnd & 0x80000001) == 0) {
                do {
                    ++j;
                } while (((rnd >>>= 1) & 1) != 0);
                if (j > h.level()) j = h.level() + 1;
            }
            K k = e.getKey();
            V v = e.getValue();
            if (k == null || v == null)
                throw new NullPointerException();
            Node<K,V> z = new Node<K,V>(k, v, null);
            basepred.next(z);
            basepred = z;
            if (j > 0) {
                Index<K,V> idx = null;
                for (int i = 1; i <= j; ++i) {
                    idx = new Index<K,V>(z, idx, null);
                    if (i > h.level())
                        h = new HeadIndex<K,V>(h.node(), h, idx, i);

                    if (i < preds.size()) {
                        preds.get(i).right(idx);
                        preds.set(i, idx);
                    } else
                        preds.add(idx);
                }
            }
        }
        head(h);
    }

    /* ------ Map API methods ------ */

    @SuppressWarnings("unchecked")
    public boolean containsKey(Object key) {
        return doGet((K)key) != null;
    }

    @SuppressWarnings("unchecked")
    public <L, K extends ComparableWith<L>> boolean containsKey(L key, Class<K> cls) {
        return doGet(key) != null;
    }

    @SuppressWarnings("unchecked")
    public V get(Object key) {
        return doGet((K)key);
    }

    @SuppressWarnings("unchecked")
    public <L, K extends ComparableWith<L>> V get(L key, Class<K> cls) {
        return doGet(key);
    }

    public V getOrDefault(AnyPersistent key, V defaultValue) {
        V v;
        return (v = doGet(key)) == null ? defaultValue : v;
    }

    public V put(K key, V value) {
        if (value == null)
            throw new NullPointerException();
        return doPut(key, value, false);
    }

    @SuppressWarnings("unchecked")
    public V remove(Object key) {
        return doRemove((K)key, null);
    }


    @SuppressWarnings("unchecked")
    public boolean containsValue(Object value) {
        if (value == null)
            throw new NullPointerException();
        for (Node<K,V> n = findFirst(); n != null; n = n.next()) {
            V v = n.getValidValue();
            if (v != null && ((V)value).equals(v))
                return true;
        }
        return false;
    }

    public int size() {
        long count = 0;
        for (Node<K,V> n = findFirst(); n != null; n = n.next()) {
            if (n.getValidValue() != null)
                ++count;
        }
        return (count >= Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) count;
    }

    public boolean isEmpty() {
        return findFirst() == null;
    }

    public void clear() {
        initialize();
    }

    public V computeIfAbsent(K key,
                             Function<? super K, ? extends V> mappingFunction) {
        if (key == null || mappingFunction == null)
            throw new NullPointerException();
        V v, p, r;
        if ((v = doGet(key)) == null &&
            (r = mappingFunction.apply(key)) != null)
            v = (p = doPut(key, r, true)) == null ? r : p;
        return v;
    }

    public V computeIfPresent(K key,
                              BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (key == null || remappingFunction == null)
            throw new NullPointerException();
        Node<K,V> n; AnyPersistent v;
        while ((n = findNode(key)) != null) {
            if ((v = n.value()) != null) {
                @SuppressWarnings("unchecked") V vv = (V) v;
                V r = remappingFunction.apply(key, vv);
                if (r != null) {
                    if (n.casValue(vv, r))
                        return r;
                }
                else if (doRemove(key, vv) != null)
                    break;
            }
        }
        return null;
    }

    public V compute(K key,
                     BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (key == null || remappingFunction == null)
            throw new NullPointerException();
        for (;;) {
            Node<K,V> n; AnyPersistent v; V r;
            if ((n = findNode(key)) == null) {
                if ((r = remappingFunction.apply(key, null)) == null)
                    break;
                if (doPut(key, r, true) == null)
                    return r;
            }
            else if ((v = n.value()) != null) {
                @SuppressWarnings("unchecked") V vv = (V) v;
                if ((r = remappingFunction.apply(key, vv)) != null) {
                    if (n.casValue(vv, r))
                        return r;
                }
                else if (doRemove(key, vv) != null)
                    break;
            }
        }
        return null;
    }

    public V merge(K key, V value,
                   BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        if (key == null || value == null || remappingFunction == null)
            throw new NullPointerException();
        for (;;) {
            Node<K,V> n; AnyPersistent v; V r;
            if ((n = findNode(key)) == null) {
                if (doPut(key, value, true) == null)
                    return value;
            }
            else if ((v = n.value()) != null) {
                @SuppressWarnings("unchecked") V vv = (V) v;
                if ((r = remappingFunction.apply(vv, value)) != null) {
                    if (n.casValue(vv, r))
                        return r;
                }
                else if (doRemove(key, vv) != null)
                    return null;
            }
        }
    }

    /* ---------------- View methods -------------- */

    public NavigableSet<K> keySet() {
        KeySet<K> ks = keySet;
        return (ks != null) ? ks : (keySet = new KeySet<K>(this));
    }

    public NavigableSet<K> navigableKeySet() {
        KeySet<K> ks = keySet;
        return (ks != null) ? ks : (keySet = new KeySet<K>(this));
    }

    public Collection<V> values() {
        Values<V> vs = values;
        return (vs != null) ? vs : (values = new Values<V>(this));
    }

    public Set<Map.Entry<K,V>> entrySet() {
        EntrySet<K,V> es = entrySet;
        return (es != null) ? es : (entrySet = new EntrySet<K,V>(this));
    }

    public ConcurrentNavigableMap<K,V> descendingMap() {
        ConcurrentNavigableMap<K,V> dm = descendingMap;
        return (dm != null) ? dm : (descendingMap = new SubMap<K,V>
                                    (this, null, false, null, false, true));
    }

    public NavigableSet<K> descendingKeySet() {
        return descendingMap().navigableKeySet();
    }

    /* ---------------- AbstractMap Overrides -------------- */

    public boolean equals(AnyPersistent o) {
        if (o == this)
            return true;
        if (!(o instanceof PersistentSkipListMap))
            return false;
        PersistentSkipListMap<?,?> m = (PersistentSkipListMap<?,?>) o;
        try {
            for (Map.Entry<K,V> e : this.entrySet())
                if (! e.getValue().equals(m.get(e.getKey())))
                    return false;
            for (Map.Entry<? extends AnyPersistent,? extends AnyPersistent> e : m.entrySet()) {
                AnyPersistent k = e.getKey();
                AnyPersistent v = e.getValue();
                if (k == null || v == null || !v.equals(get(k)))
                    return false;
            }
            return true;
        } catch (ClassCastException unused) {
            return false;
        } catch (NullPointerException unused) {
            return false;
        }
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        synchronized(head()) {
            Transaction.run(() -> {
                for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
                    put(e.getKey(), e.getValue());
            });
        }
    }

    /* ------ ConcurrentMap API methods ------ */

    public V putIfAbsent(K key, V value) {
        if (value == null)
            throw new NullPointerException();
        return doPut(key, value, true);
    }

    @SuppressWarnings("unchecked")
    public boolean remove(Object key, Object value) {
        if (key == null)
            throw new NullPointerException();
        return (V)value != null && doRemove((K)key, (V)value) != null;
    }

    public boolean replace(K key, V oldValue, V newValue) {
        if (key == null || oldValue == null || newValue == null)
            throw new NullPointerException();
        for (;;) {
            Node<K,V> n; AnyPersistent v;
            if ((n = findNode(key)) == null)
                return false;
            if ((v = n.value()) != null) {
                if (!oldValue.equals(v))
                    return false;
                if (n.casValue(v, newValue))
                    return true;
            }
        }
    }

    public V replace(K key, V value) {
        if (key == null || value == null)
            throw new NullPointerException();
        for (;;) {
            Node<K,V> n; AnyPersistent v;
            if ((n = findNode(key)) == null)
                return null;
            if ((v = n.value()) != null && n.casValue(v, value)) {
                @SuppressWarnings("unchecked") V vv = (V)v;
                return vv;
            }
        }
    }

    /* ------ SortedMap API methods ------ */

    public Comparator<? super K> comparator() {
        return comparator;
    }

    public K firstKey() {
        Node<K,V> n = findFirst();
        if (n == null)
            throw new NoSuchElementException();
        return n.key();
    }

    public K lastKey() {
        Node<K,V> n = findLast();
        if (n == null)
            throw new NoSuchElementException();
        return n.key();
    }

    public ConcurrentNavigableMap<K,V> subMap(K fromKey,
                                              boolean fromInclusive,
                                              K toKey,
                                              boolean toInclusive) {
        if (fromKey == null || toKey == null)
            throw new NullPointerException();
        return new SubMap<K,V>
            (this, fromKey, fromInclusive, toKey, toInclusive, false);
    }

    public ConcurrentNavigableMap<K,V> headMap(K toKey,
                                               boolean inclusive) {
        if (toKey == null)
            throw new NullPointerException();
        return new SubMap<K,V>
            (this, null, false, toKey, inclusive, false);
    }

    public ConcurrentNavigableMap<K,V> tailMap(K fromKey,
                                               boolean inclusive) {
        if (fromKey == null)
            throw new NullPointerException();
        return new SubMap<K,V>
            (this, fromKey, inclusive, null, false, false);
    }

    public ConcurrentNavigableMap<K,V> subMap(K fromKey, K toKey) {
        return subMap(fromKey, true, toKey, false);
    }

    public ConcurrentNavigableMap<K,V> headMap(K toKey) {
        return headMap(toKey, false);
    }

    public ConcurrentNavigableMap<K,V> tailMap(K fromKey) {
        return tailMap(fromKey, true);
    }

    /* ---------------- Relational operations -------------- */

    public Map.Entry<K,V> lowerEntry(K key) {
        return getNear(key, LT);
    }

    public K lowerKey(K key) {
        Node<K,V> n = findNear(key, LT, comparator);
        return (n == null) ? null : n.key();
    }

    public Map.Entry<K,V> floorEntry(K key) {
        return getNear(key, LT|EQ);
    }

    public K floorKey(K key) {
        Node<K,V> n = findNear(key, LT|EQ, comparator);
        return (n == null) ? null : n.key();
    }

    public Map.Entry<K,V> ceilingEntry(K key) {
        return getNear(key, GT|EQ);
    }

    public K ceilingKey(K key) {
        Node<K,V> n = findNear(key, GT|EQ, comparator);
        return (n == null) ? null : n.key();
    }

    public Map.Entry<K,V> higherEntry(K key) {
        return getNear(key, GT);
    }

    public K higherKey(K key) {
        Node<K,V> n = findNear(key, GT, comparator);
        return (n == null) ? null : n.key();
    }

    public Map.Entry<K,V> firstEntry() {
        for (;;) {
            Node<K,V> n = findFirst();
            if (n == null)
                return null;
            AbstractMap.SimpleImmutableEntry<K,V> e = n.createSnapshot();
            if (e != null)
                return e;
        }
    }

    public Map.Entry<K,V> lastEntry() {
        for (;;) {
            Node<K,V> n = findLast();
            if (n == null)
                return null;
            AbstractMap.SimpleImmutableEntry<K,V> e = n.createSnapshot();
            if (e != null)
                return e;
        }
    }

    public Map.Entry<K,V> pollFirstEntry() {
        return doRemoveFirstEntry();
    }

    public Map.Entry<K,V> pollLastEntry() {
        return doRemoveLastEntry();
    }

    /* ---------------- Iterators -------------- */

    abstract class Iter<T> implements Iterator<T> {
        /** the last node returned by next() */
        Node<K,V> lastReturned;
        /** the next node to return from next(); */
        Node<K,V> next;
        /** Cache of next value field to maintain weak consistency */
        V nextValue;

        /** Initializes ascending iterator for entire range. */
        Iter() {
            while ((next = findFirst()) != null) {
                AnyPersistent x = next.value();
                if (x != null && x != next) {
                    @SuppressWarnings("unchecked") V vv = (V)x;
                    nextValue = vv;
                    break;
                }
            }
        }

        public final boolean hasNext() {
            return next != null;
        }

        /** Advances next to higher entry. */
        final void advance() {
            if (next == null)
                throw new NoSuchElementException();
            lastReturned = next;
            while ((next = next.next()) != null) {
                AnyPersistent x = next.value();
                if (x != null && x != next) {
                    @SuppressWarnings("unchecked") V vv = (V)x;
                    nextValue = vv;
                    break;
                }
            }
        }

        public void remove() {
            Node<K,V> l = lastReturned;
            if (l == null)
                throw new IllegalStateException();
            // It would not be worth all of the overhead to directly
            // unlink from here. Using remove is fast enough.
            PersistentSkipListMap.this.remove(l.key());
            lastReturned = null;
        }

    }

    final class ValueIterator extends Iter<V> {
        public V next() {
            V v = nextValue;
            advance();
            return v;
        }
    }

    final class KeyIterator extends Iter<K> {
        public K next() {
            Node<K,V> n = next;
            advance();
            return n.key();
        }
    }

    final class EntryIterator extends Iter<Map.Entry<K,V>> {
        public Map.Entry<K,V> next() {
            Node<K,V> n = next;
            V v = nextValue;
            advance();
            return new AbstractMap.SimpleImmutableEntry<K,V>(n.key(), v);
        }
    }

    Iterator<K> keyIterator() {
        return new KeyIterator();
    }

    Iterator<V> valueIterator() {
        return new ValueIterator();
    }

    Iterator<Map.Entry<K,V>> entryIterator() {
        return new EntryIterator();
    }

    /* ---------------- View Classes -------------- */

    static final <E> List<E> toList(Collection<E> c) {
        // Using size() here would be a pessimization.
        ArrayList<E> list = new ArrayList<E>();
        for (E e : c)
            list.add(e);
        return list;
    }

    static final class KeySet<E extends AnyPersistent>
            extends AbstractSet<E> implements NavigableSet<E> {
        final ConcurrentNavigableMap<E,? extends AnyPersistent> m;
        KeySet(ConcurrentNavigableMap<E,? extends AnyPersistent> map) { m = map; }
        public int size() { return m.size(); }
        public boolean isEmpty() { return m.isEmpty(); }
        public boolean contains(Object o) { return m.containsKey(o); }
        public boolean remove(Object o) { return m.remove(o) != null; }
        public void clear() { m.clear(); }
        public E lower(E e) { return m.lowerKey(e); }
        public E floor(E e) { return m.floorKey(e); }
        public E ceiling(E e) { return m.ceilingKey(e); }
        public E higher(E e) { return m.higherKey(e); }
        public Comparator<? super E> comparator() { return m.comparator(); }
        public E first() { return m.firstKey(); }
        public E last() { return m.lastKey(); }
        public E pollFirst() {
            Map.Entry<E,?> e = m.pollFirstEntry();
            return (e == null) ? null : e.getKey();
        }
        public E pollLast() {
            Map.Entry<E,?> e = m.pollLastEntry();
            return (e == null) ? null : e.getKey();
        }
        @SuppressWarnings("unchecked")
        public Iterator<E> iterator() {
            if (m instanceof PersistentSkipListMap)
                return ((PersistentSkipListMap<E,AnyPersistent>)m).keyIterator();
            else
                return ((PersistentSkipListMap.SubMap<E,AnyPersistent>)m).keyIterator();
        }
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof Set))
                return false;
            Collection<?> c = (Collection<?>) o;
            try {
                return containsAll(c) && c.containsAll(this);
            } catch (ClassCastException unused) {
                return false;
            } catch (NullPointerException unused) {
                return false;
            }
        }
        public Object[] toArray()     { return toList(this).toArray();  }
        public <T> T[] toArray(T[] a) { return toList(this).toArray(a); }
        public Iterator<E> descendingIterator() {
            return descendingSet().iterator();
        }
        public NavigableSet<E> subSet(E fromElement,
                                      boolean fromInclusive,
                                      E toElement,
                                      boolean toInclusive) {
            return new KeySet<E>(m.subMap(fromElement, fromInclusive,
                                          toElement,   toInclusive));
        }
        public NavigableSet<E> headSet(E toElement, boolean inclusive) {
            return new KeySet<E>(m.headMap(toElement, inclusive));
        }
        public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
            return new KeySet<E>(m.tailMap(fromElement, inclusive));
        }
        public NavigableSet<E> subSet(E fromElement, E toElement) {
            return subSet(fromElement, true, toElement, false);
        }
        public NavigableSet<E> headSet(E toElement) {
            return headSet(toElement, false);
        }
        public NavigableSet<E> tailSet(E fromElement) {
            return tailSet(fromElement, true);
        }
        public NavigableSet<E> descendingSet() {
            return new KeySet<E>(m.descendingMap());
        }
        @SuppressWarnings("unchecked")
        public Spliterator<E> spliterator() {
            if (m instanceof PersistentSkipListMap)
                return ((PersistentSkipListMap<E,?>)m).keySpliterator();
            else
                return (Spliterator<E>)((SubMap<E,?>)m).keyIterator();
        }
    }

    static final class Values<E extends AnyPersistent> extends AbstractCollection<E> {
        final ConcurrentNavigableMap<?, E> m;
        Values(ConcurrentNavigableMap<?, E> map) {
            m = map;
        }
        @SuppressWarnings("unchecked")
        public Iterator<E> iterator() {
            if (m instanceof PersistentSkipListMap)
                return ((PersistentSkipListMap<?,E>)m).valueIterator();
            else
                return ((SubMap<?,E>)m).valueIterator();
        }
        public boolean isEmpty() {
            return m.isEmpty();
        }
        public int size() {
            return m.size();
        }
        public boolean contains(Object o) {
            return m.containsValue(o);
        }
        public void clear() {
            m.clear();
        }
        public Object[] toArray()     { return toList(this).toArray();  }
        public <T> T[] toArray(T[] a) { return toList(this).toArray(a); }
        @SuppressWarnings("unchecked")
        public Spliterator<E> spliterator() {
            if (m instanceof PersistentSkipListMap)
                return ((PersistentSkipListMap<?,E>)m).valueSpliterator();
            else
                return (Spliterator<E>)((SubMap<?,E>)m).valueIterator();
        }
    }

    static final class EntrySet<K1 extends AnyPersistent,V1 extends AnyPersistent> extends AbstractSet<Map.Entry<K1,V1>> {
        final ConcurrentNavigableMap<K1, V1> m;
        EntrySet(ConcurrentNavigableMap<K1, V1> map) {
            m = map;
        }
        @SuppressWarnings("unchecked")
        public Iterator<Map.Entry<K1,V1>> iterator() {
            if (m instanceof PersistentSkipListMap)
                return ((PersistentSkipListMap<K1,V1>)m).entryIterator();
            else
                return ((SubMap<K1,V1>)m).entryIterator();
        }

        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> e = (Map.Entry<?,?>)o;
            V1 v = m.get(e.getKey());
            return v != null && v.equals(e.getValue());
        }
        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> e = (Map.Entry<?,?>)o;
            return m.remove(e.getKey(),
                            e.getValue());
        }
        public boolean isEmpty() {
            return m.isEmpty();
        }
        public int size() {
            return m.size();
        }
        public void clear() {
            m.clear();
        }
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof Set))
                return false;
            Collection<?> c = (Collection<?>) o;
            try {
                return containsAll(c) && c.containsAll(this);
            } catch (ClassCastException unused) {
                return false;
            } catch (NullPointerException unused) {
                return false;
            }
        }
        public Object[] toArray()     { return toList(this).toArray();  }
        public <T> T[] toArray(T[] a) { return toList(this).toArray(a); }
        @SuppressWarnings("unchecked")
        public Spliterator<Map.Entry<K1,V1>> spliterator() {
            if (m instanceof PersistentSkipListMap)
                return ((PersistentSkipListMap<K1,V1>)m).entrySpliterator();
            else
                return (Spliterator<Map.Entry<K1,V1>>)
                    ((SubMap<K1,V1>)m).entryIterator();
        }
    }

    static final class SubMap<K extends AnyPersistent,V extends AnyPersistent> extends AbstractMap<K,V>
        implements ConcurrentNavigableMap<K,V>, Cloneable {

    /** Underlying map */
        private final PersistentSkipListMap<K,V> m;
        /** lower bound key, or null if from start */
        private final K lo;
        /** upper bound key, or null if to end */
        private final K hi;
        /** inclusion flag for lo */
        private final boolean loInclusive;
        /** inclusion flag for hi */
        private final boolean hiInclusive;
        /** direction */
        private final boolean isDescending;

        // Lazily initialized view holders
        private transient KeySet<K> keySetView;
        private transient Set<Map.Entry<K,V>> entrySetView;
        private transient Collection<V> valuesView;

        SubMap(PersistentSkipListMap<K,V> map,
               K fromKey, boolean fromInclusive,
               K toKey, boolean toInclusive,
               boolean isDescending) {
            Comparator<? super K> cmp = map.comparator;
            if (fromKey != null && toKey != null &&
                cpr(cmp, fromKey, toKey) > 0)
                throw new IllegalArgumentException("inconsistent range");
            this.m = map;
            this.lo = fromKey;
            this.hi = toKey;
            this.loInclusive = fromInclusive;
            this.hiInclusive = toInclusive;
            this.isDescending = isDescending;
        }

        boolean tooLow(AnyPersistent key, Comparator<? super K> cmp) {
            int c;
            return (lo != null && ((c = cpr(cmp, key, lo)) < 0 ||
                                   (c == 0 && !loInclusive)));
        }

        boolean tooHigh(AnyPersistent key, Comparator<? super K> cmp) {
            int c;
            return (hi != null && ((c = cpr(cmp, key, hi)) > 0 ||
                                   (c == 0 && !hiInclusive)));
        }

        boolean inBounds(AnyPersistent key, Comparator<? super K> cmp) {
            return !tooLow(key, cmp) && !tooHigh(key, cmp);
        }

        void checkKeyBounds(K key, Comparator<? super K> cmp) {
            if (key == null)
                throw new NullPointerException();
            if (!inBounds(key, cmp))
                throw new IllegalArgumentException("key out of range");
        }

        boolean isBeforeEnd(PersistentSkipListMap.Node<K,V> n,
                            Comparator<? super K> cmp) {
            if (n == null)
                return false;
            if (hi == null)
                return true;
            K k = n.key();
            if (k == null) // pass by markers and headers
                return true;
            int c = cpr(cmp, k, hi);
            if (c > 0 || (c == 0 && !hiInclusive))
                return false;
            return true;
        }

        PersistentSkipListMap.Node<K,V> loNode(Comparator<? super K> cmp) {
            if (lo == null)
                return m.findFirst();
            else if (loInclusive)
                return m.findNear(lo, GT|EQ, cmp);
            else
                return m.findNear(lo, GT, cmp);
        }

        PersistentSkipListMap.Node<K,V> hiNode(Comparator<? super K> cmp) {
            if (hi == null)
                return m.findLast();
            else if (hiInclusive)
                return m.findNear(hi, LT|EQ, cmp);
            else
                return m.findNear(hi, LT, cmp);
        }

        K lowestKey() {
            Comparator<? super K> cmp = m.comparator;
            PersistentSkipListMap.Node<K,V> n = loNode(cmp);
            if (isBeforeEnd(n, cmp))
                return n.key();
            else
                throw new NoSuchElementException();
        }

        K highestKey() {
            Comparator<? super K> cmp = m.comparator;
            PersistentSkipListMap.Node<K,V> n = hiNode(cmp);
            if (n != null) {
                K last = n.key();
                if (inBounds(last, cmp))
                    return last;
            }
            throw new NoSuchElementException();
        }

        Map.Entry<K,V> lowestEntry() {
            Comparator<? super K> cmp = m.comparator;
            for (;;) {
                PersistentSkipListMap.Node<K,V> n = loNode(cmp);
                if (!isBeforeEnd(n, cmp))
                    return null;
                Map.Entry<K,V> e = n.createSnapshot();
                if (e != null)
                    return e;
            }
        }

        Map.Entry<K,V> highestEntry() {
            Comparator<? super K> cmp = m.comparator;
            for (;;) {
                PersistentSkipListMap.Node<K,V> n = hiNode(cmp);
                if (n == null || !inBounds(n.key(), cmp))
                    return null;
                Map.Entry<K,V> e = n.createSnapshot();
                if (e != null)
                    return e;
            }
        }

        Map.Entry<K,V> removeLowest() {
            Comparator<? super K> cmp = m.comparator;
            for (;;) {
                Node<K,V> n = loNode(cmp);
                if (n == null)
                    return null;
                K k = n.key();
                if (!inBounds(k, cmp))
                    return null;
                V v = m.doRemove(k, null);
                if (v != null)
                    return new AbstractMap.SimpleImmutableEntry<K,V>(k, v);
            }
        }

        Map.Entry<K,V> removeHighest() {
            Comparator<? super K> cmp = m.comparator;
            for (;;) {
                Node<K,V> n = hiNode(cmp);
                if (n == null)
                    return null;
                K k = n.key();
                if (!inBounds(k, cmp))
                    return null;
                V v = m.doRemove(k, null);
                if (v != null)
                    return new AbstractMap.SimpleImmutableEntry<K,V>(k, v);
            }
        }

        Map.Entry<K,V> getNearEntry(K key, int rel) {
            Comparator<? super K> cmp = m.comparator;
            if (isDescending) { // adjust relation for direction
                if ((rel & LT) == 0)
                    rel |= LT;
                else
                    rel &= ~LT;
            }
            if (tooLow(key, cmp))
                return ((rel & LT) != 0) ? null : lowestEntry();
            if (tooHigh(key, cmp))
                return ((rel & LT) != 0) ? highestEntry() : null;
            for (;;) {
                Node<K,V> n = m.findNear(key, rel, cmp);
                if (n == null || !inBounds(n.key(), cmp))
                    return null;
                K k = n.key();
                V v = n.getValidValue();
                if (v != null)
                    return new AbstractMap.SimpleImmutableEntry<K,V>(k, v);
            }
        }

        K getNearKey(K key, int rel) {
            Comparator<? super K> cmp = m.comparator;
            if (isDescending) { // adjust relation for direction
                if ((rel & LT) == 0)
                    rel |= LT;
                else
                    rel &= ~LT;
            }
            if (tooLow(key, cmp)) {
                if ((rel & LT) == 0) {
                    PersistentSkipListMap.Node<K,V> n = loNode(cmp);
                    if (isBeforeEnd(n, cmp))
                        return n.key();
                }
                return null;
            }
            if (tooHigh(key, cmp)) {
                if ((rel & LT) != 0) {
                    PersistentSkipListMap.Node<K,V> n = hiNode(cmp);
                    if (n != null) {
                        K last = n.key();
                        if (inBounds(last, cmp))
                            return last;
                    }
                }
                return null;
            }
            for (;;) {
                Node<K,V> n = m.findNear(key, rel, cmp);
                if (n == null || !inBounds(n.key(), cmp))
                    return null;
                K k = n.key();
                V v = n.getValidValue();
                if (v != null)
                    return k;
            }
        }

        public boolean containsKey(AnyPersistent key) {
            if (key == null) throw new NullPointerException();
            return inBounds(key, m.comparator) && m.containsKey(key);
        }

        public V get(AnyPersistent key) {
            if (key == null) throw new NullPointerException();
            return (!inBounds(key, m.comparator)) ? null : m.get(key);
        }

        public V put(K key, V value) {
            checkKeyBounds(key, m.comparator);
            return m.put(key, value);
        }

        public V remove(AnyPersistent key) {
            return (!inBounds(key, m.comparator)) ? null : m.remove(key);
        }

        public int size() {
            Comparator<? super K> cmp = m.comparator;
            long count = 0;
            for (PersistentSkipListMap.Node<K,V> n = loNode(cmp);
                 isBeforeEnd(n, cmp);
                 n = n.next()) {
                if (n.getValidValue() != null)
                    ++count;
            }
            return count >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)count;
        }

        public boolean isEmpty() {
            Comparator<? super K> cmp = m.comparator;
            return !isBeforeEnd(loNode(cmp), cmp);
        }

        public boolean containsValue(Object value) {
            if (value == null)
                throw new NullPointerException();
            Comparator<? super K> cmp = m.comparator;
            for (PersistentSkipListMap.Node<K,V> n = loNode(cmp);
                 isBeforeEnd(n, cmp);
                 n = n.next()) {
                V v = n.getValidValue();
                if (v != null && value.equals(v))
                    return true;
            }
            return false;
        }

        public void clear() {
            Comparator<? super K> cmp = m.comparator;
            for (PersistentSkipListMap.Node<K,V> n = loNode(cmp);
                 isBeforeEnd(n, cmp);
                 n = n.next()) {
                if (n.getValidValue() != null)
                    m.remove(n.key());
            }
        }

        public V putIfAbsent(K key, V value) {
            checkKeyBounds(key, m.comparator);
            return m.putIfAbsent(key, value);
        }

        @SuppressWarnings("unchecked")
        public boolean remove(Object key, Object value) {
            return inBounds((K)key, m.comparator) && m.remove((K)key, (V)value);
        }

        public boolean replace(K key, V oldValue, V newValue) {
            checkKeyBounds(key, m.comparator);
            return m.replace(key, oldValue, newValue);
        }

        public V replace(K key, V value) {
            checkKeyBounds(key, m.comparator);
            return m.replace(key, value);
        }

        public Comparator<? super K> comparator() {
            Comparator<? super K> cmp = m.comparator();
            if (isDescending)
                return Collections.reverseOrder(cmp);
            else
                return cmp;
        }

        SubMap<K,V> newSubMap(K fromKey, boolean fromInclusive,
                              K toKey, boolean toInclusive) {
            Comparator<? super K> cmp = m.comparator;
            if (isDescending) { // flip senses
                K tk = fromKey;
                fromKey = toKey;
                toKey = tk;
                boolean ti = fromInclusive;
                fromInclusive = toInclusive;
                toInclusive = ti;
            }
            if (lo != null) {
                if (fromKey == null) {
                    fromKey = lo;
                    fromInclusive = loInclusive;
                }
                else {
                    int c = cpr(cmp, fromKey, lo);
                    if (c < 0 || (c == 0 && !loInclusive && fromInclusive))
                        throw new IllegalArgumentException("key out of range");
                }
            }
            if (hi != null) {
                if (toKey == null) {
                    toKey = hi;
                    toInclusive = hiInclusive;
                }
                else {
                    int c = cpr(cmp, toKey, hi);
                    if (c > 0 || (c == 0 && !hiInclusive && toInclusive))
                        throw new IllegalArgumentException("key out of range");
                }
            }
            return new SubMap<K,V>(m, fromKey, fromInclusive,
                                   toKey, toInclusive, isDescending);
        }

        public SubMap<K,V> subMap(K fromKey, boolean fromInclusive,
                                  K toKey, boolean toInclusive) {
            if (fromKey == null || toKey == null)
                throw new NullPointerException();
            return newSubMap(fromKey, fromInclusive, toKey, toInclusive);
        }

        public SubMap<K,V> headMap(K toKey, boolean inclusive) {
            if (toKey == null)
                throw new NullPointerException();
            return newSubMap(null, false, toKey, inclusive);
        }

        public SubMap<K,V> tailMap(K fromKey, boolean inclusive) {
            if (fromKey == null)
                throw new NullPointerException();
            return newSubMap(fromKey, inclusive, null, false);
        }

        public SubMap<K,V> subMap(K fromKey, K toKey) {
            return subMap(fromKey, true, toKey, false);
        }

        public SubMap<K,V> headMap(K toKey) {
            return headMap(toKey, false);
        }

        public SubMap<K,V> tailMap(K fromKey) {
            return tailMap(fromKey, true);
        }

        public SubMap<K,V> descendingMap() {
            return new SubMap<K,V>(m, lo, loInclusive,
                                   hi, hiInclusive, !isDescending);
        }

        public Map.Entry<K,V> ceilingEntry(K key) {
            return getNearEntry(key, GT|EQ);
        }

        public K ceilingKey(K key) {
            return getNearKey(key, GT|EQ);
        }

        public Map.Entry<K,V> lowerEntry(K key) {
            return getNearEntry(key, LT);
        }

        public K lowerKey(K key) {
            return getNearKey(key, LT);
        }

        public Map.Entry<K,V> floorEntry(K key) {
            return getNearEntry(key, LT|EQ);
        }

        public K floorKey(K key) {
            return getNearKey(key, LT|EQ);
        }

        public Map.Entry<K,V> higherEntry(K key) {
            return getNearEntry(key, GT);
        }

        public K higherKey(K key) {
            return getNearKey(key, GT);
        }

        public K firstKey() {
            return isDescending ? highestKey() : lowestKey();
        }

        public K lastKey() {
            return isDescending ? lowestKey() : highestKey();
        }

        public Map.Entry<K,V> firstEntry() {
            return isDescending ? highestEntry() : lowestEntry();
        }

        public Map.Entry<K,V> lastEntry() {
            return isDescending ? lowestEntry() : highestEntry();
        }

        public Map.Entry<K,V> pollFirstEntry() {
            return isDescending ? removeHighest() : removeLowest();
        }

        public Map.Entry<K,V> pollLastEntry() {
            return isDescending ? removeLowest() : removeHighest();
        }

        public NavigableSet<K> keySet() {
            KeySet<K> ks = keySetView;
            return (ks != null) ? ks : (keySetView = new KeySet<K>(this));
        }

        public NavigableSet<K> navigableKeySet() {
            KeySet<K> ks = keySetView;
            return (ks != null) ? ks : (keySetView = new KeySet<K>(this));
        }

        public Collection<V> values() {
            Collection<V> vs = valuesView;
            return (vs != null) ? vs : (valuesView = new Values<V>(this));
        }

        public Set<Map.Entry<K,V>> entrySet() {
            Set<Map.Entry<K,V>> es = entrySetView;
            return (es != null) ? es : (entrySetView = new EntrySet<K,V>(this));
        }

        public NavigableSet<K> descendingKeySet() {
            return descendingMap().navigableKeySet();
        }

        Iterator<K> keyIterator() {
            return new SubMapKeyIterator();
        }

        Iterator<V> valueIterator() {
            return new SubMapValueIterator();
        }

        Iterator<Map.Entry<K,V>> entryIterator() {
            return new SubMapEntryIterator();
        }

        abstract class SubMapIter<T> implements Iterator<T>, Spliterator<T> {
            /** the last node returned by next() */
            Node<K,V> lastReturned;
            /** the next node to return from next(); */
            Node<K,V> next;
            /** Cache of next value field to maintain weak consistency */
            V nextValue;

            SubMapIter() {
                Comparator<? super K> cmp = m.comparator;
                for (;;) {
                    next = isDescending ? hiNode(cmp) : loNode(cmp);
                    if (next == null)
                        break;
                    AnyPersistent x = next.value();
                    if (x != null && x != next) {
                        if (! inBounds(next.key(), cmp))
                            next = null;
                        else {
                            @SuppressWarnings("unchecked") V vv = (V)x;
                            nextValue = vv;
                        }
                        break;
                    }
                }
            }

            public final boolean hasNext() {
                return next != null;
            }

            final void advance() {
                if (next == null)
                    throw new NoSuchElementException();
                lastReturned = next;
                if (isDescending)
                    descend();
                else
                    ascend();
            }

            private void ascend() {
                Comparator<? super K> cmp = m.comparator;
                for (;;) {
                    next = next.next();
                    if (next == null)
                        break;
                    AnyPersistent x = next.value();
                    if (x != null && x != next) {
                        if (tooHigh(next.key(), cmp))
                            next = null;
                        else {
                            @SuppressWarnings("unchecked") V vv = (V)x;
                            nextValue = vv;
                        }
                        break;
                    }
                }
            }

            private void descend() {
                Comparator<? super K> cmp = m.comparator;
                for (;;) {
                    next = m.findNear(lastReturned.key(), LT, cmp);
                    if (next == null)
                        break;
                    AnyPersistent x = next.value();
                    if (x != null && x != next) {
                        if (tooLow(next.key(), cmp))
                            next = null;
                        else {
                            @SuppressWarnings("unchecked") V vv = (V)x;
                            nextValue = vv;
                        }
                        break;
                    }
                }
            }

            public void remove() {
                Node<K,V> l = lastReturned;
                if (l == null)
                    throw new IllegalStateException();
                m.remove(l.key());
                lastReturned = null;
            }

            public Spliterator<T> trySplit() {
                return null;
            }

            public boolean tryAdvance(Consumer<? super T> action) {
                if (hasNext()) {
                    action.accept(next());
                    return true;
                }
                return false;
            }

            public void forEachRemaining(Consumer<? super T> action) {
                while (hasNext())
                    action.accept(next());
            }

            public long estimateSize() {
                return Long.MAX_VALUE;
            }

        }

        final class SubMapValueIterator extends SubMapIter<V> {
            public V next() {
                V v = nextValue;
                advance();
                return v;
            }
            public int characteristics() {
                return 0;
            }
        }

        final class SubMapKeyIterator extends SubMapIter<K> {
            public K next() {
                Node<K,V> n = next;
                advance();
                return n.key();
            }
            public int characteristics() {
                return Spliterator.DISTINCT | Spliterator.ORDERED |
                    Spliterator.SORTED;
            }
            public final Comparator<? super K> getComparator() {
                return SubMap.this.comparator();
            }
        }

        final class SubMapEntryIterator extends SubMapIter<Map.Entry<K,V>> {
            public Map.Entry<K,V> next() {
                Node<K,V> n = next;
                V v = nextValue;
                advance();
                return new AbstractMap.SimpleImmutableEntry<K,V>(n.key(), v);
            }
            public int characteristics() {
                return Spliterator.DISTINCT;
            }
        }
    }

    public void forEach(BiConsumer<? super K, ? super V> action) {
        if (action == null) throw new NullPointerException();
        V v;
        for (Node<K,V> n = findFirst(); n != null; n = n.next()) {
            if ((v = n.getValidValue()) != null)
                action.accept(n.key(), v);
        }
    }

    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        if (function == null) throw new NullPointerException();
        Transaction.run(() -> {
            V v;
            for (Node<K,V> n = findFirst(); n != null; n = n.next()) {
                while ((v = n.getValidValue()) != null) {
                    V r = function.apply(n.key(), v);
                    if (r == null) throw new NullPointerException();
                    if (n.casValue(v, r))
                        break;
                }
            }
        });
    }

    abstract static class CSLMSpliterator<K extends AnyPersistent,V extends AnyPersistent> {
        final Comparator<? super K> comparator;
        final K fence;     // exclusive upper bound for keys, or null if to end
        Index<K,V> row;    // the level to split out
        Node<K,V> current; // current traversal node; initialize at origin
        int est;           // pseudo-size estimate
        CSLMSpliterator(Comparator<? super K> comparator, Index<K,V> row,
                        Node<K,V> origin, K fence, int est) {
            this.comparator = comparator; this.row = row;
            this.current = origin; this.fence = fence; this.est = est;
        }

        public final long estimateSize() { return (long)est; }
    }

    static final class KeySpliterator<K extends AnyPersistent,V extends AnyPersistent> extends CSLMSpliterator<K,V>
        implements Spliterator<K> {
        KeySpliterator(Comparator<? super K> comparator, Index<K,V> row,
                       Node<K,V> origin, K fence, int est) {
            super(comparator, row, origin, fence, est);
        }

        public Spliterator<K> trySplit() {
            Node<K,V> e; K ek;
            Comparator<? super K> cmp = comparator;
            K f = fence;
            if ((e = current) != null && (ek = e.key()) != null) {
                for (Index<K,V> q = row; q != null; q = row = q.down()) {
                    Index<K,V> s; Node<K,V> b, n; K sk;
                    if ((s = q.right()) != null && (b = s.node()) != null &&
                        (n = b.next()) != null && n.value() != null &&
                        (sk = n.key()) != null && cpr(cmp, sk, ek) > 0 &&
                        (f == null || cpr(cmp, sk, f) < 0)) {
                        current = n;
                        Index<K,V> r = q.down();
                        row = (s.right() != null) ? s : s.down();
                        est -= est >>> 2;
                        return new KeySpliterator<K,V>(cmp, r, e, sk, est);
                    }
                }
            }
            return null;
        }

        public void forEachRemaining(Consumer<? super K> action) {
            if (action == null) throw new NullPointerException();
            Comparator<? super K> cmp = comparator;
            K f = fence;
            Node<K,V> e = current;
            current = null;
            for (; e != null; e = e.next()) {
                K k; Object v;
                if ((k = e.key()) != null && f != null && cpr(cmp, f, k) <= 0)
                    break;
                if ((v = e.value()) != null && v != e)
                    action.accept(k);
            }
        }

        public boolean tryAdvance(Consumer<? super K> action) {
            if (action == null) throw new NullPointerException();
            Comparator<? super K> cmp = comparator;
            K f = fence;
            Node<K,V> e = current;
            for (; e != null; e = e.next()) {
                K k; Object v;
                if ((k = e.key()) != null && f != null && cpr(cmp, f, k) <= 0) {
                    e = null;
                    break;
                }
                if ((v = e.value()) != null && v != e) {
                    current = e.next();
                    action.accept(k);
                    return true;
                }
            }
            current = e;
            return false;
        }

        public int characteristics() {
            return Spliterator.DISTINCT | Spliterator.SORTED |
                Spliterator.ORDERED | Spliterator.CONCURRENT |
                Spliterator.NONNULL;
        }

        public final Comparator<? super K> getComparator() {
            return comparator;
        }
    }

    final KeySpliterator<K,V> keySpliterator() {
        Comparator<? super K> cmp = comparator;
        for (;;) { // ensure h corresponds to origin p
            HeadIndex<K,V> h; Node<K,V> p;
            Node<K,V> b = (h = head()).node();
            if ((p = b.next()) == null || p.value() != null)
                return new KeySpliterator<K,V>(cmp, h, p, null, (p == null) ?
                                               0 : Integer.MAX_VALUE);
            p.helpDelete(b, p.next());
        }
    }

    static final class ValueSpliterator<K extends AnyPersistent,V extends AnyPersistent> extends CSLMSpliterator<K,V>
        implements Spliterator<V> {
        ValueSpliterator(Comparator<? super K> comparator, Index<K,V> row,
                       Node<K,V> origin, K fence, int est) {
            super(comparator, row, origin, fence, est);
        }

        public Spliterator<V> trySplit() {
            Node<K,V> e; K ek;
            Comparator<? super K> cmp = comparator;
            K f = fence;
            if ((e = current) != null && (ek = e.key()) != null) {
                for (Index<K,V> q = row; q != null; q = row = q.down()) {
                    Index<K,V> s; Node<K,V> b, n; K sk;
                    if ((s = q.right()) != null && (b = s.node()) != null &&
                        (n = b.next()) != null && n.value() != null &&
                        (sk = n.key()) != null && cpr(cmp, sk, ek) > 0 &&
                        (f == null || cpr(cmp, sk, f) < 0)) {
                        current = n;
                        Index<K,V> r = q.down();
                        row = (s.right() != null) ? s : s.down();
                        est -= est >>> 2;
                        return new ValueSpliterator<K,V>(cmp, r, e, sk, est);
                    }
                }
            }
            return null;
        }

        public void forEachRemaining(Consumer<? super V> action) {
            if (action == null) throw new NullPointerException();
            Comparator<? super K> cmp = comparator;
            K f = fence;
            Node<K,V> e = current;
            current = null;
            for (; e != null; e = e.next()) {
                K k; AnyPersistent v;
                if ((k = e.key()) != null && f != null && cpr(cmp, f, k) <= 0)
                    break;
                if ((v = e.value()) != null && v != e) {
                    @SuppressWarnings("unchecked") V vv = (V)v;
                    action.accept(vv);
                }
            }
        }

        public boolean tryAdvance(Consumer<? super V> action) {
            if (action == null) throw new NullPointerException();
            Comparator<? super K> cmp = comparator;
            K f = fence;
            Node<K,V> e = current;
            for (; e != null; e = e.next()) {
                K k; AnyPersistent v;
                if ((k = e.key()) != null && f != null && cpr(cmp, f, k) <= 0) {
                    e = null;
                    break;
                }
                if ((v = e.value()) != null && v != e) {
                    current = e.next();
                    @SuppressWarnings("unchecked") V vv = (V)v;
                    action.accept(vv);
                    return true;
                }
            }
            current = e;
            return false;
        }

        public int characteristics() {
            return Spliterator.CONCURRENT | Spliterator.ORDERED |
                Spliterator.NONNULL;
        }
    }

    final ValueSpliterator<K,V> valueSpliterator() {
        Comparator<? super K> cmp = comparator;
        for (;;) {
            HeadIndex<K,V> h; Node<K,V> p;
            Node<K,V> b = (h = head()).node();
            if ((p = b.next()) == null || p.value() != null)
                return new ValueSpliterator<K,V>(cmp, h, p, null, (p == null) ?
                                                 0 : Integer.MAX_VALUE);
            p.helpDelete(b, p.next());
        }
    }

    static final class EntrySpliterator<K extends AnyPersistent,V extends AnyPersistent> extends CSLMSpliterator<K,V>
        implements Spliterator<Map.Entry<K,V>> {
        EntrySpliterator(Comparator<? super K> comparator, Index<K,V> row,
                         Node<K,V> origin, K fence, int est) {
            super(comparator, row, origin, fence, est);
        }

        public Spliterator<Map.Entry<K,V>> trySplit() {
            Node<K,V> e; K ek;
            Comparator<? super K> cmp = comparator;
            K f = fence;
            if ((e = current) != null && (ek = e.key()) != null) {
                for (Index<K,V> q = row; q != null; q = row = q.down()) {
                    Index<K,V> s; Node<K,V> b, n; K sk;
                    if ((s = q.right()) != null && (b = s.node()) != null &&
                        (n = b.next()) != null && n.value() != null &&
                        (sk = n.key()) != null && cpr(cmp, sk, ek) > 0 &&
                        (f == null || cpr(cmp, sk, f) < 0)) {
                        current = n;
                        Index<K,V> r = q.down();
                        row = (s.right() != null) ? s : s.down();
                        est -= est >>> 2;
                        return new EntrySpliterator<K,V>(cmp, r, e, sk, est);
                    }
                }
            }
            return null;
        }

        public void forEachRemaining(Consumer<? super Map.Entry<K,V>> action) {
            if (action == null) throw new NullPointerException();
            Comparator<? super K> cmp = comparator;
            K f = fence;
            Node<K,V> e = current;
            current = null;
            for (; e != null; e = e.next()) {
                K k; AnyPersistent v;
                if ((k = e.key()) != null && f != null && cpr(cmp, f, k) <= 0)
                    break;
                if ((v = e.value()) != null && v != e) {
                    @SuppressWarnings("unchecked") V vv = (V)v;
                    action.accept
                        (new AbstractMap.SimpleImmutableEntry<K,V>(k, vv));
                }
            }
        }

        public boolean tryAdvance(Consumer<? super Map.Entry<K,V>> action) {
            if (action == null) throw new NullPointerException();
            Comparator<? super K> cmp = comparator;
            K f = fence;
            Node<K,V> e = current;
            for (; e != null; e = e.next()) {
                K k; AnyPersistent v;
                if ((k = e.key()) != null && f != null && cpr(cmp, f, k) <= 0) {
                    e = null;
                    break;
                }
                if ((v = e.value()) != null && v != e) {
                    current = e.next();
                    @SuppressWarnings("unchecked") V vv = (V)v;
                    action.accept
                        (new AbstractMap.SimpleImmutableEntry<K,V>(k, vv));
                    return true;
                }
            }
            current = e;
            return false;
        }

        public int characteristics() {
            return Spliterator.DISTINCT | Spliterator.SORTED |
                Spliterator.ORDERED | Spliterator.CONCURRENT |
                Spliterator.NONNULL;
        }

        public final Comparator<Map.Entry<K,V>> getComparator() {
            // Adapt or create a key-based comparator
            if (comparator != null) {
                return Map.Entry.comparingByKey(comparator);
            }
            else {
                return (Comparator<Map.Entry<K,V>>) (e1, e2) -> {
                    @SuppressWarnings("unchecked")
                    Comparable<? super K> k1 = (Comparable<? super K>) e1.getKey();
                    return k1.compareTo(e2.getKey());
                };
            }
        }
    }

    final EntrySpliterator<K,V> entrySpliterator() {
        Comparator<? super K> cmp = comparator;
        for (;;) { // almost same as key version
            HeadIndex<K,V> h; Node<K,V> p;
            Node<K,V> b = (h = head()).node();
            if ((p = b.next()) == null || p.value() != null)
                return new EntrySpliterator<K,V>(cmp, h, p, null, (p == null) ?
                                                 0 : Integer.MAX_VALUE);
            p.helpDelete(b, p.next());
        }
    }
}
