package org.processmining.specpp.config.parameters;

public class TreeHeuristcAlpha implements Parameters {

    public static TreeHeuristcAlpha alpha(double alpha) {
        return new TreeHeuristcAlpha(alpha);
    }

    public static TreeHeuristcAlpha getDefault() {
        return alpha(1);
    }

    private final double alpha;

    public TreeHeuristcAlpha(double alpha) {
        this.alpha = alpha;
    }

    public double getAlpha() {
        return alpha;
    }

}
