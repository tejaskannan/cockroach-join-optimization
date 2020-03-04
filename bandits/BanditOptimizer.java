package bandits;

import java.io.Serializable;
import java.util.List;
import java.util.Random;
import org.la4j.Vector;


public abstract class BanditOptimizer implements Serializable {

    private double[] rewards;
    private int[] counts;
    private int numArms;
    private double[] typeMax;
    private int numTypes;
    private RewardDistribution[] rewardDistributions;
    private String name;
    private double rewardEpsilon;
    private double rewardAnneal;
    private double originalEpsilon;
    private int updateThreshold;
    private Random rand;

    public BanditOptimizer(int numArms, int numTypes, double rewardEpsilon, double rewardAnneal, int updateThreshold, String name) {
        this.rewards = new double[numArms];
        this.counts = new int[numArms];
        this.numArms = numArms;
        this.numTypes = numTypes;
        this.typeMax = new double[numTypes];
        this.name = name;
        this.rand = new Random();

        this.originalEpsilon = rewardEpsilon;
        this.rewardEpsilon = rewardEpsilon;
        this.rewardAnneal = rewardAnneal;
        this.updateThreshold = updateThreshold;

        // Initialize reward distributions
        this.rewardDistributions = new RewardDistribution[numTypes];
        for (int i = 0; i < numTypes; i++) {
            this.rewardDistributions[i] = new RewardDistribution(numArms, updateThreshold);
        }
    }

    public void addQueryTypes(int numToAdd) {
        if (numToAdd <= 0) {
            return;
        }
        
        // Add new reward distributions
        int newNumTypes = this.getNumTypes() + numToAdd;
        RewardDistribution[] newDistributions = new RewardDistribution[newNumTypes];

        for (int a = 0; a < newNumTypes; a++) {
            if (a < this.getNumTypes()) {
                newDistributions[a] = this.rewardDistributions[a];
            } else {
                newDistributions[a] = new RewardDistribution(this.getNumArms(), this.updateThreshold);
            }
        }

        this.rewardDistributions = newDistributions;
        this.numTypes = newNumTypes;
        
        this.rewardEpsilon = this.originalEpsilon;
    }

    public void reset(int numTypes) {
        /**
         * Resets the bandit optimizer with the new number of query types.
         */

        // Reset rewards and counts
        this.rewards = new double[this.getNumArms()];
        this.counts = new int[this.getNumArms()];

        // Initialize new reward distributions
        this.rewardDistributions = new RewardDistribution[numTypes];
        for (int i = 0; i < numTypes; i++) {
            this.rewardDistributions[i] = new RewardDistribution(this.getNumArms(), updateThreshold);
        }

        this.numTypes = numTypes;
    }

    public String getName() {
        return this.name;
    }

    public boolean shouldUpdate(int arm, int type) {
        return this.rewardDistributions[type].shouldUpdate(arm);
    }

    public void recordSample(double reward, int arm, int type) {
        this.rewardDistributions[type].addSample(reward, arm);
    }

    public boolean shouldActGreedy()  {
        double sample = this.rand.nextDouble();
        
        boolean result = false;
        if (sample < 1.0 - this.rewardEpsilon) {
            result = true;
        }

        this.rewardEpsilon *= this.rewardAnneal;
        return result;
    }

    public double normalizeReward(double reward, int type) {
        // System.out.printf("Type: %d, Reward: %f ", type, reward);
        double normalized = this.rewardDistributions[type].getReward(reward);
        // System.out.printf(" Normalized: %f\n", normalized);
        return normalized;
    }

    public int getNumArms() {
        return this.numArms;
    }

    public int getNumTypes() {
        return this.numTypes;
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
    
    public void update(int arm, int type, double reward, List<Vector> contexts) { }

    public abstract int getArm(int time, int type, List<Vector> contexts, boolean shouldExploit);
}
