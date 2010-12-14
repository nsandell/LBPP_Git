package bn.impl;

import java.io.PrintStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.Map.Entry;


import util.IterableWrapper;

import bn.BNException;
import bn.IDiscreteDynBayesNode;
import bn.IDynBayesNode;
import bn.distributions.DiscreteDistribution;
import bn.distributions.Distribution;
import bn.distributions.Distribution.SufficientStatistic;
import bn.distributions.Distribution.ValueSet.ValueObject;
import bn.interfaces.InnerNode;
import bn.messages.DiscreteMessage;
import bn.messages.Message;
import bn.messages.Message.MessageInterface;

abstract class DBNNode implements InternalIBayesNode, IDynBayesNode
{
	protected DBNNode(DynamicBayesianNetwork net,String name,
						InnerNode<Integer> inner)
	{
		this.innerNode = inner;
		this.bayesNet = net;
		this.name = name;
	}
	
	boolean hasInterChild(DBNNode child)
	{
		return this.interChildren.containsKey(child);
	}
	
	boolean hasIntraChild(DBNNode child)
	{
		return this.intraChildren.containsKey(child);
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
	
	public final String getName()
	{
		return this.name;
	}
	
	void chooseLLPath()
	{ //TODO this will work for now but really isn't the best idea.
		if(this.interChildren.containsKey(this))
			this.llTreeParent = new ParentPair(this,true);
		else if(this.intraParents.size() > 0)
			this.llTreeParent = new ParentPair(this.intraParents.get(0),false);
		else
		{
			this.llTreeParent = null;//TODO Is UM?
		}
	}
	
	abstract double getLLAdjust() throws BNException; //TODO Ditto
	
	public void addInterChild(DBNNode child) throws BNException
	{
		if(this.interChildren.containsKey(child))
			throw new BNException("Attempted to add child that already exists.");
		MessageInterface[] added = new MessageInterface[this.bayesNet.getT()-1];
		int t = -1;
		try
		{
			for(t = 0; t < this.bayesNet.getT()-1; t++)
			{
				added[t] = this.innerNode.newChildInterface(t);
				child.innerNode.addParentInterface(added[t], t+1);
			}
			this.interChildren.put(child,added);
			child.interParents.add(this);
			child.parents.add(new ParentPair(this, true));
		} catch(BNException e) {
			for(int t2 = 0; t2 <= t; t2++)
			{
				added[t2].invalidate();
			}
			child.innerNode.clearInvalidParents();
			this.innerNode.clearInvalidChildren();
		}
	}
	
	private void removeParentPair(DBNNode parent) throws BNException
	{
		int index = -1;
		for(int i=  0; i < this.parents.size(); i++)
		{
			if(this.parents.get(i).parent==parent)
				index= i;
		}
		if(index >=0)
			this.parents.remove(index);
		else throw new BNException("Unable to remove parent pair! Not found.");
	}
	
	public void resetMessages()
	{
		this.innerNode.resetMessages();
	}
	
	public final void addIntraChild(DBNNode child) throws BNException
	{
		if(this.intraChildren.containsKey(child))
			throw new BNException("Atttempted to add child that already exists.");
		MessageInterface[] added = new MessageInterface[this.bayesNet.getT()];
		int t = -1;
		try
		{
			for(t = 0; t < this.bayesNet.getT(); t++)
			{
				added[t] = this.innerNode.newChildInterface(t);
				child.innerNode.addParentInterface(added[t], t);
			}
			this.intraChildren.put(child,added);
			child.intraParents.add(this);
			child.parents.add(new ParentPair(this,false));
		} catch(BNException e) {
			for(int t2 = 0; t2 <= t; t2++)
			{
				added[t2].invalidate();
			}
			child.innerNode.clearInvalidParents();
			this.innerNode.clearInvalidChildren();
		}
	}

	public void removeInterChild(DBNNode child) throws BNException
	{
		if(!this.intraChildren.containsKey(child))
			throw new BNException("Attempted to remove inter-child " + child.name + " from node " + this.name + " where it is not a child.");
		MessageInterface[] ints = this.interChildren.get(child);
		try
		{
			for(int i = 0; i < ints.length; i++)
				ints[i].invalidate();
			child.innerNode.clearInvalidParents();
			this.innerNode.clearInvalidChildren();
		} catch(BNException e) {
			String err = "Unexpected exception removing child - network is in bad state - do not continue.";
			System.err.println(err);
			throw new BNException(err + " : " + e.getMessage());
		}
		child.interParents.remove(this);
		child.removeParentPair(this);
		this.interChildren.remove(child);
	}
	
	public final void removeAllChildren() throws BNException
	{
		for(Entry<DBNNode, MessageInterface[]> entry : this.intraChildren.entrySet())
		{
			for(int i = 0; i < entry.getValue().length; i++)
				entry.getValue()[i].invalidate();
			entry.getKey().intraParents.remove(this);
			entry.getKey().innerNode.clearInvalidParents();
		}
		this.innerNode.clearInvalidChildren();
		this.intraChildren.clear();
		for(Entry<DBNNode, MessageInterface[]> entry : this.interChildren.entrySet())
		{
			for(int i = 0; i < entry.getValue().length; i++)
				entry.getValue()[i].invalidate();
			entry.getKey().interParents.remove(this);
			entry.getKey().parents.remove(this);
			entry.getKey().innerNode.clearInvalidParents();
		}
		this.innerNode.clearInvalidChildren();
		this.interChildren.clear();
	}
	
	public Message getMarginal(int t) throws BNException
	{
		return this.innerNode.getMarginal(t);
	}
	
	public final void removeAllParents() throws BNException
	{
		ArrayList<DBNNode> intraParentCopy = new ArrayList<DBNNode>(this.intraParents);
		for(DBNNode parent : intraParentCopy)
			parent.removeIntraChild(this);
		ArrayList<DBNNode> interParentCopy = new ArrayList<DBNNode>(this.interParents);
		for(DBNNode parent : interParentCopy)
			parent.removeInterChild(this);
	}
	
	public void setEvidence(int t0, Object[] ev) throws BNException
	{
		for(int t = 0; t < ev.length; t++)
			this.innerNode.setValue(t0+t, ev[t]);
	}
	
	/*public void invalidate() //TODO THink this is no longer needed, verify!
	{
		for(int t = 0; t < this.bayesNet.getT(); t++)
			this.contextManager.getLocalLambda(t).invalidate();
		for(int t = 0; t < this.bayesNet.getT(); t++)
			this.contextManager.getLocalPi(t).invalidate();
	}*/
	
	public void setEvidence(int t, Object ev) throws BNException
	{
		this.innerNode.setValue(t, ev);
	}
	
	public Distribution getInitialDistribution()
	{
		return this.innerNode.getDistribution(0);
	}
	
	public void setInitialDistribution(Distribution dist) throws BNException
	{
		this.innerNode.setDistribution(0,dist.copy());
	}
	
	public void setAdvanceDistribution(Distribution dist) throws BNException
	{
		this.innerNode.setDistribution(1,dist.copy());
	}
	
	public void setDistribution(Distribution dist) throws BNException
	{
		this.innerNode.setDistribution(1,dist.copy());
	}
	
	public final void removeIntraChild(DBNNode child) throws BNException
	{
		if(!this.intraChildren.containsKey(child))
			throw new BNException("Attempted to remove intra-child " + child.name + " from node " + this.name + " where it is not a child.");
		MessageInterface[] ints = this.intraChildren.get(child);
		try
		{
			for(int i = 0; i < ints.length; i++)
				ints[i].invalidate();
			child.innerNode.clearInvalidParents();
			this.innerNode.clearInvalidChildren();
		} catch(BNException e) {
			String err = "Unexpected exception removing child - network is in bad state - do not continue.";
			System.err.println(err);
			throw new BNException(err + " : " + e.getMessage());
		}
		child.intraParents.remove(this);
		child.removeParentPair(this);
		this.intraChildren.remove(child);
	}
	
	public boolean hasInterChild(IDynBayesNode child)
	{
		return this.interChildren.containsKey(child);
	}
	public boolean hasIntraChild(IDynBayesNode child)
	{
		return this.intraChildren.containsKey(child);
	}
	public boolean hasInterParent(IDynBayesNode parent)
	{
		return this.interParents.contains(parent);
	}
	public boolean hasIntraParent(IDynBayesNode parent)
	{
		return this.intraParents.contains(parent);
	}
	
	public void validate() throws BNException
	{
		for(int i = 0; i < this.bayesNet.getT(); i++)
			this.innerNode.validate(i);
	}
	
	public Iterable<DBNNode> getInterChildren()
	{
		return this.interChildren.keySet();
	}
	
	public Iterable<DBNNode> getInterParents()
	{
		return this.interParents;
	}
	
	public Iterable<InternalIBayesNode> getChildren()
	{
		return new IterableWrapper<InternalIBayesNode>(this.intraChildren.keySet());
	}
	
	public Iterable<InternalIBayesNode> getParents()
	{
		return new IterableWrapper<InternalIBayesNode>(this.intraParents);
	}
	
	public Iterable<DBNNode> getChildrenI()
	{
		return this.intraChildren.keySet();
	}
	
	public Iterable<DBNNode> getParentsI()
	{
		return this.intraParents;
	}
	
	public Iterable<DBNNode> getIntraChildren()
	{
		return this.intraChildren.keySet();
	}
	
	public Iterable<DBNNode> getIntraParents()
	{
		return this.intraParents;
	}
	
	public Iterable<DBNNode> getInterParentsI()
	{
		return this.interParents;
	}
	
	public Iterable<DBNNode> getIntraParentsI()
	{
		return this.intraParents;
	}
	
	public Iterable<DBNNode> getInterChildrenI()
	{
		return this.interChildren.keySet();
	}
	
	public Iterable<DBNNode> getIntraChildrenI()
	{
		return this.intraChildren.keySet();
	}
	
	private boolean forward = true;
	
	public final double updateMessages(int tmin, int tmax) throws BNException
	{
		double maxErr = 0;
		maxErr = Math.max(maxErr, this.updateMessages(tmin, tmax,forward));
		forward = !forward;
		return maxErr;
	}
	
	public final double updateMessages(int tmin, int tmax, boolean forward) throws BNException
	{
		double maxErr = 0;
		if(forward)
		{
			for(int t = tmin; t <= tmax; t++)
				maxErr = Math.max(this.innerNode.updateMessages(t),maxErr);
		}
		else
		{
			for(int t = tmax; t >= tmin; t--)
				maxErr = Math.max(this.innerNode.updateMessages(t), maxErr);
		}
		return maxErr;
	}

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
	
	public void clearEvidence()
	{
		this.innerNode.clearValue();
	}
	
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
	}
	
