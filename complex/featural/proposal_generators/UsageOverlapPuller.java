package complex.featural.proposal_generators;

import java.util.Vector;

import util.MathUtil;

import complex.IParentProcess;
import complex.featural.FeaturalModelController;
import complex.featural.IFeaturalChild;
import complex.featural.ProposalAction;
import complex.featural.ProposalGenerator;
import complex.metrics.UsageProvider;

public class UsageOverlapPuller<ChildProcess extends IFeaturalChild, ParentProcess extends IParentProcess> implements ProposalGenerator<ChildProcess,ParentProcess> {

	@Override
	public complex.featural.ProposalGenerator.Proposal<ChildProcess, ParentProcess> generate(
			FeaturalModelController<ChildProcess, ParentProcess> cont) {
		Vector<ParentProcess> parents = cont.getLatentNodes();
		double [][] overlap = new double[parents.size()][parents.size()];
		double [] rowdist = new double[parents.size()];
		double [] rowsum = new double[parents.size()];
		
		double[][] ts = new double[parents.size()][];
		double sum = 0;
		try {
			for(int i = 0; i < parents.size(); i++)
				ts[i] = ((UsageProvider)parents.get(i)).usagets();
		} catch(ClassCastException e) {
			System.err.println("Usageoverlappuller only for binary nodes operating through or distributions");
			return null;
		}
		for(int i = 0; i < parents.size(); i++)
		{
			rowdist[i] = 0;
			rowsum[i] = 0;
			for(int j = 0; j < parents.size(); j++)
			{
				if(j==i){/*System.err.print("0 ");*/ continue;}
				overlap[i][j] = overlap(ts[i],ts[j]);
				rowsum[i] += overlap[i][j];
				sum += overlap[i][j];
				//System.err.print(overlap[i][j] + " ");
			}
			//System.err.println();
		}
		for(int i = 0; i < parents.size(); i++)
			rowdist[i] = rowsum[i]/sum;

		int row = MathUtil.discreteSample(rowdist);
		double[] coldist = overlap[row];
		for(int i = 0; i < parents.size(); i++)
			coldist[i] /= rowsum[row];
		int col = MathUtil.discreteSample(coldist);

		return new Proposal<ChildProcess, ParentProcess>(1, 1, new ProposalAction.SwitchProposerAction<ChildProcess, ParentProcess>(parents.get(row),parents.get(col)));
	}

	private static double overlap(double[] t1, double[] t2)  // This is how much of t1 contains t2
	{
		double both = 0, only2 = 0;
		for(int t = 0; t < t1.length; t++)
		{
			both += Math.sqrt(t1[t]*t2[t]);
			only2 += Math.sqrt((1-t1[t])*t2[t]);
		}
		return both/(both+only2);
	}

	@Override
	public String name() {
		return "Low usage deleter";
	}

}
