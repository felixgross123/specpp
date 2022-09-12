package org.processmining.specpp.orchestra;

import com.google.common.collect.ImmutableList;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.processmining.specpp.config.parameters.Parameters;
import org.processmining.specpp.preprocessing.orderings.*;

import java.util.List;
import java.util.Objects;

public class PreProcessingParameters implements Parameters {

    private final XEventClassifier eventClassifier;
    private final boolean addStartEndTransitions;
    private final Class<? extends ActivityOrderingBuilder> transitionEncodingsBuilderClass;

    public PreProcessingParameters(XEventClassifier eventClassifier, boolean addStartEndTransitions, Class<? extends ActivityOrderingBuilder> transitionEncodingsBuilderClass) {
        this.eventClassifier = eventClassifier;
        this.addStartEndTransitions = addStartEndTransitions;
        this.transitionEncodingsBuilderClass = transitionEncodingsBuilderClass;
    }

    public static PreProcessingParameters getDefault() {
        return new PreProcessingParameters(new XEventNameClassifier(), true, AverageTracePositionOrdering.class);
    }

    public static List<Class<? extends ActivityOrderingBuilder>> getAvailableTransitionEncodingsBuilders() {
        return ImmutableList.of(AverageTracePositionOrdering.class, TraceFrequencyOrdering.class, ActivityFrequencyOrdering.class, LexicographicTransitionOrdering.class);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PreProcessingParameters that = (PreProcessingParameters) o;
        return addStartEndTransitions == that.addStartEndTransitions && Objects.equals(eventClassifier, that.eventClassifier) && Objects.equals(transitionEncodingsBuilderClass, that.transitionEncodingsBuilderClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventClassifier, addStartEndTransitions, transitionEncodingsBuilderClass);
    }

    @Override
    public String toString() {
        return "PreProcessingParameters{" + "eventClassifierBuilder=" + eventClassifier + ", addStartEndTransitions=" + addStartEndTransitions + ", transitionEncodingsBuilderClass=" + transitionEncodingsBuilderClass + '}';
    }

    public Class<? extends ActivityOrderingBuilder> getTransitionEncodingsBuilderClass() {
        return transitionEncodingsBuilderClass;
    }

    public boolean isAddStartEndTransitions() {
        return addStartEndTransitions;
    }

    public XEventClassifier getEventClassifier() {
        return eventClassifier;
    }
}
