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
import java.util.TreeMap;
import java.util.Map;

public class WordFrequency {
	private static Map<String, Integer> counts = new TreeMap<>();

	public static void main(String[] args) {
		if (args.length == 0) System.out.println("usage: WordFrequency <list of files to process>");
		for (int i = 0; i < args.length; i++) {
			try {
				Scanner scanner = new Scanner(new File(args[i]));
				while (scanner.hasNext()) {
					String word = scanner.next();
					counts.merge(word, 1, Integer::sum);
				}
			}
			catch (FileNotFoundException fnf) {throw new RuntimeException(fnf.getCause());}
		}

		// print counts
		for (Map.Entry<String, Integer> e : counts.entrySet()) {
			System.out.format("%d %s\n", e.getValue().intValue(), e.getKey());
		}
	}

}