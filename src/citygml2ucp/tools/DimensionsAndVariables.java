package citygml2ucp.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ucar.nc2.Dimension;
import ucar.nc2.Variable;

public class DimensionsAndVariables {

	public final List<Dimension> dimension;
	public final List<Variable> variable;

	public DimensionsAndVariables(List<Dimension> dimension, List<Variable> variable) {
		this.dimension = (Objects.isNull(dimension)) ? new ArrayList<Dimension>() : dimension;
		this.variable  = (Objects.isNull(variable))  ? new ArrayList<Variable>()  : variable;
	}
	
	public DimensionsAndVariables() {
		this.dimension = new ArrayList<Dimension>();
		this.variable  = new ArrayList<Variable>();
	}
}
