# CMake configuration for Vantafyn Native Retro Emulator Module
cmake_minimum_required(VERSION 3.22.1)
project(vantafyn_emulator C)

add_library(vantafyn_emulator SHARED
    vantafyn_host.c
    vantafyn_jni.c
)

target_include_directories(vantafyn_emulator PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

target_compile_options(vantafyn_emulator PRIVATE
    -std=c11
    -Wall
    -Wextra
    -O3
)

target_link_libraries(vantafyn_emulator PRIVATE
    android
    log
)
