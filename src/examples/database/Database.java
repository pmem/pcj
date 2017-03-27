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

package examples.database;

import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.util.stream.*;
import lib.util.persistent.*;
import lib.util.persistent.spi.*;

@SuppressWarnings("unchecked")
public class Database {

    static PersistentSkipListMap<PersistentString, ColumnFamily> keyspace;

    public static void main(String[] args) {
        PersistentMemoryProvider.getDefaultProvider().getHeap().open();
        if (ObjectDirectory.get("keyspace", PersistentSkipListMap.class) == null) {
            keyspace = new PersistentSkipListMap<>();
            ObjectDirectory.put("keyspace", keyspace);
        } else {
            keyspace = ObjectDirectory.get("keyspace", PersistentSkipListMap.class);
        }

        int numThreads = 0;
        if (args.length != 0) {
            numThreads = Integer.parseInt(args[0]);
        } else {
            System.out.println("usage: Database <number of threads>");
            return;
        }

        if (args.length >= 2) {
            Thread[] threads = new Thread[numThreads];
            for (int i = 0; i < numThreads; i++) {
                final int ii = i;
                threads[i] = new Thread( () -> {
                    try (Stream<String> stream = Files.lines(Paths.get(args[1]))) {
                        stream.forEach(examples.database.Database::handleCommand);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                threads[i].start();
            }
            for (int i = 0; i < numThreads; i++) {
                try {
                    threads[i].join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
            }
            return;
        }

        Console c = System.console();
        if (c == null) {
            System.out.println("No console.");
            System.exit(1);
        }

        boolean exit = false;
        while (!exit) {
            String cmdline = c.readLine("Command (CREATE TABLE, SELECT, UPDATE, INSERT INTO, DELETE FROM, LIST, DROP TABLE, BULK INSERT, EXIT): ");
            exit = handleCommand(cmdline);
        }
    }

    static boolean handleCommand(String cmdline) {
        String[] cmdArgs = cmdline.split(" ");
        switch(cmdArgs[0]) {
            case "CREATE":
            case "create":
                handleCreate(cmdArgs);
                break;
            case "SELECT":
            case "select":
                handleSelect(cmdArgs);
                break;
            case "UPDATE":
            case "update":
                handleUpdate(cmdArgs);
                break;
            case "INSERT":
            case "insert":
                handleInsert(cmdArgs);
                break;
            case "DELETE":
            case "delete":
                handleDelete(cmdArgs);
                break;
            case "LIST":
            case "list":
                System.out.println(keyspace.keySet());
                break;
            case "DROP":
            case "drop":
                handleDrop(cmdArgs);
                break;
            case "BULK":
            case "bulk":
                handlebulkInsert(cmdArgs);
                break;
            case "EXIT":
            case "exit":
                return true;
            default:
                System.out.println("Command should start with one of CREATE TABLE, SELECT, UPDATE, INSERT INTO, DELETE FROM, LIST, DROP TABLE, BULK INSERT, or EXIT!");
        }
        return false;
    }

    static void handleCreate(String[] args) {
        if (args.length < 4) {
            System.out.println("usage: CREATE TABLE <table name> <key column name> [value column name1] ... [value column name n]");
            return;
        }
        if (!(args[1].equals("TABLE")) &&
            !(args[1].equals("table"))) {
            System.out.println("usage: CREATE TABLE <table name> <key column name> [value column name1] ... [value column name n]");
            return;
        }
        // ignore 0th ("CREATE") and 1st ("TABLE")
        String tableName = args[2];
        String[] colNames = Arrays.copyOfRange(args, 3, args.length);
        Transaction.run(() -> {
            ColumnFamily cf = new ColumnFamily(colNames);
            keyspace.put(new PersistentString(tableName), cf);
        });
    }

    static void handleInsert(String[] args) {
        if (args.length < 3) {
            System.out.println("usage: INSERT INTO <table name> [key value] [column value 1] ... [column value n]");
            return;
        }
        if (!(args[1].equals("INTO")) &&
            !(args[1].equals("into"))) {
            System.out.println("usage: INSERT INTO <table name> [key value] [column value 1] ... [column value n]");
            return;
        }
        ColumnFamily cf = keyspace.get(new PersistentString(args[2]));
        if (cf == null) {
            System.out.println("Column family " + args[2] + " does not exist.");
            return;
        }
        if (args.length > (3 + cf.colCount())) {
            System.out.println("Too many columns in INSERT");
            return;
        }
        // ignore 0th ("INSERT") and 1st ("INTO")

        if (args.length == 3) return;   // do nothing
        Transaction.run(() -> {
            Key key = new Key(args[3]);
            Value value = new Value(cf.colCount()-1);
            long ts = System.currentTimeMillis();
            // example: INSERT INTO <tableName> <key> <vals>
            // i starts at the 4th element, where vals start
            // i-3 because colNames() include key column name, so start counting at 1
            for (int i = 4; i < args.length; i++) {
                Cell c = new Cell(cf.colNames().get(i-3), args[i], ts);
                value.set(i-4, c);
            }
            cf.put(key, value);
        });
    }

    static void handleSelect(String[] args) {
        if (args.length != 4) {
            System.out.println("usage: SELECT * FROM <table name>");
            return;
        }

        String tableName = args[args.length-1];
        String[] cols = Arrays.copyOfRange(args, 1, args.length-2);
        ColumnFamily cf = keyspace.get(new PersistentString(tableName));
        if (cf == null) {
            System.out.println("Column family " + tableName + " does not exist.");
            return;
        }

        printTable(cf, cols);
    }

    static void printTable(ColumnFamily cf, String[] cols) {
        printHeader(cf, cols);
        printCells(cf, cols);
    }

    static void printHeader(ColumnFamily cf, String[] cols) {
        if (cols[0].equals("*")) {
            System.out.println(cf.colNames());
        } else {
            /*Set<String> colSet = new HashSet<String>(Arrays.asList(cols));
            StringBuilder b = new StringBuilder();
            b.append("{" + cf.colNames().get(0));
            for (int i = 1; i < cf.colNames().size(); i++) {
                if (colSet.contains(cf.colNames().get(i).toString())) {
                    b.append(", " + cf.colNames().get(i));
                }
            }
            b.append("}");
            System.out.println(b.toString());*/
        }
    }

    static void printCells(ColumnFamily cf, String[] cols) {
        if (cols[0].equals("*")) {
            for (Map.Entry<Key, Value> e: cf.table().entrySet()) {
                System.out.println(e.getKey() + ", " + e.getValue());
            }
        } else {
        }
    }

    static void handleUpdate(String[] args) {
        if (args.length != 8) {
            System.out.println("usage: UPDATE <table name> SET <column name> <new value> WHERE <key column name> <key value>");
            return;
        }
        if (!(args[2].equals("SET")) &&
            !(args[2].equals("set"))) {
            System.out.println("usage: UPDATE <table name> SET <column name> <new value> WHERE <key column name> <key value>");
            return;
        }
        if (!(args[5].equals("WHERE")) &&
            !(args[5].equals("where"))) {
            System.out.println("usage: UPDATE <table name> SET <column name> <new value> WHERE <key column name> <key value>");
            return;
        }

        String tableName = args[1];
        ColumnFamily cf = keyspace.get(new PersistentString(tableName));
        if (cf == null) {
            System.out.println("Column family " + tableName + " does not exist.");
            return;
        }

        String keyColName = args[6];
        if (!(keyColName.equals(cf.colNames().get(0).toString()))) return;

        Transaction.run(() -> {
            Key key = new Key(args[7]);
            long ts = System.currentTimeMillis();
            PersistentString fieldName = new PersistentString(args[3]);
            String fieldValue = args[4];
            Cell newCell = new Cell(fieldName, fieldValue, ts);
            Value val = cf.get(key);
            for (int i = 1; i < cf.colNames().length(); i++) {
                if (cf.colNames().get(i).equals(fieldName)) {
                    val.set(i-1, newCell);
                    return;
                }
            }
        });
    }

    static void handleDelete(String[] args) {
        if (args.length < 4) {
            System.out.println("usage: DELETE FROM <table name> <key value>");
            return;
        }
        if (!(args[1].equals("FROM")) &&
            !(args[1].equals("from"))) {
            System.out.println("usage: DELETE FROM <table name> <key value>");
            return;
        }

        String tableName = args[2];
        ColumnFamily cf = keyspace.get(new PersistentString(tableName));
        if (cf == null) {
            System.out.println("Column family " + tableName + " does not exist.");
            return;
        }

        Key key = new Key(args[3]);
        if (cf.remove(key) == null) {
            System.out.println("Key " + args[3] + " does not exist in table " + tableName + ".");
        }
    }

    static void handleDrop(String[] args) {
        if (args.length < 3) {
            System.out.println("usage: DROP TABLE <table name>");
            return;
        }
        if (!(args[1].equals("TABLE")) &&
            !(args[1].equals("table"))) {
            System.out.println("usage: DROP TABLE <table name>");
            return;
        }

        if (keyspace.remove(new PersistentString(args[2])) == null) {
            System.out.println("Table " + args[2] + " does not exist.");
        }
    }

    static void handlebulkInsert(String[] args){
        if (args.length < 3) {
            System.out.println("usage: BULK INSERT <table name> [num]");
            return;
        }
        if (!(args[1].equals("INSERT")) &&
            !(args[1].equals("insert"))) {
            System.out.println("usage: BULK INSERT <table name> [num]");
            return;
        }
        ColumnFamily cf = keyspace.get(new PersistentString(args[2]));
        if (cf == null) {
            System.out.println("Column family " + args[2] + " does not exist.");
            return;
        }
        if (args.length > 4) {
            System.out.println("Too many arguments for BULK INSERT");
            return;
        }
        int iteration = 1;
        if (args.length == 4){
            try{
                iteration = Integer.parseInt(args[3]);
            }catch(NumberFormatException e){
                System.out.println("Too many arguments for BULK INSERT");
                return;
            }
        }
        final int iterations = iteration;
        // ignore 0th ("INSERT") and 1st ("INTO")
        //Transaction.run(() -> {
            for (int i = 0; i < iterations; i++) {
                final long ts = System.currentTimeMillis();
                Key key = new Key("key_"+i);
                Value value = new Value(cf.colCount()-1);
                // example: INSERT INTO <tableName> <key> <vals>
                // i starts at the 4th element, where vals start
                // i-3 because colNames() include key column name, so start counting at 1
                for (int j = 1; j < cf.colCount(); j++) {
                    Cell c = new Cell(cf.colNames().get(j), "Column"+j+"_"+ts, ts);
                    value.set(j-1, c);
                }
                cf.put(key, value);
            }
        //});
    }
}
