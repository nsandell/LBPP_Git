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
	
	public final void addChild(BNNodeI child) throws BNException
	{
		this.addChildI(child);
		this.children.add(child);
	}

	public final void removeChild(BNNodeI child) throws BNException
	{
		this.removeChildI(child);
		this.children.remove(child);
	}
	
	public final void addParent(BNNodeI parent) throws BNException
	{
		this.addParentI(parent);
		this.parents.add(parent);
	}
	
	public final void removeParent(BNNodeI parent) throws BNException
	{
		this.removeParentI(parent);
		this.parents.remove(parent);
	}
	
	protected abstract void addChildI(BNNodeI child) throws BNException;
	protected abstract void removeChildI(BNNodeI child) throws BNException;
	protected abstract void addParentI(BNNodeI parent) throws BNException;
	protected abstract void removeParentI(BNNodeI parent) throws BNException;
	
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
