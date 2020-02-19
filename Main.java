import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Random;
import java.util.HashMap;

import utils.Utils;
import utils.OutputStats;
import database.SQLDatabase;
import bandits.OptimizerFactory;
import bandits.BanditOptimizer;
import parsing.SQLParser;


public class Main {

    public static void main(String[] args) {
     
        System.out.println("Starting Join Optimizer Interface to Cockroach DB...");
        Scanner inputReader = new Scanner(System.in);

        SQLDatabase db = null;
        boolean run = true;
        while (run) {
            System.out.print("> ");
            String line = inputReader.nextLine().trim();
            
            String[] tokens = line.split(" ");
            if (tokens.length == 0) {
                continue;  // Skip empty lines
            }

            String cmd = tokens[0].toUpperCase();

            if (cmd.equals("EXIT") || cmd.equals("QUIT")) {
                run = false;
            } else if (cmd.equals("CONNECT")) {
                if (tokens.length < 3) {
                    System.out.println("Must provide both database name and user name.");
                } else {                
                    String dbName = tokens[1].trim();
                    String userName = Utils.strip(tokens[2]);

                    boolean shouldCreateStats = false;
                    if (tokens.length > 3) {
                        shouldCreateStats = Boolean.parseBoolean(tokens[3]);
                    }

                    db = new SQLDatabase("localhost", 26257, dbName, userName);
                    db.open();
                    db.refreshStats(shouldCreateStats);
                    System.out.println("Connected to database");
                }
            } else if (cmd.equals("SELECT")) {
                if (db == null) {
                    System.out.println("Not connected to a database.");
                } else {
                    db.select(line, true);
                }
            } else if (cmd.equals("DISCONNECT")) {
                db.close();
                db = null;
                System.out.println("Disconnected from database");
            } else if (cmd.equals("CREATE-TABLES")) {
                if (db == null) {
                    System.out.println("Not connected to a database.");
                } else if (tokens.length < 2) {
                    System.out.println("Must provide a schema text file.");
                } else {
                    String tablesPath = Utils.strip(tokens[1]);  // Path to file containing CREATE TABLE commands.
                    int numCreated = db.createTables(tablesPath);
                    System.out.printf("Created %d tables.\n", numCreated);
                }
            } else if (cmd.equals("IMPORT")) {
                if (db == null) {
                    System.out.println("Not connected to a database.");
                } else if (tokens.length < 3) {
                    System.out.println("Must provide a table name and CSV file path");
                } else {
                    String tableName = tokens[1].trim();
                    String csvPath = Utils.strip(tokens[2].trim());

                    boolean useHeaders = true;
                    int numHeaders = -1;
                    if (tokens.length > 3) {
                        useHeaders = Boolean.parseBoolean(tokens[3]);
                    }

                    String[] dataTypes = null;
                    if (tokens.length > 4) {
                        dataTypes = new String[tokens.length - 4];
                        for (int i = 4; i < tokens.length; i++) {
                            dataTypes[i-4] = tokens[i];
                        }
                    }

                    int wereInserted = db.importCsv(tableName, csvPath, useHeaders, dataTypes);
                    System.out.printf("Inserted %d records into %s\n", wereInserted, tableName);
                }
            } else if (cmd.equals("IMPORT-MANY")) {
                if (db == null) {
                    System.out.println("Not connected to a database.");
                } else if (tokens.length < 2) {
                    System.out.println("Must provide a CSV file.");
                } else {
                    String path = tokens[1].trim();
                    
                    boolean useHeaders = true;
                    if (tokens.length > 2) {
                        useHeaders = Boolean.parseBoolean(tokens[2]);
                    }
                    
                    List<String> filePaths = Utils.getFiles(path, ".csv");
                    for (String filePath : filePaths) {
                        String fileName = Utils.getFileName(filePath);
                        String tableName = fileName.split("\\.")[0];

                        int wereInserted = db.importCsv(tableName, filePath, useHeaders, null);
                        System.out.printf("Inserted %d records into %s\n", wereInserted, tableName);
                    }
                }
            } else if (cmd.equals("RUN")) {
                if (db == null) {
                    System.out.println("Not connected to a database.");
                } 
                else if (tokens.length < 6) {
                     System.out.println("Must provide an optimizer file, number of trials, query folder, profile folder, and output file.");
                } else {

                    String optConfig = tokens[1].trim();
                    int numTrials = Integer.parseInt(tokens[2].trim());
                    String queryPath = tokens[3].trim();
                    String profileFolder = tokens[4].trim();
                    String outputFile = Utils.strip(tokens[5]);
                    
                    List<String> filePaths = Utils.getFiles(queryPath, ".sql");
                    List<List<String>> queries = new ArrayList<List<String>>();
                    List<HashMap<String, List<Double>>> queryRuntimes = new ArrayList<HashMap<String, List<Double>>>();
                    
                    // double[] averageRuntimes = new double[filePaths.size()];

                    int index = 0;
                    for (String path : filePaths) {
                        queries.add(Utils.readQueries(path));

                        String fileName = Utils.getFileName(path);
                        String profilePath = String.format("%s/%s", profileFolder, fileName.replace(".sql", ".json")); 
                        queryRuntimes.add(Utils.readProfilingFromJson(profilePath));

                        // averageRuntimes[index] = Utils.readProfilingFromJson(profilePath);

                        index += 1;
                    }

                    int numArms = queries.get(0).size();
                    int numTypes = queries.size();
                    boolean shouldSimulate = true;
                    List<BanditOptimizer> optimizers = Utils.getOptimizers(optConfig, numArms, numTypes);

                    HashMap<String, OutputStats[]> results = new HashMap<String, OutputStats[]>();
                    for (BanditOptimizer optimizer : optimizers) {
                        OutputStats[] outputStats = db.runJoinQuery(queries, optimizer, numTrials, queryRuntimes, shouldSimulate);
                        results.put(optimizer.getName(), outputStats);
                    }

                    Utils.saveRegretsAsJson(results, outputFile);
                }
            } else if (cmd.equals("PARSE")) {
                if (tokens.length < 2) {
                    System.out.println("Must provide a file to parse");
                } else {
                    List<String> queries = Utils.readQueries(tokens[1]);
                    SQLParser parser = new SQLParser();
                    parser.getWhereCounts(queries.get(0));
                    System.out.println(parser.whereToInnerJoin(queries.get(0)));
                }
            } else if (cmd.equals("PROFILE")) {
                if (tokens.length < 4) {
                    System.out.println("Must provide a folder/file, number of trials and output folder.");
                } else {
                    String path = tokens[1].trim();
                    int numTrials = Integer.parseInt(tokens[2]);
                    String outputFolder = Utils.strip(tokens[3]);

                    List<String> queryPaths = Utils.getFiles(path, ".sql");
                    for (String queryPath : queryPaths) {
                        // TODO: Replace this path construction to be platform-independent
                        String[] queryFileTokens = queryPath.split("/");
                        String queryFileName = queryFileTokens[queryFileTokens.length - 1];
                        String outputPath = String.format("%s/%s", outputFolder, queryFileName.replace(".sql", ".json"));
                        System.out.println(outputPath);

                        List<String> queries = Utils.readQueries(queryPath);
                        db.profileQueries(queries, numTrials, outputPath);
                    }
                }
            } else {
                System.out.printf("Unknown command %s\n", tokens[0]);
            }
        }
    }
}
