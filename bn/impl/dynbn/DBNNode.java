package bn.impl.dynbn;

import java.io.PrintStream;


import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Vector;

import bn.BNException;
import bn.IBayesNode;
import bn.distributions.Distribution.SufficientStatistic;
import bn.dynamic.IDBNNode;
import bn.impl.InternalIBayesNode;
import bn.impl.dynbn.DynamicContextManager.DynamicMessageIndex;
import bn.messages.Message.MessageInterfaceSet;

abstract class DBNNode implements InternalIBayesNode, IDBNNode
{
	protected DBNNode(DynamicBayesianNetwork net,String name)
	{
		this.bayesNet = net;
		this.name = name;
	}
	
	/*
	 * General information accessors.
	 */
	
	@Override
	public String toString()
	{
		return this.name;
	}
	
	public final String getName()
	{
		return this.name;
	}
	public final int numParents()
	{
		return this.intraParents.size();
	}
	public final int numTotalParents()
	{
		return this.intraParents.size()+this.interParents.size();
	}
	public final int numChildren()
	{
		return this.intraChildren.size()+this.interChildren.size();
	}

	public String getEdgeDefinition()
	{
		String ret = "";
		Vector<Entry<DBNNode,DynamicMessageIndex>> edgeVec = new Vector<Entry<DBNNode,DynamicMessageIndex>>(this.interParents.entrySet());
		Collections.sort(edgeVec,new EdgeComparer());
		for(int i = 0; i < edgeVec.size(); i++)
			ret += edgeVec.get(i).getKey().getName()+"=>"+this.getName()+"\n";
		
		edgeVec = new Vector<Entry<DBNNode,DynamicMessageIndex>>(this.intraParents.entrySet());
		Collections.sort(edgeVec,new EdgeComparer());
		for(int i = 0; i < edgeVec.size(); i++)
			ret += edgeVec.get(i).getKey().getName()+"->"+this.getName()+"\n";
		/*for(DBNNode child : this.intraChildren.keySet())
			ret += this.getName()+"->"+child.getName()+"\n";
		for(DBNNode child : this.interChildren.keySet())
			ret += this.getName()+"=>"+child.getName()+"\n";*/
		return ret;
	}
	
	private static class EdgeComparer implements Comparator<Entry<DBNNode, DynamicMessageIndex>>
	{
		@Override
		public int compare(Entry<DBNNode, DynamicMessageIndex> o1,
				Entry<DBNNode, DynamicMessageIndex> o2) {
			return o1.getValue().compareTo(o2.getValue());
		}
	}
	
	@Override
	public DynamicBayesianNetwork getNetwork()
	{
		return this.bayesNet;
	}
	
	/*
	 *  Abstract methods for the creation and removal of edges.  Expected
	 *  to be implemented by specific node types.
	 */
	
	protected abstract MessageInterfaceSet<?> newChildInterface(int T) throws BNException;
	
	protected abstract DynamicMessageIndex addInterParentInterface(MessageInterfaceSet<?> mia) throws BNException;
	protected abstract DynamicMessageIndex addIntraParentInterface(MessageInterfaceSet<?> mia) throws BNException;
	protected abstract DynamicMessageIndex addInterChildInterface(MessageInterfaceSet<?> mia)  throws BNException;
	protected abstract DynamicMessageIndex addIntraChildInterface(MessageInterfaceSet<?> mia)  throws BNException;
	
	protected abstract void removeInterParentInterface(DynamicMessageIndex index) throws BNException;
	protected abstract void removeIntraParentInterface(DynamicMessageIndex index) throws BNException;
	protected abstract void removeInterChildInterface(DynamicMessageIndex index)  throws BNException;
	protected abstract void removeIntraChildInterface(DynamicMessageIndex index)  throws BNException;
	
	/*
	 * Edge creation methods
	 */
	
