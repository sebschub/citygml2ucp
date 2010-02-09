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
 * @author Sebastian Schubert
 *
 */
public class WritableDimension extends Dimension implements NetCDFWritable {

	/**
	 * @param name
	 * @param length
	 */
	public WritableDimension(String name, int length) {
		super(name, length);
	}

	/**
	 * @param name
	 * @param from
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

	/* (non-Javadoc)
	 * @see pik.clminputdata.configuration.NetCDFWritable#addVariablesToNetCDFfile(ucar.nc2.NetcdfFileWriteable)
	 */
	@Override
	public List<Dimension> addVariablesToNetCDFfile(NetcdfFileWriteable ncfile) {
		List<Dimension> l = new LinkedList<Dimension>();
		l.add(ncfile.addDimension(null, this));
		return l;
	}

	/* (non-Javadoc)
	 * @see pik.clminputdata.configuration.NetCDFWritable#writeVariablesToNetCDFfile(ucar.nc2.NetcdfFileWriteable)
	 */
	@Override
	public void writeVariablesToNetCDFfile(NetcdfFileWriteable ncfile)
			throws IOException, InvalidRangeException {
	}

}
