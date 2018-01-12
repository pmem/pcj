cmake_minimum_required(VERSION 2.8.9)
project(llpl)
set(CMAKE_BUILD_TYPE Release)

#include Java packages
find_package(Java REQUIRED)
FIND_PACKAGE(JNI REQUIRED)

link_directories(/usr/local/lib64)

INCLUDE_DIRECTORIES(${JAVA_INCLUDE_PATH})
INCLUDE_DIRECTORIES(${JAVA_INCLUDE_PATH2})
LINK_DIRECTORIES(/usr/lib)

include_directories(include)

#Add the source files
file(GLOB SOURCES "*.cpp")

#Generate the shared library from the sources
add_library(llpl SHARED ${SOURCES})
#Add dependency for memkind library
target_link_libraries(llpl pmemobj)
