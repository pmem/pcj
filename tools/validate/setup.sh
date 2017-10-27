#!/bin/bash

path="tools/validate"
cd ../../
echo "Compiling..."
out=`make examples`
echo "Populating table test1 for testing data persistence across reboots..."
cmdline='java -cp .:src:target/classes:target/test_classes -Djava.library.path=target/cppbuild examples.database.Database 1'
out=eval $cmdline $path/insert_script1
echo "Done."
cd $path
