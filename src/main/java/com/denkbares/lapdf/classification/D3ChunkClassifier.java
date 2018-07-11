/*
 * Copyright (C) 2013 denkbares GmbH
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package com.denkbares.lapdf.classification;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import edu.isi.bmkeg.lapdf.classification.Classifier;
import edu.isi.bmkeg.lapdf.extraction.exceptions.ClassificationException;
import edu.isi.bmkeg.lapdf.features.ChunkFeatures;
import edu.isi.bmkeg.lapdf.model.ChunkBlock;
import edu.isi.bmkeg.lapdf.model.factory.AbstractModelFactory;

import com.denkbares.lapdf.classification.structures.ChunkStructures;
import com.denkbares.plugin.JPFPluginManager;
import com.denkbares.plugin.test.InitPluginManager;
import de.d3web.core.io.PersistenceManager;
import de.d3web.core.knowledge.KnowledgeBase;
import de.d3web.core.knowledge.TerminologyManager;
import de.d3web.core.knowledge.terminology.Choice;
import de.d3web.core.knowledge.terminology.QuestionNum;
import de.d3web.core.knowledge.terminology.QuestionOC;
import de.d3web.core.knowledge.terminology.QuestionText;
import de.d3web.core.knowledge.terminology.QuestionYN;
import de.d3web.core.knowledge.terminology.Rating;
import de.d3web.core.knowledge.terminology.Solution;
import de.d3web.core.session.Session;
import de.d3web.core.session.SessionFactory;
import de.d3web.core.session.blackboard.Fact;
import de.d3web.core.session.blackboard.FactFactory;
import de.d3web.core.session.values.ChoiceValue;
import de.d3web.core.session.values.NumValue;
import de.d3web.core.session.values.TextValue;
import de.d3web.indication.inference.PSMethodUserSelected;

/**
 * @author Sebastian Furth (denkbares GmbH)
 * @created 11.05.15
 */
public class D3ChunkClassifier implements Classifier<ChunkBlock> {

	private final KnowledgeBase knowledgeBase;
	private final AbstractModelFactory modelFactory;

	public D3ChunkClassifier(KnowledgeBase knowledgeBase, AbstractModelFactory modelFactory) {
		Objects.requireNonNull(knowledgeBase);
		Objects.requireNonNull(modelFactory);
		this.knowledgeBase = knowledgeBase;
		this.modelFactory = modelFactory;
	}

	public D3ChunkClassifier(File knowledgeBaseFile, AbstractModelFactory modelFactory) throws IOException {
		Objects.requireNonNull(knowledgeBaseFile);
		Objects.requireNonNull(modelFactory);
		this.knowledgeBase = loadKnowledgeBase(knowledgeBaseFile);
		this.modelFactory = modelFactory;
	}

	private KnowledgeBase loadKnowledgeBase(File knowledgeBaseFile) throws IOException {
		if (new File("./lib/").exists()) {
			JPFPluginManager.init("./lib/");
		}
		else {
			InitPluginManager.init();
		}
		PersistenceManager persistenceManager = PersistenceManager.getInstance();
		return persistenceManager.load(knowledgeBaseFile);
	}

	@Override
	public void classify(List<ChunkBlock> blockList) throws ClassificationException {

		TerminologyManager manager = knowledgeBase.getManager();
		for (ChunkBlock block : blockList) {
			Session session = SessionFactory.createSession(knowledgeBase);
			try {
				session.getPropagationManager().openPropagation();

				setFacts(block, manager, session);
			}
			finally {

				session.getPropagationManager().commitPropagation();
			}
			setChunkType(block, manager, session);
		}

		// TODO (SF): find most probable sequence of chunk classifications (e.g. using a viterbi style algorithm)
		// TODO: * consider ambiguous classifications
		// TODO: * consider document model, i.e. avoid inconsistencies

	}

	private void setChunkType(ChunkBlock block, TerminologyManager manager, Session session) {
		for (Solution solution : manager.getSolutions()) {
			Rating state = session.getBlackboard().getRating(solution);
			if (state.hasState(Rating.State.ESTABLISHED)) {
				block.setType(solution.getName());
				block.setWasClassified(true);
				return;
			}
		}

		// fallback
		block.setType("docbook:Text");
		block.setWasClassified(false);
	}

