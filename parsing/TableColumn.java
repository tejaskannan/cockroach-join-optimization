package parsing;


public class TableColumn {

    private String tableName;
    private String columnName;
    
    public TableColumn(String tableName, String columnName) {
        this.tableName = tableName;
        this.columnName = columnName;
    }

    public String getTableName() {
        return this.tableName;
    }

    public String getColumnName() {
        return this.columnName;
    }

    @Override
    public String toString() {
        return String.format("%s -> %s", this.getTableName(), this.getColumnName());
    }

}