	public void printDistributionInfo(PrintStream ps)
	{
		if(this.innerNode.getDistribution(0)!=this.innerNode.getDistribution(1))
		{
			ps.println("Initial distribution for node " + this.getName() + " : ");
			this.innerNode.getDistribution(0).printDistribution(ps);
		}
		if(this.innerNode.getDistribution(1)!=null)
		{
			ps.println("Advance distribution for node " + this.getName() + " : ");
			this.innerNode.getDistribution(1).printDistribution(ps);
		}
	}
	
	@Override
	public void print(PrintStream ps)
	{
		this.printCreation(ps);
		
		Vector<Object> values = new Vector<Object>(); 
		int startSpot = -1;
		for(int t = 0; t < this.getT(); t++)
		{
			Object value = this.innerNode.getValue(t);
			if(startSpot==-1 && value!=null)
				startSpot = t;
			else if(startSpot!=-1 && value==null)
			{
				ps.print(this.getName()+"("+startSpot+") =");
				for(int i = 0; i < values.size(); i++)
					ps.print(" "  + values.get(i));
				ps.println();
				startSpot =-1;
				values.clear();
			}

			if(value!=null)
			{
				values.add(value);
			}
		}
		
		if(this.innerNode.getDistribution(0)!=this.innerNode.getDistribution(1));
		{
			ps.print(this.getName()+"___CPD__INIT < ");
			this.innerNode.getDistribution(0).print(ps);
			ps.println(this.getName() + " ~~ " + this.getName() + "___CPD__INIT");
		}
		ps.print(this.getName()+"___CPD__ADVA < ");
		this.innerNode.getDistribution(1).print(ps);
		ps.println(this.getName() + " ~ " + this.getName() + "___CPD__ADVA");
	}
	
