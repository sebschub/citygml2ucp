/**
 * 
 */
package pik.clminputdata.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;

/**
 * @author Sebastian Schubert
 *
 */
public class NetCDFData implements NetCDFWritable {

	protected List<NetCDFWritable> toWrite = new ArrayList<NetCDFWritable>();
	
	public void toNetCDFfile(String filename) throws IOException, InvalidRangeException{
		NetcdfFileWriteable ncfile = NetcdfFileWriteable.createNew(filename,
				false);
		addVariablesToNetCDFfile(ncfile);
		ncfile.create();
		writeVariablesToNetCDFfile(ncfile);
		ncfile.close();
	}

	/* (non-Javadoc)
	 * @see pik.clminputdata.configuration.NetCDFWritable#addVariablesToNetCDFfile(ucar.nc2.NetcdfFileWriteable)
	 */
	@Override
	public List<Dimension> addVariablesToNetCDFfile(NetcdfFileWriteable ncfile) {
		List<Dimension> list = new LinkedList<Dimension>();		
		for (NetCDFWritable item : toWrite) {
			List<Dimension> lt = item.addVariablesToNetCDFfile(ncfile);
			if (lt!=null) {
				list.addAll(0,lt);
			}
		}
		return list;
	}

	/* (non-Javadoc)
	 * @see pik.clminputdata.configuration.NetCDFWritable#writeVariablesToNetCDFfile(ucar.nc2.NetcdfFileWriteable)
	 */
	@Override
	public void writeVariablesToNetCDFfile(NetcdfFileWriteable ncfile)
			throws IOException, InvalidRangeException {
		for (NetCDFWritable item : toWrite) {
			item.writeVariablesToNetCDFfile(ncfile);
		}
	}
	
	
}