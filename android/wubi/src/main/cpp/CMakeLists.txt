cmake_minimum_required(VERSION 3.22.1)
project("wubi-engine")

add_library(
    wubi-engine
    SHARED
    wubi_engine.cpp
    wubi_dict.cpp
    wubi_search.cpp
)

target_link_libraries(
    wubi-engine
    android
    log
)
