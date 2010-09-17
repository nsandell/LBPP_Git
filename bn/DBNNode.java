package bn;

import java.util.ArrayList;
import java.util.Iterator;

import bn.BayesNet.BNException;
import bn.interfaces.BNNodeI;
import bn.interfaces.DBNNodeI;

abstract class DBNNode<InnerType extends BNNode> implements DBNNodeI<InnerType>
{
	protected DBNNode(DynamicBayesNetwork net)
	{
		this.bayesNet = net;
		this.interChildrenSet = new DBNNodeSet(this.interChildren);
		this.interParentSet = new DBNNodeSet(this.interParents);
		this.intraChildrenSet = new DBNNodeSet(this.intraChildren);
		this.intraParentSet = new DBNNodeSet(this.intraParents);
		this.nodeInstances = new ArrayList<InnerType>(net.T);
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
	
	public DBNNodeISet getInterChildren()
	{
		return this.interChildrenSet;
	}
	
	public DBNNodeISet getInterParents()
	{
		return this.interParentSet;
	}
	
	public DBNNodeISet getIntraChildren()
	{
		return this.intraChildrenSet;
	}
	
	public DBNNodeISet getIntraParents()
	{
		return this.intraParentSet;
	}
	
	protected ArrayList<InnerType> nodeInstances;
	
	protected abstract Class<? extends BNNodeI> getBNNodeClass();
	
	private ArrayList<DBNNode<?>> interChildren = new ArrayList<DBNNode<?>>();
	private ArrayList<DBNNode<?>> interParents = new ArrayList<DBNNode<?>>();
	private ArrayList<DBNNode<?>> intraChildren = new ArrayList<DBNNode<?>>();
	private ArrayList<DBNNode<?>> intraParents = new ArrayList<DBNNode<?>>();
	
	private DBNNodeISet interChildrenSet;
	private DBNNodeISet intraChildrenSet;
	private DBNNodeISet interParentSet;
	private DBNNodeISet intraParentSet;
	
	DynamicBayesNetwork bayesNet;
	
	static class DBNNodeSet implements DBNNodeISet
	{
		public DBNNodeSet(ArrayList<DBNNode<?>> set){this.set = set;}
		public Iterator<DBNNodeI<?>> iterator(){return new IteratorWrapper(this.set.iterator());}
		static class IteratorWrapper implements Iterator<DBNNodeI<?>>
		{
			public IteratorWrapper(Iterator<DBNNode<?>> it){this.it = it;}
			public boolean hasNext(){return it.hasNext();}
			public DBNNodeI<?> next(){return it.next();}
			public void remove(){}
			private Iterator<DBNNode<?>> it;
		}
		private ArrayList<DBNNode<?>> set;
	}
}
