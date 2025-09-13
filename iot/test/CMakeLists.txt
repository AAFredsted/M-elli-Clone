include(FetchContent)

find_package(GTest)

if (NOT GTest_FOUND)
    FetchContent_Declare(
        googletest
        URL "https://github.com/google/googletest/releases/download/v1.15.2/googletest-1.15.2.tar.gz"
        DOWNLOAD_EXTRACT_TIMESTAMP NEW
    )
    FetchContent_MakeAvailable(googletest)
endif()

enable_testing()

add_executable(test_runner lib_test.cpp)
target_link_libraries(test_runner GTest::gtest_main lib)

include(GoogleTest)
gtest_discover_tests(test_runner)
