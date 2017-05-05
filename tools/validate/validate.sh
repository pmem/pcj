# Copyright (C) 2017  Intel Corporation
# 
# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License
# version 2 only, as published by the Free Software Foundation.
# This file has been designated as subject to the "Classpath"
# exception as provided in the LICENSE file that accompanied
# this code.
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License version 2 for more details (a copy
# is included in the LICENSE file that accompanied this code).
# 
# You should have received a copy of the GNU General Public License
# version 2 along with this program; if not, write to the Free
# Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
# Boston, MA  02110-1301, USA.

#!/bin/bash

path="tools/validate"
cmdline='java -cp src:target/classes:target/test_classes -Djava.library.path=target/cppbuild examples.database.Database 1'
pass_count=0
total_count=0

function unit_tests {
    echo "Running unit tests..."
    make_res=`make`
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
        echo "Inserting 1000 elements into example database, table test2; this may take several seconds."
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
