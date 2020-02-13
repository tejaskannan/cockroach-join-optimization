package utils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
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

    public static List<String> getFiles(String path) {
        File file = new File(path);
        List<String> result = new ArrayList<String>();

        if (!file.exists()) {
            return result;
        }

        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                if (f.getName().endsWith(".csv")) {
                    result.add(f.getPath());
                }
            }
        } else {
            result.add(path);
        }

        return result;
    }

}
