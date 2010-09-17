package bn.interfaces;

import bn.messages.DiscreteMessage;

public interface DiscreteChildSubscriber extends BNNodeI
{
	void handleLambda(BNNodeI child, DiscreteMessage dm); // note every iteration there will be messages thrown to GC
}
