package bn;

import java.util.HashMap;

import java.util.HashSet;
import java.util.Iterator;

import bn.interfaces.BNNodeI;
import bn.interfaces.BayesNetI;

public class BayesNet implements BayesNetI
{
	public BayesNet(){}
	
	public void addEdge(String from, String to) throws BNException
	{
		BNNode fromN = this.nodes.get(from);
		BNNode toN = this.nodes.get(to);
		if(fromN==null || toN==null)
			throw new BNException("Attempted to add to nonexistant node, either " + to + " or " + from);
		this.addEdgeI(fromN,toN);
	}
	
	public void addEdge(BNNodeI from, BNNodeI to) throws BNException
	{
		if(!(from instanceof BNNode) || !(to instanceof BNNode))
			throw new BNException("Attempted to connect nodes of unknown type...");
		this.addEdgeI((BNNode)from,(BNNode)to);
	}
	
	private void addEdgeI(BNNode fromN, BNNode toN) throws BNException
	{
		try
		{
			fromN.addChild(toN);
		} catch(BNException e) {
			throw new BNException("Failed to add child : ",e);
		}	
		try
		{
			toN.addParent(fromN);
		} catch(BNException e) {
			fromN.removeChild(toN);
			throw new BNException("Failed to add parent : ",e);
		}
	}
	
	public DiscreteBNNode addDiscreteNode(String name, int cardinality) throws BNException
	{
		if(this.nodes.get(name)!=null)
			throw new BNException("Attempted to add nodes with existing name : " + name);
		DiscreteBNNode node = new DiscreteBNNode(this,cardinality);
		this.nodes.put(name, node);
		return node;
	}
	
	public void validate() throws BNException
	{
		HashSet<BNNodeI> marks = new HashSet<BNNodeI>();
		HashSet<BNNodeI> ancestors = new HashSet<BNNodeI>(); // Can we replace this, we don't need value..
		
		for(BNNode node : nodes.values())
		{
			// Depth first search to make sure we've no cycles.
			if(!marks.contains(node))
					this.dfs_cycle_detect(marks,ancestors,node);
				
			// Node should validate its CPT matches its parents, etc.
			node.validate();
		}
	}
	
	private void dfs_cycle_detect(HashSet<BNNodeI> marks, HashSet<BNNodeI> ancestors, BNNodeI current) throws BNException
	{
		ancestors.add(current);
		Iterator<BNNodeI> children = current.getChildren().iterator();
		while(children.hasNext())
		{
			BNNodeI child = children.next();
			if(ancestors.contains(child))
				throw new BNException("Bayesian network is cyclic!");
			dfs_cycle_detect(marks, ancestors, child);
		}
		marks.add(current);
		ancestors.remove(current);
	}
	
	public Iterator<String> getNodeNames()
	{
		return this.nodes.keySet().iterator();
	}
	
	public BNNodeI getNode(String name)
	{
		return this.nodes.get(name);
	}
	
	public void run(int max_iterations, double convergence) throws BNException
	{
		for(BNNode node : nodes.values())
			node.sendInitialMessages();
		
		double err = Double.POSITIVE_INFINITY;
	
		int i;
		for(i = 0; i < max_iterations && err > convergence; i++)
		{
			err = 0;
			for(String nodeName: nodes.keySet())
			{
				BNNode node = nodes.get(nodeName);
				try{err = Math.max(err,node.updateMessages());}
				catch(BNException e){throw new BNException("Node " + nodeName + " threw an exception while updating : ",e);}
			}
		}
		System.out.println("Converged after " + i + " iterations with max change " + err);
	}
	
	HashMap<String,BNNode> nodes = new HashMap<String, BNNode>();
	
	public static class BNException extends Exception {

		public BNException(String message) {
			super(message);
			System.err.println("Exception of type " + this.getClass().toString() + 
					" thrown : " + message);
		}

		public BNException(BNException inner) {
			super(inner);
			System.err.println("Exception of type " + this.getClass().toString() + 
					" thrown wrapping one of " + inner.getClass().toString() + " : "
					+ inner.getMessage());
		}

		public BNException(String message, BNException inner) {
			super(message, inner);
			System.err.println("Exception of type " + this.getClass().toString() + 
					" thrown wrapping one of " + inner.getClass().toString() + " : "
					+ message + "  (" + inner.getMessage() + ")");

		}
		private static final long serialVersionUID = 1L;
	}
}
