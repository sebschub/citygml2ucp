/**
 * 
 */
package citygml2ucp.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;

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
	protected List<NetCDFWritable> toWrite = new ArrayList<NetCDFWritable>();

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
		NetcdfFileWriteable ncfile = NetcdfFileWriteable.createNew(filename,
				false);
		// check whether data is larger than 2GiB (with some safety margin)
		if (dataSize > 1900000000L) {
			ncfile.setLargeFile(true);
		} else {
			ncfile.setLargeFile(false);
		}
		addVariablesToNetCDFfile(ncfile);
		ncfile.create();
		writeVariablesToNetCDFfile(ncfile);
		ncfile.close();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * citygml2ucp.configuration.NetCDFWritable#addVariablesToNetCDFfile
	 * (ucar.nc2.NetcdfFileWriteable)
	 */
	@Override
	public List<Dimension> addVariablesToNetCDFfile(NetcdfFileWriteable ncfile) {
		List<Dimension> list = new LinkedList<Dimension>();
		for (NetCDFWritable item : toWrite) {
			List<Dimension> lt = item.addVariablesToNetCDFfile(ncfile);
			if (lt != null) {
				list.addAll(0, lt);
			}
		}
		return list;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * citygml2ucp.configuration.NetCDFWritable#writeVariablesToNetCDFfile
	 * (ucar.nc2.NetcdfFileWriteable)
	 */
	@Override
	public void writeVariablesToNetCDFfile(NetcdfFileWriteable ncfile)
			throws IOException, InvalidRangeException {
		for (NetCDFWritable item : toWrite) {
			item.writeVariablesToNetCDFfile(ncfile);
		}
	}

}
