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

import java.util.Random;

public class Config {
    public static final int MONITOR_ENTER_TIMEOUT = 30; // ms
    public static final int MAX_MONITOR_ENTER_TIMEOUT = 125; // ms
    public static final float MONITOR_ENTER_TIMEOUT_INCREASE_FACTOR = 1.5f;

    public static final int MAX_TRANSACTION_ATTEMPTS = 26;
    public static final int BASE_TRANSACTION_RETRY_DELAY = 200; //ms
    public static final int MAX_TRANSACTION_RETRY_DELAY = 5000; //ms
    public static final float TRANSACTION_RETRY_DELAY_INCREASE_FACTOR = 1.5f;

    public static final boolean EXIT_ON_TRANSACTION_FAILURE = false;
    public static final boolean BLOCK_ON_MAX_TRANSACTION_ATTEMPTS = true;

    public static final boolean USE_BLOCKING_LOCKS_FOR_DEBUG = false;

    public static final boolean REMOVE_FROM_OBJECT_CACHE_ON_ENQUEUE = false;
    public static final boolean COLLECT_CYCLES = false;

    public static boolean ENABLE_STATS = true;
    public static final boolean ENABLE_OBJECT_CACHE_STATS = ENABLE_STATS &&     false;
    public static final boolean ENABLE_MEMORY_STATS = ENABLE_STATS &&           true;
    public static final boolean ENABLE_TRANSACTION_STATS = ENABLE_STATS &&      true;
    public static final boolean ENABLE_LOCK_STATS = ENABLE_STATS &&             false;
    public static final boolean ENABLE_ALLOC_STATS = ENABLE_STATS &&            false;
}