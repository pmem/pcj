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

package examples.putget;

import lib.persistent.PersistentByteBuffer;
import lib.persistent.PersistentTreeMap;
import lib.persistent.ObjectDirectory;

public class PutGet {

	public static void main(String[] args) {
		doPut();
		doGet();
	}

	public static void doPut() {
		PersistentTreeMap<PersistentByteBuffer, PersistentByteBuffer> employees = new PersistentTreeMap<PersistentByteBuffer, PersistentByteBuffer>();
		ObjectDirectory.put("employees", employees);

		PersistentByteBuffer id = PersistentByteBuffer.allocate(8);
		id.putLong(1457);
		String name = "Bob Smith";
		int len = name.length();
		int salary = 25000;
		PersistentByteBuffer data = PersistentByteBuffer.allocate(len + 4 + 4);
		data.putInt(len).put(name.getBytes()).putInt(salary);

		employees.put(id, data);
	}

	@SuppressWarnings("unchecked")
	public static void doGet() {
		PersistentTreeMap<PersistentByteBuffer, PersistentByteBuffer> employees = ObjectDirectory.get("employees", PersistentTreeMap.class);

		long empId = 1457;
		PersistentByteBuffer id = PersistentByteBuffer.allocate(8);
		id.putLong(empId);

		PersistentByteBuffer data = employees.get(id);

		int len = data.rewind().getInt();
		byte[] bytes = new byte[len];
		data.get(bytes);
		String name = new String(bytes);

		int salary = data.getInt();

		System.out.println("Employee id" + "\t" + "Name" + "\t" + "Salary");
		System.out.println(empId + "\t" + name + "\t" + salary);
	}
}

