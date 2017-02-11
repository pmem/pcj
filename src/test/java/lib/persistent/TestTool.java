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

import lib.persistent.Util;
import java.util.Arrays;
import java.util.Stack;
import java.util.stream.Stream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

@SuppressWarnings("unchecked") 
public class TestTool
{
	private static PersistentTreeMap<PersistentByteBuffer, PersistentByteBuffer> data;
	private static Stack<Time> times;
	private static int indent;

	static {
		times = new Stack<>();
		indent = 0;
		String name = "TestToolEntries";
		data = ObjectDirectory.get(name, PersistentTreeMap.class);
		if (data == null) { 
			data = new PersistentTreeMap<>();
			ObjectDirectory.put(name, data);
		}
	}

	public static void main(String[] args) throws Exception {
		if (args.length > 0) run("command line", args);
	}

	private static void run(String name, String[] args) throws Exception {
		message(":run " + name);
		TestTool.indent++;
		for (int i = 0; i < args.length; i++) {
			String op = args[i].trim();
			if (op.length() == 0) continue;
			switch(op) {
				case "run": run(args[i += 1], strings(args[i])); break;
				case "put": put(Integer.parseInt(args[i += 1])); break;
				case "putnew": putNew(Integer.parseInt(args[i += 1])); break;
				case "list": list(); break;
				case "clear": message(":clear"); data.clear(); break;
				case "debug": message(":debug"); Util.debugPool(); break;
				case "size": message(":size -> " + data.size());	break;
				case "tstart":	tstart(); break;
				case "tstop": tstop(); break;
				case "delete": delete(); break;
				case "null": message(":null"); break;
				default:	message("unknown operation: " + op); break;
			}
		}
		TestTool.indent--;
	}	

	private static void message(String... ss) {
		String pad = "";
		for (int i = 0; i < TestTool.indent; i++) pad += "   ";
		for (String s : ss) System.out.println(pad + s);
	}

	public static class Entry {
		private String name;
		private long id;

		public Entry(String name, long id) {
			this.name = name;
			this.id = id;
		}

		public PersistentByteBuffer keyBuffer() {
			byte[] bytes = name.getBytes();
			return PersistentByteBuffer.allocate(bytes.length + 4).putInt(bytes.length).put(bytes).rewind();
		}

		public PersistentByteBuffer valueBuffer() {
			byte[] bytes = name.getBytes();
			return PersistentByteBuffer.allocate(bytes.length + 4 + 8).putInt(bytes.length).put(bytes).putLong(id).rewind();
		}

		public static Entry fromBuffer(PersistentByteBuffer buff) {
			int n = buff.rewind().getInt();
			byte[] bytes = new byte[n];
			buff.get(bytes);
			String name = new String(bytes);
			long id = buff.getLong();
			return new Entry(name, id);
		}

		public String toString() {
			return "Entry(" + name + ", " + id + ")";
		}
	}

	private static void put(int n) {
		message(":put " + n);
		for (int i = 0; i < n; i++) {
			Entry e = new Entry("Entry" + i, i);
			data.put(e.keyBuffer(), e.valueBuffer());
		}
	}

	private static void putNew(int n) {
		message(":putnew " + n);
		for (int i = 0; i < n; i++) {
			long nano = System.nanoTime();
			Entry e = new Entry("NewEntry" + nano, nano);
			data.put(e.keyBuffer(), e.valueBuffer());
		}
	}

	private static void list() {
		message(":list", "Entries", "{");
		for (PersistentByteBuffer key : data.keySet()) {
			message("  " + Entry.fromBuffer(data.get(key)));
		}
		message("}");
	}

	private static void delete() throws Exception {
		message(":delete"); 
		Runtime.getRuntime().exec("rm /mnt/mem/persistent_pool").waitFor(); 
	}


	private static void tstart() {
		Time time = new Time(); 
		message(":tstart(" + time.id + ")");
		times.push(time);
	}

	private static void tstop() {
		Time time = times.pop();
		float dur = ((float)System.nanoTime() - (float)time.start) / 1e9f;
		message(":tstop(" + time.id + "), " + dur + " s"); 
	}

	private static class Time {
		private static int next = 0;
		int id;
		long start;

		public Time()
		{
			this.id = next++;
			this. start = System.nanoTime();
		}
	}

	private static String[] strings(String pathStr) throws Exception {
		Path path = Paths.get(pathStr);
		Stream<String> lines = Files.lines(path);
		Stream<String> strings = lines.flatMap(line -> Arrays.stream(line.split(" ")));
		return strings.toArray(String[]::new);
	}
}
