package complex.mixture.controllers;

import java.util.HashMap;

import java.util.HashSet;
import java.util.Vector;

import bn.BNException;
import bn.dynamic.IDynamicBayesNet;
import complex.CMException;
import complex.IParentProcess;
import complex.latents.LatentFactory;
import complex.mixture.IMixtureChild;
import complex.mixture.MixtureModelController;

public class MHMMController extends MixtureModelController {
	

	public MHMMController(IDynamicBayesNet network, Vector<? extends IMixtureChild> children, LatentFactory lf)
	{
		super(children);
		this.network = network;
	}
	
	@Override
	protected void deleteParentI(IParentProcess parent) throws CMException
	{
		if(this.getChildren(parent).size()!=0)
			throw new CMException("Cannot have an orphaned observation sequence in an mHMM!");
		parent.kill();
		this.parentNames.remove(parent.getName());
		this.killID(parent.id());
	}

	@Override
	protected IParentProcess newParentI() throws CMException {
		int id = this.nextID();
		//TODO  There's some ugliness as far as IDs/names... need to solidify where name is actually specified...
		IParentProcess parent = this.lf.newLatent("X"+id,id,this.network);
		this.parentNames.add("X"+id);
		return parent;
	}

	@Override
	protected void setParentI(IMixtureChild child, IParentProcess parent)
	throws CMException
	{
		if(parents.containsKey(child))
			parents.get(child).removeChild(child);
		parent.addChild(child);
		this.parents.put(child,parent);
	}

	@Override
	public double learn(int max_learn_it, double learn_conv, int max_run_it, double run_conv) throws CMException
	{
		try {
			this.network.optimize_subsets_parallel(this.parentNames, max_learn_it, learn_conv, max_run_it, run_conv);
			return this.run(max_run_it,run_conv);
		} catch(BNException e) {
			throw new CMException("Error optimizing the model : " + e.toString());
		}
	}

	@Override
	public double run(int max_it, double conv) throws CMException
	{
		try {
			this.network.run_subsets_parallel(this.parentNames, max_it, conv);
			double ll = this.network.getLogLikelihood();
			if(Double.isNaN(ll) || ll > 0)
			{
				System.out.println("Resetting to try to recover..");
				this.network.resetMessages();
				this.network.run(max_it,conv);
				ll = this.network.getLogLikelihood();
				if(Double.isNaN(ll) || ll > 0)
				{
					this.network.print(System.err);
					this.network.getLogLikelihood();
					throw new CMException("Model returns NaN/Greater than 0 log likelihood!");
				}
			}
			return ll;
		} catch(BNException e) {
			throw new CMException("Error running the model : " + e.toString());
		}
	}

	private HashMap<IMixtureChild,IParentProcess> parents = new HashMap<IMixtureChild, IParentProcess>();
	private Vector<String> parentNames = new Vector<String>();

	@Override
	public void optimizeChildParameters(IMixtureChild child)
	{
		child.optimize();
	}

	public double runChain(IParentProcess proc, int maxit, double conv) throws CMException
	{
		Vector<String> nodes = new Vector<String>();
		nodes.addAll(proc.constituentNodeNames());
		Vector<IMixtureChild> children = this.getChildren(proc);
		for(IMixtureChild child : children)
			nodes.addAll(child.constituentNodeNames());

		try {
			this.network.run_parallel_queue(maxit,conv,nodes);
			double ll = this.network.getLogLikelihood();

			if(Double.isNaN(ll) || ll > 0)
			{
				this.network.resetMessages();
				this.network.run_parallel_block(maxit,conv);
				ll = this.network.getLogLikelihood();
				if(Double.isNaN(ll) || ll > 0)
				{
					this.log("NAN LIKELIHOOD");
					this.log(this.network.toString());
					throw new CMException("Model returns NaN/Greater than 0 log likelihood!");
				}
			}
			return ll;
		} catch(BNException e) {
			throw new CMException(e.toString());
		}
	}
	
	public double learnChain(IParentProcess proc, int maxrun, double runconv, int maxlearn, double learnconv) throws CMException
	{
		Vector<String> nodes = new Vector<String>();
		nodes.addAll(proc.constituentNodeNames());
		for(IMixtureChild child : this.getChildren(proc))
			nodes.addAll(child.constituentNodeNames());
		
		try {
			this.network.optimize_parallel(maxlearn, learnconv, maxrun, runconv,nodes);
			double ll = this.network.getLogLikelihood();
			
			if(Double.isNaN(ll) || ll > 0)
			{
				System.err.println("Encountered NaN/Greater than 0 ll, attempting to recover.");
				this.network.resetMessages();
				this.network.run_parallel_block(maxrun,runconv);
				ll = this.network.getLogLikelihood();
				if(Double.isNaN(ll) || ll > 0)
				{
					this.log("NAN LIKELIHOOD");
					this.log(this.network.toString());
					throw new CMException("Model returns NaN/Greater than 0 log likelihood!");
				}
			}
			return ll;
		} catch(BNException e) {
			throw new CMException(e.toString());
		}
	}
	
	@Override
	public double parameterPosteriorLL() {
		double ll = 0;
		return ll;
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
	private LatentFactory lf;
}
