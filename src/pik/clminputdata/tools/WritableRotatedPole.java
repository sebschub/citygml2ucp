/**
 * 
 */
package pik.clminputdata.tools;

import java.io.IOException;
import java.util.List;

import ucar.ma2.ArrayChar;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;
import ucar.unidata.geoloc.projection.RotatedPole;
import ucar.unidata.util.Parameter;

/**
 * @author Sebastian Schubert
 * 
 */
public class WritableRotatedPole extends RotatedPole implements NetCDFWritable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public WritableRotatedPole(double pollat, double pollon) {
		super(pollat, pollon);
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
		Variable var = ncfile.addVariable("rotated_pole", DataType.CHAR,
				new Dimension[0]);
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
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pik.clminputdata.configuration.NetCDFWritable#writeVariablesToNetCDFfile
	 * (ucar.nc2.NetcdfFileWriteable)
	 */
	@Override
	public void writeVariablesToNetCDFfile(NetcdfFileWriteable ncfile) throws IOException, InvalidRangeException {
//		character is written anyway so set it to ""
		ArrayChar ac = new ArrayChar.D0();
//		ac.setChar(0, ' ');
		ncfile.write("rotated_pole", ac);
	}
}