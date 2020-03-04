package bandits;


import java.util.Random;
import java.util.List;
import org.la4j.Vector;


public class UCBOptimizer extends BanditOptimizer {

    private Random rand;

    public UCBOptimizer(int numArms, int numTypes, double rewardEpsilon, double rewardAnneal, int updateThreshold) {
        super(numArms, numTypes, rewardEpsilon, rewardAnneal, updateThreshold, "UCB");
        this.rand = new Random();
    }

    @Override
    public void update(int arm, int type, double reward, List<Vector> contexts) {
        super.recordSample(reward, arm, type);

        if (super.shouldUpdate(arm, type)) {
            double normalizedReward = super.normalizeReward(reward, type);
            super.addReward(arm, normalizedReward);
            super.incrementCount(arm);
        }
    }

    @Override
    public int getArm(int time, int type, List<Vector> contexts, boolean shouldExploit) {
        int maxArm = -1;
        double maxScore = -Double.MAX_VALUE;

        for (int a = 0; a < this.getNumArms(); a++) {
            double count = ((double) super.getCount(a)) + 1e-7;
            double radius = Math.sqrt(((double) 2 * time) / count);
            double avg = super.getReward(a) / count;
            double score = avg + radius;

            if (score > maxScore) {
                maxArm = a;
                maxScore = score;
            }
        }

        return maxArm;
    }

}
