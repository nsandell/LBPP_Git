package bn.distributions;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import util.MathUtil;

import bn.BNException;
import bn.messages.DiscreteMessage;

/**
 * Sparse discrete CPT.  The storage is fairly heavy weight (hash tables) so this probably won't
 * save space unless very sparse.  More likely it will save time.  Does not necessarily have to
 * have every parent index combination specified, but if that combination comes up in runtime
 * it will cause an exception.
 * @author Nils F. Sandell
 */
public class SparseDiscreteCPT extends DiscreteDistribution
{
	/**
	 * Class representative of an entry in a CPT, this is for outside classes
	 * initializing the CPT.
	 * @author Nils F. Sandell
	 */
	public static class Entry
	{
		public int[] conditional_indices;
		public int value_index;
		public double p;
	}
	
	@Override
	public int sample(ValueSet<Integer> parents) throws BNException
	{
		int[] parentsI = new int[parents.length()];
		for(int i = 0; i < parents.length(); i++)
			parentsI[i] = parents.getValue(i);
		HashMap<Integer, Double> inner = this.entries.get(new IndexWrapper(parentsI));
		if(inner==null || inner.size()==0)
			throw new BNException("Attempted to generate sample from sparse cpt where no entries exist for that condition.");
		
		double val = MathUtil.rand.nextDouble();
		double sum = 0;
		int lastint = 0;
		for(java.util.Map.Entry<Integer, Double> ent : inner.entrySet())
		{
			lastint = ent.getKey();
			sum += ent.getValue();
			if(val < sum)
				return lastint;
		}
		return lastint;
	}
	
	public void print(PrintStream ps)
	{
		ps.print("SparseCPT(" + dimSizes[0]);
		for(int i = 1; i < this.dimSizes.length; i++)
			ps.print(","+this.dimSizes[i]);
		ps.println(")");
		for(IndexWrapper wr : this.entries.keySet() )
		{
			HashMap<Integer, Double> values = this.entries.get(wr);
			for(java.util.Map.Entry<Integer, Double> ent : values.entrySet())
			{
				ps.print(wr.indices[0]);
				for(int i = 1; i < this.dimSizes.length; i++)
					ps.print(" " + wr.indices[i]);
				ps.print(" " + ent.getKey());
				ps.println(" " + ent.getValue());
			}
		}
		ps.println("***");
	}

	/**
	 * Wrap a set of indices for hashing and comparison, for use in our tables.
	 * @author Nils F. Sandell
	 */
	private static class IndexWrapper
	{
		public IndexWrapper(int[] indices)
		{
			this.indices = indices;
		}

		@Override
		public boolean equals(Object other)
		{
			return (other instanceof IndexWrapper) && this.equalsI((IndexWrapper)other);
		}

		@Override
		public int hashCode()
		{
			if(this.hashCodeV==null)
				this.hashCodeV = Arrays.hashCode(this.indices);
			return this.hashCodeV;
		}

		private boolean equalsI(IndexWrapper other)
		{
			if(this.indices.length!=other.indices.length)
				return false;
			else
			{
				for(int i = 0; i < this.indices.length; i++)
				{
					if(this.indices[i]!=other.indices[i])
						return false;
				}
			}
			return true;
		}

		private Integer hashCodeV = null;
		private int[] indices;
	}

	/**
	 * Constructor, build sparse table from an iterator over entries.
	 * @param entryTable Iterator over entries to set the table with.
	 * @param dimSizes Size of conditioning variables.
	 * @param cardinality Cardinality of the varibale.
	 * @throws BNException If the Sparse CPT has a row that doesn't sum to ones
	 * 			(that has at least one entry) or the arguments are otherwise inconsistent
	 */
	public SparseDiscreteCPT(Iterator<Entry> entryTable, int[] dimSizes, int cardinality) throws BNException
	{
		super(cardinality);

		this.entries = new HashMap<IndexWrapper, HashMap<Integer,Double>>();
		this.dimSizes = dimSizes;

		while(entryTable.hasNext())
		{
			Entry next = entryTable.next();
			if(next.p < 0) throw new BNException("Attempted to initialize with an entry that has p < 0");
			if(next.value_index >= cardinality || !goodIndex(next.conditional_indices,dimSizes))
				throw new BNException("Bad entry with indices " + DiscreteDistribution.indexString(next.conditional_indices) + " for entry " 
						+ next.value_index + " where size is " +DiscreteDistribution.indexString(this.dimSizes) + " with cardinality "
						+ this.getCardinality());
			IndexWrapper cur_indices = new IndexWrapper(next.conditional_indices);
			if(entries.get(cur_indices)==null)
				entries.put(cur_indices, new HashMap<Integer,Double>());
			else if(entries.get(cur_indices).get(next.value_index)!=null)
				throw new BNException("Duplicate entry specified for index " + indexString(next.conditional_indices));
			entries.get(cur_indices).put(next.value_index, next.p);
		}
		this.validate();
	}
	