	public void addInterChild(DBNNode child) throws BNException
	{
		if(child.bayesNet!=this.bayesNet)
			throw new BNException("Attempted to connect two nodes from different networks...");
		if(this.interChildren.containsKey(child))
			return;
		
		try {
			MessageInterfaceSet<?> mi = this.newChildInterface(this.bayesNet.getT()-1);
			DynamicMessageIndex pi = this.addInterChildInterface(mi);
			try {
				DynamicMessageIndex ci = child.addInterParentInterface(mi);
				this.interChildren.put(child, pi);
				child.interParents.put(this, ci);
				
				this.neighbors.add(child);
				child.neighbors.add(this);
			} catch(BNException e) {
				this.removeInterChildInterface(pi);
				throw new BNException(e);
			}
		} catch(BNException e) {
			 throw new BNException("Failed to connect nodes " + this.name + " and " + child.name, e);
		}
	}
	public void addIntraChild(DBNNode child) throws BNException
	{
		if(child.bayesNet!=this.bayesNet)
			throw new BNException("Attempted to connect two nodes from different networks...");
		if(this.intraChildren.containsKey(child))
			return;
		
		try {
			MessageInterfaceSet<?> mi = this.newChildInterface(this.bayesNet.getT());
			DynamicMessageIndex pi = this.addIntraChildInterface(mi);
			try {
				DynamicMessageIndex ci = child.addIntraParentInterface(mi);
				this.intraChildren.put(child, pi);
				child.intraParents.put(this, ci);
				
				this.neighbors.add(child);
				child.neighbors.add(this);
			} catch(BNException e) {
				this.removeIntraChildInterface(pi);
				throw new BNException(e);
			}
		} catch(BNException e) {
			 throw new BNException("Failed to connect nodes " + this.name + " and " + child.name, e);
		}
	}
	
	/*
	 * Edge removal methods.
	 */
	
	public void removeInterChild(DBNNode child) throws BNException
	{
		if(!this.interChildren.containsKey(child))
			throw new BNException("Attempted to remove inter-child " + child.name + " from node " + this.name + " where it is not a child.");

		DynamicMessageIndex pi = this.interChildren.remove(child);
		DynamicMessageIndex ci = child.interParents.remove(this);
		
		this.removeInterChildInterface(pi);
		child.removeInterParentInterface(ci);
		child.interParents.remove(this);
		if(!this.intraChildren.containsKey(child))
		{
			this.neighbors.remove(child);
			child.neighbors.remove(this);
		}
	}
	public void removeIntraChild(DBNNode child) throws BNException
	{
		if(!this.intraChildren.containsKey(child))
			throw new BNException("Attempted to remove intra-child " + child.name + " from node " + this.name + " where it is not a child.");

		DynamicMessageIndex pi = this.intraChildren.remove(child);
		DynamicMessageIndex ci = child.intraParents.remove(this);
		
		this.removeIntraChildInterface(pi);
		child.removeIntraParentInterface(ci);
		child.intraParents.remove(this);
		if(!this.interChildren.containsKey(child))
		{
			this.neighbors.remove(child);
			child.neighbors.remove(this);
		}
	}
	
	public Iterable<DBNNode> getNeighborsI()
	{
		return this.neighbors;
	}
	
	public final void removeAllChildren() throws BNException
	{
		Vector<DBNNode> childCopy = new Vector<DBNNode>(this.interChildren.keySet());
		for(DBNNode child : childCopy)
			this.removeInterChild(child);
		childCopy = new Vector<DBNNode>(this.intraChildren.keySet());
		for(DBNNode child : childCopy)
			this.removeIntraChild(child);
	}
	public final void removeAllParents() throws BNException
	{
		Vector<DBNNode> parentCopy = new Vector<DBNNode>(this.interParents.keySet());
		for(DBNNode parent : parentCopy)
			parent.removeInterChild(this);
		parentCopy = new Vector<DBNNode>(this.intraParents.keySet());
		for(DBNNode parent : parentCopy)
			parent.removeIntraChild(this);
	}

	/*
	 * Children/parent edge existence checkers, both internal and external.
	 */
	
	public boolean hasInterChild(IDBNNode child)
	{
		return this.interChildren.containsKey(child);
	}
	public boolean hasIntraChild(IDBNNode child)
	{
		return this.intraChildren.containsKey(child);
	}
	public boolean hasInterParent(IDBNNode parent)
	{
		return this.interParents.containsKey(parent);
	}
	public boolean hasIntraParent(IDBNNode parent)
	{
		return this.intraParents.containsKey(parent);
	}
	boolean hasInterChild(DBNNode child)
	{
		return this.interChildren.containsKey(child);
	}
	
