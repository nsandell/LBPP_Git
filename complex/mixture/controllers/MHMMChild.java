package complex.mixture.controllers;

import java.util.Collection;

import complex.mixture.IMixtureChild;

public interface MHMMChild extends IMixtureChild
{
	Collection<String> constituentNodeNames();
}