	protected abstract void printCreation(PrintStream ps);

	public double updateMessages() throws BNException
	{
		return updateMessages(0,this.bayesNet.getT()-1);
	}

	protected int getT()
	{
		return this.bayesNet.getT();
	}
	
	static class DiscreteDBNNode extends DBNNode implements IDiscreteDynBayesNode, ValueObject<Integer>
	{
		public DiscreteDBNNode(DynamicBayesianNetwork net, String name, int cardinality) throws BNException
		{
			super(net,name,new DiscreteNode<Integer>(cardinality, new DynamicContextManager<DiscreteDistribution,
					DiscreteMessage, Integer>(	getMessageSet(cardinality, net.getT()),
												getMessageSet(cardinality, net.getT()))));
			this.cardinality = cardinality;
		}
		int cardinality;
		
		private static final ArrayList<DiscreteMessage> getMessageSet(int cardinality, int T)
		{
			ArrayList<DiscreteMessage> ret = new ArrayList<DiscreteMessage>(T);
			for(int i = 0; i < T; i++)
				ret.add(DiscreteMessage.normalMessage(cardinality));
			return ret;
		}
		
		public double betheFreeEnergy() throws BNException
		{
			double sum = 0;
			for(int i = 0; i < this.getT(); i++)
				sum += this.innerNode.betheFreeEnergy(i);
			return sum;
		}
		
