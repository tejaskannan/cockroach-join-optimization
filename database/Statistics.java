package database;

import java.lang.Math;
import java.lang.Iterable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;

import org.la4j.Vector;
import org.la4j.vector.dense.BasicVector;


public class Statistics {

    private String tableName;
    private String columnName;
    private double tableRows;
    private double tableDistinct;
    private double rowCount;
    private Range range;

    public Statistics(String tableName, String columnName, int numRows, int numDistinct) {
        this.tableName = tableName;
        this.columnName = columnName;
        this.tableRows = (double) numRows;
        this.tableDistinct = (double) numDistinct;

        // For now, we assume that the distribution is uniform.
        // TODO: Integrate histograms
        this.rowCount = this.tableRows / this.tableDistinct;

        this.range = null;
    }

    public String getTableName() {
        return this.tableName;
    }

    public String getColumnName() {
        return this.columnName;
    }

    public double getTableRows() {
        return this.tableRows;
    }

    public double getTableDistinct() {
        return this.tableDistinct;
    }

    public double getRowCount() {
        return this.rowCount;
    }

    public Range getRange() {
        return this.range;
    }

    public void setRange(int min, int max) {
        this.range = new Range(min, max);
    }

    public String toString() {
        return String.format("Table: %s, Column: %s, # Rows: %d, # Distinct: %d, # Rows per Value: %s", this.tableName, this.columnName, this.getTableRows(), this.getTableDistinct(), this.getRowCount());
    }


    private static double applyColumnSelectivity(String tableName, double tableCount, double distinctCount, HashMap<String, Double> whereSelectivity) {
        if (!whereSelectivity.containsKey(tableName)) {
            return distinctCount;
        }

        double removeProb = Math.pow(1.0 - whereSelectivity.get(tableName), tableCount / distinctCount);
        return distinctCount * (1.0 - removeProb);
    }

    private static double applyTableSelectivity(String tableName, double tableCount, HashMap<String, Double> whereSelectivity) {
        if (!whereSelectivity.containsKey(tableName)) {
            return tableCount;
        }
        return tableCount * whereSelectivity.get(tableName);
    }

