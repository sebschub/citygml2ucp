package citygml2ucp.convert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List; //import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.citygml4j.CityGMLContext;
import org.citygml4j.builder.jaxb.CityGMLBuilder;
import org.citygml4j.model.citygml.CityGML;
import org.citygml4j.model.citygml.CityGMLClass;
import org.citygml4j.model.citygml.core.CityModel;
import org.citygml4j.xml.io.CityGMLInputFactory;
import org.citygml4j.xml.io.reader.CityGMLReadException;
import org.citygml4j.xml.io.reader.CityGMLReader;
import org.proj4.PJ;

import citygml2ucp.configuration.UrbanCLMConfiguration;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;
import ucar.nc2.units.DateFormatter;

/**
 * Main programme.
 * 
 * @author Sebastian Schubert
 * 
 */
public class CityGMLConverter {

	/**
	 * Read data of impervious surfaces.
	 * 
	 * @param conf
	 *            Configuration of this run (includes file information)
	 * @param uclm
	 *            Urban configuration to store the data to
	 * @throws IOException
	 *             File from conf not found
	 */
	private static void readImpSurfaceFile(CityGMLConverterConf conf,
			UrbanCLMConfiguration uclm) throws IOException {
		if (conf.impSurfFileNC) {
			NetcdfFile ncfile = NetcdfFile.open(conf.impSurfFile);
			Variable v = ncfile.findVariable(conf.variableImpSurf);
			double[][] data = (double[][]) v.read().copyToNDJavaArray();
			ncfile.close();
			for (int lat = 0; lat < conf.je_tot; lat++) {
				for (int lon = 0; lon < conf.ie_tot; lon++) {
					uclm.setUrbanFrac(lat, lon, data[lat][lon]);
				}
			}
		} else {
			Scanner scanner = new Scanner(new File(conf.impSurfFile));
			for (int i = 0; i < conf.skipLines; i++) {
				if (scanner.hasNextLine()) {
					scanner.nextLine();
				} else {
					scanner.close();
					throw new IOException(
							"Impervious Surface has less lines then skipped at beginning.");
				}
			}
			while (scanner.hasNextLine()) {
				Scanner lScanner = new Scanner(scanner.nextLine());
				lScanner.useDelimiter(conf.sepString);
				List<Double> values = new LinkedList<Double>();
				while (lScanner.hasNext()) {
					values.add(Double.parseDouble(lScanner.next()));
				}
				int lat = uclm.getRLatIndex(values.get(conf.rowLat - 1));
				int lon = uclm.getRLonIndex(values.get(conf.rowLon - 1));
				uclm.setUrbanFrac(lat, lon, values.get(conf.rowImpSurf - 1));
				lScanner.close();
			}
			scanner.close();
		}
	}
	
