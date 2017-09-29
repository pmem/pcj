#!/bin/bash

path="tools/validate"
cmdline='java -cp src:target/classes:target/test_classes -Djava.library.path=target/cppbuild examples.database.Database 1'
pass_count=0
total_count=0

function unit_tests {
    echo "Running unit tests..."
    make_res=`make`
    make_res=`make tests`
    if [[ $make_res =~ ([0-9]{2})/([0-9]{2}) ]]; then
        pass_count=${BASH_REMATCH[1]}
        total_count=${BASH_REMATCH[2]}
    fi
    echo "All unit tests completed."
}

function test1_persistence {
    echo "Testing that table test1 persisted properly..." 
    res1=`$cmdline $path/select_script1`
    if [[ $res1 =~ "Column family test1 does not exist" ]]; then
        echo "ERROR: table test1 in the example database is expected to persist across reboots, but cannot be found!"
        echo "Ignore this error if this is the first time setup."
        eval $cmdline $path/insert_script1
        res1=`$cmdline $path/select_script1`
    else
        pass_count="$((pass_count+1))"
    fi
    num=$(echo "$res1" | wc -l)
    # there are 2 extra lines of output for wc -l
    if [ "$num" == 1002 ]; then
        pass_count="$((pass_count+1))"
    fi
    total_count="$((total_count+2))"
    echo "All table test1 tests completed."
}

function test2_recreation {
    echo "Testing creation of table test2..."
    res2=`$cmdline $path/select_script2`
    if [[ $res2 =~ "Column family test2 does not exist" ]]; then
        echo "Inserting 1000 elements into example database, table test2; this may take some time."
        eval $cmdline $path/insert_script2
        res1=`$cmdline $path/select_script2`
        pass_count="$((pass_count+1))"
    else
        echo "ERROR: table test2 in the example database should have been deleted in the last round of testing, but still exists!"
    fi
    total_count="$((total_count+1))"
    eval $cmdline $path/drop_script
    echo "All table test2 tests completed."
}

cd ../../
unit_tests
test1_persistence
test2_recreation
cd tools/validate

echo "Test result: $pass_count/$total_count passed."
