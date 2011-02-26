package bn.impl.staticbn;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Vector;

import bn.BNException;
import bn.IBayesNode;
import bn.impl.InternalIBayesNode;
import bn.impl.staticbn.StaticContextManagers.StaticMessageIndex;
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
			StaticMessageIndex pi = this.addChildInterface(mi);
			try
			{
				StaticMessageIndex ci = child.addParentInterface(mi);
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

		StaticMessageIndex child_idx = this.children.remove(child);
		StaticMessageIndex rent_idx = child.parents.remove(this);

		child.removeParentInterface(rent_idx);
		this.removeChildInterface(child_idx);
	}
	
	public final void removeAllChildren() throws BNException
	{
		Vector<BNNode> childrenCopy = new Vector<BNNode>(this.children.keySet());
		for(BNNode child : childrenCopy)
			this.removeChild(child);
	}
	
	public final void removeAllParents() throws BNException
	{
		Vector<BNNode> parentsCopy = new Vector<BNNode>(this.parents.keySet());
		for(BNNode parent : parentsCopy)
			parent.removeChild(this);
	}
	
	protected abstract MessageInterface<?> newChildInterface() throws BNException;

	protected abstract StaticMessageIndex addParentInterface(MessageInterface<?> mi) throws BNException;
	protected abstract StaticMessageIndex addChildInterface (MessageInterface<?> mi) throws BNException;
	protected abstract void removeParentInterface(StaticMessageIndex index) throws BNException;
	protected abstract void removeChildInterface (StaticMessageIndex index) throws BNException;

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
	
	@Override
    public String getEdgeDefinition()
    {
		Vector<Entry<BNNode,StaticMessageIndex>> orderedEdges = new Vector<Entry<BNNode,StaticMessageIndex>>(this.parents.entrySet());
		Collections.sort(orderedEdges,new EntComp());
		String ret = "";
		for(int i = 0; i < orderedEdges.size(); i++)
			ret += orderedEdges.get(i).getKey().getName()+"->"+this.getName()+"\n";
		return ret;
    }
	
	private static class EntComp implements Comparator<Entry<BNNode, StaticMessageIndex>>
	{
		@Override
		public int compare(Entry<BNNode, StaticMessageIndex> o1,
				Entry<BNNode, StaticMessageIndex> o2) {
			return o1.getValue().compareTo(o2.getValue());
		}
	}
	
	@Override
	public StaticBayesianNetwork getNetwork()
	{
		return this.bnet;
	}
	
	public abstract double updateMessages() throws BNException;
	public abstract void validate() throws BNException;
	
	private static final long serialVersionUID = 50L;
	
	protected StaticBayesianNetwork bnet;
	private String name;
	
	protected HashMap<BNNode, StaticMessageIndex> parents = new HashMap<BNNode, StaticMessageIndex>();
	protected HashMap<BNNode, StaticMessageIndex> children = new HashMap<BNNode, StaticMessageIndex>();
}
