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

public class MF2HMMController extends FeaturalModelController<IFeaturalChild, F2HMMX> {
	
	public static interface MF2HMMInitialParamGenerator
	{
		public DiscreteCPT getInitialA();
		public DiscreteCPTUC getInitialPi();
		public DiscreteCPT getInitialC();
		public double getA_LL(DiscreteCPT A);
		public double getC_LL(DiscreteCPT C);
		public double getPi_LL(DiscreteCPT pi);
	}
	
	public MF2HMMController(IDynamicBayesNet network, Vector<IFeaturalChild> observations, MF2HMMInitialParamGenerator paramgen, int Ns, int Nsz) throws BNException
	{
		super(observations);
		this.network = network;
		for(IFeaturalChild child : observations)
			this.observables.add(child);
		this.paramgen = paramgen;
		this.ns = Ns;
		this.nsz = Nsz;
	}

	public MF2HMMController(IDynamicBayesNet network, Vector<IFeaturalChild> observations, MF2HMMInitialParamGenerator paramgen, int Ns, int Nsz, boolean lockParameters) throws BNException
	{
		this(network,observations,paramgen,Ns,Nsz);
		this.lockXParameters = lockParameters;
	}
	
	public double parameterPosteriorLL()
	{
		double ll = 0;
		for(F2HMMX lat : this.getLatentNodes())
			ll += this.paramgen.getA_LL((DiscreteCPT)lat.xnd.getAdvanceDistribution())
				+ this.paramgen.getPi_LL((DiscreteCPT)lat.xnd.getInitialDistribution())
				+ this.paramgen.getC_LL((DiscreteCPT)lat.znd.getAdvanceDistribution());
		for(IFeaturalChild child : this.getObservedNodes())
			ll += child.parameterLL();
		return ll;
	}
	
	@Override
	public void saveStates(String dir) throws CMException
	{
		for(F2HMMX par : this.latents)
		{
			try {
			PrintStream ps = new PrintStream(dir+"/"+par.getName()+".dat");
			for(int j = 0; j < ns; j++)
			{
				ps.print(par.xnd.getMarginal(0).getValue(j));
				for(int i = 1; i < this.network.getT(); i++)
					ps.print(" " + par.xnd.getMarginal(i).getValue(j));
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
	protected void killLatentModelI(F2HMMX node) throws CMException {
		try {
			this.network.removeNode(node.znd.getName());
			this.network.removeNode(node.getName());
			this.killID(node.ID);
		} catch(BNException e) { throw new CMException(e.getMessage()); }
		catch(ClassCastException e) {throw new CMException("MFHMM Controller got invalid state ... really shouldn't happen.");}
	}

	@Override
	protected F2HMMX newLatentModelI() throws CMException {
		try {
			
			int id = this.nextID();
			F2HMMX newNode = new F2HMMX(this.network.addDiscreteNode("X"+id, this.ns),this.network.addDiscreteNode("Z"+id, this.nsz),id);

			if(this.lockXParameters)
			{
				newNode.xnd.lockParameters();
				newNode.znd.lockParameters();
			}
			
			this.network.addInterEdge(newNode.znd, newNode.znd);
			this.network.addIntraEdge(newNode.znd, newNode.xnd);
			newNode.xnd.setAdvanceDistribution(this.paramgen.getInitialC());
			newNode.znd.setAdvanceDistribution(this.paramgen.getInitialA());
			newNode.znd.setInitialDistribution(this.paramgen.getInitialPi());
			
			return newNode;
		} catch(BNException e) { throw new CMException(e.getMessage()); }
	}

	@Override
	protected void disconnectI(F2HMMX latent, IFeaturalChild observed)
			throws CMException {
		try {
			this.network.removeIntraEdge(latent.getName(), observed.hook().getName());
		} catch(BNException e) { throw new CMException(e.getMessage()); }
	}

	@Override
	protected void connectI(F2HMMX latent, IFeaturalChild observed)
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
	protected void backupLatentModelParameters(F2HMMX latent, LatentBackup<IFeaturalChild, F2HMMX> backup) {
		try {
			backup.advance = latent.xnd.getAdvanceDistribution().copy();
			//backup.init = latent.xnd.getInitialDistribution().copy();
		} catch(BNException e) {
			System.err.println("Failed to backup parameters for latent node " + latent.getName());
		}
	}

	@Override
	protected void restoreLatentModelParameters(F2HMMX latent, LatentBackup<IFeaturalChild, F2HMMX> backup) {
		try {
			latent.xnd.setAdvanceDistribution(backup.advance);
			//latent.xnd.setInitialDistribution(backup.init);
		} catch(BNException e) {
			System.err.println("Failed to backup parameters for latent node " + latent.getName());
		}
	}
	
	private boolean lockXParameters = false;
	private HashSet<Integer> usedIDs = new HashSet<Integer>();
	private int minimum_available_id = 0;
	private int ns, nsz;
	private MF2HMMInitialParamGenerator paramgen;
}
