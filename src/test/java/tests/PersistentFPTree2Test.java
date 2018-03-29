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
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Arrays;
import java.util.concurrent.ConcurrentNavigableMap;

public class PersistentFPTree2Test {

	static boolean verbose = false;
	static boolean success = true;
	static String[] args;

	public static void main(String[] args) {
		//Stats.enable(true);
		PersistentMemoryProvider.getDefaultProvider().getHeap().open();
		verbose = true;
		PersistentFPTree2Test.args = (args.length == 0)? null : args;
		run();
		//Stats.printStats();
	}

	public static boolean run() {
		System.out.println("****************PersistentFPTree2 Tests****************");
		return sisterTypeTest() && singleThreadedTest() && singleThreadedSubMapTest() && multiThreadedTest(args) && testPersistence(args) && testPersistence2(args) && testIterators();
		//return testPersistence(args) && testPersistence2(args);
	}

	public static boolean sisterTypeTest() {
		int I = 3;
		int L = 4;
		int N = 5000;
		PersistentFPTree2<PersistentString, PersistentInteger> fpt = new PersistentFPTree2<>(I, L);

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
		int X = 5;
		int N = 4 * X * L;
		PersistentFPTree2<PersistentInteger, PersistentString> fpt = new PersistentFPTree2<>(I, L);

		Set<Integer> s = new HashSet<>();
		Integer[] rands = new Integer[N];
		for(int i = 0; i < N; i++) {
			rands[i] = i; //ThreadLocalRandom.current().nextInt(0, N);
			s.add(rands[i]);
		}

		if(verbose) System.out.println("Generated keys: " + rands.length);
		assert(fpt.isEmpty() == true);

		for(int num : rands) {
			fpt.put(new PersistentInteger(num), new PersistentString(Integer.toString(num)));
		}
		assert(fpt.isEmpty() == false);

		for(int i = 0; i < N; i++) {
			int num = rands[i];
			PersistentInteger n = new PersistentInteger(num);
			assert(fpt.ceilingKey(n).intValue() == num);
			assert(fpt.ceilingEntry(n).getValue().equals(new PersistentString(Integer.toString(num))));
			if(i == 0) {
				assert(fpt.lowerKey(n) == null);
				assert(fpt.lowerEntry(n) == null);
			}
			else {
				assert(fpt.lowerKey(n).intValue() == (num-1));
				assert(fpt.lowerEntry(n).getValue().equals(new PersistentString(Integer.toString(num-1))));
			}

			assert(fpt.floorKey(n).intValue() == num);
			assert(fpt.floorEntry(n).getValue().equals(new PersistentString(Integer.toString(num))));
			if(i == N-1) {
				assert(fpt.higherKey(n) == null);
				assert(fpt.higherEntry(n) == null);
			}
			else {
				assert(fpt.higherKey(n).intValue() == (num+1));
				assert(fpt.higherEntry(n).getValue().equals(new PersistentString(Integer.toString(num+1))));
			}
			//System.out.println("higher key: " + num + " --> " + fpt.higherKey(n));
		}
		assert(fpt.firstKey().intValue() == 0);
		assert(fpt.firstEntry().getValue().equals(new PersistentString(Integer.toString(0))));
		assert(fpt.lastEntry().getValue().equals(new PersistentString(Integer.toString(N-1))));
		assert(fpt.lastKey().intValue() == N-1);

		//System.out.println(fpt.lastKey().intValue());
		//fpt.printLeaves();

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

		if(verbose) {
			System.out.println("VERIFY NEXT " + fpt.verifyNext());
		} else assert(fpt.verifyNext() == true);

		if(verbose) System.out.println("VERIFY SIZE(" + fpt.size() + ") == N(" + N + ")");
		else assert(fpt.size() == N); 

		assert(fpt.pollFirstEntry().getValue().equals(new PersistentString(Integer.toString(0))));
		assert(fpt.pollLastEntry().getValue().equals(new PersistentString(Integer.toString(N-1))));
		assert(fpt.firstKey().intValue() == 1);
		assert(fpt.lastKey().intValue() == N-2);
		assert(fpt.firstEntry().getValue().equals(new PersistentString(Integer.toString(1))));
		assert(fpt.lastEntry().getValue().equals(new PersistentString(Integer.toString(N-2))));

		boolean rmvSuccess = true;
		for(int i = 1; i < N-1; i++) {
			int num = rands[i];
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
		assert(fpt.isEmpty() == true);
		if(verbose) System.out.println("REMOVE SUCCESS: " + rmvSuccess);
		else assert(rmvSuccess == true);
		if(verbose) fpt.verifyDelete("Single threaded test |> ");
		else assert(fpt.verifyDelete() == true); 

		return true;
	}

	public static boolean singleThreadedSubMapTest() {
		int I = 3;
		int L = 4;
		int X = 20;
		int N = 4 * X;
		PersistentFPTree2<PersistentInteger, PersistentString> fpt = new PersistentFPTree2<>(I, L);

		Set<Integer> s = new HashSet<>();
		Integer[] rands = new Integer[N];
		for(int i = 0; i < N; i++) {
			rands[i] = ThreadLocalRandom.current().nextInt(0, N);
			s.add(rands[i]);
		}

		for(int num : rands) {
			fpt.put(new PersistentInteger(num), new PersistentString(Integer.toString(num)));
		}

		final int SIZE = s.size();
		Integer[] uniques = s.toArray(new Integer[SIZE]);
		Arrays.sort(uniques);
		int lo = SIZE/4;
		int hi = 3 * SIZE/4;

		PersistentInteger lowK = new PersistentInteger(uniques[lo]);
		PersistentInteger highK = new PersistentInteger(uniques[hi]);
		boolean loInc = ThreadLocalRandom.current().nextBoolean();
		boolean hiInc = ThreadLocalRandom.current().nextBoolean();
		//fpt.printLeaves();
		if(verbose) System.out.println("Clear test for Map range: " + (loInc ? "[" : "(") + lowK + "," + highK + (hiInc ? "]" : ")"));
		ConcurrentNavigableMap<PersistentInteger, PersistentString> subMap = fpt.subMap(lowK, loInc, highK, hiInc);
		
		int SZ = (hi - lo + 1) - (!loInc? 1 : 0) - (!hiInc? 1 : 0);
		assert(SZ == subMap.size());
		if(verbose) System.out.println(SZ + " size " + subMap.size());
		subMap.clear();
		assert(subMap.size() == 0);
		

		for (int i = lo + 1; i < hi; i++ ) assert(fpt.get(new PersistentInteger(uniques[i])) == null);
		if(loInc) assert(fpt.get(new PersistentInteger(uniques[lo])) == null);
		if(hiInc) assert(fpt.get(new PersistentInteger(uniques[hi])) == null);
		//fpt.printLeaves();
		fpt.descendingMap();
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

		PersistentFPTree2<PersistentInteger, PersistentString> cfpt = new PersistentFPTree2<>(I, L);
		PutGetTask[] tasks = new PutGetTask[procs];
		for(int i = 0; i < procs; i++) tasks[i] = new PutGetTask(i, N, procs, readRatio, cfpt);

		multiThreadedTask(procs, tasks, 0);
		multiThreadedTask(procs, tasks, 3);
		multiThreadedTask(procs, tasks, 2);
		if(!verbose) assert(cfpt.verifyDelete() == true);
		else cfpt.verifyDelete("Multi threaded test |> ");

		/*
		multiThreadedTask(procs, tasks, 0);
		multiThreadedTask(procs, tasks, 1);
		multiThreadedTask(procs, tasks, 2);
		if(!verbose) assert(cfpt.verifyDelete() == true);
		else cfpt.verifyDelete("Multi threaded test |> ");*/
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
		final PersistentFPTree2<PersistentInteger, PersistentString> cfpt;
		public int taskCode;
		final int[] rands;
		final int T;

		public PutGetTask(int id, int N, int procs, double readRatio, PersistentFPTree2<PersistentInteger, PersistentString> cfpt) {
			this.id = id;
			this.N = N;
			this.procs = procs;
			this.readRatio = readRatio;
			this.cfpt = cfpt;
			this.taskCode = 0;
			this.T= N/procs;
			this.rands = new int[2*T];
			for(int i = 0; i < 2 * T; i++) rands[i] = ThreadLocalRandom.current().nextInt(0, N);
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
			for (int i = 0; i < 2*T; i++) {
				PersistentString val = cfpt.remove(new PersistentInteger(rands[i]));
				if(val != null) assert(Integer.parseInt(val.toString()) == rands[i]);
			}
		}

		private void get() {
			for (int i = 0; i < T; i++) {
				//PersistentString val = cfpt.get(new PersistentInteger(rands[i]));
				PersistentString val = cfpt.get(rands[i], PersistentInteger.class);
				if(val == null) throw new IllegalStateException("Thread " + id + "> FPTree GET FAILED for " + rands[i]);
				else assert(Integer.parseInt(val.toString()) == rands[i]);
			}
		}

		private void putGet() {
			for (Integer i = 0; i < T; i++) {
				int n = rands[T + i]; //ThreadLocalRandom.current().nextInt(0, N);
				if (ThreadLocalRandom.current().nextInt(0, 100) <= (int) (readRatio * 100)) {
					PersistentString ans = cfpt.get(n, PersistentInteger.class); //cfpt.get(new PersistentInteger(n));
				}
				else {
					cfpt.putIfAbsent(new PersistentInteger(n), new PersistentString(Integer.toString(n)));
				}
			}
		}
	}


	@SuppressWarnings("unchecked")
	public static boolean testPersistence(String[] args) {
		if (verbose) System.out.println("PersistentFPTree2: testing persistence across vm runs - test1");
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


		String id = "tests.fptree2_persistance.RC_EXP";
		PersistentFPTree2<PersistentInteger, PersistentString> cfptRC = ObjectDirectory.get(id, PersistentFPTree2.class);
		PersistentLong pl = ObjectDirectory.get("pseed2_EXP", PersistentLong.class);
		PersistentInteger mapSize = ObjectDirectory.get("map_size", PersistentInteger.class);
		if (cfptRC == null && pl == null) {
			cfptRC = new PersistentFPTree2<PersistentInteger, PersistentString>(I, L);
			pl = new PersistentLong(ThreadLocalRandom.current().nextLong(N));
			ObjectDirectory.put(id, cfptRC);
			ObjectDirectory.put("pseed2_EXP", pl);
			if (verbose) System.out.println("Saving to pmem...");
			Random rnd = new Random(pl.longValue());
			for(int i = 0; i < N; i++) {
				int rand = rnd.nextInt(N);
				cfptRC.put(new PersistentInteger(rand), new PersistentString(Integer.toString(rand)));
			}
			
			mapSize = new PersistentInteger(cfptRC.size());
			ObjectDirectory.put("map_size", mapSize);
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
			//System.out.println(cfptRC.size() + ", " + mapSize.intValue());
			assert(cfptRC.size() == mapSize.intValue());

		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public static boolean testPersistence2(String[] args) {
		if (verbose) System.out.println("PersistentFPTree2: testing persistence across vm runs - test2");
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

		String id = "tests.fptree2_persistance2.RC_EXP";
		PersistentFPTree2<PersistentInteger, PersistentString> cfptRC = ObjectDirectory.get(id, PersistentFPTree2.class);
		if (cfptRC == null) {
			cfptRC = new PersistentFPTree2<PersistentInteger, PersistentString>(I, L);
			ObjectDirectory.put(id, cfptRC);
			if (verbose) System.out.println("Saving to pmem...");
			Random rnd = new Random();
			for(int i = 0; i < N; i++) {
				int rand = i; //rnd.nextInt(N);
				cfptRC.put(new PersistentInteger(rand), new PersistentString(Integer.toString(rand)));
			}
			cfptRC.randomlyDeleteLeaves();
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

	@SuppressWarnings("unchecked")
	public static boolean testPersistence2old(String[] args) {
		if (verbose) System.out.println("PersistentFPTree2: testing persistence across vm runs - test2");
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

		String id = "tests.fptree2_persistance2_WITH_CLEANUP.RC";
		PersistentFPTree2<PersistentInteger, PersistentString> cfptRC2 = ObjectDirectory.get(id, PersistentFPTree2.class);
		if (cfptRC2 == null) {
			cfptRC2 = new PersistentFPTree2<PersistentInteger, PersistentString>(I, L);
			ObjectDirectory.put(id, cfptRC2);
			if (verbose) System.out.println("Saving to pmem...");
			Random rnd = new Random();
			for(int i = 0; i < N; i++) {
				int rand = i; //rnd.nextInt(N);
				cfptRC2.put(new PersistentInteger(rand), new PersistentString(Integer.toString(rand)));
			}
		}
		else {
			/*if (verbose) System.out.println("Retrieving from pmem...");
			HashMap<PersistentInteger, PersistentString> map = cfptRC2.getHashMap();
			//cfptRC.printLeaves();
			for(PersistentInteger k : map.keySet()) {
				assert(cfptRC2.get(k).equals(map.get(k)));
			}*/
		}
		return true;
	}

	public static boolean testIterators() {
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
		PersistentFPTree2<PersistentInteger, PersistentString> fpt = new PersistentFPTree2<>(I, L);

		for(int i = 0; i < N; i++) {
			int rand = ThreadLocalRandom.current().nextInt(0, N);
			fpt.put(new PersistentInteger(rand), new PersistentString(Integer.toString(rand)));
		}
		fpt.randomlyDeleteLeaves();

		/*if(verbose) {
			fpt.printLeaves();
			for(Map.Entry<PersistentInteger, PersistentString> e : fpt.entrySet()) System.out.print(e.getKey() + ",");
		}*/


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
