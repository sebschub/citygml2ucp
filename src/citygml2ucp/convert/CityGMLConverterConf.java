package citygml2ucp.convert;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;

import citygml2ucp.tools.PropertiesEnh;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriter;

/**
 * Reading and storing of CityGMLConverter configuration.
 * 
 * The configuration is either read from a standard file name or from
 * specifically given file.
 * 
 * @author Sebastian Schubert
 * 
 */
public class CityGMLConverterConf {

	/**
	 * If no filename is given, it is tried to open that file
	 */
	private static String filenameDefault = "/home/schubert/Documents/workspace/citygml2clm/converter.properties";

	// variables with default

	/**
	 * Latitude of rotated pole
	 */
	double pollat;
	private static final double pollatDefault = 32.5;

	/**
	 * Longitude of rotated pole
	 */
	double pollon;
	private static final double pollonDefault = -170.0;

	/**
	 * Grid spacing in meridional direction (rotated latitude)
	 */
	double dlat;
	private static final double dlatDefault = 0.008;

	/**
	 * Grid spacing in zonal direction (rotated longitude)
	 */
	double dlon;
	private static final double dlonDefault = 0.008;

	/**
	 * Lower left latitude of region
	 */
	double startlat_tot;
	private static final double startlat_totDefault = -7.972;

	/**
	 * Lower left longitude of region
	 */
	double startlon_tot;
	private static final double startlon_totDefault = -1.252;

	/**
	 * Total number of grid points in meridional direction (rotated latitude)
	 */
	int ie_tot;
	private static final int ie_totDefault = 51;

	/**
	 * Total number of grid points in zonal direction (rotated latitude)
	 */
	int je_tot;
	private static final int je_totDefault = 51;

	// int ke_tot;
	// private static final int ke_totDefault = 20;

	/**
	 * Number of urban classes (ONLY 1 SUPPORTED AT THE MOMENT)
	 */
	int n_uclass;
	private static final int n_uclassDefault = 1;

	/**
	 * Angles of street direction
	 */
	double[] angle_udir;
	private static final double angle_udirDefault[] = { 0., 90. };

	/**
	 * Number of urban height levels for every urban class
	 */
	int[] ke_uhl;
	private static final int ke_uhlDefault[] = { 10 };

	/**
	 * Urban height levels
	 */
	double[] hhl_uhl;
	private static final double hhl_uhlDefault[] = { 0., 5., 10., 15., 20., 25.,
			30., 35., 40., 45. };

	/**
	 * Input coordinate system for proj4 transformation
	 */
	String proj4code = "+init=epsg:3068";
	private static String proj4codeDefault = "+init=epsg:3068";

	/**
	 * Largest distance of buildings for determination of street width
	 */
	double maxbuild_radius;
	double maxbuild_radius_sq;
	private static final double maxbuild_radiusDefault = 100.;

	/**
	 * Largest distance building can be in the way
	 */
	double maxcheck_radius;
	private static final double maxcheck_radiusDefault = 100.;

	/**
	 * Minimal distance of surface for street width
	 */
	double mindist;
	private static final double mindistDefault = 2.;

	/**
	 * Use effective distance between two surfaces (as they would be just
	 * opposite of each other?)
	 */
	boolean effDist;
	private static final boolean effDistDefault = false;

	/**
	 * Number of maximal parallel threads
	 */
	int nThreads;
	private static int nThreadsDefault = 1;

	/**
	 * Number of buildings per thread
	 */
	int nBuildingsPerThread;
	private static int nBuildingsPerThreadDefault = 1000;

	
	/**
	 * Folder/File of the CityGML data set
	 */
	String inputGMLFolder;
	private static String inputGMLFolderDefault = "/home/schubert/Documents/workspace/datasets/gml/";

	/**
	 * Output folder
	 */
	String outputFolder;
	private static String outputFolderDefault = "/home/schubert/";

	/**
	 * Name of the main output file (relative to outputFolder)
	 */
	String outputFile;
	private static String outputFileDefault = "city.nc";

	/**
	 * Building statistics (relative to outputFolder)
	 */
	String statsFile;
	private static String statsFileDefault = "stats.nc";

	/**
	 * ASCII file which includes the impervious surface
	 */
	String impSurfFile;
	private static String impSurfFileDefault = "/home/schubert/Documents/workspace/datasets/vg";

	boolean impSurfFileNC;
	private static boolean impSurfFileNCDefault = true;
	
	String logFile;
	private static String logFileDefault = "run.log";
	
	/**
	 * Row of latitude in impSurfFile
	 */
	int rowLat;
	private static int rowLatDefault = 2;

	/**
	 * Row of longitude in impSurfFile
	 */
	int rowLon;
	private static int rowLonDefault = 1;

	/**
	 * Row of impervious surface in impSurfFile
	 */
	int rowImpSurf;
	private static int rowImpSurfDefault = 3;
	
	String variableImpSurf;
	private static String variableImpSurfDefault = "fr_urb";

	/**
	 * Number of lines to skip at the at end of the file
	 */
	int skipLines;
	private static int skipLinesDefault = 0;

	/**
	 * String separating the values in impSurfFile
	 */
	String sepString;
	private static String sepStringDefault = ",";

	/**
	 * Output all fields only where urban fraction >= frUrbLimit?
	 */
	boolean consistentOutput;
	private static boolean consistentOutputDefault = true;
	
