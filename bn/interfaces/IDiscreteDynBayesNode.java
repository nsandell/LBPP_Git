package bn.interfaces;

import bn.BNException;
import bn.distributions.DiscreteDistribution;
import bn.messages.DiscreteMessage;

public interface IDiscreteDynBayesNode extends IDynBayesNode {
	public void setInitialDistribution(DiscreteDistribution dist) throws BNException;
	public void setAdvanceDistribution(DiscreteDistribution dist) throws BNException;
	public DiscreteMessage getMarginal(int t) throws BNException;
	public void setValue(int t, int value) throws BNException;
	public void setValue(int[] values,int t0) throws BNException;
	
	public IDiscreteBayesNode getDiscreteInstance(int t);
}
