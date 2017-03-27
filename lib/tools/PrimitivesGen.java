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

public class PrimitivesGen {
	public static void main(String[] args) throws Exception {
		STGroup group = new STGroupFile("./lib/tools/code.stg");
		String templateName = "primitives_class";
		String[] classes  = new String[] {"Byte", "Short", "Character", "Integer", "Long", "Boolean", "Double", "Float"};
		String[] identifiers = new String[] {"Byte", "Short", "Char", "Int", "Long", "Boolean", "Double", "Float"};
		String[] fields = new String[] {"BYTE", "SHORT", "CHAR", "INT", "LONG", "BOOLEAN", "DOUBLE", "FLOAT"};
		for (int i = 0; i < classes.length; i++) {
				ST st = group.getInstanceOf(templateName);
				st.add("class", classes[i]);
				st.add("identifier", identifiers[i]);
				st.add("field", fields[i]);
				st.add("smallcaps", fields[i].toLowerCase());

                switch (i){
                    case 0: st.add("byte", i);
                            break;
                    case 1: st.add("short", i);
                            break;
                    case 2: st.add("char", i);
                            break;
                    case 3: st.add("int", i);
                            break;
                    case 4: st.add("long", i);
                            break;
                    case 5: st.add("bool", i);
                            break;
                    case 6: st.add("double", i);
                            break;
                    case 7: st.add("float", i);
                            break;
                    default:break;
                }
				String source = st.render();
				Files.write(Paths.get("./src/main/java/lib/util/persistent/Persistent" + classes[i] + ".java"), source.getBytes());
		}
	}
}
