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
 * A dimension which can be used for a field in a NetCDF file.
 * 
 * @author Sebastian Schubert
 * 
 */
public class WritableDimension extends Dimension implements NetCDFWritable {

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
	}

	/**
	 * Define a new dimension from a given one.
	 * 
	 * @param name
	 *            Name must be unique within group
	 * @param from
	 *            Old dimension
	 */
	public WritableDimension(String name, Dimension from) {
		super(name, from);
	}

	/**
	 * A new dimension with option to share it.
	 * 
	 * @param name
	 *            Name must be unique within group
	 * @param length
	 *            Length, or UNLIMITED.length or UNKNOWN.length
	 * @param isShared
	 *            Whether its shared or local to Variable
	 */
	public WritableDimension(String name, int length, boolean isShared) {
		super(name, length, isShared);
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
