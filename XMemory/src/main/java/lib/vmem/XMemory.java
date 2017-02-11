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

package lib.vmem;

import java.io.*;
import java.nio.ByteBuffer;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.Reference;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class XMemory implements Memory {

    static final Cleaner cleaner;

    private static class Cleaner {
        private static final ConcurrentHashMap<Reference, Long> ref_to_addr_map = new ConcurrentHashMap<Reference, Long>();
        private static final ReferenceQueue bbq = new ReferenceQueue();

        private Cleaner() {
            Thread t = new Thread(new CleanerThread());
            t.setDaemon(true);
            t.start();
        }

        private static class CleanerThread implements Runnable {
            public void run() {
                while (true) {
                    try {
                        PhantomReference ref = (PhantomReference)(bbq.remove());
                        nativeFree((long)(ref_to_addr_map.remove(ref)));
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.exit(-1);
                    }
                }
            }
        }

        private static Cleaner create() {
            return new Cleaner();
        }

        public void register(Object obj, long addr) {
            PhantomReference ref = new PhantomReference(obj, bbq);
            ref_to_addr_map.put(ref, addr);
        }
    }

    /* (non-Javadoc)
     * Intentionally empty constructor.
     */
    public XMemory() {}

    static {
        cleaner = Cleaner.create();
        try {
            loadNativeLibraryFromJar("XMemory");
            System.out.println("Native library loaded successfully");
            Properties prop = new Properties();
            try {
                InputStream is = XMemory.class.getClassLoader().getResourceAsStream("memory.properties");
                prop.load(is);
                is.close();
                String path = prop.getProperty("device.name");
                long size = Long.parseLong(prop.getProperty("device.size"));
                System.out.println("Will try to allocate size = " + size + " in " + path);
                int retval = initialize(path, size);
                if (retval < 0) {
                    System.out.println("Issue initalizing memory space. Exiting the application!!");
                    System.exit(-1);
                }
            } catch (Exception e) {
                System.out.println("Error reading properties");
                System.exit(-1);
            }
        } catch (Exception e) {
            System.exit(-1);
        }
    }

    private static void loadNativeLibraryFromJar(String lib) {
        String libPathName = System.mapLibraryName(lib);
        InputStream ipStream = XMemory.class.getClassLoader().getResourceAsStream(libPathName);
        try {
            File tmpFile = File.createTempFile("lib", "so");
            tmpFile.deleteOnExit();

            byte[] buff = new byte[1024];
            int streamLength;
            OutputStream opStream = new FileOutputStream(tmpFile);
            while ((streamLength = ipStream.read(buff)) != -1) {
                opStream.write(buff, 0, streamLength);
            }
            opStream.close();
            ipStream.close();
            System.load(tmpFile.getAbsolutePath());
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /* (non-Javadoc)
     * @see com.intel.memory.Memory#allocateByteBuffer(int)
     */
    public ByteBuffer allocateByteBuffer(int size) {
        long offset = nativeReserveByteBufferMemory(size);
        ByteBuffer bb = nativeCreateByteBuffer(offset, size);
        if (bb == null) {  // allocation failed; free memory and try again
            freeMemory();
            bb = nativeCreateByteBuffer(offset, size);
        }
        if (bb != null) {  // check again if enough memory available after freeing
            cleaner.register(bb, offset);  // register this DirectByteBuffer so it could be cleaned later automatically
        }
        return bb;
    }

    private void freeMemory() {
        System.gc();
        try {
            // thread to sleep for 100 milliseconds for GC to kick in
            Thread.sleep(100);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * Reserves the memory for the ByteBuffer.
     *
     * @param size size of the buffer created
     * @return the offset of the reserved memory
     */
    private native long nativeReserveByteBufferMemory(int size);

    /**
     * Creates a ByteBuffer based on the memory at the given offset.
     *
     * @param offset location of the ByteBuffer struct.
     * @param size size of the ByteBuffer
     * @return the ByteBuffer
     */
    private native ByteBuffer nativeCreateByteBuffer(long offset, int size);

    /**
     * Initialize the memory pool.
     *
     * @param path path to the memory device
     * @param mem_size size reserved on the memory device
     * @return the status of initialization. If value < 0, then initialization failed
     */
    private static native int initialize(String path, long mem_size);

    /**
     * Frees the underlying memory of the DirectByteBuffer at the given offset.
     *
     * @param address memory address of the DirectByteBuffer
     * @return none
     */
    private static native void nativeFree(long offset);
}
