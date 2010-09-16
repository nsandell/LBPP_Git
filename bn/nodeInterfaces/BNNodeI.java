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
	
	void addChild(BNNodeI child) throws BNException;
	void removeChild(BNNodeI child) throws BNException;
	void addParent(BNNodeI parent) throws BNException;
	void removeParent(BNNodeI parent) throws BNException;
}
