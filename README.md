# Persistent Collections for Java* #


## OVERVIEW ##
This is a "pilot" project to develop a library for Java objects stored in persistent memory.
Persistent collections are being emphasized because many applications for persistent memory
seem to map well to the use of collections.  One of this project's goals is to make programming 
with persistent objects feel natural to a Java developer, for example, by using familiar Java
constructs when incorporating persistence elements such as data consistency and object lifetime.  

The breadth of persistent types is currently limited and the code is not performance-optimized.
We are making the code available because we believe it can be useful in experiments to retrofit 
existing Java code to use persistent memory and to explore persistent Java programming in general.

This library provides Java classes whose instances can persist (i.e. remain reachable) beyond the
life of a Java VM instance. Persistent classes include:

1.  Primitive arrays
2.  Generic arrays
3.  Tuples
4.  ArrayList
5.  HashMap
6.  LinkedList
7.  LinkedQueue
8.  SkipListMap
9.  FPTree
10. SIHashMap
11. ObjectDirectory
12. Boxed primitives
13. String
14. AtomicReference
15. ByteBuffer

This Java library uses the libpmemobj library from the Persistent Memory Development Kit (PMDK). 
For more information on PMDK, please visit http://pmem.io and https://github.com/pmem/pmdk.

For a brief introduction on use of the library, please see [Introduction.txt](Introduction.txt).

## HOW TO BUILD & RUN ##

### PREREQUISITES TO BUILD ###
The following are the prerequisites for building this Java library:

1. Linux operating system (tested on CentOS 7.2 and Ubuntu 16.04)
2. Persistent Memory Development Kit (PMDK)
3. Java 8 or above
4. Build tools - g++ compiler and make

### PREREQUISITES TO RUN ###
This library assumes the availability of hardware persistent memory or emulated persistent memory.
Properties of this memory such as path and size can be specified in the [config.properties](config.properties) file at 
the top level directory. Further information can be found inside the file. 

Instructions for creating emulated persistent memory are shown below.

### EMULATING PERSISTENT MEMORY ###
The preferred way is to create an in-memory DAX file system. This requires Linux kernel 4.2 or 
greater. Please follow the steps at:

   http://pmem.io/2016/02/22/pm-emulation.html

Alternatively, for use with older kernels, create a tmpfs partition as follows (as root):
   ```
   $ mount -t tmpfs -o size=4G tmpfs /mnt/mem  # creates a 4GB tmpfs partition
   $ chmod -R a+rw /mnt/mem                    # enables read/write permissions to all users
   ```
### STEPS TO BUILD AND RUN TESTS ###
Once all the prerequisites have been satisfied:
   ```
   $ git clone https://github.com/pmem/pcj
   $ cd pcj
   $ make && make tests
   ```
Available Makefile targets include:

   - `sources` - builds only sources
   - `examples` - builds the sources and examples
   - `tests` - builds and runs tests

### USING THIS LIBRARY IN EXISTING JAVA APPLICATIONS ###
To import this library into an existing Java application, include the project's target/classes 
directory in your Java classpath and the project's ```target/cppbuild``` directory in your 
```java.library.path```.  For example: 
   ```
   $ javac -cp .:<path>/pcj/target/classes <source>
   $ java -cp .:<path>/pcj/target/classes -Djava.library.path=<path>/pcj/target/cppbuild <class>
   ```

## NOTES ON FUNCTIONALITY ##
1. ```PersistentString``` objects are backed by a byte array and only supports ASCII characters.

## CONTRIBUTING ##
Thanks for your interest! Right now, substantial architectural changes are still happening in the
project.  This makes it difficult to contribute code and difficult to effectively process pull
requests.  We expect these changes to settle out around December of this year and we look forward to
code contributions once this happens.  We will update this README then.

In the meantime, we would love to hear your comments and suggestions via the contacts listed below.

## Contacts ##
For more information on this library, contact Olasoji Denloye (olasoji.denloye@intel.com) or Steve Dohrmann
(steve.dohrmann@intel.com).

\* Other names and brands may be claimed as the property of others.
