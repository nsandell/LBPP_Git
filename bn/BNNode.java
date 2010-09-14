package bn;

import java.util.ArrayList;
import java.util.Iterator;

import bn.BayesNet.BNException;
import bn.nodeInterfaces.BNNodeI;

/**
 * Bayes node root class.  Simply has parents and children so we can
 *  to a BFS or DFS to detect cycles, etc.
 * @author Nils F Sandell
 */
public abstract class BNNode implements BNNodeI
{
	protected BNNode(){}

	protected final void addChildI(BNNodeI child)
	{
		this.children.add(child);
	}
	
	protected final void addParentI(BNNodeI parent)
	{
		this.parents.add(parent);
	}
	
	public final Iterator<BNNodeI> getChildren()
	{
		return this.children.iterator();
	}
	
	public final Iterator<BNNodeI> getParents()
	{
		return this.parents.iterator();
	}
	
	public abstract void validate() throws BNException;
	
	protected ArrayList<BNNodeI> children = new ArrayList<BNNodeI>();
	protected ArrayList<BNNodeI> parents = new ArrayList<BNNodeI>();
}
