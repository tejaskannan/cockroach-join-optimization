package utils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;


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

    public static List<String> readQueries(String path) {
        List<String> queries = new ArrayList<String>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            StringBuilder q = new StringBuilder();

            String line = reader.readLine();
            while (line != null) {
                q.append(line);
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


    public static void printResultSet(ResultSet rs) {
        try {
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
        } catch (SQLException ex) {
            System.out.printf("SQL Execution ERROR: { state => %s, cause => %s, message => %s }\n",
                              ex.getSQLState(), ex.getCause(), ex.getMessage());
        }
    }


}


