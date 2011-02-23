package bn.impl.dynbn;

import java.io.PrintStream;


import java.util.ArrayList;

import bn.BNException;
import bn.Optimizable;
import bn.distributions.DiscreteDistribution;
import bn.distributions.DiscreteDistribution.DiscreteFiniteDistribution;
import bn.distributions.Distribution;
import bn.distributions.DiscreteDistribution.DiscreteSufficientStatistic;
import bn.distributions.Distribution.SufficientStatistic;
import bn.dynamic.IFDiscDBNNode;
import bn.impl.nodengines.FiniteDiscreteNode;
import bn.messages.FiniteDiscreteMessage;
import bn.messages.FiniteDiscreteMessage.FDiscMessageInterfaceSet;
import bn.messages.Message.MessageInterfaceSet;

import bn.impl.dynbn.DynamicContextManager.DynamicChildManager;
import bn.impl.dynbn.DynamicContextManager.DynamicMessageIndex;
import bn.impl.dynbn.DynamicContextManager.DynamicMessageSet;
import bn.impl.dynbn.DynamicContextManager.DynamicParentManager;

public class FDiscDBNNode extends DBNNode implements IFDiscDBNNode, Optimizable {
	
	public FDiscDBNNode(DynamicBayesianNetwork net, String name, int cardinality) throws BNException
	{
		super(net,name);
		this.cardinality = cardinality;
		
		this.localLambda = new ArrayList<FiniteDiscreteMessage>(net.T);
		this.localPi = new ArrayList<FiniteDiscreteMessage>(net.T);
		this.marginal = new ArrayList<FiniteDiscreteMessage>(net.T);
		for(int i = 0; i < net.T; i++)
		{
			this.localLambda.add(FiniteDiscreteMessage.normalMessage(cardinality));
			this.localPi.add(FiniteDiscreteMessage.normalMessage(cardinality));
			this.marginal.add(FiniteDiscreteMessage.normalMessage(cardinality));
		}
		
		this.childrenMessages = new DynamicChildManager<FiniteDiscreteMessage>(net.T);
		this.parentMessages  =  new DynamicParentManager<FiniteDiscreteMessage>(net.T);
	}
	
	protected FDiscMessageInterfaceSet newChildInterface(int l)
	{
		FDiscMessageInterfaceSet ret = new FDiscMessageInterfaceSet(l);
		for(int i = 0; i < l; i++)
		{
			ret.lambda_v.add(FiniteDiscreteMessage.normalMessage(this.cardinality));
			ret.pi_v.add(FiniteDiscreteMessage.normalMessage(this.cardinality));
		}
		return ret;
	}

	protected DynamicMessageIndex addInterChildInterface(MessageInterfaceSet<?>  mia) throws BNException
	{
		if(!(mia instanceof FDiscMessageInterfaceSet))
			throw new BNException("Attempted to add a child interface that is not a finite discrete message set.");
		FDiscMessageInterfaceSet set = (FDiscMessageInterfaceSet)mia;
		return this.childrenMessages.newInterChild(set.pi_v,set.lambda_v);
	}
	protected DynamicMessageIndex addIntraChildInterface(MessageInterfaceSet<?>  mia) throws BNException
	{
		if(!(mia instanceof FDiscMessageInterfaceSet))
			throw new BNException("Attempted to add a child interface that is not a finite discrete message set.");
		FDiscMessageInterfaceSet set = (FDiscMessageInterfaceSet)mia;
		return this.childrenMessages.newIntraChild(set.pi_v,set.lambda_v);
	}
	protected DynamicMessageIndex addIntraParentInterface(MessageInterfaceSet<?>  mia) throws BNException
	{
		if(!(mia instanceof FDiscMessageInterfaceSet))
			throw new BNException("Attempted to add a child interface that is not a finite discrete message set.");
		FDiscMessageInterfaceSet set = (FDiscMessageInterfaceSet)mia;
		return this.parentMessages.newIntraParent(set.pi_v,set.lambda_v);
	}
	protected DynamicMessageIndex addInterParentInterface(MessageInterfaceSet<?>  mia) throws BNException
	{
		if(!(mia instanceof FDiscMessageInterfaceSet))
			throw new BNException("Attempted to add a child interface that is not a finite discrete message set.");
		FDiscMessageInterfaceSet set = (FDiscMessageInterfaceSet)mia;
		return this.parentMessages.newInterParent(set.pi_v,set.lambda_v);
	}
	
	protected void removeInterChildInterface(DynamicMessageIndex index)
	{
		this.childrenMessages.removeInterChild(index);
	}
	protected void removeIntraChildInterface(DynamicMessageIndex index)
	{
		this.childrenMessages.removeIntraChild(index);
	}
	protected void removeInterParentInterface(DynamicMessageIndex index)
	{
		this.parentMessages.removeInterParent(index);
	}
	protected void removeIntraParentInterface(DynamicMessageIndex index)
	{
		this.parentMessages.removeIntraParent(index);
	}
	
