package bn;

import bn.interfaces.IDynBayesNode;
import bn.interfaces.IDynBayesNet;

class DynamicBayesianNetwork extends BayesianNetwork<IDynBayesNode,DBNNode<?>> implements IDynBayesNet{

	public DynamicBayesianNetwork(int T){this.T = T;}

	public DiscreteDBNNode addDiscreteNode(String name, int cardinality) throws BNException
	{
		if(this.getNode(name)!=null)
			throw new BNException("Node " + name + " already exists in this DBN.");
		DiscreteDBNNode nd = new DiscreteDBNNode(this, unrolled_network, name, cardinality);
		this.addNodeI(nd);
		return nd;
	}

	public void addInterEdge(String fromname, String toname) throws BNException
	{
		DBNNode<?> from = this.getNode(fromname), to = this.getNode(toname);
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

	public void addIntraEdge(String fromname, String toname) throws BNException
	{		
		DBNNode<?> from = this.getNode(fromname), to = this.getNode(toname);
		if(from==null || to==null)
			throw new BNException("Failed to add intraconnection, either (or both) node " + from + " or node " + to + " doesn't exist.");
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

	public void addIntraEdge(IDynBayesNode from, IDynBayesNode to) throws BNException
	{
		if(from==null || to==null)
			throw new BNException("Null argument passed to addIntraEdge...");
		this.addIntraEdge(from.getName(), to.getName());
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
}
