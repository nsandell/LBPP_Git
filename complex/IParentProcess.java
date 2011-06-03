package complex;

import java.io.PrintStream;
import java.util.Collection;

import complex.metrics.FinDiscMarginalDivergence.FinDiscMarginalHolder;
//TODO Some methods throw exceptions, some just print errors, need consistency
//TODO Findiscmarginalholder asserts need to return a finite discrete marginal
// which prevents us from doing say an mHMM kalman setup.  need to revist this
// situation in the future and perhaps define a more flexible interface such as
// say "similarity"
public interface IParentProcess extends FinDiscMarginalHolder {
	String getName();
	
	void backupParameters();
	void restoreParameters();
	void lockParameters();
	
	double parameterLL();
	
	int id();
	
	void printMarginal(PrintStream ps) throws CMException;
	
	void kill() throws CMException;
	void addChild(IChildProcess child) throws CMException;
	void removeChild(IChildProcess child) throws CMException;
	
	Collection<String> constituentNodeNames();
}
