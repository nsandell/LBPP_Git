package complex.mixture.controllers;

import java.util.Collection;
import java.util.HashSet;
import java.util.Vector;

import bn.BNException;
import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;
import bn.dynamic.IDBNNode;
import bn.dynamic.IDynamicBayesNet;
import bn.dynamic.IFDiscDBNNode;
import bn.messages.FiniteDiscreteMessage;
import complex.CMException;
import complex.featural.IChildProcess;
import complex.featural.IParentProcess;
import complex.mixture.MixtureModelController;

public class MHMMController extends MixtureModelController {
	
	public MHMMController(IDynamicBayesNet network, Vector<MHMMChild> children, MHMMParameterPrior priors, int ns)
	{
		super(children);
		this.network = network;
		this.priors = priors;
		this.ns = ns;
	}
	
	public static interface MHMMChild extends IChildProcess
	{
		IDBNNode hook();
		
		void optimize(Vector<FiniteDiscreteMessage> incPis);
		double evaluateP();
		void sampleInit();
		void samplePosterior();
		Collection<String> constituentNodeNames();
	}
	
	private static class MHMMX implements IParentProcess
	{
		MHMMX(IFDiscDBNNode xnd,int ID)
		{
			this.xnd = xnd;
			this.ID = ID;
		}
		public String getName()
		{
			return this.xnd.getName();
		}
		public FiniteDiscreteMessage marginal(int t)
		{
			try {
				return this.xnd.getMarginal(t);
			} catch(BNException e) {
				System.err.println("Error : " + e.toString());
				return null;
			}
		}
		int ID;
		IFDiscDBNNode xnd;
	}
	
	public static interface MHMMParameterPrior
	{
		public double evaluate(DiscreteCPT A);
		public double evaluate(DiscreteCPTUC pi);
		
		public DiscreteCPT initialSampleA();
		public DiscreteCPTUC initialSamplePi();
		public DiscreteCPT posteriorSampleA(DiscreteCPT A);
		public DiscreteCPTUC posteriorSamplePi(DiscreteCPTUC pi);
	}

	@Override
	protected void deleteParentI(IParentProcess parent) throws CMException
	{
		if(this.getChildren(parent).size()!=0)
			throw new CMException("Cannot have an orphaned observation sequence in an mHMM!");
		try {
			this.network.removeNode(parent.getName());
			this.killID(((MHMMX)parent).ID);
		} catch(BNException e) {
			throw new CMException("Failed to remove node " + parent.getName() + " : " + e.toString());
		}
	}

	@Override
	protected IParentProcess newParentI() throws CMException {
		try {
			int id = this.nextID();
			IFDiscDBNNode newp = network.addDiscreteNode("X"+id, this.ns);
			newp.setInitialDistribution(this.priors.initialSamplePi());
			newp.setAdvanceDistribution(this.priors.initialSampleA());
			this.network.addInterEdge(newp, newp);
			MHMMX parent = new MHMMX(newp, id);
			return parent;
		} catch(BNException e) {
			throw new CMException("Failed to add a new node to network!");
		}
	}

	@Override
	protected void setParentI(IChildProcess child, IParentProcess parent)
			throws CMException
	{
		if(child instanceof MHMMChild && parent instanceof MHMMX)
		{
			try {
				MHMMChild mchild = (MHMMChild)child;
				MHMMX mp = (MHMMX)parent;
				Vector<IDBNNode> parents = new Vector<IDBNNode>();
				for(IDBNNode parentnd : mchild.hook().getIntraParents())
					parents.add(parentnd);
				if(parents.size()>1)
					throw new BNException("Expected child " + child.getName() + " to have one and only one parent!");
				if(parents.size()==1)
					this.network.removeIntraEdge(parents.get(0).getName(), mchild.hook().getName());
				this.network.addIntraEdge(mp.xnd.getName(), mchild.hook().getName());
			} catch(BNException e) {
				throw new CMException("Error changing parent for node " + child.getName() + " : " + e.toString());
			}
		}
		else
			throw new CMException("Attempted to change parent of non-MHMM observation process.");
	}
	
	@Override
	public void optimizeChildParameters(IChildProcess child)
	throws CMException {
		if(child instanceof MHMMChild)
		{
			try {
				MHMMChild mchild = (MHMMChild)child;
				MHMMX parent = (MHMMX)this.getParent(child);
				Vector<FiniteDiscreteMessage> pis = new Vector<FiniteDiscreteMessage>();
				for(int t = 0; t < this.network.getT(); t++)
					pis.add(parent.xnd.getMarginal(t));
				mchild.optimize(pis);
			} catch(BNException e) {
				throw new CMException("Failed to optimize child node...");
			}
		}
		else throw new CMException("Attempted to optimize non-mHMM observation process.");
	}
	
	public double runChain(IParentProcess proc, int maxit, double conv) throws CMException
	{
		if(proc instanceof MHMMX)
		{
			MHMMX procx = (MHMMX)proc;
			
			Vector<String> nodes = new Vector<String>();
			nodes.add(procx.xnd.getName());
			Vector<IChildProcess> children = this.getChildren(proc);
			for(IChildProcess child : children)
			{
				if(child instanceof MHMMChild)
					nodes.addAll(((MHMMChild)child).constituentNodeNames());
				else
					throw new CMException("MHMM model has non MHMM child process...");
			}
			
			try {
				this.network.run_parallel_block(nodes,maxit,conv);
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
		else
			throw new CMException("MHMM model has non MHMM latent process...");
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
