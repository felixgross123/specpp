package org.processmining.specpp.config.parameters;

public class TreeHeuristcAlpha implements Parameters {

    public static TreeHeuristcAlpha p(double p) {
        return new TreeHeuristcAlpha(p);
    }

    public static TreeHeuristcAlpha getDefault() {
        return p(1.0);
    }

    private final double p;

    public TreeHeuristcAlpha(double p) {
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
