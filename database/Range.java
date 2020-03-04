package database;


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

    public String toString() {
        return String.format("[%d, %d]", this.getMin(), this.getMax());
    }
}
