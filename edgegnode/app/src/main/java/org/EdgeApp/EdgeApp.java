package org.EdgeApp;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import org.apache.commons.cli.*;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class EdgeApp {

    public static void main(String[] args) {
        Options options = new Options();

        Option areaNameOption = Option.builder("a")
            .longOpt("area")
            .hasArg()
            .argName("areaName")
            .desc("Name of the area matching the first section of a JsonFile in data folder")
            .required()
            .build();
        options.addOption(areaNameOption);

        Option weightsPathOption = Option.builder("p")
            .longOpt("weightsPath")
            .hasArg()
            .argName("weightsPath")
            .desc("Path to a folder where we find weights.json to base our load on")
            .required()
            .build();
        options.addOption(weightsPathOption);

        Option endpointOption = Option.builder("url")
            .longOpt("endpoint")
            .hasArg()
            .argName("URL")
            .desc("Optional URL for hosted frontend https://gitlab.lrz.de/sgc/frontend")
            .build();
        options.addOption(endpointOption);

        Option edgeIDOption = Option.builder("eID")
            .longOpt("edgeID")
            .hasArg()
            .argName("edgeID")
            .desc("Optional ID for edge app to generate ids for webserver communication")
            .build();
        options.addOption(edgeIDOption);

        Option startPointOption = Option.builder("sID")
            .longOpt("startGnodeID")
            .hasArg()
            .argName("GnodeID")
            .desc("Optional Starting point for pathfinding algorithm (id from [area]_[date]_.json used in application)")
            .build();
        options.addOption(startPointOption);

        Option maxWeightOption = Option.builder("w")
            .longOpt("maxWeight")
            .hasArg()
            .argName("maxWeight")
            .desc("Optional upper bound for a Garbage Trucks (GT) cycle")
            .build();
        options.addOption(maxWeightOption);

        Option countGTOption = Option.builder("cGT")
            .longOpt("countGarbageTruck")
            .hasArg()
            .argName("conutGarbageTruck")
            .desc("Optional upper bound for amount of garbage trucks to find cycles for")
            .build();
        options.addOption(countGTOption);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Input missing, please provide 2 arguments in the format [areaname | example: Garching] [path_to_weights] [optional: endpoint] [optional: startid] [optional: maxWeight] [optional: number of Garbage Trucks]");
            System.err.println("Parsing failed. Reason: " + e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("EdgeApp", options);
            System.exit(1);
        }
        String area = cmd.getOptionValue("a");
        String weightsPath = cmd.getOptionValue("p");
        String endpointUrl = cmd.getOptionValue("url", "https://muelli.orchards.dev");
        String edgeID = cmd.getOptionValue("eID", "GAR-1-");
        Integer startId = Integer.parseInt(cmd.getOptionValue("sID", "1134"));
        Integer maxWeight = Integer.parseInt(cmd.getOptionValue("w", "12"));
        Integer countGarbageTrucks = Integer.parseInt(cmd.getOptionValue("cGT", "5"));
        String appPath = System.getProperty("app.path");

        // Check if geojson file exists
        String geoJsonPath = findGeoJsonPath(appPath, area);
        if (geoJsonPath == null) {
            System.err.println("Vector data for requested area not available");
            return;
        }

        String date = extractDateFromFileName(geoJsonPath);
        String jsonPath = findJsonPath(appPath, area);
        GNodeGraph graph = null;
   
        if (jsonPath == null) {
            System.out.println("First time generating graph...");
            graph = new GNodeGraph(geoJsonPath);
            try {
                jsonPath = appPath + "/data/" + area + "_" + date + "_.json";
                graph.saveToJSON(jsonPath);
            } catch (Exception e) {
                System.err.println("Error occurred while saving graph to JSON");
                e.printStackTrace();
            }
        } else if (!extractDateFromFileName(jsonPath).equals(date)) {
            System.out.println("Regenerating Graph from new map...");

            File oldJsonFile = new File(jsonPath);
            if (oldJsonFile.exists()) {
                boolean deleted = oldJsonFile.delete();
                if (deleted) {
                    System.out.println("Deleted old JSON file: " + jsonPath);
                } else {
                    System.err.println("Failed to delete old JSON file: " + jsonPath);
                }
            }

            graph = new GNodeGraph(geoJsonPath);
            try {
                jsonPath = appPath + "/data/" + area + "_" + date + "_.json";
                graph.saveToJSON(jsonPath);
            } catch (Exception e) {
                System.err.println("Error occurred while saving graph to JSON");
                e.printStackTrace();
            }
        } else {
            System.out.println("Graph is up-to-date, no regeneration needed.");
        }
        
        try {
            if(graph == null){
                graph = new GNodeGraph(true, jsonPath);
            }
        
            List<GNode> GnodeList = graph.getGnodeList();

            //update metadata on server
            ArrayNode trashIconMetadata = HttpPathSender.getTrashCans(endpointUrl);
            ArrayNode TrashCanDataToAdd = MetaDataHelper.establishMetadataToUpdate(trashIconMetadata, GnodeList, weightsPath, edgeID);
            if(TrashCanDataToAdd.size() > 0){
                HttpPathSender.sendTrashMetaData(endpointUrl, TrashCanDataToAdd);
            }


            //updata paths on server
            String outPath = appPath + "/data/path"; // Extracting the value into variable for reuse.
            subCycleFinder.subRouteFinder(GnodeList, weightsPath, outPath, startId, maxWeight, countGarbageTrucks);
            HttpPathSender.sendJson(endpointUrl, outPath + ".json");

        } catch (Exception e) {
            System.err.println("Error occurred in generating json from shortest path");
            e.printStackTrace();
        }
    }


    private static String extractDateFromFileName(String filePath) {
        String fileName = new File(filePath).getName();
        String[] parts = fileName.split("_");
        if (parts.length > 1) {
            return parts[1];
        }
        return "";
    }

    private static String findGeoJsonPath(String appPath, String input) {
        File directory = new File(Paths.get(appPath, "data").toString());
        File[] files = directory.listFiles((dir, name) -> name.startsWith(input) && name.endsWith(".geojson"));

        if (files == null || files.length == 0) {
            return null;
        }
        return files[0].getAbsolutePath();
    }

    private static String findJsonPath(String appPath, String input) {
        File directory = new File(Paths.get(appPath, "data").toString());
        File[] files = directory.listFiles((dir, name) -> name.startsWith(input) && name.endsWith(".json"));

        if (files == null || files.length == 0) {
            return null;
        }
        return files[0].getAbsolutePath();
    }

}
