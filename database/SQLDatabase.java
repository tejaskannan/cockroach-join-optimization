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
                 System.out.printf("SQL Execution ERROR: { state => %s, cause => %s, message => %s }\n",
                                   ex.getSQLState(), ex.getCause(), ex.getMessage());
            }
        } catch (SQLException ex) {
             System.out.printf("SQL Execution ERROR: { state => %s, cause => %s, message => %s }\n",
                               ex.getSQLState(), ex.getCause(), ex.getMessage());
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
                 System.out.printf("SQL Execution ERROR: { state => %s, cause => %s, message => %s }\n",
                                   ex.getSQLState(), ex.getCause(), ex.getMessage());
            }
        } catch (SQLException ex) {
             System.out.printf("SQL Execution ERROR: { state => %s, cause => %s, message => %s }\n",
                               ex.getSQLState(), ex.getCause(), ex.getMessage());
        
        }
        return tables;
    }

    public boolean select(String sql, String... args) {
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
                System.out.printf("SQL Execution ERROR: { state => %s, cause => %s, message => %s }\n",
                              ex.getSQLState(), ex.getCause(), ex.getMessage());
            }
        } catch (SQLException ex) {
            System.out.printf("SQL Execution ERROR: { state => %s, cause => %s, message => %s }\n",
                              ex.getSQLState(), ex.getCause(), ex.getMessage());
        }
        return returnVal;
    }
}
