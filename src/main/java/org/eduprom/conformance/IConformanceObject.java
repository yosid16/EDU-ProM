package org.eduprom.conformance;

import org.eduprom.miners.adaptiveNoise.conformance.ConformanceInfo;

public interface IConformanceObject {

    ConformanceInfo getConformanceInfo();

    void setConformanceInfo(ConformanceInfo conformanceInfo);
}
