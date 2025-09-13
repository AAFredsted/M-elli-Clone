#pragma once

#ifndef ID_DECODER_H
#define ID_DECODER_H
#include <cstdint>
#include <string>

typedef union {
    struct {
        uint32_t city_code_1: 5;
        uint32_t city_code_2: 5;
        uint32_t city_code_3: 5;
        uint32_t relative_edge: 6;
        uint32_t relative_gnode: 11;
    };
    uint32_t value;
} data_id_t;

class id_decoder {
    data_id_t id;
    char* city_code;
    int relative_edge;
    int relative_gnode;

public:
    /**
     * Creates a new object that decodes a 32-bit encoded id.
     * @param id 32-bit encoded id. Must be uint32. Anything else and I'll cry.
     */
    explicit id_decoder(uint32_t id);
    [[nodiscard]] std::string get_id() const { return std::string(city_code) + "-" + std::to_string(relative_edge) + "-" + std::to_string(relative_gnode); }
};

#endif //ID_DECODER_H
