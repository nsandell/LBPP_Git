package complex.featural.proposal_generators;

import java.util.Vector;

import complex.featural.FeaturalModelController;
import complex.featural.FeaturalModelController.LatentPair;
import complex.featural.IFeaturalChild;
import complex.featural.MHProposalGenerator;
import complex.featural.ProposalAction;

public class RandomAbsorbGenerator implements MHProposalGenerator
{
	public RandomAbsorbGenerator(double pabs, double pexp)
	{
		this.pexp = pexp;
		this.pabs = pabs;
	}
	
	public String name()
	{
		return "Random Absorbs";
	}
	
	public MHProposal generate(FeaturalModelController cont)
	{
		int N = cont.getLatentNodes().size();
		if(N < 2)
			return null;
		
		LatentPair pair = cont.randomLatentPair();
		
		if(pair==null)
			return null;
		
		cont.log("Proposing that node " + pair.l1.getName() + " adds all the children of node " + pair.l2.getName());
		
		Vector<IFeaturalChild> newNds = new Vector<IFeaturalChild>();
		for(IFeaturalChild child : cont.getChildren(pair.l2))
			if(!cont.getChildren(pair.l1).contains(child))
				newNds.add(child);
		
		if(newNds.size()==0)
			return null;

		cont.log("Proposing that node " + pair.l1.getName() + " adds all the children of node " + pair.l2.getName());
		
		double fp = this.pabs/((double)(N-1)*N);
		double bp = this.pexp/((double)(N-1)*N);
		
		return new MHProposal(fp,bp,new ProposalAction.ConnectAction(pair.l1, newNds));
	}
	
	double pabs, pexp;
}
