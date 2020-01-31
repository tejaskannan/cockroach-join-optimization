package database;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileNotFoundException;

import utils.Utils;


public class SQLTable {

    private DataSource ds;
    private boolean debug = false;
    private static final int BATCH_SIZE = 1000;


    public SQLTable(DataSource ds) {
        this.ds = ds;
    }

    public void setDebug(boolean d) {
        this.debug = d;
    }

    public ResultSet execute(String sql, String... args) {
        ResultSet rs = null;

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
            
                boolean rv = pstmt.execute();
                if (rv) {
                    rs = pstmt.getResultSet();
                    if (this.debug) {
                        Utils.printResultSet(rs);
                    }
                }

            } catch (SQLException ex) {
                System.out.printf("SQL Execution ERROR: { state => %s, cause => %s, message => %s }\n",
                              ex.getSQLState(), ex.getCause(), ex.getMessage());
                rs = null;
            }
        } catch (SQLException ex) {
            System.out.printf("SQL Execution ERROR: { state => %s, cause => %s, message => %s }\n",
                              ex.getSQLState(), ex.getCause(), ex.getMessage());
            rs = null;
        }
        return rs;
    }

    public int uploadCsvAsTable(String path, String tableName, String[] columns) {

        BufferedReader csvReader = null;
        try {
            csvReader = new BufferedReader(new FileReader(path));
        } catch (FileNotFoundException ex) {
            System.out.printf("Caught FileNotFoundExeception: %s\n", ex.getMessage());
            return 0;
        }

        String[] headers;
        try {
            String row = csvReader.readLine();
            headers = row.split(",");
            String[] dataTypes = new String[headers.length];
        } catch (IOException ex) {
            System.out.printf("Caught IOException: %s\n", ex.getMessage());
            return 0;
        }

        // Get Column Indices
        int[] indices = new int[headers.length];
        for (int i = 0; i < columns.length; i++) {
            for (int j = 0; j < headers.length; j++) {
                if (headers[j].equals(columns[i])) {
                    indices[i] = j;
                    break;
                }
            }
        }
    
        // Upload the data
        StringBuilder insertBuilder = new StringBuilder();
        insertBuilder.append("INSERT INTO soccer.");
        insertBuilder.append(tableName);
        insertBuilder.append(" VALUES (");

        try {
            String row;
            String insertQuery = insertBuilder.toString();
            while ((row = csvReader.readLine()) != null) {
                String[] data = row.split(",");
                // String[] insertData = new String[indices.length];
                String query = insertQuery;
                
                for (int i = 0; i < indices.length; i++) {
                    query += data[indices[i]];
                    if (i < indices.length - 1) {
                        query += ", ";
                    }
                }
                query += ");";
                execute(query);
            }
        } catch (IOException ex) {
            System.out.printf("Caught IOException: %s\n", ex.getMessage());
            return 0;
        }

        return 1;
    }

    private String getDataType(String val) {
        try {
            Integer.parseInt(val);
            return "int";
        } catch (NumberFormatException ex) {
            return "varchar(100)";
        } finally {
            return "varchar(100)";
        } 
    }


}
