package database;

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

    public Statistics(String tableName, String columnName, int numRows, int numDistinct) {
        this.tableName = tableName;
        this.columnName = columnName;
        this.tableRows = (double) numRows;
        this.tableDistinct = (double) numDistinct;

        // For now, we assume that the distribution is uniform.
        // TODO: Integrate histograms
        this.rowCount = this.tableRows / this.tableDistinct;

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

    public String toString() {
        return String.format("Table: %s, Column: %s, # Rows: %d, # Distinct: %d, # Rows per Value: %s", this.tableName, this.columnName, this.getTableRows(), this.getTableDistinct(), this.getRowCount());
    }

    public static Vector combineStatistics(Iterable<Statistics> statsIter, HashMap<String, Double> whereMultipliers) {
        /**
         * Packages the given statistics into a single context vector.
         * 
         * @param statsIter: Sequence of statistics from involved relations and columns
         * @param whereMultipliers: Fractions to keep based on where selectivity
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
            columnNames.add(String.format("%s.%s".format(stats.getTableName(), stats.getColumnName())));
        }

        // Normalize results and save into a single vector
        Vector result = new BasicVector(tableStats.size() + columnStats.size());

        HashSet<String> seenTables = new HashSet<String>();
        double minTableSize = Double.MAX_VALUE;
        for (int i = 0; i < tableStats.size(); i += 2) {
            
            double firstTableCount = tableStats.get(i);
            if (seenTables.contains(tableNames.get(i))) {
                firstTableCount = Math.min(firstTableCount, minTableSize);
            }

            double secondTableCount = tableStats.get(i+1);
            if (seenTables.contains(tableNames.get(i+1))) {
                secondTableCount = Math.min(secondTableCount, minTableSize);
            }

            double smallerCount = Math.min(firstTableCount, secondTableCount);
            double largerCount = Math.max(firstTableCount, secondTableCount);
            
            result.set(i, largerCount);
            result.set(i+1, smallerCount);

            seenTables.add(tableNames.get(i));
            seenTables.add(tableNames.get(i+1));

            minTableSize = Math.min(smallerCount, minTableSize);
        }

        int offset = tableStats.size();
        HashSet<String> seenColumns = new HashSet<String>();
        double minColumnCount = Double.MAX_VALUE;
        for (int i = 0; i < columnStats.size(); i += 2) {
            
            double firstColumnCount = columnStats.get(i);
            if (seenColumns.contains(columnNames.get(i))) {
                firstColumnCount = Math.min(firstColumnCount, minColumnCount);
            }

            double secondColumnCount = columnStats.get(i+1);
            if (seenColumns.contains(columnNames.get(i+1))) {
                secondColumnCount = Math.min(secondColumnCount, minColumnCount);
            }

            double smallerCount = Math.min(firstColumnCount, secondColumnCount);
            double largerCount = Math.max(firstColumnCount, secondColumnCount);
            
            result.set(i + offset, largerCount);
            result.set(i + offset + 1, smallerCount);

            seenColumns.add(columnNames.get(i));
            seenColumns.add(columnNames.get(i+1));

            minColumnCount = Math.min(smallerCount, minColumnCount);
        }

        return result;
    }


}
