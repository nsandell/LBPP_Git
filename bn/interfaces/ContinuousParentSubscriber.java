package bn.interfaces;

import bn.IBayesNode;

/**
 * Nodes that can have a scalar-continuous parent should handle
 * this interface 
 * @author Nils F. Sandell
 *
 */
public interface ContinuousParentSubscriber extends IBayesNode 
{
//TODO No continuous nodes/messages yet.
	//void handlePi(IBayesNode parent, ContinuousMessage msg)
}
