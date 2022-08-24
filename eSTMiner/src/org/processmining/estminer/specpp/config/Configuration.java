package org.processmining.estminer.specpp.config;

import org.processmining.estminer.specpp.componenting.data.ParameterRequirements;
import org.processmining.estminer.specpp.componenting.system.ComponentInitializer;
import org.processmining.estminer.specpp.componenting.system.GlobalComponentRepository;
import org.processmining.estminer.specpp.config.parameters.SupervisionParameters;

public class Configuration extends ComponentInitializer {
    public Configuration(GlobalComponentRepository gcr) {
        super(gcr);
    }

    public <T> T createFrom(SimpleBuilder<T> builder) {
        return checkout(checkout(builder).build());
    }

    public <T, A> T createFrom(InitializingBuilder<T, A> builder, A argument) {
        return checkout(checkout(builder).build(argument));
    }

    protected boolean shouldBeInstrumented(Object o) {
        SupervisionParameters ask = componentSystemAdapter().parameters()
                                                            .askForData(ParameterRequirements.SUPERVISION_PARAMETERS);
        return ask != null && ask.shouldBeInstrumented(o);
    }
}
