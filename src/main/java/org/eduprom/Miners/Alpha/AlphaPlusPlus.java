package org.eduprom.Miners.Alpha;

import org.processmining.alphaminer.parameters.AlphaVersion;

/**
 * Created by ydahari on 4/12/2017.
 */
public class AlphaPlusPlus extends Alpha {

    public AlphaPlusPlus(String filename) throws Exception {
        super(filename);
    }

    public AlphaVersion GetVersion(){
        return AlphaVersion.PLUS_PLUS;
    }
}