	boolean hasIntraChild(DBNNode child)
	{
		return this.intraChildren.containsKey(child);
	}
	
	
	public abstract void validate() throws BNException;

	
	/*
	 * Internal and external iterators over children and parents.
	 */
	
	public Iterable<DBNNode> getInterChildren()
	{
		return this.interChildren.keySet();
	}
	public Iterable<DBNNode> getInterParents()
	{
		return this.interParents.keySet();
	}
	public Iterable<? extends IBayesNode> getChildren()
	{
		return this.intraChildren.keySet();
	}
	public Iterable<? extends IBayesNode> getParents()
	{
		return this.intraParents.keySet();
	}
	public Iterable<DBNNode> getChildrenI()
	{
		return this.intraChildren.keySet();
	}
	public Iterable<DBNNode> getParentsI()
	{
		return this.intraParents.keySet();
	}
	public Iterable<DBNNode> getIntraChildren()
	{
		return this.intraChildren.keySet();
	}
	public Iterable<DBNNode> getIntraParents()
	{
		return this.intraParents.keySet();
	}
	public Iterable<DBNNode> getInterParentsI()
	{
		return this.interParents.keySet();
	}
	public Iterable<DBNNode> getIntraParentsI()
	{
		return this.intraParents.keySet();
	}
	public Iterable<DBNNode> getInterChildrenI()
	{
		return this.interChildren.keySet();
	}
	public Iterable<DBNNode> getIntraChildrenI()
	{
		return this.intraChildren.keySet();
	}

	/*
	 * Message updating methods.
	 */
	
	public double updateMessages() throws BNException
	{
		return updateMessages(0,this.bayesNet.getT()-1);
	}
	public final double updateMessages(int tmin, int tmax) throws BNException
	{
		double maxErr = 0;
		maxErr = Math.max(maxErr, this.updateMessages(tmin, tmax, this.lastPassBackward));
		this.lastPassBackward = !this.lastPassBackward;
		return maxErr;
	}
	public final double updateMessages(int tmin, int tmax, boolean forward) throws BNException
	{
		double maxErr = 0;
		if(forward)
			for(int t = tmin; t <= tmax; t++)
				maxErr = Math.max(this.updateMessages(t),maxErr);
		else
			for(int t = tmax; t >= tmin; t--)
				maxErr = Math.max(this.updateMessages(t), maxErr);
		
		return maxErr;
	}
	protected abstract double updateMessages(int t) throws BNException;
	public abstract void resetMessages();

	public abstract void printDistributionInfo(PrintStream ps);
	
	public void lockParameters()
	{
		this.parametersLocked = true;
	}
	public void unlockParameters()
	{
		this.parametersLocked = false;
	}
	public boolean isLocked()
	{
		return this.parametersLocked;
	}
	
	public final double optimizeParameters() throws BNException
	{
		if(this.parametersLocked)
			return 0;
		else
			return this.optimizeParametersI();
	}
	public final double optimizeParameters(SufficientStatistic stat) throws BNException
	{
		if(this.parametersLocked)
			return 0;
		else
			return this.optimizeParametersI(stat);
	}
	
	protected abstract double optimizeParametersI() throws BNException;
	protected abstract double optimizeParametersI(SufficientStatistic stat) throws BNException;
	
	protected boolean parametersLocked = false;
	private boolean lastPassBackward = true;

	protected DynamicBayesianNetwork bayesNet;
	protected String name;
	

	protected HashSet<DBNNode> neighbors = new HashSet<DBNNode>();
	protected HashMap<DBNNode,DynamicMessageIndex> interChildren = new HashMap<DBNNode, DynamicMessageIndex>();
	protected HashMap<DBNNode,DynamicMessageIndex> intraChildren = new HashMap<DBNNode, DynamicMessageIndex>();
	protected HashMap<DBNNode,DynamicMessageIndex> interParents = new HashMap<DBNNode, DynamicMessageIndex>();
	protected HashMap<DBNNode,DynamicMessageIndex> intraParents = new HashMap<DBNNode, DynamicMessageIndex>();
	
}
