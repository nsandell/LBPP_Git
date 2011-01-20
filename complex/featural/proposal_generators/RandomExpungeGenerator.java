package complex.featural.proposal_generators;

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
	
	@Override
	public Proposal generate(ModelController cont)
	{
		int N = cont.getLatentNodes().size();
		
		LatentPair pair = cont.randomLatentPair();
		
		cont.log("Attempting to expel the children of node " + pair.l2.getName() + " from node " + pair.l1.getName());
		
		double fp = pexp/((double)(N*(N-1)));
		double bp = pabs/((double)(N*(N-1)));
	
		return new Proposal(fp, bp, new ProposalAction.DisconnectAction(pair.l1,cont.getChildren(pair.l2)));
	}

	private double pexp, pabs;
}
