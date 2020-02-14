package utils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import bandits.OptimizerFactory;
import bandits.BanditOptimizer;

public class Utils {

    public static void printSQLException(SQLException ex) {
        System.out.printf("SQL Execution ERROR: { state => %s, cause => %s, message => %s }\n",
                          ex.getSQLState(), ex.getCause(), ex.getMessage());
    }

    public static String strip(String token) {
        token = token.trim();
        if (token.endsWith(";")) {
            return token.substring(0, token.length() - 1);
        }
        return token;
    }

    public static String getFileName(String path) {
        /**
         * Returns the file name for the given path.
         */
        return Paths.get(path).getFileName().toString();
    }

    public static List<String> readQueries(String path) {
        List<String> queries = new ArrayList<String>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            StringBuilder q = new StringBuilder();

            String line = reader.readLine();
            while (line != null) {
                q.append(line + " ");
                if (line.endsWith(";")) {
                    queries.add(q.toString());
                    q = new StringBuilder();
                }
                line = reader.readLine();
            }
        } catch (IOException ex) {
            System.out.printf("Caught IO Exception %s\n", ex.getMessage());
        }
        return queries;
    }

    public static void writeResults(String path, double[] times) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(path));
            for (int i = 0; i < times.length; i++) {
                writer.write(String.format("%d,%.4f\n", i, times[i]));
            }
            writer.flush();
        } catch (IOException ex) {
            System.out.printf("Caught IO Exception %s\n", ex.getMessage());
        }
    }

    public static List<String> getFiles(String path, String extension) {
        File file = new File(path);
        List<String> result = new ArrayList<String>();

        if (!file.exists()) {
            return result;
        }

        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                if (f.getName().endsWith(extension)) {
                    result.add(f.getPath());
                }
            }
        } else {
            result.add(path);
        }

        return result;
    }

    public static List<BanditOptimizer> getOptimizers(String configPath, int numArms, int numTypes) {
        /**
         * Loads the bandit optimizers as specified by the configuration path.
         *
         * @param configPath: Path to configuration JSON file
         * @return A list of bandit optimizers
         */
        JSONParser parser = new JSONParser();
        List<BanditOptimizer> optimizers = new ArrayList<BanditOptimizer>();

        try (FileReader reader = new FileReader(configPath)) {
            
            // Read file
            Object rawObject = parser.parse(reader);

            // Extract JSON array
            JSONArray configArray = (JSONArray) rawObject;

            // Load configuration
            for (Object configObj : configArray) {
                JSONObject config = (JSONObject) configObj;

                String name = (String) config.get("name");
                JSONArray args = (JSONArray) config.get("args");
                
                double[] argArray = new double[args.size()];
                for (int i = 0; i < args.size(); i++) {
                    argArray[i] = (double) args.get(i);
                }

                optimizers.add(OptimizerFactory.banditFactory(name, numArms, numTypes, argArray));
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
    
        return optimizers;
    }


    public static double readProfilingFromJson(String path) {
        /**
         * Loads profiling results from serialized JSON file.
         *
         * @param path: Path to JSON file with profile results
         * @return The best average query latency for all queries in the file
         */        
        JSONParser parser = new JSONParser();
        double bestAverage = Double.MAX_VALUE;

        try (FileReader reader = new FileReader(path)) {
 
            // Read file into object
            Object rawObject = parser.parse(reader);

            // Process resulting array
            JSONArray profilingArray = (JSONArray) rawObject;
            for (Object profileObject : profilingArray) {
                JSONObject profileJsonObj = (JSONObject) profileObject;
                
                double latencyAverage = average((JSONArray) profileJsonObj.get("latency"));
                if (latencyAverage < bestAverage) {
                    bestAverage = latencyAverage;
                }
            }

        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (ParseException ex) {
                ex.printStackTrace();
        }

        return bestAverage;
    }

    public static void saveRegretsAsJson(Map<String, OutputStats[]> regretMap, String outputFile) {
        /**
         * Save the map of regret values as a JSON
         *
         * @param regretMap: Map from optimizer name to output statistics
         * @param outputFile: Path to output file
         */
        JSONArray result = new JSONArray();

        for (String optName : regretMap.keySet()) {
            JSONObject optResult = new JSONObject();

            // Copy regrets into JSON Array
            JSONArray stats = new JSONArray();
            OutputStats[] statsArray = regretMap.get(optName);
            for (int i = 0; i < statsArray.length; i++) {
                stats.add(statsArray[i].toJsonObject());
            }

            optResult.put("optimizer_name", optName);
            optResult.put("stats", stats);

            result.add(optResult);
        }

        writeAsJson(result, outputFile);
    }


    public static void saveResultsAsJson(Map<String, List<Double>> resultsMap, String outputFile) {
        /**
         * Save the given map as a JSON object.
         * 
         * @param resultsMap: Map containing query latency results
         * @param outputFile: File path at which to save results
         */
        // Convert to JSON object
        JSONArray result = new JSONArray();
        for (String key : resultsMap.keySet()) {
            JSONObject queryObject = new JSONObject();
            queryObject.put("query", key);
            queryObject.put("latency", listToJsonArray(resultsMap.get(key)));

            result.add(queryObject);
        }

        writeAsJson(result, outputFile);
    }

    public static double average(Iterable<Double> iter) {
        /**
         * Compute the arithmetic mean of the given iterator.
         */
        double sum = 0.0;
        double count = 0.0;

        for (Double i : iter) {
            sum += i;
            count += 1.0;
        }

        return sum / count;
    }

    private static void writeAsJson(JSONArray array, String path) {
        /**
         * Write the given JSON Array to the output path
         */
        try (FileWriter file = new FileWriter(path)) {
            file.write(array.toJSONString());
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static <T> JSONArray listToJsonArray(List<T> lst) {
        /**
         * Converts the given list to a JsonArray
         */
        JSONArray result = new JSONArray();
        for (T element : lst) {
            result.add(element);
        }
        return result;
    }
}
