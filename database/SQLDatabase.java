package database;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.postgresql.ds.PGSimpleDataSource;
import org.la4j.Vector;
import utils.Utils;
import utils.OutputStats;
import parsing.SQLParser;
import bandits.BanditOptimizer;


public class SQLDatabase {

    private DataSource ds;
    private boolean debug = false;
    private String server;
    private int port;
    private String dbName;
    private String userName;
    private HashMap<String, HashMap<String, Statistics>> tableStats;
    private Connection connection;

    private static final int BATCH_SIZE = 1000;

    public SQLDatabase(String server, int port, String dbName, String userName) {
        this.server = server;
        this.port = port;
        this.dbName = dbName;
        this.userName = userName;

        // Initialize data source
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setServerName(server); 
        ds.setPortNumber(port);
        ds.setDatabaseName(dbName);
        ds.setUser(userName);
        ds.setPassword(null);
        ds.setSsl(false);
        this.ds = ds;
 
        this.tableStats = new HashMap<String, HashMap<String, Statistics>>();
        this.connection = null;
    }

    public void refreshStats(boolean shouldCreate) {
        // TODO: Do this in a synchronous manner to support refreshing in another thread
        ArrayList<String> tables = this.getTables();

        for (String table : tables) {
            if (shouldCreate) {
                this.createStats(table);
            }
            this.tableStats.put(table, this.getColumnStats(table));
        }
    }

    public void setDebug(boolean d) {
        this.debug = d;
    }

    public void open() {
        /**
         * Open the database connection
         */
        try {
            this.connection = this.ds.getConnection();
        } catch (SQLException ex) {
            Utils.printSQLException(ex);
        }
    }

    public void close() {
        /**
         * Close the database connection
         */
        try {
            this.connection.close();
            this.connection = null;
        } catch (SQLException ex) {
            Utils.printSQLException(ex);
        }
    }

    public HashMap<String, Statistics> getColumnStats(String tableName) {
        /**
         * Fetch statistics for all columns in the given table.
         *
         * @param tableName: Name of table to fetch stats for
         * @return Map from column name to set of statistics
         */
        HashMap<String, Statistics> result = new HashMap<String, Statistics>();

        // We should use a prepared statement to avoid injection, but for some reason Cockroach complains about them
        String query = String.format("SHOW STATISTICS FOR TABLE %s;", tableName);
        
        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            // Execute query
            ResultSet rs = pstmt.executeQuery();

            // Get information about results
            ResultSetMetaData meta = rs.getMetaData();
            int numColumns = meta.getColumnCount();

            // Fetch all statistics
            while (rs.next()) {
                String colName = rs.getString("column_names");
                int rowCount = rs.getInt("row_count");
                int distinctCount = rs.getInt("distinct_count");
                
                Statistics stats = new Statistics(tableName, colName, rowCount, distinctCount);
                result.put(colName, stats);
            }
        } catch (SQLException ex) {
             Utils.printSQLException(ex);
        }

