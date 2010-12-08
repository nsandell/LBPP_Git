package bn.impl;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import bn.BNException;
import bn.IBayesNode;
import bn.impl.InternalIBayesNode;
import bn.IBayesNet.RunResults;
import bn.distributions.Distribution.SufficientStatistic;

abstract class BayesianNetwork<BaseNodeType extends InternalIBayesNode> {

	protected BayesianNetwork(){}
	
	public void validate() throws BNException
	{
		HashSet<InternalIBayesNode> marks = new HashSet<InternalIBayesNode>();
		HashSet<InternalIBayesNode> ancestors = new HashSet<InternalIBayesNode>(); // Can we replace this, we don't need value..
		
		for(BaseNodeType node : nodes.values())
		{
			// Depth first search to make sure we've no cycles.
			if(!marks.contains(node))
					this.dfs_cycle_detect(marks,ancestors,node);
				
			// Node should validate its CPT matches its parents, etc.
			node.validate();
		}
	}
	
	public void print(PrintStream ps)
	{
		for(BaseNodeType nd : this.nodes.values())
			nd.print(ps);
	}
	
	public void print()
	{
		this.print(System.out);
	}
	
	public void printDistributionInfo(String name, PrintStream ps) throws BNException
	{
		BaseNodeType node = this.getNode(name);
		if(node==null) throw new BNException("Attempted to print distribution info for a nonexistant node named " + name);
		node.printDistributionInfo(ps);
	}
	
	public void sample() throws BNException
	{
		ArrayList<InternalIBayesNode> frontier = new ArrayList<InternalIBayesNode>();
		for(InternalIBayesNode node : nodes.values())
		{
			if(node.numParents()==0)
				frontier.add(node);
		}
		frontierSample(new HashSet<InternalIBayesNode>(), frontier);
	}
	
	private void frontierSample(HashSet<InternalIBayesNode> marks, ArrayList<InternalIBayesNode> frontier) throws BNException
	{
		ArrayList<InternalIBayesNode> newFrontier = new ArrayList<InternalIBayesNode>();
		for(InternalIBayesNode node : frontier)
		{
			node.sample();
			marks.add(node);
			for(InternalIBayesNode child : node.getChildrenI())
			{
				if(!marks.contains(child))
					newFrontier.add(child);
			}
		}
		if(newFrontier.size()>0)
			frontierSample(marks, newFrontier);
	}
	
	public void clearAllEvidence()
	{
		for(InternalIBayesNode node : this.nodes.values())
			node.clearEvidence();
	}
	
	public void resetMessages()
	{
		for(BaseNodeType nd : this.nodes.values())
			nd.resetMessages();
	}
	
	public double getLogLikelihood() throws BNException
	{
		double esum = 0;
		for(BaseNodeType nd : this.nodes.values())
			esum -= nd.betheFreeEnergy();
		return esum;
	}
	
	
	public RunResults optimize(int maxLearnIt, double learnErrConvergence, int maxInfIt, double infErrConvergence) throws BNException
	{
		long startTime = System.currentTimeMillis();
		int i = 0;
		double learnErr = 0;
		while(i < maxLearnIt)
		{
			learnErr = 0;
			this.run(maxInfIt, infErrConvergence);
			for(BaseNodeType node : nodes.values())
			{
				learnErr = Math.max(node.optimizeParameters(),learnErr);
			}
			if(learnErr <= learnErrConvergence)
				break;
			i++;
		}
		return new RunResults(i, ((double)(System.currentTimeMillis()-startTime))/1000.0, learnErr);
	}
	
	private void dfs_cycle_detect(HashSet<InternalIBayesNode> marks, HashSet<InternalIBayesNode> ancestors, InternalIBayesNode current) throws BNException
	{
		ancestors.add(current);
		for(InternalIBayesNode child : current.getChildrenI())
		{
			if(marks.contains(child))
				continue;
			if(ancestors.contains(child))
				throw new BNException("Bayesian network is cyclic!");
			dfs_cycle_detect(marks, ancestors, child);
		}
		marks.add(current);
		ancestors.remove(current);
	}
	
	public final BaseNodeType getNode(String name)
	{
		return this.nodes.get(name);
	}
	
	public void removeNode(IBayesNode node) throws BNException
	{
		this.removeNode(node.getName());
	}
	
	public void removeNode(String name) throws BNException
	{
		BaseNodeType node = nodes.get(name);
		if(node!=null)
		{
			this.removeNodeI(node);
			this.nodes.remove(name);
		}
	}
	
	public int numNodes()
	{
		return this.nodes.size();
	}
	
	protected final void addNodeI(BaseNodeType node) throws BNException
	{
		if(this.nodes.get(node.getName())!=null)
			throw new BNException("Attempted to add node with name " + node.getName() + " where it already exists.");
		nodes.put(node.getName(), node);
	}
	
	public Iterable<String> getNodeNames()
	{
		return this.nodes.keySet();
	}
	
	public Iterable<BaseNodeType> getNodes()
	{
		return this.nodes.values();
	}
	
	public void setNodeOrder(Iterable<String> nodeOrder)
	{
		this.nodeOrder = nodeOrder;
	}
	
	public RunResults run(int maxit, double conv) throws BNException
	{
		long start_time = System.currentTimeMillis();
		double err = Double.POSITIVE_INFINITY;
	
		int i;
		for(i = 0; i < maxit && err > conv; i++)
		{
			err = 0;
			if(nodeOrder==null)
				nodeOrder = nodes.keySet();
			for(String nodeName: nodes.keySet())
			{
				BaseNodeType node = nodes.get(nodeName);
				try{err = Math.max(err,node.updateMessages());}
				catch(BNException e){throw new BNException("Node " + nodeName + " threw an exception while updating : ",e);}
			}
		}
		if(nodeOrder==null)
			nodeOrder = nodes.keySet();
		long end_time = System.currentTimeMillis();
		return new RunResults(i, ((double)(end_time-start_time))/1000.0, err);
	}
	
	public void clearEvidence(String nodeName) throws BNException
	{
		BaseNodeType node = this.getNode(nodeName);
		if(node==null)
			throw new BNException("Attempted to clear node evidence from node " + nodeName + " - does note exist.");
		node.clearEvidence();
	}
	
	public void collectSufficientStatistics(Iterable<String> nodeNames, HashMap<String,SufficientStatistic> stats) throws BNException
	{
		for(String nodename : nodeNames)
		{
			BaseNodeType node = this.getNode(nodename);
			SufficientStatistic stat = stats.get(nodename);
			if(stat==null)
				stats.put(nodename, node.getSufficientStatistic());
			else
				stats.get(nodename).update(node.getSufficientStatistic());
		}
	}
	
	public void optimize(Iterable<String> nodeNames, HashMap<String,SufficientStatistic> stats) throws BNException
	{
		for(String nodename : nodeNames)
		{
			BaseNodeType node = nodes.get(nodename);
			SufficientStatistic stat = stats.get(nodename);
			if(node==null)
				throw new BNException("Cannot optimize, node " + nodename + " does not exist.");
			if(stat==null)
				throw new BNException("Cannot optimize node " + nodename + ", not given sufficient statistic for it.");
			node.optimizeParameters(stat);
		}
	}
	
	
	protected abstract void removeNodeI(BaseNodeType node) throws BNException;
	private Iterable<String> nodeOrder = null;
	protected HashMap<String, BaseNodeType> nodes = new HashMap<String, BaseNodeType>();
}
