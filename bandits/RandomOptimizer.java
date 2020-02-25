package bandits;

import java.util.Random;
import java.util.List;
import org.la4j.Vector;


public class RandomOptimizer extends BanditOptimizer {

    private Random rand;

    public RandomOptimizer(int numArms, int numTypes) {
        super(numArms, numTypes, 0.0, 0.0, 0, "Random");
        this.rand = new Random();
    }

    @Override
    public int getArm(int time, int type, List<Vector> contexts) {
        return this.rand.nextInt(super.getNumArms());
    }
}


