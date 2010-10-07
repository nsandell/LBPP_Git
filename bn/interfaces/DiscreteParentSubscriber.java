package bn.interfaces;

import bn.IBayesNode;
import bn.messages.DiscreteMessage;

/**
 * This interface must be implemented by any node type that can
 * have a discrete parent, as they must be able to handle discrete
 * pi messages.
 * @author Nils F. Sandell
 */
public interface DiscreteParentSubscriber extends IBayesNode
{
	/**
	 * Handle a discrete message from your parent
	 * @param parent Parent node
	 * @param dm Discrete Message
	 */
	void handlePi(IBayesNode parent, DiscreteMessage dm);
}
