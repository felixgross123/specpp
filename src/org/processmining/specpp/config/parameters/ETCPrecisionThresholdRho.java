package org.processmining.specpp.config.parameters;

public class ETCPrecisionThresholdRho implements Parameters {

    public static ETCPrecisionThresholdRho p(double p) {
        return new ETCPrecisionThresholdRho(p);
    }

    public static ETCPrecisionThresholdRho getDefault() {
        return p(1.0);
    }

    private final double p;

    public ETCPrecisionThresholdRho(double p) {
        this.p = p;
    }

    public double getP() {
        return p;
    }

    @Override
    public String toString() {
        return "ETCPrecisionThreshold(p=" + p + ")";
    }
}
