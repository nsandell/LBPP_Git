package complex.mixture;

import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;

import complex.CMException;
import complex.IParentProcess;
import complex.ModelController;

public abstract class MixtureModelController extends ModelController {//<IMixtureChild extends IChildProcess, IParentProcess extends IParentProcess> extends ModelController {
	
	public MixtureModelController(Collection<? extends IMixtureChild> children)
	{
		this.allChildren = new Vector<IMixtureChild>(children);
	}
	
	public IParentProcess getParent(IMixtureChild child)
	{
		return this.parents.get(child);
	}
	
	public Vector<IMixtureChild> getChildren(IParentProcess parent)
	{
		return this.children.get(parent);
	}
	
	public void setParent(IMixtureChild child, IParentProcess parent) throws CMException
	{
		this.setParentI(child, parent);
		if(parents.get(child)!=null)
			this.children.get(parents.get(child)).remove(child);
		this.children.get(parent).add(child);
		this.parents.put(child, parent);
	}
	
	public Vector<IMixtureChild> getAllChildren()
	{
		return this.allChildren;
	}
	
	public Vector<IParentProcess> getAllParents()
	{
		return this.allParents;
	}
	
	public void backupChildrenParameters(IParentProcess chain) throws CMException
	{
		for(IMixtureChild child : this.children.get(chain))
			child.backupParameters();
	}
	
	public void restoreChildrenParameters(IParentProcess chain) throws CMException
	{
		for(IMixtureChild child : this.children.get(chain))
			child.restoreParameters();
	}
	
	public abstract double runChain(IParentProcess proc, int maxit, double conv) throws CMException;
	public abstract double learnChain(IParentProcess proc, int runmaxit, double runconv, int learnmaxit, double learnconv) throws CMException;
	
	public abstract void optimizeChildParameters(IMixtureChild child) throws CMException;
	
	public void deleteParent(IParentProcess parent) throws CMException
	{
		this.deleteParentI(parent);
		for(IMixtureChild child : this.children.get(parent))
			this.parents.put(child, null);
		this.parents.remove(parent);
		this.allParents.remove(parent);
	}
	
	public IParentProcess newParent() throws CMException
	{
		IParentProcess proc = this.newParentI();
		this.allParents.add(proc);
		this.children.put(proc, new Vector<IMixtureChild>());
		return proc;
	}
	
	protected abstract void deleteParentI(IParentProcess parent) throws CMException;
	protected abstract IParentProcess newParentI() throws CMException;
	protected abstract void setParentI(IMixtureChild child, IParentProcess parent) throws CMException;

	private Vector<IMixtureChild> allChildren;
	private Vector<IParentProcess> allParents = new Vector<IParentProcess>();
	private HashMap<IParentProcess, Vector<IMixtureChild>> children = new HashMap<IParentProcess, Vector<IMixtureChild>>();
	private HashMap<IMixtureChild, IParentProcess> parents = new HashMap<IMixtureChild, IParentProcess>();
}
