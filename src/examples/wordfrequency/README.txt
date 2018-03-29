README for word frequency sample code

There are 3 versions of a word frequency counting program:

WordFrequency.java -- counts word frequency of supplied text files using a sorted map
ParalllelWordFrequency.java -- as above but uses one thread per text file
PersistentParalllelWordFrequency.java -- as above but accumulates results in a persistent sorted map

These files will compile as part of the PCJ project 'make' if added to the src/examples folder.

To compile these outside of the PCJ make process:

javac -cp target/classes:src src/examples/wordfrequency/*.java

Below is an example command line to run one of the programs from the top level pcj project directory:

java -cp target/classes:src -Djava.library.path=target/cppbuild/ examples.wordfrequency.WordFrequency <list of text files>
