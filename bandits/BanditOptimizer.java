package bandits;


import java.util.List;
import org.la4j.Vector;


public abstract class BanditOptimizer {

    private double[] rewards;
    private int[] counts;
    private int numArms;
    private double[] typeMax;
    private int numTypes;
    private RewardDistribution[] rewardDistributions;
    private String name;

    public BanditOptimizer(int numArms, int numTypes, String name) {
        this.rewards = new double[numArms];
        this.counts = new int[numArms];
        this.numArms = numArms;
        this.numTypes = numTypes;
        this.typeMax = new double[numTypes];
        this.name = name;

        this.rewardDistributions = new RewardDistribution[numTypes];
        for (int i = 0; i < numTypes; i++) {
            this.rewardDistributions[i] = new RewardDistribution(0.1, 0.9, 5);
        }
    }

    public String getName() {
        return this.name;
    }

    public boolean shouldUpdate(int type) {
        return this.rewardDistributions[type].shouldUpdate();
    }

    public void recordSample(double reward, int type) {
        this.rewardDistributions[type].addSample(reward);
    }

    public boolean shouldActGreedy(int type)  {
        return this.rewardDistributions[type].shouldActGreedy();
    }

    public double normalizeReward(double reward, int type) {
        return this.rewardDistributions[type].getReward(reward);
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

    public abstract int getArm(int time, int type, List<Vector> contexts);
}
