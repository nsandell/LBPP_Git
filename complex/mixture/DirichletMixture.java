package complex.mixture;

import java.util.HashMap;
import java.util.Vector;

import util.MathUtil;

import complex.CMException;
import complex.IParentProcess;

public class DirichletMixture {
	
	public static class DMModelOptions
	{
		public DMModelOptions(MixtureModelController model, double alpha)
		{
			this.controller = model;
			this.alpha = alpha;
		}
		
		public int maxRunIterations = 10, maxLearnIterations = 7;
		public double runConv = 1e-5, learnConv = 1e-4;
		
		public String modelBaseName = null;
		
		public MixtureModelController controller;
		public double alpha;
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
	public static void learnDirichletMixture(DMModelOptions opts) throws CMException
	{
		
		Vector<IMixtureChild> childProcs = opts.controller.getAllChildren();
		int M = opts.controller.getAllChildren().size();
		if(opts.initialAssignment==null)
		{
			opts.initialAssignment = new int[M];
			HashMap<Integer,Integer> numPrev = new HashMap<Integer, Integer>();
			numPrev.put(0, 1);
			opts.initialAssignment[0] = 0;
			for(int i = 1; i < M; i++)
			{
				double [] dist =new double[numPrev.size()+1];
				dist[dist.length-1] = opts.alpha;
				double sum = opts.alpha;
				for(java.util.Map.Entry<Integer,Integer> ent : numPrev.entrySet())
				{
					dist[ent.getKey()] = ent.getValue();
					sum += dist[ent.getKey()];
				}
				for(int j = 0; j < dist.length; j++)
					dist[j] /= sum;
				opts.initialAssignment[i] = MathUtil.discreteSample(dist);
				if(opts.initialAssignment[i]==dist.length-1)
					numPrev.put(opts.initialAssignment[i],1);
				else
					numPrev.put(opts.initialAssignment[i], numPrev.get(opts.initialAssignment[i])+1);
			}
		}
		
		if(opts.initialAssignment.length!=M)
			throw new CMException("Initial assignment matrix is invalidly sized!");
		
		int N = 0;
		for(int i = 0; i < M; i++)
		{
			N = Math.max(N, opts.initialAssignment[i]+1);
			if(opts.initialAssignment[i] < 0)
				throw new CMException("Initial assignment contains invalid assignment!");
		}
		
		Vector<IParentProcess> latentProcs = opts.controller.getAllParents();
		for(int i = 0; i < N; i++)
			opts.controller.newParent();
		
		for(int i = 0; i < M; i++)
			opts.controller.setParent(childProcs.get(i),latentProcs.get(opts.initialAssignment[i]));
		
		opts.controller.trace("Initial Assigments: ");
		for(IParentProcess parent : opts.controller.getAllParents())
		{
			for(IMixtureChild child : opts.controller.getChildren(parent))
				opts.controller.trace(parent.getName() + " -> " + child.getName());
		}
		opts.controller.trace("\n");
		opts.controller.validate();
		
		double ll = opts.controller.learn(opts.maxLearnIterations,opts.learnConv,opts.maxRunIterations,opts.runConv);
		//TODO Parameters!
		ll += llDP(opts);
		
		opts.controller.trace("Initial LL : " + ll);
		opts.controller.trace("Starting:");
			
		int iteration = 1;
		while(iteration <= opts.maxIterations)
		{
			
			if(opts.modelBaseName!=null)
				opts.controller.printNetwork(opts.modelBaseName+iteration+".lbp");
			
			for(IMixtureChild cchild : childProcs)
			{
				System.err.println("Testing " + cchild.getName() + " against:");

				Vector<IParentProcess> subset = new Vector<IParentProcess>(latentProcs);
				subset.remove(opts.controller.getParent(cchild));

				int parenti = MathUtil.rand.nextInt(subset.size()+1);
				if(parenti < subset.size())
				{
					IParentProcess parent = latentProcs.get(parenti);
					IParentProcess currentParent = opts.controller.getParent(cchild);
					if(parent==currentParent)
						continue;
					System.err.println("\t" + parent.getName());

					opts.controller.backupChildrenParameters(currentParent);
					opts.controller.backupChildrenParameters(parent);

					opts.controller.setParent(cchild, parent);
					opts.controller.optimizeChildParameters(cchild);

					opts.controller.learnChain(currentParent,opts.maxRunIterations,opts.runConv,opts.maxLearnIterations,opts.learnConv);
					double llprop = opts.controller.learnChain(parent,opts.maxRunIterations,opts.runConv,opts.maxLearnIterations,opts.learnConv);

					double llprop_struct = llDP(opts);
					double llparam = 0; //TODO get parameter prior ll in here

					if(MathUtil.rand.nextDouble() > Math.exp(llprop + llprop_struct - ll))
					{
						opts.controller.setParent(cchild, currentParent);
						opts.controller.restoreChildrenParameters(currentParent);
						opts.controller.restoreChildrenParameters(parent);
					}
					else
						ll = llprop + llprop_struct + llparam;
				}
				else
				{
					System.err.println("\tNew Parent.");
					IParentProcess currentParent = opts.controller.getParent(cchild);
					opts.controller.backupChildrenParameters(currentParent);

					IParentProcess newParent = opts.controller.newParent();
					opts.controller.setParent(cchild, newParent);

					opts.controller.learnChain(currentParent,opts.maxRunIterations,opts.runConv,opts.maxLearnIterations,opts.learnConv);
					double llprop = opts.controller.learnChain(newParent, opts.maxRunIterations, opts.runConv, opts.maxLearnIterations, opts.learnConv);
					double llprop_struct = llDP(opts);
					double llparam = 0; //TODO get parameter prior ll in here

					if(MathUtil.rand.nextDouble() > Math.exp(llprop + llprop_struct - ll))
					{
						opts.controller.setParent(cchild, currentParent);
						opts.controller.restoreChildrenParameters(currentParent);
						opts.controller.deleteParent(newParent);
					}
					else
						ll = llprop + llprop_struct + llparam;
				}
				
				Vector<IParentProcess> removes = new Vector<IParentProcess>();
				for(IParentProcess proc : opts.controller.getAllParents())
				{
					if(opts.controller.getChildren(proc).size()==0)
						removes.add(proc);
				}
				for(IParentProcess proc : removes)
					opts.controller.deleteParent(proc);

				
				for(IMixtureChild child : childProcs)
					System.out.print(opts.controller.getParent(child).getName() + " " );
				System.out.println();
			}
			opts.controller.trace("Iteration " + iteration + " completed with log likelihood : " + ll);
			iteration++;
		}
		opts.controller.log("\nFinal Assigments: ");
		for(IParentProcess parent : opts.controller.getAllParents())
		{
			for(IMixtureChild child : opts.controller.getChildren(parent))
				opts.controller.log(parent.getName() + " -> " + child.getName());
		}
	}
	
	private static double llDP(DMModelOptions opts)
	{
		double ll = 0;

		for(IParentProcess par : opts.controller.getAllParents())
		{
			for(int i = 2; i < opts.controller.getChildren(par).size(); i++)
				ll += Math.log(i);
		}
		
		ll += (opts.controller.getAllParents().size()-1)*Math.log(opts.alpha);
		for(int i = 1; i < opts.controller.getAllChildren().size(); i++)
			ll -= Math.log(opts.alpha + i);
		
		return ll;
	}
}
