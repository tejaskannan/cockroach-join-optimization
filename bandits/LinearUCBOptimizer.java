package bandits;


import java.lang.Math;
import java.util.Random;
import java.util.List;

import org.la4j.Matrix;
import org.la4j.Vector;
import org.la4j.inversion.GaussJordanInverter;

import utils.Utils;


public class LinearUCBOptimizer extends BanditOptimizer {

    private double alpha;
    private double lambda;
    private int numFeatures;  // D
    private double[][] A;  // D x D
    private double[] b;  // D x 1
    private Random rand;

    public LinearUCBOptimizer(int numArms, int numTypes, int numFeatures, double rewardEpsilon, double rewardAnneal, int updateThreshold, double alpha, double lambda) {
        super(numArms, numTypes, rewardEpsilon, rewardAnneal, updateThreshold, String.format("LinUCB-%2.3f-%2.3f", alpha, lambda));

        this.rand = new Random();
        this.alpha = alpha;
        this.lambda = lambda;
        this.numFeatures = numFeatures;
        
        // Initialize parameters
        this.A = new double[numFeatures][numFeatures];
        this.b = new double[numFeatures];
        for (int i = 0; i < numFeatures; i++) {
            this.A[i][i] = lambda;
        }
    }

    @Override
    public void update(int arm, int type, double reward, List<Vector> contexts) {
        super.recordSample(reward, arm, type);

        if (!super.shouldUpdate(arm, type)) {
            return;
        }

        // Normalize reward based on the query type (add one to bring into range [0, 1])
        double normalizedReward = super.normalizeReward(reward, type);
        
        // Stack contexts into (K x D) matrix and normalize columns
        Matrix contextMatrix = Utils.stackContexts(contexts);
        Utils.normalizeColumns(contextMatrix);

        Vector armContext = contextMatrix.getRow(arm);
        
        Matrix AUpdate = armContext.outerProduct(armContext);
        for (int i = 0; i < this.numFeatures; i++) {
            for (int j = 0; j < this.numFeatures; j++) {
                this.A[i][j] += AUpdate.get(i, j);
            }
        }
        
        Vector bUpdate = armContext.multiply(normalizedReward);
        for (int i = 0; i < this.numFeatures; i++) {
            this.b[i] += bUpdate.get(i);
        }
    }

    @Override
    public int getArm(int time, int type, List<Vector> contexts, boolean shouldExploit) {
        // Stack contexts into (K x D) matrix and normalize columns
        Matrix contextMatrix = Utils.stackContexts(contexts);
        Utils.normalizeColumns(contextMatrix);

        // Compute A inverse
        Matrix AMat = Matrix.from2DArray(this.A);
        GaussJordanInverter AInverter = new GaussJordanInverter(AMat);
        Matrix AInv = AInverter.inverse();

        Vector contextVector;
        Vector theta = AInv.multiply(Vector.fromArray(this.b));
        Vector s = Vector.zero(this.getNumArms());
        for (int a = 0; a < this.getNumArms(); a++) {
            contextVector = contextMatrix.getRow(a);
            double contextTheta = contextVector.innerProduct(theta);
            double contextA = contextVector.innerProduct(AInv.multiply(contextVector));
            s.set(a, contextTheta + this.alpha * Math.sqrt(contextA));
        }

        int arm = Utils.argMax(s);
        return arm;
    }
}
