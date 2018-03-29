/* Copyright (C) 2018  Intel Corporation
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

package examples.wordfrequency;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import lib.util.persistent.PersistentHashMap;
import lib.util.persistent.PersistentSkipListMap;
import lib.util.persistent.PersistentString;
import lib.util.persistent.PersistentInteger;
import lib.util.persistent.ObjectDirectory;
import java.util.Map;
import java.util.function.BiFunction;

public class PersistentParallelWordFrequency {
	private static Map<PersistentString, PersistentInteger> counts = getOrInitializeCounts();

	@SuppressWarnings("unchecked")
	private static Map<PersistentString, PersistentInteger> getOrInitializeCounts() {
		String DATA_KEY = "WordFrequencyData";
		PersistentSkipListMap<PersistentString, PersistentInteger> map = ObjectDirectory.get(DATA_KEY, PersistentSkipListMap.class);
		if (map == null) ObjectDirectory.put(DATA_KEY, map = new PersistentSkipListMap<>());
		return map;
	}

	public static void main(String[] args) throws InterruptedException {
		if (args.length == 0) System.out.println("usage: PersistentParallelWordFrequency <list of files to process>");
		final PersistentInteger ONE = new PersistentInteger(1);
		Thread[] ts = new Thread[args.length];
		for (int i = 0; i < args.length; i++) {
			int ii = i;
			ts[ii] = new Thread(() -> {
				try {
					Scanner scanner = new Scanner(new File(args[ii]));
					while (scanner.hasNext()) {
						PersistentString word = new PersistentString(scanner.next());
						counts.merge(word, ONE, PersistentParallelWordFrequency::sum);
					}
				}
				catch (FileNotFoundException fnf) {throw new RuntimeException(fnf.getCause());}
			});
		}
		for (Thread t : ts) t.start();
		for (Thread t : ts) t.join();

		// print current counts
		for (Map.Entry<PersistentString, PersistentInteger> e : counts.entrySet()) {
			System.out.format("%d %s\n", e.getValue().intValue(), e.getKey());
		}
	}

    public static PersistentInteger sum(PersistentInteger x, PersistentInteger y) {
        return new PersistentInteger(x.intValue() + y.intValue());
    }
}