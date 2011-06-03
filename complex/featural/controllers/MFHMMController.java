package complex.featural.controllers;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Vector;


import bn.BNException;
import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;
import bn.dynamic.IDynamicBayesNet;
import complex.CMException;
import complex.IParentProcess;
import complex.featural.IFeaturalChild;
import complex.featural.FeaturalModelController;
import complex.latents.LatentFactory;

public class MFHMMController extends FeaturalModelController {
	
	public static interface MFHMMInitialParamGenerator
	{
		public DiscreteCPT getInitialA();
		public DiscreteCPTUC getInitialPi();
		public double getA_LL(DiscreteCPT A);
		public double getPi_LL(DiscreteCPTUC pi);
	}
	
	public MFHMMController(IDynamicBayesNet network, Vector<IFeaturalChild> observations, LatentFactory lf) throws BNException
	{
		super(observations);
		this.network = network;
		for(IFeaturalChild child : observations)
			this.observables.add(child);
		this.lf = lf;
	}

	public MFHMMController(IDynamicBayesNet network, Vector<IFeaturalChild> observations, LatentFactory lf, boolean lockParameters) throws BNException
	{
		this(network,observations,lf);
		this.lockXParameters = lockParameters;
	}
	
	public double parameterPosteriorLL()
	{
		double ll = 0;
		for(IParentProcess lat : this.getLatentNodes())
			ll += lat.parameterLL();
		for(IFeaturalChild child : this.getObservedNodes())
			ll += child.parameterLL();
		return ll;
	}
	
	@Override
	public void saveStates(String dir) throws CMException
	{
		for(IParentProcess par : this.latents)
		{
			try {
				PrintStream ps = new PrintStream(dir+"/"+par.getName()+".dat");
				par.printMarginal(ps);
			} catch(FileNotFoundException e) {
				System.err.println("Failed to create state file: " + e.toString());
			} 
		}
	}

	@Override
	protected void killLatentModelI(IParentProcess node) throws CMException {
		try {
			this.network.removeNode(node.getName());
			this.killID(node.id());
		} catch(BNException e) { throw new CMException(e.getMessage()); }
		catch(ClassCastException e) {throw new CMException("MFHMM Controller got invalid state ... really shouldn't happen.");}
	}

	@Override
	protected IParentProcess newLatentModelI() throws CMException {
		int id = this.nextID();
		IParentProcess newNode = lf.newLatent("X"+id, id, this.network);

		if(this.lockXParameters)
			newNode.lockParameters();

		return newNode;
	}

	@Override
	protected void disconnectI(IParentProcess latent, IFeaturalChild observed)
			throws CMException {
		try {
			this.network.removeIntraEdge(latent.getName(), observed.hook().getName());
		} catch(BNException e) { throw new CMException(e.getMessage()); }
	}

	@Override
	protected void connectI(IParentProcess latent, IFeaturalChild observed)
			throws CMException {
		try {
			this.network.addIntraEdge(latent.getName(), observed.hook().getName());
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
	
	private boolean lockXParameters = false;
	private HashSet<Integer> usedIDs = new HashSet<Integer>();
	private int minimum_available_id = 0;
	private LatentFactory lf;
}
