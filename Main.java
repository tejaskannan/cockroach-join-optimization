import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Random;

import utils.Utils;
import database.SQLDatabase;
import bandits.OptimizerFactory;
import bandits.BanditOptimizer;
import bandits.LinearThompsonSamplingOptimizer;
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
                    db = new SQLDatabase("localhost", 26257, dbName, userName);
                    db.refreshStats();
                    System.out.println("Connected to database");
                }
            } else if (cmd.equals("SELECT")) {
                if (db == null) {
                    System.out.println("Not connected to a database.");
                } else {
                    db.select(line, true);
                }
            } else if (cmd.equals("DISCONNECT")) {
                db = null;
                System.out.println("Disconnected from database");
            } else if (cmd.equals("CREATE-TABLES")) {
                if (db == null) {
                    System.out.println("Not connected to a database.");
                } else if (tokens.length < 2) {
                    System.out.println("Must provide a schema text file.");
                } else {
                    String tablesPath = Utils.strip(tokens[1]);  // Path to 'txt' file containing CREATE TABLE commands.
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
                    int wereInserted = db.importCsv(tableName, csvPath);
                    System.out.printf("Inserted %d records into %s\n", wereInserted, tableName);
                }
            } else if (cmd.equals("RUN")) {
                if (db == null) {
                    System.out.println("Not connected to a database.");
                // } 
                // else if (tokens.length < 4) {
                //     System.out.println("Must provide an optimizer name, query file and number of trials.");
                } else {
                    List<String> queries = new ArrayList<String>() {{
                        add("SELECT * FROM player AS p INNER HASH JOIN player_attributes AS pa ON p.player_api_id = pa.player_api_id");
                        add("SELECT * FROM player_attributes AS pa INNER HASH JOIN player AS p ON pa.player_api_id = p.player_api_id");
                    }};

                    List<List<String>> tableOrders = new ArrayList<List<String>>() {{
                        add(new ArrayList<String>() {{ add("player"); add("player_attributes"); }});
                        add(new ArrayList<String>() {{ add("player_attributes"); add("player"); }});
                    }};

                    List<List<String>> colOrders = new ArrayList<List<String>>() {{
                        add(new ArrayList<String>() {{ add("player_api_id"); add("player_api_id"); }});
                        add(new ArrayList<String>() {{ add("player_api_id"); add("player_api_id"); }});
                    }};

                    BanditOptimizer optimizer = new LinearThompsonSamplingOptimizer(queries.size(), 1, 4, 0.01, 10.0);

                    db.runJoinQuery(queries, tableOrders, colOrders, optimizer, 10);
 
                   // String optName = tokens[1];
                   // List<String> queries = Utils.readQueries(tokens[2]);
                   // int numTrials = Integer.parseInt(Utils.strip(tokens[3]));

                   // double[] optArgs = new double[tokens.length - 4];
                   // for (int i = 4; i < tokens.length; i++) {
                   //     optArgs[i-4] = Double.parseDouble(Utils.strip(tokens[i]));
                   // }

                   // double[] times = runOptimizer(db, optName, queries, numTrials, optArgs);
                   // Utils.writeResults(optName + "_results.txt", times);
                }
            } else if (cmd.equals("PARSE")) {
                List<String> queries = Utils.readQueries(tokens[1]);
                SQLParser parser = new SQLParser();
                parser.whereToInnerJoin(queries.get(0));
            }
            else {
                System.out.printf("Unknown command %s\n", tokens[0]);
            }
        }
    }

//    private static double[] runOptimizer(SQLDatabase db, String optimizerName, List<String> queries, int trials, double... args) {
//        SQLParser parser = new SQLParser();
//        
//        List<List<String>> joinQueries = new ArrayList<List<String>>();
//        for (String query : queries) {
//            String innerJoinQuery = parser.whereToInnerJoin(query);
//            List<String> joinOptions = parser.getJoinOptions(innerJoinQuery);
//            joinQueries.add(joinOptions);
//        }
//
//        // For now, we assume that all queries have the same number of joins
//        int numArms = joinQueries.get(0).size();
//        int numTypes = joinQueries.size();
//        BanditOptimizer opt = OptimizerFactory.banditFactory(optimizerName, numArms, numTypes, args);
//
//        double[] times = new double[trials];
//        Random rand = new Random();
//        for (int i = 0; i <= trials; i++) {
//            int queryType =  rand.nextInt(joinQueries.size());
//            List<String> options = joinQueries.get(queryType);
//            
//            long start = System.currentTimeMillis();
//            int arm = opt.getArm(i);
//            String chosenQuery = options.get(arm);
//            db.select(chosenQuery, false);
//            long end = System.currentTimeMillis();
//
//            if (i > 0) {
//                double elapsed = (double) (end - start);
//                opt.update(arm, queryType, -1 * elapsed);
//                times[i-1] = opt.normalizeReward(queryType, elapsed);
//            }
//        }
//
//        return times;
//    }

}

