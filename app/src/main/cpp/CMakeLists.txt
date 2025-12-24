cmake_minimum_required(VERSION 3.22.1)
project(ble_native)

add_library(ble_native SHARED
    jni_bridge.cpp
)

# Shared parser sources (repo_root/shared/ble_parser)
set(SHARED_DIR ${CMAKE_SOURCE_DIR}/../../../../shared/ble_parser)

add_library(ble_parser STATIC
    ${SHARED_DIR}/crc16.cpp
    ${SHARED_DIR}/packet.cpp
)

target_include_directories(ble_native PRIVATE
    ${SHARED_DIR}
)

target_include_directories(ble_parser PUBLIC
    ${SHARED_DIR}
)

find_library(log-lib log)

target_link_libraries(ble_native
    ble_parser
    ${log-lib}
)
