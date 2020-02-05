package parsing;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.HashMap;


public class SQLParser {

    private String[] joins;
    private Pattern innerJoinPattern;

    public SQLParser() {
        this.innerJoinPattern = Pattern.compile(".*(INNER JOIN).*");
        this.joins = new String[]{ "MERGE", "HASH" };
    }

    public String whereToInnerJoin(String query) {
        String[] tokens = query.split(" ");
        StringBuilder joinBuilder = new StringBuilder();

        int index = 0;
        while (index < tokens.length && !tokens[index].equals("FROM")) {
            joinBuilder.append(tokens[index]);
            joinBuilder.append(" ");
            index += 1; 
        }

        // There is no FROM clause
        if (index == tokens.length) {
            return query;
        }

        joinBuilder.append("FROM ");
        index += 1;

        // Get tables and aliases
        HashMap<String, String> tables = new HashMap<String, String>();
        ArrayList<String> aliases = new ArrayList<String>();
        while (index < tokens.length && !tokens[index].equals("WHERE")) {
            StringBuilder tableBuilder = new StringBuilder();

            if (tokens[index+1].equals("AS")) {
                String last = tokens[index + 2];
                if (last.endsWith(",") || last.endsWith(";")) {
                    last = last.substring(0, last.length() - 1);
                }
                tableBuilder.append(tokens[index] + " " + tokens[index + 1] + " " + last);
                index += 2;
            } else {
                String t = tokens[index];
                if (t.endsWith(",")) {
                    t = t.substring(0, t.length() - 1);
                }
                tableBuilder.append(t);
            }

            aliases.add(tokens[index]);
            tables.put(tokens[index], tableBuilder.toString());
            index += 1;
        }

        if (index == tokens.length) {
            return query;
        }

        int numClauses = tables.size() - 1;
        int count = 0;
        for (String name : aliases) {
            String t = tables.get(name);
            joinBuilder.append(tables.get(name));
            if (count < numClauses) {
                joinBuilder.append(" INNER JOIN ");
            }
            count += 1;
        }
        joinBuilder.append(";");
        
        return joinBuilder.toString();
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
