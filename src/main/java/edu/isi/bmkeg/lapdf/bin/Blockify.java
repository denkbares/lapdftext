package edu.isi.bmkeg.lapdf.bin;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import edu.isi.bmkeg.lapdf.controller.LapdfEngine;
import edu.isi.bmkeg.lapdf.model.LapdfDocument;
import edu.isi.bmkeg.lapdf.xml.model.LapdftextXMLDocument;
import edu.isi.bmkeg.utils.Converters;
import edu.isi.bmkeg.utils.xml.XmlBindingTools;
import javafx.application.Application;
import javafx.stage.Stage;
import org.apache.log4j.Logger;

public class Blockify extends Application {

	private static Logger logger = Logger.getLogger(Blockify.class);
	
	private static String USAGE = "usage: <input-dir-or-file> [<output-dir>]\n\n"
			+ "<input-dir-or-file> - the full path to the PDF file or directory to be extracted \n"
			+ "<output-dir> (optional or '-') - the full path to the output directory \n\n"
			+ "Running this command on a PDF file or directory will attempt to generate \n"
			+ "one XML document per file with unnannotated text chunks .\n";

	public static void main(String args[]) throws Exception	{
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {

		LapdfEngine engine = new LapdfEngine();

		List<String> parameters = getParameters().getRaw();
		String[] args = parameters.toArray(new String[parameters.size()]);

		if (args.length < 1) {
			System.err.println(USAGE);
			System.exit(1);
		}

		String inputFileOrDirPath = args[0];
		String outputDirPath = "";

		File inputFileOrDir = new File(inputFileOrDirPath);
		if (!inputFileOrDir.exists()) {
			System.err.println(USAGE);
			System.err.println("Input file / dir '" + inputFileOrDirPath
					+ "' does not exist.");
			System.err.println("Please include full path");
			System.exit(1);
		}

		// output folder is set.
		if (args.length > 1) {
			outputDirPath = args[1];
		} else {
			outputDirPath = "-";
		}

		if (outputDirPath.equals("-")) {
			if (inputFileOrDir.isDirectory()) {
				outputDirPath = inputFileOrDirPath;
			} else {
				outputDirPath = inputFileOrDir.getParent();
			}
		}

		File outDir = new File(outputDirPath);
		if (!outDir.exists()) {
			outDir.mkdir();
		}

		if (inputFileOrDir.isDirectory()) {

			Pattern patt = Pattern.compile("\\.pdf$");
			Map<String, File> inputFiles = Converters.recursivelyListFiles(
					inputFileOrDir, patt);

			String[] fileNameArray = inputFiles.keySet().toArray(new String[inputFiles.size()]);
			Arrays.sort(fileNameArray);

			for( int i=0; i<fileNameArray.length; i++) {
				File pdf = inputFiles.get(fileNameArray[i]);
				logger.info("Processing " + pdf.getPath());
				String pdfStem = pdf.getName();
				pdfStem = pdfStem.replaceAll("\\.pdf", "");

				String outXmlPath = Converters.mimicDirectoryStructure(
						inputFileOrDir, outDir, pdf).getPath();
				outXmlPath = outXmlPath.replaceAll("\\.pdf", "_lapdf.xml");

				File outFile = new File(outXmlPath);

				LapdfDocument lapdf = engine.blockifyFile(pdf);
				if( lapdf == null ) {
					logger.info("Parse failed, skipping.");
					continue;
				}
				LapdftextXMLDocument xmlDoc = lapdf
						.convertToLapdftextXmlFormat();
				XmlBindingTools.saveAsXml(xmlDoc, outFile);

				logger.info(outFile.getPath() + " generated.");

			}

		} else {

			String pdfStem = inputFileOrDir.getName();
			pdfStem = pdfStem.replaceAll("\\.pdf", "");

			String outPath = outDir + "/" + pdfStem + "_lapdf.xml";
			File outFile = new File(outPath);

			LapdfDocument lapdf = engine.blockifyFile(inputFileOrDir);
			LapdftextXMLDocument xmlDoc = lapdf.convertToLapdftextXmlFormat();
			XmlBindingTools.saveAsXml(xmlDoc, outFile);

			logger.info(outFile.getPath() + " generated.");

		}

	}

}
