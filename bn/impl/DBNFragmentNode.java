package bn.impl;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import bn.BNException;
import bn.IDiscreteDynBayesNode;
import bn.distributions.DiscreteDistribution;
import bn.distributions.Distribution;
import bn.distributions.Distribution.ValueSet.ValueObject;
import bn.impl.DBNFragment.FragmentSpot;
import bn.interfaces.InnerNode;
import bn.messages.DiscreteMessage;
import bn.messages.Message;

abstract class DBNFragmentNode extends DBNNode {
	
	protected DBNFragmentNode(DBNFragment dbnf, String name, InnerNode inner)
	{
		super(dbnf,name,inner);
		this.frag = dbnf;
	}
	
	@Override
	public void addInterChild(DBNNode node) throws BNException
	{
		super.addInterChild(node);
		if(frag.fragSpot!=FragmentSpot.Back)
		{
			Message.MessageInterface<? extends Message> fintf = 
				this.innerNode.newChildInterface(this.bayesNet.getT()-1);
			if(frag.fwdMessageInterface.get(this.getName())==null)
				frag.fwdMessageInterface.put(this.getName(), new HashMap<String, Message.MessageInterface<? extends Message>>());
			frag.fwdMessageInterface.get(this.getName()).put(node.getName(), fintf);
		}
		if(frag.fragSpot!=FragmentSpot.Front)
		{
			Message.MessageInterface<? extends Message> bintf = 
				new Message.MessageInterface<Message>(
						this.getBlankMessage(),this.getBlankMessage(),this.getBlankMessage());
			
			this.innerNode.addParentInterface(bintf, 0);
			if(frag.bwdMessageInterface.get(this.getName())==null)
				frag.bwdMessageInterface.put(this.getName(), new HashMap<String, Message.MessageInterface<? extends Message>>());
			frag.bwdMessageInterface.get(this.getName()).put(node.getName(),bintf);
		}
	}
	
	@Override
	public void removeInterChild(DBNNode node) throws BNException
	{
		super.removeInterChild(node);
		if(frag.fragSpot!=FragmentSpot.Back)
			this.frag.fwdMessageInterface.get(this.getName()).remove(node.getName());
		if(frag.fragSpot!=FragmentSpot.Front)
			this.frag.bwdMessageInterface.get(this.getName()).remove(node.getName());
	}
	
	static class DiscreteDBNFNode extends DBNFragmentNode implements IDiscreteDynBayesNode, ValueObject<Integer>
	{
		public DiscreteDBNFNode(DBNFragment net, String name, int cardinality) throws BNException
		{
			super(net,name,
				  new DiscreteNode<Integer>(cardinality,
					new DynamicFragmentManager<DiscreteDistribution,DiscreteMessage, Integer>(
										getMessageSet(cardinality, net.getT()),
										getMessageSet(cardinality, net.getT()),
										net.fragSpot)));
			this.cardinality = cardinality;
		}
		int cardinality;
	
		@Override
		protected void printCreation(PrintStream pr)
		{
			pr.println(this.getName()+":Discrete("+this.getCardinality()+")");
		}
		
		private static final ArrayList<DiscreteMessage> getMessageSet(int cardinality, int T)
		{
			ArrayList<DiscreteMessage> ret = new ArrayList<DiscreteMessage>(T);
			for(int i = 0; i < T; i++)
				ret.add(DiscreteMessage.normalMessage(cardinality));
			return ret;
		}
		
		public void sample() throws BNException
		{
			for(int t = 0; t < this.bayesNet.getT(); t++)
				this.sample(t);
		}
		

		public DiscreteMessage getBlankMessage()
		{
			return DiscreteMessage.normalMessage(this.cardinality);
		}
		
		@Override
		public DiscreteDistribution getInitialDistribution() {
			return (DiscreteDistribution) this.contextManager.getInitial();
		}

		@Override
		public void setInitialDistribution(DiscreteDistribution dist)
				throws BNException {
			this.contextManager.setInitialDistribution(dist);
		}

		@Override
		public void setAdvanceDistribution(DiscreteDistribution dist)
				throws BNException {
			this.contextManager.setAdvanceDistribution(dist);
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
			return (DiscreteMessage)this.contextManager.getMarginal(t);
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
			this.setValue(t, ((DiscreteDistribution)this.contextManager.getCPD(t)).sample(new Distribution.ValueSet<Integer>(pvals)));
		}
		
		private static final long serialVersionUID = 50L;
	}

	protected abstract Message getBlankMessage();
	private static final long serialVersionUID = 50L;
	protected DBNFragment frag;
}
