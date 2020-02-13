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
        } else if (name.equals("linear_thompson")) {
            int d = (int) args[0];
            double r = args[1];
            double delta = args[2];
            return new LinearThompsonSamplingOptimizer(numArms, numTypes, d, r, delta);
        }
        System.out.printf("No optimizer with name %s\n", name);
        return null;
    }
}
