package org.eduprom.Miners.Synthesis.Algorithms;

import org.eduprom.Miners.AbstractPetrinetMiner;
import org.eduprom.Miners.EnumerateAllPaths;
import org.eduprom.Miners.IProcessTreeMiner;
import org.eduprom.Miners.Synthesis.CutsMiners.ICutsProcessTreeMiner;
import org.eduprom.Miners.Synthesis.CutsMiners.InductiveCutMiner;
import org.eduprom.Miners.Synthesis.Entities.ProcessTreeCuts;
import javafx.util.Pair;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import org.deckfour.xes.model.XLog;
import org.processmining.plugins.InductiveMiner.mining.*;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.pnalignanalysis.conformance.AlignmentPrecGenRes;
import org.processmining.pnanalysis.metrics.impl.PetriNetNofTransitionsMetric;
import org.processmining.pnanalysis.models.PetriNetMetrics;
import org.processmining.processtree.Block;
import org.processmining.processtree.Edge;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.impl.AbstractBlock;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet;

import static org.processmining.ptconversions.pn.ProcessTree2Petrinet.PetrinetWithMarkings;

import java.util.*;

public class ConformanceTraversal extends AbstractPetrinetMiner {

	public ConformanceTraversal(String filename) throws Exception {
		super(filename);
		_parameters = new MiningParametersIM();
	}
	private final Lock lock = new ReentrantLock();
	private MiningParametersIM _parameters;

	private HashMap<UUID, XLog> _logs;
	private ProcessTree _originalTree;
	private PetrinetWithMarkings _res;

	@Override
	protected PetrinetWithMarkings TrainPetrinet() throws Exception {

		logger.info("Started mining a petri nets using ConformanceTraversale");
		ProcessTree synthesizedTree = ConformanceTraversale(new InductiveCutMiner(_filename),
				new IProcessTreeMiner[] { new EnumerateAllPaths(_filename) },
				_log);
		logger.info("Completed mining a petri nets using ConformanceTraversale");

		return _petrinetHelper.ConvertToPetrinet(synthesizedTree);
	}

	public ProcessTree ConformanceTraversale(ICutsProcessTreeMiner baseline,
											 IProcessTreeMiner[] optimizers,
											 XLog log) throws Exception {
		ProcessTreeCuts processTreeCuts = baseline.Mine(log);
		_res = _petrinetHelper.ConvertToPetrinet(processTreeCuts.processTree);
		logger.info(String.format("Performed baseline mining using %s", baseline.GetName()));

		logger.info(String.format("Psi: %s for baseline", Psi(_log, processTreeCuts.processTree)));

		_logs = processTreeCuts.logs;
		ProcessTree T = processTreeCuts.processTree;
		_originalTree = T.toTree();
		Traverse(T.getRoot(), optimizers);
		return T;
	}