	@Override
	public SparseDiscreteCPT copy() throws BNException
	{
		class EntryItWrap implements Iterator<Entry> { 
			public EntryItWrap(HashMap<IndexWrapper, HashMap<Integer,Double>> map)
			{
				this.outterMap = map;
				this.innerIt = innerMap.keySet().iterator();
				this.outterIt = map.keySet().iterator();
				this.currentIndices = this.outterIt.next();
				this.outterMap.get(this.currentIndices);
			}
			
			public boolean hasNext()
			{
				return (this.innerIt.hasNext() || this.outterIt.hasNext());
			}
			
			public Entry next()
			{
				Entry ent = new Entry();
				if(!this.innerIt.hasNext())
				{
					this.currentIndices = outterIt.next();
					this.innerMap = this.outterMap.get(this.currentIndices);
					this.innerIt = this.innerMap.keySet().iterator();
				}
				ent.conditional_indices = this.currentIndices.indices;
				ent.value_index = this.innerIt.next();
				ent.p = this.innerMap.get(ent.value_index);
				return ent;
			}
			
			public void remove(){}
	
			private HashMap<IndexWrapper,HashMap<Integer,Double>> outterMap;
			private HashMap<Integer,Double> innerMap;
			private Iterator<Integer> innerIt;
			private Iterator<IndexWrapper> outterIt;
			IndexWrapper currentIndices;
		}

		return new SparseDiscreteCPT(new EntryItWrap(this.entries),this.dimSizes,this.getCardinality());
	}

	/**
	 * Validate this CPT
	 * @throws BNException If invalid
	 */
	private void validate() throws BNException
	{
		int[] indices = new int[this.dimSizes.length];
		for(int i = 0; i < indices.length; i++)
			indices[i] = 0;

		do
		{
			HashMap<Integer,Double> ucdist = entries.get(new IndexWrapper(indices));

			double sum = 0;
			Iterator<Double> values = ucdist.values().iterator();
			while(values.hasNext())
				sum += values.next();

			if(Math.abs(sum-1) > 1e-12)
				throw new BNException("Failed to correctly specify distribution for indices " + indexString(indices));

		} while((indices = incrementIndices(indices, dimSizes))!=null);
	}

	/**
	 * Ensure an index set is valid given the dimensions of conditioning variables.
	 * @param indexes Index set over conditioning variables
	 * @param dimSizes Conditioning variables dimensions
	 * @return True of good, else false
	 */
	private final static boolean goodIndex(int[] indexes, int[] dimSizes)
	{
		if(indexes.length!=dimSizes.length)
			return false;
		for(int i = 0; i < indexes.length; i++)
		{
			if(indexes[i] >= dimSizes[i])
				return false;
		}
		return true;
	}
	
	@Override
	public void validateConditionDimensions(int [] dimens) throws BNException
	{
		if(dimens.length!=this.dimSizes.length)
			throw new BNException("Invalid parent set for CPT!");
		for(int i = 0; i < dimens.length; i++)
			if(dimens[i]!=dimSizes[i])
				throw new BNException("Invalid parent set for CPT!");
	}

	@Override
	public double evaluate(int[] indices,int value) throws BNException
	{
		if(!goodIndex(indices,this.dimSizes))
			throw new BNException("Failure to evaluate CPT, invalid indices " + indexString(indices) + " for dimensions " + indexString(this.dimSizes));
		if(value >= this.getCardinality())
			throw new BNException("Failure to evaluate CPT, bad value " + value + " where cardinality is " + this.getCardinality());

		try
		{
			return this.entries.get(new IndexWrapper(indices)).get(value);
		} catch(NullPointerException e)
		{
			return 0;
		}
	}

	/**
	 * Evaluate using an index wrapper rather than int[]
	 * @param indexWrapper Conditioning variable values
	 * @param value value of variable of interest
	 * @return probability
	 */
	public double evaluate(IndexWrapper indexWrapper, int value)
	{
		try{
			return this.entries.get(indexWrapper).get(value);
		} catch(NullPointerException e) {
			return 0;
		}
	}
	
