package org.EdgeApp;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.locationtech.jts.geom.Coordinate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class MetaDataHelper {

    public static ArrayNode establishMetadataToUpdate(ArrayNode curMetadata, List<GNode> GnodeList,  String weightsPath, String edgeId) throws IOException{
        ObjectMapper om = new ObjectMapper();
        JsonNode rootNode = om.readTree(new File(weightsPath));
        JsonNode sgcNode = rootNode.path("SGC");

        Map<String, Float> idToWeightMap = new HashMap<>();
        for (JsonNode node : curMetadata) {
            idToWeightMap.put(node.path("id").asText(), node.path("lastWeight").floatValue());
        }

        ArrayNode missingIds = om.createArrayNode();
        ZonedDateTime currentTime = ZonedDateTime.now(ZoneId.of("Europe/Berlin"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        String formattedTime = currentTime.format(formatter);

        sgcNode.fields().forEachRemaining(entry -> {
            String id = entry.getKey();
            Float newWeight = entry.getValue().path("weight").floatValue();
            
            if (!idToWeightMap.containsKey(id) || !idToWeightMap.get(id).equals(newWeight)) {
                System.out.println("We update Gnode " + id);
                ObjectNode newNode = om.createObjectNode();
                Coordinate coor = GnodeList.get(Integer.parseInt(entry.getKey().split("-")[2])).getCoordinate();
                newNode.put("id", id);
                newNode.put("lastWeight", entry.getValue().path("weight").asDouble());
                newNode.put("firmwareVersion", entry.getValue().path("firmwareVersion").asText());
                newNode.put("lastUpdate", formattedTime);
                newNode.put("longitude", coor.getX());
                newNode.put("latitude", coor.getY());
                missingIds.add(newNode);
            }
        });

        return missingIds;
    }
}
