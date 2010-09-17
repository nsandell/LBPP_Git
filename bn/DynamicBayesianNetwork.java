package bn;

import java.util.HashMap;
import java.util.HashSet;
import bn.interfaces.DBNNodeI;

class DynamicBayesianNetwork {
	
	public DynamicBayesianNetwork(int T){this.T = T;}
	
	public DiscreteDBNNode addDiscreteNode(String name, int cardinality) throws BNException
	{
		if(nodes.get(name)!=null)
			throw new BNException("Node " + name + " already exists in this DBN.");
		DiscreteDBNNode nd = new DiscreteDBNNode(this, unrolled_network, name, cardinality);
		this.nodes.put(name, nd);
		return nd;
	}
	
	private void addInterEdgeI(DBNNode<?> from, DBNNode<?> to) throws BNException
	{
		try
		{
			from.addInterChild(to);
		} catch(BNException e) {}
		try
		{
			to.addInterParent(from);
		} catch(BNException e) {}
	}
	
	public void validate() throws BNException
	{
		HashSet<DBNNodeI> marks = new HashSet<DBNNodeI>();
		HashSet<DBNNodeI> ancestors = new HashSet<DBNNodeI>();
		for(String ndname : this.nodes.keySet())
		{
			DBNNodeI nd = this.nodes.get(ndname);
			try{nd.validate();}
			catch(BNException e){throw new BNException("Failed to validate node " + ndname,e);}
			if(!marks.contains(nd))
				dfs_cycle_detect(marks, ancestors, nd);
		}
	}
	
	private void dfs_cycle_detect(HashSet<DBNNodeI> marks, HashSet<DBNNodeI> ancestors, 
				DBNNodeI current) throws BNException
	{
		ancestors.add(current);
		for(DBNNodeI child : current.getIntraChildren())
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
	
	public int getT()
	{
		return this.T;
	}
	
	protected int T;
	protected StaticBayesianNetwork unrolled_network = new StaticBayesianNetwork();
	protected HashMap<String,DBNNode<?>> nodes = new HashMap<String, DBNNode<?>>();
}
