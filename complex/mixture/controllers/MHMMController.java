package complex.mixture.controllers;

import java.util.HashMap;

import java.util.HashSet;
import java.util.Vector;

import bn.BNException;
import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;
import bn.dynamic.IDynamicBayesNet;
import bn.dynamic.IFDiscDBNNode;
import complex.CMException;
import complex.mixture.MixtureModelController;

public class MHMMController extends MixtureModelController<MHMMChild,MHMMX> {
	

	public MHMMController(IDynamicBayesNet network, Vector<MHMMChild> children, MHMMParameterPrior priors, int ns)
	{
		super(children);
		this.network = network;
		this.priors = priors;
		this.ns = ns;
	}
	
	public MHMMController(IDynamicBayesNet network, Vector<MHMMChild> children, MHMMParameterPrior priors, int ns, boolean lockMCParams)
	{
		this(network,children,priors,ns);
		this.lockMCParams = lockMCParams;
	}
	private boolean lockMCParams =  false;

	public static interface MHMMParameterPrior
	{
		public double evaluate(DiscreteCPT A);
		public double evaluate(DiscreteCPTUC pi);

		public DiscreteCPT initialSampleA();
		public DiscreteCPTUC initialSamplePi();
		public DiscreteCPT posteriorSampleA(DiscreteCPT A, int T);
		public DiscreteCPTUC posteriorSamplePi(DiscreteCPTUC pi);
	}

	@Override
	protected void deleteParentI(MHMMX parent) throws CMException
	{
		if(this.getChildren(parent).size()!=0)
			throw new CMException("Cannot have an orphaned observation sequence in an mHMM!");
		try {
			this.network.removeNode(parent.getName());
			this.parentNames.remove(parent.getName());
			this.killID(((MHMMX)parent).ID);
		} catch(BNException e) {
			throw new CMException("Failed to remove node " + parent.getName() + " : " + e.toString());
		}
	}

	@Override
	protected MHMMX newParentI() throws CMException {
		try {
			int id = this.nextID();
			IFDiscDBNNode newp = network.addDiscreteNode("X"+id, this.ns);
			newp.setInitialDistribution(this.priors.initialSamplePi());
			newp.setAdvanceDistribution(this.priors.initialSampleA());
			this.network.addInterEdge(newp, newp);
			MHMMX parent = new MHMMX(newp, id);
			this.parentNames.add("X"+id);
			if(this.lockMCParams)
				parent.xnd.lockParameters();
			return parent;
		} catch(BNException e) {
			throw new CMException("Failed to add a new node to network!");
		}
	}

	@Override
	protected void setParentI(MHMMChild child, MHMMX parent)
	throws CMException
	{
		try {
			MHMMChild mchild = (MHMMChild)child;
			MHMMX mp = (MHMMX)parent;

			if(parents.containsKey(mchild))
				this.network.removeIntraEdge(parents.get(mchild).xnd.getName(),mchild.hook().getName());
			this.network.addIntraEdge(mp.xnd.getName(), mchild.hook().getName());
			this.parents.put(mchild,mp);
		} catch(BNException e) {
			throw new CMException("Error changing parent for node " + child.getName() + " : " + e.toString());
		}
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

	private HashMap<MHMMChild,MHMMX> parents = new HashMap<MHMMChild, MHMMX>();
	private Vector<String> parentNames = new Vector<String>();

	@Override
	public void optimizeChildParameters(MHMMChild child)
	{
		child.optimize();
	}

	public double runChain(MHMMX proc, int maxit, double conv) throws CMException
	{
		Vector<String> nodes = new Vector<String>();
		nodes.add(proc.xnd.getName());
		Vector<MHMMChild> children = this.getChildren(proc);
		for(MHMMChild child : children)
			nodes.addAll(child.constituentNodeNames());

		try {
			//this.network.run_parallel_block(nodes,maxit,conv);
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
	
	public double learnChain(MHMMX proc, int maxrun, double runconv, int maxlearn, double learnconv) throws CMException
	{
		Vector<String> nodes = new Vector<String>();
		nodes.add(proc.xnd.getName());
		for(MHMMChild child : this.getChildren(proc))
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

	private int ns;
	private HashSet<Integer> usedIDs = new HashSet<Integer>();
	private int minimum_available_id = 0;
	private MHMMParameterPrior priors;

}
