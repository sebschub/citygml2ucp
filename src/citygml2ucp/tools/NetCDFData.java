/**
 * 
 */
package citygml2ucp.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFileWriter;

/**
 * Class for managing several fields and dimensions ought to be written to a
 * NetCDF file.
 * 
 * @author Sebastian Schubert
 * 
 */
public class NetCDFData implements NetCDFWritable {

	/**
	 * size of the data in byte to write to file; controls whether large file
	 * format is necessary
	 */
	protected long dataSize;

	/**
	 * List of items to write to NetCDF file
	 */
	protected List<NetCDFWritable> toWrite = new ArrayList<>();

	/**
	 * add additional NetCDFWritable to output list 
	 * @param field to output
	 */
	protected void addToWrite(NetCDFWritable field) {
		toWrite.add(field);
		if (field instanceof WritableField) {
			dataSize += ((WritableField) field).getSavedSizeBytes();
		}
	}
	
	/**
	 * Write data to NetCDF file.
	 * 
	 * @param filename
	 *            the filename of the NetCDF file
	 * @throws IOException
	 * @throws InvalidRangeException
	 */
	public void toNetCDFfile(String filename) throws IOException,
			InvalidRangeException {
		NetcdfFileWriter ncfile = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, filename);
		// check whether data is larger than 2GiB (with some safety margin)
		if (dataSize > 1900000000L) {
			ncfile.setLargeFile(true);
		} else {
			ncfile.setLargeFile(false);
		}
		addToNetCDFfile(ncfile);
		ncfile.create();
		writeToNetCDFfile(ncfile);
		ncfile.close();
	}

	@Override
	public DimensionsAndVariables addToNetCDFfile(NetcdfFileWriter ncfile) {
		DimensionsAndVariables dimensionsAndVariables = new DimensionsAndVariables();
		for (NetCDFWritable item : toWrite) {
			DimensionsAndVariables lt = item.addToNetCDFfile(ncfile);
			dimensionsAndVariables.dimension.addAll(lt.dimension);
			dimensionsAndVariables.variable.addAll(lt.variable);
		}
		return dimensionsAndVariables;
	}

	@Override
	public void writeToNetCDFfile(NetcdfFileWriter ncfile)
			throws IOException, InvalidRangeException {
		for (NetCDFWritable item : toWrite) {
			item.writeToNetCDFfile(ncfile);
		}
	}

}