	/**
	 * Faster evaluation, without checking properness of
	 * indices
	 * @param indices Values of conditioning variables
	 * @param value Value of variable of interest
	 * @return Probability.
	 */
	public double evaluateFast(int[] indices,int value)
	{
		try
		{
			return this.entries.get(new IndexWrapper(indices)).get(value);
		} catch(NullPointerException e) {
			return 0;
		}
	}
	
	@Override
	public void computeLocalPi(DiscreteMessage local_pi, Vector<DiscreteMessage> incoming_pis, Integer value) throws BNException
	{
		for(IndexWrapper indexset : this.entries.keySet())
		{
			double tmp = 1;
			for(int j = 0; j < indexset.indices.length; j++)
				tmp *= incoming_pis.get(j).getValue(indexset.indices[j]);
			for(Integer i : this.entries.get(indexset).keySet())
				local_pi.setValue(i, local_pi.getValue(i)+tmp*this.evaluate(indexset, i));
		}
		local_pi.normalize();
	}
	
	@Override
	public double optimize(SufficientStatistic stat) throws BNException
	{
		if(!(stat instanceof SparseCPTSuffStat))
			throw new BNException("Failed to optimize : incorrect (or null) sufficient statistic.");
		double maxChange = 0;
		SparseCPTSuffStat stato = (SparseCPTSuffStat)stat;
		this.entries.clear();
		for(IndexWrapper index : stato.expected_trans.keySet())
		{
			HashMap<Integer,Double> innerMap_stat = stato.expected_trans.get(index);
			HashMap<Integer,Double> innerMap = new HashMap<Integer, Double>();
			this.entries.put(index,innerMap); 
			double rowsum = stato.row_sum.get(index);
			for(Integer value : innerMap_stat.keySet())
			{
				double newval = innerMap_stat.get(value)/rowsum;
				if(innerMap.get(value)==null)
					maxChange = Math.max(maxChange,newval);
				else
					maxChange = Math.max(maxChange, Math.abs(newval-innerMap.get(value)));
				innerMap.put(value, newval);
			}
		}
		return maxChange;
	}
	
	public void printDistribution(PrintStream ps)
	{
		ps.println("Sparse CPT: ");
		for(IndexWrapper iw : this.entries.keySet())
		{
			HashMap<Integer,Double> inner = this.entries.get(iw);
			String iws = indexString(iw.indices);
			for(Map.Entry<Integer, Double> en : inner.entrySet())
			{
				ps.println(iws + " => " + en.getKey() + " w.p." + en.getValue());
			}
		}
	}
	
	@Override
	public void computeLambdas(Vector<DiscreteMessage> lambdas_out, Vector<DiscreteMessage> incoming_pis, DiscreteMessage local_lambda, Integer obsvalue) throws BNException
	{
		for(IndexWrapper indexW : this.entries.keySet())
		{
			double pi_product = 1;
			int zeroParent = -1;
			for(int i = 0; i < indexW.indices.length; i++)
			{
				double value = incoming_pis.get(i).getValue(indexW.indices[i]);
				if(value==0 && zeroParent==-1)
					zeroParent = i;
				else if(value==0){pi_product = 0;break;}
				else
					pi_product *= value;
			}

			if(obsvalue==null)
			{
				for(Integer i : this.entries.get(indexW).keySet())
				{
					double p = this.entries.get(indexW).get(i);

					for(int j = 0; j < indexW.indices.length; j++)
					{
						double local_pi_product = pi_product;
						if(local_pi_product > 0 && zeroParent==-1)
							local_pi_product /= incoming_pis.get(j).getValue(indexW.indices[j]);

						lambdas_out.get(j).setValue(indexW.indices[j], lambdas_out.get(j).getValue(indexW.indices[j]) + p*local_pi_product*local_lambda.getValue(i));
					}
				}
			}
			else
			{
				Double p = this.entries.get(indexW).get(obsvalue);
				if(p!=null)
				{
					for(int j = 0; j < indexW.indices.length; j++)
					{
						double local_pi_product = pi_product;
						if(local_pi_product > 0 && zeroParent==-1)
							local_pi_product /= incoming_pis.get(j).getValue(indexW.indices[j]);

						lambdas_out.get(j).setValue(indexW.indices[j], lambdas_out.get(j).getValue(indexW.indices[j]) + p*local_pi_product*local_lambda.getValue(obsvalue));
					}				
				}
			}
		}
	}
	
