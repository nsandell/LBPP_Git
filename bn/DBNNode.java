package bn;

import java.util.ArrayList;
import java.util.Vector;

import util.IterableWrapper;

import bn.interfaces.IBayesNode;
import bn.interfaces.IDynBayesNode;

abstract class DBNNode<InnerType extends BNNode> implements IDynBayesNode
{
	protected DBNNode(DynamicBayesianNetwork net,String name)
	{
		this.bayesNet = net;
		this.name = name;
		this.interChildrenSet = new IterableWrapper<IDynBayesNode>(this.interChildren);
		this.interParentSet = new IterableWrapper<IDynBayesNode>(this.interParents);
		this.intraChildrenSet = new IterableWrapper<IDynBayesNode>(this.intraChildren);
		this.intraParentSet = new IterableWrapper<IDynBayesNode>(this.intraParents);
		this.childrenSet = new IterableWrapper<IBayesNode>(this.intraChildren);
		this.parentSet = new IterableWrapper<IBayesNode>(this.intraParents);
		this.nodeInstances = new Vector<InnerType>(net.getT());
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
		return this.interChildrenSet;
	}
	
	public Iterable<IDynBayesNode> getInterParents()
	{
		return this.interParentSet;
	}
	
	public Iterable<IBayesNode> getChildren()
	{
		return this.childrenSet;
	}
	
	public Iterable<IBayesNode> getParents()
	{
		return this.parentSet;
	}
	
	public Iterable<IDynBayesNode> getIntraChildren()
	{
		return this.intraChildrenSet;
	}
	
	public Iterable<IDynBayesNode> getIntraParents()
	{
		return this.intraParentSet;
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

	
	protected Vector<InnerType> nodeInstances;
	
	private ArrayList<DBNNode<?>> interChildren = new ArrayList<DBNNode<?>>();
	private ArrayList<DBNNode<?>> intraChildren = new ArrayList<DBNNode<?>>();
	private ArrayList<DBNNode<?>> interParents = new ArrayList<DBNNode<?>>();
	private ArrayList<DBNNode<?>> intraParents= new ArrayList<DBNNode<?>>();

	private IterableWrapper<IBayesNode> childrenSet;
	private IterableWrapper<IBayesNode> parentSet;
	private IterableWrapper<IDynBayesNode> interChildrenSet;
	private IterableWrapper<IDynBayesNode> intraChildrenSet;
	private IterableWrapper<IDynBayesNode> interParentSet;
	private IterableWrapper<IDynBayesNode> intraParentSet;
	
	DynamicBayesianNetwork bayesNet;
	String name;
}
