package parsing;


public class TableJoin {

    private JoinEntry leftTable;
    private JoinEntry rightTable;

    public TableJoin(String leftTableName, String leftTableAttribute, String rightTableName, String rightTableAttribute) {
        this.leftTable = new JoinEntry(leftTableName, leftTableAttribute);
        this.rightTable = new JoinEntry(rightTableName, rightTableAttribute);
    }

    public TableJoin(JoinEntry leftTable, JoinEntry rightTable) {
        this.leftTable = leftTable;
        this.rightTable = rightTable;
    }

    public JoinEntry getLeft() {
        return this.leftTable;
    }

    public JoinEntry getRight() {
        return this.rightTable;
    }
}