	@Override
	public DiscreteSufficientStatistic getSufficientStatisticObj()
	{
		return new SparseCPTSuffStat(this);
	}
	
	/**
	 * Sufficient statistic for sparse CPT.  Similar to dense statistic but stored
	 * sparsely
	 * @author Nils F. Sandell
	 */
	private static class SparseCPTSuffStat implements DiscreteSufficientStatistic
	{
		public SparseCPTSuffStat(SparseDiscreteCPT cpt)
		{
			this.cpt =cpt;
			this.expected_trans = new HashMap<SparseDiscreteCPT.IndexWrapper, HashMap<Integer,Double>>();
			this.row_sum = new HashMap<SparseDiscreteCPT.IndexWrapper, Double>();
		}
		
		public void reset()
		{
			this.row_sum.clear();
			this.expected_trans.clear();
		}
		
		@Override
		public SparseCPTSuffStat update(DiscreteMessage lambda,
				Vector<DiscreteMessage> incomingPis)
		{
			HashMap<IndexWrapper, HashMap<Integer, Double>> current = new HashMap<SparseDiscreteCPT.IndexWrapper, HashMap<Integer,Double>>();
			double sum = 0;
			for(IndexWrapper indices : this.cpt.entries.keySet())
			{
				double current_prod = 1;
				for(int i = 0; i < indices.indices.length; i++)
					current_prod *= incomingPis.get(i).getValue(indices.indices[i]);
				HashMap<Integer,Double> inner = this.cpt.entries.get(indices);
				HashMap<Integer,Double> innerC = new HashMap<Integer, Double>();
				for(Integer x : inner.keySet())
				{
					double value = current_prod*inner.get(x)*lambda.getValue(x);
					if(value > 0)
					{
						innerC.put(x, value);
						sum += value;
					}
				}
				if(innerC.size()>0)
					current.put(indices, innerC);
			}
			
			for(IndexWrapper indices : current.keySet())
			{
				HashMap<Integer,Double> innerE = this.expected_trans.get(indices);
				HashMap<Integer,Double> innerC = current.get(indices);
				if(innerE==null)
				{
					innerE = new HashMap<Integer, Double>();
					this.expected_trans.put(indices,innerE);
				}
				double crowsum = 0;
				for(Integer x : innerC.keySet())
				{
					Double existing = innerE.get(x);
					if(existing==null)
						existing = 0.0;
					existing += innerC.get(x)/sum;
					crowsum += existing;
					innerE.put(x, existing);
				}					
				Double existingrs = this.row_sum.get(indices);
				if(existingrs==null)
					existingrs = 0.0;
				existingrs += crowsum;
				this.row_sum.put(indices, existingrs);
			}
			return this;
		}
		

		public SparseCPTSuffStat update(SufficientStatistic stat) throws BNException
		{
			if(!(stat instanceof SparseCPTSuffStat))
				throw new BNException("Attempted to update sparse CPT with non sparse CPT statistics");
			SparseCPTSuffStat stato = (SparseCPTSuffStat)stat;
			for(IndexWrapper indices : stato.expected_trans.keySet())
			{
				HashMap<Integer,Double> innerThis  = this.expected_trans.get(indices);
				HashMap<Integer,Double> innerOther = stato.expected_trans.get(indices);
				
				if(innerThis==null)
				{
					innerThis = new HashMap<Integer, Double>();
					this.expected_trans.put(indices,innerThis);
				}
				
				for(Integer value : innerOther.keySet())
				{
					Double curr = innerThis.get(value);
					if(curr==null)
						curr = innerOther.get(value);
					else
						curr += innerOther.get(value);
					innerOther.put(value, curr);
				}
			}
			return this;
		}
		
		private SparseDiscreteCPT cpt;
		private HashMap<IndexWrapper,Double> row_sum;
		private HashMap<IndexWrapper,HashMap<Integer,Double>> expected_trans;
	}
	
	@Override
	public double computeBethePotential(Vector<DiscreteMessage> incoming_pis,
			DiscreteMessage local_lambda, DiscreteMessage local_pi,Integer value, int numChildren)
			throws BNException {
		// TODO Auto-generated method stub, sparse CPT bethe energy to be implemented...
		return 0;
	}
	
	private int[] dimSizes;
	private HashMap<IndexWrapper,HashMap<Integer,Double>> entries;
	
	private static final long serialVersionUID = 50L;


}