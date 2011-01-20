package complex.featural;

import java.util.Vector;

import complex.featural.ProposalGenerator.Proposal;

import util.MathUtil;

import bn.IDynBayesNode;

public class IBPMixture {
	
	public IBPMixture(ProposalGenerator[] generators, double[] p_gens, double[] p) throws FMMException
	{
		if(generators.length!=p.length)
			throw new FMMException("Provided different number of generators from generator probabilities");
		double sum = 0;
		for(double dub : p)
			sum+=dub;
		if(Math.abs(sum-1) > 1e-12)
			throw new FMMException("Provided generator probability distribution not summing to one!");
		if(Math.abs(p[0]+p[1]+p[2]-1) > 1e-12)
			throw new FMMException("Provided move vs verticle vs horizontal distribution not summing to one!");
		
		this.generators = generators;
		this.generator_probs = p;
		this.main_probs = p;
	}
	
	public static class IBPMModelOptions
	{
		public IBPMModelOptions(ModelController controller, boolean[][] initAssign)
		{
			this.controller = controller;
			this.initialAssignments = initAssign;
		}
		
		public static boolean[][] randomAssignment(int obs, int lat, double p)
		{
			boolean[][] assign = new boolean[obs][lat];
			
			for(int j = 0; j < obs; j++)
			{
				boolean hasAssignment = false;
				for(int i = 0; i < lat; i++)
				{
					if(MathUtil.rand.nextDouble() < p)
					{
						assign[j][i] = true;
						hasAssignment = true;
					}
					else
					{
						assign[j][i] = false;
					}
				}
				if(!hasAssignment)
				{
					int feature = MathUtil.rand.nextInt(lat+1);
					assign[j][feature] = true;
				}
			}
			return assign;
		}
	
		public ModelController controller;
		public boolean optimizeParameters = false;		// Optimize parameters at each time step
		public double alpha = 1;						// Indian Buffet
		public boolean[][] initialAssignments;			// Feature matrix initialization
		public int maxIterations = Integer.MAX_VALUE;   // maximum possible number of iterations
	}
	
	public void learn(IBPMModelOptions opts) throws FMMException
	{
		ModelController cont = opts.controller;
		//int N = opts.initialAssignments.length;  	// The number of obsevation sequences
		int M = opts.initialAssignments[0].length;	// The number of latent processes
	
		for(int i = 0; i < M; i++)
		{
			IDynBayesNode latent = cont.newLatentModel();
			int counter = 0;
			for(IDynBayesNode child : cont.getObservedNodes())
			{
				if(opts.initialAssignments[counter][i])
					cont.connect(latent, child);
				counter++;
			}
		}
	
		double ll = cont.run() + structureLL(cont);
		
		cont.log("Learning run started: Initial Log Likelihood = " + ll);
		
		for(int iteration = 0; iteration < opts.maxIterations; iteration++)
		{
			int choice = MathUtil.discreteSample(this.main_probs);
			if(choice==0)
			{
				Vector<IDynBayesNode> obs = cont.getObservedNodes();
				Vector<IDynBayesNode> lats = cont.getLatentNodes();
				choice = MathUtil.rand.nextInt(obs.size());
				IDynBayesNode hsn = obs.get(choice);
				cont.log("Starting horizontal sample run for node : " + hsn.getName());
			}
			else if(choice==1)
			{
				Vector<IDynBayesNode> obs = cont.getObservedNodes();
				Vector<IDynBayesNode> lats = cont.getLatentNodes();
				choice = MathUtil.rand.nextInt(lats.size());
				IDynBayesNode vsn = lats.get(choice);
				cont.log("Starting vertical sample run for node : " + vsn.getName());
			}
			else
			{
				choice = MathUtil.discreteSample(this.generator_probs);
				Proposal proposal = generators[choice].generate(cont);
				
				if(proposal==null)
					continue;
				
				proposal.action().perform(cont);
				double newLL = cont.run() + structureLL(cont);
				
				double p_accept = Math.exp(ll+Math.log(proposal.forwardP())-newLL-Math.log(proposal.backwardP()));
				if(MathUtil.rand.nextDouble() < p_accept)
					ll = newLL;
				else
					proposal.action().undo(cont);
			}
		}
	}
	
	private double structureLL(ModelController cont)
	{
		return 0;
	}
	
	private ProposalGenerator[] generators;
	private double[] generator_probs;
	private double[] main_probs;
}
