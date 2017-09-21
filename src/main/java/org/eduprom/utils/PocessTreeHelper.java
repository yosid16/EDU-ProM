package org.eduprom.utils;

import nu.xom.Nodes;
import org.eduprom.exceptions.MiningException;
import org.jbpt.petri.untangling.Process;
import org.processmining.processtree.Block;
import org.processmining.processtree.Edge;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.impl.AbstractBlock;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Stream.concat;

public class PocessTreeHelper {

    private final Lock lock = new ReentrantLock();

    private Stream<Node> getAllRecursiveNodes(Node node){


        if (node instanceof AbstractBlock){
            Stream rec = ((AbstractBlock)node).getChildren().stream().flatMap(this::getAllRecursiveNodes);
            return concat(rec, Stream.of(node));
        }

        return Stream.of(node);
    }

    private Stream<Edge> getAllRecursiveEdges(Node node){

        if (node instanceof AbstractBlock){
            Stream<Edge> rec = ((AbstractBlock)node).getChildren().stream().flatMap(this::getAllRecursiveEdges);
            Stream<Edge> edgeStream = (((AbstractBlock) node).getOutgoingEdges()).stream();
            return Stream.concat(rec, edgeStream);
        }

        return Stream.empty();
    }

    /*
    public void merge(Node source, Node target) throws MiningException {
        if(source.toString().equals(target.toString())){
            return;
        }

        lock.lock();
        ProcessTree tree = target.getProcessTree();
        String pre = tree.toString();
        try{

            tree.addNode(source);
            source.getProcessTree().getNodes().forEach(x -> x.setProcessTree(tree));

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

            List<Node> nodes = getAllRecursiveNodes(target).collect(Collectors.toList());
            nodes.forEach(tree::removeNode);
            List<Edge> edges = getAllRecursiveEdges(target).collect(Collectors.toList());
            edges.forEach(tree::removeEdge);
            nodes.forEach(tree::removeNode);
        }
        finally {
            lock.unlock();
        }

        String post = tree.toString();
        if(pre.equals(post)){
            throw new MiningException("tree did not changed as a result of a tree merge operation");
        }
    }
    */

    public void merge(Node source, Node target) throws MiningException {
        lock.lock();
        ProcessTree tree = target.getProcessTree();
        String pre = tree.toString();
        tree.addNode(source);

        List<Node> replacesNodes = null;
        List<Edge> replacesEdges = null;
        try{
            if (target.isRoot()){
                tree.setRoot(source);
            }
            else {
                Collection<Block> replaceBlocks = target.getParents();
                if (replaceBlocks.size() != 1){
                    throw new MiningException("more than one replace blocks is not allowed.");
                }

                Block replaceBlock = replaceBlocks.stream().findAny().get();
                replaceBlock.swapChildAt(source, replaceBlock.getChildren().indexOf(target));

                /*
                List<Node> nodes = getAllRecursiveNodes(target).collect(Collectors.toList());
                nodes.forEach(tree::removeNode);
                List<Edge> edges = getAllRecursiveEdges(target).collect(Collectors.toList());
                edges.forEach(tree::removeEdge);
                */
            }
            //add all new nodes and edges
            List<Node> sourceNodes = getAllRecursiveNodes(source).collect(Collectors.toList());
            sourceNodes.forEach(x -> tree.addNode(x));
            List<Edge> sourceEdges = getAllRecursiveEdges(source).collect(Collectors.toList());
            sourceEdges.forEach(x->tree.addEdge(x));


            List<Node> nodes = getAllRecursiveNodes(target).collect(Collectors.toList());
            List<Edge> edges = getAllRecursiveEdges(target).collect(Collectors.toList());
            nodes.forEach(x->tree.removeNode(x));
            edges.forEach(x->tree.removeEdge(x));
            /*
            tree.getNodes().stream().filter(node -> !sourceNodes.contains(node)).collect(Collectors.toList())
                    .forEach(x -> tree.removeNode(x));
            tree.getEdges().stream().filter(edge -> !sourceEdges.contains(edge)).collect(Collectors.toList())
                    .forEach(x -> tree.removeEdge(x));
            */
        }
        finally {
            lock.unlock();
        }
        /*
        String post = tree.toString();
        if(source.toString() != target.toString() && pre.equals(post)){
            throw new MiningException("tree did not changed as a result of a tree merge operation");
        }*/
    }
    /*
    public void align(Node node){
        ProcessTree tree = node.getProcessTree();
        Stack<Node> toProcess = new Stack<Node>();
        toProcess.push(node);
        while(!toProcess.isEmpty()){
            Node currentNode = toProcess.pop();
            if


        }
    }*/
}
