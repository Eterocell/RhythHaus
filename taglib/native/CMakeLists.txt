cmake_minimum_required(VERSION 3.22)

project(rhythhaus_taglib LANGUAGES CXX)

include(FetchContent)

option(RHYTHHAUS_TAGLIB_BUILD_JNI "Build the JVM JNI entrypoints into rhythhaus_taglib" OFF)

# TagLib is linked into the RhythHaus shared library, so the upstream static
# archive must be built as position-independent code.
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# Keep the upstream dependency definition in native build code instead of
# Gradle shell checkout/build tasks. These cache variables match TagLib's CMake
# options where available; unknown ones are harmless.
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(BUILD_TESTING OFF CACHE BOOL "" FORCE)
set(BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(BUILD_BINDINGS OFF CACHE BOOL "" FORCE)
set(WITH_ZLIB OFF CACHE BOOL "" FORCE)
set(TAGLIB_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(TAGLIB_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(TAGLIB_BUILD_BINDINGS OFF CACHE BOOL "" FORCE)

FetchContent_Declare(
    taglib
    GIT_REPOSITORY https://github.com/taglib/taglib.git
    GIT_TAG 1b94b93762636ebe5733180c3e825be4621e4c7f
    GIT_SHALLOW FALSE
)
FetchContent_MakeAvailable(taglib)

set(RHYTHHAUS_TAGLIB_SOURCES
    src/rh_taglib.cpp
)

if(RHYTHHAUS_TAGLIB_BUILD_JNI)
    list(APPEND RHYTHHAUS_TAGLIB_SOURCES jni/rh_taglib_jni.cpp)
endif()

add_library(rhythhaus_taglib SHARED
    ${RHYTHHAUS_TAGLIB_SOURCES}
)

target_include_directories(rhythhaus_taglib
    PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)

target_compile_features(rhythhaus_taglib PRIVATE cxx_std_17)

target_include_directories(rhythhaus_taglib
    PRIVATE
        ${taglib_SOURCE_DIR}/taglib
        ${taglib_SOURCE_DIR}/taglib/toolkit
        ${taglib_BINARY_DIR}
        ${taglib_BINARY_DIR}/taglib
)

if(RHYTHHAUS_TAGLIB_BUILD_JNI)
    find_package(JNI REQUIRED)
    target_include_directories(rhythhaus_taglib PRIVATE ${JNI_INCLUDE_DIRS})
endif()

if(TARGET TagLib::tag)
    target_link_libraries(rhythhaus_taglib PRIVATE TagLib::tag)
    target_compile_definitions(rhythhaus_taglib PRIVATE RH_TAGLIB_HAS_TAGLIB=1)
elseif(TARGET TagLib::TagLib)
    target_link_libraries(rhythhaus_taglib PRIVATE TagLib::TagLib)
    target_compile_definitions(rhythhaus_taglib PRIVATE RH_TAGLIB_HAS_TAGLIB=1)
elseif(TARGET tag)
    target_link_libraries(rhythhaus_taglib PRIVATE tag)
    target_compile_definitions(rhythhaus_taglib PRIVATE RH_TAGLIB_HAS_TAGLIB=1)
elseif(TARGET taglib)
    target_link_libraries(rhythhaus_taglib PRIVATE taglib)
    target_compile_definitions(rhythhaus_taglib PRIVATE RH_TAGLIB_HAS_TAGLIB=1)
else()
    message(FATAL_ERROR "Fetched TagLib did not expose a known CMake target")
endif()
