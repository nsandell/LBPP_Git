package bn;

import java.util.HashMap;

import bn.interfaces.IDynBayesNode;
import bn.interfaces.IDynBayesNet;

class DynamicBayesianNetwork extends BayesianNetwork<IDynBayesNode,DBNNode<?>> implements IDynBayesNet{
	
	public DynamicBayesianNetwork(int T){this.T = T;}
	
	public DiscreteDBNNode addDiscreteNode(String name, int cardinality) throws BNException
	{
		if(nodes.get(name)!=null)
			throw new BNException("Node " + name + " already exists in this DBN.");
		DiscreteDBNNode nd = new DiscreteDBNNode(this, unrolled_network, name, cardinality);
		this.nodes.put(name, nd);
		return nd;
	}
	
	public void addInterEdge(String fromname, String toname) throws BNException
	{
		DBNNode<?> from = nodes.get(fromname), to = nodes.get(toname);
		if(from==null || to==null)
			throw new BNException("Failed to add interconnection, either (or both) node " + from + " or node " + to + " doesn't exist.");
		try
		{
			from.addInterChild(to);
		} catch(BNException e) {throw new BNException("Whilst interconnecting "+from+"=>"+to+":",e);}
		try
		{
			to.addInterParent(from);
		} catch(BNException e) {
			from.removeInterChild(to);
			throw new BNException("Whilst interconnecting "+from+"=>"+to+":",e);
		}
	}
	
	public void addInterEdge(IDynBayesNode from, IDynBayesNode to) throws BNException
	{
		if(from==null || to==null)
			throw new BNException("Null argument passed to addInterEdge...");
		this.addInterEdge(from.getName(), to.getName());
	}
	
	public void addIntraEdge(String from, String to) throws BNException
	{
		this.addIntraEdgeI(nodes.get(from), nodes.get(to));
	}
	
	public void addIntraEdge(IDynBayesNode from, IDynBayesNode to) throws BNException
	{
		this.addIntraEdge(nodes.get(from.getName()), nodes.get(to.getName()));
	}
	
	private void addIntraEdgeI(DBNNode<?> from, DBNNode<?> to) throws BNException
	{
		if(from==null || to==null)
			throw new BNException("Attempted to intraconnect a nonexistant node..");
		try
		{
			from.addIntraChild(to);
		} catch(BNException e) {throw new BNException("Whilst intraconnecting "+from+"=>"+to+":",e);}
		try
		{
			to.addIntraParent(from);
		} catch(BNException e) {
			from.removeIntraChild(to);
			throw new BNException("Whilst intraconnecting "+from+"=>"+to+":",e);
		}
	}
	
	public int getT()
	{
		return this.T;
	}
	
	@Override
	protected void removeNodeI(DBNNode<?> node) throws BNException
	{
		for(DBNNode<?> intrachild : node.getIntraChildrenI())
			intrachild.removeIntraParent(node);
		for(DBNNode<?> interchild : node.getInterChildrenI())
			interchild.removeInterParent(node);
		for(DBNNode<?> intraparent : node.getIntraParentsI())
			intraparent.removeIntraChild(node);
		for(DBNNode<?> interparent: node.getInterParentsI())
			interparent.removeInterChild(node);
	}
	
	protected int T;
	protected StaticBayesianNetwork unrolled_network = new StaticBayesianNetwork();
	protected HashMap<String,DBNNode<?>> nodes = new HashMap<String, DBNNode<?>>();
}
