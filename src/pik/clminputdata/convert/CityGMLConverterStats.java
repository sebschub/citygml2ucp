/**
 * 
 */
package pik.clminputdata.convert;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import pik.clminputdata.tools.NetCDFData;
import pik.clminputdata.tools.WritableDimension;
import pik.clminputdata.tools.WritableField;
import pik.clminputdata.tools.WritableFieldFloat;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;

/**
 * @author Sebastian Schubert
 * 
 */
public class CityGMLConverterStats extends NetCDFData {

	private ArrayList<String>[] notPlanarList;
	private ArrayList<String>[] noSurfButBuildFracList;
	private File[] flist;

	File logNonPlanar;
	File logNoSurfButBuildFrac;

	public ArrayList<Double> buildingHeights = new ArrayList<Double>();
	public ArrayList<Double> buildingGrounds = new ArrayList<Double>();
	private WritableField buildingHeightsNetCDF;
	private WritableField buildingGroundsNetCDF;
	private WritableDimension unlimetedDimension;

	@SuppressWarnings("unchecked")
	public CityGMLConverterStats(int length, File[] flist, File logNonPlanar,
			File logNoSurfButBuildFrac) {
		notPlanarList = (ArrayList<String>[]) new ArrayList[length];
		noSurfButBuildFracList = (ArrayList<String>[]) new ArrayList[length];
		this.flist = flist;
		this.logNonPlanar = logNonPlanar;
		this.logNoSurfButBuildFrac = logNoSurfButBuildFrac;

		unlimetedDimension = new WritableDimension("counter", 0, true, true,
				false);
		toWrite.add(unlimetedDimension);
	}

	public void addNonPlanar(int id, ArrayList<String> list) {
		notPlanarList[id] = list;
	}
	
	public void addNoSurfButBuildFrac(int id, ArrayList<String> list) {
		noSurfButBuildFracList[id] = list;
	}

	private void writeLog(ArrayList<String>[] list, File log) throws IOException {
		if (log.exists()) {
			log.delete();
		}

		Writer fw = new FileWriter(log);

		for (int i = 0; i < list.length; i++) {
			if (list[i] != null && list[i].size() != 0) {
				fw.append(flist[i].toString());
				fw.append(System.getProperty("line.separator"));
				for (int j = 0; j < list[i].size(); j++) {
					fw.append(list[i].get(j));
					fw.append(System.getProperty("line.separator"));
				}
			}
		}
		fw.close();
	}

	public void writeLogs() throws IOException {
		writeLog(notPlanarList, logNonPlanar);
		writeLog(noSurfButBuildFracList, logNoSurfButBuildFrac);
	}
	
	
	
	private void fillNetCDFVariables() {
		Index ind = buildingHeightsNetCDF.getIndex();
		for (int i = 0; i < buildingHeights.size(); i++) {
			buildingHeightsNetCDF.setDouble(ind.set(i), buildingHeights.get(i));
			buildingGroundsNetCDF.setDouble(ind.set(i), buildingGrounds.get(i));
		}
	}

	@Override
	public List<Dimension> addVariablesToNetCDFfile(NetcdfFileWriteable ncfile) {
		ncfile.addGlobalAttribute("institution", "PIK");

		List<Dimension> dimlist = new ArrayList<Dimension>();
		dimlist.add(unlimetedDimension);
		unlimetedDimension.setLength(buildingHeights.size());
		buildingHeightsNetCDF = new WritableFieldFloat("bheights", dimlist,
				"Building Height", "Building Height", "", "");
		toWrite.add(buildingHeightsNetCDF);
		buildingGroundsNetCDF = new WritableFieldFloat("bgrounds", dimlist,
				"Building Ground Surface size", "Building Ground Surface", "",
				"");
		toWrite.add(buildingGroundsNetCDF);

		return super.addVariablesToNetCDFfile(ncfile);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pik.clminputdata.tools.NetCDFData#writeVariablesToNetCDFfile(ucar.nc2
	 * .NetcdfFileWriteable)
	 */
	@Override
	public void writeVariablesToNetCDFfile(NetcdfFileWriteable ncfile)
			throws IOException, InvalidRangeException {
		fillNetCDFVariables();
		super.writeVariablesToNetCDFfile(ncfile);
	}

}
