package org.processmining.specpp.config.parameters;

public class TreeHeuristcAlpha implements Parameters {

    public static TreeHeuristcAlpha alpha(double alpha) {
        return new TreeHeuristcAlpha(alpha);
    }

    public static TreeHeuristcAlpha getDefault() {
        return alpha(1.0);
    }

    private final double alpha;

    public TreeHeuristcAlpha(double p) {
        this.alpha = p;
    }

    public double getAlpha() {
        return alpha;
    }

    @Override
    public String toString() {
        return "PrecisionThreshold(p=" + alpha + ")";
    }
}
