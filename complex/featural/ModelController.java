package complex.featural;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import util.MathUtil;

import bn.BNException;
import bn.IDynBayesNet;
import bn.IDynBayesNode;

public abstract class ModelController {
	
	public ModelController(IDynBayesNet net, boolean learn)
	{
		this(net,30,1e-5,5,1e-5);
		this.learning = learn;
	}

	public ModelController(IDynBayesNet net, int run_it, double run_conv)
	{
		this.learning = false;
		this.run_it = run_it;
		this.run_conv = run_conv;
		this.network = net;
	}
	
	public ModelController(IDynBayesNet net, int run_it, double run_conv, int learn_it, double learn_conv)
	{
		this.learning = false;
		this.run_it = run_it;
		this.run_conv = run_conv;
		this.learn_it = learn_it;
		this.learn_conv = learn_conv;
		this.network = net;
	}
	
	public double run()  throws FMMException
	{
		try {
			this.network.run_parallel_block(run_it, run_conv);
			double ll = this.network.getLogLikelihood();
			if(Double.isNaN(ll))
			{
				this.network.resetMessages();
				this.network.run_parallel_block(run_it,run_conv);
				if(Double.isNaN(ll))
					throw new FMMException("Model returns NaN log likelihood!");
			}
			return ll;
		} catch(BNException e) {
			throw new FMMException("Error running the model : " + e.toString());
		}
	}
	
	public double learn() throws FMMException
	{
		try {
			this.network.optimize_parallel(learn_it, learn_conv, run_it, run_conv);
			return this.run();
		} catch(BNException e) {
			throw new FMMException("Error running the model : " + e.toString());
		}
	}

	public IDynBayesNode newLatentModel() throws FMMException
	{
		IDynBayesNode newl = this.newLatentModelI();
		this.latents.add(newl);
		return newl;
	}

	public void killLatentModel(IDynBayesNode node) throws FMMException
	{
		this.killLatentModelI(node);
		this.latents.remove(node);
	}

	public LatentBackup backupAndRemoveLatentModel(IDynBayesNode latent) throws FMMException
	{
		LatentBackup backup = new LatentBackup(this, latent);
		for(IDynBayesNode child : backup.children)
			this.disconnect(latent, child);
		this.killLatentModel(latent);
		return backup;
	}

	public IDynBayesNode restoreBackup(LatentBackup backup) throws FMMException
	{
		IDynBayesNode latent = this.newLatentModel();
		for(IDynBayesNode child : backup.children)
			this.connect(latent, child);
		return latent;
	}

	public void connect(IDynBayesNode latent, IDynBayesNode observed) throws FMMException
	{
		this.connectI(latent,observed);
		this.children.get(latent).add(observed);
	}

	public void disconnect(IDynBayesNode latent, IDynBayesNode observed) throws FMMException
	{
		this.disconnectI(latent, observed);
		this.children.get(latent).remove(observed);
	}

	public HashSet<IDynBayesNode> getChildren(IDynBayesNode latent)
	{
		return this.children.get(latent);
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
		public LatentPair(IDynBayesNode l1, IDynBayesNode l2){this.l1 = l1; this.l2 = l2;}
		public IDynBayesNode l1, l2;
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
	
	public IDynBayesNode randomLatent()
	{
		return this.latents.get(MathUtil.rand.nextInt(this.latents.size()));
	}
	
	public Vector<IDynBayesNode> getLatentNodes(){return this.latents;}
	public Vector<IDynBayesNode> getObservedNodes(){return this.observables;}

	public abstract void saveInfo(Vector<IDynBayesNode> latents, Vector<IDynBayesNode> observeds, double ll);
	
	protected abstract void killLatentModelI(IDynBayesNode node) throws FMMException;
	protected abstract IDynBayesNode newLatentModelI() throws FMMException;
	
	protected abstract void disconnectI(IDynBayesNode latent, IDynBayesNode observed) throws FMMException;
	protected abstract void connectI(IDynBayesNode latent, IDynBayesNode observed) throws FMMException;
	
	public static class LatentBackup
	{
		public LatentBackup(ModelController cont, IDynBayesNode node)
		{
			this.children = cont.getChildren(node);
		}
		public HashSet<IDynBayesNode> children;
	}

	protected PrintStream logger = null;
	protected int learn_it, run_it;
	protected double learn_conv, run_conv;
	protected boolean learning;
	protected IDynBayesNet network;
	protected Vector<IDynBayesNode> latents = new Vector<IDynBayesNode>();
	protected Vector<IDynBayesNode> observables = new Vector<IDynBayesNode>();
	private HashMap<IDynBayesNode, HashSet<IDynBayesNode>> children = new HashMap<IDynBayesNode, HashSet<IDynBayesNode>>();
}