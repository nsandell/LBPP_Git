package bn.impl.dynbn;

import java.io.PrintStream;

import bn.BNException;
import bn.Optimizable;
import bn.distributions.DiscreteDistribution.InfDiscDistSufficientStat;
import bn.distributions.Distribution;
import bn.distributions.DiscreteDistribution.InfiniteDiscreteDistribution;
import bn.distributions.Distribution.SufficientStatistic;
import bn.dynamic.IInfDiscEvDBNNode;
import bn.impl.dynbn.DynamicContextManager.DynamicMessageIndex;
import bn.impl.dynbn.FDiscDBNNode.TwoSliceStatistics;
import bn.messages.FiniteDiscreteMessage;
import bn.messages.FiniteDiscreteMessage.FDiscMessageInterfaceSet;
import bn.messages.MessageSet;
import bn.messages.Message.MessageInterfaceSet;

public class InfDiscEvDBNNode extends DBNNode implements IInfDiscEvDBNNode, Optimizable {
	
	public InfDiscEvDBNNode(DynamicBayesianNetwork net, String name, int[] values) throws BNException
	{
		super(net,name);
		if(values.length!=net.T)
			throw new BNException("Failed to create discrete evidence node -- provided values aren't of sufficient length.");
		this.parentMessages = new DynamicContextManager.DynamicParentManager<FiniteDiscreteMessage>(net.T);
		this.values = values;
	}

	@Override
	public double betheFreeEnergy() throws BNException {
		double bfe = 0;
		if(init!=null)
			bfe += this.init.computeBethePotential(this.parentMessages.getIncomingPis(0), this.values[0]);
		else
			bfe += this.advance.computeBethePotential(this.parentMessages.getIncomingPis(0), this.values[0]);
		for(int t = 1; t < this.bayesNet.T; t++)
			bfe += this.advance.computeBethePotential(this.parentMessages.getIncomingPis(0), this.values[0]);
		return bfe;
	}

	@Override
	public String getNodeDefinition() {
		return ""; //TODO determine the way evidence nodes are defined... evidence in parens, after entered?
	}

	@Override
	public void clearEvidence(){}

	@Override
	public void setInitialDistribution(Distribution dist) throws BNException {
		if(dist instanceof InfiniteDiscreteDistribution)
			this.init = (InfiniteDiscreteDistribution)dist;
		else
			throw new BNException("Expected infinite discrete distribution for evidence node.");
	}

	@Override
	public void setAdvanceDistribution(Distribution dist) throws BNException {
		if(dist instanceof InfiniteDiscreteDistribution)
			this.advance = (InfiniteDiscreteDistribution)dist;
		else
			throw new BNException("Expected infinite discrete distribution for evidence node.");
	}

	@Override
	public InfiniteDiscreteDistribution getAdvanceDistribution() {
		return this.advance;
	}

	@Override
	public InfiniteDiscreteDistribution getInitialDistribution() {
		return this.init;
	}

	@Override
	protected MessageInterfaceSet<?> newChildInterface(int T) throws BNException
	{throw new BNException("Discrete evidence nodes cannot have children!");}

	@Override
	protected DynamicMessageIndex addInterParentInterface(
			MessageInterfaceSet<?> mia) throws BNException {
		if(mia instanceof FDiscMessageInterfaceSet)
		{
			FDiscMessageInterfaceSet miad = (FDiscMessageInterfaceSet)mia;
			return this.parentMessages.newInterParent(miad.pi_v, miad.lambda_v);
		}
		else throw new BNException("Discrete evidence nodes only support finite discrete parents.");
	}

	@Override
	protected DynamicMessageIndex addIntraParentInterface(
			MessageInterfaceSet<?> mia) throws BNException {
		if(mia instanceof FDiscMessageInterfaceSet)
		{
			FDiscMessageInterfaceSet miad = (FDiscMessageInterfaceSet)mia;
			return this.parentMessages.newIntraParent(miad.pi_v, miad.lambda_v);
		}
		else throw new BNException("Discrete evidence nodes only support finite discrete parents.");
	}

	@Override
	protected DynamicMessageIndex addInterChildInterface(
			MessageInterfaceSet<?> mia) throws BNException {
		throw new BNException("Discrete evidence nodes do not support children.");
	}

	@Override
	protected DynamicMessageIndex addIntraChildInterface(
			MessageInterfaceSet<?> mia) throws BNException {
		throw new BNException("Discrete evidence nodes do not support children.");
	}

	@Override
	protected void removeInterParentInterface(DynamicMessageIndex index)
			throws BNException {
		this.parentMessages.removeInterParent(index);
	}

	@Override
	protected void removeIntraParentInterface(DynamicMessageIndex index) 
			throws BNException {
		this.parentMessages.removeIntraParent(index);
	}

	@Override
	protected void removeInterChildInterface(DynamicMessageIndex index)
			throws BNException {}

