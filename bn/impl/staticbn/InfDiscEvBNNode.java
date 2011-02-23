package bn.impl.staticbn;

import java.io.PrintStream;

import bn.BNException;
import bn.Optimizable;
import bn.distributions.DiscreteDistribution.InfiniteDiscreteDistribution;
import bn.distributions.Distribution;
import bn.distributions.Distribution.SufficientStatistic;
import bn.impl.staticbn.StaticContextManagers.StaticMessageIndex;
import bn.messages.FiniteDiscreteMessage;
import bn.messages.MessageSet;
import bn.messages.FiniteDiscreteMessage.FDiscMessageInterface;
import bn.messages.Message.MessageInterface;
import bn.statc.IInfDiscEvBNNode;

public class InfDiscEvBNNode extends BNNode implements IInfDiscEvBNNode, Optimizable
{
	public InfDiscEvBNNode(StaticBayesianNetwork net, String name, int value)
	{
		super(net,name);
		this.value = value;
	}

	@Override
	public void printDistributionInfo(PrintStream ps) throws BNException {
		this.dist.printDistribution(ps);
	}

	@Override
	public void resetMessages() 
	{
		this.parentMsgs.resetMessages();
	}
	
    public double conditionalLL()
    {
    	double PE = 0;
    	for(FiniteDiscreteMessage pi : this.parentMsgs.getIncomingPis())
    	{
    		for(int i = 0; i < pi.getCardinality(); i++)
    			if(pi.getValue(i) > 0 && pi.getValue(i) < 1)
    				PE -= pi.getValue(i)*Math.log(pi.getValue(i));
    	}
    	double BE = this.dist.computeBethePotential(this.parentMsgs.getIncomingPis(), this.value);
    	return BE+PE;
    }

	@Override
	public double betheFreeEnergy() throws BNException {
		return this.dist.computeBethePotential(this.parentMsgs.getIncomingPis(), this.value);
	}

	@Override
	public String getNodeDefinition() {
		return this.getName()+":InfDiscEv()\n";
	}

	@Override
	public void clearEvidence() {}

	@Override
	public InfiniteDiscreteDistribution getDistribution() {
		return this.dist;
	}

	@Override
	public void setDistribution(Distribution dist) throws BNException {
		if(dist instanceof InfiniteDiscreteDistribution)
			this.dist = (InfiniteDiscreteDistribution)dist;
		else throw new BNException("Exepected infinite distribution for infinite discrete evidence node.");
	}
	
	public void setDistribution(InfiniteDiscreteDistribution dist)
	{
		this.dist = dist;
	}

	@Override
	public SufficientStatistic getSufficientStatistic() throws BNException {
		return dist.getSufficientStatisticObj();
	}

	@Override
	public double optimizeParameters(SufficientStatistic stat)
			throws BNException {
		return dist.optimize(stat);
	}

	@Override
	public double optimizeParameters() throws BNException {
		return dist.optimize(dist.getSufficientStatisticObj());
	}

	@Override
	public int getValue() throws BNException {
		return this.value;
	}

	@Override
	public void setValue(int o) throws BNException {
		this.value = o;
	}

	@Override
	protected MessageInterface<?> newChildInterface() throws BNException {
		throw new BNException("Infinite discrete evidence nodes do not support children.");
	}

	@Override
	protected StaticMessageIndex addParentInterface(MessageInterface<?> mi)
			throws BNException {
		if(mi instanceof FiniteDiscreteMessage.FDiscMessageInterface)
			throw new BNException("Infinite discrete evidence nodes only support finite discrete parents.");
		FDiscMessageInterface dmi = (FDiscMessageInterface)mi;
		return this.parentMsgs.newParent(dmi.pi, dmi.lambda);
	}

	@Override
	protected StaticMessageIndex addChildInterface(MessageInterface<?> mi)
			throws BNException {
		throw new BNException("Infinite discrete evidence nodes do not support children.");
	}

	@Override
	protected void removeParentInterface(StaticMessageIndex index)
			throws BNException {
		this.parentMsgs.removeParent(index);
	}

	@Override
	protected void removeChildInterface(StaticMessageIndex index)
			throws BNException {
		throw new BNException("Infinite discrete evidence nodes do not support children.");
	}

	@Override
	public double updateMessages() throws BNException {
		this.dist.computeLambdas(this.parentMsgs.getOutgoingLambdas(), this.parentMsgs.getIncomingPis(), this.value);
		return 0;
	}

	@Override
	public void validate() throws BNException {
		MessageSet<FiniteDiscreteMessage> pis = this.parentMsgs.getIncomingPis();
		int[] parentDims = new int[pis.size()];
		for(int i = 0; i < parentDims.length; i++)
			parentDims[i] = pis.get(i).getCardinality();
		this.dist.validateConditionDimensions(parentDims);
	}
	
	@Override
	public void updateOutgoingLambda(StaticMessageIndex idx) throws BNException
	{
		dist.computeLambda(this.parentMsgs.getOutgoingLambdas(), idx.getIndex(), this.parentMsgs.getIncomingPis(), this.value);
	}

	@Override
	public void updateOutgoingPi(StaticMessageIndex idx) throws BNException {}
	
	private StaticContextManagers.StaticParentManager<FiniteDiscreteMessage> parentMsgs = new StaticContextManagers.StaticParentManager<FiniteDiscreteMessage>();
	private InfiniteDiscreteDistribution dist;
	private int value;

}
