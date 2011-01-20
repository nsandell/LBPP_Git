package complex.featural;

public interface ProposalGenerator
{
	public Proposal generate(ModelController cont);
	
	public static class Proposal
	{
		public Proposal(double fp, double bp, ProposalAction act)
		{
			this.fp = fp;
			this.bp = bp;
			this.act = act;
		}
		
		double forwardP()
		{
			return this.fp;
		}
		double backwardP()
		{
			return this.bp;
		}
		ProposalAction action()
		{
			return this.act;
		}
		
		private double fp, bp;
		private ProposalAction act;
	}
}
