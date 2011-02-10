package bn.impl.staticbn;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Vector;

import bn.BNException;
import bn.IBayesNode;
import bn.impl.InternalIBayesNode;
import bn.messages.Message.MessageInterface;
import bn.statc.IBNNode;

/**
 * Bayes node root class.  Simply has parents and children so we can
 *  to a BFS or DFS to detect cycles, etc.
 * @author Nils F Sandell
 */
abstract class BNNode implements InternalIBayesNode, IBNNode
{
	protected BNNode(StaticBayesianNetwork net, String name)
	{
		this.bnet = net;
		this.name = name;
	}
	
	boolean hasChild(BNNode child)
	{
		return this.children.containsKey(child);
	}
	
	public final void addChild(BNNode child) throws BNException
	{
		if(child.bnet!=this.bnet)
			throw new BNException("Attempted to connect nodes from different networks!");
		if(this.children.containsKey(child))
			return;
		try
		{
			MessageInterface<?> mi = this.newChildInterface();
			int pi = this.addChildInterface(mi);
			try
			{
				int ci = child.addParentInterface(mi);
				this.children.put(child, pi);
				child.parents.put(this,  ci);
			} catch(BNException e) {
				this.removeChildInterface(pi);
				throw new BNException(e);
			}
		} catch(BNException e) {
			throw new BNException("Error adding child " + child.getName() + " to parent " + this.getName() + ": " + e.getMessage());
		}
	}

	public final void removeChild(BNNode child) throws BNException
	{
		if(!this.children.containsKey(child))
			return;

		Integer child_idx = this.children.remove(child);
		Integer rent_idx = child.parents.remove(this);

		child.removeParentInterface(rent_idx);
		this.removeChildInterface(child_idx);
		
		for(Entry<BNNode,Integer> entry : children.entrySet())
			if(entry.getValue() > child_idx)
				entry.setValue(entry.getValue()-1);
		
		for(Entry<BNNode,Integer> entry : child.parents.entrySet())
			if(entry.getValue() > rent_idx)
				entry.setValue(entry.getValue()-1);
	}
	
	public final void removeAllChildren() throws BNException
	{
		Vector<BNNode> children = new Vector<BNNode>(this.children.keySet());
		for(BNNode child : children)
		{
			this.removeChild(child);
		}
	}
	
	public final void removeAllParents() throws BNException
	{
		Vector<BNNode> parentCopy = new Vector<BNNode>(this.parents.keySet());
		for(BNNode parent : parentCopy)
			parent.removeChild(this);
	}
	
	protected abstract MessageInterface<?> newChildInterface();

	protected abstract int addParentInterface(MessageInterface<?> mi) throws BNException;
	protected abstract int addChildInterface (MessageInterface<?> mi) throws BNException;
	protected abstract void removeParentInterface(int index) throws BNException;
	protected abstract void removeChildInterface (int index) throws BNException;

	public final int numParents()
	{
		return this.parents.size();
	}
	
	public final int numChildren()
	{
		return this.children.size();
	}
	
	public final String getName()
	{
		return this.name;
	}
	
	public final Iterable<BNNode> getChildrenI()
	{
		return this.children.keySet();
	}
	
	public final Iterable<BNNode> getParentsI()
	{
		return this.parents.keySet();
	}
	
	public final Iterable<? extends IBayesNode> getParents()
	{
		return this.parents.keySet();
	}
	
	public final Iterable<? extends IBayesNode> getChildren()
	{
		return this.children.keySet();
	}
	
	public abstract double updateMessages() throws BNException;
	public abstract void validate() throws BNException;
	
	private static final long serialVersionUID = 50L;
	
	protected StaticBayesianNetwork bnet;
	private String name;
	
	protected HashMap<BNNode,Integer> parents = new HashMap<BNNode,Integer>();
	protected HashMap<BNNode,Integer> children = new HashMap<BNNode, Integer>();
}
