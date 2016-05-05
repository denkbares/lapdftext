package edu.isi.bmkeg.lapdf.bin;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import edu.isi.bmkeg.lapdf.controller.LapdfEngine;
import edu.isi.bmkeg.lapdf.model.LapdfDocument;
import edu.isi.bmkeg.lapdf.parser.MaxPowerChunkingClass;
import edu.isi.bmkeg.lapdf.xml.model.LapdftextXMLDocument;
import edu.isi.bmkeg.utils.Converters;
import edu.isi.bmkeg.utils.xml.XmlBindingTools;

public class BlockifyClassify {

	private static String USAGE = "usage: <input-dir-or-file> [<output-dir>] [<rule-file>]\n\n"
			+ "<input-dir-or-file> - the full path to the PDF file or directory to be extracted \n"
			+ "<output-dir> (optional or '-') - the full path to the output directory \n"
			+ "<rule-file> (optional or '-') - the full path to the rule file \n\n"
			+ "Running this command on a PDF file or directory will attempt to generate \n"
			+ "one XML document per file with text chunks annotated with section.\n";

	public static void main(String args[]) throws Exception {

		LapdfEngine engine = new LapdfEngine();

		//NOTE : Here we can set which parser to use
		//Comment the line for using the default parser
		engine.setParser(new MaxPowerChunkingClass());


		if (args.length < 1) {
			System.err.println(USAGE);
			System.exit(1);
		}

		String inputFileOrDirPath = args[0];
		String outputDirPath = "";
		String ruleFilePath = "";

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

		// output folder is set.
		File ruleFile = null;
		if (args.length > 2) {
			ruleFilePath = args[2];
		} else {
			ruleFilePath = "-";
		}

		if (ruleFilePath.equals("-")) {
			ruleFile = Converters
					.extractFileFromJarClasspath(".", "rules/general.drl");
		} else {
			ruleFile = new File(ruleFilePath);
		}

		if (!ruleFile.exists()) {
			System.err.println(USAGE);
			System.err.println(ruleFilePath + " does not exist.");
			System.err.println("Please include full path");
		}

		if (inputFileOrDir.isDirectory()) {

			Pattern patt = Pattern.compile("\\.pdf$");
			Map<String, File> inputFiles = Converters.recursivelyListFiles(
					inputFileOrDir, patt);
			Iterator<String> it = inputFiles.keySet().iterator();
			while (it.hasNext()) {
				String key = it.next();
				File pdf = inputFiles.get(key);
				String pdfStem = pdf.getName();
				pdfStem = pdfStem.replaceAll("\\.pdf", "");

				String outXmlPath = Converters.mimicDirectoryStructure(
						inputFileOrDir, outDir, pdf).getPath();
				outXmlPath = outXmlPath.replaceAll("\\.pdf", "")
						+ "_lapdf.xml";
				File outXmlFile = new File(outXmlPath);

				LapdfDocument lapdf = engine.blockifyFile(pdf);
				engine.classifyDocument(lapdf, ruleFile);
				
				LapdftextXMLDocument xmlDoc = lapdf
						.convertToLapdftextXmlFormat();
				XmlBindingTools.saveAsXml(xmlDoc, outXmlFile);
				
			}

		} else {

			String pdfStem = inputFileOrDir.getName();
			pdfStem = pdfStem.replaceAll("\\.pdf", "");

			String outPath = outDir + "/" + pdfStem + "_lapdf.xml";
			File outXmlFile = new File(outPath);

			LapdfDocument lapdf = engine.blockifyFile(inputFileOrDir);
			engine.classifyDocument(lapdf, ruleFile);
			LapdftextXMLDocument xmlDoc = lapdf
					.convertToLapdftextXmlFormat();
			XmlBindingTools.saveAsXml(xmlDoc, outXmlFile);
			
		}
		
	}
	
}
