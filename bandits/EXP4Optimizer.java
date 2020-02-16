package bandits;


import java.util.List;

import org.la4j.Matrix;
import org.la4j.Vector;
import org.la4j.matrix.dense.Basic2DMatrix;

import utils.Utils;


public class EXP4Optimizer extends BanditOptimizer {

    private double nu;
    private double gamma;
    private int numExperts;
    private Vector weights;

    public EXP4Optimizer(int numArms, int numTypes, int numExperts, double nu, double gamma) {
        super(numArms, numTypes, String.format("EXP4-%4.3f-%4.3f", nu, gamma));

        this.nu = nu;
        this.gamma = gamma;
        this.numExperts = numExperts;

        // Initialize weights to uniform distribution
        double[] weightsArray = new double[numExperts];
        for (int i = 0; i < numExperts; i++) {
            weightsArray[i] = 1.0 / ((double) numExperts);
        }
        this.weights = Vector.fromArray(weightsArray);
    }

    @Override
    public void update(int arm, int type, double reward, List<Vector> contexts) {

        // Normalize reward based on the type
        double normalizedReward = this.normalizeReward(reward, type);

        // Stack contexts into a matrix (K x M)
        Matrix contextMatrix = this.stackContexts(contexts);

        // Form distribution (K x 1)
        Vector contextProduct = contextMatrix.multiply(this.weights);
        Vector distribution = Utils.normalizeVector(contextProduct);

        // Estimate Action Rewards (K x 1)
        double[] actionRewardsArray = new double[distribution.length()];
        for (int i = 0; i < distribution.length(); i++) {
            if (i == arm) {
                double rewardFactor = 1.0 / (distribution.get(i) + this.gamma);
                actionRewardsArray[i] = 1.0 - rewardFactor * (1.0 - normalizedReward);
            } else {
                actionRewardsArray[i] = 1.0;
            }
        }

        // Compute expert rewards
        Vector actionRewards = Vector.fromArray(actionRewardsArray);  // (K x 1)
        Vector expertRewards = contextMatrix.transpose().multiply(actionRewards);  // (M x 1)

        // Update weight distribution
        double expSum = 0.0;
        for (int i = 0; i < this.weights.length(); i++) {
            expSum += Math.exp(this.nu * expertRewards.get(i)) * this.weights.get(i);
        }

        double expWeight;
        for (int i = 0; i < this.weights.length(); i++) {
            expWeight = Math.exp(this.nu * expertRewards.get(i)) * this.weights.get(i);
            this.weights.set(i, expWeight / expSum);
        }

        System.out.println(this.weights);

        // Record sample for better future normalization
        super.recordSample(reward, type);
    }

    @Override
    public int getArm(int time, int type, List<Vector> contexts) {
        // Stack contexts into a matrix (K x M)
        Matrix contextMatrix = this.stackContexts(contexts);

        // Form distribution (K x 1)
        Vector contextProduct = contextMatrix.multiply(this.weights);
        Vector distribution = Utils.normalizeVector(contextProduct);

        // Sample from the distribution to get the right arm
        int arm = Utils.sampleDistribution(distribution);

        return arm;
    }

    private Matrix stackContexts(List<Vector> contexts) {
        int numRows = contexts.size();
        int numCols = contexts.get(0).length();
        Matrix contextMatrix = new Basic2DMatrix(numRows, numCols);
        for (int i = 0; i < numRows; i++) {
            contextMatrix.setRow(i, contexts.get(i));
        }
    
        return contextMatrix;
    }

}
