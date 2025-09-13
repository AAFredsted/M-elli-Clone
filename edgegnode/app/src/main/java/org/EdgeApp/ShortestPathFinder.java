package org.EdgeApp;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.locationtech.jts.geom.Point;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ShortestPathFinder {

    private static class GNodeDistance {
        GNode gnode;
        double distance;

        GNodeDistance(GNode gnode, double distance) {
            this.gnode = gnode;
            this.distance = distance;
        }
    }

    private static double calculateDistance(GNode node1, GNode node2) {
        Point p1 = node1.getPoint();
        Point p2 = node2.getPoint();
        return p1.distance(p2);
    }

    private static List<GNode> reconstructPath(GNode startNode, GNode targetNode, Map<GNode, GNode> predecessors) {
        List<GNode> path = new ArrayList<>();
        for (GNode at = targetNode; at != null; at = predecessors.get(at)) {
            path.add(at);
        }
        Collections.reverse(path);
        return path;
    }

    public static void CycleFinderGnode(List<GNode> GnodeList, String weightsPath, String filePath) throws IOException {
        // Reading in weights
        ObjectMapper om = new ObjectMapper();
        JsonNode rootNode = om.readTree(new File(weightsPath));
        JsonNode sgcNode = rootNode.path("SGC");

        HashMap<Integer, Integer> idToWeightMap = new HashMap<>();
        sgcNode.fields().forEachRemaining(entry -> {
            int id = Integer.parseInt(entry.getKey());
            int weight = entry.getValue().path("weight").asInt();
            if (weight >= 1) {
                idToWeightMap.put(id, weight);
            }
        });

        // Start finding the shortest cycle
        int startId = 19; // Assuming starting from ID 19
        GNode startNode = GnodeList.get(startId);
        List<GNode> cycle = new ArrayList<>();

        while (!idToWeightMap.isEmpty()) {
            // Find the path to any target node in idToWeightMap
            List<GNode> pathToTarget = DijkstraGnodeSol(GnodeList, startNode, idToWeightMap);
            GNode reachedTarget = pathToTarget.getLast();
            cycle.addAll(pathToTarget);

            // Update starting node and remove target from map
            startNode = reachedTarget;
            idToWeightMap.remove(GnodeList.indexOf(reachedTarget));
        }

        // Find the path back to the starting node
        List<GNode> pathToStart = DijkstraGnodeSol(GnodeList, startNode, Collections.singletonMap(startId, 0));
        cycle.addAll(pathToStart);

        // Convert the cycle to JSON and write to file
        toJson(cycle, filePath);
    }

    private static List<GNode> DijkstraGnodeSol(List<GNode> GnodeList, GNode startNode, Map<Integer, Integer> targetIds) {
        Map<GNode, Double> distances = new HashMap<>();
        Map<GNode, GNode> predecessors = new HashMap<>();
        PriorityQueue<GNodeDistance> gnodeDistancePQ = new PriorityQueue<>(Comparator.comparingDouble(gnodedist -> gnodedist.distance));

        for (GNode gnode : GnodeList) {
            distances.put(gnode, Double.POSITIVE_INFINITY);
            predecessors.put(gnode, null);
        }

        distances.replace(startNode, 0.0);
        gnodeDistancePQ.add(new GNodeDistance(startNode, 0.0));

        while (!gnodeDistancePQ.isEmpty()) {
            GNodeDistance cur = gnodeDistancePQ.poll();
            if (targetIds.containsKey(GnodeList.indexOf(cur.gnode))) {
                return reconstructPath(startNode, cur.gnode, predecessors);
            }
            for (GNode neighbor : cur.gnode.getAdjacentNodes()) {
                double distance = calculateDistance(cur.gnode, neighbor);
                double newDist = distances.get(cur.gnode) + distance;

                if (newDist < distances.get(neighbor)) {
                    distances.put(neighbor, newDist);
                    predecessors.put(neighbor, cur.gnode);
                    gnodeDistancePQ.add(new GNodeDistance(neighbor, newDist));
                }
            }
        }

        throw new IllegalStateException("No path found to any target node");
    }

    public static void toJson(List<GNode> GnodeList, String filePath) throws IOException {
        ObjectMapper om = new ObjectMapper();
        ObjectNode root = om.createObjectNode();
        ArrayNode gnodesArray = om.createArrayNode();

        for (GNode gnode : GnodeList) {
            gnodesArray.add(gnode.toJson());
        }

        root.set("gnodes", gnodesArray);
        om.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), root);
    }
}