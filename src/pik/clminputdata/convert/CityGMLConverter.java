package pik.clminputdata.convert;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List; //import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement; //import javax.xml.bind.Marshaller;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import javax.xml.parsers.ParserConfigurationException;
import org.proj4.Proj4;
import org.xml.sax.SAXException;
import org.citygml4j.CityGMLContext;
import org.citygml4j.factory.CityGMLFactory;
import org.citygml4j.model.citygml.core.CityModel;

import pik.clminputdata.configuration.UrbanCLMConfiguration;
import pik.clminputdata.tools.GMLFilenameFilter;
import ucar.ma2.InvalidRangeException;

public class CityGMLConverter {

	private static void readImpSurfaceFile(CityGMLConverterConf conf,
			UrbanCLMConfiguration uclm) throws IOException {
		Scanner scanner = new Scanner(new File(conf.impSurfFile));
		for (int i = 0; i < conf.skipLines; i++) {
			if (scanner.hasNextLine()) {
				scanner.nextLine();
			} else {
				throw new IOException(
						"Impervious Surface has less lines then skipped at beginning.");
			}
		}
		while (scanner.hasNextLine()) {
			Scanner lScanner = new Scanner(scanner.nextLine())
					.useDelimiter(conf.sepString);
			List<Double> values = new LinkedList<Double>();
			while (lScanner.hasNextDouble()) {
				values.add(lScanner.nextDouble());
			}
			int lat = uclm.getRLatIndex(values.get(conf.rowLat - 1));
			int lon = uclm.getRLonIndex(values.get(conf.rowLon - 1));
			uclm.setUrbanFrac(lat, lon, values.get(conf.rowImpSurf - 1) / 100.);
			lScanner.close();
		}
		scanner.close();

	}

	static List<CityGMLConverterThread> lThreads = new LinkedList<CityGMLConverterThread>();

	/**
	 * @param args
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws JAXBException
	 * @throws InvalidRangeException
	 * @throws IllegalArgumentException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException, SAXException,
			ParserConfigurationException, JAXBException,
			IllegalArgumentException, InvalidRangeException,
			InterruptedException {

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
				conf.startlon_tot, conf.ie_tot, conf.je_tot, conf.nuclasses,
				conf.streetdir, conf.ke_urban, conf.height);

		Proj4 soldner = new Proj4(conf.proj4code, "+init=epsg:4326 +latlong");

		// set up citygml4j context
		CityGMLContext ctx = new CityGMLContext();
		CityGMLFactory citygml = ctx.createCityGMLFactory();

		// create a JAXBContext, an Unmarshaller and unmarshal input file
		JAXBContext jaxbCtx = ctx.createJAXBContext();

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

		ThreadPoolExecutor exec = new ThreadPoolExecutor(conf.nThreads,
				conf.nThreads, Long.MAX_VALUE, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>(conf.nThreadsQueue),
				new ThreadPoolExecutor.CallerRunsPolicy());

		// here loop over citygmlfiles
		for (int i = 0; i < flist.length; i++) {

			File file = flist[i];

			System.out.println("Processing file " + (i + 1) + "/"
					+ flist.length + ": " + file);

			// unmarshaller has to be in loop, otherwise memory is not freed
			// (reference in unmarshaller?)

			Unmarshaller um = jaxbCtx.createUnmarshaller();
			JAXBElement<?> featureElem = (JAXBElement<?>) um.unmarshal(file);

			// map the JAXBElement class to the citygml4j object model
			CityModel cityModel = (CityModel) citygml.jaxb2cityGML(featureElem);

			CityGMLConverterThread cgmlct = new CityGMLConverterThread(uclm,
					conf, soldner, stats, cityModel, i, file.toString());

			// everything that is need is now in cgmlct, rest can be deleted
			cityModel = null;
			featureElem = null;
			um = null;

			if (conf.nThreads > 1) {
				exec.execute(cgmlct);
			} else {
				cgmlct.run();
			}
		}

		exec.shutdown();
		exec.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

		System.out.println("Largest Building: " + uclm.maxHeight + " m");
		System.out.println("Smallest Building: " + uclm.minHeight + " m");

		uclm.fakeUrbanClassFrac();
		uclm.normBuildingFrac();
		uclm.normBuildProbAndCalcStreetFraction();
		uclm.normStreetWidth();
		uclm.calculateBuildingWidth();
		if (conf.consistentOutput) {
			uclm.defineMissingData();
		}
		
		uclm.toNetCDFfile(conf.outputFile);

		stats.writeLogs();
		stats.toNetCDFfile(conf.statsFile);

		long lasted = new Date().getTime() - startTime;

		System.out
				.println("Program took " + lasted / 1000. / 60. + " minutes.");

	}
}
