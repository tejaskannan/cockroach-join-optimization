
import org.postgresql.ds.PGSimpleDataSource;
import database.SQLTable;
import utils.Utils;
import parsing.SQLParser;
import bandits.BanditOptimizer;
import bandits.EpsilonGreedyOptimizer;

import java.util.List;
import java.util.ArrayList;



public class Main {

    public static void main(String[] args) {
        
        // Configure the connection
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setServerName("localhost");
        ds.setPortNumber(26257);
        ds.setDatabaseName("soccer");
        ds.setUser("maxroach");
        ds.setPassword(null);
        ds.setSsl(false);
        ds.setApplicationName("BasicExample");
        
        SQLTable table = new SQLTable(ds);
        table.setDebug(false);

        String[] tables = new String[] { "match", "player", "country" };
        SQLParser parser = new SQLParser(tables);

        String query = "SELECT * FROM match as m INNER JOIN player as p ON m.home_player_1 = p.player_api_id INNER JOIN country as c ON c.id = m.country_id;";
        List<String> joinOptions = parser.getJoinOptions(query);

        int numArms = joinOptions.size();
        BanditOptimizer optimizer = new EpsilonGreedyOptimizer(0.1, numArms);

        long[] queryTimes = new long[joinOptions.size()];
        int[] counts = new int[joinOptions.size()];
        long queryTime = 0;
        int trials = 15;
        for (int i = 0; i < trials; i++) {
            long start = System.currentTimeMillis();
            int option = optimizer.getArm(i);
            String q = joinOptions.get(option);
            table.execute(q);
            if (i > 0) {
                long elapsed = System.currentTimeMillis() - start;
                optimizer.update(option, -1 * elapsed);
                queryTimes[option] += elapsed;
                queryTime += elapsed;
                counts[option] += 1;
            }
        }

        System.out.printf("Overall average time: %.4f\n", ((double) queryTime) / ((double) (trials - 1)));
        System.out.println("===========");
        for (int i = 0; i < joinOptions.size(); i++) {
            System.out.printf("Query %d average time: %.4f\n", i, ((double) (queryTimes[i])) / ((double) (counts[i])));
        }

    }
}

