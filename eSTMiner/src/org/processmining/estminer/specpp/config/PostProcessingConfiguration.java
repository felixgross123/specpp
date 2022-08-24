package org.processmining.estminer.specpp.config;

import org.processmining.estminer.specpp.base.PostProcessor;
import org.processmining.estminer.specpp.base.Result;
import org.processmining.estminer.specpp.base.impls.PostProcessorPipe;
import org.processmining.estminer.specpp.componenting.system.GlobalComponentRepository;
import org.processmining.estminer.specpp.componenting.system.link.PostProcessorComponent;
import org.processmining.estminer.specpp.postprocessing.WrappedPostProcessor;
import org.processmining.estminer.specpp.supervision.instrumentators.InstrumentedPostProcessor;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

public class PostProcessingConfiguration<R extends Result, F extends Result> extends Configuration {
    private final Deque<SimpleBuilder<? extends PostProcessorComponent<?, ?>>> list;

    public PostProcessingConfiguration(GlobalComponentRepository gcr, SimpleBuilder<? extends PostProcessorComponent<R, ?>> first, SimpleBuilder<? extends PostProcessorComponent<?, F>> last, Deque<SimpleBuilder<? extends PostProcessorComponent<?, ?>>> list) {
        super(gcr);
        this.list = list;
        assert !list.isEmpty() && first != null && last != null;
        assert list.peekFirst().equals(first);
        assert list.peekLast().equals(last);
    }

    public PostProcessorComponent<?, ?> createPossiblyInstrumented(SimpleBuilder<? extends PostProcessorComponent<?, ?>> builder) {
        PostProcessorComponent<?, ?> pp = createFrom(builder);
        return shouldBeInstrumented(pp) ? checkout(new InstrumentedPostProcessor<>(pp.toString(), pp)) : pp;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public PostProcessor<R, F> createPostProcessorPipeline() {
        if (list.isEmpty()) return null;
        Iterator<SimpleBuilder<? extends PostProcessorComponent<?, ?>>> it = list.iterator();
        if (list.size() > 1) it.next(); // skip first dummy if there are actual post processors available
        PostProcessor curr = createPossiblyInstrumented(it.next());
        while (it.hasNext()) {
            SimpleBuilder<? extends PostProcessorComponent<?, ?>> next = it.next();
            curr = new PostProcessorPipe(curr, createPossiblyInstrumented(next));
        }
        return (PostProcessor<R, F>) curr;
    }

    public static class Configurator<R extends Result, F extends Result> implements ComponentInitializerBuilder<PostProcessingConfiguration<R, F>> {

        private final SimpleBuilder<? extends PostProcessorComponent<R, ?>> first;
        private final SimpleBuilder<? extends PostProcessorComponent<?, F>> last;
        private final Deque<SimpleBuilder<? extends PostProcessorComponent<?, ?>>> list;

        public Configurator(SimpleBuilder<? extends PostProcessor<R, F>> initial) {
            SimpleBuilder<WrappedPostProcessor<R, F>> wrappedInitial = () -> new WrappedPostProcessor<>(initial.build());
            this.first = wrappedInitial;
            this.last = wrappedInitial;
            list = new LinkedList<>();
            list.add(wrappedInitial);
        }

        public Configurator(SimpleBuilder<? extends PostProcessorComponent<R, ?>> first, SimpleBuilder<? extends PostProcessorComponent<?, F>> last, Deque<SimpleBuilder<? extends PostProcessorComponent<?, ?>>> list) {
            this.first = first;
            this.last = last;
            this.list = list;
        }

        public <T extends Result> Configurator<R, T> processor(SimpleBuilder<? extends PostProcessor<? super F, T>> builder) {
            WrappedPostProcessor.Builder<? super F, T> wB = new WrappedPostProcessor.Builder<>(builder);
            list.add(wB);
            return new Configurator<>(first, wB, list);
        }

        public <T extends Result> Configurator<R, T> processorComponent(SimpleBuilder<PostProcessorComponent<? super F, T>> builder) {
            list.add(builder);
            return new Configurator<>(first, builder, list);
        }

        public PostProcessingConfiguration<R, F> build(GlobalComponentRepository gcr) {
            return new PostProcessingConfiguration<>(gcr, first, last, list);
        }

    }

}
