package parsing;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;

import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.expression.operators.arithmetic.*;
import net.sf.jsqlparser.expression.operators.conditional.*;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.schema.Column;


public class InnerJoinVisitor implements SelectVisitor, FromItemVisitor, ExpressionVisitor, ItemsListVisitor {
	
    private List<String> tables;
    private List<String> aliases;
    private List<TableJoin> joins;


	public List<String> getTableList(Select select) {
		tables = new ArrayList<String>();
        aliases = new ArrayList<String>();
        joins = new ArrayList<TableJoin>();
		select.getSelectBody().accept(this);
		return tables;
	}

    @Override
	public void visit(PlainSelect plainSelect) {
		plainSelect.getFromItem().accept(this);
		
		if (plainSelect.getJoins() != null) {
			for (Iterator joinsIt = plainSelect.getJoins().iterator(); joinsIt.hasNext();) {
				Join join = (Join) joinsIt.next();
				join.getRightItem().accept(this);
			}
		}

		if (plainSelect.getWhere() != null) {
			plainSelect.getWhere().accept(this);
        }
	}

    @Override
	public void visit(Union union) {
		for (Iterator iter = union.getPlainSelects().iterator(); iter.hasNext();) {
			PlainSelect plainSelect = (PlainSelect) iter.next();
			visit(plainSelect);
		}
	}

    @Override
	public void visit(Table tableName) {
		String tableWholeName = tableName.getWholeTableName();
		String alias = tableName.getAlias();

        tables.add(tableWholeName);
        if (alias != null && alias.length() > 0) {
            aliases.add(alias);
        }
	}

    @Override
	public void visit(SubSelect subSelect) {
		subSelect.getSelectBody().accept(this);
	}

    @Override
	public void visit(SubJoin subjoin) {
        subjoin.getLeft().accept(this);
		subjoin.getJoin().getRightItem().accept(this);
	}

    @Override
    public void visit(Addition addition) {
		visitBinaryExpression(addition);
	}

    @Override
	public void visit(AndExpression andExpression) {
		visitBinaryExpression(andExpression);
	}

    @Override
	public void visit(Between between) {
		between.getLeftExpression().accept(this);
		between.getBetweenExpressionStart().accept(this);
		between.getBetweenExpressionEnd().accept(this);
	}

    @Override
	public void visit(Column tableColumn) { }

    @Override
	public void visit(Division division) {
		visitBinaryExpression(division);
	}

    @Override
	public void visit(DoubleValue doubleValue) { }

    @Override
	public void visit(EqualsTo equalsTo) {
        Expression left = equalsTo.getLeftExpression();
        Expression right = equalsTo.getRightExpression();

		if (left instanceof Column && right instanceof Column) {
            TableJoin join = new TableJoin((Column) left, (Column) right);
            System.out.println(join);
            joins.add(join);
        } else {
            visitBinaryExpression(equalsTo);
       }
	}

    @Override
	public void visit(Function function) { }

    @Override
	public void visit(GreaterThan greaterThan) {
		visitBinaryExpression(greaterThan);
	}

    @Override
	public void visit(GreaterThanEquals greaterThanEquals) {
		visitBinaryExpression(greaterThanEquals);
	}

	public void visit(InExpression inExpression) {
		inExpression.getLeftExpression().accept(this);
		inExpression.getItemsList().accept(this);
	}

    @Override
	public void visit(InverseExpression inverseExpression) {
		inverseExpression.getExpression().accept(this);
	}

    @Override
	public void visit(IsNullExpression isNullExpression) { }

    @Override
	public void visit(JdbcParameter jdbcParameter) { }

    @Override
	public void visit(LikeExpression likeExpression) {
		visitBinaryExpression(likeExpression);
	}

    @Override
	public void visit(ExistsExpression existsExpression) {
		existsExpression.getRightExpression().accept(this);
	}

    @Override
	public void visit(LongValue longValue) { }

    @Override
	public void visit(MinorThan minorThan) {
		visitBinaryExpression(minorThan);
	}

    @Override
	public void visit(MinorThanEquals minorThanEquals) {
		visitBinaryExpression(minorThanEquals);
	}

    @Override
	public void visit(Multiplication multiplication) {
		visitBinaryExpression(multiplication);
	}

    @Override
	public void visit(NotEqualsTo notEqualsTo) {
		visitBinaryExpression(notEqualsTo);
	}

    @Override
	public void visit(NullValue nullValue) { }

    @Override
	public void visit(OrExpression orExpression) {
		visitBinaryExpression(orExpression);
	}

    @Override
	public void visit(Parenthesis parenthesis) {
		parenthesis.getExpression().accept(this);
    }

    @Override
	public void visit(StringValue stringValue) { }

    @Override
	public void visit(Subtraction subtraction) {
		visitBinaryExpression(subtraction);
	}

    @Override
    public void visit(Matches match) { }

    @Override
    public void visit(Concat concat) { }

    @Override
    public void visit(AnyComparisonExpression anyExpression) { }

    @Override
    public void visit(AllComparisonExpression allExpression) { }

    @Override
    public void visit(BitwiseXor xorExpression) {
        visitBinaryExpression(xorExpression);
    }

    @Override
    public void visit(BitwiseAnd andExpression) {
        visitBinaryExpression(andExpression);
    }
    
    @Override
    public void visit(BitwiseOr orExpression) {
        visitBinaryExpression(orExpression);
    }
	
    public void visitBinaryExpression(BinaryExpression binaryExpression) {
        binaryExpression.getLeftExpression().accept(this);
		binaryExpression.getRightExpression().accept(this);
	}

    @Override
	public void visit(ExpressionList expressionList) {
		for (Iterator iter = expressionList.getExpressions().iterator(); iter.hasNext();) {
			Expression expression = (Expression) iter.next();
			expression.accept(this);
		}
	}

    @Override
	public void visit(DateValue dateValue) { }

    @Override
	public void visit(TimestampValue timestampValue) { }
	
    @Override
	public void visit(TimeValue timeValue) { }

    @Override
	public void visit(CaseExpression caseExpression) { }

    @Override
	public void visit(WhenClause whenClause) { }

}

