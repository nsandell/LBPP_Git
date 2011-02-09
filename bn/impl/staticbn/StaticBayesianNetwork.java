package bn.impl.staticbn;

import bn.BNException;
import bn.distributions.Distribution;
import bn.impl.BayesianNetwork;
import bn.impl.InternalIBayesNode;
import bn.statc.IDiscreteBayesNode;
import bn.statc.IStaticBayesNet;

class StaticBayesianNetwork extends BayesianNetwork<BNNode> implements IStaticBayesNet
{
	public StaticBayesianNetwork(){}
	
	public void addEdge(String from, String to) throws BNException
	{
		BNNode fromN = this.getNode(from);
		BNNode toN = this.getNode(to);
		if(fromN==null || toN==null)
			throw new BNException("Attempted to add to nonexistant node, either " + to + " or " + from);
		this.addEdgeI(fromN,toN);
	}
	
	public void addEdge(InternalIBayesNode from, InternalIBayesNode to) throws BNException
	{
		if(!(from instanceof BNNode) || !(to instanceof BNNode))
			throw new BNException("Attempted to connect nodes of unknown type...");
		this.addEdgeI((BNNode)from,(BNNode)to);
	}
	
	protected String getDefinitionFileHeader(){return "";}
	
	private void addEdgeI(BNNode fromN, BNNode toN) throws BNException
	{
		fromN.addChild(toN);
	}

	public IDiscreteBayesNode addDiscreteNode(String name, int cardinality) throws BNException
	{
		if(this.getNode(name)!=null)
			throw new BNException("Attempted to add nodes with existing name : " + name);
		DiscreteBNNode nd = new DiscreteBNNode(this, name, cardinality);
		this.addNodeI(nd);
		return nd;
	}
	
	@Override
	protected void removeNodeI(BNNode node) throws BNException
	{
		node.removeAllChildren();
		for(BNNode parent : node.getParentsI())
			parent.removeChild(node);
	}

	public void removeEdge(InternalIBayesNode from, InternalIBayesNode to) throws BNException
	{
		this.removeEdge(from.getName(), to.getName());
	}
	
	public void removeEdge(String from, String to) throws BNException
	{
		BNNode fromN = this.getNode(from);
		BNNode toN = this.getNode(to);
		if(fromN==null || toN==null)
			throw new BNException("Attempted to remove edge ("+from+","+to+") where one of the nodes doesn't exist.");
		fromN.removeChild(toN);
	}
	
	public boolean edgeExists(String from, String to) throws BNException
	{
		BNNode fromN = this.getNode(from);
		BNNode toN = this.getNode(to);
		
		if(fromN==null || toN==null)
			throw new BNException("Either node " + from + " or node " + to + " doesn't exist in this network.");
		return fromN.hasChild(toN);
	}
	
	public void setDistribution(String name, Distribution dist) throws BNException
	{
		if(this.getNode(name)==null)
			throw new BNException("Attempted to set the distribution of nonexistant node " + name);
		this.getNode(name).setDistribution(dist);
	}
	public Distribution getDistribution(String name) throws BNException
	{
		if(this.getNode(name)==null)
			throw new BNException("Attempted to set the distribution of nonexistant node " + name);
		return this.getNode(name).getDistribution();
	}
}
