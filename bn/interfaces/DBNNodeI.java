package bn.interfaces;

public interface DBNNodeI<InnerND extends BNNodeI>
{
	DBNNodeISet getInterChildren();
	DBNNodeISet getInterParents();
	DBNNodeISet getIntraChildren();
	DBNNodeISet getIntraParents();
	
	InnerND getInstance(int t);
	
	static interface DBNNodeISet extends Iterable<DBNNodeI<?>>{};
}
