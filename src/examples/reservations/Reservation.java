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

package examples.reservations;

import lib.persistent.PersistentByteBuffer;
import java.time.Instant;

public class Reservation 
{
	private String name;
	private Instant time;
	private String issue;

	public Reservation(String name, Instant time, String issue) {
		this.name = name;
		this.time = time;
		this.issue = issue;
	}

	public String name() {return name;} 
	public Instant time() {return time;} 
	public String issue() {return issue;} 

	public PersistentByteBuffer toBuffer() {
		int nlen = name.length();
		int ilen = issue.length();
		PersistentByteBuffer ans = PersistentByteBuffer.allocate(nlen + ilen + 2 * 4 + 2 * 8);
		putString(ans, name);
		ans.putLong(time.getEpochSecond()).putLong(time.getNano());
		putString(ans, issue);
		return ans; 
	}

	public static Reservation fromBuffer(PersistentByteBuffer buff) {
		Reservation ans = null;
		if (buff != null) {
			buff.rewind();
			String name = getString(buff);
			Instant time = Instant.ofEpochSecond(buff.getLong(), buff.getLong());
			String issue = getString(buff);
			ans = new Reservation(name, time, issue);
		}
		return ans;
	}

	private static void putString(PersistentByteBuffer buff, String s) {
		buff.putInt(s.length()).put(s.getBytes());
	}

	private static String getString(PersistentByteBuffer buff) {
		int n = buff.getInt();
		byte[] bytes = new byte[n];
		buff.get(bytes);
		return new String(bytes);
	}

	public String toString() {
		return "Reservation(" + name + ", " + time + ", " + issue + ")";
	}
}
