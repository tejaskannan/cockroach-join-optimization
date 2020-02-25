package bandits;

import java.util.Random;
import java.util.ArrayList;
import java.util.List;

import org.la4j.Matrix;
import org.la4j.Vector;
import org.la4j.matrix.dense.Basic2DMatrix;

import utils.Utils;


public class EXP4Optimizer extends BanditOptimizer {

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

        if (!super.shouldUpdate(type)) {
            super.recordSample(reward, type);
            return;
        }

        // Normalize reward based on the type
        double normalizedReward = super.normalizeReward(reward, type);

        // Stack contexts into a matrix (K x M)
        Matrix contextMatrix = this.stackContexts(contexts);
        Utils.normalizeColumns(contextMatrix);

        // Form distribution (K x 1)
        Vector weightVector = Vector.fromArray(this.weights);
        Vector distribution = contextMatrix.multiply(weightVector);

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
        for (int i = 0; i < this.weights.length; i++) {
            expSum += Math.exp(this.nu * expertRewards.get(i)) * this.weights[i];
        }

        double expWeight;
        for (int i = 0; i < this.weights.length; i++) {
            expWeight = Math.exp(this.nu * expertRewards.get(i)) * this.weights[i];
            this.weights[i] = expWeight / expSum;
        }

        // Record sample for better future normalization
        super.recordSample(reward, type);
    }

    @Override
    public int getArm(int time, int type, List<Vector> contexts, boolean shouldExploit) {

        if (!shouldExploit && super.shouldActGreedy(type)) {
            return this.rand.nextInt(this.getNumArms());
        }
        
        // Stack contexts into a matrix (K x M)
        Matrix contextMatrix = this.stackContexts(contexts);
        Utils.normalizeColumns(contextMatrix);

        // Form distribution (K x 1)
        Vector weightVector = Vector.fromArray(this.weights);
        Vector distribution = contextMatrix.multiply(weightVector);

        int arm = 0;
        if (shouldExploit) {
            double maxElem = -Double.MAX_VALUE;
            for (int i = 0; i < distribution.length(); i++) {
                if (distribution.get(i) > maxElem) {
                    maxElem = distribution.get(i);
                    arm = i;
                }
            }
        } else {
            // Sample from the distribution
            arm = Utils.sampleDistribution(distribution, this.rand);
        }

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
