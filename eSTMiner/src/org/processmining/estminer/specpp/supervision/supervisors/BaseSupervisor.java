package org.processmining.estminer.specpp.supervision.supervisors;

import com.google.common.collect.ImmutableList;
import org.processmining.estminer.specpp.base.ConstraintEvent;
import org.processmining.estminer.specpp.base.impls.CandidateConstraint;
import org.processmining.estminer.specpp.componenting.delegators.ContainerUtils;
import org.processmining.estminer.specpp.componenting.delegators.DelegatingAdHocObservable;
import org.processmining.estminer.specpp.componenting.delegators.DelegatingObservable;
import org.processmining.estminer.specpp.componenting.supervision.ObserverRequirement;
import org.processmining.estminer.specpp.componenting.supervision.SupervisionRequirements;
import org.processmining.estminer.specpp.datastructures.tree.base.GenerationConstraint;
import org.processmining.estminer.specpp.datastructures.tree.events.HeuristicComputationEvent;
import org.processmining.estminer.specpp.datastructures.tree.heuristic.DoubleScore;
import org.processmining.estminer.specpp.supervision.MessageLogger;
import org.processmining.estminer.specpp.supervision.monitoring.KeepLastMonitorMap;
import org.processmining.estminer.specpp.supervision.monitoring.Monitor;
import org.processmining.estminer.specpp.supervision.observations.*;
import org.processmining.estminer.specpp.supervision.observations.performance.PerformanceEvent;
import org.processmining.estminer.specpp.supervision.observations.performance.PerformanceStatistics;
import org.processmining.estminer.specpp.supervision.piping.ConcurrencyBridge;
import org.processmining.estminer.specpp.supervision.piping.PipeWorks;
import org.processmining.estminer.specpp.supervision.piping.SummarizingBufferingPipe;
import org.processmining.estminer.specpp.supervision.traits.Monitoring;
import org.processmining.estminer.specpp.supervision.transformers.Transformers;
import org.processmining.estminer.specpp.util.JavaTypingUtils;

import java.time.Duration;
import java.util.Collection;

import static org.processmining.estminer.specpp.componenting.supervision.SupervisionRequirements.*;

public class BaseSupervisor extends SchedulingSupervisor implements Monitoring {

    private final MessageLogger fl, cl;
    public static final ObserverRequirement<LogMessage> FILE_LOGGER_REQUIREMENT = observer("logger.file", LogMessage.class);
    public static final ObserverRequirement<LogMessage> CONSOLE_LOGGER_REQUIREMENT = observer("logger.console", LogMessage.class);
    private final KeepLastMonitorMap<Statistics<StatisticKey, Count>> accumulatedStatisticsMonitor;

    private final ConcurrencyBridge<PerformanceEvent> performanceEventConcurrencyBridge = PipeWorks.concurrencyBridge();

    public BaseSupervisor() {
        componentSystemAdapter().provide(pipe("performance", PerformanceEvent.class, PerformanceStatistics.class, performanceStatistics))
                                .require(observable("tree.events", TreeEvent.class), treeEvents)
                                .require(adHocObservable("tree.stats", TreeStatsEvent.class), treeStats)
                                .require(observable(regex("^.+\\.performance$"), PerformanceEvent.class), ContainerUtils.observeResults(performanceEventConcurrencyBridge))
                                .require(observable("tree.performance", PerformanceEvent.class), treePerformance)
                                .require(observable("proposer.performance", PerformanceEvent.class), proposerPerformance)
                                .require(observable("evaluator.performance", PerformanceEvent.class), evaluatorPerformance)
                                .require(observable("composer.performance", PerformanceEvent.class), composerPerformance)
                                .require(observable("pec.performance", PerformanceEvent.class), specPerformance)
                                .require(observable("composer.constraints", JavaTypingUtils.castClass(CandidateConstraint.class)), composerConstraints)
                                .require(observable("proposer.constraints", GenerationConstraint.class), proposerConstraints)
                                .require(observable("heuristics.events", JavaTypingUtils.castClass(HeuristicComputationEvent.class)), heuristicsEvents)
                                .require(adHocObservable("heuristics.stats", HeuristicStatsEvent.class), heuristicStats);

        fl = PipeWorks.fileLogger();
        cl = PipeWorks.consoleLogger();
        componentSystemAdapter().provide(observer(FILE_LOGGER_REQUIREMENT, fl))
                                .provide(observer(CONSOLE_LOGGER_REQUIREMENT, cl));

        accumulatedStatisticsMonitor = new KeepLastMonitorMap<>();
    }


    private final SummarizingBufferingPipe<PerformanceEvent, PerformanceStatistics> performanceStatistics = PipeWorks.summarizingBuffer(Transformers.performanceEventSummarizer());

