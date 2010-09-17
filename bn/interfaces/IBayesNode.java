package bn.interfaces;

import bn.BNException;

public interface IBayesNode
{
	String getName();
	Iterable<IBayesNode> getChildren();
	Iterable<IBayesNode> getParents();
	void validate() throws BNException;
	
	void sendInitialMessages() throws BNException;
	double updateMessages() throws BNException;
}
