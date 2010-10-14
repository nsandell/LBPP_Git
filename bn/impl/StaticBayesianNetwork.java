package bn.impl;

import bn.BNException;
import bn.IBayesNode;
import bn.IStaticBayesNet;
import bn.distributions.DiscreteDistribution;
import bn.distributions.Distribution;

class StaticBayesianNetwork extends BayesianNetwork<IBayesNode,BNNode> implements IStaticBayesNet
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
	
	public void addEdge(IBayesNode from, IBayesNode to) throws BNException
	{
		if(!(from instanceof BNNode) || !(to instanceof BNNode))
			throw new BNException("Attempted to connect nodes of unknown type...");
		this.addEdgeI((BNNode)from,(BNNode)to);
	}
	
	public void setDistribution(String nodeName, Distribution dist) throws BNException
	{
		BNNode node = this.getNode(nodeName);
		if(node instanceof DiscreteBNNode && dist instanceof DiscreteDistribution)
			((DiscreteBNNode)node).setDistribution((DiscreteDistribution)dist);
		else
			throw new BNException("Unsupported node/distribution pair.");
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
		if(this.getNode(name)!=null)
			throw new BNException("Attempted to add nodes with existing name : " + name);
		DiscreteBNNode node = new DiscreteBNNode(this,name,cardinality);
		this.addNodeI(node);
		return node;
	}
	
	@Override
	protected void removeNodeI(BNNode node) throws BNException
	{
		for(BNNode child : node.getChildrenI())
			child.removeParent(node);
	}
	
	public double nodeLogLikelihood(String nodename) throws BNException
	{
		BNNode node = this.getNode(nodename);
		if(node==null)
			throw new BNException("Node " + nodename + " does not exist.");
		return node.getLogLikelihood();
	}
	
	public void addDiscreteEvidence(String nodename, int evidence) throws BNException
	{
		BNNode node = this.getNode(nodename);
		if(!(node instanceof DiscreteBNNode))
			throw new BNException("Attempted to add discrete evidence to non-discrete node.");
		else
			((DiscreteBNNode)node).setValue(evidence);
	}
	
	public void removeEdge(IBayesNode from, IBayesNode to) throws BNException
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
		toN.removeParent(fromN);
	}
}
