package org.processmining.specpp.config.parameters;

public class CutOffETCBasedPrecision implements Parameters {

    public static CutOffETCBasedPrecision cutOff(boolean cutOff) {
        return new CutOffETCBasedPrecision(cutOff);
    }

    public static CutOffETCBasedPrecision getDefault() {
        return cutOff(false);
    }

    private final boolean cutOff;

    public CutOffETCBasedPrecision(boolean cutOff) {
        this.cutOff = cutOff;
    }

    public boolean getCutOff() {
        return cutOff;
    }

}
