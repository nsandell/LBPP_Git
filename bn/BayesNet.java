package bn;

import java.util.HashMap;
import java.util.Iterator;

import bn.nodeInterfaces.BNNodeI;

public class BayesNet
{
	public BayesNet(){}
	
	public DiscreteBNNode addDiscreteNode(String name, int cardinality) throws BNException
	{
		if(this.nodes.get(name)!=null)
			throw new BNException("Attempted to add nodes with existing name : " + name);
		DiscreteBNNode node = new DiscreteBNNode(cardinality);
		this.nodes.put(name, node);
		return node;
	}

	public void validate() throws BNException
	{
		for(BNNodeI node : nodes.values())
			node.validate();
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
