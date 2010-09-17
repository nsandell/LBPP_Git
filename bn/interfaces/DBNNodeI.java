package bn.interfaces;

public interface DBNNodeI
{
	Iterable<DBNNodeI> getInterChildren();
	Iterable<DBNNodeI> getIntraChildren();
	Iterable<DBNNodeI> getInterParents();
	Iterable<DBNNodeI> getIntraParents();
	
	BNNodeI getInstance(int t);
}
