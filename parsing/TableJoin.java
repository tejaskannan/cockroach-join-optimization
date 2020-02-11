package parsing;

import net.sf.jsqlparser.schema.Column;

public class TableJoin {

    private Column left;
    private Column right;

    public TableJoin(Column left, Column right) {
        this.left = left;
        this.right = right;
    }

    public Column getLeft() {
        return this.left;
    }

    public Column getRight() {
        return this.right;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        
        builder.append(this.getLeft().getTable().getWholeTableName());
        if (this.getLeft().getTable().getAlias() != null && this.getLeft().getTable().getAlias().length() > 0) {
            builder.append(" AS ");
            builder.append(this.getLeft().getTable().getAlias());
        }

        builder.append(" INNER JOIN ");
        
        builder.append(this.getRight().getTable().getWholeTableName());
        if (this.getRight().getTable().getAlias() != null && this.getRight().getTable().getAlias().length() > 0) {
            builder.append(" AS ");
            builder.append(this.getRight().getTable().getAlias());
        }

        builder.append(" ON ");

        // For now, we only support equi-joins
        builder.append(this.getLeft().getWholeColumnName());
        builder.append(" = ");
        builder.append(this.getRight().getWholeColumnName());

        return builder.toString();
    }
}


