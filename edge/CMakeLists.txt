cmake_minimum_required(VERSION 3.25)
project(coapTestServer)

set(CMAKE_CXX_STANDARD 20)

find_package(libcoap)

if (libcoap_FOUND)
	set(COAP_LIBRARIES libcoap::coap-3)
else()
	find_package(PkgConfig REQUIRED)
	set(PKG_CONFIG_USE_CMAKE_PREFIX_PATH ON)
	pkg_check_modules(COAP REQUIRED libcoap-3)
endif()

find_package(cJSON REQUIRED)

include_directories(${COAP_INCLUDE_DIRS})
link_directories(${COAP_LIBRARY_DIRS})

include_directories(${cJSON_INCLUDE_DIRS})
link_directories(${cJSON_LIBRARY_DIRS})

add_executable(coapServer
    main.cpp
    CoAPServer/CoAPServer.cpp CoAPServer/CoAPServer.h
    jsonHelpers/jsonHelpers.cpp jsonHelpers/jsonHelpers.h
    idDecoder/id_decoder.cpp
    idDecoder/id_decoder.h
    debounce/debounce.h
)

target_link_libraries(coapServer ${COAP_LIBRARIES} ${CJSON_LIBRARIES})
