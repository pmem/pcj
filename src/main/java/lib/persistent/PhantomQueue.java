/* Copyright (C) 2016  Intel Corporation
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

package lib.persistent;

import java.lang.ref.*;
import java.util.*;
import java.util.concurrent.*;

class PhantomQueue<T> {

    private static long nextID = 10001;

    private Listener listener;
    private ReferenceQueue<T> queue;
    private Map<Long, Ref> refs;

    public class Ref extends PhantomReference<T> {

        private long info;
        private long id;
        private String name;

        public Ref(T obj, long info, String name) {
            super(obj, queue);
            this.info = info;
            this.name = name;
            this.id = nextID++;
        }

        public long getInfo() { return info; }
        public String getName() { return name; }
        public long getID() { return id; }
        public String toString() { return "Ref(\"" + name + "\", " + getInfo() + "), ID: " + getID(); }
    }

    @FunctionalInterface
    interface Listener {
        public void notify(long info, String name);
    }

    @SuppressWarnings("unchecked")
    public PhantomQueue(Listener listener) {

        this.listener = listener;
        this.queue = new ReferenceQueue<T>();
        this.refs = new ConcurrentHashMap<Long, Ref>();

        Thread collector = new Thread(() -> {
            try {
                while (true) {
                    Ref ref = (Ref)(queue.remove());
                    refs.remove(ref.getID());
                    //System.out.println("Just removed " + ref + " from queue");
                    listener.notify(ref.getInfo(), ref.getName());
                }
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        });

        collector.setDaemon(true);
        collector.start();
    }

    public void clearRefs() { refs.clear(); }

    public void registerObject(T t, long info) {
        Ref r = new Ref(t, info, "Offset: " + ((Persistent)t).getOffset());
        refs.put(r.getID(), r);
        Util.registerOffset(info);
        //System.out.println("Just created ref " + r);
    }

    public String toString() {
        StringBuffer buff = new StringBuffer("PhantomRefs" + "\n{\n");
        for (Map.Entry<Long, Ref> e : refs.entrySet()) {
            buff.append("  " + e.getValue() + "\n");
        }
        buff.append("}");
        return buff.toString();
    }
}
