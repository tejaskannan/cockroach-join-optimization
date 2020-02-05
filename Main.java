
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import database.SQLDatabase;


public class Main {

    public static void main(String[] args) {
     
        System.out.println("Starting Join Optimizer Interface to Cockroach DB...");
        Scanner inputReader = new Scanner(System.in);

        

        SQLDatabase db = null;
        boolean run = true;
        while (run) {
            System.out.print("> ");
            String line = inputReader.nextLine().trim();
            
            String[] tokens = line.split(" ");
            if (tokens.length == 0) {
                continue;  // Skip empty lines
            }

            String cmd = tokens[0].toUpperCase();

            if (cmd.equals("EXIT") || cmd.equals("QUIT")) {
                run = false;
            } else if (cmd.equals("CONNECT")) {
                if (tokens.length < 3) {
                    System.out.println("Must provide both database name and user name.");
                } else {                
                    String dbName = tokens[1].trim();
                    String userName = tokens[2].trim();  // TODO: Remove any trailing semicolons
                    db = new SQLDatabase("localhost", 26257, dbName, userName);
                    db.refreshStats();
                    System.out.println("Connected to database");
                }
            } else if (cmd.equals("SELECT")) {
                if (db == null) {
                    System.out.println("Not connected to a database.");
                } else {
                    db.select(line);
                }
            } else if (cmd.equals("DISCONNECT")) {
                db = null;
                System.out.println("Disconnected from database");
            } else if (cmd.equals("CREATE-TABLES")) {
                if (db == null) {
                    System.out.println("Not connected to a database.");
                } else if (tokens.length < 2) {
                    System.out.println("Must provide a schema text file.");
                } else {
                    String tablesPath = tokens[1];  // Path to 'txt' file containing CREATE TABLE commands.
                    int numCreated = db.createTables(tablesPath);
                    System.out.printf("Created %d tables.\n", numCreated);
                }
            } else if (cmd.equals("IMPORT")) {
                if (db == null) {
                    System.out.println("Not connected to a database.");
                } else if (tokens.length < 3) {
                    System.out.println("Must provide a table name and CSV file path");
                }
                String tableName = tokens[1].trim();
                String csvPath = tokens[2].trim();
                int wereInserted = db.importCsv(tableName, csvPath);
                System.out.printf("Inserted %d records into %s\n", wereInserted, tableName);
            }
            else {
                System.out.printf("Unknown command %s\n", tokens[0]);
            }
        }
    }
}