	@Override
	public void validate() throws BNException
	{
		DynamicMessageSet<FiniteDiscreteMessage> msgSet;
		int[] dimensions;
		if(this.initialDist!=null)
		{
			msgSet = this.parentMessages.getIncomingPis(0);
			dimensions = new int[msgSet.size()];
			for(int i = 0; i < msgSet.size(); i++)
				dimensions[i] = msgSet.get(i).getCardinality();
			this.initialDist.validateDimensionality(dimensions, cardinality);
		}
		msgSet = this.parentMessages.getIncomingPis(1);
		dimensions = new int[msgSet.size()];
		for(int i = 0; i < msgSet.size(); i++)
			dimensions[i] = msgSet.get(i).getCardinality();
		try {
			this.advanceDist.validateDimensionality(dimensions, cardinality);
		} catch(BNException e) {
			throw new BNException("Error validating node : " + this.name + " : " + e.toString(),e);
		}
	}
	
	@Override
	public void resetMessages()
	{
		this.parentMessages.resetMessages();
		this.childrenMessages.resetMessages();
	}
	
	@Override
	public double updateMessages(int t) throws BNException
	{
		return FiniteDiscreteNode.updateMessages((t==0 && this.initialDist!=null) ? this.initialDist : this.advanceDist, 
				this.localLambda.get(t), this.localPi.get(t), this.marginal.get(t),
				this.parentMessages.getIncomingPis(t),this.childrenMessages.getOutgoingPis(t),
				this.childrenMessages.getIncomingLambdas(t),this.parentMessages.getOutgoingLambdas(t),
				this.values==null ? null : this.values[t],this.cardinality);
	}
	

	public double betheFreeEnergy() throws BNException
	{
		double sum = 0;
		for(int i = 0; i < this.bayesNet.T; i++)
			sum += this.betheFreeEnergy(i);
		return sum;
	}
	
	private double betheFreeEnergy(int i) throws BNException
	{
		DiscreteFiniteDistribution dist = (i==0 && this.initialDist!=null) ? this.initialDist : this.advanceDist;
		return dist.computeBethePotential(this.parentMessages.getIncomingPis(i), localLambda.get(i), 
				this.marginal.get(i), values==null ? null : values[i], this.childrenMessages.getIncomingLambdas(i).size()); 
	}
	
	public void clearEvidence()
	{
		this.values = null;
	}
	
	public void setEvidence(int t, int value)
	{
		if(this.values==null)
			this.values = new Integer[this.bayesNet.T];
		this.values[t] = value;
	}   

	public DiscreteDistribution getAdvanceDistribution() {
		return this.advanceDist;
	}
	public DiscreteDistribution getInitialDistribution() {
		return this.initialDist;
	}
	public void setInitialDistribution(DiscreteFiniteDistribution dist)
	throws BNException {
		this.initialDist = dist;
	}
	public void setAdvanceDistribution(DiscreteFiniteDistribution dist)
	throws BNException {
		this.advanceDist = dist.copy();
	}
	public void setInitialDistribution(Distribution dist)
	throws BNException {
		if(dist instanceof DiscreteFiniteDistribution)
			this.initialDist = (DiscreteFiniteDistribution)dist.copy();
	}
	public void setAdvanceDistribution(Distribution dist)
	throws BNException {
		if(dist instanceof DiscreteFiniteDistribution)
			this.advanceDist = (DiscreteFiniteDistribution)dist.copy();
	}
	
	public void setValue(int t, int value) throws BNException {
		if(t < 0 || t >= this.bayesNet.T)
			throw new BNException("Attempted to set finite discrete node with value outside of its range.");
		if(this.values==null)
			this.values = new Integer[this.bayesNet.T];
		this.values[t] = value;
	}
	public void setValue(int[] values, int t0) throws BNException {
		if(t0 < 0 || t0+values.length > this.bayesNet.T)
			throw new BNException("Attempted to set finite discrete node with value outside of its range.");
		if(this.values==null)
			this.values = new Integer[this.bayesNet.T];
		for(int i = 0; i < values.length; i++)
			this.setValue(t0+i,values[i]);
	}
	public Integer getValue(int t) throws BNException {
		if(t < 0 || t >= this.bayesNet.T)
			throw new BNException("Attempted to get value from finite discrete with value outside of its range.");
		if(this.values==null)
			throw new BNException("Attempted to get value from finite discrete node where no values are set.");
		return this.values[t];
	}
	public int getCardinality() {
		return this.cardinality;
	}
	public FiniteDiscreteMessage getMarginal(int t) throws BNException
	{
		return this.marginal.get(t);
	}
	
