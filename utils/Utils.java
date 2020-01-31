package utils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;


public class Utils {

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


