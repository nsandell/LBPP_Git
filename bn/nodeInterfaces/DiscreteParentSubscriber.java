package bn.nodeInterfaces;

import bn.messages.DiscreteMessage;

public interface DiscreteParentSubscriber extends BNNodeI
{
	void handlePi(BNNodeI parent, DiscreteMessage dm);
}
