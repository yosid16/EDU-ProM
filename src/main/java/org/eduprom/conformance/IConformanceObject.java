package org.eduprom.conformance;

import org.eduprom.miners.adaptiveNoise.ConformanceInfo;

public interface IConformanceObject {

    ConformanceInfo getConformanceInfo();

    void setConformanceInfo(ConformanceInfo conformanceInfo);
}
