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
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.la4j.Vector;
import org.la4j.Matrix;

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
            JSONObject rawObject = (JSONObject) parser.parse(reader);

            double rewardEpsilon = (double) rawObject.get("rewardEpsilon");
            double rewardAnneal = (double) rawObject.get("rewardAnneal");
            int updateThreshold = ((Long) rawObject.get("updateThreshold")).intValue();

            // Extract JSON array containing optimizer configurations
            JSONArray configArray = (JSONArray) rawObject.get("optimizers");

            // Load configuration
            for (Object configObj : configArray) {
                JSONObject config = (JSONObject) configObj;

                String name = (String) config.get("name");
                JSONArray args = (JSONArray) config.get("args");
                
                double[] argArray = new double[args.size()];
                for (int i = 0; i < args.size(); i++) {
                    argArray[i] = (double) args.get(i);
                }

                optimizers.add(OptimizerFactory.banditFactory(name, numArms, numTypes, rewardEpsilon, rewardAnneal, updateThreshold, argArray));
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


    public static HashMap<String, List<Double>> readProfilingFromJson(String path) {
        /**
         * Loads profiling results from serialized JSON file.
         *
         * @param path: Path to JSON file with profile results
         * @return A map of query to a list of latency traces
         */        
        JSONParser parser = new JSONParser();
        HashMap<String, List<Double>> resultMap = new HashMap<String, List<Double>>();

        try (FileReader reader = new FileReader(path)) {
 
            // Read file into object
            Object rawObject = parser.parse(reader);

            // Process resulting array
            JSONArray profilingArray = (JSONArray) rawObject;
            for (Object profileObject : profilingArray) {
                JSONObject profileJsonObj = (JSONObject) profileObject;
                String query = (String) profileJsonObj.get("query");

                List<Double> latencies = new ArrayList<Double>();
                JSONArray latencyArray = (JSONArray) profileJsonObj.get("latency");
                for (Object measurement : latencyArray) {
                    latencies.add((double) measurement);
                }

                resultMap.put(query, latencies);
            }

        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (ParseException ex) {
                ex.printStackTrace();
        }

        return resultMap;
    }

    
    public static double getBestAverage(HashMap<String, List<Double>> latencies, List<String> queries) {
        /**
         * Returns the best average latency for each query in the given map.
         */
        double bestAverage = Double.MAX_VALUE;
        for (String query : queries) {
            double avg = average(latencies.get(query));
            if (avg < bestAverage) {
                bestAverage = avg;
            }
        }
        return bestAverage;
    }

    
    public static double getWorstAverage(HashMap<String, List<Double>> latencies, List<String> queries) {
        /**
         * Returns the worse average latency for each query in the given map.
         */
        double worstAverage = -Double.MAX_VALUE;
        for (String query : queries) {
            double avg = average(latencies.get(query));
            if (avg > worstAverage) {
                worstAverage = avg;
            }
        }
        return worstAverage;
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

    public static int sampleDistribution(Vector distribution, Random rand) {
        /**
         * Samples the given probability distribution
         */
        
        double sample = rand.nextDouble();

        double sum = 0.0;
        for (int i = 0; i < distribution.length(); i++) {
            sum += distribution.get(i);

            if (sample < sum) {
                return i;
            }
        }

        return distribution.length();
    }

    public static int argMax(Vector distribution) {
        /**
         * Returns the index corresponding to the largest value.
         */
        double maxElem = -Double.MAX_VALUE;
        int max_index = 0;
        for (int i = 0; i < distribution.length(); i++) {
            if (distribution.get(i) > maxElem) {
                maxElem = distribution.get(i);
                max_index = i;
            }
        }
        return max_index;
    }

    public static Vector normalizeVector(Vector v) {
        /**
         * Normalizes the given vector such that the sum
         * of all entries equals 1.
         */
        double[] normalized = new double[v.length()];
        double sum = v.sum();
        
        for (int i = 0; i < v.length(); i++) {
            normalized[i] = v.get(i) / sum;
        }
    
        return Vector.fromArray(normalized);
    }

    public static Matrix normalizeColumns(Matrix mat) {
        for (int i = 0; i < mat.columns(); i++) {
            Vector col = mat.getColumn(i);
            mat.setColumn(i, normalizeVector(col));
        }
        return mat;
    }

    public static void saveOptimizer(BanditOptimizer opt, String outputFolder) {
        FileOutputStream outStream = null;
        ObjectOutputStream objStream = null;

        try {
            String outputFile = String.format("%s/%s.ser", outputFolder, opt.getName());

            outStream = new FileOutputStream(outputFile);
            objStream = new ObjectOutputStream(outStream);

            objStream.writeObject(opt);

            objStream.close();
            outStream.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static List<BanditOptimizer> loadOptimizers(String folder) {
        List<String> serializedFiles = Utils.getFiles(folder, ".ser");

        List<BanditOptimizer> optimizers = new ArrayList<BanditOptimizer>();

        String fileName;
        FileInputStream inStream;
        ObjectInputStream objStream;
        for (String filePath : serializedFiles) {
            
            fileName = getFileName(filePath);
 
            try {
                inStream = new FileInputStream(filePath);
                objStream = new ObjectInputStream(inStream);

                Object optimizerObj = objStream.readObject();
                BanditOptimizer opt = OptimizerFactory.loadBandit(optimizerObj, fileName);

                if (opt != null) {
                    optimizers.add(opt);
                }

                objStream.close();
                inStream.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        }

        return optimizers;
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
