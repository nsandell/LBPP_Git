package bn.impl;

import java.io.PrintStream;

import java.util.HashMap;
import java.util.Map.Entry;

import util.IterableWrapper;

import bn.BNException;
import bn.IDiscreteBayesNode;
import bn.distributions.DiscreteDistribution;
import bn.distributions.Distribution;
import bn.distributions.Distribution.SufficientStatistic;
import bn.interfaces.InnerNode;
import bn.messages.DiscreteMessage;
import bn.messages.Message;
import bn.messages.Message.MessageInterface;

/**
 * Bayes node root class.  Simply has parents and children so we can
 *  to a BFS or DFS to detect cycles, etc.
 * @author Nils F Sandell
 */
abstract class BNNode implements InternalIBayesNode
{
	protected BNNode(StaticBayesianNetwork net, String name, 
					InnerNode<Void> inner)
	{
		this.bnet = net;
		this.name = name;
		this.parentSet =  new IterableWrapper<InternalIBayesNode>(parents.keySet());
		this.childrenSet = new IterableWrapper<InternalIBayesNode>(children.keySet());
		this.inner = inner;
	}
	
	boolean hasChild(BNNode child)
	{
		return (this.children.get(child)!=null);
	}
	
	public final void addChild(BNNode child) throws BNException
	{
		if(this.children.containsKey(child))
			return;
		try
		{
			MessageInterface intrfc = this.inner.newChildInterface(null);
			child.inner.addParentInterface(intrfc, null);
			this.children.put(child,intrfc);
			child.parents.put(this, intrfc);
		} catch(BNException e) {
			throw new BNException("Error adding child " + child.getName() + " to parent " + this.getName() + ": " + e.getMessage());
		}
	}
	
	public void resetMessages()
	{
		this.inner.resetMessages();
	}

	public final void removeChild(BNNode child) throws BNException
	{
		if(!this.children.containsKey(child))
			return;
		this.children.get(child).invalidate();
		this.children.remove(child);
		child.parents.remove(this);
		this.inner.clearInvalidChildren();
		child.inner.clearInvalidParents();
	}
	
	public final void removeAllChildren() throws BNException
	{
		for(Entry<BNNode, MessageInterface> childEntry : this.children.entrySet())
		{
			childEntry.getValue().invalidate();
			childEntry.getKey().parents.remove(this);
			childEntry.getKey().inner.clearInvalidParents();
		}
		this.inner.clearInvalidChildren();
		this.children.clear();
	}
	
	public final void removeAllParents() throws BNException
	{
		for(Entry<BNNode, MessageInterface> parentEntry : this.parents.entrySet())
		{
			parentEntry.getValue().invalidate();
			parentEntry.getKey().children.remove(this);
			parentEntry.getKey().inner.clearInvalidChildren();
		}
		this.inner.clearInvalidParents();
		this.parents.clear();	
	}
	
	public final int numParents()
	{
		return this.parents.size();
	}
	
	public final int numChildren()
	{
		return this.children.size();
	}
	
	public final String getName()
	{
		return this.name;
	}
	
	public final Iterable<InternalIBayesNode> getChildren()
	{
		return this.childrenSet;
	}
	
	public final Iterable<InternalIBayesNode> getParents()
	{
		return this.parentSet;
	}
	
	public final Iterable<BNNode> getChildrenI()
	{
		return this.children.keySet();
	}
	
	public final Iterable<BNNode> getParentsI()
	{
		return this.parents.keySet();
	}
	
	public double updateMessages() throws BNException
	{
		return this.inner.updateMessages(null);
	}
	
	public Object getValue() throws BNException
	{
		return this.inner.getValue(null);
	}
	
	public void setValue(Object value) throws BNException
	{
		this.inner.setValue(null, value);
	}
	
	public SufficientStatistic getSufficientStatistic() throws BNException
	{
		return this.inner.getDistribution(null).getSufficientStatisticObj();
	}
	
	public void printDistributionInfo(PrintStream ps) throws BNException {
		this.inner.getDistribution(null).printDistribution(ps);
	}

	public Distribution getDistribution() throws BNException
	{
		return this.inner.getDistribution(null);
	}
	
	public void setDistribution(Distribution dist) throws BNException
	{
		this.inner.setDistribution(null,dist.copy());
	}
	
