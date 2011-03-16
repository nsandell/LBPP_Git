package complex.mixture;

import java.util.Vector;

import complex.CMException;
import complex.IParentProcess;

import util.MathUtil;

public class FixedMixture
{
	public static class FMModelOptions<ChildProcess extends IMixtureChild, ParentProcess extends IParentProcess>
	{
		public FMModelOptions(MixtureModelController<ChildProcess,ParentProcess> model, int N)
		{
			this.controller = model;
			this.N = N;
		}
		
		public int maxAssignmentIterations = 1000;
		public int maxRunIterations = 10, maxLearnIterations = 10;
		public double runConv = 1e-8, learnConv = 1e-6;
		
		public double totalConvergence = 1e-4;
		
		public String modelBaseName = null;
		
		public MixtureModelController<ChildProcess, ParentProcess> controller;
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
	public static <ChildProcess extends IMixtureChild, ParentProcess extends IParentProcess> 
		void learnFixedMixture(FMModelOptions<ChildProcess,ParentProcess> opts) throws CMException
	{
		Vector<ParentProcess> latentProcs = new Vector<ParentProcess>();
		for(int i = 0; i < opts.N; i++)
			latentProcs.add(opts.controller.newParent());
		
		Vector<ChildProcess> childProcs = opts.controller.getAllChildren();
		
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
		
		opts.controller.trace("Initial Assigments: ");
		for(ParentProcess parent : opts.controller.getAllParents())
		{
			for(ChildProcess child : opts.controller.getChildren(parent))
				opts.controller.trace(parent.getName() + " -> " + child.getName());
		}
		opts.controller.trace("\n");
		
		opts.controller.validate();
		
		double ll = opts.controller.learn(opts.maxLearnIterations,opts.learnConv,opts.maxRunIterations,opts.runConv);
	
		opts.controller.trace("Initial LL : " + ll);
		
		opts.controller.trace("Starting:");
			
		boolean changed = true;
		int iteration = 1;
		while(changed && iteration <= opts.maxAssignmentIterations)
		{
			
			double llprev = ll;
		
			if(opts.modelBaseName!=null)
				opts.controller.printNetwork(opts.modelBaseName+iteration+".lbp");
			
			changed = false;
			
			for(ChildProcess cchild : childProcs)
			{
				ParentProcess originalParent = opts.controller.getParent(cchild);
				ParentProcess bestParent = originalParent;
				cchild.backupParameters();
				double bestLL = ll;
				
				for(ParentProcess parent : latentProcs)
				{
					if(parent==originalParent)
						continue;
					opts.controller.setParent(cchild, parent);
					opts.controller.optimizeChildParameters(cchild);
					double tmp = opts.controller.run(opts.maxRunIterations, opts.runConv);
					if(tmp > bestLL)
					{
						bestParent = parent;
						bestLL = tmp;
					}
				}
				opts.controller.setParent(cchild, bestParent);
				if(bestParent==originalParent)
					cchild.restoreParameters();
				else
					opts.controller.optimizeChildParameters(cchild);
				
				ll = opts.controller.run(opts.maxRunIterations, opts.runConv);
				if(bestParent!=originalParent)
					ll = opts.controller.learn(opts.maxLearnIterations, opts.learnConv, opts.maxRunIterations, opts.runConv);

				if(bestParent!=originalParent)
				{
					opts.controller.trace(bestParent.getName() + " -> " + cchild.getName());
					changed = true;
				}
				else
					opts.controller.trace(cchild.getName() + " (" + originalParent.getName() + ", NO CHANGE )");
			}

			ll = opts.controller.learn(opts.maxLearnIterations, opts.learnConv, opts.maxRunIterations, opts.runConv);
			opts.controller.trace("Iteration " + iteration + " completed with log likelihood : " + ll);
			iteration++;
			if((ll-llprev)/Math.abs(llprev) < opts.totalConvergence)
				break;
		}
		opts.controller.log("\nFinal Assigments: ");
		for(ParentProcess parent : opts.controller.getAllParents())
		{
			for(ChildProcess child : opts.controller.getChildren(parent))
				opts.controller.log(parent.getName() + " -> " + child.getName());
		}
	}
}
