# Copyright (C) 2016  Intel Corporation
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

CC = g++
JAVAC = javac 
JAVA = java

JNI_INCLUDES = $(JAVA_HOME)/include $(JAVA_HOME)/include/linux

CFLAGS = -O3 -DNDEBUG -fPIC
JAVAFLAGS = -Xlint:unchecked
LINK_FLAGS = -fPIC -O3 -DNDEBUG -shared -lpmem -lpmemobj -Wl,-rpath,/usr/local/lib:/usr/local/lib64

CPP_SOURCE_DIR = src/main/cpp
JAVA_SOURCE_DIR = src/main/java
PACKAGE_NAME = lib/persistent
XPACKAGE_NAME = lib/xpersistent

TEST_DIR = src/test/java/$(PACKAGE_NAME)

TARGET_DIR = target
CPP_BUILD_DIR = $(TARGET_DIR)/cppbuild
CLASSES_DIR = $(TARGET_DIR)/classes
TEST_CLASSES_DIR = $(TARGET_DIR)/test_classes

ALL_CPP_SOURCES = $(wildcard $(CPP_SOURCE_DIR)/*.cpp)
ALL_JAVA_SOURCES = $(wildcard $(JAVA_SOURCE_DIR)/$(PACKAGE_NAME)/*.java) $(wildcard $(JAVA_SOURCE_DIR)/$(XPACKAGE_NAME)/*.java) $(wildcard $(JAVA_SOURCE_DIR)/$(PACKAGE_NAME)/*/*.java)
ALL_OBJ = $(addprefix $(CPP_BUILD_DIR)/, $(notdir $(ALL_CPP_SOURCES:.cpp=.o)))

ALL_TEST_SOURCES = $(addprefix $(TEST_DIR)/, TestUtil.java PersistentStringTest.java PersistentLongTest.java MultithreadTest.java ObjectDirectoryTest.java PersistentByteBufferTest.java PersistentTreeMapTest.java PersistentArrayTest.java MemoryRegionObjectTest.java TestTool.java)
ALL_TEST_CLASSES = $(addprefix $(TEST_CLASSES_DIR)/, $(notdir $(ALL_TEST_SOURCES:.java=.class)))

LIBRARIES = $(addprefix $(CPP_BUILD_DIR)/, libPersistent.so)

EXAMPLES_DIR = src/examples
ALL_EXAMPLE_DIRS = $(wildcard $(EXAMPLES_DIR)/*)
#$(addprefix $(EXAMPLES_DIR)/, reservations employees)

all: sources examples tests
sources: cpp java
cpp: $(LIBRARIES)
java: classes

examples: sources
	$(foreach example_dir,$(ALL_EXAMPLE_DIRS), $(JAVAC) $(JAVAFLAGS) -cp $(CLASSES_DIR):$(example_dir) $(example_dir)/*.java;)

clean: cleanex
	rm -rf target

cleanex:
	$(foreach example_dir,$(ALL_EXAMPLE_DIRS), rm -rf $(example_dir)/*.class;)

tests: $(ALL_TEST_CLASSES)
	$(foreach test,$^, $(JAVA) -ea -cp $(CLASSES_DIR):$(TEST_CLASSES_DIR) -Djava.library.path=$(CPP_BUILD_DIR) $(PACKAGE_NAME)/$(notdir $(test:.class=));)

$(LIBRARIES): | $(CPP_BUILD_DIR)
$(ALL_OBJ): | $(CPP_BUILD_DIR)
$(ALL_TEST_CLASSES): | $(TEST_CLASSES_DIR)

classes: | $(CLASSES_DIR)
	$(JAVAC) $(JAVAFLAGS) -d $(CLASSES_DIR) $(ALL_JAVA_SOURCES)

$(TEST_CLASSES_DIR)/%.class: $(TEST_DIR)/%.java
	$(JAVAC) -cp $(CLASSES_DIR):$(TEST_CLASSES_DIR) -d $(TEST_CLASSES_DIR) $<

$(CPP_BUILD_DIR)/%.so: $(ALL_OBJ)
	$(CC) -Wl,-soname,$@ -o $@ $(ALL_OBJ) $(LINK_FLAGS)

$(CPP_BUILD_DIR)/%.o: $(CPP_SOURCE_DIR)/%.cpp
ifndef JAVA_HOME
	$(error JAVA_HOME not set)
endif
	$(CC) $(CFLAGS) $(addprefix -I, $(JNI_INCLUDES)) -o $@ -c $<

$(CPP_BUILD_DIR):
	mkdir -p $(CPP_BUILD_DIR)

$(CLASSES_DIR):
	mkdir -p $(CLASSES_DIR)

$(TEST_CLASSES_DIR):
	mkdir -p $(TEST_CLASSES_DIR)
