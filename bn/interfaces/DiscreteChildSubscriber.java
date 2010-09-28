package bn.interfaces;

import bn.IBayesNode;
import bn.messages.DiscreteMessage;

public interface DiscreteChildSubscriber extends IBayesNode
{
	void handleLambda(IBayesNode child, DiscreteMessage dm); // note every iteration there will be messages thrown to GC
}
