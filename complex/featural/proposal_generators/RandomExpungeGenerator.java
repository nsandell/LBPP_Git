package complex.featural.proposal_generators;

import java.util.Vector;

import complex.featural.IChildProcess;
import complex.featural.ModelController;
import complex.featural.ProposalAction;
import complex.featural.ProposalGenerator;
import complex.featural.ModelController.LatentPair;

public class RandomExpungeGenerator implements ProposalGenerator {
	
	public RandomExpungeGenerator(double pexp, double pabs)
	{
		this.pexp = pexp;
		this.pabs = pabs;
	}
	
	public String name()
	{
		return "Random expunges";
	}
	
	@Override
	public Proposal generate(ModelController cont)
	{
		int N = cont.getLatentNodes().size();
		
		LatentPair pair = cont.randomLatentPair();
		if(pair==null)
			return null;
		
		
		double fp = pexp/((double)(N*(N-1)));
		double bp = pabs/((double)(N*(N-1)));
		
		Vector<IChildProcess> expells = new Vector<IChildProcess>();
		for(IChildProcess child : cont.getChildren(pair.l2))
			if(cont.getChildren(pair.l1).contains(child))
					expells.add(child);
	
		if(expells.size()==0)
			return null;

		cont.log("Attempting to expel the children of node " + pair.l2.getName() + " from node " + pair.l1.getName());
	
		return new Proposal(fp, bp, new ProposalAction.DisconnectAction(pair.l1,expells));
	}

	private double pexp, pabs;
}
