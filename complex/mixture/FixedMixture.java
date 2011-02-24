package complex.mixture;

import java.util.Vector;

import complex.CMException;
import complex.featural.IChildProcess;
import complex.featural.IParentProcess;

import util.MathUtil;

public class FixedMixture
{
	public static class FMModelOptions
	{
		public FMModelOptions(MixtureModelController model, int N)
		{
			this.controller = model;
			this.N = N;
		}
		
		public int maxRunIterations = 10, maxLearnIterations = 10;
		public double runConv = 1e-8, learnConv = 1e-6;
		
		public MixtureModelController controller;
		public boolean optimizeParameters = true;		// Optimize parameters at each timestep
		public int N;									// Number of latent processes
		public int[] initialAssignment;					// Initial assignment matrix (optional)
		public int maxIterations = Integer.MAX_VALUE;   // maximum possible number of iterations
	}
	
	/**
	 * Learn a dynamic mixture model using a fixed number of underlying processes.
	 * Each observation sequence will wind up being assigned to one and only one
	 * of these processes.  
	 * @param network The network to use.  This can be fragmented over multiple
	 * machines or a monolithic one.
	 * @param obsConnectors Vector of nodes that will be used to connect the latent 
	 * processes to the observed processes.
	 */
	public static void learnFixedMixture(FMModelOptions opts) throws CMException
	{
		Vector<IParentProcess> latentProcs = new Vector<IParentProcess>();
		for(int i = 0; i < opts.N; i++)
			latentProcs.add(opts.controller.newParent());
		
		Vector<IChildProcess> childProcs = opts.controller.getAllChildren();
		
		int N = opts.N;
		int M = opts.controller.getAllChildren().size();
		
		if(opts.initialAssignment!=null)
		{
			if(opts.initialAssignment.length!=M)
				throw new CMException("Initial assignment matrix is invalidly sized!");
			for(int i = 0; i < M; i++)
			{
				if(opts.initialAssignment[i] >= N || opts.initialAssignment[i] < 0)
					throw new CMException("Initial assignment contains invalid assignment!");
				opts.controller.setParent(childProcs.get(i),latentProcs.get(opts.initialAssignment[i]));
			}
		}
		else
		{
			for(int i = 0; i < M; i++)
				opts.controller.setParent(childProcs.get(i), latentProcs.get(MathUtil.rand.nextInt(N)));
		}
		
		opts.controller.log("Initial Assigments: ");
		for(IParentProcess parent : opts.controller.getAllParents())
		{
			for(IChildProcess child : opts.controller.getChildren(parent))
				opts.controller.log(parent.getName() + " -> " + child.getName());
		}
		opts.controller.log("\n");
		
		opts.controller.validate();
		
		double ll;
		if(opts.optimizeParameters)
			ll = opts.controller.learn(opts.maxLearnIterations,opts.learnConv,opts.maxRunIterations,opts.runConv);
		else
			ll = opts.controller.run(opts.maxRunIterations,opts.runConv);
	
		
		opts.controller.log("Starting:");
			
		boolean changed = true;
		int iteration = 1;
		while(changed)
		{
			changed = false;
			
			for(int i = 0; i < M; i++)
			{
				IChildProcess currentC = childProcs.get(i);
				IParentProcess currentP = opts.controller.getParent(currentC);
				int ci = -1;
				double maxnewll = Double.NEGATIVE_INFINITY;
				int maxnewlli = -1;
				for(int j = 0; j < N; j++)
				{
					IParentProcess newP = latentProcs.get(j);
					if(newP==currentP)
					{
						ci = j;
						if(ll > maxnewll)
						{
							maxnewll = ll;
							maxnewlli = j;
						}
					}
					else
					{
						opts.controller.setParent(childProcs.get(i), newP);
						double tmp;
						if(opts.controller.getChildren(newP).size()==1)
						{
							tmp = opts.controller.learn(opts.maxLearnIterations, opts.learnConv, opts.maxRunIterations, opts.runConv);
						}
						else
						{
							opts.controller.optimizeChildParameters(currentC);
							tmp = opts.controller.run(opts.maxRunIterations, opts.runConv);
						}
						if(tmp > maxnewll)
						{
							maxnewll = tmp;
							maxnewlli = j;
						}
					}
				}
				opts.controller.setParent(currentC, latentProcs.get(maxnewlli));
				if(maxnewlli!=ci)
				{
					opts.controller.log(latentProcs.get(maxnewlli).getName() + " -> " + currentC.getName());
					changed = true;
				}
				else
					opts.controller.log(currentC.getName() + " (" + opts.controller.getParent(currentC).getName() + ", NO CHANGE )");
				opts.controller.optimizeChildParameters(currentC);
				ll = opts.controller.learn(opts.maxLearnIterations, opts.learnConv, opts.maxRunIterations, opts.runConv);
			}
			ll = opts.controller.learn(opts.maxLearnIterations, opts.learnConv, opts.maxRunIterations, opts.runConv);
			opts.controller.log("Iteration " + iteration + " completed with log likelihood : " + ll);
			iteration++;
		}
		opts.controller.log("\nFinal Assigments: ");
		for(IParentProcess parent : opts.controller.getAllParents())
		{
			for(IChildProcess child : opts.controller.getChildren(parent))
				opts.controller.log(parent.getName() + " -> " + child.getName());
		}
	}
}
