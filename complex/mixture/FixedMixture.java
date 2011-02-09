package complex.mixture;

import java.util.Vector;

import util.MathUtil;

import bn.BNException;
import bn.dynamic.IDynNet;
import bn.dynamic.IDynNode;

public class FixedMixture
{
	public static class FMMException extends BNException
	{
		public FMMException(String cause)
		{
			super(cause);
		}
		private static final long serialVersionUID = 1L;
	}
	
	public static interface ModelController {
		public IDynNode newLatentModel(IDynNet network);
		public void connect(IDynNet network, IDynNode latent, IDynNode observed) throws FMMException;
		public void disconnect(IDynNet network, IDynNode latent, IDynNode observed) throws FMMException;
		public void saveInfo(IDynNet network, Vector<IDynNode> latents, Vector<IDynNode> observeds, double ll);
	}

	public static class FMModelOptions
	{
		public FMModelOptions(int N)
		{
			this.N = N;
		}
		
		public ModelController controller;
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
	static void learnFixedMixture(IDynNet network,Vector<IDynNode> obsConnectors, FMModelOptions opts) throws FMMException
	{
		Vector<IDynNode> latentProcs = new Vector<IDynNode>();
		for(int i = 0; i < opts.N; i++)
			latentProcs.add(opts.controller.newLatentModel(network));
		
		if(opts.initialAssignment!=null)
		{
			if(opts.initialAssignment.length!=obsConnectors.size())
				throw new FMMException("Initial assignment matrix is invalidly sized!");
			for(int i = 0; i < obsConnectors.size(); i++)
			{
				if(opts.initialAssignment[i] >= opts.N || opts.initialAssignment[i] < 0)
					throw new FMMException("Initial assignment contains invalid assignment!");
				opts.controller.connect(network, latentProcs.get(opts.initialAssignment[i]), obsConnectors.get(i));
			}
		}
		else
		{
			for(int i = 0; i < obsConnectors.size(); i++)
				opts.controller.connect(network, latentProcs.get(MathUtil.rand.nextInt(opts.N+1)), obsConnectors.get(obsConnectors.size()+1));
		}
		
		//int it = 0;
		//double ll = Double.NEGATIVE_INFINITY;
			
	}
}