	@Override
	protected void removeIntraChildInterface(DynamicMessageIndex index)
			throws BNException {}

	@Override
	public void validate() throws BNException {
		if(this.advance==null)
			throw new BNException("No distribution set for infinite discrete evidence node!");
		
		MessageSet<FiniteDiscreteMessage> pis = this.parentMessages.getIncomingPis(0);
		int[] initdims = new int[pis.size()];
		for(int i = 0; i < initdims.length; i++)
			initdims[i] = pis.get(i).getCardinality();
		if(this.init!=null)
			this.init.validateConditionDimensions(initdims);
		else
			this.advance.validateConditionDimensions(initdims);
		
		pis = this.parentMessages.getIncomingPis(1);
		int[] advancedims = new int[pis.size()];
		for(int i = 0; i < advancedims.length; i++)
			advancedims[i] = pis.get(i).getCardinality();
		this.advance.validateConditionDimensions(advancedims);
	}

	@Override
	protected double updateMessages(int t) throws BNException {
		
		for(FiniteDiscreteMessage msg : this.parentMessages.getOutgoingLambdas(t))
			msg.empty();
		
		if(t==0 && init!=null)
			this.init.computeLambdas(this.parentMessages.getOutgoingLambdas(t), this.parentMessages.getIncomingPis(t), this.values[t]);
		else
			this.advance.computeLambdas(this.parentMessages.getOutgoingLambdas(t), this.parentMessages.getIncomingPis(t), this.values[t]);
		return 0;
	}

	@Override
	public void resetMessages() {
		this.parentMessages.resetMessages();
	}
	
	@Override
	public void printDistributionInfo(PrintStream ps)
	{
		if(this.init!=null)
		{
			ps.println("Initial distribution for node " + this.getName() + " : ");
			this.init.printDistribution(ps);
		}
		ps.println("Advance distribution for node " + this.getName() + " : ");
		this.advance.printDistribution(ps);
	}
	
	@Override
	public int getValue(int t) throws BNException {
		try { 
			return this.values[t];
		} catch(ArrayIndexOutOfBoundsException e) {
			throw new BNException("Requested out of range value at t="+t);
		}
	}

	@Override
	public void setAdvanceDistribution(InfiniteDiscreteDistribution dist)
			throws BNException {
		this.advance = dist;
		
	}

	@Override
	public void setInitialDistribution(InfiniteDiscreteDistribution dist)
			throws BNException {
		this.init = dist;
	}

	@Override
	public void setValue(int t, int o) throws BNException {
		try { 
			this.values[t] = o;
		} catch(ArrayIndexOutOfBoundsException e) {
			throw new BNException("Requested out of range value at t="+t);
		}
	}

	@Override
	public void setValues(int t, int[] o) throws BNException {
		if(t < 0 || t+o.length>this.bayesNet.T)
			throw new BNException("Attempted to set values out of range, spanning ("+t+","+t+o.length+")");
		for(int i = 0; i < o.length; i++)
			this.values[t+i] = o[i];
	}

	@Override
	public SufficientStatistic getSufficientStatistic() throws BNException {
		TwoSliceStatistics<InfDiscDistSufficientStat> tss = new TwoSliceStatistics<InfDiscDistSufficientStat>();
		if(this.init!=null)
			tss.initialStat = init.getSufficientStatisticObj();
		tss.advanceStat = advance.getSufficientStatisticObj();
		this.updateSufficientStatistic(tss);
		return tss;
	}
	
	public void updateSufficientStatistic(TwoSliceStatistics<InfDiscDistSufficientStat> tss) throws BNException
	{
		if(this.advance==null)
			throw new BNException("Node not initialized - no advance distribution, so cannot extract sufficient statistics.");
		if(this.init!=null)
			tss.initialStat.update(this.parentMessages.getIncomingPis(0), this.values[0]);
		else
			tss.advanceStat.update(this.parentMessages.getIncomingPis(0), this.values[0]);

		for(int t = 1; t < this.bayesNet.T; t++)
			tss.advanceStat.update(this.parentMessages.getIncomingPis(t),this.values[t]);
	}
	
	@Override
	public double optimizeParameters(SufficientStatistic stat) throws BNException
	{
		if(!(stat instanceof TwoSliceStatistics<?>))
			throw new BNException("Attempted to optimize dynamic node with static statistic.");
		TwoSliceStatistics<?> tss = (TwoSliceStatistics<?>)stat;
		if(this.init!=null)
		{
			double err = this.init.optimize(tss.initialStat);
			return Math.max(err,this.advance.optimize(tss.advanceStat));
		}
		else
			return this.advance.optimize(tss.advanceStat);
	}
	
	@Override 
	public double optimizeParameters() throws BNException
	{
		return this.optimizeParameters(this.getSufficientStatistic());
	}

	private DynamicContextManager.DynamicParentManager<FiniteDiscreteMessage> parentMessages;
	private InfiniteDiscreteDistribution init, advance;
	private int[] values;
}
