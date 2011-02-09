package bn.impl.dynbn;

import java.io.PrintStream;


import java.util.HashMap;
import java.util.Vector;
import java.util.Map.Entry;

import bn.BNException;
import bn.IBayesNode;
import bn.dynamic.IDynNode;
import bn.impl.InternalIBayesNode;
import bn.messages.Message.MessageInterfaceSet;

abstract class DBNNode implements InternalIBayesNode, IDynNode
{
	protected DBNNode(DynamicBayesianNetwork net,String name)
	{
		this.bayesNet = net;
		this.name = name;
	}
	
	/*
	 * General information accessors.
	 */
	
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
		for(DBNNode child : this.intraChildren.keySet())
			ret += this.getName()+"->"+child.getName()+"\n";
		for(DBNNode child : this.interChildren.keySet())
			ret += this.getName()+"=>"+child.getName()+"\n";
		return ret;
	}
	
	/*
	 *  Abstract methods for the creation and removal of edges.  Expected
	 *  to be implemented by specific node types.
	 */
	
	protected abstract MessageInterfaceSet<?> newChildInterface(int T);
	
	protected abstract int addInterParentInterface(MessageInterfaceSet<?> mia) throws BNException;
	protected abstract int addIntraParentInterface(MessageInterfaceSet<?> mia) throws BNException;
	protected abstract int addInterChildInterface(MessageInterfaceSet<?> mia)  throws BNException;
	protected abstract int addIntraChildInterface(MessageInterfaceSet<?> mia)  throws BNException;
	
	protected abstract void removeInterParentInterface(int index) throws BNException;
	protected abstract void removeIntraParentInterface(int index) throws BNException;
	protected abstract void removeInterChildInterface(int index)  throws BNException;
	protected abstract void removeIntraChildInterface(int index)  throws BNException;
	
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
			int pi = this.addInterChildInterface(mi);
			try {
				int ci = child.addInterParentInterface(mi);
				this.interChildren.put(child, ci);
				child.interParents.put(this, pi);
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
			int pi = this.addIntraChildInterface(mi);
			try {
				int ci = child.addIntraParentInterface(mi);
				this.intraChildren.put(child, ci);
				child.intraParents.put(this, pi);
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

		int pi = this.interChildren.remove(child);
		int ci = child.interParents.remove(this);
		
		this.removeInterChildInterface(pi);
		child.removeInterParentInterface(ci);
		
		for(Entry<DBNNode, Integer> entry : this.interChildren.entrySet())
			if(entry.getValue() >= pi)
				entry.setValue(entry.getValue()-1);
		
		for(Entry<DBNNode, Integer> entry : child.interParents.entrySet())
			if(entry.getValue() >= ci)
				entry.setValue(entry.getValue()-1);
	}
	public void removeIntraChild(DBNNode child) throws BNException
	{
		if(!this.intraChildren.containsKey(child))
			throw new BNException("Attempted to remove intra-child " + child.name + " from node " + this.name + " where it is not a child.");

		int pi = this.intraChildren.remove(child);
		int ci = child.intraParents.remove(this);
		
		this.removeInterChildInterface(pi);
		child.removeInterParentInterface(ci);
		
		for(Entry<DBNNode, Integer> entry : this.intraChildren.entrySet())
			if(entry.getValue() >= pi)
				entry.setValue(entry.getValue()-1);
		
		for(Entry<DBNNode, Integer> entry : child.intraParents.entrySet())
			if(entry.getValue() >= ci)
				entry.setValue(entry.getValue()-1);
	}
	public final void removeAllChildren() throws BNException
	{
		Vector<DBNNode> childrenCopy = new Vector<DBNNode>(this.interChildren.keySet());
		for(DBNNode nd : childrenCopy)
			this.removeInterChild(nd);
		childrenCopy = new Vector<DBNNode>(this.intraChildren.keySet());
		for(DBNNode nd : childrenCopy)
			this.removeIntraChild(nd);
	}
	public final void removeAllParents() throws BNException
	{
		Vector<DBNNode> parentCopy = new Vector<DBNNode>(this.interParents.keySet());
		for(DBNNode nd : parentCopy)
			nd.removeInterChild(this);
		parentCopy = new Vector<DBNNode>(this.intraParents.keySet());
		for(DBNNode nd : parentCopy)
			nd.removeIntraChild(this);
	}

	/*
	 * Children/parent edge existence checkers, both internal and external.
	 */
	
	public boolean hasInterChild(IDynNode child)
	{
		return this.interChildren.containsKey(child);
	}
	public boolean hasIntraChild(IDynNode child)
	{
		return this.intraChildren.containsKey(child);
	}
	public boolean hasInterParent(IDynNode parent)
	{
		return this.interParents.containsKey(parent);
	}
	public boolean hasIntraParent(IDynNode parent)
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

	//TODO Evaluate where to place the optimization code
	/*
	public static class TwoSliceStatistics<StatType extends SufficientStatistic> implements SufficientStatistic
	{
		@Override
		public void reset()
		{
			this.initialStat.reset();
			this.advanceStat.reset();
		}
		@Override
		public SufficientStatistic update(SufficientStatistic stat) throws BNException
		{
			if(!(stat instanceof TwoSliceStatistics<?>))
				throw new BNException("Failure to update 2 slice statistic.. wrong type.");
			TwoSliceStatistics<?> tmp = (TwoSliceStatistics<?>)stat;
			this.initialStat.update(tmp.initialStat);
			this.advanceStat.update(tmp.advanceStat);
			return this;
		}
	
		StatType initialStat = null;
		StatType advanceStat = null;
	}
	
	/*
	public TwoSliceStatistics<SufficientStatistic> getSufficientStatistic() throws BNException
	{
		TwoSliceStatistics<SufficientStatistic> tss = new TwoSliceStatistics<Distribution.SufficientStatistic>();
		if(this.innerNode.getDistribution(0)!=this.innerNode.getDistribution(1))
		{
			tss.initialStat = this.innerNode.getSufficientStatistic(0);
			tss.advanceStat = this.innerNode.getSufficientStatistic(1);
		}
		else
		{
			tss.advanceStat = this.innerNode.getSufficientStatistic(0);
			this.innerNode.updateSufficientStatistic(1, tss.advanceStat);
		}
		for(int t = 2; t < this.bayesNet.getT(); t++)
			this.innerNode.updateSufficientStatistic(t, tss.advanceStat);
		return tss;
	}
	
	public void updateSufficientStatistic(SufficientStatistic stat) throws BNException
	{
		if(!(stat instanceof TwoSliceStatistics))
			throw new BNException("Failure to optimize parametrs on dynamic node - expected two slice statistic!");
		TwoSliceStatistics<?> tss = (TwoSliceStatistics<?>)stat;
		
		if(this.innerNode.getDistribution(0)!=this.innerNode.getDistribution(1))
		{
			this.innerNode.updateSufficientStatistic(0, tss.initialStat);
			this.innerNode.updateSufficientStatistic(1, tss.advanceStat);
		}
		else
		{
			this.innerNode.updateSufficientStatistic(0, tss.advanceStat);
			this.innerNode.updateSufficientStatistic(1, tss.advanceStat);
		}
		for(int t = 2; t < bayesNet.getT(); t++)
			this.innerNode.updateSufficientStatistic(t, tss.advanceStat);
	}
	
	public double optimizeParameters() throws BNException
	{
		TwoSliceStatistics<SufficientStatistic> tss = this.getSufficientStatistic();
		if(this.innerNode.getDistribution(0)!=this.innerNode.getDistribution(1))
		{
			double err = 0;
			if(tss.initialStat!=null)
				err = this.innerNode.optimize(0, tss.initialStat);
			
			return Math.max(err, this.innerNode.optimize(1, tss.advanceStat));
		}
		else
			return this.innerNode.optimize(0, tss.advanceStat);
	}
	
	public double optimizeParameters(SufficientStatistic stat) throws BNException
	{
		if(!(stat instanceof TwoSliceStatistics))
			throw new BNException("Failure to optimize parametrs on dynamic node - expected two slice statistic!");
		TwoSliceStatistics<?> tss = (TwoSliceStatistics<?>)stat;
		if(this.innerNode.getDistribution(0)!=this.innerNode.getDistribution(1))
		{
			double err = this.innerNode.optimize(0, tss.initialStat);
			return Math.max(err,this.innerNode.optimize(1, tss.advanceStat));
		}
		else
			return this.innerNode.optimize(0, tss.advanceStat);
	}//TODO New internal interfaces - optimizable, etc should allow us to better customize 
	*/
	public abstract void printDistributionInfo(PrintStream ps);
	

	private boolean lastPassBackward = true;

	protected DynamicBayesianNetwork bayesNet;
	protected String name;

	protected HashMap<DBNNode,Integer> interChildren = new HashMap<DBNNode, Integer>();
	protected HashMap<DBNNode,Integer> intraChildren = new HashMap<DBNNode, Integer>();
	protected HashMap<DBNNode,Integer> interParents = new HashMap<DBNNode, Integer>();
	protected HashMap<DBNNode,Integer> intraParents = new HashMap<DBNNode, Integer>();
	
}
