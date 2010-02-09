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
 * @author Sebastian Schubert
 * 
 */
public interface NetCDFWritable {

	List<Dimension> addVariablesToNetCDFfile(NetcdfFileWriteable ncfile);

	void writeVariablesToNetCDFfile(NetcdfFileWriteable ncfile) throws IOException, InvalidRangeException;
}
