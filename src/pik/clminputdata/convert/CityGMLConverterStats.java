/**
 * 
 */
package pik.clminputdata.convert;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedList;
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
 * Additional information and statistics for a CityGMLConverter run.
 * 
 * @author Sebastian Schubert
 * 
 */
public class CityGMLConverterStats extends NetCDFData {

	/**
	 * List for all non planar polygons
	 */
	private List<LinkedList<String>> notPlanarList = new LinkedList<LinkedList<String>>();
	/**
	 * IDs of the entries in {@code notPlanarList}
	 */
	private List<Integer> notPlanarListID = new LinkedList<Integer>();

	/**
	 * List for cell with no wall surfaces taken into account but building
	 * fraction > 0
	 */
	private List<LinkedList<String>> noSurfButBuildFracList = new LinkedList<LinkedList<String>>();
	/**
	 * IDs of the entries in {@code noSurfButBuildFracList}
	 */
	private List<Integer> noSurfButBuildFracListID = new LinkedList<Integer>();

	/**
	 * List for buildings with no defined wall
	 */
	private List<LinkedList<String>> noWallList = new LinkedList<LinkedList<String>>();
	/**
	 * IDs of the entries in {@code noWallList}
	 */
	private List<Integer> noWallListID = new LinkedList<Integer>();
	/**
	 * List for buildings with no defined roof
	 */
	private List<LinkedList<String>> noRoofList = new LinkedList<LinkedList<String>>();
	/**
	 * IDs of the entries in {@code noRoofList}
	 */
	private List<Integer> noRoofListID = new LinkedList<Integer>();
	/**
	 * List for buildings with no defined ground
	 */
	private List<LinkedList<String>> noGroundList = new LinkedList<LinkedList<String>>();
	/**
	 * IDs of the entries in {@code noGroundList}
	 */
	private List<Integer> noGroundListID = new LinkedList<Integer>();

	/**
	 * All files that are analysed
	 */
	private final File[] flist;

	/**
	 * Configuration of the run
	 */
	private final CityGMLConverterConf conf;

	/**
	 * Height of the buildings
	 */
	public List<Double> buildingHeights = new ArrayList<Double>();
	/**
	 * Ground size of the buildings
	 */
	public List<Double> buildingGrounds = new ArrayList<Double>();

	/**
	 * Field for output
	 */
	private WritableField buildingHeightsNetCDF, buildingGroundsNetCDF;
	/**
	 * Unlimited dimension for output
	 */
	private WritableDimension unlimetedDimension;

	/**
	 * Constructor.
	 * 
	 * @param flist
	 *            List of analysed files
	 * @param conf
	 *            CityGMLConverter configuration
	 */
	public CityGMLConverterStats(File[] flist, CityGMLConverterConf conf) {
		this.conf = conf;
		this.flist = flist;

		unlimetedDimension = new WritableDimension("counter", 0, true, true,
				false);
		toWrite.add(unlimetedDimension);
	}

	/**
	 * Add non planar information.
	 * 
	 * @param id
	 *            Number of file for which to add information
	 * @param list
	 *            List of buildings with non-planar surfaces
	 */
	public void addNonPlanar(int id, LinkedList<String> list) {
		notPlanarListID.add(id);
		notPlanarList.add(list);
	}

	/**
	 * Add no surface but building fraction information.
	 * 
	 * @param id
	 *            Number of file for which to add information
	 * @param list
	 *            List of ground sizes ignored
	 */
	public void addNoSurfButBuildFrac(int id, LinkedList<String> list) {
		noSurfButBuildFracListID.add(id);
		noSurfButBuildFracList.add(list);
	}
	
	/**
	 * Add no wall information.
	 * 
	 * @param id
	 *            Number of file for which to add information
	 * @param list
	 *            List of buildings with no defined wall
	 */
	public void addNoWall(int id, LinkedList<String> list) {
		noWallListID.add(id);
		noWallList.add(list);
	}
	
	/**
	 * Add no roof information.
	 * 
	 * @param id
	 *            Number of file for which to add information
	 * @param list
	 *            List of buildings with no defined roof
	 */
	public void addNoRoof(int id, LinkedList<String> list) {
		noRoofListID.add(id);
		noRoofList.add(list);
	}
	
	/**
	 * Add no ground information.
	 * 
	 * @param id
	 *            Number of file for which to add information
	 * @param list
	 *            List of buildings with no defined ground
	 */
	public void addNoGround(int id, LinkedList<String> list) {
		noGroundListID.add(id);
		noGroundList.add(list);
	}

	private void writeLog(List<LinkedList<String>> list, List<Integer> id,
			File log) throws IOException {
		if (log.exists()) {
			log.delete();
		}

		Writer fw = new FileWriter(log);

		for (int i = 0; i < list.size(); i++) {
			if (list.get(i) != null && list.get(i).size() != 0) {
				fw.append(flist[id.get(i)].toString());
				fw.append(System.getProperty("line.separator"));
				for (int j = 0; j < list.get(i).size(); j++) {
					fw.append(list.get(i).get(j));
					fw.append(System.getProperty("line.separator"));
				}
			}
		}
		fw.close();
	}

	public void writeLogs() throws IOException {
		writeLog(notPlanarList, notPlanarListID, new File(conf.logNonPlanar));
		writeLog(noSurfButBuildFracList, noSurfButBuildFracListID, new File(
				conf.logNoSurfButBuildFrac));
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
