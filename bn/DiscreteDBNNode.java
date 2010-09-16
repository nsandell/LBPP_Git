package bn;

import java.util.ArrayList;

public class DiscreteDBNNode
{
	public DiscreteDBNNode(int cardinality)
	{
		
	}
	
	// Parent ordering will go inter parents then intra parents
	private ArrayList<DiscreteBNNode> interParents = new ArrayList<DiscreteBNNode>();
	private ArrayList<DiscreteBNNode> intraParents = new ArrayList<DiscreteBNNode>();
}
