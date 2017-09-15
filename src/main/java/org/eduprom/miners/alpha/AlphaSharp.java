package org.eduprom.miners.alpha;

import org.eduprom.exceptions.LogFileNotFoundException;
import org.processmining.alphaminer.parameters.AlphaVersion;

public class AlphaSharp extends Alpha {

    public AlphaSharp(String filename) throws LogFileNotFoundException {
        super(filename);
    }

    @Override
    public AlphaVersion getVersion(){
        return AlphaVersion.SHARP;
    }
}
