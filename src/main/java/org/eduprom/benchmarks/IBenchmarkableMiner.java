package org.eduprom.benchmarks;

import org.eduprom.conformance.IConformanceObject;
import org.eduprom.miners.IPetrinetMiner;
import org.processmining.processtree.ProcessTree;

public interface IBenchmarkableMiner extends IPetrinetMiner, IConformanceObject {
    ProcessTree getProcessTree();
}
