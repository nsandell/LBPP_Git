package complex.mixture;

import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;

import complex.CMException;
import complex.IChildProcess;
import complex.IParentProcess;
import complex.ModelController;

public abstract class MixtureModelController<ChildType extends IChildProcess, ParentType extends IParentProcess> extends ModelController {
	
	public MixtureModelController(Collection<ChildType> children)
	{
		this.allChildren = new Vector<ChildType>(children);
	}
	
	public ParentType getParent(ChildType child)
	{
		return this.parents.get(child);
	}
	
	public Vector<ChildType> getChildren(ParentType parent)
	{
		return this.children.get(parent);
	}
	
	public void setParent(ChildType child, ParentType parent) throws CMException
	{
		this.setParentI(child, parent);
		if(parents.get(child)!=null)
			this.children.get(parents.get(child)).remove(child);
		this.children.get(parent).add(child);
		this.parents.put(child, parent);
	}
	
	public Vector<ChildType> getAllChildren()
	{
		return this.allChildren;
	}
	
	public Vector<ParentType> getAllParents()
	{
		return this.allParents;
	}
	
	public abstract double runChain(ParentType proc, int maxit, double conv) throws CMException;
	
	public abstract void optimizeChildParameters(ChildType child) throws CMException;
	
	public void deleteParent(ParentType parent) throws CMException
	{
		this.deleteParentI(parent);
		for(ChildType child : this.children.get(parent))
			this.parents.put(child, null);
		this.parents.remove(parent);
		this.allParents.remove(parent);
	}
	
	public ParentType newParent() throws CMException
	{
		ParentType proc = this.newParentI();
		this.allParents.add(proc);
		this.children.put(proc, new Vector<ChildType>());
		return proc;
	}
	
	protected abstract void deleteParentI(ParentType parent) throws CMException;
	protected abstract ParentType newParentI() throws CMException;
	protected abstract void setParentI(ChildType child, ParentType parent) throws CMException;

	private Vector<ChildType> allChildren;
	private Vector<ParentType> allParents = new Vector<ParentType>();
	private HashMap<ParentType, Vector<ChildType>> children = new HashMap<ParentType, Vector<ChildType>>();
	private HashMap<ChildType, ParentType> parents = new HashMap<ChildType, ParentType>();
}
