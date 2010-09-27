package bn.distributions;

import java.util.HashMap;

import util.Parser.LineHandler;
import util.Parser.ParserException;

public class Distribution
{
	public static interface DistributionBuilder extends LineHandler
	{
		public Distribution getFinished() throws ParserException;
	}
	
	public static DistributionBuilder getDistributionBuilder(String type, String name, String arguments, HashMap<String, Distribution> distmap) throws ParserException
	{
		if(type.equals("PV") || type.equals("CPT") || type.equals("SparseCPT") || type.equals("NoisyOr"))
		{	
			return DiscreteDistribution.getDiscreteDistributionBuilder(type,name,arguments,distmap);
		}
		else throw new ParserException("Requested unknown CPD type '" + type + "'.");
	}

	public static enum DiscreteDistributionType
	{
		PV,
		CPT,
		SparseCPT,
		NoisyOr
	}
}
