package bn.interfaces;

import bn.IBayesNode;
import bn.messages.DiscreteMessage;

/**
 * Any node that handles discrete messages from children should 
 * implement this interface (probably just discrete nodes).
 * @author Nils F. Sandell
 */
public interface DiscreteChildSubscriber extends IBayesNode
{
	/**
	 * Handle a discrete lambda message from your child.
	 * @param child Node passing the message
	 * @param dm Message to handle.
	 */ 
	void handleLambda(IBayesNode child, DiscreteMessage dm); 
}
