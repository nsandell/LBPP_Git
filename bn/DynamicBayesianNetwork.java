package bn;

import java.util.HashMap;
import java.util.LinkedList;

import bn.interfaces.IDynBayesNode;
import bn.interfaces.IDynBayesNet;

class DynamicBayesianNetwork extends BayesianNetwork<IDynBayesNode,DBNNode<?>> implements IDynBayesNet{

	public DynamicBayesianNetwork(int T){this.T = T;}

	public DiscreteDBNNode addDiscreteNode(String name, int cardinality) throws BNException
	{
		if(this.getNode(name)!=null)
			throw new BNException("Node " + name + " already exists in this DBN.");
		DiscreteDBNNode nd = new DiscreteDBNNode(this, unrolled_network, name, cardinality);
		this.dnodes.put(name, nd);
		this.addNodeI(nd);
		return nd;
	}

	public void addInterEdge(String fromname, String toname) throws BNException
	{
		DBNNode<?> from = this.getNode(fromname), to = this.getNode(toname);
		if(from==null || to==null)
			throw new BNException("Failed to add interconnection, either (or both) node " + from + " or node " + to + " doesn't exist.");
		try
		{
			from.addInterChild(to);
		} catch(BNException e) {throw new BNException("Whilst interconnecting "+from+"=>"+to+":",e);}
		try
		{
			to.addInterParent(from);
		} catch(BNException e) {
			from.removeInterChild(to);
			throw new BNException("Whilst interconnecting "+from+"=>"+to+":",e);
		}
	}

	public void addInterEdge(IDynBayesNode from, IDynBayesNode to) throws BNException
	{
		if(from==null || to==null)
			throw new BNException("Null argument passed to addInterEdge...");
		this.addInterEdge(from.getName(), to.getName());
	}

	public void addIntraEdge(String fromname, String toname) throws BNException
	{		
		DBNNode<?> from = this.getNode(fromname), to = this.getNode(toname);
		if(from==null || to==null)
			throw new BNException("Failed to add intraconnection, either (or both) node " + from + " or node " + to + " doesn't exist.");
		try
		{
			from.addIntraChild(to);
		} catch(BNException e) {throw new BNException("Whilst intraconnecting "+from+"=>"+to+":",e);}
		try
		{
			to.addIntraParent(from);
		} catch(BNException e) {
			from.removeIntraChild(to);
			throw new BNException("Whilst intraconnecting "+from+"=>"+to+":",e);
		}
	}

	public void addIntraEdge(IDynBayesNode from, IDynBayesNode to) throws BNException
	{
		if(from==null || to==null)
			throw new BNException("Null argument passed to addIntraEdge...");
		this.addIntraEdge(from.getName(), to.getName());
	}

	public int getT()
	{
		return this.T;
	}

	@Override
	protected void removeNodeI(DBNNode<?> node) throws BNException
	{
		for(DBNNode<?> intrachild : node.getIntraChildrenI())
			intrachild.removeIntraParent(node);
		for(DBNNode<?> interchild : node.getInterChildrenI())
			interchild.removeInterParent(node);
		for(DBNNode<?> intraparent : node.getIntraParentsI())
			intraparent.removeIntraChild(node);
		for(DBNNode<?> interparent: node.getInterParentsI())
			interparent.removeInterChild(node);
	}
	
	public void run_parallel(int max_iterations, double convergence, ParallelInferenceCallback callback) throws BNException
	{
		LinkedList<DBNNode<?>> nodelist = new LinkedList<DBNNode<?>>();
		for(DBNNode<?> node : this.getNodes())
		{
			node.sendInitialMessages();
			nodelist.add(node);
		}
		
		/* Must reserve "boundary" nodes for single thread operation to avoid updating
		 *  neighboring nodes simultaneously..
		 *  Say N is the number of processors.  There are N-1 slices to reserve for
		 *  final updating.  That leaves TNB=T-(N-1) slices to be divvied up among the processors.
		 *  For simplicity, all but the last slice will have floor(TNB/N) slices, and the last slice
		 *  will take the remainder
		 */
		
		ParallelStatus status = new ParallelStatus(this,convergence,max_iterations,callback,nodelist);
		parallel_iteration_regions(status);
	}
	
	public void parallel_iteration_regions(ParallelStatus status)
	{
		double nnb = this.T - (status.maxThreads -1);
		int numEarlySlice = (int)Math.floor(nnb/status.maxThreads);
		int numLastSlice = T - numEarlySlice*(status.maxThreads-1);
		
		for(int i = 0; i < status.maxThreads-1; i++)
		{
			// For i < maxthreads-1, slice range is i+i*numEarlySlice : 1 : (i+1)*numEarlySlice+i-1
			SliceRangeSample thread = new SliceRangeSample(status, status.nodes, i*numEarlySlice+i, (i+1)*numEarlySlice+i-1);
			thread.start();
		}
		SliceRangeSample thread = new SliceRangeSample(status, status.nodes, this.T-numLastSlice, this.T-1);
		thread.start();
	}
	
	public void parallel_iteration_borders(ParallelStatus status)
	{
		double nnb = this.T - (status.maxThreads -1);
		int numEarlySlice = (int)Math.floor(nnb/status.maxThreads);
		
		try
		{
			for(int i = 0; i < status.maxThreads-1; i++)
			{
				int border = i+ (i+1)*numEarlySlice;
				for(DBNNode<?> node : status.nodes)
					node.updateMessages(border, border);
			}
		} catch(BNException e)
		{
			status.ok = false;
			status.message = e.toString();
		}
	}
	
	private static class ParallelStatus
	{
		
		public ParallelStatus(DynamicBayesianNetwork bn, double tolerance, int maxIterations, ParallelInferenceCallback callback, Iterable<DBNNode<?>> nodes)
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
					if(this.doneThreads==availableProcs)
					{
						this.doneThreads = 0;
						this.bn.parallel_iteration_borders(this);
						
						this.iteration++;
						if(this.iteration!=this.maxIterations && this.getError() > this.tolerance)
						{
							this.reset();
							this.bn.parallel_iteration_regions(this);
						}
						else
							this.callback.callback(bn);
					}
				}
			}
			else
				this.callback.error(bn,this.message);
		}
		
		public boolean ok = true;
		public String message;
		public int iteration = 0;
		public int maxIterations;
		public int maxThreads = availableProcs;
		public Iterable<DBNNode<?>> nodes;

		private ParallelInferenceCallback callback;
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
		public SliceRangeSample(ParallelStatus status, Iterable<DBNNode<?>> nodes, int tmin, int tmax)
		{
			this.nodes = nodes; this.tmin = tmin; this.tmax = tmax;  this.status = status;
		}
		
		public void run()
		{
			status.threadStart();
			try
			{
				double maxerr = 0;
				for(DBNNode<?> node : this.nodes)
					maxerr = Math.max(maxerr,node.updateMessages(this.tmin, this.tmax));
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
		private Iterable<DBNNode<?>> nodes;
	}

	protected int T;
	protected StaticBayesianNetwork unrolled_network = new StaticBayesianNetwork();
	HashMap<String, DBNNode<?>> dnodes = new HashMap<String, DBNNode<?>>();
}
