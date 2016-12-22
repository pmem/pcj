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

import lib.persistent.Util;

public class Driver
{
    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String s = args[i];
            System.out.println("-- " + s + " --");
            switch(s) {
                case "script1": script1(); break;
                case "script2": script2(); break;
                case "script3": script3(5000); break;
                case "put": script3(Integer.parseInt(args[i += 1])); break;
                case "list": Reservations.listReservations(); break;
                case "clear": Reservations.clearReservations(); break;
                case "debug": Util.debugPool(); break;
                case "null": break;
            }
        }
    }

    public static void script1() {
        Reservations.addReservation("Bob", "2017-12-03T11:00:00.00Z", "storage");
        Reservation r = Reservations.getReservation("Bob", "2017-12-03T11:00:00.00Z");
        System.out.println(r);
        Reservations.addReservation("Anil", "2017-12-03T13:15:00.00Z", "performance");
        Reservations.addReservation("Sue", "2017-12-03T10:30:00.00Z", "data consistency");
    }

    public static void script2() {
        Reservations.addReservation("Bob", "2017-12-03T11:30:00.00Z", "power");
        Reservations.removeReservation("Bob", "2017-12-03T11:00:00.00Z");
    }

    public static void script3(int N) {
        System.out.print("adding " + N + " reservations... ");
        for (int i = 0; i < N; i++) {
            Reservations.addReservation("Lawrence_" + i, "2017-10-17T15:00:00.00Z", "issue_" + i);
        }
        System.out.println("done.");
    }
}



