package database;


public class Statistics {

    private String tableName;
    private String columnName;
    private int numRows;
    private int numDistinct;
    private double[] features;

    public Statistics(String tableName, String columnName, int numRows, int numDistinct) {
        this.tableName = tableName;
        this.columnName = columnName;
        this.numRows = numRows;
        this.numDistinct = numDistinct;
        this.features = new double[]{ (double) numRows, (double) numDistinct};
    }

    public double[] getFeatures() {
        return this.features;
    }

    public String toString() {
        return String.format("Table: %s, Column: %s, # Rows: %d, # Distinct: %d", this.tableName, this.columnName, this.numRows, this.numDistinct);
    }

}
