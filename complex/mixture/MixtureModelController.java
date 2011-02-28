package complex.mixture;

import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;

import complex.CMException;
import complex.featural.IChildProcess;
import complex.featural.IParentProcess;

public abstract class MixtureModelController extends complex.ModelController{
	
	public MixtureModelController(Collection<? extends IChildProcess> children)
	{
		this.allChildren = new Vector<IChildProcess>(children);
	}
	
	public IParentProcess getParent(IChildProcess child)
	{
		return this.parents.get(child);
	}
	
	public Vector<IChildProcess> getChildren(IParentProcess parent)
	{
		return this.children.get(parent);
	}
	
	public void setParent(IChildProcess child, IParentProcess parent) throws CMException
	{
		this.setParentI(child, parent);
		if(parents.get(child)!=null)
			this.children.get(parents.get(child)).remove(child);
		this.children.get(parent).add(child);
		this.parents.put(child, parent);
	}
	
	public Vector<IChildProcess> getAllChildren()
	{
		return this.allChildren;
	}
	
	public Vector<IParentProcess> getAllParents()
	{
		return this.allParents;
	}
	
	public abstract double runChain(IParentProcess proc, int maxit, double conv) throws CMException;
	
	public abstract void optimizeChildParameters(IChildProcess child) throws CMException;
	
	public void deleteParent(IParentProcess parent) throws CMException
	{
		this.deleteParentI(parent);
		for(IChildProcess child : this.children.get(parent))
			this.parents.put(child, null);
		this.parents.remove(parent);
		this.allParents.remove(parent);
	}
	
	public IParentProcess newParent() throws CMException
	{
		IParentProcess proc = this.newParentI();
		this.allParents.add(proc);
		this.children.put(proc, new Vector<IChildProcess>());
		return proc;
	}
	
	protected abstract void deleteParentI(IParentProcess parent) throws CMException;
	protected abstract IParentProcess newParentI() throws CMException;
	protected abstract void setParentI(IChildProcess child, IParentProcess parent) throws CMException;

	private Vector<IChildProcess> allChildren;
	private Vector<IParentProcess> allParents = new Vector<IParentProcess>();
	private HashMap<IParentProcess, Vector<IChildProcess>> children = new HashMap<IParentProcess, Vector<IChildProcess>>();
	private HashMap<IChildProcess, IParentProcess> parents = new HashMap<IChildProcess, IParentProcess>();
}
