package bn.interfaces;

public interface IDynBayesNode extends IBayesNode
{
	Iterable<IDynBayesNode> getInterChildren();
	Iterable<IDynBayesNode> getIntraChildren();
	Iterable<IDynBayesNode> getInterParents();
	Iterable<IDynBayesNode> getIntraParents();
	
	IBayesNode getInstance(int t);
}
