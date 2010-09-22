package bn;

import java.util.concurrent.CopyOnWriteArrayList;

import util.IterableWrapper;

import bn.interfaces.IBayesNode;
import bn.interfaces.IDynBayesNode;

abstract class DBNNode<InnerType extends BNNode> implements IDynBayesNode
{
	protected DBNNode(DynamicBayesianNetwork net,String name)
	{
		this.bayesNet = net;
		this.name = name;
		this.nodeInstances = new CopyOnWriteArrayList<InnerType>();
	}
	
	public final String getName()
	{
		return this.name;
	}
	
	public final void addInterChild(DBNNode<?> child) throws BNException
	{
		for(int i = 0; i < this.bayesNet.getT()-1; i++)
			this.nodeInstances.get(i).addChild(child.nodeInstances.get(i+1));
		this.interChildren.add(child);
	}
	
	public final void addIntraChild(DBNNode<?> child) throws BNException
	{
		for(int i = 0; i < this.bayesNet.getT(); i++)
			this.nodeInstances.get(i).addChild(child.nodeInstances.get(i));
		this.intraChildren.add(child);
	}

	public final void removeInterChild(DBNNode<?> child) throws BNException
	{
		for(int i = 0; i < this.bayesNet.getT()-1; i++)
			this.nodeInstances.get(i).removeChild(child.nodeInstances.get(i+1));
		this.interChildren.remove(child);
	}
	
	public final void removeIntraChild(DBNNode<?> child) throws BNException
	{
		for(int i = 0; i < this.bayesNet.getT(); i++)
			this.nodeInstances.get(i).removeChild(child.nodeInstances.get(i));
		this.intraChildren.remove(child);
	}
	
	public final void addInterParent(DBNNode<?> parent) throws BNException
	{
		for(int i = 1; i < this.bayesNet.getT(); i++)
			this.nodeInstances.get(i).addParent(parent.nodeInstances.get(i-1));
		this.interParents.add(parent);
	}
		
	public final void addIntraParent(DBNNode<?> parent) throws BNException
	{
		for(int i = 0; i < this.bayesNet.getT(); i++)
			this.nodeInstances.get(i).addParent(parent.nodeInstances.get(i));
		this.intraParents.add(parent);
	}
	
	public final void removeInterParent(DBNNode<?> parent) throws BNException
	{
		for(int i = 1; i < this.bayesNet.getT(); i++)
			this.nodeInstances.get(i).removeParent(parent.nodeInstances.get(i-1));
		this.interParents.add(parent);
	}
	
	public final void removeIntraParent(DBNNode<?> parent) throws BNException
	{
		for(int i = 0; i < this.bayesNet.getT(); i++)
			this.nodeInstances.get(i).removeParent(parent.nodeInstances.get(i));
		this.intraParents.remove(parent);
	}
	
	public InnerType getInstance(int t)
	{
		return this.nodeInstances.get(t);
	}
	
	public void validate() throws BNException
	{
		for(InnerType nd : this.nodeInstances)
			nd.validate();
	}
	
	public Iterable<IDynBayesNode> getInterChildren()
	{
		return new IterableWrapper<IDynBayesNode>(this.interChildren);
	}
	
	public Iterable<IDynBayesNode> getInterParents()
	{
		return new IterableWrapper<IDynBayesNode>(this.interParents);
	}
	
	public Iterable<IBayesNode> getChildren()
	{
		return new IterableWrapper<IBayesNode>(this.intraChildren);
	}
	
	public Iterable<IBayesNode> getParents()
	{
		return new IterableWrapper<IBayesNode>(this.intraParents);
	}
	
	public Iterable<IDynBayesNode> getIntraChildren()
	{
		return new IterableWrapper<IDynBayesNode>(this.intraChildren);
	}
	
	public Iterable<IDynBayesNode> getIntraParents()
	{
		return new IterableWrapper<IDynBayesNode>(this.intraParents);
	}
	
	public Iterable<DBNNode<?>> getInterParentsI()
	{
		return this.interParents;
	}
	
	public Iterable<DBNNode<?>> getIntraParentsI()
	{
		return this.intraParents;
	}
	
	public Iterable<DBNNode<?>> getInterChildrenI()
	{
		return this.interChildren;
	}
	
	public Iterable<DBNNode<?>> getIntraChildrenI()
	{
		return this.intraChildren;
	}
	
	public final double updateMessages(int tmin, int tmax) throws BNException
	{
		return this.updateMessagesI(tmin, tmax);
	}
	
	protected abstract double updateMessagesI(int tmin, int tmax) throws BNException;
	
	protected CopyOnWriteArrayList<InnerType> nodeInstances;
	
	private CopyOnWriteArrayList<DBNNode<?>> interChildren = new CopyOnWriteArrayList<DBNNode<?>>();
	private CopyOnWriteArrayList<DBNNode<?>> intraChildren = new CopyOnWriteArrayList<DBNNode<?>>();
	private CopyOnWriteArrayList<DBNNode<?>> interParents = new CopyOnWriteArrayList<DBNNode<?>>();
	private CopyOnWriteArrayList<DBNNode<?>> intraParents = new CopyOnWriteArrayList<DBNNode<?>>();

	DynamicBayesianNetwork bayesNet;
	String name;
}
