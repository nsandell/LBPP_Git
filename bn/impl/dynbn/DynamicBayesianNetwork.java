package bn.impl.dynbn;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Vector;

import bn.BNException;
import bn.IBayesNode;
import bn.distributions.Distribution;
import bn.dynamic.IFDiscDBNNode;
import bn.dynamic.IDynamicBayesNet;
import bn.dynamic.IDBNNode;
import bn.dynamic.IInfDiscEvDBNNode;
import bn.impl.BayesianNetwork;

class DynamicBayesianNetwork extends BayesianNetwork<DBNNode> implements IDynamicBayesNet
{
	public DynamicBayesianNetwork(int T){this.T = T;}
	
	public RunResults optimize_subsets_parallel(Vector<String> nodeSeeds, int lmaxit, double lconv, int rmaxit, double rconv) throws BNException
	{
		long startTime = System.currentTimeMillis();
		int i = 0;
		double learnErr = 0;
		while(i < lmaxit)
		{
			learnErr = 0;
			this.run_subsets_parallel(nodeSeeds, rmaxit, rconv);
			for(DBNNode node : this.getNodes())
				learnErr = Math.max(node.optimizeParameters(),learnErr);
			if(learnErr < lconv)
				break;
			i++;
		}
		return new RunResults(i, ((double)(System.currentTimeMillis()-startTime))/1000.0, learnErr);
	}
	
	public void run_subsets_parallel(Vector<String> nodeSeeds,int maxit, double conv) throws BNException
	{
		Vector<HashSet<String>> sets = new Vector<HashSet<String>>();
		for(int i = 0; i < nodeSeeds.size(); i++)
		{
			DBNNode nd = this.getNode(nodeSeeds.get(i));
			if(nd==null)
				throw new BNException("Node seed " + nodeSeeds.get(i) + " is not a node...");
			boolean contained = false;
			for(int j= 0; j < i; j++)
				if(sets.get(j).contains(nodeSeeds.get(i)))
					contained = true;
			if(!contained)
			{
				HashSet<String> set = new HashSet<String>();
				this.addNeighbors(nd, set);
				sets.add(set);
			}
		}
		
		try {
			SubsetThreadPool pool = new SubsetThreadPool(Runtime.getRuntime().availableProcessors(), this, maxit, conv);
			for(int i = 0; i < sets.size(); i++)
			{
				pool.spawnThread(sets.get(i));
			}
			synchronized (pool.threads) {
				while(pool.threads.size()!=Runtime.getRuntime().availableProcessors())
					pool.threads.wait();
			}
		} catch(InterruptedException e) {
			throw new BNException("Subset parallel run interrupted!");
		}
	}

	private void addNeighbors(DBNNode node, Collection<String> coll)
	{
		for(DBNNode nd : node.getInterChildren())
		{
			if(!coll.contains(nd.getName()))
			{
				coll.add(nd.getName());
				this.addNeighbors(nd,coll);
			}
		}
		for(DBNNode nd : node.getIntraChildren())
		{
			if(!coll.contains(nd.getName()))
			{
				coll.add(nd.getName());
				this.addNeighbors(nd,coll);
			}
		}
		for(DBNNode nd : node.getIntraParents())
		{
			if(!coll.contains(nd.getName()))
			{
				coll.add(nd.getName());
				this.addNeighbors(nd,coll);
			}
		}
		for(DBNNode nd : node.getInterParents())
		{
			if(!coll.contains(nd.getName()))
			{
				coll.add(nd.getName());
				this.addNeighbors(nd,coll);
			}
		}
	}

	private static class SubsetThreadPool
	{
		public SubsetThreadPool(int maxthreads, DynamicBayesianNetwork net, int maxit, double conv)
		{
			for(int i = 0; i < maxthreads; i++)
				this.threads.add(new SubsetThread(net, maxit, conv, this));
		}

		public void spawnThread(Iterable<String> nodes) throws InterruptedException
		{
			synchronized (threads)
			{
				while(threads.isEmpty()) {
					threads.wait();
				}
				SubsetThread thread = threads.remove(0);
				thread.setSubset(nodes);
				thread.start();
			}
		}
		
		public void returnThread(SubsetThread thread)
		{
			synchronized(threads)
			{
				//this.threads.add(thread);
				this.threads.add(new SubsetThread(thread.net, thread.maxit, thread.conv, this));
				threads.notify();
			}
		}
		ArrayList<SubsetThread> threads = new ArrayList<DynamicBayesianNetwork.SubsetThread>();
	}

	private static class SubsetThread extends Thread
	{
		public SubsetThread(DynamicBayesianNetwork net, int maxit, double conv, SubsetThreadPool pool)
		{
			this.pool = pool;
			this.net = net;
			this.maxit = maxit;
			this.conv = conv;
		}

		public void setSubset(Iterable<String> nodes)
		{
			this.nodes = nodes;
		}

