package pik.clminputdata.convert;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;

import pik.clminputdata.tools.PropertiesEnh;

public class CityGMLConverterConf {

	private File confFilename = new File("/home/schubert/Documents/workspace/citygml2clm/converter.properties");

	// variables with default

	// for UrbanCLMConfiguration
	double pollat;
	private static final double pollatDefault = 32.5;

	double pollon;
	private static final double pollonDefault = -170.0;

	double dlat;
	private static final double dlatDefault = 0.008;

	double dlon;
	private static final double dlonDefault = 0.008;

	double startlat_tot;
	private static final double startlat_totDefault = -7.972;

	double startlon_tot;
	private static final double startlon_totDefault = -1.252;

	int ie_tot;
	private static final int ie_totDefault = 51;

	int je_tot;
	private static final int je_totDefault = 51;

	// propably not important
	int ke_tot;
	private static final int ke_totDefault = 20;

	int nuclasses;
	private static final int nuclassesDefault = 1;

	double[] streetdir;
	private static final double streetdirDefault[] = { 0., 90. };

	int[] ke_urban;
	private static final int ke_urbanDefault[] = { 10 };

	double[] height;
	private static final double heightDefault[] = { 0., 5., 10., 15., 20., 25., 30.,
			35., 40., 45. };

	String proj4code = "+init=epsg:3068";
	private static String proj4codeDefault = "+init=epsg:3068";

	double maxbuild_radius;
	private static final double maxbuild_radiusDefault=100.;

	double maxcheck_radius;
	private static final double maxcheck_radiusDefault=100.;
	
	double mindist;
	private static final double mindistDefault=2.;
	
	int nThreads;
	private static int nThreadsDefault = 1;

	int nThreadsQueue;
	private static int nThreadsQueueDefault = 1;

	String inputGMLFolder;
	private static String inputGMLFolderDefault="/home/schubert/Documents/workspace/datasets/gml/";
	
	String outputFolder;
	private static String outputFolderDefault="/home/schubert/";
	
	String logNonPlanar;
	private static String logNonPlanarDefault = "NonPlanar.log";

	String logNoSurfButBuildFrac;
	private static String logNoSurfButBuildFracDefault = "NoSurfButBuildingFrac.log";
	
	String outputFile;
	private static String outputFileDefault = "city.nc";

	String statsFile;
	private static String statsFileDefault = "stats.nc";
	
	public CityGMLConverterConf() throws IOException {
		readConf(false);
	}

	public CityGMLConverterConf(String confFilename) throws IOException {
		this.confFilename = new File(confFilename);
		readConf(true);
	}

	private void readConf(boolean explicitlyGivenFile) throws IOException {
		if (confFilename.exists()) {

			// read the file
			Reader reader = new FileReader(confFilename);
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
			ke_tot = prop.getInt("ke_tot", ke_totDefault);

			nuclasses = prop.getInt("nuclasses", nuclassesDefault);

			streetdir = prop.getDoubleArray("streetdir", streetdirDefault);
			ke_urban = prop.getIntArray("ke_urban", ke_urbanDefault);
			height = prop.getDoubleArray("height", heightDefault);

			proj4code = prop.getString("proj4code", proj4codeDefault);
	
			maxbuild_radius = prop.getDouble("maxbuild_radius", maxbuild_radiusDefault);
			maxcheck_radius = prop.getDouble("maxcheck_radius", maxcheck_radiusDefault);
			mindist = prop.getDouble("mindist", mindistDefault);
			
			nThreads = prop.getInt("nThreads", nThreadsDefault);
			nThreadsQueue = prop.getInt("nThreadsQueue", nThreadsQueueDefault);
			
			inputGMLFolder = prop.getString("inputGMLFolder", inputGMLFolderDefault);
			outputFolder = prop.getString("outputFolder", outputFolderDefault);
			
			logNonPlanar = prop.getString("logNonPlanar", logNonPlanarDefault);
			logNonPlanar = outputFolder + logNonPlanar;
			logNoSurfButBuildFrac = prop.getString("logNoSurfButBuildFrac", logNoSurfButBuildFracDefault);
			logNoSurfButBuildFrac = outputFolder + logNoSurfButBuildFrac;
			
			outputFile = prop.getString("outputFile", outputFileDefault);
			outputFile = outputFolder + outputFile;

			statsFile = prop.getString("statsFile", statsFileDefault);
			statsFile = outputFolder + statsFile;
			
		} else {
			if (explicitlyGivenFile) {
				throw new FileNotFoundException("Configuration file not found");
			}
			System.err
					.println("Configuration file not found, using default configuration");
		}
	}
	
	private Properties getProperties() {
		PropertiesEnh prop = new PropertiesEnh();
		
		prop.setProperty("pollat", pollat);
		prop.setProperty("pollon", pollon);
		prop.setProperty("dlat", dlat);
		prop.setProperty("dlon", dlon);

		prop.setProperty("startlat_tot", startlat_tot);
		prop.setProperty("startlon_tot", startlon_tot);

		prop.setProperty("ie_tot", ie_tot);
		prop.setProperty("je_tot", je_tot);
		prop.setProperty("ke_tot", ke_tot);

		prop.setProperty("nuclasses", nuclasses);

		prop.setProperty("streetdir", streetdir);
		prop.setProperty("ke_urban", ke_urban);
		prop.setProperty("height", height);

		prop.setProperty("maxbuild_radius", maxbuild_radius);
		prop.setProperty("maxcheck_radius", maxcheck_radius);
		prop.setProperty("mindist", mindist);
		
		prop.setProperty("proj4code", proj4code);
		
		prop.setProperty("nThreads", nThreads);
		prop.setProperty("nThreadsQueue", nThreadsQueue);
		
		prop.setProperty("inputGMLFolder", inputGMLFolder);
		prop.setProperty("outputFolder", outputFolder);
		
		prop.setProperty("logNonPlanar", logNonPlanar);
		prop.setProperty("logNoSurfButBuildFrac", logNoSurfButBuildFrac);
		
		prop.setProperty("outputFile", outputFile);
		
		prop.setProperty("statsFile", statsFile);
		
		return prop;
	}
	
	public void writeConf(File file) throws IOException {
		Writer writer = new FileWriter(file);
		getProperties().store(writer, "read configuration");
		writer.close();
	}
	
	public void outputConf() {
		getProperties().list(System.out);
//		write also the arrays
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
		
		System.out.print("streetdir: ");
		for (int i = 0; i < streetdir.length; i++) {
			System.out.print(streetdir[i] + " ");
		}
		System.out.println();
	}
	
}
