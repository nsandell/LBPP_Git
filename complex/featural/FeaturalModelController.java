package complex.featural;

import java.io.File;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import bn.distributions.Distribution;

import util.MathUtil;

import complex.CMException;
import complex.IParentProcess;

public abstract class FeaturalModelController<ChildProcess extends IFeaturalChild, ParentProcess extends IParentProcess> extends complex.ModelController
{
	
	public FeaturalModelController(Collection<? extends ChildProcess> children)
	{
		for(ChildProcess child : children)
		{
			this.parents.put(child, new HashSet<ParentProcess>());
		}
	}
	
	public ParentProcess newLatentModel() throws CMException
	{
		ParentProcess newl = this.newLatentModelI();
		this.latents.add(newl);
		this.children.put(newl, new HashSet<ChildProcess>());
		return newl;
	}

	public void killLatentModel(ParentProcess node) throws CMException
	{
		this.killLatentModelI(node);
		for(ChildProcess child : this.children.get(node))
		{
			this.parents.get(child).remove(node);
			child.killParent(node);
		}
		this.children.get(node).clear();
		this.latents.remove(node);
		this.children.remove(node);
	}
	
	public void backupParameters() throws CMException
	{
		for(ChildProcess child : this.observables)
			child.backupParameters();
		for(ParentProcess parent : this.latents)
			parent.backupParameters();
	}
	
	public void restoreParameters() throws CMException
	{
		for(ChildProcess child : this.observables)
			child.restoreParameters();
		for(ParentProcess parent : this.latents)
			parent.restoreParameters();
	}

	public LatentBackup<ChildProcess,ParentProcess> backupAndRemoveLatentModel(ParentProcess latent) throws CMException
	{
		LatentBackup<ChildProcess,ParentProcess> backup = new LatentBackup<ChildProcess,ParentProcess>(this, latent);
		this.backupLatentModelParameters(latent, backup);
		this.killLatentModel(latent);
		return backup;
	}
	protected abstract void backupLatentModelParameters(ParentProcess latent, LatentBackup<ChildProcess, ParentProcess> backup);

	public ParentProcess restoreBackup(LatentBackup<ChildProcess,ParentProcess> backup) throws CMException
	{
		ParentProcess latent = this.newLatentModel();
		this.restoreLatentModelParameters(latent, backup);
		for(ChildProcess child : backup.children)
			this.connect(latent, child);
		return latent;
	}
	protected abstract void restoreLatentModelParameters(ParentProcess latent, LatentBackup<ChildProcess, ParentProcess> backup);

	public void connect(ParentProcess latent, ChildProcess observed) throws CMException
	{
		this.connectI(latent,observed);
		this.children.get(latent).add(observed);
		this.parents.get(observed).add(latent);
		observed.addParent(latent);
	}

	public void disconnect(ParentProcess latent, ChildProcess observed) throws CMException
	{
		this.disconnectI(latent, observed);
		this.children.get(latent).remove(observed);
		this.parents.get(observed).remove(latent);
		observed.killParent(latent);
	}

	public HashSet<ChildProcess> getChildren(ParentProcess latent)
	{
		return this.children.get(latent);
	}
	
	public HashSet<ParentProcess> getParents(ChildProcess obs)
	{
		return this.parents.get(obs);
	}
	

	public static class LatentPair<ChildProcess extends IFeaturalChild,ParentProcess extends IParentProcess>
	{
		public LatentPair(ParentProcess l1, ParentProcess l2){this.l1 = l1; this.l2 = l2;}
		public ParentProcess l1, l2;
	}
	
	public LatentPair<ChildProcess,ParentProcess> randomLatentPair()
	{
		if(this.latents.size() < 2)
			return null;
		if(this.latents.size()==2)
			return new LatentPair<ChildProcess,ParentProcess>(this.latents.get(0), this.latents.get(1));
		
		int i1 = MathUtil.rand.nextInt(this.latents.size());
		int i2 = MathUtil.rand.nextInt(this.latents.size()-1);
		if(i2>=i1)
			i2++;
		
		return new LatentPair<ChildProcess,ParentProcess>(this.latents.get(i1), this.latents.get(i2));
	}
	
	public ParentProcess randomLatent()
	{
		return this.latents.get(MathUtil.rand.nextInt(this.latents.size()));
	}
	
	public static class LatentBackup<ChildProcess extends IFeaturalChild, ParentProcess extends IParentProcess>
	{
		public LatentBackup(FeaturalModelController<ChildProcess,ParentProcess> cont, ParentProcess node)
		{
			this.children =  new HashSet<ChildProcess>(cont.getChildren(node));
		}
		public HashSet<ChildProcess> children;
		public Distribution advance, init;
	}
	
	public Vector<ParentProcess> getLatentNodes(){return this.latents;}
	public Vector<ChildProcess> getObservedNodes(){return this.observables;}
	
	public void saveBest(String mainDir,double ll) throws CMException
	{
		if(mainDir==null)
			return;
	
		String dir = mainDir+"/maxll/";
		if(!(new File(dir).exists()))
			if(!((new File(dir)).mkdir()))
				throw new CMException("Could not create directory" + dir);
		
		new File(dir+"model.lbp").delete();
		new File(dir+"info.txt").delete();
		
		saveInfo(dir,ll);
	}

	public void saveInfo(String directory,double ll) throws CMException
	{
		if(directory==null)
			return;
		
		if(!(new File(directory).exists()))
			if(!((new File(directory)).mkdir()))
				throw new CMException("Could not create directory " + directory);

		try
		{
			this.network.print(new PrintStream(directory+"/model.lbp"));
			PrintStream info = new PrintStream(directory+"/info.txt");
			info.println("Log Likelihood : " + ll);
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

			File state_dir = new File(directory+"/state/");
			state_dir.mkdir();
			this.saveStates(directory+"/state/");
		} catch(FileNotFoundException e) {
			throw new CMException(e.toString());
		}
	}
	public abstract void saveStates(String directory) throws CMException;
	
	public int getT()
	{
		return this.network.getT();
	}
	
	protected abstract void killLatentModelI(ParentProcess node) throws CMException;
	protected abstract ParentProcess newLatentModelI() throws CMException;
	
	protected abstract void disconnectI(ParentProcess latent, ChildProcess observed) throws CMException;
	protected abstract void connectI(ParentProcess latent, ChildProcess observed) throws CMException;
	
	//protected IDynamicBayesNet network;
	protected Vector<ParentProcess> latents = new Vector<ParentProcess>();
	protected Vector<ChildProcess> observables = new Vector<ChildProcess>();
	private HashMap<ParentProcess, HashSet<ChildProcess>> children = new HashMap<ParentProcess, HashSet<ChildProcess>>();
	private HashMap<ChildProcess, HashSet<ParentProcess>> parents = new HashMap<ChildProcess, HashSet<ParentProcess>>();
}