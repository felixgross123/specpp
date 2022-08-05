package org.processmining.estminer.specpp.componenting.evaluation;

import org.processmining.estminer.specpp.base.Evaluable;
import org.processmining.estminer.specpp.base.Evaluation;
import org.processmining.estminer.specpp.base.Evaluator;
import org.processmining.estminer.specpp.componenting.delegators.DelegatingEvaluator;
import org.processmining.estminer.specpp.componenting.system.ComponentType;
import org.processmining.estminer.specpp.componenting.system.Requirement;
import org.processmining.estminer.specpp.datastructures.util.NoRehashing;
import org.processmining.estminer.specpp.datastructures.util.Tuple2;
import org.processmining.estminer.specpp.util.JavaTypingUtils;

public class EvaluatorRequirement<I extends Evaluable, E extends Evaluation> extends NoRehashing<Tuple2<Class<I>, Class<E>>> implements Requirement<Evaluator<I, E>, EvaluatorRequirement<?, ?>> {


    private final Class<I> evaluableClass;
    private final Class<E> evaluationClass;

    public EvaluatorRequirement(Class<I> evaluableClass, Class<E> evaluationClass) {
        super(new Tuple2<>(evaluableClass, evaluationClass));
        this.evaluableClass = evaluableClass;
        this.evaluationClass = evaluationClass;
    }

    public Class<I> getEvaluableClass() {
        return evaluableClass;
    }

    public Class<E> getEvaluationClass() {
        return evaluationClass;
    }

    @Override
    public boolean gt(EvaluatorRequirement<?, ?> other) {
        return evaluableClass.isAssignableFrom(other.evaluableClass) && other.evaluationClass.isAssignableFrom(evaluationClass);
    }

    @Override
    public boolean lt(EvaluatorRequirement<?, ?> other) {
        return other.evaluableClass.isAssignableFrom(evaluableClass) && evaluationClass.isAssignableFrom(other.evaluationClass);
    }


    public DelegatingEvaluator<I, E> emptyDelegator() {
        return new DelegatingEvaluator<>();
    }

    @Override
    public ComponentType componentType() {
        return ComponentType.Evaluation;
    }

    @Override
    public Class<Evaluator<I, E>> contentClass() {
        return JavaTypingUtils.castClass(Evaluator.class);
    }

    public FulfilledEvaluatorRequirement<I, E> fulfilWith(Evaluator<I, E> delegate) {
        return new FulfilledEvaluatorRequirement<>(this, delegate);
    }

    @Override
    public String toString() {
        return "EvaluatorRequirement(" + evaluableClass.getSimpleName() + ", " + evaluationClass.getSimpleName() + ")";
    }
}
