cmake_minimum_required(VERSION 3.22)

project(rhythhaus_taglib LANGUAGES CXX)

add_library(rhythhaus_taglib SHARED
    src/rh_taglib.cpp
)

target_include_directories(rhythhaus_taglib
    PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)

target_compile_features(rhythhaus_taglib PRIVATE cxx_std_17)

find_package(TagLib QUIET)

if(TARGET TagLib::TagLib)
    target_link_libraries(rhythhaus_taglib PRIVATE TagLib::TagLib)
    target_compile_definitions(rhythhaus_taglib PRIVATE RH_TAGLIB_HAS_TAGLIB=1)
elseif(TARGET TagLib::tag)
    target_link_libraries(rhythhaus_taglib PRIVATE TagLib::tag)
    target_compile_definitions(rhythhaus_taglib PRIVATE RH_TAGLIB_HAS_TAGLIB=1)
else()
    find_package(PkgConfig QUIET)
    if(PkgConfig_FOUND)
        pkg_check_modules(TAGLIB QUIET taglib)
    endif()

    if(TAGLIB_FOUND)
        target_include_directories(rhythhaus_taglib PRIVATE ${TAGLIB_INCLUDE_DIRS})
        target_link_directories(rhythhaus_taglib PRIVATE ${TAGLIB_LIBRARY_DIRS})
        target_link_libraries(rhythhaus_taglib PRIVATE ${TAGLIB_LIBRARIES})
        target_compile_options(rhythhaus_taglib PRIVATE ${TAGLIB_CFLAGS_OTHER})
        target_compile_definitions(rhythhaus_taglib PRIVATE RH_TAGLIB_HAS_TAGLIB=1)
    else()
        message(STATUS "TagLib was not found by CMake find_package(TagLib) or pkg-config; building unsupported shim skeleton only.")
    endif()
endif()