        return result;
    }

    public void createStats(String table) {
        /**
         * Create statistics for the given table
         */
        String query = String.format("CREATE STATISTICS %s FROM %s;", table, table);

        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            pstmt.executeQuery();
        } catch (SQLException ex) {
            Utils.printSQLException(ex);
        }
    }

    private Vector getStats(List<String> tableOrder, List<String> colOrder) {
        double[] statsList = new double[tableOrder.size() * 2];
         
        for (int j = 0; j < tableOrder.size(); j++) {
            String table = tableOrder.get(j);
            String column = colOrder.get(j);
           
            Statistics colStats = this.tableStats.get(table).get(String.format("{%s}", column));
            double[] features = colStats.getFeatures();
            statsList[2 * j] = features[0];
            statsList[2 * j + 1] = features[1];
        }

        return Vector.fromArray(statsList);
    }

    public void profileQueries(List<String> queries, int numTrials, String outputPath) {
        /**
         * Profile given queries by measuring query execution latency.
         *
         * @param queries: Queries to execute
         * @param numTrials: Number of trials to execute
         * @param outputPath: Output JSON path (results are saved directly into this file)
         */

        // Initialize the results map (query -> list of latency measurements)   
        HashMap<String, List<Double>> results = new HashMap<String, List<Double>>();
        for (String query : queries) {
            results.put(query, new ArrayList<Double>());
        }

        SQLParser parser = new SQLParser();

        for (int i = 0; i <= numTrials; i++) {
            for (String query : queries) {
                
                // Convert to hash joins to control query ordering
                String hashJoin = parser.toHashJoin(query);

                try (PreparedStatement pstmt = this.connection.prepareStatement(hashJoin)) {
                    long start = System.currentTimeMillis();
                    pstmt.executeQuery();
                    long end = System.currentTimeMillis();

                    // Omit first round due to variance in caching
                    if (i > 0) {
                        results.get(query).add((double) (end - start));
                    }
                
                } catch (SQLException ex) {
                    Utils.printSQLException(ex);
                }
            }
        }

        Utils.saveResultsAsJson(results, outputPath);
    }


    public OutputStats[] runJoinQuery(List<List<String>> queries, BanditOptimizer optimizer, int numTrials, double[] averageRuntimes) {
        SQLParser parser = new SQLParser();

        ArrayList<Vector> stats;
        Random rand = new Random();
        OutputStats[] outputStats = new OutputStats[numTrials];
        for (int i = 0; i <= numTrials; i++) {

            // Start time to include all required preprocessing
            long start = System.currentTimeMillis();

            int queryType = rand.nextInt(queries.size());
            List<String> queryOrders = queries.get(queryType);

            stats = new ArrayList<Vector>();
            for (String query : queryOrders) {
                List<String> tableOrder = parser.getTableOrder(query);
                List<String> columnOrder = parser.getColumnOrder(query);
                stats.add(this.getStats(tableOrder, columnOrder));
            }

            int arm = optimizer.getArm(i + 1, queryType, stats); 
            String chosenQuery = queryOrders.get(arm);
            Vector chosenContext = stats.get(arm);

            // Turn query into a Hash Join to prevent later reordering
            String hashJoin = parser.toHashJoin(chosenQuery);

            // Execute query
            this.select(hashJoin, false);

            // Record ending time once query is complete
            long end = System.currentTimeMillis();

            // Don't record first trial to avoid outliers from caching
            if (i > 0) {
                double elapsed = (double) (end - start);
                double reward = -1 * elapsed;
                optimizer.update(arm, queryType, reward, chosenContext);
               
                double normalizedReward = optimizer.normalizeReward(reward, queryType);
                double regret = elapsed - averageRuntimes[queryType];
                outputStats[i-1] = new OutputStats(elapsed, normalizedReward, regret, arm, queryType);
            }
        }

        return outputStats;
    }

    public ArrayList<String> getTables() {
        ArrayList<String> tables = new ArrayList<String>();

        try (PreparedStatement pstmt = this.connection.prepareStatement("SHOW TABLES")) {
            ResultSet rs = pstmt.executeQuery();

            ResultSetMetaData meta = rs.getMetaData();
            int numColumns = meta.getColumnCount();

            while (rs.next()) {
                String tableName = rs.getString("table_name");
                tables.add(tableName);
            }
        } catch (SQLException ex) {
            Utils.printSQLException(ex);
        }

        return tables;
    }

    public boolean select(String sql, boolean shouldPrint, String... args) {
        boolean returnVal = false;
        try (PreparedStatement pstmt = this.connection.prepareStatement(sql)) {

            // Load arguments into the SQL Statement
            for (int i = 0; i < args.length; i++) {
                try {
                    int val = Integer.parseInt(args[i]);
                    pstmt.setInt(i + 1, val);
                } catch (NumberFormatException e) {
                    pstmt.setString(i + 1, args[i]);
                }
            }
        
            returnVal = pstmt.execute();

            if (returnVal && shouldPrint) {
                ResultSet rs = pstmt.getResultSet();
                ResultSetMetaData meta = rs.getMetaData();
                int numColumns = meta.getColumnCount();

                // Iterate through the result set
                while (rs.next()) {
                    for (int i = 1; i <= numColumns; i++) {
                        String name = meta.getColumnName(i);
                        String type = meta.getColumnTypeName(i);

                        if (type.equals("int8")) {
                            int val = rs.getInt(name);
                            System.out.printf("    %-8s -> %10s\n", name, val);
                        } else {
                            String str = rs.getString(name);
                            System.out.printf("    %-8s -> %10s\n", name, str);
                        }
                    }
                }
            }    
        } catch (SQLException ex) {
            Utils.printSQLException(ex);
        }

        return returnVal;
    }

    public int createTables(String path) {
        int numCreated = 0;
        BufferedReader reader;

        try {
            reader = new BufferedReader(new FileReader(path));
            String line = reader.readLine();
            StringBuilder cmd = new StringBuilder();

            connection.setAutoCommit(false);
            while (line != null) {
                line = line.trim();
                cmd.append(line);
                if (line.endsWith(";")) {
                    String createTableCmd = cmd.toString();
                    boolean isSuccess = execute(createTableCmd);
                    numCreated += isSuccess ? 1 : 0;
                    cmd = new StringBuilder();  // Clear the buffer
                    connection.commit();
                }
                line = reader.readLine();
            }
        } catch (SQLException ex) { 
            Utils.printSQLException(ex);
        } catch (IOException ex) {
            System.out.printf("Caught IO Exception: %s\n", ex.getMessage());
        }
        return numCreated;
    }

    public int importCsv(String tableName, String filePath, boolean useHeaders, String[] dataTypes) {
        int insertCount = 0;
        String headers = null;

        // Derive number of headers from given data types
        int numHeaders = -1;
        if (dataTypes != null && dataTypes.length > 0) {
            numHeaders = dataTypes.length;
        }

        try (Connection connection = this.ds.getConnection()) {
            // Read the file
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            
            // Obtain the data headers
            String insertQuery;
            if (useHeaders) {
                headers = reader.readLine().trim();
                numHeaders = headers.split(",").length;

                // Format query and records list
                insertQuery = "INSERT INTO %s (%s) VALUES %s;";
            } else {
                insertQuery = "INSERT INTO %s VALUES %s;";
            }
            
            ArrayList<String> insertList = new ArrayList<String>();

            // Read data line-by-line
            String line = reader.readLine();
            while (line != null) {
                String[] tokens = line.split(",");

                int length = tokens.length;
                if (numHeaders != -1) {
                    length = numHeaders;
                }

                String[] cleanedTokens = new String[length];
                
                // Convert to correct data types
                for (int i = 0; i < tokens.length && i < cleanedTokens.length; i++) {
                    if (dataTypes != null) {
                        if (dataTypes[i].equals("int")) {
                            try {
                                Integer.parseInt(tokens[i]);
                                cleanedTokens[i] = tokens[i];
                            } catch (NumberFormatException ex) {
                                cleanedTokens[i] = "0";
                            }
                        } else if (dataTypes[i].equals("float")) {
                            try {
                                Float.parseFloat(tokens[i]);
                                cleanedTokens[i] = tokens[i];
                            } catch (NumberFormatException ex) {
                                cleanedTokens[i] = "0.0";
                            }
                        } else if (dataTypes[i].startsWith("string")) {
                            String[] dataTypeTokens = dataTypes[i].split("\\(");
                            String strLen = dataTypeTokens[1].substring(0, dataTypeTokens[1].length() - 1);

                            int maxLen = Integer.parseInt(strLen);
                            String t = tokens[i].replace("'", "''");
                            
                            if (t.length() > 0 && tokens[i].length() < maxLen) {
                                cleanedTokens[i] = String.format("'%s'", tokens[i].replace("'", "''"));
                            } else {
                                cleanedTokens[i] = "''";
                            }
                        } else {
                            cleanedTokens[i] = null;
                        }
                    } else {
                        try {
                            Integer.parseInt(tokens[i]);
                            cleanedTokens[i] = tokens[i];
                        } catch (NumberFormatException ex1) {
                            try {
                                Float.parseFloat(tokens[i]);
                                cleanedTokens[i] = tokens[i];
                            } catch (NumberFormatException ex2) {
                                cleanedTokens[i] = String.format("'%s'", tokens[i].replace("'", "''"));
                            }
                        }
                    }
                }

                // Append nulls if needed
                int k = tokens.length;
                while (useHeaders && numHeaders > -1 && k < cleanedTokens.length) {
                    cleanedTokens[k] = null;
                    k += 1;
                }

                if ((!useHeaders && numHeaders == -1) || cleanedTokens.length == numHeaders) {
                    String values = String.join(",", cleanedTokens);
                    insertList.add("(" + values + ")"); 
                }

                // Batch insert when size threshold reached
                if (insertList.size() >= BATCH_SIZE) {
                    String records = String.join(", ", insertList);

                    String sql;
                    if (useHeaders) {
                        sql = String.format(insertQuery, tableName, headers, records);
                    } else {
                        sql = String.format(insertQuery, tableName, records);
                    }

                    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                       insertCount += pstmt.executeUpdate();
                    } catch (SQLException ex) {
                        Utils.printSQLException(ex);
                    }

                    System.out.printf("Inserted %d records.\r", insertCount);
                    insertList = new ArrayList<String>();
                }
                line = reader.readLine();
            }

            // Cleanup Insertions
            if (insertList.size() > 0) {
                String records = String.join(", ", insertList);
                
                String sql;
                if (useHeaders) {
                    sql = String.format(insertQuery, tableName, headers, records);
                } else {
                    sql = String.format(insertQuery, tableName, records);
                }
                
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                   insertCount += pstmt.executeUpdate();
                } catch (SQLException ex) {
                    Utils.printSQLException(ex);
                }
            }

            return insertCount;
        } catch (SQLException ex) {
            Utils.printSQLException(ex);
        } catch (IOException ex) {
            System.out.printf("Caught IO Exception: %s\n", ex.getMessage());
        }

        return 0;
    }


    private boolean execute(String sql) {
        try (PreparedStatement pstmt = this.connection.prepareStatement(sql)) {
            pstmt.execute();
            return true;
        } catch (SQLException ex) {
            Utils.printSQLException(ex);
        }
        return false;
    }

}
