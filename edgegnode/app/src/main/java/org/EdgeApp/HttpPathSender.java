package org.EdgeApp;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Simple class that is responsible for HTTP communication.
 */
public class HttpPathSender {

    /**
     * Sends the generated JSON to the remote server.
     * @param path Path to path.json file from before.
     * @throws FileNotFoundException if json isn't found at the path.
     */
    public static void sendJson(String endpoint, String path) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
            .version(Version.HTTP_1_1)
            .build();
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(endpoint + "/data/addCycle"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofFile(Paths.get(path)))
            .build();

        HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());

        if(response.statusCode() != 200){
            System.out.println("Sending request to: " + req.uri());
            System.out.println("Request headers: " + req.headers().map());
            System.out.println("Response status code: " + response.statusCode());
            response.headers().map().forEach((k, v) -> System.out.println(k + ": " + v));
            System.out.println("Response body: " + response.body());
        }
    }

    public static ArrayNode getTrashCans(String endpoint) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
            .version(Version.HTTP_1_1)
            .build();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(endpoint + "/data/metadata.json"))
            .header("Accept", "application/json")
            .GET()
            .build();
        /*

        */
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if(response.statusCode() != 200){
            System.out.println("Sending request to: " + request.uri());
            System.out.println("Request headers: " + request.headers().map());
            System.out.println("Response status code: " + response.statusCode());
            response.headers().map().forEach((k, v) -> System.out.println(k + ": " + v));
            System.out.println("Response body: " + response.body());
        }

        String responseBody = response.body();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);

        if (jsonNode.isArray()) {
            return (ArrayNode) jsonNode;
        } else {
            throw new IOException("Expected an array but got: " + jsonNode.getNodeType());
        }
    }

    public static void sendTrashMetaData(String endpoint, ArrayNode metaData) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
            .version(Version.HTTP_1_1)
            .build();
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(metaData);

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(endpoint + "/data/metaMultiple"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

    
        
        HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());

        if(response.statusCode() != 200){
            System.out.println("Sending request to: " + req.uri());
            System.out.println("Request headers: " + req.headers().map());
            System.out.println("Request body: " + json);
            System.out.println("Response status code: " + response.statusCode());
            response.headers().map().forEach((k, v) -> System.out.println(k + ": " + v));
            System.out.println("Response body: " + response.body());
        }
        
    }
}