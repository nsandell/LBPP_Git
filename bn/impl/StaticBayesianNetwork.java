package bn.impl;

import bn.BNException;
import bn.IDiscreteBayesNode;
import bn.IStaticBayesNet;
import bn.distributions.Distribution;
import bn.messages.Message;

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
	
	public void setDistribution(String nodeName, Distribution dist) throws BNException
	{
		BNNode node = this.getNode(nodeName);
		node.setDistribution(dist);
	}
	
	private void addEdgeI(BNNode fromN, BNNode toN) throws BNException
	{
		try
		{
			fromN.addChild(toN);
		} catch(BNException e) {
			fromN.removeChild(toN);
			throw new BNException("Failed to add child : ",e);
		}	
	}
	
	public Message getMarginal(String name) throws BNException
	{
		BNNode node = this.getNode(name);
		if(node==null) throw new BNException("Attempted to get marginal for node " + name + ", doesn't exist..");
		return node.getMarginal();
	}
	
	public Object getEvidence(String name) throws BNException
	{
		BNNode node = this.getNode(name);
		if(node==null) throw new BNException("Attempted to get evidence for node " + name + ", doesn't exist..");
		return node.getValue();
	}
	
	public IDiscreteBayesNode addDiscreteNode(String name, int cardinality) throws BNException
	{
		if(this.getNode(name)!=null)
			throw new BNException("Attempted to add nodes with existing name : " + name);
		BNNode.DiscreteBNNode nd = new BNNode.DiscreteBNNode(this, name, cardinality);
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

	public void addEvidence(String nodename, Object evidence) throws BNException
	{
		BNNode node = this.getNode(nodename);
		if(node==null)
			throw new BNException("Error while adding evidence : Node " + nodename + " does not exist.");
		node.setValue(evidence);
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
	
}
