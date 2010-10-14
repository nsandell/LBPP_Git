package bn.impl;

import java.util.ArrayList;

import util.IterableWrapper;

import bn.BNException;
import bn.IBayesNode;

/**
 * Bayes node root class.  Simply has parents and children so we can
 *  to a BFS or DFS to detect cycles, etc.
 * @author Nils F Sandell
 */
abstract class BNNode implements IBayesNode
{
	protected BNNode(StaticBayesianNetwork net, String name) 
	{
		this.bnet = net;
		this.name = name;
		this.parentSet =  new IterableWrapper<IBayesNode>(parents);
		this.childrenSet = new IterableWrapper<IBayesNode>(children);
	}
	
	boolean hasChild(BNNode child)
	{
		return (this.children.contains(child));
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
	
	public final int numParents()
	{
		return this.parents.size();
	}
	
	public final int numChildren()
	{
		return this.children.size();
	}
	
	public final void removeParent(BNNode parent) throws BNException
	{
		this.removeParentI(parent);
		this.parents.remove(parent);
	}
	
	public final String getName()
	{
		return this.name;
	}
	
	protected abstract void addChildI(BNNode child) throws BNException;
	protected abstract void removeChildI(BNNode child) throws BNException;
	protected abstract void addParentI(BNNode parent) throws BNException;
	protected abstract void removeParentI(BNNode parent) throws BNException;
	
	public final Iterable<IBayesNode> getChildren()
	{
		return this.childrenSet;
	}
	
	public final Iterable<IBayesNode> getParents()
	{
		return this.parentSet;
	}
	
	public final Iterable<BNNode> getChildrenI()
	{
		return this.children;
	}
	
	public final Iterable<BNNode> getParentsI()
	{
		return this.parents;
	}
		
	protected StaticBayesianNetwork bnet;
	
	private String name;
	
	private IterableWrapper<IBayesNode> childrenSet;
	private IterableWrapper<IBayesNode> parentSet;
	private ArrayList<BNNode> children = new ArrayList<BNNode>();
	private ArrayList<BNNode> parents = new ArrayList<BNNode>();
}
