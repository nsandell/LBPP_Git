package complex.featural;

import java.util.HashSet;
import java.util.Vector;

import complex.featural.ProposalAction.UniqueParentAddAction;
import complex.featural.ProposalGenerator.Proposal;

import util.MathUtil;

public class IBPMixture {
	
	public IBPMixture(ProposalGenerator[] generators, double[] p_gens, double[] p) throws FMMException
	{
		if(generators.length!=p_gens.length)
			throw new FMMException("Provided different number of generators from generator probabilities");
		double sum = 0;
		for(double dub : p_gens)
			sum+=dub;
		if(Math.abs(sum-1) > 1e-12)
			throw new FMMException("Provided generator probability distribution not summing to one!");
		if(Math.abs(p[0]+p[1]+p[2]-1) > 1e-12)
			throw new FMMException("Provided move vs verticle vs horizontal distribution not summing to one!");
		
		this.accepted_genprops = new int[generators.length];
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
					int feature = MathUtil.rand.nextInt(lat);
					assign[j][feature] = true;
				}
			}
			return assign;
		}
		
		public String savePath = null;
		public int numSave = 0;
		public double mll = Double.NEGATIVE_INFINITY;
	
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
		
		double bestLL = Double.NEGATIVE_INFINITY;
		
		
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
		
		cont.validate();
		
		double ll = cont.run(opts.max_run_it,opts.run_conv) + structureLL(cont,opts);
		
		cont.log("Learning run started: Initial Log Likelihood = " + ll);
		
		for(int iteration = 0; iteration < opts.maxIterations; iteration++)
		{
			int choice = MathUtil.discreteSample(this.main_probs);
			if(choice==0)
			{
				choice = MathUtil.rand.nextInt(obs.size());
				IChildProcess hsn = obs.get(choice);
				cont.log("Starting horizontal sample run for node : " + hsn.getName());
				
				HashSet<IParentProcess> parents = cont.getParents(hsn);
				
				for(IParentProcess latNd : lats)
				{
					if(parents.contains(latNd))
						ll = this.attemptDisconnect(latNd, hsn, cont, opts, ll);
					else
						ll = this.attemptConnect(latNd, hsn, cont, opts, ll);
				}
				
				//Attempt to add unique parent
				cont.log("Attempting to add unique parent to node " + hsn.getName());
				UniqueParentAddAction act = new UniqueParentAddAction(hsn);
				act.perform(cont);
				double newLL = cont.run(opts.max_run_it, opts.run_conv) + structureLL(cont,opts);
				if(accept(newLL,this.main_probs[0],ll,this.main_probs[0]+this.main_probs[1],cont))
					ll = newLL;
				else
					act.undo(cont);
			}
			else if(choice==1)
			{
				choice = MathUtil.rand.nextInt(lats.size());
				IParentProcess vsn = lats.get(choice);
				cont.log("Starting vertical sample run for node : " + vsn.getName());

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
				
				this.accepted_genprops[choice]++;
				
				if(proposal==null)
					continue;
				
				proposal.action().perform(cont);
				double newLL = cont.run(opts.max_run_it,opts.run_conv) + structureLL(cont,opts);
				
				if(accept(newLL,proposal.forwardP(),ll,proposal.backwardP(),cont))
					ll = newLL;
				else
					proposal.action().undo(cont);
			}
			
			for(int i = 0; i < lats.size(); i++)
			{
				if(cont.getChildren(lats.get(i)).size()==0)
					cont.killLatentModel(lats.get(i));
			}
	
			if(opts.savePath!=null)
			{
				if(ll > bestLL)
				{
					bestLL = ll;
					cont.saveInfo(opts.savePath + "/iteration"+iteration);
				}
			}
		}
		
		System.out.println("Accepted moves:");
		for(int i = 0; i < this.generators.length; i++)
		{
			System.out.println("\t"+this.generators[i].name()+ " : " + this.accepted_genprops[i]);
		}
		System.out.println("Final LL : " + ll);

		for(int i = 0; i < obs.size(); i++)
		{
			for(int j = 0; j < lats.size(); j++)
			{
				if(cont.getChildren(lats.get(j)).contains(obs.get(i)))
					System.out.print("1\t");
				else
					System.out.print("0\t");
			}
			System.out.println();
		}
	}
	
	private double attemptDisconnect(IParentProcess latent, IChildProcess observed, ModelController cont, IBPMModelOptions opts, double ll) throws FMMException
	{
		if(!cont.getChildren(latent).contains(observed))
			throw new FMMException("Attempted to disconnect a latent node from an observed that was not its child!");
		if(cont.getParents(observed).size()==1)
			return ll;
		cont.log("Attempting to disconnect observation " + observed.getName() + " from latent sequence " + latent.getName());
		cont.disconnect(latent, observed);
		double newLL = cont.run(opts.max_run_it,opts.run_conv) + structureLL(cont,opts);
		if(accept(newLL,this.main_probs[1]+this.main_probs[0],ll,this.main_probs[1]+this.main_probs[0],cont))
			ll = newLL;
		else
			cont.connect(latent, observed);
		
		return ll;
	}
	
	private double attemptConnect(IParentProcess latent, IChildProcess observed, ModelController cont, IBPMModelOptions opts, double ll) throws FMMException
	{
		if(cont.getChildren(latent).contains(opts))
			throw new FMMException("Attempted to connect parent " + latent.getName() + " to child " + observed.getName() + " when they already are connected.");
		cont.log("Attempting to connect observation " + observed.getName() + " to latent sequence " + latent.getName());
		cont.connect(latent, observed);
		double newLL = cont.run(opts.max_run_it,opts.run_conv) + structureLL(cont,opts);
		if(accept(newLL,this.main_probs[1]+this.main_probs[0],ll,this.main_probs[1]+this.main_probs[0],cont))
			ll = newLL;
		else
			cont.disconnect(latent, observed);
		
		return ll;
	}
	
	private boolean accept(double newLL, double pf, double oldLL, double pb, ModelController cont) throws FMMException
	{
		boolean ret = (MathUtil.rand.nextDouble() < Math.exp(newLL+Math.log(pb)-oldLL-Math.log(pf)));
		if(ret)
			cont.log("Proposal accepted, new Log Likelihood : " + newLL);
		else
			cont.log("Proposal rejected.");
		return ret;
	}
	
	private double structureLL(ModelController cont, IBPMModelOptions opts)
	{
		double lladj = -opts.alpha*cont.latents.size()*Math.log(opts.alpha);
		for(int i = 1; i <= cont.latents.size(); i++)
			lladj -= Math.log(i);
		for(IParentProcess parent : cont.latents)
		{
			if(cont.getChildren(parent).size() > 1)
			{
				for(int i = 1; i < cont.getChildren(parent).size(); i++)
					lladj += Math.log(i);
				for(int i = 1; i <= cont.observables.size()-cont.getChildren(parent).size(); i++)
					lladj += Math.log(i);
				for(int i = 2; i <= cont.observables.size(); i++)
					lladj -= Math.log(i);
			}
		}
		if(lladj > 0)
			System.err.println("Whaaa");
		return lladj;
	}
	
	private ProposalGenerator[] generators;
	private double[] generator_probs;
	private int[] accepted_genprops;
	private double[] main_probs;
}
