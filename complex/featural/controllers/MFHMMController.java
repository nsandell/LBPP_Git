package complex.featural.controllers;

import java.util.HashSet;
import java.util.Vector;


import bn.BNException;
import bn.IDynBayesNet;
import bn.IDynBayesNode;
import bn.distributions.DiscreteCPT;
import complex.featural.FMMException;
import complex.featural.IChildProcess;
import complex.featural.IParentProcess;
import complex.featural.ModelController;

public class MFHMMController extends ModelController {
	
	public static interface IFHMMChild extends IChildProcess
	{
		public IDynBayesNode hook();
	}
	
	private static class FHMMX implements IParentProcess
	{
		FHMMX(IDynBayesNode xnd,int ID)
		{
			this.xnd = xnd;
			this.ID = ID;
		}
		public String getName()
		{
			return this.xnd.getName();
		}
		int ID;
		IDynBayesNode xnd;
	}
	
	public static interface MFHMMInitialParamGenerator
	{
		public DiscreteCPT getInitialA();
		public DiscreteCPT getInitialPi();
	}
	
	public MFHMMController(IDynBayesNet network, Vector<IFHMMChild> observations, MFHMMInitialParamGenerator paramgen, int Ns) throws BNException
	{
		this.network = network;
		for(IFHMMChild child : observations)
			this.observables.add(child);
		this.paramgen = paramgen;
		this.ns = Ns;
	}

	@Override
	public void saveInfo()
	{
		// TODO Auto-generated method stub
	}

	@Override
	protected void killLatentModelI(IParentProcess node) throws FMMException {
		try {
			this.network.removeNode(node.getName());
			this.killID(((FHMMX)node).ID);
		} catch(BNException e) { throw new FMMException(e.getMessage()); }
		catch(ClassCastException e) {throw new FMMException("MFHMM Controller got invalid state ... really shouldn't happen.");}
	}

	@Override
	protected IParentProcess newLatentModelI() throws FMMException {
		try {
			
			int id = this.nextID();
			FHMMX newNode = new FHMMX(this.network.addDiscreteNode("X"+id, this.ns),id);
			this.network.addInterEdge(newNode.xnd, newNode.xnd);
			newNode.xnd.setInitialDistribution(this.paramgen.getInitialPi());
			newNode.xnd.setDistribution(this.paramgen.getInitialA());
			
			return newNode;
		} catch(BNException e) { throw new FMMException(e.getMessage()); }
	}

	@Override
	protected void disconnectI(IParentProcess latent, IChildProcess observed)
			throws FMMException {
		if(!(observed instanceof IFHMMChild))
			throw new FMMException("MFHMM Controller received invalid child... this really shouldn't happen.");
		try {
			this.network.removeIntraEdge(latent.getName(), ((IFHMMChild)observed).hook().getName());
		} catch(BNException e) { throw new FMMException(e.getMessage()); }
	}

	@Override
	protected void connectI(IParentProcess latent, IChildProcess observed)
			throws FMMException {
				if(!(observed instanceof IFHMMChild))
			throw new FMMException("MFHMM Controller received invalid child... this really shouldn't happen.");
		try {
			this.network.addIntraEdge(latent.getName(), ((IFHMMChild)observed).hook().getName());
		} catch(BNException e) { throw new FMMException(e.getMessage()); }
	}

	private int nextID()
	{
		int ret = this.minimum_available_id;
		while(true)
		{
			this.minimum_available_id++;
			if(!this.usedIDs.contains(this.minimum_available_id))
				return ret;
		}
	}
	
	private void killID(int id)
	{
		this.usedIDs.remove(id);
		if(id < this.minimum_available_id)
			this.minimum_available_id = id;
	}
	
	private HashSet<Integer> usedIDs = new HashSet<Integer>();
	private int minimum_available_id = 0;
	private int ns;
	private IDynBayesNet network;
	private MFHMMInitialParamGenerator paramgen;
}
