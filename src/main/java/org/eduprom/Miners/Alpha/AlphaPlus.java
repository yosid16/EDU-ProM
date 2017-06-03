package org.eduprom.Miners.Alpha;

import org.processmining.alphaminer.parameters.AlphaVersion;

/**
 * Created by ydahari on 4/12/2017.
 */
public class AlphaPlus extends Alpha {

    public AlphaPlus(String filename) throws Exception {
        super(filename);
    }

    public AlphaVersion GetVersion(){
        return AlphaVersion.PLUS;
    }
}
