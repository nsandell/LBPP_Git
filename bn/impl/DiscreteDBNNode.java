package bn.impl;

import java.util.ArrayList;

import bn.BNException;
import bn.IDiscreteBayesNode;
import bn.IDiscreteDynBayesNode;
import bn.distributions.DiscreteDistribution;

import bn.messages.DiscreteMessage;

class DiscreteDBNNode extends DBNNode<DiscreteBNNode> implements IDiscreteDynBayesNode {
	
	public DiscreteDBNNode(DynamicBayesianNetwork bn, StaticBayesianNetwork unrolled, String basename, int cardinality) throws BNException
	{
		super(bn,basename);
		ArrayList<DiscreteBNNode> instanceBuilder = new ArrayList<DiscreteBNNode>();
		for(int t = 0; t < bn.getT(); t++)
			instanceBuilder.add(unrolled.addDiscreteNode(basename+"["+t+"]",cardinality));
		this.nodeInstances.addAll(instanceBuilder);
			//this.nodeInstances.add(unrolled.addDiscreteNode(basename+"["+t+"]",cardinality));
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
	
	public int[] getSlice0ParentDim()
	{
		return this.nodeInstances.get(0).getParentDimensions();
	}
	
	public int[] getSlice1ParentDim()
	{
		return this.nodeInstances.get(1).getParentDimensions();
	}
	
	public int getCardinality()
	{
		return this.nodeInstances.get(0).getCardinality();
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
	public double updateMessages() throws BNException {
		double max = 0;
		if(forward)
		{
			for(DiscreteBNNode nd : this.nodeInstances)
				max = Math.max(max, nd.updateMessages());
		}
		else
		{
			for(int i = this.nodeInstances.size()-1; i >= 0; i--)
				max = Math.max(max, this.nodeInstances.get(i).updateMessages());
		}
		forward = !forward;
		return max;
	}
	boolean forward = true;
	
	@Override
	protected double updateMessagesI(int tmin, int tmax) throws BNException
	{
		double max = 0;
		if(forward)
		{
			for(int t = tmin; t <= tmax; t++)
				max = Math.max(max, this.nodeInstances.get(t).updateMessages());
		}
		else
		{
			for(int t = tmax; t>=tmin; t--)
				max = Math.max(max, this.nodeInstances.get(t).updateMessages());
		}
		forward = !forward;
		return max;
	}
	
	public double getLogLikelihood() 
	{
		double ll = 0;
		for(int i = 0; i < bayesNet.getT(); i++)
		{
			if(this.nodeInstances.get(i).isObserved())
				ll += Math.log(this.nodeInstances.get(i).getLogLikelihood());
		}
		return ll;
	}

	@Override
	public IDiscreteBayesNode getDiscreteInstance(int t) {
		return this.nodeInstances.get(t);
	}
	
	public void clearEvidence()
	{
		for(DiscreteBNNode node : this.nodeInstances)
			node.clearEvidence();
	}
}
