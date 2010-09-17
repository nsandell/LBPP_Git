package bn.interfaces;

import bn.BNException;
import bn.distributions.DiscreteDistribution;
import bn.messages.DiscreteMessage;

public interface IDiscreteBayesNode extends IBayesNode
{
	void setDistribution(DiscreteDistribution dist) throws BNException;
	void setValue(int o) throws BNException;
	DiscreteMessage getMarginal();
}
