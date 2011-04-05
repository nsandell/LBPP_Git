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
import complex.featural.IFeaturalChild;
import complex.featural.FeaturalModelController;

public class MFHMMController extends FeaturalModelController<IFeaturalChild, FHMMX> {
	
	public static interface MFHMMInitialParamGenerator
	{
		public DiscreteCPT getInitialA();
		public DiscreteCPTUC getInitialPi();
		public double getA_LL(DiscreteCPT A);
		public double getPi_LL(DiscreteCPTUC pi);
	}
	
	public MFHMMController(IDynamicBayesNet network, Vector<IFeaturalChild> observations, MFHMMInitialParamGenerator paramgen, int Ns) throws BNException
	{
		super(observations);
		this.network = network;
		for(IFeaturalChild child : observations)
			this.observables.add(child);
		this.paramgen = paramgen;
		this.ns = Ns;
	}

	public MFHMMController(IDynamicBayesNet network, Vector<IFeaturalChild> observations, MFHMMInitialParamGenerator paramgen, int Ns, boolean lockParameters) throws BNException
	{
		this(network,observations,paramgen,Ns);
		this.lockXParameters = lockParameters;
	}
	
	public double parameterPosteriorLL()
	{
		double ll = 0;
		for(FHMMX lat : this.getLatentNodes())
			ll += this.paramgen.getA_LL((DiscreteCPT)lat.xnd.getAdvanceDistribution())
				+ this.paramgen.getPi_LL((DiscreteCPTUC)lat.xnd.getInitialDistribution());
		for(IFeaturalChild child : this.getObservedNodes())
			ll += child.parameterLL();
		return ll;
	}
	
	@Override
	public void saveStates(String dir) throws CMException
	{
		for(FHMMX par : this.latents)
		{
			try {
			PrintStream ps = new PrintStream(dir+"/"+par.getName()+".dat");
			for(int j = 0; j < ns; j++)
			{
				ps.print(par.xnd.getMarginal(0).getValue(j));
				for(int i = 1; i < this.network.getT(); i++)
				{
					ps.print(" " + par.xnd.getMarginal(i).getValue(j));
				}
				ps.println();
			}
			} catch(FileNotFoundException e) {
				System.err.println("Failed to create state file: " + e.toString());
			} catch(BNException e) {
				System.err.println("Failed to create state file: " + e.toString());
			}
		}
	}

	@Override
	protected void killLatentModelI(FHMMX node) throws CMException {
		try {
			this.network.removeNode(node.getName());
			this.killID(node.ID);
		} catch(BNException e) { throw new CMException(e.getMessage()); }
		catch(ClassCastException e) {throw new CMException("MFHMM Controller got invalid state ... really shouldn't happen.");}
	}

	@Override
	protected FHMMX newLatentModelI() throws CMException {
		try {
			
			int id = this.nextID();
			FHMMX newNode = new FHMMX(this.network.addDiscreteNode("X"+id, this.ns),id);

			if(this.lockXParameters)
				newNode.xnd.lockParameters();
			
			this.network.addInterEdge(newNode.xnd, newNode.xnd);
			newNode.xnd.setInitialDistribution(this.paramgen.getInitialPi());
			newNode.xnd.setAdvanceDistribution(this.paramgen.getInitialA());
			
			return newNode;
		} catch(BNException e) { throw new CMException(e.getMessage()); }
	}

	@Override
	protected void disconnectI(FHMMX latent, IFeaturalChild observed)
			throws CMException {
		try {
			this.network.removeIntraEdge(latent.getName(), observed.hook().getName());
		} catch(BNException e) { throw new CMException(e.getMessage()); }
	}

	@Override
	protected void connectI(FHMMX latent, IFeaturalChild observed)
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
	
	@Override
	protected void backupLatentModelParameters(FHMMX latent, LatentBackup<IFeaturalChild, FHMMX> backup) {
		try {
			backup.advance = latent.xnd.getAdvanceDistribution().copy();
			backup.init = latent.xnd.getInitialDistribution().copy();
		} catch(BNException e) {
			System.err.println("Failed to backup parameters for latent node " + latent.getName());
		}
	}

	@Override
	protected void restoreLatentModelParameters(FHMMX latent, LatentBackup<IFeaturalChild, FHMMX> backup) {
		try {
			latent.xnd.setAdvanceDistribution(backup.advance);
			latent.xnd.setInitialDistribution(backup.init);
		} catch(BNException e) {
			System.err.println("Failed to backup parameters for latent node " + latent.getName());
		}
	}
	
	private boolean lockXParameters = false;
	private HashSet<Integer> usedIDs = new HashSet<Integer>();
	private int minimum_available_id = 0;
	private int ns;
	private MFHMMInitialParamGenerator paramgen;
	
}
