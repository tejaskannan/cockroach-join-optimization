package database;


import java.lang.Iterable;
import java.util.ArrayList;
import java.util.HashSet;

import org.la4j.Vector;
import org.la4j.vector.dense.BasicVector;


public class Statistics {

    private String tableName;
    private String columnName;
    private double tableRows;
    private int tableDistinct;
    private double rowCount;
    private double[] features;

    public Statistics(String tableName, String columnName, int numRows, int numDistinct) {
        this.tableName = tableName;
        this.columnName = columnName;
        this.tableRows = (double) numRows;
        this.tableDistinct = numDistinct;

        // For now, we assume that the distribution is uniform.
        // TODO: Integrate histograms
        this.rowCount = this.tableRows / ((double) this.tableDistinct);

        this.features = new double[]{ this.tableRows, this.rowCount};
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

    public int getTableDistinct() {
        return this.tableDistinct;
    }

    public double getRowCount() {
        return this.rowCount;
    }

    public double[] getFeatures() {
        return this.features;
    }

    public String toString() {
        return String.format("Table: %s, Column: %s, # Rows: %d, # Distinct: %d, # Rows per Value: %s", this.tableName, this.columnName, this.getTableRows(), this.getTableDistinct(), this.getRowCount());
    }

    public static Vector combineStatistics(Iterable<Statistics> statsIter) {
        /**
         * Packages the given statistics into a single context vector.
         * 
         * @param statsIter: Sequence of statistics from involved relations and columns
         * @return A vector containing the normalized statistics
         */
        double maxTableCount = -Double.MAX_VALUE;
        double maxRowCount = -Double.MAX_VALUE;
        
        ArrayList<Double> tableStats = new ArrayList<Double>();
        ArrayList<Double> columnStats = new ArrayList<Double>(); 

        for (Statistics stats : statsIter) {
            // Track maximum values
            if (stats.getTableRows() > maxTableCount) {
                maxTableCount = stats.getTableRows();
            }

            if (stats.getRowCount() > maxRowCount) {
                maxRowCount = stats.getRowCount();
            }

            // Save results for each table and column
            tableStats.add(stats.getTableRows());
            columnStats.add(stats.getRowCount());
        }

        // Normalize results and save into a single vector
        Vector result = new BasicVector(tableStats.size() + columnStats.size());

        for (int i = 0; i < tableStats.size(); i++) {
            result.set(i, tableStats.get(i) / maxTableCount);
        }

        int offset = tableStats.size();
        for (int i = 0; i < columnStats.size(); i++) {
            result.set(i + offset, columnStats.get(i) / maxRowCount);
        }

        return result;
    }


}
