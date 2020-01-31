package bandits;

public abstract class BanditOptimizer {

    private double[] rewards;
    private int[] counts;
    private int numArms;

    public BanditOptimizer(int numArms) {
        this.rewards = new double[numArms];
        this.counts = new int[numArms];
        this.numArms = numArms;
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
    
    public void update(int arm, double reward) { }

    public abstract int getArm(int time);
}
