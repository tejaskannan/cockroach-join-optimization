package bandits;

import java.io.Serializable;
import java.lang.Math;
import java.util.Random;
import java.util.ArrayList;
import org.apache.commons.math3.distribution.NormalDistribution;


public class RewardDistribution implements Serializable {

    private int warmUpPeriod;
    private double epsilon;
    private double annealRate;

    private ArrayList<Double> samples;
    private Random rand;

    public RewardDistribution(double epsilon, double annealRate, int warmUpPeriod) {
        this.epsilon = epsilon;
        this.annealRate = annealRate;
        this.warmUpPeriod = warmUpPeriod;
        this.samples = new ArrayList<Double>();
        this.rand = new Random();
    }

    public void addSample(double x) {
        this.samples.add(x);
    }

    public int getWarmUpPeriod() {
        return this.warmUpPeriod;
    }

    public boolean shouldUpdate() {
        return this.samples.size() >= this.warmUpPeriod;
    }

    public boolean shouldActGreedy() {
        double z = this.rand.nextDouble();
        epsilon = this.epsilon;

        // Update epsilon
        this.epsilon *= this.annealRate;

        return (boolean) (z < epsilon);
    }

    public double getReward(double x) {
        NormalDistribution dist = new NormalDistribution(this.getMean(), Math.sqrt(this.getVariance()));
        return dist.cumulativeProbability(x) - 1.0;  // Reward in range [-1.0, 0.0]
    }

    private double getMean() {
        if (this.samples.size() <= 0) {
            return 0.0;
        }
        
        double sum = 0.0;
        for (double x : this.samples) {
            sum += x;
        }
        return sum / ((double) this.samples.size());
    }

    private double getVariance() {
        if (this.samples.size() <= 0) {
            return 0.1;
        }

        double mean = this.getMean();
        double diff = 0.1;  // Initialize to small value to prevent zero variance
        for (double x : this.samples) {
            double d = (x - mean);
            diff += d * d;
        }

        return diff / ((double) this.samples.size());
    }

}
