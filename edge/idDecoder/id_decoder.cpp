#include "id_decoder.h"

id_decoder::id_decoder(uint32_t id) {
    this->id.value = id;

    this->city_code = new char[3];
    this->city_code[0] = this->id.city_code_1 + 'A';
    this->city_code[1] = this->id.city_code_2 + 'A';
    this->city_code[2] = this->id.city_code_3 + 'A';

    this->relative_edge = this->id.relative_edge;
    this->relative_gnode = this->id.relative_gnode;
}
