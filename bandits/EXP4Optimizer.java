package bandits;

import java.util.Random;
import java.util.ArrayList;
import java.util.List;

import org.la4j.Matrix;
import org.la4j.Vector;
import org.la4j.matrix.dense.Basic2DMatrix;

import utils.Utils;


public class EXP4Optimizer extends BanditOptimizer {

    private static final double MARGIN = 0.5;
    private double nu;
    private double gamma;
    private int numExperts;
    private double[] weights;
    private Random rand;

    public EXP4Optimizer(int numArms, int numTypes, int numExperts, double rewardEpsilon, double rewardAnneal, int updateThreshold, double nu, double gamma) {
        super(numArms, numTypes, rewardEpsilon, rewardAnneal, updateThreshold, String.format("EXP4-%4.3f-%4.3f", nu, gamma));

        this.nu = nu;
        this.gamma = gamma;
        this.numExperts = numExperts;
        
        this.rand = new Random();

        // Initialize weights to uniform distribution
        this.weights = new double[numExperts];
        for (int i = 0; i < numExperts; i++) {
            this.weights[i] = 1.0 / ((double) numExperts);
        }
    }

    @Override
    public void update(int arm, int type, double reward, List<Vector> contexts) {

        super.recordSample(reward, arm, type);

        if (!super.shouldUpdate(arm, type)) {
            return;
        }

        // Normalize reward based on the type (add 1 to bring into range [0.0, 1.0])
        double normalizedReward = super.normalizeReward(reward, type);

        // System.out.printf("Normalized Reward: %f\n", normalizedReward);

        // Stack contexts into a matrix (K x M)
        Matrix contextMatrix = Utils.stackContexts(contexts);
        Utils.normalizeColumns(contextMatrix);

        // Form distribution (K x 1)
        Vector weightVector = Vector.fromArray(this.weights);
        Vector distribution = contextMatrix.multiply(weightVector);

        // Estimate Action Rewards (K x 1)
        double[] actionRewardsArray = new double[distribution.length()];
        for (int i = 0; i < distribution.length(); i++) {
            if (i == arm) {
                // System.out.println(distribution.get(i));
                actionRewardsArray[i] = (1.0 / (Math.max(distribution.get(i) + this.gamma, 1e-3))) * (normalizedReward + MARGIN);
            } else {
                actionRewardsArray[i] = 0.0;
            }
       }

        // Compute expert rewards
        Vector actionRewards = Vector.fromArray(actionRewardsArray);  // (K x 1)
        Vector expertRewards = contextMatrix.transpose().multiply(actionRewards);  // (M x 1)

        // Update weight distribution
        double expSum = 0.0;
        for (int i = 0; i < this.weights.length; i++) {
            expSum += Math.exp(this.nu * expertRewards.get(i)) * this.weights[i];
        }

        double expWeight;
        for (int i = 0; i < this.weights.length; i++) {
            expWeight = Math.exp(this.nu * expertRewards.get(i)) * this.weights[i];
            this.weights[i] = expWeight / expSum;
        }
    }

    @Override
    public int getArm(int time, int type, List<Vector> contexts, boolean shouldExploit) {
        // Stack contexts into a matrix (K x M)
        Matrix contextMatrix = Utils.stackContexts(contexts);
        Utils.normalizeColumns(contextMatrix);

        // Form distribution (K x 1)
        Vector weightVector = Vector.fromArray(this.weights);
        Vector distribution = contextMatrix.multiply(weightVector);

        int arm = 0;
        if (shouldExploit || super.shouldActGreedy()) {
            arm = Utils.argMax(distribution);
        } else if (super.shouldActRandom(type)) {
            arm = this.rand.nextInt(this.getNumArms());
        } else {
            // Sample from the distribution
            arm = Utils.sampleDistribution(distribution, this.rand);
        }

        // System.out.printf("Type: %d, Arm: %d, Distribution: %s ", type, arm, distribution.toString());

        return arm;
    }
}
