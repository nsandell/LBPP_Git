package bn.impl;


import java.io.PrintStream;
import java.util.HashMap;

import bn.BNException;
import bn.IBayesNode;
import bn.IDiscreteDynBayesNode;
import bn.IDynBayesNet;
import bn.distributions.Distribution;
import bn.messages.Message;

class DynamicBayesianNetwork extends BayesianNetwork<DBNNode> implements IDynBayesNet
{
	public DynamicBayesianNetwork(int T){this.T = T;}

	public IDiscreteDynBayesNode addDiscreteNode(String name, int cardinality) throws BNException
	{
		if(this.getNode(name)!=null)
			throw new BNException("Node " + name + " already exists in this DBN.");
		DBNNode.DiscreteDBNNode nd = new DBNNode.DiscreteDBNNode(this, name, cardinality);
		this.dnodes.put(name, nd);
		this.addNodeI(nd);
		return nd;
	}
	
	@Override
	public void print(PrintStream pr)
	{
		pr.println(this.T); pr.println();

		super.print(pr);
		
		for(DBNNode node : this.dnodes.values())
		{
			for(DBNNode child : node.getInterChildren())
				pr.println(node.getName() + "=>" + child.getName());
			
			for(DBNNode child : node.getIntraChildren())
				pr.println(node.getName() + "->" + child.getName());
		}
	}

	public void addInterEdge(String fromname, String toname) throws BNException
	{
		DBNNode from = this.getNode(fromname), to = this.getNode(toname);
		if(from==null || to==null)
			throw new BNException("Failed to add interconnection, either (or both) node " + from + " or node " + to + " doesn't exist.");
		try
		{
			from.addInterChild(to);
		} catch(BNException e) {throw new BNException("Whilst interconnecting "+from+"=>"+to+":",e);}
	}

	public void addInterEdge(IBayesNode from, IBayesNode to) throws BNException
	{
		if(from==null || to==null)
			throw new BNException("Null argument passed to addInterEdge...");
		this.addInterEdge(from.getName(), to.getName());
	}

	public void addIntraEdge(String fromname, String toname) throws BNException
	{		
		DBNNode from = this.getNode(fromname), to = this.getNode(toname);
		if(from==null || to==null)
			throw new BNException("Failed to add intraconnection, either (or both) node " + from + " or node " + to + " doesn't exist.");
		try
		{
			from.addIntraChild(to);
		} catch(BNException e) {throw new BNException("Whilst intraconnecting "+from+"=>"+to+":",e);}
	}
	
	public Message getMarginal(String name, int t) throws BNException
	{
		DBNNode node = this.getNode(name);
		if(node==null) throw new BNException("Attempted to get marginal for nonexistant node : " + name);
		return node.getMarginal(t);
	}

	public void addIntraEdge(IBayesNode from, IBayesNode to) throws BNException
	{
		if(from==null || to==null)
			throw new BNException("Null argument passed to addIntraEdge...");
		this.addIntraEdge(from.getName(), to.getName());
	}

	public int getT()
	{
		return this.T;
	}
	
	public RunResults optimize_parallel(int maxLearnIt, double learnErrConvergence, int maxInfIt, double infErrConvergence) throws BNException
	{
		long startTime = System.currentTimeMillis();
		int i = 0;
		double learnErr = 0;
		while(i < maxLearnIt)
		{
			learnErr = 0;
			this.run_parallel_block(maxInfIt, infErrConvergence);
			for(DBNNode node : this.dnodes.values())
				learnErr = Math.max(node.optimizeParameters(),learnErr);
			if(learnErr < learnErrConvergence)
				break;
			i++;
		}
		return new RunResults(i, ((double)(System.currentTimeMillis()-startTime))/1000.0, learnErr);
	}

	@Override
	protected void removeNodeI(DBNNode node) throws BNException
	{
		node.removeAllChildren();
		node.removeAllParents();
		this.dnodes.remove(node.getName());
	}
	
	public void setDistribution(String nodeName, Distribution dist) throws BNException
	{
		DBNNode node = this.getNode(nodeName);
		node.setAdvanceDistribution(dist);
	}
	
	public void setInitialDistribution(String nodeName, Distribution dist) throws BNException
	{	
		DBNNode node = this.getNode(nodeName);
		node.setInitialDistribution(dist);
	}
	
