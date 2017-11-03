package org.eduprom.benchmarks;

import org.eduprom.conformance.IConformanceObject;
import org.eduprom.miners.IMiner;
import org.eduprom.miners.IPetrinetMiner;
import org.eduprom.miners.adaptiveNoise.ConformanceInfo;

import javax.resource.NotSupportedException;

public interface IBenchmarkableMiner extends IPetrinetMiner, IConformanceObject {
}
