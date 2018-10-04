/**
 * 
 */
package citygml2ucp.tools;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;

/**
 * A dimension which can be used for a field in a NetCDF file.
 * 
 * @author Sebastian Schubert
 * 
 */
public class WritableDimension extends Dimension implements NetCDFWritable {

	protected final Map<NetcdfFileWriter,DimensionsAndVariables> dimensionsAndVariablesAddedToNetcdf;
	
	/**
	 * Define a dimension by name and length.
	 * 
	 * @param name
	 *            Name of the dimension
	 * @param length
	 *            Length of the dimension
	 */
	public WritableDimension(String name, int length) {
		super(name, length);
		this.dimensionsAndVariablesAddedToNetcdf = new HashMap<>();
	}

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            Name must be unique within group
	 * @param length
	 *            Length, or UNLIMITED.length or UNKNOWN.length
	 * @param isShared
	 *            Whether its shared or local to Variable
	 * @param isUnlimited
	 *            Whether the length can grow
	 * @param isVariableLength
	 *            Whether the length is unknown until the data is read
	 */
	public WritableDimension(String name, int length, boolean isShared,
			boolean isUnlimited, boolean isVariableLength) {
		super(name, length, isShared, isUnlimited, isVariableLength);
		this.dimensionsAndVariablesAddedToNetcdf = new HashMap<>();
	}

	
	
	@Override
	public DimensionsAndVariables addToNetCDFfile(NetcdfFileWriter ncfile) {
		DimensionsAndVariables dimensionsAndVariables = this.dimensionsAndVariablesAddedToNetcdf.get(ncfile);
		if (Objects.isNull(dimensionsAndVariables)) {
			Dimension dim = ncfile.addDimension(null, this.shortName, this.getLength());
			dimensionsAndVariables = new DimensionsAndVariables(Arrays.asList(dim), null);
			this.dimensionsAndVariablesAddedToNetcdf.put(ncfile, dimensionsAndVariables);
		}
		return dimensionsAndVariables;
	}

	@Override
	public void writeToNetCDFfile(NetcdfFileWriter ncfile) throws IOException, InvalidRangeException {
		// nothing to do for dimension
	}

}
