package org.processmining.specpp.headless;

import org.deckfour.xes.classification.XEventNameClassifier;
import org.processmining.specpp.base.AdvancedComposition;
import org.processmining.specpp.componenting.data.ParameterRequirements;
import org.processmining.specpp.componenting.evaluation.EvaluatorConfiguration;
import org.processmining.specpp.composition.StatefulPlaceComposition;
import org.processmining.specpp.composition.composers.FelixNewPlaceComposer;
import org.processmining.specpp.composition.composers.PlaceFitnessFilter;
import org.processmining.specpp.config.*;
import org.processmining.specpp.config.parameters.ParameterProvider;
import org.processmining.specpp.config.parameters.PlaceGeneratorParameters;
import org.processmining.specpp.config.parameters.SupervisionParameters;
import org.processmining.specpp.config.parameters.TauFitnessThresholds;
import org.processmining.specpp.datastructures.petri.CollectionOfPlaces;
import org.processmining.specpp.datastructures.petri.Place;
import org.processmining.specpp.datastructures.petri.ProMPetrinetWrapper;
import org.processmining.specpp.datastructures.tree.base.HeuristicStrategy;
import org.processmining.specpp.datastructures.tree.base.impls.EnumeratingTree;
import org.processmining.specpp.datastructures.tree.base.impls.VariableExpansion;
import org.processmining.specpp.datastructures.tree.heuristic.HeuristicTreeExpansion;
import org.processmining.specpp.datastructures.tree.heuristic.TreeNodeScore;
import org.processmining.specpp.datastructures.tree.nodegen.MonotonousPlaceGenerationLogic;
import org.processmining.specpp.datastructures.tree.nodegen.PlaceNode;
import org.processmining.specpp.datastructures.tree.nodegen.PlaceState;
import org.processmining.specpp.evaluation.fitness.AbsolutelyNoFrillsFitnessEvaluator;
import org.processmining.specpp.evaluation.heuristics.AvgFirstOccIndexDeltaTreeHeuristic;
import org.processmining.specpp.evaluation.heuristics.DirectlyFollowsHeuristic;
import org.processmining.specpp.evaluation.heuristics.DirectlyFollowsTreeHeuristic;
import org.processmining.specpp.evaluation.heuristics.EventuallyFollowsTreeHeuristic;
import org.processmining.specpp.evaluation.implicitness.ImplicitnessTestingParameters;
import org.processmining.specpp.evaluation.implicitness.LPBasedImplicitnessCalculator;
import org.processmining.specpp.evaluation.markings.LogHistoryMaker;
import org.processmining.specpp.orchestra.PreProcessingParameters;
import org.processmining.specpp.orchestra.SPECppConfigBundle;
import org.processmining.specpp.orchestra.SPECppOperations;
import org.processmining.specpp.postprocessing.LPBasedImplicitnessPostProcessing;
import org.processmining.specpp.postprocessing.ProMConverter;
import org.processmining.specpp.postprocessing.SelfLoopPlaceMerger;
import org.processmining.specpp.preprocessing.InputData;
import org.processmining.specpp.preprocessing.InputDataBundle;
import org.processmining.specpp.preprocessing.orderings.AverageFirstOccurrenceIndex;
import org.processmining.specpp.prom.mvc.config.ConfiguratorCollection;
import org.processmining.specpp.proposal.ConstrainablePlaceProposer;
import org.processmining.specpp.supervision.supervisors.BaseSupervisor;
import org.processmining.specpp.supervision.supervisors.DebuggingSupervisor;
import org.processmining.specpp.supervision.supervisors.TerminalSupervisor;
import org.processmining.specpp.util.PublicPaths;


public class DevelopmentEntryPointWTreeHeuristic {

    public static void main(String[] args) {
        String path = PublicPaths.REALLIFE_RTFM;
        PreProcessingParameters prePar = new PreProcessingParameters(new XEventNameClassifier(), true, AverageFirstOccurrenceIndex.class);
        InputDataBundle inputData = InputData.loadData(path, prePar).getData();
        SPECppConfigBundle configuration = createConfiguration();
        SPECppOperations.configureAndExecute(configuration, inputData, false);
    }

