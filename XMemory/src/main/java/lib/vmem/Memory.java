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
 * The Memory interface provides a standard method for a developer to access
 * pieces of memory from an open-ended set of physical devices, including but
 * not limited to DRAM.
 *
 * A developer will be given access to memory with java.nio.ByteBuffers, with
 * support for all semantics thereof, whose backing memory will be provided
 * and possibly managed by instances of classes that implement this interface.
 */
public interface Memory {

    /**
     * Allocate byte buffer.
     *
     * @param size size of the buffer created
     * @return a byte buffer
     */
    public ByteBuffer allocateByteBuffer(int size);

}
