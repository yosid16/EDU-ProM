package org.eduprom.Miners.Matching;

import org.deckfour.xes.model.XLog;
import org.eduprom.Miners.IProcessTreeMiner;
import org.eduprom.Partitioning.Partitioning;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by ydahari on 6/24/2017.
 */
public class NaiveMatching {

    public class MatchingResult{
        private IProcessTreeMiner matchingProcessTreeMiner;

        private ProcessTree matchingProcessTree;

        public MatchingResult(IProcessTreeMiner matchingProcessTreeMiner, ProcessTree matchingProcessTree){
            this.matchingProcessTreeMiner = matchingProcessTreeMiner;
            this.matchingProcessTree = matchingProcessTree;
        }

        public IProcessTreeMiner getMatchingProcessTreeMiner() {
            return matchingProcessTreeMiner;
        }

        public ProcessTree getMatchingProcessTree() {
            return matchingProcessTree;
        }
    }


    public Map<XLog, MatchingResult> match(Partitioning partitioning, List<IProcessTreeMiner> candidates) throws Exception {
        Map<Node, XLog> partitions = partitioning.getExclusiveLogs();
        Map<XLog, MatchingResult> matching = new HashMap<XLog, MatchingResult>();
        for(Map.Entry<Node, XLog> entry : partitions.entrySet()){
            double bestPsi = 0;
            double currPsi = -1;
            ProcessTree bestModel = null;
            IProcessTreeMiner bestMiner = null;

            for(IProcessTreeMiner miner : candidates){
                ProcessTree processTree = miner.Mine(entry.getValue());
                currPsi = psi(processTree, entry.getValue());
                if (currPsi > bestPsi){
                    bestModel = processTree;
                    bestPsi = currPsi;
                    bestMiner = miner;
                }
            }

            matching.put(entry.getValue(), new MatchingResult(bestMiner, bestModel));
        }

        return matching;
    }




    public double psi(ProcessTree tree, XLog log){
        return 1;
    }
}
