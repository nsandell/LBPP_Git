package bn.distributions;

import java.util.Arrays;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Pattern;

import bn.BNException;
import bn.messages.DiscreteMessage;

public class SparseDiscreteCPT extends DiscreteDistribution
{
	public static class Entry
	{
		public int[] conditional_indices;
		public int value_index;
		public double p;
	}

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

	private static final Pattern patt = Pattern.compile("\\s*(\\d+\\s+)+(0*(\\.\\d+)?)");
	private static final int[] pattgroups = new int[]{1,2};
	
	protected String getBuilderPrompt()
	{
		return "Enter CPT row:";
	}
	
	protected Pattern getBuilderRegex()
	{
		return patt;
	}
	
	protected int[] getRegExGroups()
	{
		return pattgroups;
	}	
	
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
	
	public void validateConditionDimensions(int [] dimens) throws BNException
	{
		if(dimens.length!=this.dimSizes.length)
			throw new BNException("Invalid parent set for CPT!");
		for(int i = 0; i < dimens.length; i++)
			if(dimens[i]!=dimSizes[i])
				throw new BNException("Invalid parent set for CPT!");
	}

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

	public double evaluateFast(int[] indices,int value)
	{
		return this.entries.get(new IndexWrapper(indices)).get(value);
	}
	
	public double computeLocalPi(DiscreteMessage local_pi, Vector<DiscreteMessage> incoming_pis, Vector<DiscreteMessage> parent_pis, Integer value) throws BNException
	{
		boolean observed = value!=null;
		double ll = 0;
		for(IndexWrapper indexset : this.entries.keySet())
		{
			double tmp = 1;
			double observation_ll_tmp = 1;
			for(int j = 0; j < indexset.indices.length; j++)
			{
				tmp *= incoming_pis.get(j).getValue(indexset.indices[j]);
				if(observed)
					observation_ll_tmp *= parent_pis.get(j).getValue(indexset.indices[j]);
			}
			for(Integer i : this.entries.get(indexset).keySet())
				local_pi.setValue(i, local_pi.getValue(i)+tmp*this.evaluate(indexset.indices, i));
			if(observed)
				ll += observation_ll_tmp*this.evaluate(indexset.indices, value);
		}
		local_pi.normalize();
		return ll;
	}
	
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
	
	public int[] getConditionDimensions(){return this.dimSizes;}
	
	private int[] dimSizes;
	private HashMap<IndexWrapper,HashMap<Integer,Double>> entries;
}