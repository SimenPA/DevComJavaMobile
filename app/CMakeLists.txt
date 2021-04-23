# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.6.0)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

set(OPENSSL_DIR "app/libs" )
#set(OPENSSL_LIBRARIES_DIR "src/main/cpp/openssl/armeabi-v7a/lib" )
#set(OPENSSL_INCLUDE_DIR "src/main/cpp/openssl/armeabi-v7a/include" )
#set(OPENSSL_LIBRARIES "ssl" "crypto")

add_library( # Sets the name of the library.
        native-lib

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        src/main/cpp/native-lib.cpp)

add_library(
        devcom
        SHARED
        src/main/cpp/devcom/security.c
)

add_library(
        ssl STATIC IMPORTED
)
set_target_properties(ssl PROPERTIES IMPORTED_LOCATION ${PROJECT_SOURCE_DIR}/../${OPENSSL_DIR}/${ANDROID_ABI}/lib/libssl.a)

add_library(
        crypto STATIC IMPORTED
)
set_target_properties(crypto PROPERTIES IMPORTED_LOCATION ${PROJECT_SOURCE_DIR}/../${OPENSSL_DIR}/${ANDROID_ABI}/lib/libcrypto.a)

include_directories(libs/${ANDROID_ABI}/include)

target_link_libraries(devcom ssl crypto)
target_link_libraries(native-lib ssl crypto)

#target_link_libraries(
#        native-lib
#        ${OPENSSL_LIBRARIES}
#)

target_link_libraries(
        native-lib
        devcom
)


# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

#target_link_libraries( # Specifies the target library.
#        native-lib
#        devcom
#        # Links the target library to the log library
#        # included in the NDK.
#        ${OPENSSL_LIBRARIES}
#        ${log-lib})
#
#target_link_libraries(
#        devcom
#        ${log-lib})

target_link_libraries(
       devcom
       ${log-lib})

target_link_libraries(
        native-lib
        ${log-lib})