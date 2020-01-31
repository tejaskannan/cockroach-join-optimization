package bandits;

import java.util.Random;


public class EpsilonGreedyOptimizer extends BanditOptimizer {

    private double epsilon;
    private Random rand;

    public EpsilonGreedyOptimizer(double epsilon, int numArms) {
        super(numArms);

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
    public void update(int arm, double reward) {
        super.addReward(arm, reward);
        super.incrementCount(arm);
    }

    @Override
    public int getArm(int time) {
        if (Math.random() < this.epsilon) {
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
