package bn;

import java.util.ArrayList;
import java.util.Vector;

import util.IterableWrapper;

import bn.BayesNet.BNException;
import bn.interfaces.DBNNodeI;

abstract class DBNNode<InnerType extends BNNode> implements DBNNodeI
{
	protected DBNNode(DynamicBayesNetwork net)
	{
		this.bayesNet = net;
		this.interChildrenSet = new IterableWrapper<DBNNodeI>(this.interChildren);
		this.interParentSet = new IterableWrapper<DBNNodeI>(this.interParents);
		this.intraChildrenSet = new IterableWrapper<DBNNodeI>(this.intraChildren);
		this.intraParentSet = new IterableWrapper<DBNNodeI>(this.intraParents);
		this.nodeInstances = new Vector<InnerType>(net.T);
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
	
	/*public abstract void addInterParentI(DBNNode parent);
	public abstract void addIntraParentI(DBNNode parent);
	public abstract void addInterChildI(DBNNode child);
	public abstract void addIntraChildI(DBNNode child);
	public abstract void removeInterParentI(DBNNode parent);
	public abstract void removeIntraParentI(DBNNode parent);
	public abstract void removeInterChildI(DBNNode child);
	public abstract void removeIntraChildI(DBNNode child);*/
	
	public Iterable<DBNNodeI> getInterChildren()
	{
		return this.interChildrenSet;
	}
	
	public Iterable<DBNNodeI> getInterParents()
	{
		return this.interParentSet;
	}
	
	public Iterable<DBNNodeI> getIntraChildren()
	{
		return this.intraChildrenSet;
	}
	
	public Iterable<DBNNodeI> getIntraParents()
	{
		return this.intraParentSet;
	}
	
	Vector<InnerType> nodeInstances;
	
	private ArrayList<DBNNode<?>> interChildren = new ArrayList<DBNNode<?>>();
	private ArrayList<DBNNode<?>> intraChildren = new ArrayList<DBNNode<?>>();
	private ArrayList<DBNNode<?>> interParents = new ArrayList<DBNNode<?>>();
	private ArrayList<DBNNode<?>> intraParents= new ArrayList<DBNNode<?>>();
	
	private IterableWrapper<DBNNodeI> interChildrenSet;
	private IterableWrapper<DBNNodeI> intraChildrenSet;
	private IterableWrapper<DBNNodeI> interParentSet;
	private IterableWrapper<DBNNodeI> intraParentSet;
	
	DynamicBayesNetwork bayesNet;
}
