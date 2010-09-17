package bn.interfaces;

public interface BNNodeI
{
	BNNodeISet getChildren();
	BNNodeISet getParents();
	
	static interface BNNodeISet extends Iterable<BNNodeI>{};
}
