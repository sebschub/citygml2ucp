/**
 * 
 */
package citygml2ucp.convert;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import citygml2ucp.tools.DimensionsAndVariables;
import citygml2ucp.tools.NetCDFData;
import citygml2ucp.tools.WritableDimension;
import citygml2ucp.tools.WritableField;
import citygml2ucp.tools.WritableFieldFloat;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFileWriter;

/**
 * Additional information and statistics for a CityGMLConverter run.
 * 
 * @author Sebastian Schubert
 * 
 */
public class CityGMLConverterStats extends NetCDFData {

	/**
	 * Map for all invalid polygons
	 */
	private Map<String,List<String>> invalid = new HashMap<>();
	
	/**
	 * Map for all non planar polygons
	 */
	private Map<String,List<String>> nonPlanar = new HashMap<>();

	/**
	 * Map for surfaces without distances taken into account
	 */
	private Map<String,List<String>> surfaceWithoutDistance = new HashMap<>();

	/**
	 * List for files with read error
	 */
	private List<String> readErrorList = new LinkedList<>();

	/**
	 * List for files with ignored buildingParts
	 */
	private List<String> ignoredBuildingPartList = new LinkedList<>();
	
	/**
	 * List for buildings with no defined wall
	 */
	private List<String> noWallList = new LinkedList<>();

	/**
	 * List for buildings with no defined roof
	 */
	private List<String> noRoofList = new LinkedList<>();
	
	/**
	 * List for buildings with no defined ground
	 */
	private List<String> noGroundList = new LinkedList<>();

	/**
	 * Configuration of the run
	 */
	private final CityGMLConverterConf conf;

	/**
	 * Height of the buildings
	 */
	private List<Double> buildingHeights = new ArrayList<Double>();

	/**
	 * Ground size of the buildings
	 */
	private List<Double> buildingGrounds = new ArrayList<Double>();

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
	 * @param conf
	 *            CityGMLConverter configuration
	 */
	public CityGMLConverterStats(CityGMLConverterConf conf) {
		this.conf = conf;

		unlimetedDimension = new WritableDimension("counter", 0, true, true,
				false);
		toWrite.add(unlimetedDimension);
	}

	
	private void addMapElements(Map<String, List<String>> map, String key, String value) {
		List<String> list;
		if (map.containsKey(key)) {
			list = map.get(key);
			// put surface id that includes the faulty polygon in the list
			list.add(value);
		} else {
			list = new LinkedList<>();
			// put surface id that includes the faulty polygon in the list
			list.add(value);
			// put list into Map
			map.put(key, list);
		}
	}
	
	/**
	 * Add invalid polygon information.
	 * @param buildingId
	 * @param surfaceId
	 */
	public void addInvalid(String buildingId, String surfaceId) {
		addMapElements(invalid, buildingId, surfaceId);
	}
	
	/**
	 * Add non planar information.
	 * 
	 * @param nonPlanar
	 *            Map of building id and list of non-planar surface ids
	 */
	public void addNonPlanar(String buildingId, String surfaceId) {
		addMapElements(nonPlanar, buildingId, surfaceId);
	}

	/**
	 * Add no surface but building fraction information.
	 * 
	 * @param id
	 *            Number of file for which to add information
	 * @param list
	 *            List of ground sizes ignored
	 */
	public void addSurfaceWithoutDistance(String buildingId, String surfaceId) {
		addMapElements(surfaceWithoutDistance, buildingId, surfaceId);
	}

	/**
	 * Add read error information.
	 * 
	 * @param readError
	 *            String of file with read error
	 */
	public void addReadError(String readError) {
		readErrorList.add(readError);
	}

	/**
	 * Add ignored buildingPart information.
	 * 
	 * @param buildingId
	 *            String of building Id
	 */
	public void addIgnoredBuildingPart(String buildingId) {
		ignoredBuildingPartList.add(buildingId);
	}
	
	
	/**
	 * Add no wall information.
	 * 
	 * @param noWall
	 *            List of building IDs with no defined wall
	 */
	public void addNoWall(String noWall) {
		noWallList.add(noWall);
	}
	
