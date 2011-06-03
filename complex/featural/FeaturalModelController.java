package complex.featural;

import java.io.File;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import util.MathUtil;

import complex.CMException;
import complex.IParentProcess;
import complex.ModelController;

public abstract class FeaturalModelController extends ModelController
{
	
	public FeaturalModelController(Collection<? extends IFeaturalChild> children)
	{
		for(IFeaturalChild child : children)
			this.parents.put(child, new HashSet<IParentProcess>());
	}
	
	public IParentProcess newLatentModel() throws CMException
	{
		IParentProcess newl = this.newLatentModelI();
		this.latents.add(newl);
		this.children.put(newl, new HashSet<IFeaturalChild>());
		return newl;
	}

	public void killLatentModel(IParentProcess node) throws CMException
	{
		this.killLatentModelI(node);
		for(IFeaturalChild child : this.children.get(node))
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
		for(IFeaturalChild child : this.observables)
			child.backupParameters();
		for(IParentProcess parent : this.latents)
			parent.backupParameters();
	}
	
	public void restoreParameters() throws CMException
	{
		for(IFeaturalChild child : this.observables)
			child.restoreParameters();
		for(IParentProcess parent : this.latents)
			parent.restoreParameters();
	}

	public void connect(IParentProcess latent, IFeaturalChild observed) throws CMException
	{
		this.connectI(latent,observed);
		this.children.get(latent).add(observed);
		this.parents.get(observed).add(latent);
		observed.addParent(latent);
	}

	public void disconnect(IParentProcess latent, IFeaturalChild observed) throws CMException
	{
		this.disconnectI(latent, observed);
		this.children.get(latent).remove(observed);
		this.parents.get(observed).remove(latent);
		observed.killParent(latent);
	}

	public HashSet<IFeaturalChild> getChildren(IParentProcess latent)
	{
		return this.children.get(latent);
	}
	
	public HashSet<IParentProcess> getParents(IFeaturalChild obs)
	{
		return this.parents.get(obs);
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
	
	public Vector<IParentProcess> getLatentNodes(){return this.latents;}
	public Vector<IFeaturalChild> getObservedNodes(){return this.observables;}
	
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
	
	protected abstract void killLatentModelI(IParentProcess node) throws CMException;
	protected abstract IParentProcess newLatentModelI() throws CMException;
	
	protected abstract void disconnectI(IParentProcess latent, IFeaturalChild observed) throws CMException;
	protected abstract void connectI(IParentProcess latent, IFeaturalChild observed) throws CMException;
	
	protected Vector<IParentProcess> latents = new Vector<IParentProcess>();
	protected Vector<IFeaturalChild> observables = new Vector<IFeaturalChild>();
	private HashMap<IParentProcess, HashSet<IFeaturalChild>> children = new HashMap<IParentProcess, HashSet<IFeaturalChild>>();
	private HashMap<IFeaturalChild, HashSet<IParentProcess>> parents = new HashMap<IFeaturalChild, HashSet<IParentProcess>>();
}