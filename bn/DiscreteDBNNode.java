package bn;

import bn.distributions.DiscreteDistribution;

import bn.interfaces.IDiscreteBayesNode;
import bn.interfaces.IDiscreteDynBayesNode;
import bn.messages.DiscreteMessage;

class DiscreteDBNNode extends DBNNode<DiscreteBNNode> implements IDiscreteDynBayesNode {
	
	public DiscreteDBNNode(DynamicBayesianNetwork bn, StaticBayesianNetwork unrolled, String basename, int cardinality) throws BNException
	{
		super(bn,basename);
		for(int t = 0; t < bn.getT(); t++)
			this.nodeInstances.add(unrolled.addDiscreteNode(basename+"["+t+"]",cardinality));
//			this.nodeInstances.set(t,unrolled.addDiscreteNode(basename+"["+t+"]", cardinality));
	}

	@Override
	public void setInitialDistribution(DiscreteDistribution dist) throws BNException
	{
		this.nodeInstances.get(0).setDistribution(dist);
	}

	@Override
	public void setAdvanceDistribution(DiscreteDistribution dist) throws BNException
	{
		for(int t = 1; t < this.bayesNet.getT(); t++)
			this.nodeInstances.get(t).setDistribution(dist);
	}

	@Override
	public DiscreteMessage getMarginal(int t) throws BNException
	{
		return this.nodeInstances.get(t).getMarginal();
	}

	@Override
	public void setValue(int t, int value) throws BNException
	{
		this.nodeInstances.get(t).setValue(value);
	}

	@Override
	public void setValue(int[] values, int t0) throws BNException
	{
		int tmax = t0+values.length;
		if(tmax > this.bayesNet.getT() || t0 < 0)
			throw new BNException("Attempted to set sequence of values out of boounds ("+t0+","+tmax+")");
		for(int t = t0; t < tmax; t++)
			this.nodeInstances.get(t).setValue(values[t-t0]);
	}

	@Override
	public void sendInitialMessages() throws BNException {
		for(DiscreteBNNode nd : this.nodeInstances)
			nd.sendInitialMessages();
	}

	@Override
	public double updateMessages() throws BNException {
		double max = 0;
		for(DiscreteBNNode nd : this.nodeInstances)
			max = Math.max(max, nd.updateMessages());
		return max;
	}
	
	public double updateMessages(int tmin, int tmax) throws BNException
	{
		double max = 0;
		DiscreteBNNode[] arr = this.nodeInstances.subList(tmin, tmax+1).toArray(new DiscreteBNNode[1]);
		for(int t = 0; t < arr.length; t++)
			max = Math.max(max, arr[t].updateMessages());
		return max;
		/*
		for(int t = tmin; t <= tmax; t++)
		{
			max = Math.max(max, this.nodeInstances.get(t).updateMessages());
		}
		*/
	}

	@Override
	public IDiscreteBayesNode getDiscreteInstance(int t) {
		return this.nodeInstances.get(t);
	}
}