	static class BlockCallback2 implements ParallelCallback
	{
		public void callback(IDynBayesNet net, int numIts, double err, double time) {
			synchronized (blockLock) {
				this.timeElapsed = time;
				this.errorD = err;
				this.numIts = numIts;
				this.blockLock.notify();
			}
		}

		public void error(IDynBayesNet net, String error)
		{
			synchronized (blockLock) {
				this.error = error;
				this.blockLock.notify();
			}
		}
	
		double timeElapsed, errorD;
		int numIts;
		String error = null;
		Object blockLock = new Object();
	}
	
	public RunResults run_parallel_block(int maxit, double conv) throws BNException
	{
		BlockCallback2 cb = new BlockCallback2();
		synchronized (cb.blockLock) {
			this.run_parallel(maxit, conv, cb);
			try{cb.blockLock.wait();}catch(InterruptedException e){System.err.println("Interrupted..");}
		}
		if(cb.error!=null)
			throw new BNException(cb.error);
		return new RunResults(cb.numIts, cb.timeElapsed, cb.errorD);
		//System.out.println("Parellel inference has converged after " + elapsed_seconds + " seconds.");
	}

	public void run_parallel(int maxIt, double conv, ParallelCallback callback)
	{
		/* Must reserve "boundary" nodes for single thread operation to avoid updating
		 *  neighboring nodes simultaneously..
		 *  Say N is the number of processors.  There are N-1 slices to reserve for
		 *  final updating.  That leaves TNB=T-(N-1) slices to be divvied up among the processors.
		 *  For simplicity, all but the last slice will have floor(TNB/N) slices, and the last slice
		 *  will take the remainder
		 */
		ParallelStatus status = new ParallelStatus(this,conv,maxIt,callback,this.getNodes());
		status.start_time = System.currentTimeMillis();
		parallel_iteration_regions(status);
	}
	
	private void parallel_iteration_regions(ParallelStatus status)
	{
		double nnb = this.T - (status.maxThreads -1);
		int numEarlySlice = (int)Math.floor(nnb/status.maxThreads);
		int numLastSlice = ((int)nnb) - numEarlySlice*(status.maxThreads-1);
		
		for(int i = 0; i < status.maxThreads-1; i++)
		{
			SliceRangeSample thread = new SliceRangeSample(status, status.nodes, i*numEarlySlice+i, (i+1)*numEarlySlice+i-1);
			thread.start();
		}
		SliceRangeSample thread = new SliceRangeSample(status, status.nodes, this.T-numLastSlice, this.T-1);
		thread.start();
	}
	

	
	private void parallel_iteration_borders(ParallelStatus status)
	{
		double nnb = this.T - (status.maxThreads -1);
		int numEarlySlice = (int)Math.floor(nnb/status.maxThreads);
		
		try
		{
			for(int i = 0; i < status.maxThreads-1; i++)
			{
				int border = i+ (i+1)*numEarlySlice;
				
				for(DBNNode node : status.nodes)
					node.updateMessages(border, border,status.forward);
			}
		} catch(BNException e)
		{
			status.ok = false;
			status.message = e.toString();
		}
	}
	
	public void setEvidence(String nodename, int t0, Object[] obs) throws BNException
	{
		DBNNode node = this.getNode(nodename);
		node.setEvidence(t0, obs);
	}
	
	public void setEvidence(String nodename, int t, Object obs) throws BNException
	{
		DBNNode node = this.getNode(nodename);
		node.setEvidence(t, obs);
	}
	
	private static class ParallelStatus
	{
		
		public ParallelStatus(DynamicBayesianNetwork bn, double tolerance, int maxIterations, ParallelCallback callback, Iterable<DBNNode> nodes)
		{
			this.bn = bn;
			this.tolerance = tolerance;
			this.maxIterations = maxIterations;
			this.callback = callback;
			this.nodes = nodes;
		}
		
		public double getError()
		{
			return this.maxError;
		}
		
		public void setIfErrorBigger(double err)
		{
			synchronized (errorLock) {
				this.maxError = Math.max(this.maxError, err);
			}
		}
		
		public void reset()
		{
			synchronized (errorLock) {
				this.maxError = 0;
			}
			this.doneThreads = 0;
		}
		
