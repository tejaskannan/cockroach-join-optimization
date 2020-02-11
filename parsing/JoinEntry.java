package parsing;


public class JoinEntry {

    private String table;
    private String attribute;

    public JoinEntry(String table, String attribute) {
        this.table = table;
        this.attribute = attribute;
    }

    public String getTable() {
        return this.table;
    }

    public String getAttribute() {
        return this.attribute;
    }
}