	/**
	 * Add no roof information.
	 * 
	 * @param noRoof
	 *            List of building IDs with no defined roof
	 */
	public void addNoRoof(String noRoof) {
		noRoofList.add(noRoof);
	}
	
	/**
	 * Add no ground information.
	 * 
	 * @param noground
	 *            List of building IDs with no defined ground
	 */
	public void addNoGround(String noGround) {
		noGroundList.add(noGround);
	}

	
	public List<Double> getBuildingHeights() {
		return buildingHeights;
	}


	public void addBuildingHeight(double buildingHeight) {
		this.buildingHeights.add(buildingHeight);
	}

	
	public List<Double> getBuildingGrounds() {
		return buildingGrounds;
	}


	public void addBuildingGround(Double buildingGround) {
		this.buildingGrounds.add(buildingGround);
	}

	
	private void writeStringList(Writer fw, String header, List<String> list) throws IOException {
		fw.append(header);
		fw.append(System.getProperty("line.separator"));
		for (String element : list) {
			fw.append(" " + element);
			fw.append(System.getProperty("line.separator"));
		}
	}

	private void writeStringMap(Writer fw, String header, Map<String,List<String>> map) throws IOException {
		fw.append(header);
		fw.append(System.getProperty("line.separator"));
		
		for (Map.Entry<String, List<String>> entry : map.entrySet()) {
			fw.append(" Building " + entry.getKey());
			fw.append(System.getProperty("line.separator"));

			for (String elementOfElement : entry.getValue()) {
				fw.append("  Surface " + elementOfElement);
				fw.append(System.getProperty("line.separator"));
			}
		}
	}

	
	public void writeLogs() throws IOException {
		File log = new File(conf.logFile);
		
		if (log.exists()) {
			log.delete();
		}
		
		Writer fw = new FileWriter(log);
		
		writeStringList(fw, "* Files with read error", readErrorList);
		writeStringList(fw, "* Building with ignored building parts", ignoredBuildingPartList);
		writeStringList(fw, "* Building without roofs", noRoofList);
		writeStringList(fw, "* Building without walls", noWallList);
		writeStringList(fw, "* Building without grounds", noGroundList);
	
		writeStringMap(fw, "* Surfaces with invalid polygons", invalid);
		writeStringMap(fw, "* Non-Planar surface", nonPlanar);
		writeStringMap(fw, "* Surface without distance", surfaceWithoutDistance);

		fw.close();
	}

	private void fillNetCDFVariables() {
		Index ind = buildingHeightsNetCDF.getIndex();
		for (int i = 0; i < buildingHeights.size(); i++) {
			buildingHeightsNetCDF.setDouble(ind.set(i), buildingHeights.get(i));
			buildingGroundsNetCDF.setDouble(ind.set(i), buildingGrounds.get(i));
		}
	}

	@Override
	public DimensionsAndVariables addToNetCDFfile(NetcdfFileWriter ncfile) {
		List<WritableDimension> dimlist = new ArrayList<>();
		dimlist.add(unlimetedDimension);
		unlimetedDimension.setLength(buildingHeights.size());
		buildingHeightsNetCDF = new WritableFieldFloat("bheights", dimlist,
				"Building Height", "Building Height", "", "");
		toWrite.add(buildingHeightsNetCDF);
		buildingGroundsNetCDF = new WritableFieldFloat("bgrounds", dimlist,
				"Building Ground Surface size", "Building Ground Surface", "",
				"");
		toWrite.add(buildingGroundsNetCDF);

		return super.addToNetCDFfile(ncfile);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * citygml2ucp.tools.NetCDFData#writeVariablesToNetCDFfile(ucar.nc2
	 * .NetcdfFileWriteable)
	 */
	@Override
	public void writeToNetCDFfile(NetcdfFileWriter ncfile)
			throws IOException, InvalidRangeException {
		fillNetCDFVariables();
		super.writeToNetCDFfile(ncfile);
	}

}
