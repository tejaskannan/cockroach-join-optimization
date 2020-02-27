package bandits;

import java.io.Serializable;
import java.lang.Math;
import java.util.Random;
import java.util.ArrayList;
import org.apache.commons.math3.distribution.NormalDistribution;


public class RewardDistribution implements Serializable {

    private static final double DEFAULT_MEAN = 0.0;
    private static final double DEFAULT_VAR = 0.01;
    private static final double SMOOTHING_FACTOR = 1.0;

    private int warmUpPeriod;
    private int numArms;

    private ArrayList<Double>[] samples;
    private Random rand;

    public RewardDistribution(int numArms, int warmUpPeriod) {
        this.warmUpPeriod = warmUpPeriod;
        this.numArms = numArms;
        this.rand = new Random();

        // Initialize array of sample lists
        this.samples = new ArrayList[numArms];
        for (int i = 0; i < numArms; i++) {
            this.samples[i] = new ArrayList<Double>();
        }
    }

    public void addSample(double x, int arm) {
        this.samples[arm].add(x);
    }

    public int getWarmUpPeriod() {
        return this.warmUpPeriod;
    }

    public boolean shouldUpdate(int arm) {
        return this.samples[arm].size() >= this.getWarmUpPeriod();
    }

    public int getNumArms() {
        return this.numArms;
    }

    public double getReward(double x) {

        // Collect parameters for the Gaussian Mixture Distribution
        double[] means = this.getMeans();
        double[] variances = this.getVariances(means);
        // double[] mixtureProbs = this.getMixtureProbs();

        // Set mixture parameters
        double mean = 0.0;
        double variance = 0.0;
        double mixtureProb = 1.0 / ((double) this.getNumArms());  // Assume even distribution of samples to avoid bias
        for (int a = 0; a < this.getNumArms(); a++) {
            mean += mixtureProb * means[a];
            variance += (mixtureProb * mixtureProb) * variances[a];
        }

        NormalDistribution dist = new NormalDistribution(mean, Math.sqrt(Math.max(variance, DEFAULT_VAR)));
        return dist.cumulativeProbability(x) - 1.0;  // Reward in range [-1.0, 0.0]
    }

    private double[] getMixtureProbs() {
        double[] mixtureProbs = new double[this.getNumArms()];
        
        // Get total number of accrued samples
        double totalCount = 0.0;
        for (int a = 0; a < this.getNumArms(); a++) {
            totalCount += (double) this.samples[a].size();
        }
        
        // Set mixture probabilities using the smoothing factor
        for (int a = 0; a < this.getNumArms(); a++) {
            mixtureProbs[a] = (((double) this.samples[a].size()) + SMOOTHING_FACTOR) / (totalCount + SMOOTHING_FACTOR);
        }
        
        return mixtureProbs;
    }

    private double[] getMeans() {
        double[] means = new double[this.getNumArms()];
        for (int a = 0; a < this.getNumArms(); a++) {
            means[a] = this.getMean(a);
        }
        return means;
    }

    private double[] getVariances(double[] means) {
        double[] variances = new double[this.getNumArms()];
        for (int a = 0; a < this.getNumArms(); a++) {
            variances[a] = this.getVariance(a, means[a]);
        }
        return variances;
    }

    private double getMean(int arm) {
        ArrayList<Double> samples = this.samples[arm];

        if (samples.size() <= 0) {
            return DEFAULT_MEAN;
        }
        
        double sum = 0.0;
        for (double x : samples) {
            sum += x;
        }

        return sum / ((double) samples.size());
    }

    private double getVariance(int arm, double mean) {
        ArrayList<Double> samples = this.samples[arm];

        if (samples.size() <= 0) {
            return DEFAULT_VAR;
        }

        double diff = 0.0;
        for (double x : samples) {
            double d = (x - mean);
            diff += d * d;
        }

        return diff / ((double) samples.size());
    }

}
