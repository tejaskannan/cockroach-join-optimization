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

import org.postgresql.ds.PGSimpleDataSource;
import utils.Utils;


public class SQLDatabase {

    private DataSource ds;
    private boolean debug = false;
    private String server;
    private int port;
    private String dbName;
    private String userName;
    private HashMap<String, HashMap<String, Statistics>> tableStats;


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
    }

    public void refreshStats() {
        // TODO: Do this in a synchronous manner to support refreshing in another thread
        ArrayList<String> tables = this.getTables();
        for (String table : tables) {
            this.tableStats.put(table, this.getColumnStats(table));
        }
    }

    public void setDebug(boolean d) {
        this.debug = d;
    }

    public HashMap<String, Statistics> getColumnStats(String tableName) {
    
        HashMap<String, Statistics> result = new HashMap<String, Statistics>();
        String query = "SHOW STATISTICS FOR TABLE " + tableName + ";";  // Use prepared statement to avoid SQL Injection attacks
        
        try (Connection connection = ds.getConnection()) {
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                boolean rv = pstmt.execute();

                if (rv) {
                    ResultSet rs = pstmt.getResultSet();
                    ResultSetMetaData meta = rs.getMetaData();
                    int numColumns = meta.getColumnCount();

                    while (rs.next()) {
                        String colName = rs.getString("column_names");
                        Statistics stats = new Statistics(tableName, colName,
                                                          rs.getInt("row_count"), rs.getInt("distinct_count"));
                        result.put(colName, stats);
                    }
                }
            } catch (SQLException ex) {
                 Utils.printSQLException(ex);
            }
        } catch (SQLException ex) {
            Utils.printSQLException(ex);
        }

        return result;
    }

    public ArrayList<String> getTables() {
        ArrayList<String> tables = new ArrayList<String>();

        try (Connection connection = ds.getConnection()) {
            try (PreparedStatement pstmt = connection.prepareStatement("SHOW TABLES")) {
                boolean rv = pstmt.execute();

                if (rv) {
                    ResultSet rs = pstmt.getResultSet();
                    ResultSetMetaData meta = rs.getMetaData();
                    int numColumns = meta.getColumnCount();

                    while (rs.next()) {
                        String tableName = rs.getString("table_name");
                        tables.add(tableName);
                    }
                }
            } catch (SQLException ex) {
                Utils.printSQLException(ex);
            }
        } catch (SQLException ex) {
            Utils.printSQLException(ex); 
        }
        return tables;
    }

    public boolean select(String sql, boolean shouldPrint, String... args) {
        boolean returnVal = false;
        try (Connection connection = ds.getConnection()) {
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
 
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
                if (returnVal) {
                    ResultSet rs = pstmt.getResultSet();
                    ResultSetMetaData meta = rs.getMetaData();
                    int numColumns = meta.getColumnCount();

                    // Iterate through the result set
                    while (rs.next()) {
                        for (int i = 1; i <= numColumns; i++) {
                            String name = meta.getColumnName(i);
                            String type = meta.getColumnTypeName(i);
                        
                            if (shouldPrint) {
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
                }    
            } catch (SQLException ex) {
                Utils.printSQLException(ex);
            }
        } catch (SQLException ex) {
            Utils.printSQLException(ex);
        }
        return returnVal;
    }

    public int createTables(String path) {
        int numCreated = 0;
        BufferedReader reader;

        try (Connection connection = this.ds.getConnection()) {
            reader = new BufferedReader(new FileReader(path));
            String line = reader.readLine();
            StringBuilder cmd = new StringBuilder();

            connection.setAutoCommit(false);
            while (line != null) {
                line = line.trim();
                cmd.append(line);
                if (line.endsWith(";")) {
                    String createTableCmd = cmd.toString();
                    boolean isSuccess = execute(createTableCmd, connection);
                    numCreated += isSuccess ? 1 : 0;
                    cmd = new StringBuilder();  // Clear the buffer
                    connection.commit();
                }
                line = reader.readLine();
            }
        } catch (SQLException ex) {
            Utils.printSQLException(ex);
        }
        catch (IOException ex) {
            System.out.printf("Caught IO Exception: %s\n", ex.getMessage());
        }
        return numCreated;
    }

    public int importCsv(String tableName, String filePath) {
        String home = System.getenv("COCKROACH_OPT_HOME");
        String fullPath = home + "/" + filePath;
        System.out.println(fullPath);

        // TODO: Batch inserts
        int insertCount = 0;
        String headers = null;
        try (Connection connection = this.ds.getConnection()) {
            BufferedReader reader = new BufferedReader(new FileReader(fullPath));
            headers = reader.readLine().trim();
            int numHeaders = headers.split(",").length;

            String insertQuery = "INSERT INTO %s (%s) VALUES (%s);";
            String line = reader.readLine();
            while (line != null) {
                String[] tokens = line.split(",");
                String[] cleanedTokens = new String[tokens.length];
                
                for (int i = 0; i < tokens.length; i++) {
                    try {
                        Integer.parseInt(tokens[i]);
                        cleanedTokens[i] = tokens[i];
                    } catch (NumberFormatException ex1) {
                        try {
                            Float.parseFloat(tokens[i]);
                            cleanedTokens[i] = tokens[i];
                        } catch (NumberFormatException ex2) {
                            cleanedTokens[i] = String.format("'%s'", tokens[i]);
                        }
                    }
                }

                if (tokens.length == numHeaders) {
                    String values = String.join(",", cleanedTokens);
                    String sql = String.format(insertQuery, tableName, headers, values);
                    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                       insertCount += pstmt.executeUpdate();
                    } catch (SQLException ex) {
                        Utils.printSQLException(ex);
                    }
                }
                line = reader.readLine();
            }

            return insertCount;
        } catch (SQLException ex) {
            Utils.printSQLException(ex);
        } catch (IOException ex) {
            System.out.printf("Caught IO Exception: %s\n", ex.getMessage());
        }

        return 0;
    }


    private boolean execute(String sql, Connection connection) {
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.execute();
            return true;
        } catch (SQLException ex) {
            Utils.printSQLException(ex);
        }
        return false;
    }

}
