cmake_minimum_required(VERSION 3.19)

if(NOT CI)
    include($ENV{IDF_PATH}/tools/cmake/project.cmake)
endif()

project(muelli-iot)

if(CI)
    # We mock the idf_component_register function to make the testable components simpler
    function(idf_component_register)
        cmake_parse_arguments(ARG "" "" "SRCS;INCLUDE_DIRS;REQUIRES" "${ARGN}")
        get_filename_component(COMPONENT_NAME "${CMAKE_CURRENT_LIST_DIR}" NAME)

        add_library("${COMPONENT_NAME}" ${ARG_SRCS})
        target_include_directories("${COMPONENT_NAME}" PUBLIC ${ARG_INCLUDE_DIRS})
        target_link_libraries("${COMPONENT_NAME}" ${ARG_REQUIRES})
    endfunction()
    
    # This would normally be done automatically by the idf project.
    # For the test we have to include everything ourselves.
    add_subdirectory(components/lib)
    add_subdirectory(test)
endif()
