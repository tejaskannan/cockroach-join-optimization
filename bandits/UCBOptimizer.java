package bandits;

import java.util.List;
import org.la4j.Vector;


public class UCBOptimizer extends BanditOptimizer {

    public UCBOptimizer(int numArms, int numTypes) {
        super(numArms, numTypes);
    }

    @Override
    public void update(int arm, int type, double reward, Vector context) {
        reward = super.normalizeReward(type, reward);
        super.addReward(arm, reward);
        super.incrementCount(arm);
    }

    @Override
    public int getArm(int time, List<Vector> contexts) {

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
