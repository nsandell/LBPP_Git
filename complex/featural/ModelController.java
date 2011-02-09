package complex.featural;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import util.MathUtil;

import bn.BNException;
import bn.dynamic.IDynNet;

public abstract class ModelController
{
	
	public ModelController(Collection<? extends IChildProcess> children)
	{
		for(IChildProcess child : children)
		{
			this.parents.put(child, new HashSet<IParentProcess>());
		}
	}
	
	public void validate() throws FMMException
	{
		try {
			this.network.validate();
		} catch(BNException e) {throw new FMMException(e.toString());}
	}

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
				{
					this.network.print(System.err);
					this.network.resetMessages();
					return Double.NEGATIVE_INFINITY;
					//throw new FMMException("Model returns NaN log likelihood!");
				}
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
		this.children.put(newl, new HashSet<IChildProcess>());
		return newl;
	}

	public void killLatentModel(IParentProcess node) throws FMMException
	{
		this.killLatentModelI(node);
		for(IChildProcess child : this.children.get(node))
			this.parents.get(child).remove(node);
		this.children.get(node).clear();
		this.latents.remove(node);
		this.children.remove(node);
	}

	public LatentBackup backupAndRemoveLatentModel(IParentProcess latent) throws FMMException
	{
		LatentBackup backup = new LatentBackup(this, latent);
		this.killLatentModel(latent);
		return backup;
	}

	public IParentProcess restoreBackup(LatentBackup backup) throws FMMException
	{
		IParentProcess latent = this.newLatentModel();
		for(IChildProcess child : backup.children)
			this.connect(latent, child);
		return latent;
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
		if(i2>=i1)
			i2++;
		
		return new LatentPair(this.latents.get(i1), this.latents.get(i2));
	}
	
	public IParentProcess randomLatent()
	{
		return this.latents.get(MathUtil.rand.nextInt(this.latents.size()));
	}
	
	public static class LatentBackup
	{
		public LatentBackup(ModelController cont, IParentProcess node)
		{
			//TODO what's this why did I do this
			this.children = (HashSet<IChildProcess>)cont.getChildren(node).clone();
		}
		public HashSet<IChildProcess> children;
	}
	
	public Vector<IParentProcess> getLatentNodes(){return this.latents;}
	public Vector<IChildProcess> getObservedNodes(){return this.observables;}

	public void saveInfo(String directory) throws FMMException
	{
		if(directory==null)
			return;
		
		if(!((new File(directory)).mkdir()))
			throw new FMMException("Could not create directory " + directory);
		
		try
		{
			this.network.print(new PrintStream(directory+"/model.lbp"));
			PrintStream info = new PrintStream(directory+"/info.txt");
			info.println("Log Likelihood : " + this.network.getLogLikelihood());
			info.println();
			info.println();
			for(int i = 0; i < latents.size(); i++)
			{
				info.print("\t" + latents.get(i).getName());
			}
			info.println();
			for(int j = 0; j < observables.size(); j++)
			{
				info.print(observables.get(j).getName());
				for(int i = 0; i < latents.size(); i++)
				{
					if(parents.get(observables.get(j)).contains(latents.get(i)))
						info.print("\t1");
					else
						info.print("\t0");
				}
				info.println();
			}		
		} catch(FileNotFoundException e) {
			throw new FMMException(e.toString());
		} catch(BNException e) {
			throw new FMMException(e.toString());
		}
	}
	
	protected abstract void killLatentModelI(IParentProcess node) throws FMMException;
	protected abstract IParentProcess newLatentModelI() throws FMMException;
	
	protected abstract void disconnectI(IParentProcess latent, IChildProcess observed) throws FMMException;
	protected abstract void connectI(IParentProcess latent, IChildProcess observed) throws FMMException;
	
	protected PrintStream logger = null;
	protected IDynNet network;
	protected Vector<IParentProcess> latents = new Vector<IParentProcess>();
	protected Vector<IChildProcess> observables = new Vector<IChildProcess>();
	private HashMap<IParentProcess, HashSet<IChildProcess>> children = new HashMap<IParentProcess, HashSet<IChildProcess>>();
	private HashMap<IChildProcess, HashSet<IParentProcess>> parents = new HashMap<IChildProcess, HashSet<IParentProcess>>();
}