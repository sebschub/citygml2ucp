/**
 * 
 */
package pik.clminputdata.tools;

import java.io.IOException;
import java.util.List;

import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;

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
	 * @return Added dimensions
	 */
	List<Dimension> addVariablesToNetCDFfile(NetcdfFileWriteable ncfile);

	/**
	 * Write data to NetCDF file
	 * 
	 * @param ncfile
	 *            NetCDF file
	 * @throws IOException
	 * @throws InvalidRangeException
	 */
	void writeVariablesToNetCDFfile(NetcdfFileWriteable ncfile)
			throws IOException, InvalidRangeException;
}
