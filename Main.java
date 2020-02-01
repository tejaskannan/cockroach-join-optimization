
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import database.SQLDatabase;


public class Main {

    public static void main(String[] args) {
     
        System.out.println("Starting Join Optimizer Interface to Cockroach DB...");
        Scanner inputReader = new Scanner(System.in);

        

        SQLDatabase db = null;
        StringBuilder cmd = new StringBuilder();
        boolean run = true;
        while (run) {
            System.out.print("> ");
            String line = inputReader.nextLine().trim();
            
            String[] tokens = line.split(" ");

            if (line.equals("exit") || line.equals("quit")) {
                run = false;
            } else if (tokens[0].equals("CONNECT")) {
                db = new SQLDatabase("localhost", 26257, tokens[1], tokens[2]);
                db.refreshStats();
                System.out.println("Connected to database");
            } else if (tokens[0].equals("SELECT")) {
                if (db == null) {
                    System.out.println("Not connected to a database.");
                } else {
                    db.select(line);
                }
            } else if (tokens[0].equals("DISCONNECT")) {
                db = null;
                System.out.println("Disconnected from database");
            } else {
                System.out.printf("Unknown command %s\n", tokens[0]);
            }
        }
    }
}