	synchronized private void Traverse(Node node, IProcessTreeMiner[] optimizers) throws Exception {

		if (node.isLeaf() ){
			logger.log(Level.FINE, String.format("node %s is leaf, finished scanning path", node.getName()));
			return;
		}

		XLog log = _logs.get(node.getID());

		if (log == null){
			return;
		}

		_logHelper.PrintLog(Level.FINE, log);
		logger.log(Level.FINE, String.format("Traversing node: %s, Number of traces: %d", node, log.size()));

		ProcessTree currTree = node.getProcessTree();
		Pair<ProcessTree, Double> tAlt = ArgMax(log, optimizers);
		Node tAltRoot = tAlt.getKey().getRoot();
		ProcessTree newTree = _originalTree.toTree();
		Merge(tAltRoot, newTree.getNode(node.getID()));

		//logger.info(String.format("New tree has: %d nodes, and %d edges", newTree.getNodes().size(), newTree.size()));
		//String path = String.format("./Output/%s_%s_%s.png" , GetName(), Paths.get(_filename).getFileName(), newTree);
		//_visualizationHelper.Export(PetrinetHelper.ConvertToPetrinet(newTree).petrinet, path);

		double currValue = Psi(_log, currTree);
		double newValue = Psi(_log, newTree);
		logger.info(String.format("Node: %s, Current: %f, New: %f", node, currValue, newValue));
		if (newValue > currValue) { //node.toString().equalsIgnoreCase("XorLoop(D, E, tau)")
			logger.log(Level.FINE, String.format("replacing sub-tree: %s with %s", node, tAltRoot));
			Merge(tAltRoot, node);
		} else {
			logger.log(Level.FINE, String.format("skipped replacing sub-tree: %s with %s", node, tAltRoot));

			Edge[] outgoing = ((AbstractBlock) node).getOutgoingEdges().toArray(new Edge[0]);
			for(Edge e : outgoing){
				Traverse(e.getTarget(), optimizers);
			}

			/*
			Iterator<Edge> outgoing = ((AbstractBlock) node).getOutgoingEdges().stream().iterator();
			while(outgoing.hasNext()){
				Edge e = outgoing.next();
				Traverse(e.getTarget(), optimizers);
			}
			*/
		}
	}

	public void Merge(Node source, Node target) {
		try{
			lock.lock();
			ProcessTree tree = target.getProcessTree();
			tree.addNode(source);

			if (target.isRoot()){
				tree.setRoot(source);
				return;
			}

			for (Block b : target.getParents()){
				for(Edge e: target.getIncomingEdges()){
					b.removeOutgoingEdge(e);
				}
				b.addChild(source);
			}

			tree.removeNode(source);
		}
		finally {
			lock.unlock();
		}
	}



	public double Psi(XLog log, ProcessTree pt) throws Exception {
		ProcessTree2Petrinet.PetrinetWithMarkings res = _petrinetHelper.ConvertToPetrinet(pt);
		PNRepResult alignment = _petrinetHelper.getAlignment(log, res.petrinet, res.initialMarking, res.finalMarking);
		AlignmentPrecGenRes conformance = _petrinetHelper.getConformance(log, res.petrinet, alignment, res.initialMarking, res.finalMarking);
		PetriNetMetrics metrics = new PetriNetMetrics(_promPluginContext, res.petrinet, res.initialMarking);

		//double pCount = metrics.getMetricValue(PetriNetNofPlacesMetric.NAME);
		double tCount = metrics.getMetricValue(PetriNetNofTransitionsMetric.NAME);

		double simplicity = Math.sqrt(Math.min(  1 / tCount, 1.0));
		//org.processmining.pnanalysis.plugins.PetriNetMetricsPlugin
		//logger.info(String.format("sim: %s, %s, %s, %s",
		//		metrics.getMetricValue(PetriNetStructurednessMetric.NAME),
		//		metrics.getMetricValue(PetriNetNofPlacesMetric.NAME),
		//		metrics.getMetricValue(PetriNetNofTransitionsMetric.NAME),
		//		metrics.getMetricValue(PetriNetNofArcsMetric.NAME)));
		logger.info(String.format("Simplicity: %s", simplicity));

		return 0.4 * conformance.getPrecision() + 0.4 *conformance.getGeneralization() + 0.2 * simplicity;
		//return Double.parseDouble(res.getInfo().get("Move-Model Fitness").toString());
	}

	public Pair<ProcessTree, Double> ArgMax(XLog log, IProcessTreeMiner[] optimizers) throws Exception {
		Double bestProcessTreeResult = -1.0;
		ProcessTree bestProcessTree = null;
		for(IProcessTreeMiner m : optimizers){
			ProcessTree pt = m.Mine(log);
			double value = Psi(log, pt);
			if (value > bestProcessTreeResult){
				bestProcessTreeResult = value;
				bestProcessTree = pt;
			}
		}

		return new Pair(bestProcessTree, bestProcessTreeResult);
	}
}