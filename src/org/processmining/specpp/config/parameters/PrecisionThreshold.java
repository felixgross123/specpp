package org.processmining.specpp.config.parameters;

public class PrecisionThreshold implements Parameters {

    public static PrecisionThreshold p(double p) {
        return new PrecisionThreshold(p);
    }

    public static PrecisionThreshold getDefault() {
        return p(1.0);
    }

    private final double p;

    public PrecisionThreshold(double p) {
        this.p = p;
    }

    public double getP() {
        return p;
    }

    @Override
    public String toString() {
        return "PrecisionThreshold(p=" + p + ")";
    }
}
