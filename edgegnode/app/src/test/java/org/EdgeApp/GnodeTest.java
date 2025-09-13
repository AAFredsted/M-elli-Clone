package org.EdgeApp;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import java.util.Arrays;

public class GnodeTest {

    private static GeometryFactory gf;

    @BeforeClass
    public static void setup() {
        gf = new GeometryFactory();
    }

    @Test
    public void testGNodeConstructionWithPoint() {
        Point point = gf.createPoint(new Coordinate(1.0, 1.0));
        GNode gNode = new GNode(true, point, new String[]{"A1", "B2"});

        assertTrue(gNode.isEndPoint());
        assertEquals(point, gNode.getPoint());
        assertEquals(2, gNode.getCountStreetIds());
        assertTrue(gNode.getStreetIds().contains("A1"));
    }

    @Test
    public void testGNodeConstructionWithCoordinate() {
        Coordinate coordinate = new Coordinate(2.0, 3.0);
        GNode gNode = new GNode(false, coordinate, new String[]{"C3"});

        assertFalse(gNode.isEndPoint());
        assertEquals(coordinate, gNode.getCoordinate());
        assertEquals(1, gNode.getCountStreetIds());
        assertTrue(gNode.getStreetIds().contains("C3"));
    }

    @Test
    public void testAddIds() {
        GNode gNode = new GNode(true, gf.createPoint(new Coordinate(1.0, 1.0)), new String[]{"A1"});

        gNode.addIds(new String[]{"B2", "C3"});
        assertEquals(3, gNode.getCountStreetIds());
        assertTrue(gNode.getStreetIds().containsAll(Arrays.asList("A1", "B2", "C3")));
    }

    @Test
    public void testAlignIds() {
        GNode gNode1 = new GNode(true, gf.createPoint(new Coordinate(1.0, 1.0)), new String[]{"A1"});
        GNode gNode2 = new GNode(false, gf.createPoint(new Coordinate(2.0, 2.0)), new String[]{"B2", "C3"});

        gNode1.alignIds(gNode2);
        assertEquals(3, gNode1.getCountStreetIds());
        assertTrue(gNode1.getStreetIds().containsAll(Arrays.asList("A1", "B2", "C3")));
    }

    @Test
    public void testAlignEndIds() {
        GNode gNode1 = new GNode(true, gf.createPoint(new Coordinate(1.0, 1.0)), new String[]{"A1"});
        GNode gNode2 = new GNode(true, gf.createPoint(new Coordinate(2.0, 2.0)), new String[]{"B2"});

        gNode1.alignEndIds(gNode2);
        assertEquals(2, gNode1.getCountStreetIds());
        assertTrue(gNode1.getStreetIds().containsAll(Arrays.asList("A1", "B2")));
    }

    @Test
    public void testAddAdjacentNode() {
        GNode gNode1 = new GNode(false, gf.createPoint(new Coordinate(1.0, 1.0)), new String[]{"A1"});
        GNode gNode2 = new GNode(false, gf.createPoint(new Coordinate(2.0, 2.0)), new String[]{"B2"});

        assertTrue(gNode1.addNode(gNode2));
        assertEquals(1, gNode1.getCountAdjacent());
        assertTrue(gNode1.getAdjacentNodes().contains(gNode2));

        // Adding the same node again should return false
        assertFalse(gNode1.addNode(gNode2));
        assertEquals(1, gNode1.getCountAdjacent());
    }

    @Test
    public void testReplaceAdjacentNode() {
        GNode gNode1 = new GNode(false, gf.createPoint(new Coordinate(1.0, 1.0)), new String[]{"A1"});
        GNode oldNode = new GNode(false, gf.createPoint(new Coordinate(2.0, 2.0)), new String[]{"B2"});
        GNode newNode = new GNode(false, gf.createPoint(new Coordinate(3.0, 3.0)), new String[]{"C3"});

        gNode1.addNode(oldNode);
        assertTrue(gNode1.replaceNode(newNode, oldNode));
        assertTrue(gNode1.getAdjacentNodes().contains(newNode));
        assertFalse(gNode1.getAdjacentNodes().contains(oldNode));
    }

    @Test
    public void testHasIdAndHasIds() {
        GNode gNode = new GNode(true, gf.createPoint(new Coordinate(1.0, 1.0)), new String[]{"A1", "B2", "C3"});

        assertTrue(gNode.hasId("A1"));
        assertFalse(gNode.hasId("D4"));

        assertTrue(gNode.hasIds(Arrays.asList("A1", "C3")));
        assertFalse(gNode.hasIds(Arrays.asList("A1", "D4")));
    }

    @Test
    public void testGetAdjacent() {
        GNode gNode1 = new GNode(false, gf.createPoint(new Coordinate(1.0, 1.0)), new String[]{"A1"});
        GNode gNode2 = new GNode(false, gf.createPoint(new Coordinate(2.0, 2.0)), new String[]{"B2"});
        GNode gNode3 = new GNode(false, gf.createPoint(new Coordinate(3.0, 3.0)), new String[]{"C3"});

        gNode1.addNode(gNode2);
        gNode1.addNode(gNode3);

        assertEquals(gNode2, gNode1.getAdjacent("B2", null));
        assertNull(gNode1.getAdjacent("D4", null));
    }

    @Test
    public void testToString() {
        GNode gNode = new GNode(true, gf.createPoint(new Coordinate(1.0, 1.0)), new String[]{"A1", "B2"});

        String expected = "GNode{point=POINT (1 1), streetIds=[A1, B2], adjacentNodes=}";
        assertEquals(expected, gNode.toString());
    }

    @Test
    public void testToStringAdjacency() {
        String idString = "A1";
        GNode start = new GNode(true, gf.createPoint(new Coordinate(1.0, 1.0)), new String[]{idString});
        GNode middle = new GNode(false, gf.createPoint(new Coordinate(2.0, 2.0)), new String[]{idString});
        GNode end = new GNode(false, gf.createPoint(new Coordinate(3.0, 3.0)), new String[]{idString});

        middle.addNode(start);
        middle.addNode(end);
        start.addNode(middle);
        end.addNode(middle);

        String expected = "GNode{point=POINT (1 1), streetIds=[A1], adjacentNodes=POINT (2 2), }";
        assertEquals(expected, start.toString());

    }
    @Test
    public void iterateAdjacency(){
        String idString = "A1";
        GNode start = new GNode(true, gf.createPoint(new Coordinate(1.0, 1.0)), new String[]{idString});
        GNode middle = new GNode(false, gf.createPoint(new Coordinate(2.0, 2.0)), new String[]{idString});
        GNode end = new GNode(false, gf.createPoint(new Coordinate(3.0, 3.0)), new String[]{idString});

        middle.addNode(start);
        middle.addNode(end);
        start.addNode(middle);
        end.addNode(middle);

        assertEquals(middle, start.getAdjacent(idString, null));
        assertEquals(end ,middle.getAdjacent(idString, start));
        assertEquals(start, middle.getAdjacent(idString, end));
    }
}
