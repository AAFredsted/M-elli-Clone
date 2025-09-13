package org.EdgeApp;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class GNode {
    private final Boolean isEndPoint;
    private final Point point;
    private final List<GNode> adjacentGNodes;
    private final List<String> streetIds;
    private Boolean isStart = false;
    private Boolean isEnd = false;
    private Boolean isVisited = false;
    
    public GNode(Boolean isEndPoint, Point point, String[] streetIds) {
        if(isEndPoint){
            System.err.println("enpoint with no start or end indicated");
        }
        this.isEndPoint = isEndPoint;
        this.point = point;
        this.adjacentGNodes = new ArrayList<>();
        this.streetIds = new ArrayList<>(List.of(streetIds));
    }
    public GNode(Boolean isEndPoint, Coordinate coordinate, String[] streetIds) {
        if(isEndPoint){
            System.err.println("enpoint with no start or end indicated");
        }
        GeometryFactory gf = new GeometryFactory();
        this.isEndPoint = isEndPoint;
        this.point = gf.createPoint(coordinate);
        this.adjacentGNodes = new ArrayList<>();
        this.streetIds = new ArrayList<>(List.of(streetIds));
    }
    public GNode(Boolean isEndPoint, Point point, String[] streetIds, Boolean isStart,Boolean isEnd){
        this.isEndPoint = isEndPoint;
        this.point = point;
        this.adjacentGNodes = new ArrayList<>();
        this.streetIds = new ArrayList<>(List.of(streetIds));
        this.isStart = isStart;
        this.isEnd = isEnd;
    }

    // Getters
    public Point getPoint(){
        return point;
    }
    public Coordinate getCoordinate(){
        return point.getCoordinate();
    }
    public List<GNode> getAdjacentNodes() {
        return adjacentGNodes;
    }
    public int getCountAdjacent(){
        return adjacentGNodes.size();
    }
    public List<String> getStreetIds(){
        return streetIds;
    }
    public int getCountStreetIds(){
        return streetIds.size();
    }
    public Boolean isEndPoint() {
        return isEndPoint;
    }
    public String getParentId(){ return isEndPoint ? streetIds.get(0) : ""; }

    //iteration
    public Boolean hasId(String searchStreetId){
        return streetIds.contains(searchStreetId);
    }
    public Boolean hasIds(List<String> searchStreetIds){
        return streetIds.containsAll(searchStreetIds);
    }

    public GNode getAdjacent(String searchStreetId, GNode avoid){

        for (GNode node: adjacentGNodes) {
            if(node.hasId(searchStreetId) && !node.equals(avoid) && !node.isVisited){
                node.isVisited = true;
                return node;
            }
        }
        return null;
    }

    public void resetGnode(){
        isVisited = false;
    }

    //modify node

    public void addIds(String[] ids){
        for(String id: ids){
            if(!streetIds.contains(id)){
                streetIds.add(id);
            }
        }
    }

    public void alignIds(GNode other){
        for (String id : other.getStreetIds()){
            if(!streetIds.contains(id)){
                streetIds.add(id);
            }
        }
    }
    
    public void alignEndIds(GNode other){
        if(!this.streetIds.contains(other.getParentId())){
            this.streetIds.add(other.getParentId());
        }
    }

    public Boolean addNode(GNode node){
        if(!adjacentGNodes.contains(node)){
            adjacentGNodes.add(node);
            return true;
        }
        return false;
    }

    public Boolean replaceNode(GNode newNode, GNode oldNode) {
        for(int i = 0; i < adjacentGNodes.size(); i++){
            if(adjacentGNodes.get(i).equals(oldNode)){
                adjacentGNodes.set(i, newNode);
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        String adjacentNodesString = "";
        for (GNode adjecentGNode : adjacentGNodes) {
            adjacentNodesString += adjecentGNode.getPoint().toString() + ", ";
        }

        return "GNode{" +
                "point=" + point +
                ", streetIds=" + streetIds.toString() +
                ", adjacentNodes=" + adjacentNodesString +
                '}';
    }

    public ObjectNode toJson(){
        ObjectMapper om = new ObjectMapper();
        ObjectNode gnodeJson = om.createObjectNode();

        gnodeJson.put("IsEndPoint", isEndPoint);
        gnodeJson.put("Point", point.toString());
        ArrayNode streetIdsArray = om.createArrayNode();

        for(String streetId: streetIds){
            streetIdsArray.add(streetId);
        }
        gnodeJson.set("streetIds", streetIdsArray);
        
        return gnodeJson;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((isEndPoint == null) ? 0 : isEndPoint.hashCode());
        result = prime * result + ((point == null) ? 0 : point.hashCode());
        result = prime * result + ((streetIds == null) ? 0 : streetIds.hashCode());
        result = prime * result + ((isStart == null) ? 0 : isStart.hashCode());
        result = prime * result + ((isEnd == null) ? 0 : isEnd.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        // Basics
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        // Involving other obj with null values
        GNode other = (GNode) obj;
        if (isEndPoint == null) {
            if (other.isEndPoint != null) return false;
        } else if (!isEndPoint.equals(other.isEndPoint)) return false;

        if (point == null) {
            if (other.point != null) return false;
        } else if (!point.equals(other.point)) return false;

        if (streetIds == null) {
            if (other.streetIds != null) return false;
        } else if (!streetIds.equals(other.streetIds)) return false;

        if (isStart == null) {
            if (other.isStart != null) return false;
        } else if (!isStart.equals(other.isStart)) return false;

        if (isEnd == null) {
            return other.isEnd == null;
        } else return isEnd.equals(other.isEnd); // Simplified if-else due to being at the end of the method
    }
}
