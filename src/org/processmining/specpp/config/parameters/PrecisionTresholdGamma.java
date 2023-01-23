package org.processmining.specpp.config.parameters;

public class PrecisionTresholdGamma implements Parameters {

    public static PrecisionTresholdGamma g(double g) {
        return new PrecisionTresholdGamma(g);
    }

    public static PrecisionTresholdGamma getDefault() {
        return g(0.0);
    }

    private final double g;

    public PrecisionTresholdGamma(double g) {
        this.g = g;
    }

    public double getG() {
        return g;
    }

    @Override
    public String toString() {
        return "PrecisionThreshold(g=" + g + ")";
    }
}
