/**
 * 
 */
package citygml2ucp.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import ucar.ma2.ArrayChar;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;
import ucar.unidata.geoloc.projection.RotatedPole;
import ucar.unidata.util.Parameter;

/**
 * A rotated pole that can be written to NetCDF files.
 * 
 * @author Sebastian Schubert
 * 
 */
public class WritableRotatedPole extends RotatedPole implements NetCDFWritable {

	private static final long serialVersionUID = -4545528607347807035L;
	
	protected final Map<NetcdfFileWriter,DimensionsAndVariables> dimensionsAndVariablesAddedToNetcdf;

	/**
	 * Use super constructor to create new pole.
	 * 
	 * @param pollat
	 *            Latitude of the new pole
	 * @param pollon
	 *            Longitude of the new pole
	 */
	public WritableRotatedPole(double pollat, double pollon) {
		super(pollat, pollon);
		this.dimensionsAndVariablesAddedToNetcdf = new HashMap<>();
	}

	@Override
	public DimensionsAndVariables addToNetCDFfile(NetcdfFileWriter ncfile) {
		DimensionsAndVariables dimensionsAndVariables = this.dimensionsAndVariablesAddedToNetcdf.get(ncfile);
		if (Objects.isNull(dimensionsAndVariables)) {
			Variable var = ncfile.addVariable(null, "rotated_pole", DataType.CHAR,
					new ArrayList<Dimension>());
			for (int i = 0; i < atts.size(); i++) {
				Parameter par = atts.get(i);
				if (par.isString()) {
					ncfile.addVariableAttribute(var, new Attribute(par.getName(),
							par.getStringValue()));
				} else {
					ncfile.addVariableAttribute(var, new Attribute(par.getName(),
							(float) par.getNumericValue()));
				}
			}
			dimensionsAndVariables = new DimensionsAndVariables(null, Arrays.asList(var));
			this.dimensionsAndVariablesAddedToNetcdf.put(ncfile, dimensionsAndVariables);
		}
		return dimensionsAndVariables;
	}

	@Override
	public void writeToNetCDFfile(NetcdfFileWriter ncfile)
			throws IOException, InvalidRangeException {
		Variable var = this.dimensionsAndVariablesAddedToNetcdf.get(ncfile).variable.get(0);
		// character is written anyway so set it to ""
		ArrayChar ac = new ArrayChar.D0();
		ncfile.write(var, ac);
	}
}