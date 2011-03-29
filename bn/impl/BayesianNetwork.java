package bn.impl;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import bn.BNException;
import bn.IBayesNode;
import bn.impl.InternalIBayesNode;
import bn.IBayesNet.RunResults;
import bn.distributions.Distribution.SufficientStatistic;

public abstract class BayesianNetwork<BaseNodeType extends InternalIBayesNode> {
	
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

	@Override
	public String toString()
	{
		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(boas,true);
		this.print(ps);
		return boas.toString();
	}
	
	public void print(PrintStream ps)
	{
		ps.print(this.getDefinition());
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
	
	public String getDefinition()
	{
		String str = this.getDefinitionFileHeader();
		
		for(BaseNodeType type : this.nodes.values())
			str+=type.getNodeDefinition();
		for(BaseNodeType type : this.nodes.values())
			str+=type.getEdgeDefinition();
		return str;
	}
	
	protected abstract String getDefinitionFileHeader();
	
	public RunResults optimize(int maxLearnIt, double learnErrConvergence, int maxInfIt, double infErrConvergence) throws BNException
	{
		long startTime = System.currentTimeMillis();
		int i = 0;
		double learnErr = 0;
		while(i < maxLearnIt)
		{
			learnErr = 0;
			this.run(maxInfIt, infErrConvergence);
			for(BaseNodeType node : this.nodes.values())
				learnErr = Math.max(node.optimizeParameters(), learnErr);
			if(learnErr <= learnErrConvergence)
				break;
			i++;
		}
		return new RunResults(i, ((double)(System.currentTimeMillis()-startTime))/1000.0, learnErr);
	}
	
	public RunResults optimize_parallel_queue(int maxLearnIt, double learnErrConvergence, int maxInfIt, double infErrConvergence) throws BNException
	{
		long startTime = System.currentTimeMillis();
		int i = 0;
		double learnErr = 0;
		while(i < maxLearnIt)
		{
			learnErr = 0;
			this.run_parallel_queue(maxInfIt, infErrConvergence);
			for(BaseNodeType node : this.nodes.values())
				learnErr = Math.max(node.optimizeParameters(), learnErr);
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
	
	public RunResults run(int maxit, double conv) throws BNException
	{
		long start_time = System.currentTimeMillis();
		double err = Double.POSITIVE_INFINITY;
	
		int i;
		for(i = 0; i < maxit && err > conv; i++)
		{
			err = 0;
			for(String nodeName: nodes.keySet())
			{
				BaseNodeType node = nodes.get(nodeName);
				try{err = Math.max(err,node.updateMessages());}
				catch(BNException e){throw new BNException("Node " + nodeName + " threw an exception while updating : ",e);}
			}
		}
		long end_time = System.currentTimeMillis();
		return new RunResults(i, ((double)(end_time-start_time))/1000.0, err);
	}
	
	public RunResults run_parallel_queue(int maxit, double conv) throws BNException
	{
		long t0 = System.currentTimeMillis();
		double error = Double.POSITIVE_INFINITY;
		int i;
		for(i = 1; i <= maxit && error > conv; i++)
		{
			NodeRunnerSync sync = new NodeRunnerSync(this.nodes.values());
			synchronized (sync) {
				sync.go();
				try {
				sync.wait();
				} catch(InterruptedException e){throw new BNException("Error, interrupted during run: " + e.toString());}
				error = sync.error;
			}
		}
		long tf = System.currentTimeMillis();
		return new RunResults(i, ((double)(tf-t0))/1000.0, error);
	}
	
	public RunResults run_parallel_queue(int maxit, double conv,Iterable<String> nodeNames) throws BNException
	{
		Vector<BaseNodeType> nodes = new Vector<BaseNodeType>();
		for(String nodeName : nodeNames)
			nodes.add(this.getNode(nodeName));
		long t0 = System.currentTimeMillis();
		double error = Double.POSITIVE_INFINITY;
		int i;
		for(i = 1; i <= maxit && error > conv; i++)
		{
			NodeRunnerSync sync = new NodeRunnerSync(nodes);
			synchronized (sync) {
				sync.go();
				try {
				sync.wait();
				} catch(InterruptedException e){throw new BNException("Error, interrupted during run: " + e.toString());}
				error = sync.error;
			}
		}
		long tf = System.currentTimeMillis();
		return new RunResults(i, ((double)(tf-t0))/1000.0, error);
	}
	
	private class NodeRunner extends Thread
	{
		NodeRunner(NodeRunnerSync sync)
		{
			this.sync = sync;
		}
		
		public void run()
		{
			InternalIBayesNode nd = sync.nextNode(null,0.0);
			try 
			{
				while(nd!=null)
					nd = sync.nextNode(nd, nd.updateMessages());
				sync.finish();
			} catch(BNException e) {
				this.sync.fault(e.toString());
			}
		}
		NodeRunnerSync sync;
	}
	// This is both a thread pool and keeps track of which nodes have neighbors in progress 
	private class NodeRunnerSync
	{
		NodeRunnerSync(Iterable<BaseNodeType> nodesit)
		{
			for(BaseNodeType nd : nodesit)
			{
				this.blocks.put(nd, 0);
				this.remainingNodes.add(nd);
			}
		}

		synchronized void go()
		{
			for(int i = 0; i < this.numThreads; i++)
			{
				this.activeThreads++;
				//System.err.println("Spawning, " + this.activeThreads + " threads active.");
				(new NodeRunner(this)).start();
			}
		}
		
		synchronized void finish()
		{
			this.activeThreads--;
			//System.err.println("Done, " + this.activeThreads + " remaining.");
			if(this.activeThreads==0)
				this.notify();
		}

		synchronized void fault(String errMsg)
		{
			this.fault = true;
			this.faultString = errMsg;
			synchronized(this.remainingNodes) {
				this.remainingNodes.clear();
			}
			this.notify();
		}
		
		InternalIBayesNode nextNode(InternalIBayesNode finishedNode, double err)
		{
			synchronized (this.remainingNodes) {
				
				this.error = Math.max(this.error,err);
				
				// Clear the locks from the finished node
				if(finishedNode!=null)
				{
					//System.err.println("Node " + finishedNode.getName() + " finished!");
					for(InternalIBayesNode neighbor : finishedNode.getNeighborsI())
						if(this.blocks.containsKey(neighbor))
							this.blocks.put(neighbor,this.blocks.get(neighbor)-1);
				}
	
				// Let any waiting threads know that blocks have been lifted
				this.remainingNodes.notifyAll();
				
				// Find the next available node.
				while(remainingNodes.size() > 0)
				{
					for(InternalIBayesNode nd : this.remainingNodes)
					{
						if(blocks.get(nd)==0)
						{
							for(InternalIBayesNode neighbor : nd.getNeighborsI())
								this.blocks.put(neighbor,this.blocks.get(neighbor)+1);
							this.remainingNodes.remove(nd);
							//System.err.println("Dispatching node " + nd.getName());
							return nd;
						}
					}
					try {
						//System.err.println("No available nodes found, waiting.");
						this.remainingNodes.wait();
					} catch(InterruptedException e) {}
				}
				return null;
			}
		}

		int numThreads = Runtime.getRuntime().availableProcessors();
		Integer activeThreads = 0;
		boolean fault = false;
		String faultString = null;
		HashSet<InternalIBayesNode> remainingNodes =  new HashSet<InternalIBayesNode>();
		HashMap<InternalIBayesNode, Integer> blocks = new HashMap<InternalIBayesNode, Integer>();
		Double error = 0.0;
	}
	
	public void sample()
	{
		for(BaseNodeType nd : this.getNodes())
			nd.sample();
	}
	
	public RunResults run(Iterable<String> nodeNames, int maxit, double conv) throws BNException
	{
		long start_time = System.currentTimeMillis();
		double err = Double.POSITIVE_INFINITY;
	
		int i;
		for(i = 0; i < maxit && err > conv; i++)
		{
			err = 0;
			for(String nodeName: nodeNames)
			{
				BaseNodeType node = nodes.get(nodeName);
				if(node==null) throw new BNException("Node " + nodeName + " does not exist...");
				try{err = Math.max(err,node.updateMessages());}
				catch(BNException e){throw new BNException("Node " + nodeName + " threw an exception while updating : ",e);}
			}
		}
		long end_time = System.currentTimeMillis();
		return new RunResults(i, ((double)(end_time-start_time))/1000.0, err);
	}
	
	public RunResults run() throws BNException
	{
		return this.run(100,0);
	}
	
	public void clearEvidence(String nodeName) throws BNException
	{
		BaseNodeType node = this.getNode(nodeName);
		if(node==null)
			throw new BNException("Attempted to clear node evidence from node " + nodeName + " - does note exist.");
		node.clearEvidence();
	}
	
	public final void collectSufficientStatistics(Iterable<String> nodeNames, HashMap<String,SufficientStatistic> stats) throws BNException
	{
		for(String nodename : nodeNames)
		{
			BaseNodeType node = this.nodes.get(nodename);
			SufficientStatistic stat = stats.get(nodename);
			if(stat==null)
				stats.put(nodename, node.getSufficientStatistic());
			else
				stats.get(nodename).update(node.getSufficientStatistic());
		}
	}
	
	public final void optimize(Iterable<String> nodeNames, HashMap<String,SufficientStatistic> stats) throws BNException
	{ 
		for(String nodename : nodeNames)
		{
			BaseNodeType node = this.getNode(nodename);
			SufficientStatistic stat = stats.get(nodename);
			if(node==null)
				throw new BNException("Cannot optimize, node " + nodename + " does not exist.");
			if(stat==null)
				throw new BNException("Cannot optimize node " + nodename + ", not given sufficient statistic for it.");
			node.optimizeParameters(stat);
		}
	}
	
	protected abstract void removeNodeI(BaseNodeType node) throws BNException;
	private HashMap<String, BaseNodeType> nodes = new HashMap<String, BaseNodeType>();
}