		public void run()
		{
			try {
				this.net.run(this.nodes, this.maxit, this.conv);
				this.pool.returnThread(this);
			} catch(BNException e) {
				System.err.println("Failure running subsets parallel.");
			}
		}
		
		int maxit;
		double conv;
		DynamicBayesianNetwork net;
		Iterable<String> nodes;
		SubsetThreadPool pool;
	}

	public IFDiscDBNNode addDiscreteNode(String name, int cardinality) throws BNException
	{
		if(this.getNode(name)!=null)
			throw new BNException("Node " + name + " already exists in this DBN.");
		FDiscDBNNode nd = new FDiscDBNNode(this, name, cardinality);
		this.addNodeI(nd);
		return nd;
	}
	
	public IInfDiscEvDBNNode addDiscreteEvidenceNode(String name, int[] values) throws BNException
	{
		if(this.getNode(name)!=null)
			throw new BNException("Node " + name + " already exists in this DBN.");
		InfDiscEvDBNNode nd = new InfDiscEvDBNNode(this, name, values);
		this.addNodeI(nd);
		return nd;
	}
	
	protected String getDefinitionFileHeader(){return ""+T+"\n\n";}

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
			for(DBNNode node : this.getNodes())
				learnErr = Math.max(node.optimizeParameters(),learnErr);
			if(learnErr < learnErrConvergence)
				break;
			i++;
		}
		return new RunResults(i, ((double)(System.currentTimeMillis()-startTime))/1000.0, learnErr);
	}
	
	public RunResults optimize_parallel(int maxLearnIt, double learnErrConvergence, int maxInfIt, double infErrConvergence,Iterable<String> nodes) throws BNException
	{
		long startTime = System.currentTimeMillis();
		int i = 0;
		double learnErr = 0;
		while(i < maxLearnIt)
		{
			learnErr = 0;
			this.run_parallel_block(maxInfIt, infErrConvergence);
			for(String nodename : nodes)
			{
				if(this.getNode(nodename)!=null)
					learnErr = Math.max(this.getNode(nodename).optimizeParameters(),learnErr);
				else
					throw new BNException("Node " + nodename + " does not exist or is not optimizable.");
			}
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
	}
	
	@Override
	public void removeNode(IDBNNode node) throws BNException
	{
		super.removeNode(node.getName());
	}
	
	public void setInitialDistribution(String name, Distribution dist) throws BNException
	{
		if(this.getNode(name)==null)
			throw new BNException("Attempted to set distribution of nonexistant node.");
		this.getNode(name).setInitialDistribution(dist);
	}
	public void setAdvanceDistribution(String name, Distribution dist) throws BNException
	{
		if(this.getNode(name)==null)
			throw new BNException("Attempted to set distribution of nonexistant node.");
		this.getNode(name).setAdvanceDistribution(dist);
	}
	public void setDistribution(String name, Distribution dist) throws BNException
	{
		if(this.getNode(name)==null)
			throw new BNException("Attempted to set distribution of nonexistant node.");
		this.getNode(name).setAdvanceDistribution(dist);
	}
	public Distribution getInitialDistribution(String name) throws BNException
	{
		if(this.getNode(name)==null)
			throw new BNException("Attempted to set distribution of nonexistant node.");
		return this.getNode(name).getInitialDistribution();
	}
	public Distribution getAdvanceDistribution(String name) throws BNException
	{
		if(this.getNode(name)==null)
			throw new BNException("Attempted to set distribution of nonexistant node.");
		return this.getNode(name).getAdvanceDistribution();
	}
	
	static class BlockCallback2 implements ParallelCallback
	{
		public void callback(IDynamicBayesNet net, int numIts, double err, double time) {
			synchronized (blockLock) {
				this.timeElapsed = time;
				this.errorD = err;
				this.numIts = numIts;
				this.blockLock.notify();
			}
		}

		public void error(IDynamicBayesNet net, String error)
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
	}
	
	public RunResults run_parallel_block(Iterable<String> nodes, int maxit, double conv) throws BNException
	{
		BlockCallback2 cb = new BlockCallback2();
		synchronized (cb.blockLock) {
			this.run_parallel(maxit, conv, cb);
			try{cb.blockLock.wait();}catch(InterruptedException e){System.err.println("Interrupted..");}
		}
		if(cb.error!=null)
			throw new BNException(cb.error);
		return new RunResults(cb.numIts, cb.timeElapsed, cb.errorD);
	}

	public void run_parallel(Iterable<String> nodeNames, int maxit, double conv, ParallelCallback callback) throws BNException
	{
		Vector<DBNNode> nodes = new Vector<DBNNode>();
		for(String nodeName : nodeNames)
		{
			DBNNode node = this.getNode(nodeName);
			if(node==null) throw new BNException("Cannot find node named : " + nodeName);
			nodes.add(node);
		}
		ParallelStatus status = new ParallelStatus(this, conv, maxit, callback, nodes);
		status.start_time = System.currentTimeMillis();
		parallel_iteration_regions(status);
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
		public int maxThreads = Runtime.getRuntime().availableProcessors();
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

}