    private final DelegatingObservable<ConstraintEvent> composerConstraints = new DelegatingObservable<>();
    private final DelegatingObservable<ConstraintEvent> proposerConstraints = new DelegatingObservable<>();
    private final DelegatingObservable<TreeEvent> treeEvents = new DelegatingObservable<>();
    private final DelegatingAdHocObservable<TreeStatsEvent> treeStats = new DelegatingAdHocObservable<>();
    private final DelegatingAdHocObservable<HeuristicStatsEvent> heuristicStats = new DelegatingAdHocObservable<>();
    private final DelegatingObservable<PerformanceEvent> treePerformance = new DelegatingObservable<>();
    private final DelegatingObservable<PerformanceEvent> proposerPerformance = new DelegatingObservable<>();
    private final DelegatingObservable<PerformanceEvent> evaluatorPerformance = new DelegatingObservable<>();
    private final DelegatingObservable<PerformanceEvent> composerPerformance = new DelegatingObservable<>();
    private final DelegatingObservable<PerformanceEvent> specPerformance = new DelegatingObservable<>();
    private final DelegatingObservable<HeuristicComputationEvent<DoubleScore>> heuristicsEvents = new DelegatingObservable<>();

    @Override
    public void init() {
        if (treeEvents.isSet()) {
            beginLaying().source(treeEvents)
                         .pipe(PipeWorks.concurrencyBridge())
                         .giveBackgroundThread()
                         .pipe(PipeWorks.summarizingBuffer(Transformers.eventCounter()))
                         .schedule(Duration.ofMillis(100))
                         .sinks(PipeWorks.loggingSinks("[\u27F3100ms] tree.events.count", fl))
                         .pipe(PipeWorks.accumulatingPipe(EventCountStatistics::new))
                         .<EventCountStatistics>export(p -> componentSystemAdapter().provide(SupervisionRequirements.observable("tree.events.accumulation", EventCountStatistics.class, p)))
                         .sinks(PipeWorks.loggingSinks("tree.events.accumulation", cl, fl))
                         .apply();
        }

        beginLaying().source(performanceEventConcurrencyBridge)
                     .giveBackgroundThread()
                     .pipe(performanceStatistics)
                     .schedule(Duration.ofMillis(100))
                     .sinks(PipeWorks.loggingSinks("[\u27F3100ms] performance", cl, fl))
                     .pipe(PipeWorks.accumulatingPipe(PerformanceStatistics::new))
                     .<PerformanceStatistics>export(p -> componentSystemAdapter().provide(observable("performance.accumulation", PerformanceStatistics.class, p)))
                     .sinks(PipeWorks.loggingSinks("performance.accumulation", cl, fl))
                     .sink(accumulatedStatisticsMonitor)
                     .apply();

        if (heuristicsEvents.isSet()) {
            beginLaying().source(heuristicsEvents)
                         .pipe(PipeWorks.asyncSummarizingBuffer(Transformers.eventCounter()))
                         .schedule(Duration.ofMillis(100))
                         .sink(PipeWorks.loggingSink("heuristic.count", fl))
                         .pipe(PipeWorks.accumulatingPipe(EventCountStatistics::new))
                         .sinks(PipeWorks.loggingSinks("heuristics.count.accumulation", cl, fl))
                         .sink(accumulatedStatisticsMonitor)
                         .apply();
        }


        if (composerConstraints.isSet()) {
            beginLaying().source(composerConstraints)
                         .pipe(PipeWorks.asyncSummarizingBuffer(Transformers.eventCounter()))
                         .schedule(Duration.ofMillis(100))
                         .sinks(PipeWorks.loggingSinks("[\u27F3100ms] composer.constraints.count", fl))
                         .pipe(PipeWorks.accumulatingPipe(EventCountStatistics::new))
                         .sinks(PipeWorks.loggingSinks("composer.constraints.count.accumulation", cl, fl))
                         .sink(accumulatedStatisticsMonitor)
                         .apply();
        }

        if (proposerConstraints.isSet()) {
            beginLaying().source(proposerConstraints)
                         .pipe(PipeWorks.asyncSummarizingBuffer(Transformers.eventCounter()))
                         .schedule(Duration.ofMillis(100))
                         .sinks(PipeWorks.loggingSinks("[\u27F3100ms] proposer.constraints.count", fl))
                         .pipe(PipeWorks.accumulatingPipe(EventCountStatistics::new))
                         .sinks(PipeWorks.loggingSinks("proposer.constraints.count.accumulation", cl, fl))
                         .sink(accumulatedStatisticsMonitor)
                         .apply();
        }
    }


    @Override
    public Collection<Monitor<?, ?>> getMonitors() {
        return ImmutableList.of(accumulatedStatisticsMonitor);
    }
}
