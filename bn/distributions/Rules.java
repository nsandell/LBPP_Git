package bn.distributions;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map.Entry;

import bn.BNException;
import bn.distributions.DiscreteDistribution.DiscreteFiniteDistribution;
import bn.distributions.SparseDiscreteCPT.IndexWrapper;
import bn.messages.FiniteDiscreteMessage;
import bn.messages.MessageSet;

public class Rules
{
	
	public Rules(int[] parentDims)
	{
		this.parentDims = parentDims.clone();
	}
	
	public void addRule(int[] indices, int out) throws BNException
	{
		if(indices.length!=this.parentDims.length)
			throw new BNException("Improper parent set for rule..");
		for(int i = 0; i < indices.length; i++)
			if(indices[i] < 0 || indices[i] >= this.parentDims[i])
				throw new BNException("Rule out of parent domain.");
		
		this.rules.put(new IndexWrapper(indices), out);
	}

	public String getDefinition() {
		
		String rulesstr = "";
		
		for(Entry<IndexWrapper, Integer> rule : this.rules.entrySet())
		{
			int[] ind = rule.getKey().indices;
			rulesstr += "("+ind[0];
			for(int i = 0; i < ind.length; i++)
				rulesstr += ","+ind[i];
			rulesstr += ") => " + rule.getValue() + "\n";
		}
		return rulesstr;
	}

	public int sample(Distribution.ValueSet<Integer> parentVals) throws BNException {
		int[] vals = new int[parentVals.length()];
		Integer val = this.rules.get(new IndexWrapper(vals));
		if(val==null) throw new BNException("Attempted to sample rules for parent values that there is no rule for..");
		return val;
	}

	public double evaluate(int[] indices, int value) throws BNException {
		return this.rules.get(new IndexWrapper(indices))==value ? 1.0 : 0.0;
	}
	
	//TODO May decide at some point we don't care about parent dimensions and just toss run time
	// when we encounter some combination we've no rules for.
	public void validateConditionDimensions(int[] dimensions)
			throws BNException {
		if(this.parentDims.length!=dimensions.length)
			throw new BNException("Node with infinite rules set has invalid parent dimensions.");
		for(int i = 0; i < dimensions.length; i++)
			if(dimensions[i]!=this.parentDims[i])
				throw new BNException("Parent has improper cardinality."); 
	}

	protected HashMap<IndexWrapper, Integer> rules = new HashMap<IndexWrapper, Integer>();
	protected int[] parentDims;
	
	public static class RulesFinite extends DiscreteFiniteDistribution
	{
		Rules rules;
		
		public RulesFinite(int cardinality,int[] parentdims)
		{
			super(cardinality);
			this.rules = new Rules(parentdims);
		}

		@Override
		public double optimize(SufficientStatistic obj) throws BNException {return 0;}

		@Override
		public void printDistribution(PrintStream pr) {
		}

		@Override
		public String getDefinition() {
			String def = "RulesFinite("+this.getCardinality();
			for(int i = 0; i < this.rules.parentDims.length; i++)
				def += ","+this.rules.parentDims[i];
			def+=")\n";
			def+= this.rules.getDefinition();
			return def;
		}

		@Override
		public DiscreteFiniteDistribution copy() throws BNException {
			RulesFinite copy = new RulesFinite(this.getCardinality(),this.rules.parentDims);
			copy.rules.rules = new HashMap<SparseDiscreteCPT.IndexWrapper, Integer>(this.rules.rules);
			return copy;
		}

		@Override
		public DiscreteSufficientStatistic getSufficientStatisticObj() {return null;}

		@Override
		public void computeLocalPi(FiniteDiscreteMessage local_pi,
				MessageSet<FiniteDiscreteMessage> incoming_pi)
				throws BNException {
			int[] indices = initialIndices(this.rules.parentDims.length);
			local_pi.empty();
			do
			{
				double p = 1;
				for(int i = 0; i < incoming_pi.size(); i++)
				{
					double pv = incoming_pi.get(i).getValue(indices[i]);
					if(pv==0)
					{
						p = 0;
						continue;
					}
					else
						p *= pv;
	
					Integer value = this.rules.rules.get(new IndexWrapper(indices));
					if(value==null)
						throw new BNException("While attempting to compute a local pi for a rule node, encountered possible parent configuration with no rule.");
					
					local_pi.setValue(value,local_pi.getValue(value)+p);
				}
			} while((indices = incrementIndices(indices, this.rules.parentDims))!=null);
			local_pi.normalize();
		}

		@Override
		public void computeLambdas(
				MessageSet<FiniteDiscreteMessage> lambdas_out,
				MessageSet<FiniteDiscreteMessage> incoming_pis,
				FiniteDiscreteMessage local_lambda, Integer value)
				throws BNException {
			
			for(Entry<IndexWrapper, Integer> rule : this.rules.rules.entrySet())
			{
				double pprod = 1;
				int[] parents = rule.getKey().indices;
				int numzeros = 0;
				int zeroIndex=-1;
				for(int i = 0; i < parents.length; i++)
				{
					double v = incoming_pis.get(i).getValue(parents[i]); 
					if(v==0 && numzeros==0)
					{
						numzeros++;
						zeroIndex = i;
					}
					else if(v==0)
					{
						numzeros++;
						break;
					}
					else
						pprod *= v;
				}
				
				if(pprod==0 && numzeros > 1)
					continue;
				
				double currn = local_lambda.getValue(rule.getValue());
				for(int i = 0; i < parents.length; i++)
				{
					if(pprod > 0)
						lambdas_out.get(i).setValue(parents[i], lambdas_out.get(i).getValue(parents[i])+currn*pprod/incoming_pis.get(i).getValue(parents[i]));
					if(pprod==0 && i==zeroIndex)
						lambdas_out.get(i).setValue(parents[i], lambdas_out.get(i).getValue(parents[i])+currn*pprod);
				}
			}
		}

		@Override
		public double computeBethePotential(
				MessageSet<FiniteDiscreteMessage> incoming_pis,
				FiniteDiscreteMessage local_lambda,
				FiniteDiscreteMessage marginal, Integer value, int numChildren)
				throws BNException {
			
			//Only uncertainty here is in parents.
			double H1 = 0;
			for(int i = 0; i < incoming_pis.size(); i++)
			{
				for(int j = 0; j < incoming_pis.get(i).getCardinality(); j++)
				{
					double v= incoming_pis.get(i).getValue(j);
					H1 += v*Math.log(v);
				}
			}
			double H2 = 0;
			if(numChildren > 0)
			{
				for(int i = 0; i < marginal.getCardinality(); i++)
				{
					double v = marginal.getValue(i);
					H2 += v*Math.log(v);
				}
				H2 *= numChildren;
			}
			return H1-H2;
		}

		@Override
		public int sample(ValueSet<Integer> parentVals) throws BNException {
			return this.rules.sample(parentVals);
		}

		@Override
		public double evaluate(int[] indices, int value) throws BNException {
			return this.rules.evaluate(indices, value);
		}

		@Override
		public void validateConditionDimensions(int[] dimensions)
				throws BNException {
			this.rules.validateConditionDimensions(dimensions);
		}
	}
}

