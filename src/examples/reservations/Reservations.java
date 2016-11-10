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
import lib.persistent.PersistentSortedMap;
import lib.persistent.ObjectDirectory;
import java.time.Instant;

public class Reservations 
{
	static PersistentSortedMap data;

	static {
		data = ObjectDirectory.get("ReservationsData", PersistentSortedMap.class);
		if (data == null) ObjectDirectory.put("ReservationsData", data = new PersistentSortedMap());
	} 

	public static Reservation addReservation(String name, String time, String issue) {
		Reservation ans = new Reservation(name, Instant.parse(time), issue);
		data.put(makeKey(name, time), ans.toBuffer());	
		return ans;
	}

	public static void removeReservation(String name, String time) {
		data.remove(makeKey(name, time));
	}

	public static Reservation getReservation(String name, String time) {
		return Reservation.fromBuffer(data.get(makeKey(name, time)));
	}

	public static void listReservations() {
		System.out.println("Reservations\n{");
		for (PersistentByteBuffer b : data.keySet()) {
			System.out.println("\t" + Reservation.fromBuffer(data.get(b)));
		}
		System.out.println("}");
	}

	public static void clearReservations() {
		data.clear();
	}

	private static PersistentByteBuffer makeKey(String name, String time) {
		int tlen = time.length();
		int nlen = name.length();
		PersistentByteBuffer k = PersistentByteBuffer.allocate(nlen + tlen + 8);
		return k.putInt(nlen).put(name.getBytes()).putInt(tlen).put(time.getBytes()).rewind();
	}
}



