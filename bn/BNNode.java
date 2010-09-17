package bn;

import java.util.ArrayList;

import java.util.Iterator;

import bn.BNException;
import bn.interfaces.BNNodeI;

/**
 * Bayes node root class.  Simply has parents and children so we can
 *  to a BFS or DFS to detect cycles, etc.
 * @author Nils F Sandell
 */
abstract class BNNode implements BNNodeI
{
	protected BNNode(BayesNet net)
	{
		this.bnet = net;
		this.parentSet = new BNNodeSet(parents);
		this.childrenSet = new BNNodeSet(children);
	}
	
	public final void addChild(BNNode child) throws BNException
	{
		this.addChildI(child);
		this.children.add(child);
	}

	public final void removeChild(BNNode child) throws BNException
	{
		this.removeChildI(child);
		this.children.remove(child);
	}
	
	public final void addParent(BNNode parent) throws BNException
	{
		this.addParentI(parent);
		this.parents.add(parent);
	}
	
	public final void removeParent(BNNode parent) throws BNException
	{
		this.removeParentI(parent);
		this.parents.remove(parent);
	}
	
	public abstract void sendInitialMessages() throws BNException;
	public abstract double updateMessages() throws BNException;
	public abstract void validate() throws BNException;
	
	protected abstract void addChildI(BNNode child) throws BNException;
	protected abstract void removeChildI(BNNode child) throws BNException;
	protected abstract void addParentI(BNNode parent) throws BNException;
	protected abstract void removeParentI(BNNode parent) throws BNException;
	
	public final BNNodeISet getChildren()
	{
		return this.childrenSet;
	}
	
	public final BNNodeISet getParents()
	{
		return this.parentSet;
	}
	
	protected BayesNet bnet;
	
	private BNNodeSet childrenSet;
	private BNNodeSet parentSet;
	
	private ArrayList<BNNode> children = new ArrayList<BNNode>();
	private ArrayList<BNNode> parents = new ArrayList<BNNode>();
	
	static class BNNodeSet implements BNNodeI.BNNodeISet
	{
		static class IteratorWrapper implements Iterator<BNNodeI>
		{
			public IteratorWrapper(Iterator<BNNode> it){this.it = it;}
			public boolean hasNext() {return it.hasNext();}
			public BNNodeI next() {return it.next();}
			public void remove(){}
			private Iterator<BNNode> it;
		}
		
		public BNNodeSet(ArrayList<BNNode> nodes){this.nodes = nodes;}
		
		public Iterator<BNNodeI> iterator() { return new IteratorWrapper(nodes.iterator());}

		ArrayList<BNNode> nodes;
	}
}
