package org.processmining.specpp.evaluation.fitness;

import org.processmining.specpp.config.parameters.Parameters;

public class ReplayComputationParameters implements Parameters {

    private final boolean clipMarkingAtZero;

    public ReplayComputationParameters(boolean clipMarkingAtZero) {
        this.clipMarkingAtZero = clipMarkingAtZero;
    }

    public static ReplayComputationParameters getDefault() {
        return new ReplayComputationParameters(true);
    }

    public static ReplayComputationParameters permitNegative(boolean permitNegativeMarkingsDuringReplay) {
        return new ReplayComputationParameters(!permitNegativeMarkingsDuringReplay);
    }

    public boolean isClipMarkingAtZero() {
        return clipMarkingAtZero;
    }
}
