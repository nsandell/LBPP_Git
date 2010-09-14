package bn.nodeInterfaces;

import java.util.Iterator;

import bn.BayesNet.BNException;

public interface BNNodeI
{
	Iterator<BNNodeI> getChildren();
	Iterator<BNNodeI> getParents();
	void sendInitialMessages();
	double updateMessages() throws BNException;
	
	void validate() throws BNException;
}
