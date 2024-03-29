package bn.impl.dynbn.fragment.vertical;
/*
import java.util.ArrayList;
import java.util.HashMap;
import bn.BNException;
import bn.IDiscreteDynBayesNode;
import bn.distributions.DiscreteDistribution;
import bn.distributions.Distribution;
import bn.distributions.Distribution.ValueSet.ValueObject;
import bn.impl.dynbn.DBNNode;
import bn.impl.dynbn.fragment.vertical.DBNFragment.FragmentSpot;
import bn.impl.nodengines.FiniteDiscreteNode;
import bn.interfaces.InnerNode;
import bn.messages.FiniteDiscreteMessage;
import bn.messages.Message;
import bn.messages.Message.MessageInterface;
*/
abstract class DBNFragmentNode //extends DBNNode
{	
/*
	protected DBNFragmentNode(DBNFragment dbnf, String name, InnerNode<Integer> inner)
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
			MessageInterface fintf = 
				this.innerNode.newChildInterface(this.bayesNet.getT()-1);
			if(frag.fwdMessageInterface.get(this.getName())==null)
				frag.fwdMessageInterface.put(this.getName(), new HashMap<String, MessageInterface>());
			frag.fwdMessageInterface.get(this.getName()).put(node.getName(), fintf);
		}
		if(frag.fragSpot!=FragmentSpot.Front)
		{
			MessageInterface bintf = 
				new MessageInterface(this.getBlankMessage(),this.getBlankMessage());
			
			this.innerNode.addParentInterface(bintf, 0);
			if(frag.bwdMessageInterface.get(this.getName())==null)
				frag.bwdMessageInterface.put(this.getName(), new HashMap<String, MessageInterface>());
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
				  new FiniteDiscreteNode<Integer>(cardinality,
					new DynamicFragmentManager<DiscreteDistribution,FiniteDiscreteMessage, Integer>(
										getMessageSet(cardinality, net.getT()),
										getMessageSet(cardinality, net.getT()),
										net.fragSpot)));
			this.cardinality = cardinality;
		}
		int cardinality;
	
		public double betheFreeEnergy() throws BNException
		{
			double energy = 0;
			for(int i = 0; i < this.getT(); i++)
				energy += this.innerNode.betheFreeEnergy(i);
			return energy;
		}
		
		private static final ArrayList<FiniteDiscreteMessage> getMessageSet(int cardinality, int T)
		{
			ArrayList<FiniteDiscreteMessage> ret = new ArrayList<FiniteDiscreteMessage>(T);
			for(int i = 0; i < T; i++)
				ret.add(FiniteDiscreteMessage.normalMessage(cardinality));
			return ret;
		}
		
		public void sample() throws BNException
		{
			for(int t = 0; t < this.bayesNet.getT(); t++)
				this.sample(t);
		}
		

		public FiniteDiscreteMessage getBlankMessage()
		{
			return FiniteDiscreteMessage.normalMessage(this.cardinality);
		}
		
		@Override
		public DiscreteDistribution getInitialDistribution() {
			return (DiscreteDistribution) this.innerNode.getDistribution(0);
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
		public FiniteDiscreteMessage getMarginal(int t) throws BNException
		{
			return (FiniteDiscreteMessage)this.innerNode.getMarginal(t);
		}
		
		public String getNodeDefinition(){return null;}//TODO Implement these I guess?
		public String getEdgeDefinition(){return null;}
		
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
		
		private static final long serialVersionUID = 50L;
	}

	protected abstract Message getBlankMessage();
	private static final long serialVersionUID = 50L;
	protected DBNFragment frag;
	*/
}
