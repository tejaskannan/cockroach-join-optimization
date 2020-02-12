package bandits;


import java.util.List;
import org.la4j.Vector;


public abstract class BanditOptimizer {

    private double[] rewards;
    private int[] counts;
    private int numArms;
    private double[] typeMax;
    private int numTypes;

    public BanditOptimizer(int numArms, int numTypes) {
        this.rewards = new double[numArms];
        this.counts = new int[numArms];
        this.numArms = numArms;
        this.numTypes = numTypes;
        this.typeMax = new double[numTypes];
    }

    public double normalizeReward(int type, double reward) {
        double absReward = Math.abs(reward);
        if (absReward > typeMax[type]) {
            typeMax[type] = absReward;
        }

        if (typeMax[type] < 1e-7) {
            return 0.0;
        }
        double normalized = reward / typeMax[type];
        return normalized;
    }

    public int getNumArms() {
        return this.numArms;
    }

    public void addReward(int arm, double reward) {
        this.rewards[arm] += reward;
    }

    public void incrementCount(int arm) {
        this.counts[arm] += 1;
    }

    public double getReward(int arm) {
        return this.rewards[arm];
    }

    public int getCount(int arm) {
        return this.counts[arm];   
    }
    
    public void update(int arm, int type, double reward, Vector context) { }

    public abstract int getArm(int time, List<Vector> contexts);
}
