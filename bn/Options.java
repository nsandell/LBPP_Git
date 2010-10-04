package bn;

import bn.IDynBayesNet.ParallelCallback;

public class Options
{
	public static class InferenceOptions
	{
		public InferenceOptions(){this.maxIterations = 20; this.convergence = 1e-5; this.parallel = false;}
		public InferenceOptions(int numIts, double conv){this.maxIterations = numIts; this.convergence = conv; this.parallel = false;}
		
		public int maxIterations;
		public double convergence;
		public boolean parallel;
		public ParallelCallback callback = null;
	}
	
	public static class LearningOptions
	{
		public LearningOptions(int maxIt, double conv){this.maxIterations = maxIt;this.convergence = conv;}
		public LearningOptions(){this.maxIterations = 20;this.convergence = 1e-5;}
		
		public int maxIterations;
		public double convergence;
	}
}
