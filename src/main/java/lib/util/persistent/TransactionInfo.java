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

import lib.util.persistent.PersistentObject;
import lib.xpersistent.XTransaction;
import java.util.ArrayList;
import static lib.util.persistent.Trace.trace;

public class TransactionInfo {
    public XTransaction transaction;
    public Transaction.State state;
    public int depth;
    public ArrayList<PersistentObject> locked;
    public ArrayList<PersistentObject> constructions;
    public int attempts;
    public int timeout;
    public int retryDelay;

    public TransactionInfo() {
        transaction = null;
        state = Transaction.State.None;
        depth = 0;
        locked  = new ArrayList<PersistentObject>();
        constructions = new ArrayList<PersistentObject>(); 
        attempts = 1;
        timeout = Config.MONITOR_ENTER_TIMEOUT;
        retryDelay = Config.BASE_TRANSACTION_RETRY_DELAY;
   }
}