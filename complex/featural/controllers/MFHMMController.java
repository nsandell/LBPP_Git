package complex.featural.controllers;

import java.util.HashSet;
import java.util.Vector;


import bn.BNException;
import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;
import bn.dynamic.IDynamicBayesNet;
import bn.dynamic.IDBNNode;
import bn.dynamic.IFDiscDBNNode;
import bn.messages.FiniteDiscreteMessage;
import complex.CMException;
import complex.featural.IChildProcess;
import complex.featural.IParentProcess;
import complex.featural.FeaturalModelController;

public class MFHMMController extends FeaturalModelController {
	
	public static interface IFHMMChild extends IChildProcess
	{
		public IDBNNode hook();
	}
	
	private static class FHMMX implements IParentProcess
	{
		FHMMX(IFDiscDBNNode xnd,int ID)
		{
			this.xnd = xnd;
			this.ID = ID;
		}
		public String getName()
		{
			return this.xnd.getName();
		}
		public FiniteDiscreteMessage marginal(int t)
		{
			try {
				return this.xnd.getMarginal(t);
			} catch(BNException e) {
				System.err.println("Error : " + e.toString());
				return null;
			}
		}
		int ID;
		IFDiscDBNNode xnd;
	}
	
	public static interface MFHMMInitialParamGenerator
	{
		public DiscreteCPT getInitialA();
		public DiscreteCPTUC getInitialPi();
	}
	
	public MFHMMController(IDynamicBayesNet network, Vector<IFHMMChild> observations, MFHMMInitialParamGenerator paramgen, int Ns) throws BNException
	{
		super(observations);
		this.network = network;
		for(IFHMMChild child : observations)
			this.observables.add(child);
		this.paramgen = paramgen;
		this.ns = Ns;
	}


	@Override
	protected void killLatentModelI(IParentProcess node) throws CMException {
		try {
			this.network.removeNode(node.getName());
			this.killID(((FHMMX)node).ID);
		} catch(BNException e) { throw new CMException(e.getMessage()); }
		catch(ClassCastException e) {throw new CMException("MFHMM Controller got invalid state ... really shouldn't happen.");}
	}

	@Override
	protected IParentProcess newLatentModelI() throws CMException {
		try {
			
			int id = this.nextID();
			FHMMX newNode = new FHMMX(this.network.addDiscreteNode("X"+id, this.ns),id);
			this.network.addInterEdge(newNode.xnd, newNode.xnd);
			newNode.xnd.setInitialDistribution(this.paramgen.getInitialPi());
			newNode.xnd.setAdvanceDistribution(this.paramgen.getInitialA());
			
			return newNode;
		} catch(BNException e) { throw new CMException(e.getMessage()); }
	}

	@Override
	protected void disconnectI(IParentProcess latent, IChildProcess observed)
			throws CMException {
		if(!(observed instanceof IFHMMChild))
			throw new CMException("MFHMM Controller received invalid child... this really shouldn't happen.");
		try {
			this.network.removeIntraEdge(latent.getName(), ((IFHMMChild)observed).hook().getName());
		} catch(BNException e) { throw new CMException(e.getMessage()); }
	}

	@Override
	protected void connectI(IParentProcess latent, IChildProcess observed)
			throws CMException {
				if(!(observed instanceof IFHMMChild))
			throw new CMException("MFHMM Controller received invalid child... this really shouldn't happen.");
		try {
			this.network.addIntraEdge(latent.getName(), ((IFHMMChild)observed).hook().getName());
		} catch(BNException e) { throw new CMException(e.getMessage()); }
	}

	private int nextID()
	{
		int ret = this.minimum_available_id;
		this.usedIDs.add(ret);
		while(true)
		{
			this.minimum_available_id++;
			if(!this.usedIDs.contains(this.minimum_available_id))
				break;
		}
		return ret;
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
	private MFHMMInitialParamGenerator paramgen;
}
