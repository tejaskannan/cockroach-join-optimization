package database;


import java.lang.Math;


public class Range {

    private int min;
    private int max;

    public Range(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public int intersect(Range that) {
        if (that == null) {
            return 0;
        }
        return Math.min(this.max, that.max) - Math.max(this.min, that.min);
    }

    public String toString() {
        return String.format("[%d, %d]", this.getMin(), this.getMax());
    }
}
