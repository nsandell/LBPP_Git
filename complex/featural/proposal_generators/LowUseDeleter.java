package complex.featural.proposal_generators;

import java.util.Vector;

import util.MathUtil;

import complex.IParentProcess;
import complex.featural.FeaturalModelController;
import complex.featural.ProposalAction;
import complex.featural.ProposalGenerator;
import complex.metrics.UsageProvider;

public class LowUseDeleter implements ProposalGenerator {

	@Override
	public Proposal generate(FeaturalModelController cont) {
		
		Vector<IParentProcess> parents = cont.getLatentNodes();
		double[] dist = new double[parents.size()];
	
		double sum = 0;
		for(int i = 0; i < parents.size(); i++)
		{
			dist[i] = 1.0/(1e-8+((UsageProvider)parents.get(i)).totalusage());
			sum += dist[i];
		}
		for(int i = 0; i < parents.size(); i++)
			dist[i] /=sum;
		
		int selection = MathUtil.discreteSample(dist);
		
		System.out.println("Proposing deletion of " + parents.get(selection).getName());
		
		return new Proposal(new ProposalAction.DeleteAction(parents.get(selection)));
	}

	@Override
	public String name() {
		return "Low usage deleter";
	}

}
