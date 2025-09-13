#include "jsonHelpers.h"

#include "vector"
#include <fcntl.h>
#include <unistd.h>
#include <sys/stat.h>
#include <cstdint>
#include "iostream"
#include "vector"

namespace jsonHelpers {
    std::string readJson(std::string path) {
        int fd = open(path.c_str() ,O_RDWR | O_CREAT, 0640);

        if (fd == -1) {
            printf("Error: unable to open or create file at %s\n", path.c_str());
            return "";
        }

        FILE *fp = fdopen(fd, "r"); 
        if (fp == nullptr) {
            printf("Error: unable to open file at %s", path.c_str());
            return "";
        }

        std::vector<char> buffer(102400);
        const int len = fread(buffer.data(), 1, buffer.size(), fp);
        
        fclose(fp);

        cJSON *json = cJSON_Parse(buffer.data());

        if (json == nullptr) {
            const char *error_ptr = cJSON_GetErrorPtr();
            if (error_ptr != nullptr) printf("Error: %s\n", error_ptr);
            cJSON_Delete(json);
            return "{}";
        }

        char *json_str = cJSON_PrintUnformatted(json);
        std::string data(json_str);
        cJSON_free(json_str);
        cJSON_Delete(json);

        return data;
    }

    bool updateWeights(std::string path, std::string id, double weight, int firmwareVersion, int time){

        int fd = open(path.c_str() ,O_RDWR | O_CREAT, 0640);

        if (fd == -1) {
            printf("Error: unable to open or create file at %s\n", path.c_str());
            return false;
        }
        FILE *fp = fdopen(fd, "r");

        //Case 1:file not found
        if (fp == nullptr) {
            printf("Error: unable to open file stream at %s\n", path.c_str());
            close(fd);
            return false;
        }

        std::vector<uint8_t> buffer(102400);
        const int len = fread(buffer.data(), 1, buffer.size(), fp);
        fclose(fp);

        cJSON *json = nullptr;

        //Case 2: File is Empty
        if(len == 0){
            json = cJSON_CreateObject();
        }
        else{
            json = cJSON_Parse((const char*) buffer.data());
            //case 3: Json is invalid
            if (json == nullptr) {
                const char *error_ptr = cJSON_GetErrorPtr();
                if (error_ptr != nullptr) printf("Error parsing JSON: %s\n", error_ptr);
                return false;
            }
        }

        cJSON *SGCList = cJSON_GetObjectItemCaseSensitive(json, "SGC");
        if (!cJSON_IsObject(SGCList)) {
            // Add "SGC" object if missing
            SGCList = cJSON_CreateObject();
            cJSON_AddItemToObject(json, "SGC", SGCList);
        }

        cJSON *SGCObject = cJSON_GetObjectItemCaseSensitive(SGCList, id.c_str());
        if (!cJSON_IsObject(SGCObject)) {
            // Add the object if missing
            SGCObject = cJSON_CreateObject();
            cJSON_AddItemToObject(SGCList, id.c_str(), SGCObject);
        }

        if (cJSON *weightPtr = cJSON_GetObjectItemCaseSensitive(SGCObject, "weight"))
            cJSON_SetNumberValue(weightPtr, weight);
        else cJSON_AddNumberToObject(SGCObject, "weight", weight);

        if (cJSON *firmwareVersionPtr = cJSON_GetObjectItemCaseSensitive(SGCObject, "firmwareVersion"))
            cJSON_SetNumberValue(firmwareVersionPtr, firmwareVersion);
        else cJSON_AddNumberToObject(SGCObject, "firmwareVersion", firmwareVersion);

        if (cJSON *timePtr = cJSON_GetObjectItemCaseSensitive(SGCObject, "time"))
            cJSON_SetNumberValue(timePtr, time);
        else cJSON_AddNumberToObject(SGCObject, "time", time);

        char *updatedJson = cJSON_PrintUnformatted(json);
        if (updatedJson == nullptr) {
            printf("Error: JSON misconfigured\n");
            cJSON_Delete(json);
            return false;
        }
        
        fp = fopen(path.c_str(), "w");
        if (fp == nullptr) {
            printf("Error: unable to open file for writing at %s\n", path.c_str());
            cJSON_free(updatedJson);
            cJSON_Delete(json);
            fclose(fp);
            return false;
        }

        fwrite(updatedJson, sizeof(char), std::string(updatedJson).length(), fp);
        fclose(fp);

        // Clean up
        cJSON_free(updatedJson);
        cJSON_Delete(json);

        return true;
    }

    bool addId(std::string &path, std::string id) {
        FILE *fp = fopen(path.c_str(), "r");

        // Case 1: file not found
        if (fp == nullptr) {
            printf("Error: unable to open file at %s\n", path.c_str());
            return false;
        }

        char buffer[1024] = {0};
        const int len = fread(buffer, 1, sizeof(buffer), fp);
        fclose(fp);

        cJSON *json = nullptr;

        if (len == 0) {
            json = cJSON_CreateObject();
        } else {
            json = cJSON_Parse(buffer);
            // Case 3: JSON is invalid
            if (json == nullptr) {
                const char *error_ptr = cJSON_GetErrorPtr();
                if (error_ptr != nullptr) printf("Error parsing JSON: %s\n", error_ptr);
                return false;
            }
        }

        cJSON *GnodeList = cJSON_GetObjectItemCaseSensitive(json, "Gnodes");
        if (!cJSON_IsArray(GnodeList)) {
            // Add "Gnodes" array if missing
            GnodeList = cJSON_CreateArray();
            cJSON_AddItemToObject(json, "Gnodes", GnodeList);
        }

        // Check if ID already exists in the list
        cJSON *existingId = nullptr;
        cJSON_ArrayForEach(existingId, GnodeList) {
            if (cJSON_IsString(existingId) && (existingId->valuestring == id)) {
                printf("Error: ID already exists\n");
                cJSON_Delete(json);
                return false;
            }
        }

        // Add the new ID to the list
        cJSON_AddItemToArray(GnodeList, cJSON_CreateString(id.c_str()));

        char *updatedJson = cJSON_PrintUnformatted(json);
        if (updatedJson == nullptr) {
            printf("Error: JSON misconfigured\n");
            cJSON_Delete(json);
            return false;
        }
        fp = fopen(path.c_str(), "w");
        if (fp == nullptr) {
            printf("Error: unable to open file for writing at %s\n", path.c_str());
            cJSON_free(updatedJson);
            cJSON_Delete(json);
            return false;
        }

        fwrite(updatedJson, sizeof(char), std::string(updatedJson).length(), fp);
        fclose(fp);

        // Clean up
        cJSON_free(updatedJson);
        cJSON_Delete(json);

        return true;
    }

    bool clearWeights(const std::string &path) {
        // Open the file in write mode with truncate behavior

        int fd = open(path.c_str() ,O_RDWR | O_CREAT | O_TRUNC, 0640);

        if (fd == -1) {
            printf("Error: unable to open or create file at %s\n", path.c_str());
            return "";
        }

        FILE *fp = fdopen(fd, "w");

        if (fp == nullptr) {
            printf("Error: unable to open file for clearing at %s\n", path.c_str());
            return false;
        }

        std::string json = "{}";
        fwrite(json.c_str(), sizeof(char), json.size(), fp);
        

        // Successfully opened the file, now clear its contents by closing it
        fclose(fp);
        return true;
    }
} //namespace