	public String getNodeDefinition()
	{
		String ret = this.getName()+":Discrete("+this.cardinality+")\n";
		boolean inSequence = false;
		if(this.values!=null)
		{
			for(int i = 0; i < this.bayesNet.T; i++)
			{
				if(this.values[i]!=null)
				{
					if(inSequence)
						ret += " " + this.values[i];
					else
					{
						ret += this.getName() +"("+i+") = " + this.values[i];
						inSequence = true;
					}
				}
				else if(inSequence)
				{
					inSequence = false;
					ret += "\n";
				}
			}
		}
		if(inSequence)
			ret+="\n";
		if(this.initialDist!=null)
		{
			ret += this.getName()+"__CPT__INITIAL < " +this.initialDist.getDefinition()
			+  this.getName()+"~~"+this.getName()+"__CPT__INITIAL\n";
		}
		ret += this.getName()+"__CPT < " + this.advanceDist.getDefinition();
		ret += this.getName() + "~" + this.getName()+"__CPT\n\n";
		return ret;
	}
	
	public void printDistributionInfo(PrintStream ps)
	{
		if(this.initialDist!=null)
		{
			ps.println("Initial distribution for node " + this.getName() + " : ");
			this.initialDist.printDistribution(ps);
		}
		ps.println("Advance distribution for node " + this.getName() + " : ");
		this.advanceDist.printDistribution(ps);
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
	
	public TwoSliceStatistics<DiscreteSufficientStatistic> getSufficientStatistic() throws BNException
	{
		TwoSliceStatistics<DiscreteSufficientStatistic> tss = new TwoSliceStatistics<DiscreteSufficientStatistic>();
		if(this.initialDist!=null)
			tss.initialStat = this.initialDist.getSufficientStatisticObj();
		tss.advanceStat = this.advanceDist.getSufficientStatisticObj();
		this.updateSufficientStatistic(tss);
		return tss;
	}

	public void updateSufficientStatistic(TwoSliceStatistics<DiscreteSufficientStatistic> tss) throws BNException
	{
		if(this.advanceDist==null)
			throw new BNException("Node not initialized - no advance distribution, so cannot extract sufficient statistics.");
		if(this.initialDist!=null)
		{
			if(this.values!=null && this.values[0]!=null)
				tss.initialStat.update(this.values[0], this.parentMessages.getIncomingPis(0));
			else
				tss.initialStat.update(this.localLambda.get(0), this.parentMessages.getIncomingPis(0));
		}
		else if(this.values!=null && this.values[0]!=null)
			tss.advanceStat.update(values[0],this.parentMessages.getIncomingPis(0));
		else
			tss.advanceStat.update(this.localLambda.get(0),this.parentMessages.getIncomingPis(0));

		if(values!=null)
		{
			for(int t = 1; t < this.bayesNet.T; t++)
			{
				if(this.values[t]!=null)
					tss.advanceStat.update(values[t],this.parentMessages.getIncomingPis(t));
				else
					tss.advanceStat.update(this.localLambda.get(t),this.parentMessages.getIncomingPis(t));
			}
		}
		else
		{
			for(int t = 1; t < this.bayesNet.T; t++)
				tss.advanceStat.update(this.localLambda.get(t),this.parentMessages.getIncomingPis(t));
		}
	}
	
	public double optimizeParameters() throws BNException
	{
		TwoSliceStatistics<DiscreteSufficientStatistic> tss = this.getSufficientStatistic();
		if(this.initialDist!=null)
		{
			double err = 0;
			err = this.initialDist.optimize(tss.initialStat);
			return Math.max(err, this.advanceDist.optimize(tss.advanceStat));
		}
		else
			return this.advanceDist.optimize(tss.advanceStat);
	}
	public double optimizeParameters(TwoSliceStatistics<DiscreteSufficientStatistic> tss) throws BNException
	{
		if((this.initialDist!=null && tss.initialStat==null) || tss.advanceStat==null)
			throw new BNException("Attempted to optimize node without providing proper amount of statistics.");
		if(this.initialDist!=null)
		{
			double err = this.initialDist.optimize(tss.initialStat);
			return Math.max(err,this.advanceDist.optimize(tss.advanceStat));
		}
		else
			return this.advanceDist.optimize(tss.advanceStat);
	}
	public double optimizeParameters(SufficientStatistic stat) throws BNException
	{
		if(!(stat instanceof TwoSliceStatistics<?>))
			throw new BNException("Attempted to optimize dynamic node with static statistic.");
		TwoSliceStatistics<?> tss = (TwoSliceStatistics<?>)stat;
		if(this.initialDist!=null)
		{
			double err = this.initialDist.optimize(tss.initialStat);
			return Math.max(err,this.advanceDist.optimize(tss.advanceStat));
		}
		else
			return this.advanceDist.optimize(tss.advanceStat);
	}
	
	@Override
	public double conditionalLL(int t)
	{
		if(this.values==null || this.values[t]==null)
			return 0.0;
		return -Math.log(this.localPi.get(t).getValue(this.values[t]));
	}
	
	DiscreteFiniteDistribution initialDist = null, advanceDist = null;

	ArrayList<FiniteDiscreteMessage> localLambda, localPi, marginal;
	Integer[] values = null;
	DynamicContextManager.DynamicChildManager<FiniteDiscreteMessage> childrenMessages;
	DynamicContextManager.DynamicParentManager<FiniteDiscreteMessage> parentMessages;

	int cardinality;
}
