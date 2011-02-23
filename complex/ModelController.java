package complex;


import java.io.PrintStream;

import bn.BNException;
import bn.dynamic.IDynamicBayesNet;

public abstract class ModelController
{
	
	public void validate() throws CMException
	{
		try {
			this.network.validate();
		} catch(BNException e) {throw new CMException(e.toString());}
	}

	public double run(int max_it, double conv)  throws CMException
	{
		try {
			this.network.run_parallel_block(max_it, conv);
			double ll = this.network.getLogLikelihood();
			if(Double.isNaN(ll) || ll > 0)
			{
				this.network.resetMessages();
				this.network.run_parallel_block(max_it,conv);
				ll = this.network.getLogLikelihood();
				if(Double.isNaN(ll) || ll > 0)
				{
					this.network.print(System.err);
					this.network.getLogLikelihood();
					throw new CMException("Model returns NaN/Greater than 0 log likelihood!");
				}
			}
			return ll;
		} catch(BNException e) {
			throw new CMException("Error running the model : " + e.toString());
		}
	}
	
	public double learn(int max_learn_it, double learn_conv, int max_run_it, double run_conv) throws CMException
	{
		try {
			this.network.optimize_parallel(max_learn_it, learn_conv, max_run_it, run_conv);
			return this.run(max_run_it,run_conv);
		} catch(BNException e) {
			throw new CMException("Error optimizing the model : " + e.toString());
		}
	}

	public void log(String msg)
	{
		if(logger!=null)
			logger.println(msg);
	}
	
	public void setLogger(PrintStream log)
	{
		this.logger = log;
	}
	
	protected PrintStream logger = null;
	protected IDynamicBayesNet network;
}
