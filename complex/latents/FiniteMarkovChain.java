package complex.latents;

import java.io.PrintStream;

import bn.BNException;
import bn.distributions.DiscreteCPT;
import bn.distributions.DiscreteCPTUC;
import bn.dynamic.IDynamicBayesNet;
import bn.dynamic.IFDiscDBNNode;
import bn.messages.FiniteDiscreteMessage;
import complex.CMException;
import complex.IParentProcess;

public class FiniteMarkovChain extends SingleNodeParent<IFDiscDBNNode> {
	
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
			try {
				return new FiniteMarkovChain(net,name,this.prior,id);
			} catch(BNException e) {
				throw new CMException("Failed to create new finite markov chain latent : " + e.toString());
			}
		}
		
		FiniteMCPrior prior;
	}

	private FiniteMarkovChain(IDynamicBayesNet net, String name, FiniteMCPrior prior, int id) throws CMException, BNException //TODO Find a better fix for this??
	{
		super(net.addDiscreteNode(name, prior.ns), id);
		try {
			this.prior = prior;
			this.nd.setInitialDistribution(prior.initialPi());
			this.nd.setAdvanceDistribution(prior.initialA());
			net.addInterEdge(this.nd, this.nd);
		} catch(BNException e) {
			throw new CMException("Failed to create finite markov chain latent process : " + e.toString());
		}
	}
	
	@Override
	public FiniteDiscreteMessage marginal(int t)
	{
		try {
			return this.nd.getMarginal(t);
		} catch(BNException e) {
			System.err.println("Error : " + e.toString());
			return null;
		}
	}
	
	@Override
	public double parameterLL() {
		return this.prior.parameterLL((DiscreteCPT)this.nd.getAdvanceDistribution(), (DiscreteCPTUC)this.nd.getInitialDistribution());
	}
	
	@Override
	public void printMarginal(PrintStream ps) throws CMException {
		try {
			for(int j = 0; j < this.nd.getCardinality(); j++)
			{
				ps.print(this.nd.getMarginal(0).getValue(j));
				for(int i = 1; i < this.nd.getNetwork().getT(); i++)
				{
					ps.print(" " + this.nd.getMarginal(i).getValue(j));
				}
				ps.println();
			}
		} catch(BNException e) {
			throw new CMException("Error while printing marginal for latent Markov chain: " + e.toString());
		}
	}
	
	public void kill() throws CMException
	{
		try {
			this.nd.getNetwork().removeNode(this.nd);
		} catch(BNException e) {
			throw new CMException("Failure to remove latent parent " + this.getName() + ": " + e.toString());
		}
	}
	
	private FiniteMCPrior prior;
}
