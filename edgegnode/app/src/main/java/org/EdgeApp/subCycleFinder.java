package org.EdgeApp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.locationtech.jts.geom.Point;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;



public class subCycleFinder {

    private static class GNodeDistance {
        GNode gnode;
        double distance;

        GNodeDistance(GNode gnode, double distance) {
            this.gnode = gnode;
            this.distance = distance;
        }
    }

    private static double calculateDistance(GNode gnode1, GNode gnode2) {
        Point p1 = gnode1.getPoint();
        Point p2 = gnode2.getPoint();
        return p1.distance(p2);
    }


    public static void subRouteFinder(List<GNode> GnodeList, String weightsPath, String filePath, int startId, float maxWeight, int numGT ) throws IOException {
        ObjectMapper om = new ObjectMapper();
        JsonNode rootNode = om.readTree(new File(weightsPath));
        JsonNode sgcNode = rootNode.path("SGC");

        HashMap<Integer, Float> idToWeightMap = new HashMap<>();
        sgcNode.fields().forEachRemaining(entry -> {
            int id = Integer.parseInt(entry.getKey().split("-")[2]);
            float weight = entry.getValue().path("weight").floatValue();
            if (weight >= 1) {
                idToWeightMap.put(id, weight);
            }
        });

        List<List<Integer>> partition = new ArrayList<>();

        int i = 0;
        while(i < numGT && !idToWeightMap.isEmpty()){
            int maxId = Collections.max(idToWeightMap.entrySet(), Map.Entry.comparingByValue()).getKey();

            List<Integer> Hood = findHood(GnodeList, maxId, idToWeightMap, maxWeight);

            partition.add(Hood);
            i++;
        }
        
        List<List<GNode>> cycles = new ArrayList<>();
        for(int o = 0; o < partition.size(); o++){
            List<Integer> hood = partition.get(o);

            List<GNode> subCycle = new ArrayList<>();

            int id = startId;
            while (hood.size() > 0) {

                List<GNode> pathToTarget = dijkstraNeighborHoodCycleFinder(id , hood, GnodeList);
                GNode reachedTarget = pathToTarget.getLast();
                int reachedTargetIndex = GnodeList.indexOf(reachedTarget);

                subCycle.addAll(pathToTarget);
    
                // Update starting node and remove target from map
                id = reachedTargetIndex;
                hood.remove(hood.indexOf(reachedTargetIndex));

            }
            List<Integer> startIdSet = new ArrayList<>(Collections.singletonList(startId)); 
            List<GNode> pathToStart = dijkstraNeighborHoodCycleFinder(id, startIdSet, GnodeList);
            subCycle.addAll(pathToStart);
            cycles.add(subCycle);
            i++;
        }

        toBundledJson(cycles, filePath);

    }


    private static List<Integer> findHood(List<GNode> GnodeList , int hoodCenter, HashMap<Integer, Float> idToWeightMap, float maxWeight){

        float curWeight = idToWeightMap.get(hoodCenter);
        List<Integer> hood = new ArrayList<>();

        hood.add(hoodCenter);
        idToWeightMap.remove(hoodCenter);

        if(idToWeightMap.size() == 0){
            return hood;
        }

        while(curWeight < maxWeight && idToWeightMap.size() > 0){

            int neighbor = dijkstraNeighbourfind(GnodeList, hoodCenter, idToWeightMap.keySet());
            float neighborWeight = idToWeightMap.get(neighbor);

            if(curWeight + neighborWeight > maxWeight){
                break;
            }
            
            hood.add(neighbor);
            idToWeightMap.remove(neighbor);
            curWeight+= neighborWeight;
        }

        return hood;

    }


    private static int dijkstraNeighbourfind(List<GNode> GnodeList, int hoodCenter,Set<Integer> targetIds ){

        Map<GNode, Double> distances = new HashMap<>();
        Map<GNode, GNode> predecessors = new HashMap<>();

        PriorityQueue<GNodeDistance> gnodeDistancePQ = new PriorityQueue<>(Comparator.comparingDouble(gnodedist -> gnodedist.distance));

        for (GNode gnode : GnodeList) {
            distances.put(gnode, Double.POSITIVE_INFINITY);
            predecessors.put(gnode, null);

        }

        GNode hoodCenterGnode = GnodeList.get(hoodCenter);
        distances.replace(hoodCenterGnode, 0.0);
        gnodeDistancePQ.add(new GNodeDistance(hoodCenterGnode, 0.0));


        while (!gnodeDistancePQ.isEmpty()) {
            GNodeDistance cur = gnodeDistancePQ.poll();
            
            int curIndex = GnodeList.indexOf(cur.gnode);

            if (targetIds.contains(curIndex)) {
                return curIndex;
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
        System.out.println("hoodcenter is: " + hoodCenter);
        System.out.println("targetIds is:" + targetIds.toString());

        throw new IllegalStateException("No neighbour found: Possible mismatch between collectionarea and trashcanid's");
    }

    private static List<GNode> dijkstraNeighborHoodCycleFinder(int startId, List<Integer> hood, List<GNode> GnodeList){

        Map<GNode, Double> distances = new HashMap<>();
        Map<GNode, GNode> predecessors = new HashMap<>();
        PriorityQueue<GNodeDistance> gnodeDistancePQ = new PriorityQueue<>(Comparator.comparingDouble(gnodedist -> gnodedist.distance));

        for(GNode gnode: GnodeList) {
            distances.put(gnode, Double.POSITIVE_INFINITY);
            predecessors.put(gnode, null);
        }

        GNode startGNode = GnodeList.get(startId);

        gnodeDistancePQ.add(new GNodeDistance(startGNode, 0.0));

        distances.put(startGNode, 0.0);

        while(!gnodeDistancePQ.isEmpty()){
            GNodeDistance cur = gnodeDistancePQ.poll();
            if(hood.contains(GnodeList.indexOf(cur.gnode))){
                return reconstructPath(startGNode, cur.gnode, predecessors);
            }

            for(GNode neighborGnode: cur.gnode.getAdjacentNodes()){
                double distance = calculateDistance(cur.gnode, neighborGnode);
                double newDist = distances.get(cur.gnode) + distance;
                if(newDist < distances.get(neighborGnode)){
                    distances.put(neighborGnode, newDist);
                    predecessors.put(neighborGnode, cur.gnode);
                    gnodeDistancePQ.add(new GNodeDistance(neighborGnode, newDist));


                }
            }

        }
        return new ArrayList<GNode>();
    }

    private static List<GNode> reconstructPath(GNode startNode, GNode targetNode, Map<GNode, GNode> predecessors) {
        List<GNode> path = new ArrayList<>();
        for (GNode at = targetNode; at != null; at = predecessors.get(at)) {
            path.add(at);
        }
        Collections.reverse(path);
        return path;
    }

    private static void toBundledJson(List<List<GNode>> GNodeListList, String filePath) throws IOException {
        ObjectMapper om = new ObjectMapper();
        ObjectNode root = om.createObjectNode();
        ArrayNode subcycleArrayNode = om.createArrayNode();

        ArrayNode subCycle;

        for(int i = 0; i < GNodeListList.size(); i++){
            List<GNode> GNodeSubCycle = GNodeListList.get(i);
            subCycle = om.createArrayNode();
            for(GNode gnode: GNodeSubCycle){
                subCycle.add(gnode.toJson());
            }
            subcycleArrayNode.add(subCycle);
        }
        root.set("subcycles", subcycleArrayNode);
        om.writerWithDefaultPrettyPrinter().writeValue(new File(filePath + ".json" ), root);
        System.out.println("Subcycles generated");
    }

}
