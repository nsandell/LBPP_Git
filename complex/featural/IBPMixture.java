package complex.featural;

import java.util.HashSet;
import java.util.Vector;

import complex.featural.ProposalGenerator.Proposal;

import util.MathUtil;

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
	
		public int max_run_it = 30, max_learn_it = 5;
		public double run_conv = 1e-6, learn_conv = 1e-5;
		
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
		
		
		Vector<IChildProcess> obs = cont.getObservedNodes();
		Vector<IParentProcess> lats = cont.getLatentNodes();
	
		for(int i = 0; i < M; i++)
		{
			IParentProcess latent = cont.newLatentModel();
			int counter = 0;
			for(IChildProcess child : cont.getObservedNodes())
			{
				if(opts.initialAssignments[counter][i])
					cont.connect(latent, child);
				counter++;
			}
		}
	
		double ll = cont.run(opts.max_run_it,opts.run_conv) + structureLL(cont);
		
		cont.log("Learning run started: Initial Log Likelihood = " + ll);
		
		for(int iteration = 0; iteration < opts.maxIterations; iteration++)
		{
			int choice = MathUtil.discreteSample(this.main_probs);
			if(choice==0)
			{
				choice = MathUtil.rand.nextInt(obs.size());
				IChildProcess hsn = obs.get(choice);
				//TODO FIX cont.log("Starting horizontal sample run for node : " + hsn.getName());
				
				HashSet<IParentProcess> parents = cont.getParents(hsn);
				
				for(IParentProcess latNd : lats)
				{
					if(parents.contains(latNd))
						ll = this.attemptDisconnect(latNd, hsn, cont, opts, ll);
					else
						ll = this.attemptConnect(latNd, hsn, cont, opts, ll);
				}
			}
			else if(choice==1)
			{
				choice = MathUtil.rand.nextInt(lats.size());
				IParentProcess vsn = lats.get(choice);
				//TODO FIX cont.log("Starting vertical sample run for node : " + vsn.getName());

				HashSet<IChildProcess> children = cont.getChildren(vsn);

				for(IChildProcess obsNode : obs)
				{
					if(children.contains(obsNode))
						ll = this.attemptDisconnect(vsn, obsNode, cont, opts, ll);
					else
						ll = this.attemptConnect(vsn, obsNode, cont, opts, ll);
				}
				
			}
			else
			{
				choice = MathUtil.discreteSample(this.generator_probs);
				Proposal proposal = generators[choice].generate(cont);
				
				if(proposal==null)
					continue;
				
				proposal.action().perform(cont);
				double newLL = cont.run(opts.max_run_it,opts.run_conv) + structureLL(cont);
				
				if(accept(newLL,proposal.forwardP(),ll,proposal.backwardP(),cont))
					ll = newLL;
				else
					proposal.action().undo(cont);
			}
		}
	}
	
	private double attemptDisconnect(IParentProcess latent, IChildProcess observed, ModelController cont, IBPMModelOptions opts, double ll) throws FMMException
	{
		//TODO FIX cont.log("Attempting to disconnect observation " + observed.getName() + " from latent sequence " + latent.getName());
		cont.disconnect(latent, observed);
		double newLL = cont.run(opts.max_run_it,opts.run_conv) + structureLL(cont);
		if(accept(newLL,this.main_probs[1]+this.main_probs[0],ll,this.main_probs[1]+this.main_probs[0],cont))
			ll = newLL;
		else
			cont.connect(latent, observed);
		
		return ll;
	}
	
	private double attemptConnect(IParentProcess latent, IChildProcess observed, ModelController cont, IBPMModelOptions opts, double ll) throws FMMException
	{
		//TODO FIX cont.log("Attempting to connect observation " + observed.getName() + " to latent sequence " + latent.getName());
		cont.connect(latent, observed);
		double newLL = cont.run(opts.max_run_it,opts.run_conv) + structureLL(cont);
		if(accept(newLL,this.main_probs[1]+this.main_probs[0],ll,this.main_probs[1]+this.main_probs[0],cont))
			ll = newLL;
		else
			cont.disconnect(latent, observed);
		
		return ll;
	}
	
	private boolean accept(double newLL, double pf, double oldLL, double pb, ModelController cont)
	{
		boolean ret = (MathUtil.rand.nextDouble() < Math.exp(newLL+Math.log(pb)-oldLL-Math.log(pf)));
		if(ret)
			cont.log("Proposal accepted.");
		else
			cont.log("Proposal rejected.");
		return ret;
	}
	
	private double structureLL(ModelController cont)
	{
		return 0;
	}
	
	private ProposalGenerator[] generators;
	private double[] generator_probs;
	private double[] main_probs;
}
