package bandits;

import java.util.Random;
import java.util.List;
import org.la4j.Vector;


public class EpsilonGreedyOptimizer extends BanditOptimizer {

    private double epsilon;
    private Random rand;
    private static final double ANNEAL_RATE = 0.95;

    public EpsilonGreedyOptimizer(double epsilon, int numArms, int numTypes, double rewardEpsilon, double rewardAnneal, int updateThreshold) {
        super(numArms, numTypes, rewardEpsilon, rewardAnneal, updateThreshold, String.format("EpsilonGreedy-%4.3f", epsilon));

        this.rand = new Random();
        if (epsilon < 0.0) {
            this.epsilon = 0.0;
        } else if (epsilon > 1.0) {
            this.epsilon = 1.0;
        } else {
            this.epsilon = epsilon;
        }
    }

    @Override
    public void update(int arm, int type, double reward, List<Vector> contexts) {
        this.epsilon = this.epsilon * ANNEAL_RATE;

        super.recordSample(reward, arm, type);

        if (super.shouldUpdate(arm, type)) {
            double normalizedReward = super.normalizeReward(reward, type);
            super.addReward(arm, normalizedReward);
            super.incrementCount(arm);
        }
    }

    @Override
    public int getArm(int time, int type, List<Vector> contexts, boolean shouldExploit) {
        if (!shouldExploit && (Math.random() < this.epsilon)) {
            return this.rand.nextInt(super.getNumArms());
        }
     
        // Get arm with the highest average reward
        int maxArm = 0;
        double maxAvg = super.getReward(0) / (((double) super.getCount(0)) + 1e-7);
        for (int a = 1; a < this.getNumArms(); a++) {
            double avg = super.getReward(a) / (((double) super.getCount(a)) + 1e-7);
            if (avg > maxAvg) {
                maxArm = a;
                maxAvg = avg;
            }
        }
        return maxArm;
    }
}
