package bn.impl.staticbn;

import java.io.PrintStream;


import bn.BNException;
import bn.distributions.Distribution;
import bn.distributions.DiscreteDistribution.DiscreteFiniteDistribution;
import bn.distributions.DiscreteDistribution.DiscreteSufficientStatistic;
import bn.distributions.Distribution.SufficientStatistic;
import bn.impl.nodengines.FiniteDiscreteNode;
import bn.impl.staticbn.StaticContextManagers.StaticMessageIndex;
import bn.messages.FiniteDiscreteMessage;
import bn.messages.MessageSet;
import bn.messages.FiniteDiscreteMessage.FDiscMessageInterface;
import bn.messages.Message.MessageInterface;
import bn.statc.IFDiscBNNode;

class FDiscBNNode extends BNNode implements IFDiscBNNode
{
	public FDiscBNNode(StaticBayesianNetwork bn, String name, int cardinality)
	{
		super(bn,name);
		this.cardinality = cardinality;
		this.localLambda = FiniteDiscreteMessage.normalMessage(cardinality);
		this.localPi = FiniteDiscreteMessage.normalMessage(cardinality);
		this.marginal = FiniteDiscreteMessage.normalMessage(cardinality);
	}
	
	@Override
	protected FDiscMessageInterface newChildInterface()
	{
		return new FDiscMessageInterface(FiniteDiscreteMessage.normalMessage(this.cardinality),FiniteDiscreteMessage.normalMessage(this.cardinality));
	}

	@Override
	public double updateMessages() throws BNException
	{
		return FiniteDiscreteNode.updateMessages(this.cpt, this.localLambda, this.localPi,this.marginal,
				this.parentInterface.getIncomingPis(),this.childrenInterface.getOutgoingPis(),
				this.childrenInterface.getIncomingLambdas(),this.parentInterface.getOutgoingLambdas(),
				this.value,this.cardinality);
	}
	
	@Override
	public void validate() throws BNException
	{
		MessageSet<FiniteDiscreteMessage> dist = this.parentInterface.getIncomingPis();
		int[] dimensions = new int[dist.size()];
		for(int i = 0; i < dist.size(); i++)
			dimensions[i] = dist.get(i).getCardinality();
		this.cpt.validateDimensionality(dimensions, cardinality);
	}
	
	@Override
	protected StaticMessageIndex addParentInterface(MessageInterface<?> mi)
			throws BNException {
		if(!(mi instanceof FDiscMessageInterface))
			throw new BNException("Attempted to add invalid interface...");
		else
			return this.parentInterface.newParent((FiniteDiscreteMessage)mi.pi,(FiniteDiscreteMessage)mi.lambda);
	}

	@Override
	protected StaticMessageIndex addChildInterface(MessageInterface<?> mi) 
			throws BNException {
		if(!(mi.lambda instanceof FiniteDiscreteMessage) || !(mi.pi instanceof FiniteDiscreteMessage))
			throw new BNException("Attempted to add invalid interface...");
		else
			return this.childrenInterface.newChild((FiniteDiscreteMessage)mi.pi,(FiniteDiscreteMessage)mi.lambda);
	}

	@Override
	protected void removeParentInterface(StaticMessageIndex index) throws BNException {
		this.parentInterface.removeParent(index);
	}

	@Override
	protected void removeChildInterface(StaticMessageIndex index) throws BNException {
		this.childrenInterface.removeChild(index);
	}
	
	public Integer getValue()
	{
		return this.value;
	}
	
	public int getCardinality()
	{
		return this.cardinality;
	}
	
	public FiniteDiscreteMessage getMarginal() throws BNException
	{
		FiniteDiscreteMessage marg = this.localLambda.multiply(this.localPi);
		marg.normalize();
		return marg;
	}
	
	public void setValue(int value) throws BNException
	{
		if(value >= this.cardinality)
			throw new BNException("Attempted to set value of finite node beyond range, " + value + " >= " + this.cardinality);
		this.value = value;
	}
	
	public void clearValue()
	{
		this.value = null;
	}
	
	public void setDistribution(DiscreteFiniteDistribution dist) throws BNException
	{
		this.cpt = dist.copy();
	}
	
	@Override
	public final void setSample(boolean sample)
	{
		this.sample = sample;
	}
	boolean sample = true;
	
	@Override
	public final void sample()
	{
		if(this.sample)
			this.sampleI();
	}
	
	protected void sampleI()
	{
		//TODO Uh, how do I get parent values...
	}
	
	public void setDistribution(Distribution dist) throws BNException
	{
		if(dist instanceof DiscreteFiniteDistribution)
			this.cpt = (DiscreteFiniteDistribution)dist.copy();
		else 
			throw new BNException("Attempted to set discrete finite node's CPT to a non discrete distribution.");
	}
	
	public DiscreteFiniteDistribution getDistribution()
	{
		return this.cpt;
	}
	
	public DiscreteSufficientStatistic getSufficientStatistic() throws BNException
	{
		return this.cpt.getSufficientStatisticObj().update(this.localLambda, this.parentInterface.getIncomingPis());
	}
	public void updateSufficientStatistic(SufficientStatistic stat) throws BNException
	{
		stat.update(this.getSufficientStatistic());
	}
	protected double optimizeParametersI(SufficientStatistic stat) throws BNException
	{
		return this.cpt.optimize(stat);
	}

	
    @Override
	public void clearEvidence()
	{
		this.value = null;
	}
	
    @Override
	protected double optimizeParametersI() throws BNException
	{
		return this.optimizeParameters(this.getSufficientStatistic());
	}
	
    @Override
	public void resetMessages()
	{
		this.parentInterface.resetMessages();
		this.childrenInterface.resetMessages();
	}
	
    @Override
	public double betheFreeEnergy() throws BNException
	{
		return this.cpt.computeBethePotential(this.parentInterface.getIncomingPis(),
				this.localLambda, this.marginal ,this.value,this.numChildren());
	}
	
    @Override
    public String getNodeDefinition()
    {
            String ret =  this.getName() + ":Discrete(" + this.cardinality + ")\n";
            ret += this.getName() + "__CPT < " + this.cpt.getDefinition();
            ret += this.getName() + "~" + this.getName() + "__CPT\n";
            if(this.value!=null)
                    ret += this.getName() + "=" + this.value + "\n";
            ret+="\n";
            return ret;
    }

    public double conditionalLL()
    {
    	if(this.value==null)
    		return 0;
    	return Math.log(this.localPi.getValue(this.value));
    }
   
    @Override
    public void printDistributionInfo(PrintStream ps)
    {
    	this.cpt.printDistribution(ps);
    }
    
	private int cardinality;
	private DiscreteFiniteDistribution cpt;
	
	Integer value;
	FiniteDiscreteMessage localLambda;
	FiniteDiscreteMessage localPi;
	FiniteDiscreteMessage marginal;
	
	private StaticContextManagers.StaticChildManager<FiniteDiscreteMessage> childrenInterface = new StaticContextManagers.StaticChildManager<FiniteDiscreteMessage>();
	private StaticContextManagers.StaticParentManager<FiniteDiscreteMessage> parentInterface = new StaticContextManagers.StaticParentManager<FiniteDiscreteMessage>();

}
