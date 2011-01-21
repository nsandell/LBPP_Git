package complex.featural;

import java.io.PrintStream;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import util.MathUtil;

import bn.BNException;
import bn.IDynBayesNet;
import bn.IDynBayesNode;

public abstract class ModelController
{

	public double run(int max_it, double conv)  throws FMMException
	{
		try {
			this.network.run_parallel_block(max_it, conv);
			double ll = this.network.getLogLikelihood();
			if(Double.isNaN(ll))
			{
				this.network.resetMessages();
				this.network.run_parallel_block(max_it,conv);
				if(Double.isNaN(ll))
					throw new FMMException("Model returns NaN log likelihood!");
			}
			return ll;
		} catch(BNException e) {
			throw new FMMException("Error running the model : " + e.toString());
		}
	}
	
	public double learn(int max_learn_it, double learn_conv, int max_run_it, double run_conv) throws FMMException
	{
		try {
			this.network.optimize_parallel(max_learn_it, learn_conv, max_run_it, run_conv);
			return this.run(max_run_it,run_conv);
		} catch(BNException e) {
			throw new FMMException("Error running the model : " + e.toString());
		}
	}

	public IParentProcess newLatentModel() throws FMMException
	{
		IParentProcess newl = this.newLatentModelI();
		this.latents.add(newl);
		return newl;
	}

	public void killLatentModel(IParentProcess node) throws FMMException
	{
		this.killLatentModelI(node);
		this.latents.remove(node);
	}

	public LatentBackup backupAndRemoveLatentModel(IParentProcess latent) throws FMMException
	{/*
		LatentBackup backup = new LatentBackup(this, latent);
		for(IDynBayesNode child : backup.children)
			this.disconnect(latent, child);
		this.killLatentModel(latent);
		return backup;*/return null;
	}

	public IDynBayesNode restoreBackup(LatentBackup backup) throws FMMException
	{/*
		IDynBayesNode latent = this.newLatentModel();
		for(IDynBayesNode child : backup.children)
			this.connect(latent, child);
		return latent;*/return null;
	}

	public void connect(IParentProcess latent, IChildProcess observed) throws FMMException
	{
		this.connectI(latent,observed);
		this.children.get(latent).add(observed);
		this.parents.get(observed).add(latent);
	}

	public void disconnect(IParentProcess latent, IChildProcess observed) throws FMMException
	{
		this.disconnectI(latent, observed);
		this.children.get(latent).remove(observed);
		this.parents.get(observed).remove(latent);
	}

	public HashSet<IChildProcess> getChildren(IParentProcess latent)
	{
		return this.children.get(latent);
	}
	
	public HashSet<IParentProcess> getParents(IChildProcess obs)
	{
		return this.parents.get(obs);
	}
	
	public void log(String msg)
	{
		if(logger!=null)
			logger.println(msg);
	}
	
	public void setLogger(PrintStream log)
	{
		this.logger = log;
	}
	
	public static class LatentPair
	{
		public LatentPair(IParentProcess l1, IParentProcess l2){this.l1 = l1; this.l2 = l2;}
		public IParentProcess l1, l2;
	}
	
	public LatentPair randomLatentPair()
	{
		if(this.latents.size() < 2)
			return null;
		if(this.latents.size()==2)
			return new LatentPair(this.latents.get(0), this.latents.get(1));
		
		int i1 = MathUtil.rand.nextInt(this.latents.size());
		int i2 = MathUtil.rand.nextInt(this.latents.size()-1);
		if(i2>=i2)
			i2++;
		
		return new LatentPair(this.latents.get(i1), this.latents.get(i2));
	}
	
	public IParentProcess randomLatent()
	{
		return this.latents.get(MathUtil.rand.nextInt(this.latents.size()));
	}
	
	public Vector<IParentProcess> getLatentNodes(){return this.latents;}
	public Vector<IChildProcess> getObservedNodes(){return this.observables;}

	public abstract void saveInfo();
	
	protected abstract void killLatentModelI(IParentProcess node) throws FMMException;
	protected abstract IParentProcess newLatentModelI() throws FMMException;
	
	protected abstract void disconnectI(IParentProcess latent, IChildProcess observed) throws FMMException;
	protected abstract void connectI(IParentProcess latent, IChildProcess observed) throws FMMException;
	
	public static class LatentBackup
	{
		public LatentBackup(ModelController cont, IParentProcess node)
		{
			this.children = cont.getChildren(node);
		}
		public HashSet<IChildProcess> children;
	}

	protected PrintStream logger = null;
	protected IDynBayesNet network;
	protected Vector<IParentProcess> latents = new Vector<IParentProcess>();
	protected Vector<IChildProcess> observables = new Vector<IChildProcess>();
	private HashMap<IParentProcess, HashSet<IChildProcess>> children = new HashMap<IParentProcess, HashSet<IChildProcess>>();
	private HashMap<IChildProcess, HashSet<IParentProcess>> parents = new HashMap<IChildProcess, HashSet<IParentProcess>>();
}