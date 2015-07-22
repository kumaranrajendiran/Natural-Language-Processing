package com.restReview.sentimentAnalysis;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.doccat.DocumentSampleStream;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
/**
 * Class used to classify the text specific to a restaurant feature
 * @author kumaran_rajendiran
 *
 */
public class DataClassification {

	/**
	 * Method to generate a maxent model which could be used to classify the text
	 * @param basePath
	 * @throws IOException
	 */
	public void train(String basePath) throws IOException {
		String modelFilePath = basePath + File.separator + "model";
		String trainingDataFilePath = basePath + File.separator
				+ "TrainingModel.txt";
		DoccatModel model = null;

		// Read training data file
		InputStream inputStream = new FileInputStream(trainingDataFilePath);
		// Read each training instance
		ObjectStream<String> lineStream = new PlainTextByLineStream(
				inputStream, "UTF-8");
		ObjectStream<DocumentSample> docSampleStream = new DocumentSampleStream(
				lineStream);
		// Calculate the training model
		model = DocumentCategorizerME.train("en", docSampleStream);
        
		inputStream.close();
		
		OutputStream outputStream = new BufferedOutputStream(
				new FileOutputStream(modelFilePath));
		model.serialize(outputStream);

		outputStream.close();

	}

	/**
	 * Method used to associate the text with one of the available classes
	 * @param baseFolderPath
	 * @param text
	 * @return
	 * @throws InvalidFormatException
	 * @throws IOException
	 */
	public String classifyText(String baseFolderPath, String text)
			throws InvalidFormatException, IOException {
		String classificationModelFilePath = baseFolderPath + File.separator
				+ "model";
		InputStream is = new FileInputStream(classificationModelFilePath);
		DoccatModel classificationModel = new DoccatModel(is);
		DocumentCategorizerME classificationME = new DocumentCategorizerME(
				classificationModel);
		String documentContent = text;
		double[] classDistribution = classificationME
				.categorize(documentContent);
		String predictedCategory = classificationME
				.getBestCategory(classDistribution);
		return predictedCategory;
	}
}
