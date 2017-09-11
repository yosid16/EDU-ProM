package org.eduprom.miners.alpha;

import org.eduprom.miners.AbstractPetrinetMiner;
import org.deckfour.xes.classification.XEventClass;
import org.processmining.alphaminer.abstractions.AlphaClassicAbstraction;
import org.processmining.alphaminer.algorithms.AlphaMiner;
import org.processmining.alphaminer.algorithms.AlphaMinerFactory;
import org.processmining.alphaminer.parameters.AlphaMinerParameters;
import org.processmining.alphaminer.parameters.AlphaVersion;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;

import org.processmining.ptconversions.pn.ProcessTree2Petrinet;

import java.util.Objects;
import java.util.stream.Collectors;


public class Alpha extends AbstractPetrinetMiner {

    public Alpha(String filename) throws Exception {
        super(filename);
    }

    @Override
    protected ProcessTree2Petrinet.PetrinetWithMarkings minePetrinet() throws Exception {
        AlphaMinerParameters p = new AlphaMinerParameters();
        p.setVersion(getVersion());
        AlphaMiner<XEventClass, ? extends AlphaClassicAbstraction<XEventClass>, ? extends AlphaMinerParameters> alphaMiner
                = AlphaMinerFactory.createAlphaMiner(getPromPluginContext(), log, getClassifier(), p);
        Pair<Petrinet, Marking> runResult = alphaMiner.run();

        ProcessTree2Petrinet.PetrinetWithMarkings pn = new ProcessTree2Petrinet.PetrinetWithMarkings();
        pn.petrinet = runResult.getFirst();
        pn.initialMarking = runResult.getSecond();
        pn.finalMarking = new Marking(pn.petrinet.getPlaces().stream()
                .filter(x-> Objects.equals(x.getLabel(), "End")).collect(Collectors.toList()));

        return pn;
    }

    public AlphaVersion getVersion(){
        return AlphaVersion.CLASSIC;
    }
}