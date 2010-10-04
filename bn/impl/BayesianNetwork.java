package bn.impl;

import java.util.HashMap;

import java.util.HashSet;

import bn.BNException;
import bn.IBayesNode;
import bn.Options.InferenceOptions;
import bn.Options.LearningOptions;

abstract class BayesianNetwork<BaseInterface extends IBayesNode, BaseNodeType extends BaseInterface> {
	
	protected BayesianNetwork(){}
	
	public void validate() throws BNException
	{
		HashSet<IBayesNode> marks = new HashSet<IBayesNode>();
		HashSet<IBayesNode> ancestors = new HashSet<IBayesNode>(); // Can we replace this, we don't need value..
		
		for(BaseNodeType node : nodes.values())
		{
			// Depth first search to make sure we've no cycles.
			if(!marks.contains(node))
					this.dfs_cycle_detect(marks,ancestors,node);
				
			// Node should validate its CPT matches its parents, etc.
			node.validate();
		}
	}
	
	public void optimize(LearningOptions opts)
	{
		for(BaseNodeType node : nodes.values())
		{
			try{node.optimizeParameters();} catch(BNException e){}
		}
	}
	
	private void dfs_cycle_detect(HashSet<IBayesNode> marks, HashSet<IBayesNode> ancestors, IBayesNode current) throws BNException
	{
		ancestors.add(current);
		for(IBayesNode child : current.getChildren())
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
	
	public void removeNode(BaseInterface node) throws BNException
	{
		this.removeNode(node.getName());
	}
	
	public void removeNode(String name) throws BNException
	{
		BaseNodeType node = nodes.get(name);
		if(node!=null)
		{
			this.removeNodeI(node);
			//this.nodes_list.remove(node);
			this.nodes.remove(name);
		}
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
	
	public void run(int maxit, double conv) throws BNException
	{
		this.run(new InferenceOptions(maxit,conv));
	}
	
	public void run(InferenceOptions opts) throws BNException
	{
		if(opts.parallel)
			throw new BNException("Parallel inference not supported for static networks at this time.");
		
		this.resetSuffStats();
		
		double err = Double.POSITIVE_INFINITY;
	
		int i;
		for(i = 0; i < opts.maxIterations - 1 && err > opts.convergence; i++)
		{
			err = 0;
			if(nodeOrder==null)
				nodeOrder = nodes.keySet();
			for(String nodeName: nodes.keySet())
			{
				BaseNodeType node = nodes.get(nodeName);
				try
				{
					err = Math.max(err,node.updateMessages(false));
				}
				catch(BNException e){throw new BNException("Node " + nodeName + " threw an exception while updating : ",e);}
			}
		}
		err = 0;
		if(nodeOrder==null)
			nodeOrder = nodes.keySet();
		for(String nodeName: nodes.keySet())
		{
			BaseNodeType node = nodes.get(nodeName);
			try
			{
				err = Math.max(err,node.updateMessages(true));
			}
			catch(BNException e){throw new BNException("Node " + nodeName + " threw an exception while updating : ",e);}
		}

		System.out.println("Converged after " + i + " iterations with max change " + err);
	}
	
	protected abstract void resetSuffStats();
	
	public void clearEvidence(String nodeName) throws BNException
	{
		BaseNodeType node = this.getNode(nodeName);
		if(node==null)
			throw new BNException("Attempted to clear node evidence from node " + nodeName + " - does note exist.");
		node.clearEvidence();
	}
	
	public final void collectSufficientStats(boolean flag)
	{
		for(BaseNodeType type : nodes.values())
			type.collectSufficientStats(flag);
	}
	
	protected abstract void removeNodeI(BaseNodeType node) throws BNException;
	private Iterable<String> nodeOrder = null;
	private HashMap<String, BaseNodeType> nodes = new HashMap<String, BaseNodeType>();
}
