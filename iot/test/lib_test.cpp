#include <gtest/gtest.h>
#include <lib.h>

TEST(LibTest, TestExampleReturn) {
    EXPECT_EQ(example_function(), 42);
}
