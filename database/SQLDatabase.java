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
import parsing.TableColumn;
import bandits.BanditOptimizer;


public class SQLDatabase {

    private DataSource ds;
    private boolean debug = false;
    private String server;
    private int port;
    private String dbName;
    private String userName;
    private HashMap<String, HashMap<String, Statistics>> tableStats;
    private HashMap<String, List<String>> tableIndexes;
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
        this.tableIndexes = new HashMap<String, List<String>>();
        this.connection = null;
    }

    public void refreshStats(boolean shouldCreate) {
        /**
         * Fetches database statistics
         *
         * @param shouldCreate: Whether we should create statistics relations (can be expensive)
         */
        // TODO: Do this in a synchronous manner to support refreshing in another thread
        ArrayList<String> tables = this.getTables();

        // Fetch statistics for all columns
        for (String table : tables) {
            if (shouldCreate) {
                this.createStats(table);
            }
            
            HashMap<String, Statistics> columnStats = this.getColumnStats(table);
            this.tableStats.put(table, columnStats);
            this.addColumnRange(table, columnStats);
            this.tableIndexes.put(table, this.getTableIndexes(table));
        }
    }

    public void setDebug(boolean d) {
        this.debug = d;
    }

    public HashMap<String, HashMap<String, Statistics>> getTableStats() {
        return this.tableStats;
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
               
                colName = colName.substring(1, colName.length() - 1);

                Statistics stats = new Statistics(tableName, colName, rowCount, distinctCount);
                result.put(colName, stats);
            }
        } catch (SQLException ex) {
             Utils.printSQLException(ex);
        }

        return result;
    }

    public void addColumnRange(String tableName, HashMap<String, Statistics> columnStats) {

        String[] columns = new String[columnStats.size()];
        StringBuilder queryBuilder = new StringBuilder();
        String query;

        int index = 0;
        for (String column : columnStats.keySet()) {
            query = String.format("SELECT MIN(%s) AS min_value, MAX(%s) AS max_value FROM %s; ", column, column, tableName);
            queryBuilder.append(query);

            columns[index] = column;
            index += 1;
        }
       
        query = queryBuilder.toString();
        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            
            boolean res = pstmt.execute();

            index = 0;
            while (res) {
                ResultSet rs = pstmt.getResultSet();
                ResultSetMetaData meta = rs.getMetaData();

                while (rs.next()) {
                    try {
                        String column = columns[index];
                        Statistics stats = columnStats.get(column);

                        int minValue = rs.getInt("min_value");
                        int maxValue = rs.getInt("max_value");
                        stats.setRange(minValue, maxValue);
                    } catch (SQLException ex) {
                        if (!ex.getSQLState().equals("22003")) {
                            throw ex;
                        }
                    }

                    index += 1;
                }

                res = pstmt.getMoreResults();
            }
        } catch (SQLException ex) {
            if (!ex.getSQLState().equals("22003")) {
                Utils.printSQLException(ex);
            }
        }
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

    public List<String> getTableIndexes(String tableName) {
        /**
         * Fetch columns which are indexed.
         */
        String query = String.format("SHOW INDEX FROM %s;", tableName);
        List<String> indexes = new ArrayList<String>();

        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            ResultSet rs = pstmt.executeQuery();

            ResultSetMetaData meta = rs.getMetaData();

            String columnName;
            while (rs.next()) {
                columnName = rs.getString("column_name");
                indexes.add(columnName);
            }
        } catch (SQLException ex) {
            Utils.printSQLException(ex);
        }

        return indexes;
    }

    private Vector getStats(List<TableColumn> colOrder, HashMap<String, Double> whereSelectivity) {
        /**
         * Return statistics for a given column order and where clause selectivity
         * 
         * @param colOrder Order of columns in the join ordering
         * @param whereSelectivity: Map of table names to where clause selectivity. Null if no where clauses.
         * @return A vector containing the statistics for this column order
         */
        // Create table multipliers
       // if (whereSelectivity != null) {
       //     for (TableColumn column : whereCounts.keySet()) {
       //         String tableName = column.getTableName();
       //         String columnName = column.getColumnName();

       //         Statistics colStats = this.tableStats.get(tableName).get(columnName);
       //         double keepFraction = ((double) whereCounts.get(column)) / colStats.getTableDistinct();

       //         whereMultipliers.put(tableName, keepFraction);
       //     }
       // }
        
        ArrayList<Statistics> statsList = new ArrayList<Statistics>();
        for (int j = 0; j < colOrder.size(); j++) {
            TableColumn column = colOrder.get(j);
            
            String tableName = column.getTableName();
            String columnName = column.getColumnName();

            Statistics colStats = this.tableStats.get(tableName).get(columnName);
            statsList.add(colStats);
        }

        return Statistics.combineStatistics(statsList, whereSelectivity);
    }

    public void profileQueries(List<String> queries, int numTrials, String outputPath, boolean fixOrderings) {
        /**
         * Profile given queries by measuring query execution latency.
         *
         * @param queries: Queries to execute
         * @param numTrials: Number of trials to execute
         * @param outputPath: Output JSON path (results are saved directly into this file)
         * @param fixOrderings: Whether to fix table orderings using join hints
         */
        // If the orders are not fixed, then we only test one query. Cockroach will automatically reorder
        // as it sees fit.
        if (!fixOrderings) {
            List<String> single = new ArrayList<String>();
            single.add(queries.get(0));
            queries = single;
        }

        // Initialize the results map (query -> list of latency measurements)   
        HashMap<String, List<Double>> results = new HashMap<String, List<Double>>();
        for (String query : queries) {
            results.put(query, new ArrayList<Double>());
        }

        SQLParser parser = new SQLParser();

        for (int i = 0; i <= numTrials; i++) {
            for (String query : queries) {
                
                // Convert to hash joins to control query ordering
                String joinQuery = query;
                if (fixOrderings) {
                    joinQuery = parser.toHashJoin(query);
                }

                try (PreparedStatement pstmt = this.connection.prepareStatement(joinQuery)) {
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


    public OutputStats[] runJoinQuery(List<List<String>> queries, BanditOptimizer optimizer, int numTrials, List<HashMap<String, List<Double>>> queryRuntimes, int[] queryTypes, boolean shouldSimulate, boolean shouldUpdate) {
        SQLParser parser = new SQLParser();

        // Compute best and worst averages for each query type
        double[] bestAverages = new double[queryRuntimes.size()];
        double[] worstAverages = new double[queryRuntimes.size()];
        for (int i = 0; i < queryRuntimes.size(); i++) {
            bestAverages[i] = Utils.getBestAverage(queryRuntimes.get(i), queries.get(i));
            worstAverages[i] = Utils.getWorstAverage(queryRuntimes.get(i), queries.get(i)); 
        }

        // Compute averages for each query
        List<HashMap<String, Double>> averageRuntimes = new ArrayList<HashMap<String, Double>>();
        int[] bestArms = new int[queries.size()];
        for (int i = 0; i < queryRuntimes.size(); i++) {
            HashMap<String, List<Double>> profilingResults  = queryRuntimes.get(i);
            HashMap<String, Double> averages = new HashMap<String, Double>();
        
            bestArms[i] = -1;
            int a = 0;
            for (String query : queries.get(i)) {
                double avg = Utils.average(profilingResults.get(query));
                averages.put(query, avg);

                // System.out.printf("%f ", avg);

                if (avg == bestAverages[i]) {
                    bestArms[i] = a;
                }

                a += 1;
            }

            System.out.printf("%d ", bestArms[i]);

            averageRuntimes.add(averages);
        }

        System.out.println();

        // Run queries
        ArrayList<Vector> stats;
        Random rand = new Random();
        OutputStats[] outputStats = new OutputStats[numTrials];
        double elapsed;
        boolean shouldExploit = !shouldUpdate;  // Exploit when we are in test mode
        for (int i = 0; i <= numTrials; i++) {

            // Start time to include all required preprocessing
            long start = System.currentTimeMillis();

            // Select random query to run
            int queryType = queryTypes[i];
            List<String> queryOrders = queries.get(queryType);

            // Create context from database statistics
            stats = new ArrayList<Vector>();
            // System.out.printf("Type: %d, Stats: ", queryType);
            for (String query : queryOrders) {
                List<TableColumn> columnOrder = parser.getColumnOrder(query);
                HashMap<String, Double> whereSelectivity = parser.getWhereSelectivity(query, this.tableStats);
                Vector s = this.getStats(columnOrder, whereSelectivity);
                // System.out.printf(s.toString());
                stats.add(s);
            }


            // System.out.println();

            // Select query using the context for each statistics ordering
            int arm = optimizer.getArm(i + 1, queryType, stats, shouldExploit); 
            String chosenQuery = queryOrders.get(arm);

            // Turn query into a Hash Join to prevent later reordering
            String hashJoin = parser.toHashJoin(chosenQuery);

            // Execute query
            if (shouldSimulate) {
                // Simulates request using profiling results
                List<Double> latencies = queryRuntimes.get(queryType).get(chosenQuery);
                int timeIndex = rand.nextInt(latencies.size());
                long end = System.currentTimeMillis();
                elapsed = latencies.get(timeIndex) + ((double) (end - start));
            } else {
                // Execute request against the database
                this.select(hashJoin, false);
                long end = System.currentTimeMillis();
                elapsed = (double) (end - start);
            }

            // Don't record first trial to avoid outliers from caching
            if (i > 0) {
                double reward = -1 * elapsed;
                
                if (shouldUpdate) {
                    optimizer.update(arm, queryType, reward, stats);
                }

                double normalizedReward = optimizer.normalizeReward(reward, queryType);
                double regret = (averageRuntimes.get(queryType).get(chosenQuery) - bestAverages[queryType]) / (worstAverages[queryType] - bestAverages[queryType]);
                outputStats[i-1] = new OutputStats(elapsed, normalizedReward, regret, arm, queryType, bestArms[queryType], bestAverages[queryType]);
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
