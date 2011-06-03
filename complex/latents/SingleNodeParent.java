package complex.latents;

import java.util.Collection;
import java.util.Vector;

import bn.BNException;
import bn.distributions.Distribution;
import bn.dynamic.IDBNNode;
import complex.CMException;
import complex.IChildProcess;
import complex.IParentProcess;

public abstract class SingleNodeParent<NdType extends IDBNNode> implements IParentProcess {
	
	public SingleNodeParent(NdType nd, int id)
	{
		this.nd = nd;
		this.id = id;
		this.nodeName = new Vector<String>();
		this.nodeName.add(nd.getName());
	}
	
	public String getName()
	{
		return this.nd.getName();
	}

	@Override
	public final void backupParameters() {
		this.initial = nd.getInitialDistribution();
		this.advance = nd.getAdvanceDistribution();
	}

	@Override
	public final void restoreParameters() {
		try {
		if(this.initial!=null)
			nd.setInitialDistribution(this.initial);
		if(this.advance!=null)
			nd.setAdvanceDistribution(this.advance);
		} catch(BNException e) {
			System.err.println("Failed to restore parameters for node " + this.getName());
		}
	}
		
	@Override
	public final void lockParameters() {
		this.nd.lockParameters();
	}

	@Override
	public final int id() {
		return this.id;
	}

	@Override
	public final void addChild(IChildProcess child) throws CMException {
		try {
			this.nd.getNetwork().addIntraEdge(this.nd, child.hook());
		} catch(BNException e) {
			throw new CMException("Failed to add child " + child.getName() + " to parent " + this.getName() + ": " + e.toString());
		}
	}

	@Override
	public final void removeChild(IChildProcess child) throws CMException {
		try {
			this.nd.getNetwork().removeInterEdge(this.nd, child.hook());
		} catch(BNException e) {
			throw new CMException("Failed to remove child " + child.getName() + " from parent " + this.getName() + ": " + e.toString());
		}
	}

	@Override
	public final Collection<String> constituentNodeNames() {
		return nodeName;
	}

	protected int id;
	protected NdType nd;
	protected Vector<String> nodeName;
	protected Distribution initial, advance;
}