		public void sample() throws BNException
		{
			for(int t = 0; t < this.bayesNet.getT(); t++)
				this.sample(t);
		}

		@Override
		protected void printCreation(PrintStream pr)
		{
			pr.println(this.getName()+":Discrete("+this.getCardinality()+")");
		}
		
		@Override
		public DiscreteDistribution getInitialDistribution() {
			return (DiscreteDistribution) this.innerNode.getDistribution(0);
		}
		
		@Override
		double getLLAdjust() throws BNException
		{
			double ll = 0;
			Vector<DBNNode> llInterChildren = new Vector<DBNNode>();
			Vector<DBNNode> llIntraChildren = new Vector<DBNNode>();
			for(DBNNode child : this.interChildren.keySet())
				if(child.llTreeParent.parent==this && child.llTreeParent.inter)
					llInterChildren.add(child);
			for(DBNNode child : this.intraChildren.keySet())
				if(child.llTreeParent.parent==this && !child.llTreeParent.inter)
					llIntraChildren.add(child);
			for(int t = 0; t < this.getT(); t++)
			{ 	
				DiscreteMessage tmp;
				boolean hasAny = false;
				if(this.innerNode.getValue(t)==null)
					tmp = DiscreteMessage.allOnesMessage(this.cardinality);
				else
				{
					tmp = new DiscreteMessage(this.cardinality);
					tmp.setValue((Integer)this.innerNode.getValue(t), 1);
				}
				if(t<this.getT()-1)
				{
					for(DBNNode child : llInterChildren)
					{
						hasAny = true;
						tmp = tmp.multiply((DiscreteMessage)this.interChildren.get(child)[t].lambda);
					}
				}
				for(DBNNode child : llIntraChildren)
				{
					hasAny = true;
					tmp = tmp.multiply((DiscreteMessage)this.intraChildren.get(child)[t].lambda);
				}
				if(hasAny)
					ll += Math.log(tmp.normalize());
			}
			ll += this.innerNode.getMarginalNorm(0);
			return ll;
		}

