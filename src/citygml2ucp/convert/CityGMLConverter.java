package citygml2ucp.convert;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List; //import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.citygml4j.CityGMLContext;
import org.citygml4j.builder.jaxb.CityGMLBuilder;
import org.citygml4j.model.citygml.CityGML;
import org.citygml4j.model.citygml.CityGMLClass;
import org.citygml4j.model.citygml.core.CityModel;
import org.citygml4j.xml.io.CityGMLInputFactory;
import org.citygml4j.xml.io.reader.CityGMLReader;
import org.proj4.PJ;

import citygml2ucp.configuration.UrbanCLMConfiguration;
import citygml2ucp.tools.GMLFilenameFilter;

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
			uclm.setUrbanFrac(lat, lon, values.get(conf.rowImpSurf - 1) / 100.);
			lScanner.close();
		}
		scanner.close();

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
				conf.angle_udir, conf.ke_uhl, conf.hhl_uhl, conf.confItems, conf.confValues);

		PJ sourcePJ = new PJ(conf.proj4code);
		PJ targetPJ = new PJ("+init=epsg:4326 +latlong");

		// set up citygml4j context
		CityGMLContext ctx = CityGMLContext.getInstance();
		CityGMLBuilder builder = ctx.createCityGMLBuilder();
		CityGMLInputFactory in = builder.createCityGMLInputFactory();

		File[] flist;
		File folder = new File(conf.inputGMLFolder);
		if (folder.isDirectory()) {
			flist = new File(conf.inputGMLFolder)
					.listFiles(new GMLFilenameFilter());
		} else {
			flist = new File[1];
			flist[0] = folder;
		}

		CityGMLConverterStats stats = new CityGMLConverterStats(flist, conf);

		readImpSurfaceFile(conf, uclm);

		CityGMLConverterThread cgmlct = null;
		ThreadPoolExecutor exec = null;
		if (conf.separateFiles) {
			exec = new ThreadPoolExecutor(conf.nThreads,
					conf.nThreads, Long.MAX_VALUE, TimeUnit.MILLISECONDS,
					new LinkedBlockingQueue<Runnable>(conf.nThreadsQueue),
					new ThreadPoolExecutor.CallerRunsPolicy());
			System.out.println("Processing files");
		} else {
			cgmlct = new CityGMLConverterThread(uclm, conf, sourcePJ, targetPJ, stats, df);
			System.out.println("Reading files");
		}
		int flistLengthLength = (int)(Math.log10(flist.length)+1);
		// here loop over citygmlfiles
		for (int i = 0; i < flist.length; i++) {

			File file = flist[i];

			System.out.println(" File " + String.format("%" + flistLengthLength + "d",i + 1) + "/"
					+ flist.length + ": " + file);

			CityGMLReader reader = in.createCityGMLReader(file);
			while (reader.hasNext()) {
				CityGML citygml = reader.nextFeature();

				if (citygml.getCityGMLClass() == CityGMLClass.CITY_MODEL) {
					CityModel cityModel = (CityModel)citygml;

					if (conf.separateFiles) {
						cgmlct = new CityGMLConverterThread(uclm, conf, sourcePJ, targetPJ, stats, df);
					}

					cgmlct.addBuildings(cityModel);
					// everything that is need is now in cgmlct, rest can be deleted
					cityModel = null;

					if (conf.separateFiles) {
						if (conf.nThreads > 1) {
							exec.execute(cgmlct);
						} else {
							cgmlct.run();
						}
					}
				}
			}

			reader.close();

		}

		if (conf.separateFiles) {
			exec.shutdown();
			exec.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		} else {
			cgmlct.run();
		}

		System.out.println("Largest Building: " + df.format(uclm.maxHeight) + " m");
		System.out.println("Smallest Building: " + df.format(uclm.minHeight) + " m");

		uclm.fakeUrbanClassFrac();
		uclm.normBuildingFrac();
		uclm.normBuildProbAndCalcStreetFraction();
		uclm.normStreetWidth();
		uclm.calculateBuildingWidth();
		if (conf.consistentOutput) {
			uclm.defineMissingData();
		}
		if (conf.doHeightReduction) {
			uclm.reduceHeight(conf.heightReductionP);
		}

		stats.writeLogs();
		stats.toNetCDFfile(conf.statsFile);

		long lasted = new Date().getTime() - startTime;
		System.out.printf("Urban parameter calculation took %.3f minutes.%n",
				lasted / 1000. / 60.);

		uclm.toNetCDFfile(conf.outputFile);

	}
}