	/**
	 * If consistentOutput, output only for fr_urb >= frUrbLimit
	 */
	double frUrbLimit;
	private static double frUrbLimitDefault = 0.05;
	
	/**
	 * Debug output
	 */
	boolean debugOutput;
	private static boolean debugOutputDefault = false;

	boolean saveMemory;
	private static boolean saveMemoryDefault = false;

	
	
	List<String> confItems = new LinkedList<String>();
	List<String> confValues = new LinkedList<String>();
		
	/**
	 * Constructor reading from default configuration file.
	 * @throws Exception 
	 */
	public CityGMLConverterConf() throws Exception {
		readConf(new File(filenameDefault), false);
	}

	/**
	 * Constructor.
	 * 
	 * @param confFilename
	 *            Name of configuration file
	 * @throws Exception 
	 */
	public CityGMLConverterConf(String confFilename) throws Exception {
		readConf(new File(confFilename), true);
	}

	/**
	 * Read a configuration file.
	 * 
	 * @param confFile
	 *            Configuration file
	 * @param explicitlyGivenFile
	 *            Explicitly given or from default?
	 * @throws Exception 
	 */
	private void readConf(File confFile, boolean explicitlyGivenFile)
			throws Exception {
		if (confFile.exists()) {

			// read the file
			Reader reader = new FileReader(confFile);
			PropertiesEnh prop = new PropertiesEnh(confItems,confValues);
			prop.load(reader);

			pollat = prop.getDouble("pollat", pollatDefault);
			pollon = prop.getDouble("pollon", pollonDefault);
			dlat = prop.getDouble("dlat", dlatDefault);
			dlon = prop.getDouble("dlon", dlonDefault);

			startlat_tot = prop.getDouble("startlat_tot", startlat_totDefault);
			startlon_tot = prop.getDouble("startlon_tot", startlon_totDefault);

			ie_tot = prop.getInt("ie_tot", ie_totDefault);
			je_tot = prop.getInt("je_tot", je_totDefault);

			n_uclass = prop.getInt("n_uclass", n_uclassDefault);

			angle_udir = prop.getDoubleArray("angle_udir", angle_udirDefault);
			ke_uhl = prop.getIntArray("ke_uhl", ke_uhlDefault);
			hhl_uhl = prop.getDoubleArray("hhl_uhl", hhl_uhlDefault);

			proj4code = prop.getString("proj4code", proj4codeDefault);

			maxbuild_radius = prop.getDouble("maxbuild_radius",
					maxbuild_radiusDefault);
			maxbuild_radius_sq = maxbuild_radius * maxbuild_radius;
			maxcheck_radius = prop.getDouble("maxcheck_radius",
					maxcheck_radiusDefault);
			mindist = prop.getDouble("mindist", mindistDefault);

			effDist = prop.getBoolean("effDist", effDistDefault);

			nThreads = prop.getInt("nThreads", nThreadsDefault);
			nBuildingsPerThread = prop.getInt("nBuildingsPerThread", nBuildingsPerThreadDefault);
			
			inputGMLFolder = prop.getString("inputGMLFolder",
					inputGMLFolderDefault);
			outputFolder = prop.getString("outputFolder", outputFolderDefault);

			logFile = prop.getString("logFile", logFileDefault);
			logFile = outputFolder + logFile;

			outputFile = prop.getString("outputFile", outputFileDefault);
			outputFile = outputFolder + outputFile;

			statsFile = prop.getString("statsFile", statsFileDefault);
			statsFile = outputFolder + statsFile;

			impSurfFileNC = prop.getBoolean("impSurfFileNC", impSurfFileNCDefault);
			impSurfFile = prop.getString("impSurfFile", impSurfFileDefault);

			rowLat = prop.getInt("rowLat", rowLatDefault);
			rowLon = prop.getInt("rowLon", rowLonDefault);
			rowImpSurf = prop.getInt("rowImpSurf", rowImpSurfDefault);

			variableImpSurf = prop.getString("variableImpSurf", variableImpSurfDefault);
			
			skipLines = prop.getInt("skipLines", skipLinesDefault);

			sepString = prop.getString("sepString", sepStringDefault);

			consistentOutput = prop.getBoolean("consistentOutput",
					consistentOutputDefault);
			
			frUrbLimit = prop.getDouble("frUrbLimit",
					frUrbLimitDefault);


			debugOutput = prop.getBoolean("debugOutput",
					debugOutputDefault);

			saveMemory = prop.getBoolean("saveMemory",
					saveMemoryDefault);
			

		} else {
			if (explicitlyGivenFile) {
				throw new FileNotFoundException("Configuration file not found");
			}
			System.err
					.println("Configuration file not found, using default configuration");
		}
	}

	/**
	 * Output of the used configuration.
	 */
	public void outputConf() {

		System.out.println("Configuration of the run");

		for (int i = 0; i < confItems.size(); i++) {
			System.out.println(" " + confItems.get(i)+": " + confValues.get(i));
		}

	}

	public void toNetCDFfile(NetcdfFileWriter ncfile) throws IOException {
		for (int i = 0; i < confItems.size(); i++) {
			ncfile.addGroupAttribute(null, new Attribute(confItems.get(i), confValues.get(i)));
		}
	}
	
}