	public void clearEvidence()
	{
		this.inner.clearValue();
	}
	
	public Message getMarginal() throws BNException
	{
		return this.inner.getMarginal(null);
	}
	
	public void updateSufficientStatistic(SufficientStatistic stat) throws BNException
	{
		this.inner.updateSufficientStatistic(null, stat);
	}
	
	public double optimizeParameters() throws BNException
	{
		return this.inner.optimize(null, this.inner.getSufficientStatistic(null));
	}
	
	public double optimizeParameters(SufficientStatistic stat) throws BNException
	{
		return this.inner.optimize(null, stat);
	}
	
	public void sample() throws BNException
	{
	}

	@Override
	public final void print(PrintStream ps)
	{
		this.printCreation(ps);

		if(this.inner.getValue(null)!=null)
			ps.print(this.getName() + " = " + this.inner.getValue(null));

		ps.print(this.getName() + "___CPD <");
		this.inner.getDistribution(null).print(ps);
		ps.println(this.getName() + " ~ " + this.getName()+"___CPD");
	}
	
	protected abstract void printCreation(PrintStream ps);
	
	
	public void validate() throws BNException
	{
		try
		{
			this.inner.validate(null);
		} catch(BNException e)
		{
			throw new BNException("Error while validating node " + this.getName() + " : " + e.getMessage());
		}
	}
	
	public Class<?> innerClass()
	{
		return inner.getClass();
	}
	
	static class DiscreteBNNode extends BNNode implements IDiscreteBayesNode
	{
		public DiscreteBNNode(StaticBayesianNetwork net, String name, int cardinality) 
		{
			super(net,name,new DiscreteNode<Void>(cardinality, new StaticContextManager<DiscreteDistribution, 
					DiscreteMessage, Integer>(	DiscreteMessage.normalMessage(cardinality),
												DiscreteMessage.normalMessage(cardinality))));
			this.cardinality = cardinality;
		}
		
		int cardinality;
		
		@Override
		protected void printCreation(PrintStream pr)
		{
			pr.println(this.getName()+":Discrete("+this.getCardinality()+")");
		}
		
		@Override
		public int getCardinality() {
			return this.cardinality;
		}
		
		@Override
		public Integer getValue() throws BNException
		{
			return (Integer)this.inner.getValue(null);
		}

		@Override
		public void setDistribution(DiscreteDistribution dist)
				throws BNException {
			this.inner.setDistribution(null,dist);
		}
		
		@Override
		public DiscreteMessage getMarginal() throws BNException
		{
			return (DiscreteMessage)this.inner.getMarginal(null);
		}

		@Override
		public void setValue(int o) throws BNException {
			this.inner.setValue(null, (Integer)o);
		}

		@Override
		public void clearValue() {
			this.inner.clearValue();
		}
		
		public double betheFreeEnergy() throws BNException
		{
			return this.inner.betheFreeEnergy(null);
		}

		public void sample() {
			//TODO make this work - will not with a hashmap because of the ordering.
		}

		public String getNodeDefinition()
		{
			String ret =  this.getName() + ":Discrete(" + this.getCardinality() + ")\n";
			ret += this.getName() + "__CPT < " + this.inner.getDistribution(null).getDefinition();
			ret += this.getName() + "~" + this.getName() + "__CPT\n";
			if(this.inner.getValue(null)!=null)
				ret += this.getName() + "=" + this.inner.getValue(null).toString() + "\n";
			ret+="\n";
			return ret;
		}

		public String getEdgeDefinition()
		{
			String ret = "";
			for(BNNode child : this.children.keySet())
				ret += this.getName()+"->"+child.getName()+"\n";
			return ret;
		}
	}
	
	
	private static final long serialVersionUID = 50L;
	
	protected StaticBayesianNetwork bnet;
	private String name;

	protected InnerNode<Void> inner;
	
	private IterableWrapper<InternalIBayesNode> childrenSet;
	private IterableWrapper<InternalIBayesNode> parentSet;
	protected HashMap<BNNode,MessageInterface> children = new HashMap<BNNode, MessageInterface>();
	protected HashMap<BNNode,MessageInterface> parents = new  HashMap<BNNode, MessageInterface>();
}
