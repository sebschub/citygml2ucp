/**
 * 
 */
package pik.clminputdata.tools;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;

/**
 * A dimension which can be written to a NetCDF file.
 * 
 * @author Sebastian Schubert
 * 
 */
public class WritableDimension extends Dimension implements NetCDFWritable {

	/**
	 * Define a dimension by name and length.
	 * 
	 * @param name
	 *            The name of the dimension.
	 * @param length
	 *            The length of the dimension.
	 */
	public WritableDimension(String name, int length) {
		super(name, length);
	}

	/**
	 * Define a new dimension from a given one.
	 * 
	 * @param name
	 *            The new name.
	 * @param from
	 *            The old dimension.
	 */
	public WritableDimension(String name, Dimension from) {
		super(name, from);
	}

	/**
	 * @param name
	 * @param length
	 * @param isShared
	 */
	public WritableDimension(String name, int length, boolean isShared) {
		super(name, length, isShared);
	}

	/**
	 * @param name
	 * @param length
	 * @param isShared
	 * @param isUnlimited
	 * @param isVariableLength
	 */
	public WritableDimension(String name, int length, boolean isShared,
			boolean isUnlimited, boolean isVariableLength) {
		super(name, length, isShared, isUnlimited, isVariableLength);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pik.clminputdata.configuration.NetCDFWritable#addVariablesToNetCDFfile
	 * (ucar.nc2.NetcdfFileWriteable)
	 */
	@Override
	public List<Dimension> addVariablesToNetCDFfile(NetcdfFileWriteable ncfile) {
		List<Dimension> l = new LinkedList<Dimension>();
		l.add(ncfile.addDimension(null, this));
		return l;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pik.clminputdata.configuration.NetCDFWritable#writeVariablesToNetCDFfile
	 * (ucar.nc2.NetcdfFileWriteable)
	 */
	@Override
	public void writeVariablesToNetCDFfile(NetcdfFileWriteable ncfile)
			throws IOException, InvalidRangeException {
		// dimensions just have to be added to the file and then are written
		// automatically to the file
	}

}
