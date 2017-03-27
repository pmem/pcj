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

package lib.tools;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.IntStream;
import org.stringtemplate.v4.*;

public class ArraysGen {
	public static void main(String[] args) throws Exception {
		STGroup group = new STGroupFile("./lib/tools/code.stg");
		String[] templateNames = new String[] {"mutable_arrays", "immutable_arrays"};
		String[] classPrefixes = new String[] {"Persistent", "PersistentImmutable"};
		String[] identifiers = new String[] {"Byte", "Short", "Char", "Int", "Long", "Boolean", "Double", "Float"};
		String[] fields = new String[] {"BYTE", "SHORT", "CHAR", "INT", "LONG", "BOOLEAN", "DOUBLE", "FLOAT"};
		for (int j = 0; j < classPrefixes.length; j++) {
		    for (int i = 0; i < fields.length; i++) {
				ST st = group.getInstanceOf(templateNames[j]);
				st.add("identifier", identifiers[i]);
				st.add("field", fields[i]);
				st.add("smallcaps", fields[i].toLowerCase());
				String source = st.render();
				Files.write(Paths.get("./src/main/java/lib/util/persistent/"+ classPrefixes[j] + identifiers[i] + "Array.java"), source.getBytes());
				Files.write(Paths.get("./tmp/"+ classPrefixes[j] + identifiers[i] + "Array.java"), source.getBytes());
		    }
        }
	}
}
