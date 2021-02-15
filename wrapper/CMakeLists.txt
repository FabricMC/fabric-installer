include(third-party/cmake-bin2h/bin2h.cmake)

cmake_minimum_required(VERSION 3.17)

set(CMAKE_CXX_STANDARD 20)

# Generate a header with the installer jar baked into it.
bin2h(SOURCE_FILE "fabric-installer.jar" HEADER_FILE "fabric-installer.h" VARIABLE_NAME "INSTALLER_BIN")

project(FabricInstaller)

add_executable(FabricInstaller
        src/main.cpp
        src/InstallerWrapper.h
        src/InstallerWrapper.cpp
        third-party/tinyfiledialogs/tinyfiledialogs.h
        third-party/tinyfiledialogs/tinyfiledialogs.c
        fabric-installer.h
        )