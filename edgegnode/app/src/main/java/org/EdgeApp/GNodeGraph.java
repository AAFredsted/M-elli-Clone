package org.EdgeApp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.data.geojson.GeoJSONReader;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.linearref.LinearLocation;
import org.locationtech.jts.linearref.LocationIndexedLine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class GNodeGraph {
    private final List<GNode> GnodeList;


    /*HELL */
    //Layer 1: input and constructor
    public GNodeGraph(String path){
        SimpleFeature[] roads = getFeaturesFromInput(path);
        GnodeList = new ArrayList<>();

        for(int first = 0; first < roads.length -1; first++){
            System.out.println("Current Level: " + ((float) first / roads.length) + "%");
            for(int second = first; second < roads.length; second++){
                Geometry g1 = (Geometry) roads[first].getDefaultGeometry();
                Geometry g2 = (Geometry) roads[second].getDefaultGeometry();
                if(g1 instanceof Polygon || g2 instanceof Polygon){
                    continue;
                }
                if(g1.intersects(g2)){
                    processIntersection(g1, g2, roads[first].getID(), roads[second].getID());
                    
                }
            }
        }

    }

    public GNodeGraph(Boolean placeholderBoolean, String path){
        //construct gnodegraph from json
        GnodeList = new ArrayList<>();

        ObjectMapper om = new ObjectMapper();
        try {
            JsonNode root = om.readTree(new File(path));
            JsonNode gnodes = root.get("gnodes");

            Set<String> streetIds = new HashSet<>();
            
            for(JsonNode gnode: gnodes){
                boolean isEndPoint = gnode.get("IsEndPoint").asBoolean();
                Point point = createPointFromString(gnode.get("Point").asText());
                List<String> streetIdList = new ArrayList<>();
                gnode.get("streetIds").forEach(streetId -> streetIdList.add(streetId.asText()));
                //take care of start and end cases
                if(isEndPoint){
                    if(streetIds.contains(streetIdList.get(0))){
                        createEndGnode(point, false, streetIdList.toArray(new String[streetIdList.size()]));
                    }
                    else{
                        streetIds.add(streetIdList.get(0));
                        createEndGnode(point, true, streetIdList.toArray(new String[streetIdList.size()]));
                    }
                }
                else{
                    createGnode(point, streetIdList.toArray(new String[streetIdList.size()]));
                }
            }
            JsonNode adjacencyLists = root.get("adjacencyList");
            for (int i = 0; i < adjacencyLists.size(); i++) {
                JsonNode adjacencyList = adjacencyLists.get(i);
                for (JsonNode id : adjacencyList.get("adjacentIds")) {
                    GnodeList.get(i).addNode(GnodeList.get(id.asInt()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }    

    private SimpleFeature[] getFeaturesFromInput(String path){
         if (path == null || path.trim().isEmpty()) {
            System.err.println("Path doesnt exist");
            return new SimpleFeature[0];
        }

        File geoJsonFile = new File(path);
        if (!geoJsonFile.exists() || !geoJsonFile.isFile()) {
            System.err.println("File not valid");
            return new SimpleFeature[0];
        }

        try (InputStream geoJsonStream = new FileInputStream(geoJsonFile);
            GeoJSONReader reader = new GeoJSONReader(geoJsonStream)) {

            SimpleFeatureCollection roadFeatures = reader.getFeatures().sort(SortBy.NATURAL_ORDER);
            return roadFeatures.toArray(new SimpleFeature[roadFeatures.size()]);

        } catch (IOException e) {
            System.err.println("Error: Input file is not valid GeoJson");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error occurred while parsing geojson: " + path);
            e.printStackTrace();
        }

        return new SimpleFeature[0];
    }

    //Layer 2: intersection processing and point processing

    private void processIntersection(Geometry f1, Geometry f2, String id1, String id2) {
        if (!id1.equals(id2)) {
            Geometry intersection = f1.intersection(f2);

            if (intersection instanceof Point) {
                processPoint((Point) intersection, f1, f2, id1, id2);
            } else if (intersection instanceof MultiPoint) {
                processMultiPoint((MultiPoint) intersection, f1, f2, id1, id2);
            } else if (intersection instanceof LineString) {
                processLineString((LineString) intersection, f1, f2, id1, id2);
            } else if (intersection instanceof MultiLineString) {
                processMultiLineString((MultiLineString) intersection, f1, f2, id1, id2);
            }
        }
    }

    private Point roundPointCoordinates(Point point, int decimalPlaces) {
        double scale = Math.pow(10, decimalPlaces);
        double x = Math.round(point.getX() * scale) / scale;
        double y = Math.round(point.getY() * scale) / scale;
        Coordinate roundedCoordinate = new Coordinate(x, y);
        GeometryFactory geometryFactory = new GeometryFactory();
        return geometryFactory.createPoint(roundedCoordinate);
    }

    private void processPoint(Point intersect, Geometry g1, Geometry g2, String id1, String id2){
        intersect  = roundPointCoordinates(intersect, 7);  
        GeometryFactory gf = new GeometryFactory();
        Coordinate[] c1 = g1.getCoordinates();
        Coordinate[] c2 = g2.getCoordinates();

        Point f1Start = gf.createPoint(c1[0]);
        Point f1End = gf.createPoint(c1[c1.length - 1]);
        Point f2Start = gf.createPoint(c2[0]);
        Point f2End = gf.createPoint(c2[c2.length - 1]);

        if(stronglyConnected(f1Start, f1End, f2Start, f2End, id1, id2)) return;
        if(weaklyConnected(intersect, f1Start, f1End, g1, f2Start, f2End, g2, id1, id2)) return;
        disconnected(intersect, f1Start, f1End, g1, id1,  f2Start, f2End, g2, id2);
    }
    
    private void processMultiPoint(MultiPoint intersections, Geometry g1, Geometry g2, String id1, String id2){
        GeometryFactory gf = new GeometryFactory();
        Coordinate[] intersectionPoints = intersections.getCoordinates();

        for(Coordinate coord: intersectionPoints) {
            processPoint(gf.createPoint(coord), g1, g2, id1, id2);
        }
    }

    private void processLineString(LineString line, Geometry f1, Geometry f2, String id1, String id2) {
        // Find endpoints
        Point startPoint = line.getStartPoint();
        Point endPoint = line.getEndPoint(); 
        // Process the endpoints
        processPoint(startPoint, f1, f2, id1, id2);
        processPoint(endPoint, f1, f2, id1, id2);
    }

    private void processMultiLineString(MultiLineString multiLine, Geometry f1, Geometry f2, String id1, String id2) {
        // Iterate through each LineString in the MultiLineString
        for (int i = 0; i < multiLine.getNumGeometries(); i++) {
            LineString line = (LineString) multiLine.getGeometryN(i);
            processLineString(line, f1, f2, id1, id2);
        }
    }
    
    

    //Layer 3: StronglyConnected, Weakly Connected and Disconnected Endpoints

    private Boolean stronglyConnected(Point f1Start, Point f1End, Point f2Start, Point f2End, String id1, String id2){
        int f1StartGnodeExists = findEndGnode(f1Start, id1);
        int f1EndGnodeExists = findEndGnode(f1End, id1);

        int f2StartGnodeExists = findEndGnode(f2Start, id2);
        int f2EndGnodeExists = findEndGnode(f2End, id2);

        GNode[] f1;
        GNode[] f2;

        if(f1Start.equalsNorm(f2Start)){
            f1 = createOrGetEndPair(f1StartGnodeExists, f1EndGnodeExists, f1Start, f1End, id1);
            f2 = createOrGetEndPair(f2StartGnodeExists, f2EndGnodeExists, f2Start, f2End, id2);
            connectEndPointGnodes(f1[0], f2[0]);
            return true;
        }

        if(f1Start.equalsNorm(f2End)){
            f1 = createOrGetEndPair(f1StartGnodeExists, f1EndGnodeExists, f1Start, f1End, id1);
            f2 = createOrGetEndPair(f2StartGnodeExists, f2EndGnodeExists, f2Start, f2End, id2);
            connectEndPointGnodes(f1[0], f2[1]);
            return true;
        }

        if(f1End.equalsNorm(f2Start)){
            f1 = createOrGetEndPair(f1StartGnodeExists, f1EndGnodeExists, f1Start, f1End, id1);
            f2 = createOrGetEndPair(f2StartGnodeExists, f2EndGnodeExists, f2Start, f2End, id2);
            connectEndPointGnodes(f1[1], f2[0]);
            return true;
        }

        if(f1End.equalsNorm(f2End)){
            f1 = createOrGetEndPair(f1StartGnodeExists, f1EndGnodeExists, f1Start, f1End, id1);
            f2 = createOrGetEndPair(f2StartGnodeExists, f2EndGnodeExists, f2Start, f2End, id2);
            connectEndPointGnodes(f1[1], f2[1]);
            return true;
        }
        return false;
    }


    private Boolean weaklyConnected(Point intersect, Point f1Start, Point f1End, Geometry g1,  Point f2Start, Point f2End, Geometry g2, String id1, String id2){
        int f1StartExists = findEndGnode(f1Start, id1);
        int f1EndExists = findEndGnode(f1End, id1);
        int f2StartExists = findEndGnode(f2Start, id2);
        int f2EndExists = findEndGnode(f2End, id2);

        if (processWeaklyConnectedIntersection(intersect, f1Start, f1End, f2Start, f2End, g2, id1, id2, f1StartExists, f1EndExists, f2StartExists, f2EndExists)) return true;
        return processWeaklyConnectedIntersection(intersect, f2Start, f2End, f1Start, f1End, g1, id2, id1, f2StartExists, f2EndExists, f1StartExists, f1EndExists);

    }

    private boolean processWeaklyConnectedIntersection(Point intersect, Point f1Start, Point f1End, Point f2Start, Point f2End, Geometry g2, String id1, String id2, int f1StartExists, int f1EndExists, int f2StartExists, int f2EndExists) {
        GNode f1StartGnode;
        GNode f2StartGnode;
        GNode f2EndGnode;
        GNode f1EndGnode;
        if(intersect.equalsNorm(f1Start)){
            GNode[] nonIntersectPair = createOrGetEndPair(f1StartExists, f1EndExists, f1Start, f1End, id1);
            f1StartGnode = nonIntersectPair[0];

            if(f2StartExists == -1 && f2EndExists == -1){
                f2StartGnode = createEndGnode(f2Start, true, new String[]{id2});
                f2EndGnode = createEndGnode(f2End, false, new String[]{id2});
            }
            else{
                f2StartGnode = GnodeList.get(f2StartExists);
                f2EndGnode = GnodeList.get(f2EndExists);
            }

            connectBetweenEndPointGnodes(f1StartGnode, f2StartGnode, f2EndGnode, id2, g2);

            return true;
        }
        if(intersect.equalsNorm(f1End)){
            GNode[] nonIntersectPair = createOrGetEndPair(f1StartExists, f1EndExists, f1Start, f1End, id1);
            f1EndGnode = nonIntersectPair[1];

            if(f2StartExists == -1 && f2EndExists == -1){
                f2StartGnode = createEndGnode(f2Start, true,  new String[]{id2});
                f2EndGnode = createEndGnode(f2End, false, new String[]{id2});
            }
            else{
                f2StartGnode = GnodeList.get(f2StartExists);
                f2EndGnode = GnodeList.get(f2EndExists);
            }

            connectBetweenEndPointGnodes(f1EndGnode, f2StartGnode, f2EndGnode, id2, g2);
            return true;
        }
        return false;
    }

    private void disconnected(Point intersect, Point f1Start, Point f1End, Geometry g1, String id1,  Point f2Start, Point f2End, Geometry g2, String id2) {
        GNode intersectGnode;
        GNode f1StartGnode;
        GNode f1EndGnode;
        GNode f2StartGnode;
        GNode f2EndGnode;

        int intersectExists = findGnode(intersect, new String[]{id1, id2});
        int f1StartExists = findEndGnode(f1Start, id1);
        int f1EndExists = findEndGnode(f1End, id1);
        int f2StartExists = findEndGnode(f2Start, id2);
        int f2EndExists = findEndGnode(f2End, id2);

        if(intersectExists == -1){
            intersectGnode = createGnode(intersect, new String[]{id1, id2});
        }
        else{
            intersectGnode = GnodeList.get(intersectExists);
            intersectGnode.addIds(new String[]{id1, id2});
        }

        if(f1StartExists == -1 && f1EndExists == -1){
            f1StartGnode = createEndGnode(f1Start, true, new String[]{id1});
            f1EndGnode = createEndGnode(f1End, false, new String[]{id1});

        }
        else{
            f1StartGnode = GnodeList.get(f1StartExists);
            f1EndGnode = GnodeList.get(f1EndExists);
        }

        //create in between these
        if(f2StartExists == -1 && f2EndExists == -1){
            f2StartGnode = createEndGnode(f2Start, true, new String[]{id2});
            f2EndGnode = createEndGnode(f2End, false, new String[]{id2});
        }
        else{
            f2StartGnode = GnodeList.get(f2StartExists);
            f2EndGnode = GnodeList.get(f2EndExists);
        }

        connectBetweenEndPointGnodes(intersectGnode, f1StartGnode, f1EndGnode, id1, g1);
        if(intersectGnode.getAdjacentNodes().size() == 0){
            throw new RuntimeException("Intersect GNode has no adjacent nodes");
        }
        connectBetweenEndPointGnodes(intersectGnode, f2StartGnode, f2EndGnode, id2, g2);
        if(intersectGnode.getAdjacentNodes().size() == 0){
            throw new RuntimeException("Intersect GNode has no adjacent nodes");
        }
    }



    //Layer 4: connect endPointsNodes, replaceintersect and addintersect

    private void connectEndPointGnodes(GNode end1, GNode end2){
        //adding them to eachothers adjecencylists
        end1.addNode(end2);
        end2.addNode(end1);
        //alligning streetnames
        end1.alignEndIds(end2);
        end2.alignEndIds(end1);

    }
    private void addIntersectGnode(GNode intersect, GNode left, GNode right) {
        //adding nodes to intersect
        intersect.addNode(left);
        intersect.addNode(right);

        //replacing nodes
        left.addNode(intersect);
        right.addNode(intersect);

        intersect.alignEndIds(right);
    }

    private void replaceIntersectGnode(GNode intersect, GNode left, GNode right, String id){
        intersect.addNode(left);
        intersect.addNode(right);
        intersect.addIds(new String[]{id});
        left.replaceNode(intersect, right);
        right.replaceNode(intersect, left);
    }

    private void connectBetweenEndPointGnodes(GNode intersect, GNode start, GNode end, String id, Geometry coordinates) {

        if (start.getCountAdjacent() == 0 && end.getCountAdjacent() == 0) {
            addIntersectGnode(intersect, start, end);
        } 
        else if(start.getCountAdjacent() == 1 && end.getCountAdjacent() == 1 && start.equals(end.getAdjacentNodes().get(0)) && end.equals(start.getAdjacentNodes().get(0))){
            replaceIntersectGnode(intersect, start, end, id);
        }
        else {
            Stack<GNode> stack = new Stack<>();
            GNode itr = start;
            GNode avoid = new GNode(false, new Coordinate(), new String[]{""});
            GNode temp;
    
            stack.push(itr);
            ArrayList<GNode> visited = new ArrayList<>();
            while (!stack.isEmpty()) {

                itr = stack.peek();
                temp = itr.getAdjacent(id, avoid);
    
                if (temp == null) {
                    // Backtrack to the previous node
                    stack.pop();
                    if (!stack.isEmpty()) {
                        avoid = stack.peek();
                    }
                    continue;
                }
                
                visited.add(temp);

                LineString subLineString = extractSubLineString(coordinates, itr.getPoint(), temp.getPoint());
                
                if (subLineString.intersects(intersect.getPoint()) || subLineString.isWithinDistance(intersect.getPoint(), 0.00005)) {
                    replaceIntersectGnode(intersect, itr, temp, id);
                    start.resetGnode();
                    end.resetGnode();
                    intersect.resetGnode();
                    for(GNode gnode: visited){
                        gnode.resetGnode();
                    }
                    return;
                }
    
                if (!stack.contains(temp)) {
                    stack.push(temp);
                    avoid = itr;
                } else {
                    stack.pop(); // Backtrack to the previous node
                }
            }
            start.resetGnode();
            end.resetGnode();
            intersect.resetGnode();
            for(GNode gnode: visited){
                gnode.resetGnode();
            }
        }
    }
    //Layer 5: find and create layer

    private int findGnode(Point point, String[] ids) {
        GNode temp;
        for(int i = 0; i < GnodeList.size(); i++){
            temp = GnodeList.get(i);
            //find with all of ids a
            if(point.equalsNorm(temp.getPoint()) && temp.hasIds(Arrays.asList(ids))){
                return i;
            }
        }
        return -1;
    }

    private int findEndGnode(Point point, String parentIdString){
        GNode temp;
        for(int i = 0; i < GnodeList.size(); i++){
            temp = GnodeList.get(i);
            //find with all of ids a
            if(point.equalsNorm(temp.getPoint()) && temp.getParentId().equals(parentIdString)){
                return i;
            }
        }
        return -1;
    }

    //creates and returns endgnode and adding it the GnodeList
    private GNode createEndGnode(Point point, Boolean isStart, String[] ids) {
        if(point.equals(null)){
            System.err.println("we create a null"); // TODO: Possibly also include an actual error handler here.
            System.err.println(point);
        }
        GnodeList.add(new GNode(true, point, ids, isStart, !isStart));
        return GnodeList.getLast();
    }

    private GNode createGnode(Point point, String[] ids) {
        GnodeList.add(new GNode(false, point, ids));
        return GnodeList.getLast();
    }

    GNode[] createOrGetEndPair(int startExists, int endExists, Point startPoint, Point endPoint, String id){
        GNode[] endPoints = new GNode[2];

        if(startExists == -1 && endExists == -1){
            endPoints[0] = createEndGnode(startPoint, true, new String[]{id});
            endPoints[1] = createEndGnode(endPoint, false, new String[]{id});
            connectEndPointGnodes(endPoints[0], endPoints[1]);
        }
        else{
            endPoints[0] = GnodeList.get(startExists);
            endPoints[1] = GnodeList.get(endExists);
        }
        return endPoints;

    }
    //Layer 6: the extractSubLineString function


    private LineString extractSubLineString(Geometry lineString, Point point1, Point point2){
        LocationIndexedLine locIndexedLine = new LocationIndexedLine(lineString);
        LinearLocation loc1 = locIndexedLine.project(point1.getCoordinate());
        LinearLocation loc2 = locIndexedLine.project(point2.getCoordinate());

        LinearLocation startLocation = loc1.compareTo(loc2) <= 0 ? loc1 : loc2;
        LinearLocation endLocation = loc1.compareTo(loc2) > 0 ? loc1 : loc2;

        return (LineString) locIndexedLine.extractLine(startLocation, endLocation);
    }

    //Layer 7: only for the true ones

    @Override
    public String toString(){

        String res = "";
        for(GNode node: GnodeList){
            res += node.toString() + "\n";
        }
        return res;
    }
    //Layer 7: actual usefull stuff

    List<GNode> getGnodeList(){ return GnodeList; }
    int getCountGnodes() { return GnodeList.size(); }

    int getCountGnodesOnId(String streetId){
        GNode start = null;
        GNode end = null;
        for(GNode i: GnodeList){
            if(i.isEndPoint() && i.hasId(streetId)){
                if(start == null){
                    start = i;
                }
                else{
                    end = i;
                }
            }
        }
        GNode itr = start;
        GNode avoid = new GNode(false, new Coordinate(), new String[]{""});
        GNode temp = null;
        int i = 0;
        while(itr != null || !itr.equals(end)) {

            temp =  itr.getAdjacent(streetId, avoid);
            avoid = itr;
            itr = temp;
            i++;
        }
        if(i != 0){
            return ++i;
        }

        return i;
    }

    public void saveToJSON(String filePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode root = objectMapper.createObjectNode();
    
        ArrayNode gnodesArray = objectMapper.createArrayNode();
        ArrayNode adjacencyArray = objectMapper.createArrayNode();
    
        for (int i = 0; i < GnodeList.size(); i++) {
            GNode gnode = GnodeList.get(i);
    
            // Create GNode JSON object
            ObjectNode gnodeJson = objectMapper.createObjectNode();
            gnodeJson.put("id", i);
            gnodeJson.put("IsEndPoint", gnode.isEndPoint());
            gnodeJson.put("Point", gnode.getPoint().toString());
    
            // Add street IDs
            ArrayNode streetIdsArray = objectMapper.createArrayNode();
            for (String streetId : gnode.getStreetIds()) {
                streetIdsArray.add(streetId);
            }
            gnodeJson.set("streetIds", streetIdsArray);
    
            gnodesArray.add(gnodeJson);
    
            // Create adjacency JSON object
            ObjectNode adjacencyJson = objectMapper.createObjectNode();
            adjacencyJson.put("id", i);
    
            // Add adjacent GNode IDs
            ArrayNode adjacentIds = objectMapper.createArrayNode();
            for (GNode adjacentGnode : gnode.getAdjacentNodes()) {
                adjacentIds.add(GnodeList.indexOf(adjacentGnode));
            }
            adjacencyJson.set("adjacentIds", adjacentIds);
    
            adjacencyArray.add(adjacencyJson);
        }
    
        root.set("gnodes", gnodesArray);
        root.set("adjacencyList", adjacencyArray);
    
        // Write to file
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), root);
    }

    public static Point createPointFromString(String pointString) {
        GeometryFactory geometryFactory = new GeometryFactory();

        if (pointString.startsWith("POINT (") && pointString.endsWith(")")) {
            String coordinates = pointString.substring(7, pointString.length() - 1);
            String[] parts = coordinates.split(" ");
            if (parts.length == 2) {
                double longitude = Double.parseDouble(parts[0]);
                double latitude = Double.parseDouble(parts[1]);

                Coordinate coordinate = new Coordinate(longitude, latitude);
                return geometryFactory.createPoint(coordinate);
            }
        }
        throw new IllegalArgumentException("Invalid POINT string: " + pointString);
    }
    
}
