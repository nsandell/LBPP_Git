package bn.impl.staticbn;

import java.io.PrintStream;

import bn.BNException;
import bn.Optimizable;
import bn.distributions.DiscreteDistribution;
import bn.distributions.Distribution;
import bn.distributions.Distribution.SufficientStatistic;
import bn.impl.staticbn.StaticContextManagers.StaticMessageIndex;
import bn.messages.Message.MessageInterface;
import bn.statc.IInfDiscEvBNNode;

public class InfDiscEvBNNode extends BNNode implements IInfDiscEvBNNode, Optimizable
{
	
	public InfDiscEvBNNode(StaticBayesianNetwork net, String name, int value)
	{
		super(net,name);
	}

	@Override
	public void printDistributionInfo(PrintStream ps) throws BNException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void resetMessages() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double betheFreeEnergy() throws BNException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getNodeDefinition() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getEdgeDefinition() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void clearEvidence() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Distribution getDistribution() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setDistribution(Distribution dist) throws BNException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public SufficientStatistic getSufficientStatistic() throws BNException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double optimizeParameters(SufficientStatistic stat)
			throws BNException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double optimizeParameters() throws BNException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Integer getValue() throws BNException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setDistribution(DiscreteDistribution dist) throws BNException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setValue(int o) throws BNException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected MessageInterface<?> newChildInterface() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected StaticMessageIndex addParentInterface(MessageInterface<?> mi)
			throws BNException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected StaticMessageIndex addChildInterface(MessageInterface<?> mi)
			throws BNException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void removeParentInterface(StaticMessageIndex index)
			throws BNException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void removeChildInterface(StaticMessageIndex index)
			throws BNException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double updateMessages() throws BNException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void validate() throws BNException {
		// TODO Auto-generated method stub
		
	}

}