	/**
	 * Main routine of the converter.
	 * 
	 * @param args
	 *            Path to properties of the run
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {

		DecimalFormat df = new DecimalFormat();
		df.setMinimumFractionDigits(2);
		df.setMaximumFractionDigits(2);
		
		long startTime = new Date().getTime();

		CityGMLConverterConf conf;

		if (args.length == 1) {
			conf = new CityGMLConverterConf(args[0]);
		} else {
			conf = new CityGMLConverterConf();
		}

		conf.outputConf();

		// create new urban configuration
		UrbanCLMConfiguration uclm = new UrbanCLMConfiguration(conf.pollat,
				conf.pollon, conf.dlat, conf.dlon, conf.startlat_tot,
				conf.startlon_tot, conf.ie_tot, conf.je_tot, conf.n_uclass,
				conf.angle_udir, conf.ke_uhl, conf.hhl_uhl);

		PJ sourcePJ = new PJ(conf.proj4code);
		PJ targetPJ = new PJ("+init=epsg:4326 +latlong");

		// set up citygml4j context
		CityGMLContext ctx = CityGMLContext.getInstance();
		CityGMLBuilder builder = ctx.createCityGMLBuilder();
		CityGMLInputFactory in = builder.createCityGMLInputFactory();

		List<Path> paths = new ArrayList<>();
		Path folder = Path.of(conf.inputGMLFolder);
		if (Files.isDirectory(folder)) {
			Files.walk(Paths.get(conf.inputGMLFolder))
					.filter((p) -> p.toFile().getAbsolutePath().toLowerCase().endsWith("gml")
							|| p.toFile().getAbsolutePath().toLowerCase().endsWith("xml"))
					.forEach((p) -> paths.add(p));
			if (paths.size() == 0) {
				System.err.println("No gml or xml files in " + conf.inputGMLFolder + " found");
				System.err.println("Stopping now");
				System.exit(10);
			}
		} else {
			paths.add(folder);
		}
		
		CityGMLConverterStats stats = new CityGMLConverterStats(conf);

		readImpSurfaceFile(conf, uclm);

		CityGMLConverterData cgml = new CityGMLConverterData(uclm, conf, sourcePJ, targetPJ, stats, df);
		System.out.println("Reading files");

		int pathsLengthLength = (int)(Math.log10(paths.size())+1);
		// here loop over citygmlfiles
		for (int i = 0; i < paths.size(); i++) {

			Path file = paths.get(i);

			System.out.println(" File " + String.format("%" + pathsLengthLength + "d",i + 1) + "/"
					+ paths.size() + ": " + file);

			CityGMLReader reader = in.createCityGMLReader(file.toFile());
			while (reader.hasNext()) {
				CityGML citygml;
				// try to read feature and skip if failed
				try{
					citygml = reader.nextFeature();
				} catch ( CityGMLReadException e ) {
					stats.addReadError(file.toString());;
					System.err.println("Cannot read " + file);
					break;
				}

				if (citygml.getCityGMLClass() == CityGMLClass.CITY_MODEL) {
					CityModel cityModel = (CityModel)citygml;

					cgml.addBuildings(cityModel);
					// everything that is need is now in cgmlct, rest can be deleted
					cityModel = null;
				}
			}

			reader.close();
		}
		
		int nChunks;
		int nThreads;
		if (conf.nThreads == 1) {
			nChunks = 1;
			nThreads = 1;
			System.out.println("Visibility calculation using 1 thread");
		} else {
			nChunks = (cgml.buildings.size() + conf.nBuildingsPerThread - 1) / conf.nBuildingsPerThread;
			nThreads = Math.min(conf.nThreads, nChunks);
			System.out.println("Splitting visibility calculation in " + 
					nChunks + " chunk(s) with " + conf.nBuildingsPerThread + " buildings each, using " + 
					nThreads + " thread(s)");
		}

		ExecutorService exec = Executors.newFixedThreadPool(nThreads);
		
		for (int indexChunk = 0; indexChunk < nChunks; indexChunk++) {
			exec.execute(new CityGMLVisibilityRunnable(cgml, indexChunk * conf.nBuildingsPerThread,
					Math.min((indexChunk + 1) * conf.nBuildingsPerThread, cgml.buildings.size()), indexChunk,
					nChunks));
		}
				
		exec.shutdown();
		exec.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		
		cgml.calcStreetProperties();
		
		System.out.println("Largest Building: " + df.format(Collections.max(stats.getBuildingHeights())) + " m");
		System.out.println("Smallest Building: " + df.format(Collections.min(stats.getBuildingHeights())) + " m");

		uclm.fakeUrbanClassFrac();
		uclm.normBuildingFrac();
		uclm.normBuildProbAndCalcStreetFraction();
		uclm.normStreetWidth();
		uclm.calculateBuildingWidth();
		if (conf.consistentOutput) {
			uclm.defineMissingData();
		}
		
		long lasted = new Date().getTime() - startTime;
		System.out.printf("Urban parameter calculation took %.1f minutes%n",
				lasted / 1000. / 60.);
		
		System.out.println("Creating output");
		// text logs
		stats.writeLogs();
		
		// building statistics
		NetcdfFileWriter ncfile = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, conf.statsFile);
		stats.toNetCDFfile(ncfile);
		ncfile.close();
		
		// main output
		ncfile = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, conf.outputFile);
		// Add additional parameters to NetCDF file.
		DateFormatter dfDate = new DateFormatter();
		ncfile.addGroupAttribute(null, new Attribute("creation_date", dfDate.toDateTimeString(new Date())));
		conf.toNetCDFfile(ncfile);
		uclm.toNetCDFfile(ncfile);
		ncfile.close();

		System.out.println("Finished");
	}
}
