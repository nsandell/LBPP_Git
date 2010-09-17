package bn.interfaces;

import bn.messages.DiscreteMessage;

public interface DiscreteParentSubscriber extends IBayesNode
{
	void handlePi(IBayesNode parent, DiscreteMessage dm);
}
