
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import utils.Utils;
import utils.OutputStats;
import database.SQLDatabase;
import bandits.BanditOptimizer;


public class ExperimentExecutor {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Must provide an experiment configuration file.");
            return;
        }

        String experimentFile = args[0];

        ArrayList<HashMap<String, String>> configs = parseExperimentFile(experimentFile);

        SQLDatabase trainDb;
        SQLDatabase testDb;
        int numArms;
        int numTypes;
        int trainTrials;
        int testTrials;
        List<List<String>> trainQueries;
        List<List<String>> testQueries;
        List<HashMap<String, List<Double>>> trainQueryRuntimes;
        List<HashMap<String, List<Double>>> testQueryRuntimes;

        int index = 0;
        for (HashMap<String, String> config : configs) {

            System.out.printf("======= Starting experiment %d =======\n", index);

            // Connect to training database
            trainDb = new SQLDatabase("localhost", 26257, config.get("train_db"), "root");
            trainDb.open();
            trainDb.refreshStats(false);

            // Read training queries and profiling
            trainQueries = getQueries(config.get("training_queries"));
            trainQueryRuntimes = getQueryRuntimes(config.get("training_queries"), config.get("training_profile"));

            // Get number of arms and types
            numArms = trainQueries.get(0).size();
            numTypes = trainQueries.size();
            trainTrials = Integer.parseInt(config.get("train_trials"));
 
            // Get optimizers
            List<BanditOptimizer> optimizers = Utils.getOptimizers(config.get("optimizer_config"), numArms, numTypes);

            int[] queryTypes = Utils.generateRandomSequence(numTypes, trainTrials);

            // Run training
            HashMap<String, OutputStats[]> results = new HashMap<String, OutputStats[]>();
            for (BanditOptimizer optimizer : optimizers) {
                OutputStats[] outputStats = trainDb.runJoinQuery(trainQueries, optimizer, trainTrials, trainQueryRuntimes, queryTypes, true, true);
                results.put(optimizer.getName(), outputStats);
            }

            // Make output folder if needed
            File outputFolder = new File(config.get("output_folder"));
            if (!outputFolder.exists()) {
                outputFolder.mkdir();
            }

            // Save training results
            String trainResultsFile = String.format("%s/train_results.json", config.get("output_folder"));
            Utils.saveRegretsAsJson(results, trainResultsFile);

            // Serialize trained bandits
            for (BanditOptimizer optimizer : optimizers) {
                Utils.saveOptimizer(optimizer, config.get("output_folder"));
            }

            // Create test database
            if (!config.get("test_db").equals(config.get("train_db"))) {
                testDb = new SQLDatabase("localhost", 26257, config.get("test_db"), "root");
                testDb.open();
                testDb.refreshStats(false);
            } else {
                testDb = trainDb;
            }

            testTrials = Integer.parseInt(config.get("test_trials"));
            testQueries = getQueries(config.get("testing_queries"));
            testQueryRuntimes = getQueryRuntimes(config.get("testing_queries"), config.get("testing_profile"));

            int numTestTypes = testQueries.size();
            boolean shouldUpdate = Boolean.parseBoolean(config.get("update_during_testing"));

            queryTypes = Utils.generateRandomSequence(numTestTypes, testTrials);

            // Run testing
            results = new HashMap<String, OutputStats[]>();
            for (BanditOptimizer optimizer : optimizers) {
                optimizer.addQueryTypes(numTestTypes - optimizer.getNumTypes());
                OutputStats[] outputStats = testDb.runJoinQuery(testQueries, optimizer, testTrials, testQueryRuntimes, queryTypes, true, shouldUpdate);
                results.put(optimizer.getName(), outputStats);
            }

            // Save testing results
            String testResultsFile = String.format("%s/test_results.json", config.get("output_folder"));
            Utils.saveRegretsAsJson(results, testResultsFile);

            index += 1;
        }

    }

    private static List<List<String>> getQueries(String queryFolder) {
        List<String> filePaths = Utils.getFiles(queryFolder, ".sql");
        List<List<String>> queries = new ArrayList<List<String>>();

        for (String path : filePaths) {
            queries.add(Utils.readQueries(path));
        }

        return queries;
    }

    private static List<HashMap<String, List<Double>>> getQueryRuntimes(String queryFolder, String profileFolder) {
        // We align the results with the query files
        List<String> filePaths = Utils.getFiles(queryFolder, ".sql");
        List<HashMap<String, List<Double>>> queryRuntimes = new ArrayList<HashMap<String, List<Double>>>();
        
        for (String path : filePaths) {
            String fileName = Utils.getFileName(path);
            String profilePath = String.format("%s/%s", profileFolder, fileName.replace(".sql", ".json"));

            queryRuntimes.add(Utils.readProfilingFromJson(profilePath));
        }

        return queryRuntimes;
    }


    private static ArrayList<HashMap<String, String>> parseExperimentFile(String path) {
        /**
         * Parses the experiment configuration file into a list of configuration settings.
         */

        ArrayList<HashMap<String, String>> configs = new ArrayList<HashMap<String, String>>();
        JSONParser parser = new JSONParser();

        try (FileReader reader = new FileReader(path)) {

            JSONArray configArray = (JSONArray) parser.parse(reader);
            
            HashMap<String, String> config;
            for (int i = 0; i < configArray.size(); i++) {
                config = new HashMap<String, String>();

                JSONObject configObj = (JSONObject) configArray.get(i);

                config.put("training_queries", (String) configObj.get("training_queries"));
                config.put("training_profile", (String) configObj.get("training_profile"));
                config.put("testing_queries", (String) configObj.get("testing_queries"));
                config.put("testing_profile", (String) configObj.get("testing_profile"));
                config.put("output_folder", (String) configObj.get("output_folder"));
                config.put("optimizer_config", (String) configObj.get("optimizer_config"));
                config.put("train_db", (String) configObj.get("train_db"));
                config.put("test_db", (String) configObj.get("test_db"));
                config.put("train_trials", (String) configObj.get("train_trials"));
                config.put("test_trials", (String) configObj.get("test_trials"));
                config.put("update_during_testing", (String) configObj.get("update_during_testing"));

                configs.add(config);
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (ParseException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return configs;
    }
}
