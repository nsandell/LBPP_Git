package bn;

import bn.BayesNet.BNException;
import bn.distributions.DiscreteDistribution;
import bn.interfaces.DiscreteDBNNodeI;
import bn.messages.DiscreteMessage;

public class DiscreteDBNNode extends DBNNode<DiscreteBNNode> implements DiscreteDBNNodeI {
	
	public DiscreteDBNNode(DynamicBayesNetwork bn, BayesNet unrolled, String basename, int cardinality) throws BNException
	{
		super(bn);
		for(int t = 0; t < bn.getT(); t++)
			this.nodeInstances.set(t,unrolled.addDiscreteNode(basename+"["+t+"]", cardinality));
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
		if(tmax >= this.bayesNet.getT() || t0 < 0)
			throw new BNException("Attempted to set sequence of values out of boounds ("+t0+","+tmax+")");
		for(int t = t0; t <= tmax; t++)
			this.nodeInstances.get(t).setValue(values[t-t0]);
	}
}