		public void threadStart()
		{
			synchronized (threadsLock) {
				this.threadsRunning++;
			}
		}
		
		public void threadStop()
		{
			if(ok)
			{	
				synchronized (threadsLock) {
					this.threadsRunning--;
					this.doneThreads++;
					if(this.doneThreads==maxThreads)
					{
						this.doneThreads = 0;
						this.bn.parallel_iteration_borders(this);
						this.iteration++;
						
						if(this.iteration!=this.maxIterations && this.getError() > this.tolerance)
						{
							this.reset();
							this.forward = !this.forward;
							this.bn.parallel_iteration_regions(this);
						}
						else
						{
							this.callback.callback(bn,this.iteration,this.getError(),((double)(System.currentTimeMillis()-this.start_time))/1000.0);
						}
					}
				}
			}
			else
				this.callback.error(bn,this.message);
		}
		
		long start_time;
		public boolean forward = true;
		public boolean ok = true;
		public String message;
		public int iteration = 0;
		public int maxIterations;
		public int maxThreads = availableProcs;
		public Iterable<DBNNode> nodes;
		private ParallelCallback callback;
		private int doneThreads;
		private double tolerance;
		private DynamicBayesianNetwork bn;
		private Object threadsLock = new Object();
		private int threadsRunning = 0;
		private Object errorLock = new Object();
		private double maxError = 0;
	}
	
	private static class SliceRangeSample extends Thread
	{
		public SliceRangeSample(ParallelStatus status, Iterable<DBNNode> nodes, int tmin, int tmax)
		{
			this.nodes = nodes; this.tmin = tmin; this.tmax = tmax;  this.status = status;
		}
		
		public void run()
		{
			status.threadStart();
			try
			{
				double maxerr = 0;
				for(DBNNode node : this.nodes)
					maxerr = Math.max(maxerr,node.updateMessages(this.tmin, this.tmax,this.status.forward));
				status.setIfErrorBigger(maxerr);
			} catch(BNException e) {
				status.ok = false;
				status.message = "BP failure between " + tmin + " and " + tmax + " : " + e.toString();
				System.err.println(status.message);
			}
			status.threadStop();
		}
		
		private ParallelStatus status;
		private int tmin, tmax;
		private Iterable<DBNNode> nodes;
	}
	
	@Override
	public void removeInterEdge(String from, String to) throws BNException {
		DBNNode fromN = this.getNode(from);
		DBNNode toN = this.getNode(to);
		
		if(fromN==null || toN==null)
			throw new BNException("Failed to remove edge, node " + from + " or " + to + " doesn't exist.");
		
		fromN.removeInterChild(toN);
	}

	@Override
	public void removeIntraEdge(String from, String to) throws BNException {
		DBNNode fromN = this.getNode(from);
		DBNNode toN = this.getNode(to);
		
		if(fromN==null || toN==null)
			throw new BNException("Failed to remove edge, node " + from + " or " + to + " doesn't exist.");
		
		fromN.removeIntraChild(toN);
	}

	@Override
	public void removeInterEdge(IBayesNode from, IBayesNode to)
			throws BNException {
		this.removeInterEdge(from.getName(), to.getName());
	}

	@Override
	public void removeIntraEdge(IBayesNode from, IBayesNode to)
			throws BNException {
		this.removeIntraEdge(from.getName(), to.getName());
	}
	
	@Override
	public boolean existsInterEdge(String fromName, String toName)
			throws BNException {
		DBNNode fromN = this.getNode(fromName);
		DBNNode toN = this.getNode(toName);
		if(fromN==null || toN==null)
			throw new BNException("Failure removing interedge : Either node " + fromName + " or node " + toName + " does not exist.");
		return fromN.hasInterChild(toN);
	}

	@Override
	public boolean existsIntraEdge(String fromName, String toName)
			throws BNException {
		DBNNode fromN = this.getNode(fromName);
		DBNNode toN = this.getNode(toName);
		if(fromN==null || toN==null)
			throw new BNException("Failure removing intraedge : Either node " + fromName + " or node " + toName + " does not exist.");
		return fromN.hasIntraChild(toN);
	}
	
	protected int T;
	protected static int availableProcs = Runtime.getRuntime().availableProcessors();
	HashMap<String, DBNNode> dnodes = new HashMap<String, DBNNode>();
}
