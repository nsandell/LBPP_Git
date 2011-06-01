package complex.latents;

import bn.BNException;
import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;
import bn.distributions.DiscreteDistribution.DiscreteFiniteDistribution;
import bn.dynamic.IDBNNode;
import bn.dynamic.IDynamicBayesNet;
import bn.dynamic.IFDiscDBNNode;
import bn.messages.FiniteDiscreteMessage;
import complex.CMException;
import complex.IParentProcess;

public class FiniteMarkovChain implements IParentProcess {
	
	public static abstract class FiniteMCPrior
	{
		public FiniteMCPrior(int ns) {
			this.ns = ns;
		}
		
		public final int NS()
		{
			return ns;
		}
		
		public abstract DiscreteCPT initialA();
		public abstract DiscreteCPTUC initialPi();
		public abstract double parameterLL(DiscreteCPT A, DiscreteCPTUC pi);
		
		boolean lockedParameters(){return false;}

		protected int ns;
	}
	
	public static class DirichletFiniteMCPrior extends FiniteMCPrior
	{
		public DirichletFiniteMCPrior(double base, double self, int ns) throws CMException
		{
			super(ns);
			throw new CMException("Unimplemented until a suitable Dirichlet distribution implementation is found.");
		}
		
		@Override
		public DiscreteCPT initialA() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public DiscreteCPTUC initialPi() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public double parameterLL(DiscreteCPT A, DiscreteCPTUC pi) {
			// TODO Auto-generated method stub
			return 0;
		}
	}
	
	public static class UniformFiniteMCPrior extends FiniteMCPrior
	{
		public UniformFiniteMCPrior(DiscreteCPT A, DiscreteCPTUC pi, int ns) throws CMException
		{
			super(ns);
			if(A.getConditionDimensions().length!=1 || A.getConditionDimensions()[0]!=ns || A.getCardinality()!=ns)
				throw new CMException("Attempted to specify improper A Matrix for Dirac Finite MC Prior.");
			if(pi.getCardinality()!=ns)
				throw new CMException("Attempted to specify improper pi vector for Dirac Finite MC Prior.");
			
			this.A = A;
			this.pi = pi;
		}
		
		@Override
		public DiscreteCPT initialA() {
			return this.A;
		}

		@Override
		public DiscreteCPTUC initialPi() {
			return this.pi;
		}

		@Override
		public double parameterLL(DiscreteCPT A, DiscreteCPTUC pi) {
			return 0.0; 
			// TODO Verify this is what we want to do... A could technically be 
			// different from the desired value and thus be 0 but this would
			// require a comparison that seems unecessary.
		}
		
		private DiscreteCPT A;
		private DiscreteCPTUC pi;
	}
	
	public static class DiracFiniteMCPrior extends UniformFiniteMCPrior
	{
		public DiracFiniteMCPrior(DiscreteCPT initA, DiscreteCPTUC initPi, int ns) throws CMException
		{
			super(initA,initPi,ns);
		}
		
		@Override
		boolean lockedParameters()
		{
			return true;
		}
	}
	
	public static class FiniteMarkovChainFactory implements LatentFactory
	{
		public FiniteMarkovChainFactory(FiniteMCPrior prior)
		{
			this.prior = prior;
		}
		
		@Override
		public IParentProcess newLatent(String name, int id, IDynamicBayesNet net) throws CMException {
			return new FiniteMarkovChain(net,name,this.prior,id);
		}
		
		FiniteMCPrior prior;
	}

	FiniteMarkovChain(IDynamicBayesNet net, String name, FiniteMCPrior prior, int id) throws CMException
	{
		try {
			this.id = id;
			this.xnd = net.addDiscreteNode(name, prior.ns);
			this.prior = prior;
			this.xnd.setInitialDistribution(prior.initialPi());
			this.xnd.setAdvanceDistribution(prior.initialA());
			net.addInterEdge(this.xnd, this.xnd);
		} catch(BNException e) {
			throw new CMException("Failed to create finite markov chain latent process : " + e.toString());
		}
	}
	int id;//TODO push ID up an inheritance level
	
	public int id()
	{
		return this.id;
	}
	
	@Override
	public String getName()
	{
		return this.xnd.getName();
	}
	
	@Override
	public FiniteDiscreteMessage marginal(int t)
	{
		try {
			return this.xnd.getMarginal(t);
		} catch(BNException e) {
			System.err.println("Error : " + e.toString());
			return null;
		}
	}
	
	@Override
	public void backupParameters() {
		if(!this.xnd.isLocked())
		{
			this.Abackup = xnd.getAdvanceDistribution();
			this.pibackup = xnd.getInitialDistribution();
		}
	}
	
	@Override
	public void restoreParameters() {
		try {
		if(!this.xnd.isLocked() && this.Abackup != null && this.pibackup !=null)
		{
			this.xnd.setAdvanceDistribution(this.Abackup);
			this.xnd.setInitialDistribution(this.pibackup);
		}
		} catch(BNException e) {
			System.err.println("Failed to restore parameters for node " + this.getName());
		}
	}
	
	public IDBNNode hook() {
		return this.xnd;
	}
	
	@Override
	public double parameterLL() {
		return this.prior.parameterLL((DiscreteCPT)this.xnd.getAdvanceDistribution(), (DiscreteCPTUC)this.xnd.getInitialDistribution());
	}
	
	private IFDiscDBNNode xnd;
	private DiscreteFiniteDistribution Abackup = null, pibackup = null;
	private FiniteMCPrior prior;
}
