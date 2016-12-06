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

import java.nio.ByteBuffer;

/**
 * The OffHeapRAM class provides off-heap (direct) java.nio.ByteBuffers via
 * the Memory interface.
 */
public class OffHeapRAM implements Memory {

    /**
     * Constructor for an OffHeapRAM instance; intentionally empty.
     */
    public OffHeapRAM() {}

    /**
     * Allocate an off-heap byte buffer.
     *
     * @param size size of the buffer created
     * @return an off-heap byte buffer
     */
    public ByteBuffer allocateByteBuffer(int size) {
        return ByteBuffer.allocateDirect(size);
    }

}
