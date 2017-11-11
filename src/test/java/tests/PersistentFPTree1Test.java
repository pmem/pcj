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

package tests;

import lib.util.persistent.*;
import lib.util.persistent.spi.PersistentMemoryProvider;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class PersistentFPTree1Test {

	static boolean verbose = false;
	static boolean success = true;
	static String[] args;

	public static void main(String[] args) {
		PersistentMemoryProvider.getDefaultProvider().getHeap().open();
		verbose = true;
		PersistentFPTree1Test.args = (args.length == 0)? null : args;
		run();
	}

	public static boolean run() {
		System.out.println("****************PersistentFPTree1 Tests****************");
		return sisterTypeTest() && singleThreadedTest() && multiThreadedTest(args) && testPersistence(args) && testPersistence2(args) && testIterators();
	}

	public static boolean sisterTypeTest() {
		int I = 3;
		int L = 4;
		int N = 5000;
		PersistentFPTree1<PersistentString, PersistentInteger> fpt = new PersistentFPTree1<>(I, L);

		Set<Integer> s = new HashSet<>();
		Integer[] rands = new Integer[N];
		for(int i = 0; i < N; i++) {
			rands[i] = ThreadLocalRandom.current().nextInt(0, N);
			s.add(rands[i]);
		}

		if(verbose) System.out.println("Sister type test: # generated keys: " + rands.length);

		for(int num : rands) {
			fpt.put(new PersistentString(Integer.toString(num)), new PersistentInteger(num));
		}

		boolean getSuccess = true;
		for(int num : s) {
			PersistentInteger val = fpt.get(Integer.toString(num), PersistentString.class); 
			if(val != null) {
				assert(num == val.intValue());
				getSuccess = getSuccess && (num == val.intValue());
			}
			else {
				if(verbose) System.out.println("Key: " + num + " => null");
				getSuccess = false;
			}
		}

		if(verbose) System.out.println("GET SUCCESS: " + getSuccess);
		else assert(getSuccess == true);
		return true;
	}


	public static boolean singleThreadedTest() {
		int I = 3;
		int L = 4;
		int X = 500;
		int N = X * L;
		PersistentFPTree1<PersistentInteger, PersistentString> fpt = new PersistentFPTree1<>(I, L);

		Set<Integer> s = new HashSet<>();
		Integer[] rands = new Integer[N];
		for(int i = 0; i < N; i++) {
			rands[i] = ThreadLocalRandom.current().nextInt(0, N);
			s.add(rands[i]);
		}

		if(verbose) System.out.println("Generated keys: " + rands.length);

		for(int num : rands) {
			fpt.put(new PersistentInteger(num), new PersistentString(Integer.toString(num)));
		}

		boolean getSuccess = true;
		for(int num : rands ) {
			PersistentString val = fpt.get(new PersistentInteger(num));
			if(val != null) {
				getSuccess = getSuccess && (num == Integer.parseInt(val.toString()));
			}
			else {
				System.out.println("Key: " + num + " => null");
				getSuccess = false;
			}
		}

		if(verbose) System.out.println("GET SUCCESS: " + getSuccess);
		else assert(getSuccess == true);

		boolean rmvSuccess = true;
		for(int num : s) {
			boolean current = false;
			PersistentString val = fpt.remove(new PersistentInteger(num));
			if(val != null) {
				if(Integer.parseInt(val.toString()) == num) current = true;
			}
			else current = false;
			rmvSuccess = rmvSuccess && current;
		}

		for(int num : s) {
			PersistentString val = fpt.remove(new PersistentInteger(num));
			if(val != null) System.out.println("2nd try FAIL: removing " + num);
			rmvSuccess = rmvSuccess && (val == null);
		}

		if(verbose) System.out.println("REMOVE SUCCESS: " + rmvSuccess);
		else assert(rmvSuccess == true);
		if(verbose) fpt.verifyDelete("Single threaded test |> ");
		else assert(fpt.verifyDelete() == true); 
		//fpt.printLeaves();
		return true;
	}


	private static boolean multiThreadedTest(String[] args) {
		int I, L, N, procs, nruns = 1;
		double readRatio;

		try {
			I = Integer.parseInt(args[0]);
			L = Integer.parseInt(args[1]);
			N = Integer.parseInt(args[2]);
			readRatio = Double.parseDouble(args[3]);
			procs = Integer.parseInt(args[4]);
		}
		catch(Exception ex) {
			I = 4;
			L = 5;
			N = 1000;
			readRatio = 0.5;
			procs = Runtime.getRuntime().availableProcessors()/2;
			if(verbose) System.out.println("Using default values for I, L, N, readRatio and procs -> " + I + ", " + L + ", " + N + ", " + readRatio + ", " + procs);
		}

		PersistentFPTree1<PersistentInteger, PersistentString> cfpt = new PersistentFPTree1<>(I, L);
		PutGetTask[] tasks = new PutGetTask[procs];
		for(int i = 0; i < procs; i++) tasks[i] = new PutGetTask(i, N, procs, readRatio, cfpt);
		multiThreadedTask(procs, tasks, 0);
		multiThreadedTask(procs, tasks, 1);
		multiThreadedTask(procs, tasks, 2);
		if(!verbose) assert(cfpt.verifyDelete() == true);
		else cfpt.verifyDelete("Multi threaded test |> ");
		//cfpt.printLeaves();
		return true;
	}

	private static void multiThreadedTask(int procs, PutGetTask[] tasks, int taskCode) {
		Thread[] threads = new Thread[procs];
		long start = System.nanoTime();
		for(int i = 0; i < procs; i++) {
			tasks[i].taskCode = taskCode;
			threads[i] = new Thread(tasks[i]);
			threads[i].start();
		}
		try {
			for(int i = 0; i < procs; i++) threads[i].join();
		}
		catch (InterruptedException ex) {

		}

		long telap = System.nanoTime() - start;
		double telapsed = (telap * 1e-9);
		if(verbose) {
			String[] ops = {"PUT", "GET", "REMOVE", "PUTGET"};
			String ratio = (taskCode != 3 ? "" : " read ratio = " + Double.toString(tasks[0].readRatio));
			System.out.println(procs + " Threads|> " + ops[taskCode] + ratio + " Avg Throughput(kilo ops/sec)= " + (int)( tasks[0].N*1e-3/(telapsed)));
		}

	}

	private static class PutGetTask implements Runnable {

		public final int id, N, procs;
		public final double readRatio;
		final PersistentFPTree1<PersistentInteger, PersistentString> cfpt;
		public int taskCode;
		final int[] rands;
		final int T;

		public PutGetTask(int id, int N, int procs, double readRatio, PersistentFPTree1<PersistentInteger, PersistentString> cfpt) {
			this.id = id;
			this.N = N;
			this.procs = procs;
			this.readRatio = readRatio;
			this.cfpt = cfpt;
			this.taskCode = 0;
			this.T= N/procs;
			this.rands = new int[T];
			for(int i = 0; i < T; i++) rands[i] = ThreadLocalRandom.current().nextInt(0, N);
		}

		@Override
		public void run() {
			switch(taskCode) {
			case 0 : put(); break;
			case 1 : get(); break;
			case 2 : remove(); break;
			case 3 : putGet(); break;
			}
		}

		private void put() {
			for (int i = 0; i < T; i++) {
				cfpt.put(new PersistentInteger(rands[i]), new PersistentString(Integer.toString(rands[i])));
			}
		}

		private void remove() {
			for (int i = 0; i < T; i++) {
				PersistentString val = cfpt.remove(new PersistentInteger(rands[i]));
				if(val != null) assert(Integer.parseInt(val.toString()) == rands[i]);
			}
		}

		private void get() {
			for (int i = 0; i < T; i++) {
				PersistentString val = cfpt.get(new PersistentInteger(rands[i]));
				if(val == null) throw new IllegalStateException("Thread " + id + "> FPTree GET FAILED for " + rands[i]);
				else assert(Integer.parseInt(val.toString()) == rands[i]);
			}
		}

		private void putGet() {
			double count = 0.0;
			for (Integer i = 0; i < T; i++) {
				if (ThreadLocalRandom.current().nextInt(0, 100) <= (int) (readRatio * 100)) {
					int n = ThreadLocalRandom.current().nextInt(-N, N);
					PersistentString ans = cfpt.get(new PersistentInteger(n));
					if (ans != null) count = count + 1.0;
				}
				else {
					int n = ThreadLocalRandom.current().nextInt(0, N);
					cfpt.put(new PersistentInteger(n), new PersistentString(Integer.toString(n)));
				}
			}
			if (count < 0.0) System.out.println(count);
		}
	}


	@SuppressWarnings("unchecked")
	public static boolean testPersistence(String[] args) {
		if (verbose) System.out.println("PersistentFPTree1: testing persistence across vm runs - test1");
		int I, L, N;
		try {
			I = Integer.parseInt(args[0]);
			L = Integer.parseInt(args[1]);
			N = Integer.parseInt(args[2]);
		}
		catch(Exception ex) {
			I = 2;
			L = 3;
			N = 1000;
			if(verbose) System.out.println("Using default values for I, L, N -> " + I + ", " + L + ", " + N);
		}

		
		String id = "tests.fptree1_persistance.RC";
		PersistentFPTree1<PersistentInteger, PersistentString> cfptRC = ObjectDirectory.get(id, PersistentFPTree1.class);
		PersistentLong pl = ObjectDirectory.get("pseed", PersistentLong.class);
		if (cfptRC == null && pl == null) {
			cfptRC = new PersistentFPTree1<PersistentInteger, PersistentString>(I, L);
			pl = new PersistentLong(ThreadLocalRandom.current().nextLong(N));
			ObjectDirectory.put(id, cfptRC);
			ObjectDirectory.put("pseed", pl);
			if (verbose) System.out.println("Saving to pmem...");
			Random rnd = new Random(pl.longValue());
			for(int i = 0; i < N; i++) {
				int rand = rnd.nextInt(N);
				cfptRC.put(new PersistentInteger(rand), new PersistentString(Integer.toString(rand)));
			}
			//cfptRC.printLeaves();
		}
		else {
			if (verbose) System.out.println("Retrieving from pmem...");
			//cfptRC.printLeaves();
			Random rnd = new Random(pl.longValue());
			for(int i = 0; i < N; i++) {
				int rand = rnd.nextInt(N);
				//PersistentString ps = cfptRC.get(new PersistentInteger(rand));
				//System.out.println(rand + "-->" + ps);
				assert(cfptRC.get(new PersistentInteger(rand)).equals(new PersistentString(Integer.toString(rand))));
			}
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public static boolean testPersistence2(String[] args) {
		if (verbose) System.out.println("PersistentFPTree1: testing persistence across vm runs - test2");
		int I, L, N;
		try {
			I = Integer.parseInt(args[0]);
			L = Integer.parseInt(args[1]);
			N = Integer.parseInt(args[2]);
		}
		catch(Exception ex) {
			I = 3;
			L = 4;
			N = 1000;
			if(verbose) System.out.println("Using default values for I, L, N -> " + I + ", " + L + ", " + N);
		}
		
		String id = "tests.fptree1_persistance2.RC";
		PersistentFPTree1<PersistentInteger, PersistentString> cfptRC = ObjectDirectory.get(id, PersistentFPTree1.class);
		if (cfptRC == null) {
			cfptRC = new PersistentFPTree1<PersistentInteger, PersistentString>(I, L);
			ObjectDirectory.put(id, cfptRC);
			if (verbose) System.out.println("Saving to pmem...");
			Random rnd = new Random();
			for(int i = 0; i < N; i++) {
				int rand = rnd.nextInt(N);
				cfptRC.put(new PersistentInteger(rand), new PersistentString(Integer.toString(rand)));
			}
			cfptRC.randomlyDeleteLeaves();
			//cfptRC.printLeaves();
		}
		else {
			if (verbose) System.out.println("Retrieving from pmem...");
			HashMap<PersistentInteger, PersistentString> map = cfptRC.getHashMap();
			//cfptRC.printLeaves();
			for(PersistentInteger k : map.keySet()) {
				assert(cfptRC.get(k).equals(map.get(k)));
			}
		}
		return true;
	}
	
	public static boolean testIterators() {
		int I = 4;
		int L = 5;
		int X = 500;
		int N = X * L;
		PersistentFPTree1<PersistentInteger, PersistentString> fpt = new PersistentFPTree1<>(I, L);

		for(int i = 0; i < N; i++) {
			int rand = ThreadLocalRandom.current().nextInt(0, N);
			fpt.put(new PersistentInteger(rand), new PersistentString(Integer.toString(rand)));
		}
		// Iterator
		fpt.randomlyDeleteLeaves();
		//fpt.printLeaves();for(PersistentInteger i : fpt.keySet()) System.out.print(i + ",");System.out.println("##############");
		
		
		HashMap<PersistentInteger, PersistentString> hmap = fpt.getHashMap();
		Set<Map.Entry<PersistentInteger, PersistentString>> eset = hmap.entrySet();
		@SuppressWarnings("unchecked")
		HashSet<PersistentInteger> kset = new HashSet(hmap.keySet());
		
		Iterator<PersistentInteger> kit = fpt.navigableKeySet().iterator();
		int size = kset.size();
		while(kit.hasNext()) {
			kset.remove(kit.next());
		}
		assert(kset.size() == 0);
		for(PersistentInteger i : fpt.keySet()) kset.add(i);
		assert(kset.size() == size);
		

		for(Map.Entry<PersistentInteger, PersistentString> e : fpt.entrySet()) eset.remove(e);
		assert(eset.size() == 0);
		
		if(verbose) System.out.println("Iterators test successful");
		return true;
	}
}
