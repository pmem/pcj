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

import java.lang.ref.*;
import java.util.*;
import java.util.concurrent.*;

class PhantomQueue<T> {
    private Listener listener;
    private ReferenceQueue<T> queue;
    private Map<Pointer, Ref> refs;
    private boolean started;
    private Thread collector;

    public class Ref extends PhantomReference<T> {
        private Pointer info;
        private String name;

        public Ref(T obj, Pointer info, String name) {
            super(obj, queue);
            this.info = info;
            this.name = name;
        }

        public Pointer getInfo() { return info; }
        public String getName() { return name; }
        public String toString() { return "Ref(\"" + name + "\", " + getInfo().addr() + ")"; }
    }

    @FunctionalInterface
    interface Listener {
        public void notify(Pointer info, String name);
    }

    @SuppressWarnings("unchecked")
    public PhantomQueue(Listener listener) {

        this.listener = listener;
        this.queue = new ReferenceQueue<T>();
        this.refs = new ConcurrentHashMap<Pointer, Ref>();

        this.collector = new Thread(() -> {
            try {
                while (true) {
                    Ref ref = (Ref)(queue.remove());
                    refs.remove(ref.getInfo());
                    listener.notify(ref.getInfo(), ref.getName());
                }
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        });

        collector.setDaemon(true);
        start();
    }

    public synchronized void start() {
        if (!started) {
            started = true;
            collector.start();
        }
    }

    public void clearRefs() { refs.clear(); }

    // static int count = 0;
    public void registerObject(T t, Pointer info) {
        Ref r = new Ref(t, info, "Offset: " + info);
        refs.put(r.getInfo(), r);
        // if (++count % 10000 == 0) {/*count = 0; System.out.println("refs.size() = " + refs.size());}*/
        //System.out.println("Just created ref " + r);
    }

    public String toString() {
        StringBuffer buff = new StringBuffer("PhantomRefs" + "\n{\n");
        for (Map.Entry<Pointer, Ref> e : refs.entrySet()) {
            buff.append("  " + e.getValue() + "\n");
        }
        buff.append("}");
        return buff.toString();
    }
}