	private void setFacts(ChunkBlock block, TerminologyManager manager, Session session) {

		// create ChunkFeatures instance
		ChunkFeatures chunkFeatures = new ChunkFeatures(block, modelFactory);

		// create ChunkStructures instances, TODO: configure with better structure detectors...
//		ChunkStructures chunkStructures = new ChunkStructures.Builder(block).build();
		ChunkStructures chunkStructures = null;

		// <-- Structures -->

		// table
//		double tableConfidence = chunkStructures.isTable();
		double tableConfidence = 0.0;
		QuestionNum table = (QuestionNum) manager.searchQuestion("Is Table? (confidence)");
		NumValue tableValue = new NumValue(tableConfidence);
		Fact fact = FactFactory.createFact(table, tableValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

		// ordered list
//		double orderedListConfidence = chunkStructures.isOrderedList();
		double orderedListConfidence = 0.0;
		QuestionNum orderedList = (QuestionNum) manager.searchQuestion("Is Ordered List? (confidence)");
		NumValue orderedListValue = new NumValue(orderedListConfidence);
		fact = FactFactory.createFact(orderedList, orderedListValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

		// unordered list
//		double unorderedListConfidence = chunkStructures.isUnorderedList();
		double unorderedListConfidence = 0.0;
		QuestionNum unorderedList = (QuestionNum) manager.searchQuestion("Is Unordered List? (confidence)");
		NumValue unorderedListValue = new NumValue(unorderedListConfidence);
		fact = FactFactory.createFact(unorderedList, unorderedListValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

		// <-- Alignment and Position -->

		//Alignment
		String alignment = block.readLeftRightMidLine();
		QuestionOC question = (QuestionOC) manager.searchQuestion("Alignment");
		for (Choice choice : question.getAllAlternatives()) {
			if (choice.getName().equalsIgnoreCase(alignment)) {
				ChoiceValue choiceValue = new ChoiceValue(choice);
				fact = FactFactory.createFact(question, choiceValue, session, PSMethodUserSelected.getInstance());
				session.getBlackboard().addValueFact(fact);
			}
		}

		//Is Aligned with Column Boundaries?
		boolean alignedWithBoundaries = chunkFeatures.isAlignedWithColumnBoundaries();
		QuestionYN aWB = (QuestionYN) manager.searchQuestion("Is Aligned with Column Boundaries?");
		ChoiceValue alignBoundariesValue = new ChoiceValue(alignedWithBoundaries ? aWB.getAnswerChoiceYes() : aWB.getAnswerChoiceNo());
		fact = FactFactory.createFact(aWB, alignBoundariesValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

		//Is Column Centered?
		boolean columnCentered = chunkFeatures.isColumnCentered();
		QuestionYN colCent = (QuestionYN) manager.searchQuestion("Is Column Centered?");
		ChoiceValue columnCenteredValue = new ChoiceValue(columnCentered ? colCent.getAnswerChoiceYes() : colCent.getAnswerChoiceNo());
		fact = FactFactory.createFact(colCent, columnCenteredValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

		//Is Outlier?
		boolean outlier = chunkFeatures.isOutlier();
		QuestionYN outl = (QuestionYN) manager.searchQuestion("Is Outlier?");
		ChoiceValue isOutlierValue = new ChoiceValue(outlier ? outl.getAnswerChoiceYes() : outl.getAnswerChoiceNo());
		fact = FactFactory.createFact(outl, isOutlierValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

		//Is in Top Half?
		boolean topHalf = chunkFeatures.isInTopHalf();
		QuestionYN topHalfQ = (QuestionYN) manager.searchQuestion("Is Outlier?");
		ChoiceValue topHalfValue = new ChoiceValue(topHalf ? outl.getAnswerChoiceYes() : outl.getAnswerChoiceNo());
		fact = FactFactory.createFact(topHalfQ, topHalfValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

		//Number of Line
		double nol = block.readNumberOfLine();
		QuestionNum numOfLine = (QuestionNum) manager.searchQuestion("Number of Line");
		NumValue numOfLineValue = new NumValue(nol);
		fact = FactFactory.createFact(numOfLine, numOfLineValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

		//Is Header or Footer
		Boolean hof = block.isHeaderOrFooter();
		QuestionYN headerOrFooter = (QuestionYN) manager.searchQuestion("Is Header or Footer");
		ChoiceValue headerOrFooterValue = new ChoiceValue(hof != null && hof ? headerOrFooter.getAnswerChoiceYes() : headerOrFooter
				.getAnswerChoiceNo());
		fact = FactFactory.createFact(headerOrFooter, headerOrFooterValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

		//X-Left
		int xl = chunkFeatures.getXCoordsLeft();
		QuestionNum xLeft = (QuestionNum) manager.searchQuestion("X-Left");
		NumValue xLeftValue = new NumValue(xl);
		fact = FactFactory.createFact(xLeft, xLeftValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

		//X-Right
		int xr = chunkFeatures.getXCoordsRight();
		QuestionNum xRight = (QuestionNum) manager.searchQuestion("X-Right");
		NumValue xRightValue = new NumValue(xr);
		fact = FactFactory.createFact(xRight, xRightValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

		//Y-Top
		int yt = chunkFeatures.getYCoordsTop();
		QuestionNum yTop = (QuestionNum) manager.searchQuestion("Y-Top");
		NumValue yTopValue = new NumValue(yt);
		fact = FactFactory.createFact(yTop, yTopValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

		//Y-Bottom
		int yb = chunkFeatures.getYCoordsBottom();
		QuestionNum yBottom = (QuestionNum) manager.searchQuestion("Y-Bottom");
		NumValue yBottomValue = new NumValue(yb);
		fact = FactFactory.createFact(yBottom, yBottomValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

		//Height
		int h = chunkFeatures.getHeight();
		QuestionNum height = (QuestionNum) manager.searchQuestion("Height");
		NumValue heightValue = new NumValue(h);
		fact = FactFactory.createFact(height, heightValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

		//Width
		int w = chunkFeatures.getWidth();
		QuestionNum width = (QuestionNum) manager.searchQuestion("Width");
		NumValue widthValue = new NumValue(w);
		fact = FactFactory.createFact(width, widthValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

		// <-- Font -->

		//Font-Name
		String fName = block.getMostPopularWordFont();
		QuestionText fontName = (QuestionText) manager.searchQuestion("Font Name");
		TextValue fontNameValue = new TextValue(fName);
		fact = FactFactory.createFact(fontName, fontNameValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

		//Most Popular Font Size
		double fSize = chunkFeatures.getMostPopularFontSize();
		QuestionNum fontSize = (QuestionNum) manager.searchQuestion("Most Popular Font Size");
		NumValue fontSizeValue = new NumValue(fSize);
		fact = FactFactory.createFact(fontSize, fontSizeValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

		//Is all Capitals?
		boolean allCapitals = chunkFeatures.isAllCapitals();
		QuestionYN allCap = (QuestionYN) manager.searchQuestion("Is all Capitals?");
		ChoiceValue isAllCapitalsValue = new ChoiceValue(allCapitals ? allCap.getAnswerChoiceYes() : allCap.getAnswerChoiceNo());
		fact = FactFactory.createFact(allCap, isAllCapitalsValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

		//Is most popular font modifier bold?
		boolean bold = chunkFeatures.isMostPopularFontModifierBold();
		QuestionYN isBold = (QuestionYN) manager.searchQuestion("Is most popular font modifier bold?");
		ChoiceValue isBoldValue = new ChoiceValue(bold ? isBold.getAnswerChoiceYes() : isBold.getAnswerChoiceNo());
		fact = FactFactory.createFact(isBold, isBoldValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

		//Is most popular font modifier italic?
		boolean italic = chunkFeatures.isMostPopularFontModifierItalic();
		QuestionYN isItalic = (QuestionYN) manager.searchQuestion("Is most popular font modifier italic?");
		ChoiceValue isItalicValue = new ChoiceValue(italic ? isItalic.getAnswerChoiceYes() : isItalic.getAnswerChoiceNo());
		fact = FactFactory.createFact(isItalic, isItalicValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

		//Is most popular font modifier italic?
		boolean mostPopularFontInDocument = chunkFeatures.isMostPopularFontInDocument();
		QuestionYN isMostPopularFontInDocument = (QuestionYN) manager.searchQuestion("Is most popular font in document?");
		ChoiceValue mostPopularFontInDocumentValue = new ChoiceValue(mostPopularFontInDocument ? isItalic.getAnswerChoiceYes() : isItalic
				.getAnswerChoiceNo());
		fact = FactFactory.createFact(isMostPopularFontInDocument, mostPopularFontInDocumentValue, session, PSMethodUserSelected
				.getInstance());
		session.getBlackboard().addValueFact(fact);

		//Is most popular font modifier italic?
		boolean nextMostPopularFontInDocument = chunkFeatures.isNextMostPopularFontInDocument();
		QuestionYN isNextMostPopularFontInDocument = (QuestionYN) manager.searchQuestion("Is next most popular font in document?");
		ChoiceValue nextMostPopularFontInDocumentValue = new ChoiceValue(nextMostPopularFontInDocument ? isItalic.getAnswerChoiceYes() : isItalic
				.getAnswerChoiceNo());
		fact = FactFactory.createFact(isNextMostPopularFontInDocument, nextMostPopularFontInDocumentValue, session, PSMethodUserSelected
				.getInstance());
		session.getBlackboard().addValueFact(fact);

		// <-- Text in Chunk -->

		//Contains first Line of Page?
		boolean flP = chunkFeatures.isContainingFirstLineOfPage();
		QuestionYN containingFlP = (QuestionYN) manager.searchQuestion("Contains first Line of Page?");
		ChoiceValue flpValue = new ChoiceValue(flP ? containingFlP.getAnswerChoiceYes() : containingFlP.getAnswerChoiceNo());
		fact = FactFactory.createFact(containingFlP, flpValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

		//Contains last Line of Page?
		boolean llP = chunkFeatures.isContainingLastLineOfPage();
		QuestionYN containingllP = (QuestionYN) manager.searchQuestion("Contains last Line of Page?");
		ChoiceValue llpValue = new ChoiceValue(llP ? containingllP.getAnswerChoiceYes() : containingllP.getAnswerChoiceNo());
		fact = FactFactory.createFact(containingllP, llpValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

		//Density
		double documentDensity = block.readDensity();
		QuestionNum density = (QuestionNum) manager.searchQuestion("Density");
		NumValue densityValue = new NumValue(documentDensity);
		fact = FactFactory.createFact(density, densityValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

		//Chunk Text Length
		double cTL = chunkFeatures.getChunkTextLength();
		QuestionNum chunkTLth = (QuestionNum) manager.searchQuestion("Chunk Text Length");
		NumValue chunkTLValue = new NumValue(cTL);
		fact = FactFactory.createFact(chunkTLth, chunkTLValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

		//Height Difference between Chunk Word and Document Word
		double hdiff = chunkFeatures.getHeightDifferenceBetweenChunkWordAndDocumentWord();
		QuestionNum heightDifference = (QuestionNum) manager.searchQuestion("Height Difference between Chunk Word and Document Word");
		NumValue heightDifferenceValue = new NumValue(hdiff);
		fact = FactFactory.createFact(heightDifference, heightDifferenceValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

		//Chunk Text
		String cText = block.readChunkText();
		QuestionText chunkText = (QuestionText) manager.searchQuestion("Chunk Text");
		TextValue chunkTextValue = new TextValue(cText);
		fact = FactFactory.createFact(chunkText, chunkTextValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

		// <-- Document -->

		//Page Number
		int pNum = chunkFeatures.getPageNumber();
		QuestionNum pageNumber = (QuestionNum) manager.searchQuestion("Page Number");
		NumValue pageNumberValue = new NumValue(pNum);
		fact = FactFactory.createFact(pageNumber, pageNumberValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

		// <-- Classification -->
		String lastClassificationText = chunkFeatures.getlastClassification();
		if (lastClassificationText != null) {
			QuestionText lastClassification = (QuestionText) manager.searchQuestion("Last Classification");
			TextValue lastClassificationValue = new TextValue(lastClassificationText);
			fact = FactFactory.createFact(lastClassification, lastClassificationValue, session, PSMethodUserSelected.getInstance());
			session.getBlackboard().addValueFact(fact);
		}
	}
}