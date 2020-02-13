package bandits;


import java.util.Random;
import java.util.List;
import org.la4j.Vector;


public class UCBOptimizer extends BanditOptimizer {

    private Random rand;

    public UCBOptimizer(int numArms, int numTypes) {
        super(numArms, numTypes);
        this.rand = new Random();
    }

    @Override
    public void update(int arm, int type, double reward, Vector context) {
        if (super.shouldUpdate(type)) {
            double normalizedReward = super.normalizeReward(reward, type);
            super.addReward(arm, normalizedReward);
            super.incrementCount(arm);
        }
        super.recordSample(reward, type);
    }

    @Override
    public int getArm(int time, int type, List<Vector> contexts) {

        if (super.shouldActGreedy(type)) {
            return this.rand.nextInt(this.getNumArms());
        }

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
