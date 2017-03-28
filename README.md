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
9.  ObjectDirectory
10. Boxed primitives
11. String
12. AtomicReference
  
This Java library uses the libpmemobj library from the Non-Volatile Memory Library (NVML). 
For more information on NVML, please visit http://pmem.io and https://github.com/pmem/nvml.

## HOW TO BUILD & RUN ##

### PREREQUISITES TO BUILD ###
The following are the prerequisites for building this Java library:

1. Linux operating system (tested on CentOS 7.2 and Ubuntu 16.04)
2. Non-Volatile Memory Library (NVML) -- to install it, please visit https://github.com/pmem/nvml
3. Java 8 or above
4. Build tools - g++ compiler and make

### PREREQUISITES TO RUN ###
This library assumes the availability of hardware persistent memory or emulated persistent memory 
at the ```/mnt/mem``` directory (with read/write permissions for the user).  Instructions for creating 
emulated persistent memory at ```/mnt/mem``` is shown below.

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
   $ make
   ```
Other available Makefile targets include:

   - `sources` - builds only sources
   - `examples` - builds and runs the sources and a test example

### USING THIS LIBRARY IN EXISTING JAVA APPLICATIONS ###
To import this library into an existing Java application, include the project's target/classes 
directory in your Java classpath and the project's ```target/cppbuild``` directory in your 
```java.library.path```.  For example: 
   ```
   $ javac -cp .:<path>/pcj/target/classes <source>
   $ java -cp .:<path>/pcj/target/classes -Djava.library.path=<path>/pcj/target/cppbuild <class>
   ```
## NOTES ON FUNCTIONALITY ##
1. While we believe that the programming model and general style of the APIs presented here is 
   headed in the right direction, the current implementation includes some choices that were made 
   with expediency in mind.  For example:

   - extensive use of JNI native methods
   - extensive use of synchronized methods for thread safety
   - use of reference counting for automatic object deallocation

2. By default, the size of the NVML pool (and hence the maximum aggregate size of all persistent 
   objects created with this library) is 2GB-1, which is the maximum pool size.  To modify the pool 
   size, you can change the value for ```PM_POOL_SIZE``` in ```pcj/src/main/cpp/persistent_heap.cpp```.

3. By default, the mount point to the NVML pool is ```/mnt/mem``` directory. To modify this path, you 
   can change the value for ```PATH``` in ```pcj/src/main/cpp/persistent_heap.cpp```.

4. ```PersistentString``` objects are backed by a byte array and only supports ASCII characters.

5. Core dumps may occur if the emulated memory is not properly set up; for example, running with insufficient
   privileges or with a nonexistent mount point.

## Contacts ##

For more information on this library, contact Lei Fan (lei.t.fan@intel.com) or Steve Dohrmann (steve.dohrmann@intel.com).

\* Other names and brands may be claimed as the property of others.
