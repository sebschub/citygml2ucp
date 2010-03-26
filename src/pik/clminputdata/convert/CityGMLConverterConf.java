package pik.clminputdata.convert;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import pik.clminputdata.tools.PropertiesEnh;

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

	int nuclasses;
	private static final int nuclassesDefault = 1;

	double[] streetdir;
	private static final double streetdirDefault[] = { 0., 90. };

	int[] ke_urban;
	private static final int ke_urbanDefault[] = { 10 };

	double[] height;
	private static final double heightDefault[] = { 0., 5., 10., 15., 20., 25.,
			30., 35., 40., 45. };

	String proj4code = "+init=epsg:3068";
	private static String proj4codeDefault = "+init=epsg:3068";

	double maxbuild_radius;
	double maxbuild_radius_sq;
	private static final double maxbuild_radiusDefault = 100.;

	double maxcheck_radius;
	private static final double maxcheck_radiusDefault = 100.;

	double mindist;
	private static final double mindistDefault = 2.;

	int nThreads;
	private static int nThreadsDefault = 1;

	int nThreadsQueue;
	private static int nThreadsQueueDefault = 1;

	String inputGMLFolder;
	private static String inputGMLFolderDefault = "/home/schubert/Documents/workspace/datasets/gml/";

	String outputFolder;
	private static String outputFolderDefault = "/home/schubert/";

	String logNonPlanar;
	private static String logNonPlanarDefault = "NonPlanar.log";

	String logNoSurfButBuildFrac;
	private static String logNoSurfButBuildFracDefault = "NoSurfButBuildingFrac.log";

	String logNoRoof;
	private static String logNoRoofDefault = "NoRoof.log";

	String logNoWall;
	private static String logNoWallDefault = "NoWall.log";

	String logNoGround;
	private static String logNoGroundDefault = "NoGround.log";

	String outputFile;
	private static String outputFileDefault = "city.nc";

	String statsFile;
	private static String statsFileDefault = "stats.nc";

	String impSurfFile;
	private static String impSurfFileDefault = "/home/schubert/Documents/workspace/datasets/vg";

	int rowLat;
	private static int rowLatDefault = 2;

	int rowLon;
	private static int rowLonDefault = 1;

	int rowImpSurf;
	private static int rowImpSurfDefault = 3;

	int skipLines;
	private static int skipLinesDefault = 0;

	String sepString;
	private static String sepStringDefault = ",";

	public CityGMLConverterConf() throws IOException {
		readConf(new File(filenameDefault), false);
	}

	public CityGMLConverterConf(String confFilename) throws IOException {
		readConf(new File(confFilename), true);
	}

	private void readConf(File confFile, boolean explicitlyGivenFile)
			throws IOException {
		if (confFile.exists()) {

			// read the file
			Reader reader = new FileReader(confFile);
			PropertiesEnh prop = new PropertiesEnh();
			prop.load(reader);

			pollat = prop.getDouble("pollat", pollatDefault);
			pollon = prop.getDouble("pollon", pollonDefault);
			dlat = prop.getDouble("dlat", dlatDefault);
			dlon = prop.getDouble("dlon", dlonDefault);

			startlat_tot = prop.getDouble("startlat_tot", startlat_totDefault);
			startlon_tot = prop.getDouble("startlon_tot", startlon_totDefault);

			ie_tot = prop.getInt("ie_tot", ie_totDefault);
			je_tot = prop.getInt("je_tot", je_totDefault);
//			ke_tot = prop.getInt("ke_tot", ke_totDefault);

			nuclasses = prop.getInt("nuclasses", nuclassesDefault);

			streetdir = prop.getDoubleArray("streetdir", streetdirDefault);
			ke_urban = prop.getIntArray("ke_urban", ke_urbanDefault);
			height = prop.getDoubleArray("height", heightDefault);

			proj4code = prop.getString("proj4code", proj4codeDefault);

			maxbuild_radius = prop.getDouble("maxbuild_radius",
					maxbuild_radiusDefault);
			maxbuild_radius_sq = maxbuild_radius * maxbuild_radius;
			maxcheck_radius = prop.getDouble("maxcheck_radius",
					maxcheck_radiusDefault);
			mindist = prop.getDouble("mindist", mindistDefault);

			nThreads = prop.getInt("nThreads", nThreadsDefault);
			nThreadsQueue = prop.getInt("nThreadsQueue", nThreadsQueueDefault);

			inputGMLFolder = prop.getString("inputGMLFolder",
					inputGMLFolderDefault);
			outputFolder = prop.getString("outputFolder", outputFolderDefault);

			logNonPlanar = prop.getString("logNonPlanar", logNonPlanarDefault);
			logNonPlanar = outputFolder + logNonPlanar;
			logNoSurfButBuildFrac = prop.getString("logNoSurfButBuildFrac",
					logNoSurfButBuildFracDefault);
			logNoSurfButBuildFrac = outputFolder + logNoSurfButBuildFrac;

			logNoGround = prop.getString("logNoGround", logNoGroundDefault);
			logNoGround = outputFolder + logNoGround;
			logNoRoof = prop.getString("logNoRoof", logNoRoofDefault);
			logNoRoof = outputFolder + logNoRoof;
			logNoWall = prop.getString("logNoWall", logNoWallDefault);
			logNoWall = outputFolder + logNoWall;

			outputFile = prop.getString("outputFile", outputFileDefault);
			outputFile = outputFolder + outputFile;

			statsFile = prop.getString("statsFile", statsFileDefault);
			statsFile = outputFolder + statsFile;

			impSurfFile = prop.getString("impSurfFile", impSurfFileDefault);

			rowLat = prop.getInt("rowLat", rowLatDefault);
			rowLon = prop.getInt("rowLon", rowLonDefault);
			rowImpSurf = prop.getInt("rowImpSurf", rowImpSurfDefault);

			skipLines = prop.getInt("skipLines", skipLinesDefault);

			sepString = prop.getString("sepString", sepStringDefault);

		} else {
			if (explicitlyGivenFile) {
				throw new FileNotFoundException("Configuration file not found");
			}
			System.err
					.println("Configuration file not found, using default configuration");
		}
	}

	public void outputConf() {

		System.out.println("CONFIGURATION OF THE RUN");
		System.out.println();

		System.out.println("pollat: " + pollat);
		System.out.println("pollon: " + pollon);
		System.out.println("dlat: " + dlat);
		System.out.println("dlon: " + dlon);

		System.out.println("startlat_tot: " + startlat_tot);
		System.out.println("startlon_tot: " + startlon_tot);

		System.out.println("ie_tot: " + ie_tot);
		System.out.println("je_tot: " + je_tot);
		//		System.out.println("ke_tot: " + ke_tot);

		System.out.println("nuclasses: " + nuclasses);

		System.out.print("streetdir: ");
		for (int i = 0; i < streetdir.length; i++) {
			System.out.print(streetdir[i] + " ");
		}
		System.out.println();
		System.out.print("ke_urban: ");
		for (int i = 0; i < ke_urban.length; i++) {
			System.out.print(ke_urban[i] + " ");
		}
		System.out.println();
		System.out.print("height: ");
		for (int i = 0; i < height.length; i++) {
			System.out.print(height[i] + " ");
		}
		System.out.println();

		System.out.println("maxbuild_radius: " + maxbuild_radius);
		System.out.println("maxcheck_radius: " + maxcheck_radius);
		System.out.println("mindist: " + mindist);

		System.out.println("proj4code: " + proj4code);

		System.out.println("nThreads: " + nThreads);
		System.out.println("nThreadsQueue: " + nThreadsQueue);

		System.out.println("inputGMLFolder: " + inputGMLFolder);
		System.out.println("outputFolder: " + outputFolder);

		System.out.println("logNonPlanar: " + logNonPlanar);
		System.out.println("logNoSurfButBuildFrac: " + logNoSurfButBuildFrac);

		System.out.println("LogNoWall: " + logNoWall);
		System.out.println("LogNoGround: " + logNoGround);
		System.out.println("LogNoRoof: " + logNoRoof);

		System.out.println("outputFile: " + outputFile);

		System.out.println("statsFile: " + statsFile);

		System.out.println("impSurfFile: " + impSurfFile);

		System.out.println("rowLat: " + rowLat);
		System.out.println("rowLon: " + rowLon);
		System.out.println("rowImpSurf: " + rowImpSurf);

		System.out.println("skipLines: " + skipLines);

		System.out.println("sepString: " + sepString);

		System.out.println();
		System.out.println();

	}

}