    public static Vector combineStatistics(Iterable<Statistics> statsIter, HashMap<String, Double> whereSelectivity) {
        /**
         * Packages the given statistics into a single context vector.
         * 
         * @param statsIter: Sequence of statistics from involved relations and columns
         * @param whereSelectivity: Fractions to keep based on where selectivity
         * @return A vector containing the statistics
         */
        ArrayList<Double> tableStats = new ArrayList<Double>();
        ArrayList<Double> columnStats = new ArrayList<Double>(); 

        ArrayList<String> tableNames = new ArrayList<String>();
        ArrayList<String> columnNames = new ArrayList<String>();

        for (Statistics stats : statsIter) {
            // Save results for each table and column
            tableStats.add(stats.getTableRows());
            columnStats.add(stats.getTableDistinct());

            tableNames.add(stats.getTableName());
            columnNames.add(String.format("%s.%s", stats.getTableName(), stats.getColumnName()));
        }

        double[] result = new double[tableStats.size() + columnStats.size()];
        int offset = tableStats.size();
        double firstTableCount;
        double firstColumnCount;
        double secondTableCount;
        double secondColumnCount;
        for (int i = 0; i < tableStats.size(); i += 2) {

            firstTableCount = applyTableSelectivity(tableNames.get(i), tableStats.get(i), whereSelectivity);
            secondTableCount = applyTableSelectivity(tableNames.get(i+1), tableStats.get(i+1), whereSelectivity);

            firstColumnCount = applyColumnSelectivity(tableNames.get(i), tableStats.get(i), columnStats.get(i), whereSelectivity);
            secondColumnCount = applyColumnSelectivity(tableNames.get(i+1), tableStats.get(i+1), columnStats.get(i+1), whereSelectivity);

            if (firstTableCount > secondTableCount) {
                result[i] = firstTableCount;
                result[i+1] = secondTableCount;
                result[i+offset] = firstColumnCount;
                result[i+offset+1] = secondColumnCount;
            } else {
                result[i] = secondTableCount;
                result[i+1] = firstTableCount;
                result[i+offset] = secondColumnCount;
                result[i+offset+1] = firstColumnCount;
            }
        }

        Vector statsVector = Vector.fromArray(result);
        return statsVector;

        // Normalize results and save into a single vector
       // Vector result = new BasicVector(tableStats.size() + columnStats.size());

       // HashSet<String> seenTables = new HashSet<String>();
       // double minTableSize = Double.MAX_VALUE;
       // for (int i = 0; i < tableStats.size(); i += 2) {
       //     
       //     double firstTableCount = tableStats.get(i);
       //     String firstTableName = tableNames.get(i);
       //     //if (whereMultipliers.containsKey(firstTableName)) {
       //     //    firstTableCount *= whereMultipliers.get(firstTableName);
       //     //}

       //     //if (seenTables.contains(firstTableName)) {
       //     //    firstTableCount = Math.min(firstTableCount, minTableSize);
       //     //}

       //     double secondTableCount = tableStats.get(i+1);
       //     String secondTableName = tableNames.get(i+1);
       //     //if (whereMultipliers.containsKey(secondTableName)) {
       //     //    secondTableCount *= whereMultipliers.get(secondTableName);
       //     //}

       //     //if (seenTables.contains(secondTableName)) {
       //     //    secondTableCount = Math.min(secondTableCount, minTableSize);
       //     //}

       //     double smallerCount = Math.min(firstTableCount, secondTableCount);
       //     double largerCount = Math.max(firstTableCount, secondTableCount);
       //     
       //     result.set(i, largerCount);
       //     result.set(i+1, smallerCount);

       //     seenTables.add(firstTableName);
       //     seenTables.add(secondTableName);

       //     minTableSize = Math.min(smallerCount, minTableSize);
       // }

       // double selectivity;
       // double remove_prob;
       // int offset = tableStats.size();
       // HashSet<String> seenColumns = new HashSet<String>();
       // double minColumnCount = Double.MAX_VALUE;
       // for (int i = 0; i < columnStats.size(); i += 2) {
       //     
       //     double firstColumnCount = columnStats.get(i);
       //     //if (seenColumns.contains(columnNames.get(i))) {
       //     //    firstColumnCount = Math.min(firstColumnCount, minColumnCount);
       //     //}

       //     double secondColumnCount = columnStats.get(i+1);
       //     //if (seenColumns.contains(columnNames.get(i+1))) {
       //     //    secondColumnCount = Math.min(secondColumnCount, minColumnCount);
       //     //}

       //     double smallerCount = Math.min(firstColumnCount, secondColumnCount);
       //     double largerCount = Math.max(firstColumnCount, secondColumnCount);
       //     
       //     result.set(i + offset, largerCount);
       //     result.set(i + offset + 1, smallerCount);

       //     seenColumns.add(columnNames.get(i));
       //     seenColumns.add(columnNames.get(i+1));

       //     // Set selectivity
       //    // double firstColumnSelectivity = columnStats.get(i);
       //    // String firstTableName = tableNames.get(i);
       //    // if (whereMultipliers.containsKey(firstTableName)) {
       //    //     selectivity = whereMultipliers.get(firstTableName);
       //    //     remove_prob = Math.pow(1.0 - selectivity, tableStats.get(i) / firstColumnSelectivity);
       //    //     firstColumnSelectivity -= firstColumnSelectivity * remove_prob;
       //    // }

       //    // double secondColumnSelectivity = columnStats.get(i+1);
       //    // String secondTableName = tableNames.get(i+1);
       //    // if (whereMultipliers.containsKey(secondTableName)) {
       //    //     selectivity = whereMultipliers.get(secondTableName);
       //    //     remove_prob = Math.pow(1.0 - selectivity, tableStats.get(i+1) / secondColumnSelectivity);
       //    //     secondColumnSelectivity -= secondColumnSelectivity - secondColumnSelectivity * remove_prob;
       //    // }
 
       //    // result.set(i + offset + columnStats.size(), firstColumnSelectivity);
       //    // result.set(i + offset + columnStats.size() + 1, secondColumnSelectivity);

       //     minColumnCount = Math.min(smallerCount, minColumnCount);
       // }

       // return result;
    }


}
