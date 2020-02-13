package bandits;


import java.lang.Math;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.la4j.Matrix;
import org.la4j.matrix.DenseMatrix;
import org.la4j.Vector;
import org.la4j.inversion.MatrixInverter;
import org.la4j.inversion.GaussJordanInverter;


public class LinearThompsonSamplingOptimizer extends BanditOptimizer {

    private double r;
    private double delta;
    private int d;
    private Matrix B;
    private Vector unnormalizedMu;
    private RewardDistribution[] rewardDistributions;
    private Random rand;

    public LinearThompsonSamplingOptimizer(int numArms, int numTypes, int d, double delta, double r) {
        super(numArms, numTypes);
        this.d = d;
        this.delta = delta;
        this.r = r;
        
        this.B = DenseMatrix.identity(d);
        this.unnormalizedMu = Vector.zero(d);
        this.rand = new Random();
    }

    private double getVariance(int time) {
        return r * Math.sqrt(9 * ((double) d) * Math.log(((double) time) / delta));
    }

    private Vector getMu(Matrix BInv) {
        return BInv.multiply(this.unnormalizedMu);
    }

    @Override
    public void update(int arm, int type, double reward, Vector context) {
        if (super.shouldUpdate(type)){
            double normalizedReward = super.normalizeReward(reward, type);
            
            System.out.printf("Raw Reward: %s\n", reward);
            System.out.printf("Normalized Reward: %s\n", normalizedReward);
            
            this.unnormalizedMu = this.unnormalizedMu.add(context.multiply(normalizedReward));
            this.B = this.B.add(context.outerProduct(context));
        }
        super.recordSample(reward, type);
    }

    @Override
    public int getArm(int time, int type, List<Vector> contexts) {
        // For now, just assume type zero. We will need to make this a parameter.
        if (super.shouldActGreedy(type)) {
            return this.rand.nextInt(this.getNumArms());
        }

        // Copy mu and B into array form
        double var = this.getVariance(time);
        MatrixInverter inverter = new GaussJordanInverter(this.B);
        Matrix BInv = inverter.inverse();

        Matrix cov = BInv.multiply(var * var);
        double[][] covMatrix = new double[cov.rows()][cov.columns()];
        for (int i = 0; i < cov.rows(); i++) {
            for (int j = 0; j < cov.columns(); j++) {
                covMatrix[i][j] = cov.get(i, j);
            }
        }

        Vector mu = this.getMu(BInv);
        double[] muArray = new double[mu.length()];
        for (int i = 0; i < mu.length(); i++) {
            muArray[i] = mu.get(i);
        }

        // Sample the distribution
        MultivariateNormalDistribution dist = new MultivariateNormalDistribution(muArray, covMatrix);
        double[] sample = dist.sample();
        Vector sampleMu = Vector.fromArray(sample);

        // Argmax over all arms
        int maxArm = -1;
        double maxValue = -Double.MAX_VALUE;
        for (int i = 0; i < contexts.size(); i++) {
            Vector context = contexts.get(i);
            double dotProd = context.innerProduct(sampleMu);
            if (dotProd > maxValue) {
                maxArm = i;
                maxValue = dotProd;
            }
        }

        return maxArm;
    }

}
