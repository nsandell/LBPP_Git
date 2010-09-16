package bn;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import bn.nodeInterfaces.BNNodeI;

public class BayesNet
{
	public BayesNet(){}
	
	public void addDiscreteNode(String name, int cardinality) throws BNException
	{
		if(this.nodes.get(name)!=null)
			throw new BNException("Attempted to add nodes with existing name : " + name);
		DiscreteBNNode node = new DiscreteBNNode(cardinality);
		this.nodes.put(name, node);
	}
	
	public void addEdge(String from, String to) throws BNException
	{
		try
		{
			nodes.get(from).addChild(nodes.get(to));
			nodes.get(to).addParent(nodes.get(from));
		} catch(BNException e) {
			throw new BNException("Error making connection " + from + " => " + to,e);
		}
	}

	public void validate() throws BNException
	{
		HashSet<BNNodeI> marks = new HashSet<BNNodeI>();
		HashSet<BNNodeI> ancestors = new HashSet<BNNodeI>(); // Can we replace this, we don't need value..
		
		for(BNNodeI node : nodes.values())
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
		Iterator<BNNodeI> children = current.getChildren();
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
		for(BNNodeI node : nodes.values())
			node.sendInitialMessages();
		
		double err = Double.POSITIVE_INFINITY;
	
		int i;
		for(i = 0; i < max_iterations && err > convergence; i++)
		{
			err = 0;
			for(String nodeName: nodes.keySet())
			{
				BNNodeI node = nodes.get(nodeName);
				try{err = Math.max(err,node.updateMessages());}
				catch(BNException e){throw new BNException("Node " + nodeName + " threw an exception while updating : ",e);}
			}
		}
		System.out.println("Converged after " + i + " iterations with max change " + err);
	}
	
	HashMap<String,BNNodeI> nodes = new HashMap<String, BNNodeI>();
	
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
