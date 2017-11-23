///*
//package org.eduprom.partitioning.trunk;
//
//import gnu.trove.set.hash.THashSet;
//import org.deckfour.xes.classification.XEventClass;
//import org.deckfour.xes.model.XEvent;
//import org.deckfour.xes.model.XLog;
//import org.deckfour.xes.model.XTrace;
//import org.deckfour.xes.model.impl.XAttributeMapImpl;
//import org.deckfour.xes.model.impl.XLogImpl;
//import org.deckfour.xes.model.impl.XTraceImpl;
//import org.processmining.plugins.InductiveMiner.MultiSet;
//import org.processmining.plugins.InductiveMiner.Sets;
//import org.processmining.plugins.InductiveMiner.dfgOnly.Dfg;
//import org.processmining.plugins.InductiveMiner.dfgOnly.dfgCutFinder.DfgCutFinder;
//import org.processmining.plugins.InductiveMiner.dfgOnly.dfgCutFinder.DfgCutFinderSimple;
//import org.processmining.plugins.InductiveMiner.jobList.JobList;
//import org.processmining.plugins.InductiveMiner.jobList.JobListConcurrent;
//import org.processmining.plugins.InductiveMiner.mining.IMLogInfo;
////import org.processmining.plugins.InductiveMiner.mining.Miner;
//import org.processmining.plugins.InductiveMiner.mining.MinerState;
//import org.processmining.plugins.InductiveMiner.mining.cuts.Cut;
//import org.processmining.plugins.InductiveMiner.mining.fallthrough.*;
//import org.processmining.plugins.InductiveMiner.mining.logSplitter.LogSplitter;
//import org.processmining.plugins.InductiveMiner.mining.logs.IMLog;
//import org.processmining.plugins.InductiveMiner.mining.logs.IMLogImpl;
//import org.processmining.plugins.InductiveMiner.mining.logs.IMTrace;
//import org.processmining.plugins.InductiveMiner.mining.logs.LifeCycles;
//import org.processmining.processtree.Block;
//import org.processmining.processtree.Node;
//import org.processmining.processtree.ProcessTree;
//import org.processmining.processtree.impl.AbstractBlock;
//import org.processmining.processtree.impl.AbstractTask;
//
//import java.util.*;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.atomic.AtomicBoolean;
//
//public class FallThroughPartitioningIM implements FallThrough {
//
//    public static class FallThroughActivityOncePerTraceConcurrentPartitioning implements FallThrough {
//
//        private final boolean strict;
//
//        */
///**
//         *
//         * @param strict
//         *            Denotes whether this case is applied strictly, i.e. true =
//         *            only apply if each trace contains precisely one activity;
//         *            false = apply it also if it's close enough.
//         *//*
//
//        public FallThroughActivityOncePerTraceConcurrentPartitioning(boolean strict) {
//            this.strict = strict;
//        }
//
//        public Node fallThrough(IMLog log, IMLogInfo logInfo, ProcessTree tree, MinerState minerState) {
//            if (logInfo.getActivities().toSet().size() > 1) {
//
//                Collection<XEventClass> activities = logInfo.getActivities().sortByCardinality();
//                for (XEventClass activity : activities) {
//
//				*/
///*
//				 * An arbitrary parallel cut is always possible. However, to
//				 * save precision we only want to split here if this activity
//				 * occurs precisely once in each trace.
//				 *//*
//
//
//                    long cardinality = logInfo.getActivities().getCardinalityOf(activity);
//                    long epsilon = logInfo.getDfg().getNumberOfEmptyTraces();
//                    boolean x = epsilon == 0 && cardinality == log.size();
//
//                    double noise = minerState.parameters.getNoiseThreshold();
//                    double avg = cardinality / log.size();
//                    double reverseNoise = noise == 1 ? Double.MAX_VALUE : 1 / (1 - noise);
//                    boolean y = epsilon < log.size() * noise && avg > 1 - noise && avg < reverseNoise;
//
//                    if (x || (!strict && y)) {
//
//                        AdaMiner.debug(" fall through: leave out one-per-trace activity", minerState);
//
//                        //create cut
//                        Set<XEventClass> sigma0 = new THashSet<>();
//                        sigma0.add(activity);
//                        Set<XEventClass> sigma1 = new THashSet<>(activities);
//                        sigma1.remove(activity);
//                        List<Set<XEventClass>> partition = new ArrayList<Set<XEventClass>>();
//                        partition.add(sigma0);
//                        partition.add(sigma1);
//                        Cut cut = new Cut(Cut.Operator.concurrent, partition);
//
//                        //split log
//                        LogSplitter.LogSplitResult logSplitResult = minerState.parameters.getLogSplitter().split(log, logInfo, cut,
//                                minerState);
//                        if (minerState.isCancelled()) {
//                            return null;
//                        }
//                        IMLog log1 = logSplitResult.sublogs.get(0);
//                        IMLog log2 = logSplitResult.sublogs.get(1);
//
//                        //construct node
//                        Block newNode = new AbstractBlock.And("");
//                        AdaMiner.addNode(tree, newNode);
//
//                        //recurse
//                        Node child1 = AdaMiner.mineNode(log1, tree, minerState);
//                        AdaMiner.addChild(newNode, child1, minerState);
//
//                        Node child2 = AdaMiner.mineNode(log2, tree, minerState);
//                        AdaMiner.addChild(newNode, child2, minerState);
//
//                        return newNode;
//                    }
//                }
//            }
//            return null;
//        }
//    }
//
//    public static class FallThroughActivityConcurrentPartitioning implements FallThrough {
//
//	*/
///*
//	 * (non-Javadoc)
//	 *
//	 * @see
//	 * org.processmining.plugins.InductiveMiner.mining.fallthrough.FallThrough
//	 * #fallThrough(org.processmining.plugins.InductiveMiner.mining.IMLog,
//	 * org.processmining.plugins.InductiveMiner.mining.IMLogInfo,
//	 * org.processmining.processtree.ProcessTree,
//	 * org.processmining.plugins.InductiveMiner.mining.MiningParameters)
//	 *
//	 * Try to leave out an activity and recurse If this works, then putting the
//	 * left out activity in parallel is fitness-preserving
//	 *//*
//
//
//        public FallThroughActivityConcurrentPartitioning() {
//
//        }
//
//        private class CutWrapper {
//            Cut cut = null;
//        }
//
//        public Node fallThrough(final IMLog log, final IMLogInfo logInfo, ProcessTree tree, final MinerState minerState) {
//
//            if (logInfo.getActivities().toSet().size() < 3) {
//                return null;
//            }
//
//            //leave out an activity
//            final DfgCutFinder dfgCutFinder = new DfgCutFinderSimple();
//            final AtomicBoolean found = new AtomicBoolean(false);
//            final FallThroughActivityConcurrentPartitioning.CutWrapper cutWrapper = new FallThroughActivityConcurrentPartitioning.CutWrapper();
//
//            JobList jobList = new JobListConcurrent(minerState.getMinerPool());
//
//            for (XEventClass leaveOutActivity : logInfo.getActivities()) {
//                //leave out a single activity and try whether that gives a valid cut
//
//                final XEventClass leaveOutActivity2 = leaveOutActivity;
//                jobList.addJob(new Runnable() {
//                    public void run() {
//
//                        if (minerState.isCancelled()) {
//                            return;
//                        }
//
//                        if (!found.get()) {
//
//                            //in a typical overcomplicated java-way, create a cut (parallel, [{a}, Sigma\{a}])
//                            Set<XEventClass> leaveOutSet = new THashSet<XEventClass>();
//                            leaveOutSet.add(leaveOutActivity2);
//                            List<Set<XEventClass>> partition = new ArrayList<Set<XEventClass>>();
//                            partition.add(leaveOutSet);
//                            partition.add(Sets.complement(leaveOutSet, logInfo.getActivities().toSet()));
//                            Cut cut = new Cut(Cut.Operator.concurrent, partition);
//
//                            AdaMiner.debug("  try cut " + cut, minerState);
//
//                            //see if a cut applies
//                            //for performance reasons, only on the directly-follows graph
//                            Cut cut2 = dfgCutFinder.findCut(logInfo.getDfg(), null);
//
//                            if (minerState.isCancelled()) {
//                                return;
//                            }
//
//                            if (cut2 != null && cut2.isValid()) {
//                                //see if we are first
//                                boolean oldFound = found.getAndSet(true);
//                                if (!oldFound) {
//                                    //we were first
//                                    cutWrapper.cut = cut;
//                                }
//                            }
//                        }
//                    }
//                });
//            }
//
//            try {
//                jobList.join();
//            } catch (ExecutionException e) {
//                e.printStackTrace();
//                return null;
//            }
//
//            if (found.get() && !minerState.isCancelled()) {
//                //the cut we made is a valid one; split the log, construct the parallel construction and recurse
//
//                AdaMiner.debug(" fall through: leave out activity", minerState);
//
//                LogSplitter.LogSplitResult logSplitResult = minerState.parameters.getLogSplitter().split(log, logInfo, cutWrapper.cut,
//                        minerState);
//                if (minerState.isCancelled()) {
//                    return null;
//                }
//                IMLog log1 = logSplitResult.sublogs.get(0);
//                IMLog log2 = logSplitResult.sublogs.get(1);
//
//                Block newNode = new AbstractBlock.And("");
//                AdaMiner.addNode(tree, newNode);
//
//                Node child1 = AdaMiner.mineNode(log1, tree, minerState);
//                AdaMiner.addChild(newNode, child1, minerState);
//
//                Node child2 = AdaMiner.mineNode(log2, tree, minerState);
//                AdaMiner.addChild(newNode, child2, minerState);
//
//                return newNode;
//            } else {
//                return null;
//            }
//        }
//    }
//
//    public static class FallThroughTauLoopStrictPartitioning implements FallThrough {
//
//        public Node fallThrough(IMLog log, IMLogInfo logInfo, ProcessTree tree, MinerState minerState) {
//
//            if (logInfo.getActivities().toSet().size() > 1) {
//
//                //try to find a tau loop
//                IMLog sublog = log.clone();
//                if (filterLog(sublog, logInfo.getDfg())) {
//
//                    AdaMiner.debug(" fall through: tau loop strict", minerState);
//                    //making a tau loop split makes sense
//                    Block loop = new AbstractBlock.XorLoop("");
//                    AdaMiner.addNode(tree, loop);
//
//                    {
//                        Node body = AdaMiner.mineNode(sublog, tree, minerState);
//                        AdaMiner.addChild(loop, body, minerState);
//                    }
//
//                    {
//                        Node redo = new AbstractTask.Automatic("tau");
//                        AdaMiner.addNode(tree, redo);
//                        AdaMiner.addChild(loop, redo, minerState);
//                    }
//
//                    {
//                        Node exit = new AbstractTask.Automatic("tau");
//                        AdaMiner.addNode(tree, exit);
//                        AdaMiner.addChild(loop, exit, minerState);
//                    }
//
//                    return loop;
//                }
//            }
//
//            return null;
//        }
//
//        */
///**
//         * Split the trace on a crossing end -> start
//         *
//         * @param log
//         * @param dfg
//         * @return
//         *//*
//
//        public static boolean filterLog(IMLog log, Dfg dfg) {
//            boolean somethingSplit = false;
//
//            for (IMTrace trace : log) {
//                boolean lastEnd = false;
//                IMTrace.IMEventIterator it = trace.iterator();
//                while (it.hasNext()) {
//
//                    it.next();
//
//                    XEventClass activity = it.classify();
//
//                    if (lastEnd && dfg.isStartActivity(activity)) {
//                        it.split();
//                        somethingSplit = true;
//                    }
//
//                    lastEnd = dfg.isEndActivity(activity);
//                }
//            }
//            return somethingSplit;
//        }
//    }
//
//    public static class FallThroughTauLoopPartitioning implements FallThrough {
//
//        private final boolean useLifeCycle;
//
//        */
///**
//         *
//         * @param useLifeCycle
//         *            Denotes whether activity instances (i.e. combination of start
//         *            & a complete event) should be kept together at all times. True
//         *            = keep activity instances together; false = activity instances
//         *            may be split.
//         *//*
//
//        public FallThroughTauLoopPartitioning(boolean useLifeCycle) {
//            this.useLifeCycle = useLifeCycle;
//        }
//
//        public Node fallThrough(IMLog log, IMLogInfo logInfo, ProcessTree tree, MinerState minerState) {
//
//            if (logInfo.getActivities().toSet().size() > 1) {
//
//                //try to find a tau loop
//                XLog sublog = new XLogImpl(new XAttributeMapImpl());
//
//                for (IMTrace trace : log) {
//                    filterTrace(log, sublog, trace, logInfo.getDfg(), useLifeCycle);
//                }
//
//                if (sublog.size() > log.size()) {
//                    AdaMiner.debug(" fall through: tau loop", minerState);
//                    //making a tau loop split makes sense
//                    Block loop = new AbstractBlock.XorLoop("");
//                    AdaMiner.addNode(tree, loop);
//
//                    {
//                        Node body = AdaMiner.mineNode(new IMLogImpl(sublog, log.getClassifier()), tree, minerState);
//                        AdaMiner.addChild(loop, body, minerState);
//                    }
//
//                    {
//                        Node redo = new AbstractTask.Automatic("tau");
//                        AdaMiner.addNode(tree, redo);
//                        AdaMiner.addChild(loop, redo, minerState);
//                    }
//
//                    {
//                        Node exit = new AbstractTask.Automatic("tau");
//                        AdaMiner.addNode(tree, exit);
//                        AdaMiner.addChild(loop, exit, minerState);
//                    }
//
//                    return loop;
//                }
//            }
//
//            return null;
//        }
//
//        public static void filterTrace(IMLog log, XLog sublog, IMTrace trace, Dfg dfg, boolean useLifeCycle) {
//            boolean first = true;
//            XTrace partialTrace = new XTraceImpl(new XAttributeMapImpl());
//
//            MultiSet<XEventClass> openActivityInstances = new MultiSet<>();
//
//            for (XEvent event : trace) {
//
//                XEventClass activity = log.classify(trace, event);
//
//                if (!first && dfg.isStartActivity(activity)) {
//                    //we discovered a transition body -> body
//                    //check whether there are no open activity instances
//                    if (!useLifeCycle || openActivityInstances.size() == 0) {
//                        sublog.add(partialTrace);
//                        partialTrace = new XTraceImpl(new XAttributeMapImpl());
//                        first = true;
//                    }
//                }
//
//                if (useLifeCycle) {
//                    if (log.getLifeCycle(event) == LifeCycles.Transition.complete) {
//                        if (openActivityInstances.getCardinalityOf(activity) > 0) {
//                            openActivityInstances.remove(activity, 1);
//                        }
//                    } else if (log.getLifeCycle(event) == LifeCycles.Transition.start) {
//                        openActivityInstances.add(log.classify(trace, event));
//                    }
//                }
//
//                partialTrace.add(event);
//                first = false;
//            }
//            sublog.add(partialTrace);
//        }
//    }
//
//    public static class FallThroughFlowerWithoutEpsilonPartitioning implements FallThrough {
//
//        public Node fallThrough(IMLog log, IMLogInfo logInfo, ProcessTree tree, MinerState minerState) {
//
//            if (logInfo.getDfg().getNumberOfEmptyTraces() != 0) {
//                return null;
//            }
//
//            AdaMiner.debug(" fall through: flower model", minerState);
//
//            Block loopNode = new AbstractBlock.XorLoop("");
//            AdaMiner.addNode(tree, loopNode);
//
//            //body: xor/activity
//            Block xorNode;
//            if (logInfo.getActivities().setSize() == 1) {
//                xorNode = loopNode;
//            } else {
//                xorNode = new AbstractBlock.Xor("");
//                AdaMiner.addNode(tree, xorNode);
//                AdaMiner.addChild(loopNode, xorNode, minerState);
//            }
//
//            for (XEventClass activity : logInfo.getActivities()) {
//                Node child = new AbstractTask.Manual(activity.toString());
//                AdaMiner.addNode(tree, child);
//                AdaMiner.addChild(xorNode, child, minerState);
//            }
//
//            //redo: tau
//            Node body = new AbstractTask.Automatic("tau");
//            AdaMiner.addNode(tree, body);
//            AdaMiner.addChild(loopNode, body, minerState);
//
//            //exit: tau
//            Node tau2 = new AbstractTask.Automatic("tau");
//            AdaMiner.addNode(tree, tau2);
//            AdaMiner.addChild(loopNode, tau2, minerState);
//
//            return loopNode;
//        }
//
//    }
//
//    public static class FallThroughFlowerWithEpsilonPartitioning implements FallThrough {
//
//        public Node fallThrough(IMLog log, IMLogInfo logInfo, ProcessTree tree, MinerState minerState) {
//
//            AdaMiner.debug(" fall through: flower model", minerState);
//
//            Block loopNode = new AbstractBlock.XorLoop("");
//            AdaMiner.addNode(tree, loopNode);
//
//            //body: tau
//            Node body = new AbstractTask.Automatic("tau");
//            AdaMiner.addNode(tree, body);
//            AdaMiner.addChild(loopNode, body, minerState);
//
//            //redo: xor/activity
//            Block xorNode;
//            if (logInfo.getActivities().setSize() == 1) {
//                xorNode = loopNode;
//            } else {
//                xorNode = new AbstractBlock.Xor("");
//                AdaMiner.addNode(tree, xorNode);
//                AdaMiner.addChild(loopNode, xorNode, minerState);
//            }
//
//            for (XEventClass activity : logInfo.getActivities()) {
//                Node child = new AbstractTask.Manual(activity.toString());
//                AdaMiner.addNode(tree, child);
//                AdaMiner.addChild(xorNode, child, minerState);
//            }
//
//            Node tau2 = new AbstractTask.Automatic("tau");
//            AdaMiner.addNode(tree, tau2);
//            AdaMiner.addChild(loopNode, tau2, minerState);
//
//            return loopNode;
//        }
//    }
//
//
//    private static List<FallThrough> fallThroughs = new ArrayList<FallThrough>(Arrays.asList(
//            new FallThroughActivityOncePerTraceConcurrentPartitioning(true),
//            new FallThroughActivityConcurrentPartitioning(),
//            new FallThroughTauLoopStrictPartitioning(),
//            new FallThroughTauLoopPartitioning(false),
//            new FallThroughFlowerWithoutEpsilonPartitioning(),
//            new FallThroughFlowerWithEpsilonPartitioning()
//    ));
//
//    public Node fallThrough(IMLog log, IMLogInfo logInfo, ProcessTree tree, MinerState minerState) {
//        Node n = null;
//        Iterator<FallThrough> it = fallThroughs.iterator();
//        while (n == null && it.hasNext()) {
//
//            if (minerState.isCancelled()) {
//                return null;
//            }
//
//            n = it.next().fallThrough(log, logInfo, tree, minerState);
//        }
//        return n;
//    }
//}*/