    public static SPECppConfigBundle createConfiguration() {
        // ** Supervision ** //

        SupervisionConfiguration.Configurator svConfig = Configurators.supervisors()
                                                                      .addSupervisor(BaseSupervisor::new)
                                                                      .addSupervisor(DebuggingSupervisor::new)
                                                                      .addSupervisor(TerminalSupervisor::new);

        // ** Evaluation ** //

        EvaluatorConfiguration.Configurator evConfig = Configurators.evaluators()
                                                                    .addEvaluatorProvider(new AbsolutelyNoFrillsFitnessEvaluator.Builder())
                                                                    .addEvaluatorProvider(new LogHistoryMaker.Builder())
                                                                    .addEvaluatorProvider(new LPBasedImplicitnessCalculator.Builder());


        EfficientTreeConfiguration.Configurator<Place, PlaceState, PlaceNode> etConfig = Configurators.<Place, PlaceState, PlaceNode, TreeNodeScore>heuristicTree()
                                                                                                      .heuristicExpansion(HeuristicTreeExpansion::new)
                                                                                                      .heuristic(new DirectlyFollowsTreeHeuristic.Builder())
                                                                                                      .childGenerationLogic(new MonotonousPlaceGenerationLogic.Builder())
                                                                                                      .tree(EnumeratingTree::new);

        // ** Proposal & Composition ** //

        ProposerComposerConfiguration.Configurator<Place, AdvancedComposition<Place>, CollectionOfPlaces> pcConfig = Configurators.<Place, AdvancedComposition<Place>, CollectionOfPlaces>proposerComposer()
                                                                                                                                  .composition(StatefulPlaceComposition::new)
                                                                                                                                  .proposer(new ConstrainablePlaceProposer.Builder());

        pcConfig.terminalComposer(FelixNewPlaceComposer::new);
        // PlaceAccepter oder CIPR

        pcConfig.composerChain(PlaceFitnessFilter::new);

        //pcConfig.composerChain(PlaceFitnessFilter::new, PlacePreSortingForBetterSimplicity::new);

        // ** Post Processing ** //

        PostProcessingConfiguration.Configurator<CollectionOfPlaces, CollectionOfPlaces> temp_ppConfig = Configurators.postProcessing();
        // ppConfig.processor(SelfLoopPlaceMerger::new);

        temp_ppConfig
         //.addPostProcessor(new ReplayBasedImplicitnessPostProcessing.Builder())
                .addPostProcessor(SelfLoopPlaceMerger::new)
                  .addPostProcessor(new LPBasedImplicitnessPostProcessing.Builder());
        PostProcessingConfiguration.Configurator<CollectionOfPlaces, ProMPetrinetWrapper> ppConfig = temp_ppConfig.addPostProcessor(ProMConverter::new);

        // ** Parameters ** //

        ParameterProvider parProv = new ParameterProvider() {
            @Override
            public void init() {
                globalComponentSystem().provide(ParameterRequirements.IMPLICITNESS_TESTING.fulfilWithStatic(new ImplicitnessTestingParameters(ImplicitnessTestingParameters.CIPRVersion.ReplayBased, ImplicitnessTestingParameters.SubLogRestriction.None)))
                                       .provide(ParameterRequirements.PLACE_GENERATOR_PARAMETERS.fulfilWithStatic(new PlaceGeneratorParameters(7, true, false, false, false)))
                                       .provide(ParameterRequirements.SUPERVISION_PARAMETERS.fulfilWithStatic(SupervisionParameters.instrumentNone(true, false)))
                        .provide(ParameterRequirements.TAU_FITNESS_THRESHOLDS.fulfilWithStatic(new TauFitnessThresholds(0.9)));
            }
        };

        return new ConfiguratorCollection(svConfig, pcConfig, evConfig, etConfig, ppConfig, parProv);
    }


}
