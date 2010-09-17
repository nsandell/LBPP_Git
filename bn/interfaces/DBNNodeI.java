package bn.interfaces;

import bn.BNException;

public interface DBNNodeI
{
	Iterable<DBNNodeI> getInterChildren();
	Iterable<DBNNodeI> getIntraChildren();
	Iterable<DBNNodeI> getInterParents();
	Iterable<DBNNodeI> getIntraParents();
	
	void validate() throws BNException;
	
	BNNodeI getInstance(int t);
}
