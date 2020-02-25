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

        for (Statistics stats : statsIter) {
            // Save results for each table and column
            tableStats.add(stats.getTableRows());
            columnStats.add(stats.getTableDistinct());
        }

        // Normalize results and save into a single vector
        Vector result = new BasicVector(tableStats.size() + columnStats.size());

        for (int i = 0; i < tableStats.size(); i += 2) {
            // Consistently order table statistics because Cockroach does not
            // care about pairwise orderings
            if (tableStats.get(i) >= tableStats.get(i+1)) {
                result.set(i, tableStats.get(i));
                result.set(i+1, tableStats.get(i+1));
            } else {
                result.set(i, tableStats.get(i+1));
                result.set(i+1, tableStats.get(i));
            }
        }

        int offset = tableStats.size();
        for (int i = 0; i < columnStats.size(); i += 2) {
            if (columnStats.get(i) >= columnStats.get(i+1)) {
                result.set(i + offset, columnStats.get(i));
                result.set(i + offset + 1, columnStats.get(i+1));
            } else {
                result.set(i + offset, columnStats.get(i+1));
                result.set(i + offset + 1, columnStats.get(i));
            }
        }

        return result;
    }


}
