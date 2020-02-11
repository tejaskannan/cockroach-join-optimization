package parsing;

import java.io.StringReader;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.HashMap;

import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.Statement;


public class SQLParser {

    private String[] joins;
    private Pattern innerJoinPattern;

    public SQLParser() {
        this.innerJoinPattern = Pattern.compile(".*(INNER JOIN).*");
        this.joins = new String[]{ "MERGE", "HASH" };
    }

    public String whereToInnerJoin(String sql) {
        try {
            CCJSqlParserManager pm = new CCJSqlParserManager();
            Statement statement = pm.parse(new StringReader(sql));
            if (statement instanceof Select) {
                Select selectStatement = (Select) statement;
                InnerJoinVisitor tablesNamesFinder = new InnerJoinVisitor();
                List<String> tableList = tablesNamesFinder.getTableList(selectStatement);
                for (String tableName : tableList) {
                    System.out.println(tableName);
                }
            }
        } catch (JSQLParserException ex) {
            ex.printStackTrace();
            // System.out.printf("Caught error when parsing: %s\n", ex.getMessage());
        }
        return sql;
    }


    public List<String> getJoinOptions(String query) {
        List<String> result = new ArrayList<String>();
        
        int numInnerJoins = this.numInnerJoins(query);
        if (numInnerJoins <= 0) {
            result.add(query);
            return result;
        }

        List<String[]> permutations = new ArrayList<String[]>();

        // TODO: Generalize to more than two options per join
        String[] tokens = query.split(" ");
        for (int i = 0; i < numInnerJoins * 2; i++) {

            StringBuilder queryBuilder = new StringBuilder();
            int k = 0;
            int joinCount = 0;
            while (k < tokens.length) {
                queryBuilder.append(tokens[k]);
                queryBuilder.append(" ");
                if (k < tokens.length - 1 && tokens[k].equals("INNER") && tokens[k+1].equals("JOIN")) {
                    int index = (i >> joinCount) & 1;
                    queryBuilder.append(this.joins[index]);
                    queryBuilder.append(" ");
                    joinCount += 1;
                }
                k += 1;
            }

            result.add(queryBuilder.toString());
        }

        return result;
    }

    public int numInnerJoins(String query) {
        String[] tokens = query.split(" ");
        int count = 0;
        for (int i = 0; i < tokens.length - 1; i++) {
            if (tokens[i].equals("INNER") && tokens[i+1].equals("JOIN")) {
                count += 1;
            }
        }
        return count;
    }

}
