package bn.impl;

import java.io.PrintStream;
import java.util.ArrayList;

import bn.BNException;
import bn.IDiscreteBayesNode;
import bn.IDiscreteDynBayesNode;
import bn.distributions.DiscreteDistribution;
import bn.distributions.Distribution.DiscreteSufficientStatistic;
import bn.distributions.Distribution.SufficientStatistic;

import bn.messages.DiscreteMessage;

class DiscreteDBNNode extends DBNNode<DiscreteBNNode> implements IDiscreteDynBayesNode {
	
	
	public DiscreteDBNNode(DynamicBayesianNetwork bn, StaticBayesianNetwork unrolled, String basename, int cardinality) throws BNException
	{
		super(bn,basename);
		ArrayList<DiscreteBNNode> instanceBuilder = new ArrayList<DiscreteBNNode>();
		for(int t = 0; t < bn.getT(); t++)
			instanceBuilder.add(unrolled.addDiscreteNode(basename+"["+t+"]",cardinality));
		this.nodeInstances.addAll(instanceBuilder);
	}

	@Override
	public void setInitialDistribution(DiscreteDistribution dist) throws BNException
	{
		this.init = dist.copy();
		try {
			this.nodeInstances.get(0).setDistribution(this.init);
		} catch(BNException e) {this.init = null; throw new BNException(e);}
	}

	@Override
	public void setAdvanceDistribution(DiscreteDistribution dist) throws BNException
	{
		this.adva = dist.copy();
		try
		{
			if(this.init==null)
				this.nodeInstances.get(0).setDistribution(this.adva);
			for(int t = 1; t < this.bayesNet.getT(); t++)
				this.nodeInstances.get(t).setDistribution(this.adva);
			
		} catch(BNException e){this.adva = null; throw new BNException(e);}
	}
	
	private DiscreteDistribution init = null;
	private DiscreteDistribution adva = null;

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
	
	public double optimizeParameters() throws BNException
	{
		double initchange = 0, advachange = 0;
		TwoSliceStatistics<DiscreteSufficientStatistic> tss = this.getSufficientStatistic();
		if(this.init!=null)
			initchange = this.init.optimize(tss.initialStat);
		advachange = this.adva.optimize(tss.advanceStat);
		return Math.max(initchange, advachange);
	}
	
	public void printDistributionInfo(PrintStream ps) throws BNException
	{
		if(this.init!=null)
		{
			ps.println("Initial Distribution for node " + this.name);
			this.init.printDistribution(ps);
		}
		if(this.adva==null)
			throw new BNException("Attempted to print node distribution where distribution hasn't been set!");
		ps.println("Distribution for node " + this.name);
		this.adva.printDistribution(ps);
	}
	
	public double optimizeParameters(SufficientStatistic stat) throws BNException
	{
		double initchange = 0, advachange = 0;
		if(!(stat instanceof TwoSliceStatistics<?>))
			throw new BNException("Can't optimize DBN parameters with non two-slice statistic.");
		TwoSliceStatistics<?> tss = (TwoSliceStatistics<?>)stat;
		if(this.init!=null)
			initchange = this.init.optimize(tss.initialStat);
		advachange = this.adva.optimize(tss.advanceStat);
		return Math.max(initchange, advachange);
	}

	@Override
	public TwoSliceStatistics<DiscreteSufficientStatistic> getSufficientStatistic() throws BNException {
		TwoSliceStatistics<DiscreteSufficientStatistic> tss = new TwoSliceStatistics<DiscreteSufficientStatistic>();
		if(this.init!=null)
		{
			tss.initialStat = this.nodeInstances.get(0).getSufficientStatistic();
			tss.advanceStat = this.nodeInstances.get(1).getSufficientStatistic();
		}
		else
		{
			tss.initialStat = null;
			tss.advanceStat = this.nodeInstances.get(0).getSufficientStatistic();
			this.nodeInstances.get(1).updateSufficientStatistic(tss.advanceStat);
		}
		for(int t = 2; t < this.bayesNet.getT(); t++)
			this.nodeInstances.get(t).updateSufficientStatistic(tss.advanceStat);
		return tss;
	}
	
	public void sample() throws BNException
	{
		for(int t = 0; t < this.nodeInstances.size(); t++)
			this.nodeInstances.get(t).sample();
	}

	@Override
	public void updateSufficientStatistic(SufficientStatistic stat)
			throws BNException {
		if(!(stat instanceof TwoSliceStatistics<?>))
			throw new BNException("Cannot update DBN two slice statistic, improper type!");
		TwoSliceStatistics<?> tss = (TwoSliceStatistics<?>)stat;
		if(this.init!=null)
			this.nodeInstances.get(0).updateSufficientStatistic(tss.initialStat);
		else
			this.nodeInstances.get(0).updateSufficientStatistic(tss.advanceStat);
		for(int t = 1; t < this.bayesNet.getT(); t++)
			this.nodeInstances.get(t).updateSufficientStatistic(tss.advanceStat);
	}
	
	public int getValue(int t) throws BNException
	{
		if(t<0 || t>= this.bayesNet.getT())
			throw new BNException("Attempted to get value out of t range (" + t + " vs [0,"+this.bayesNet.getT()+"])");
		return nodeInstances.get(t).getValue();
	}
}