		@Override
		public void setInitialDistribution(DiscreteDistribution dist)
				throws BNException {
			this.innerNode.setDistribution(0,dist);
		}

		@Override
		public void setAdvanceDistribution(DiscreteDistribution dist)
				throws BNException {
			this.innerNode.setDistribution(1,dist);
		}

		@Override
		public void setValue(int t, int value) throws BNException {
			this.innerNode.setValue(t, value);
		}

		@Override
		public void setValue(int[] values, int t0) throws BNException {
			for(int i = 0; i < values.length; i++)
				this.setValue(t0+i,values[i]);
		}

		@Override
		public Integer getValue(int t) throws BNException {
			return (Integer)this.innerNode.getValue(t);
		}

		@Override
		public int getCardinality() {
			return this.cardinality;
		}
		
		@Override
		public DiscreteMessage getMarginal(int t) throws BNException
		{
			return (DiscreteMessage)this.innerNode.getMarginal(t);
		}
		
		public void sample(int t) throws BNException
		{
			Integer[] pvals;
			if(t==0)
			{
				pvals = new Integer[this.intraParents.size()];
				for(int i = 0; i < pvals.length; i++)
					pvals[i] = ((DiscreteDBNNode)this.intraParents.get(i)).getValue(t);
			}
			else
			{
				pvals = new Integer[this.parents.size()];
				for(int i = 0; i < pvals.length; i++)
					pvals[i] = ((DiscreteDBNNode)this.parents.get(i).parent).getValue(t - (this.parents.get(i).inter ? 1 : 0));
			}
			this.setValue(t, ((DiscreteDistribution)this.innerNode.getDistribution(t)).sample(new Distribution.ValueSet<Integer>(pvals)));
		}
		
		public String getNodeDefinition()
		{
			String ret = this.getName()+":Discrete("+this.getCardinality()+")\n";
			boolean inSequence = false;
			for(int i = 0; i < this.getT(); i++)
			{
				if(this.innerNode.getValue(i)!=null)
				{
					if(inSequence)
						ret += " " + this.innerNode.getValue(i);
					else
					{
						ret += this.getName() +"("+i+") = " + this.innerNode.getValue(i);
						inSequence = true;
					}
				}
				else if(inSequence)
				{
					inSequence = false;
					ret += "\n";
				}
			}
			if(inSequence)
				ret+="\n";
			if(this.innerNode.getDistribution(0)!=this.innerNode.getDistribution(1))
			{
				ret += this.getName()+"__CPT__INITIAL < " +this.innerNode.getDistribution(0).getDefinition()
					+  this.getName()+"~~"+this.getName()+"__CPT__INITIAL\n";
			}
			ret += this.getName()+"__CPT < " + this.innerNode.getDistribution(1).getDefinition();
			ret += this.getName() + "~" + this.getName()+"__CPT\n\n";
			return ret;
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
	}
	
	protected InnerNode<Integer> innerNode;
	protected HashMap<DBNNode,MessageInterface[]> interChildren = new HashMap<DBNNode, MessageInterface[]>();
	protected HashMap<DBNNode,MessageInterface[]> intraChildren = new HashMap<DBNNode, MessageInterface[]>();
	protected ArrayList<DBNNode> interParents = new ArrayList<DBNNode>();
	protected ArrayList<DBNNode> intraParents = new ArrayList<DBNNode>();
	protected ArrayList<ParentPair> parents = new ArrayList<ParentPair>();
	protected DynamicBayesianNetwork bayesNet;
	protected ParentPair llTreeParent = null;
	protected String name;
	
	class ParentPair
	{
		public ParentPair(DBNNode parent, boolean inter)
		{
			this.parent = parent;
			this.inter = inter;
		}
		
		DBNNode parent;
		boolean inter;
	}
}
