package bandits;


public class OptimizerFactory {

    public static BanditOptimizer banditFactory(String name, int numArms, int numTypes, int numFeatures, double rewardEpsilon, double rewardAnneal, int updateThreshold, double... args) {
        name = name.toLowerCase();

        if (name.equals("epsilon_greedy")) {
            double epsilon = args[0];
            return new EpsilonGreedyOptimizer(epsilon, numArms, numTypes, rewardEpsilon, rewardAnneal, updateThreshold);
        } else if (name.equals("ucb")) {
            return new UCBOptimizer(numArms, numTypes, rewardEpsilon, rewardAnneal, updateThreshold);
        } else if (name.equals("random")) {
            return new RandomOptimizer(numArms, numTypes);
        } else if (name.equals("linear_thompson")) {
            int d = (int) args[0];
            double r = args[1];
            double delta = args[2];
            return new LinearThompsonSamplingOptimizer(numArms, numTypes, rewardEpsilon, rewardAnneal, updateThreshold, d, r, delta);
        } else if (name.equals("exp4")) {
            double nu = args[0];
            double gamma = args[1];
            return new EXP4Optimizer(numArms, numTypes, numFeatures, rewardEpsilon, rewardAnneal, updateThreshold, nu, gamma);
        } else if (name.equals("linucb")) {
            double alpha = args[0];
            double lambda = args[1];
            return new LinearUCBOptimizer(numArms, numTypes, numFeatures, rewardEpsilon, rewardAnneal, updateThreshold, alpha, lambda);
        }
        System.out.printf("No optimizer with name %s\n", name);
        return null;
    }

    public static BanditOptimizer loadBandit(Object serialized, String fileName) {
        fileName = fileName.toLowerCase();

        if (fileName.startsWith("epsilongreedy")) {
            return (EpsilonGreedyOptimizer) serialized;
        } else if (fileName.startsWith("ucb")) {
            return (UCBOptimizer) serialized;
        } else if (fileName.startsWith("random")) {
            return (RandomOptimizer) serialized;
        } else if (fileName.startsWith("exp4")) {
            return (EXP4Optimizer) serialized;
        } else if (fileName.startsWith("linucb")) {
            return (LinearUCBOptimizer) serialized;
        }

        System.out.printf("Could not parse optimizer from file: %s\n", fileName);
        return null;
    }


}
