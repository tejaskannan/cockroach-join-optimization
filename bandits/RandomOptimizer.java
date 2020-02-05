package bandits;

import java.util.Random;


public class RandomOptimizer extends BanditOptimizer {

    private Random rand;

    public RandomOptimizer(int numArms, int numTypes) {
        super(numArms, numTypes);
        this.rand = new Random();
    }

    @Override
    public int getArm(int time) {
        return this.rand.nextInt(super.getNumArms());
    }
}


