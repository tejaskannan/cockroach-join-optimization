package bandits;


public class OptimizerFactory {

    public static BanditOptimizer banditFactory(String name, int numArms, int numTypes, double... args) {
        name = name.toLowerCase();

        if (name.equals("epsilon_greedy")) {
            double epsilon = args[0];
            return new EpsilonGreedyOptimizer(epsilon, numArms, numTypes);
        } else if (name.equals("ucb")) {
            return new UCBOptimizer(numArms, numTypes);
        } else if (name.equals("random")) {
            return new RandomOptimizer(numArms, numTypes);
        }
        System.out.printf("No optimizer with name %s\n", name);
        return null;
    }
}
