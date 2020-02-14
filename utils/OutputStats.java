package utils;

import org.json.simple.JSONObject;

public class OutputStats {

    private double regret;
    private int queryType;
    private int arm;
    private double elapsedTime;
    private double normalizedReward;

    public OutputStats(double elapsedTime, double normalizedReward, double regret, int arm, int queryType) {
        this.regret = regret;
        this.queryType = queryType;
        this.elapsedTime = elapsedTime;
        this.normalizedReward = normalizedReward;
        this.arm = arm;
    }

    public double getRegret() {
        return this.regret;
    }

    public double getElapsedTime() {
        return this.elapsedTime;
    }

    public double getNormalizedReward() {
        return this.normalizedReward;
    }

    public int getQueryType() {
        return this.queryType;
    }

    public int getArm() {
        return this.arm;
    }

    public JSONObject toJsonObject() {
        JSONObject result = new JSONObject();

        result.put("regret", this.getRegret());
        result.put("elapsed_time", this.getElapsedTime());
        result.put("normalized_reward", this.getNormalizedReward());
        result.put("query_type", this.getQueryType());
        result.put("arm", this.getArm());

        return result;
    }

}
