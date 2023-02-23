package org.processmining.specpp.config.parameters;

public class PrematureAbort implements Parameters {

    public static PrematureAbort prematureAbort(boolean prematureAbort) {
        return new PrematureAbort(prematureAbort);
    }

    public static PrematureAbort getDefault() {
        return prematureAbort(true);
    }

    private final boolean prematureAbort;

    public PrematureAbort(boolean prematureAbort) {
        this.prematureAbort = prematureAbort;
    }

    public boolean getPrematureAbort() {
        return prematureAbort;
    }

}
