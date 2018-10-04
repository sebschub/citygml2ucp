/**
 * 
 */
package citygml2ucp.tools;

import java.io.IOException;

import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFileWriter;

/**
 * Routines for classes that include data which can be written to NetCDF files.
 * 
 * @author Sebastian Schubert
 * 
 */
public interface NetCDFWritable {

	/**
	 * Add necessary dimensions to a NetCDF file
	 * 
	 * @param ncfile
	 *            NetCDF file
	 * @return Added element
	 */
	DimensionsAndVariables addToNetCDFfile(NetcdfFileWriter ncfile);

	/**
	 * Write data to NetCDF file
	 * 
	 * @param ncfile
	 *            NetCDF file
	 * @throws InvalidRangeException 
	 * @throws IOException 
	 */
	void writeToNetCDFfile(NetcdfFileWriter ncfile) throws IOException, InvalidRangeException;
}
