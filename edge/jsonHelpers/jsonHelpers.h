#pragma once
#include "string"
#include "cjson/cJSON.h"

namespace jsonHelpers {
    //super basic read function as an example case
    std::string readJson(std::string path);
    bool updateWeights(std::string path, std::string id, double weight, int firmwareVersion, int time);
    bool clearWeights(const std::string& path);
}
