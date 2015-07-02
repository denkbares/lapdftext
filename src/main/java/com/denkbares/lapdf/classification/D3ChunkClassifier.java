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

import edu.isi.bmkeg.lapdf.classification.Classifier;
import edu.isi.bmkeg.lapdf.extraction.exceptions.ClassificationException;
import edu.isi.bmkeg.lapdf.features.ChunkFeatures;
import edu.isi.bmkeg.lapdf.model.ChunkBlock;
import edu.isi.bmkeg.lapdf.model.factory.AbstractModelFactory;

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
import de.d3web.plugin.JPFPluginManager;
import de.d3web.plugin.test.InitPluginManager;

/**
 * @author Sebastian Furth (denkbares GmbH)
 * @created 11.05.15
 */
public class D3ChunkClassifier implements Classifier<ChunkBlock> {

	private final KnowledgeBase knowledgeBase;
	private final AbstractModelFactory modelFactory;


	public D3ChunkClassifier(File knowledgeBaseFile, AbstractModelFactory modelFactory) throws IOException {
		this.knowledgeBase = loadKnowledgeBase(knowledgeBaseFile);
		this.modelFactory = modelFactory;
	}

	private KnowledgeBase loadKnowledgeBase(File knowledgeBaseFile) throws IOException {
		if (new File("./lib/").exists()) {
			JPFPluginManager.init("./lib/");
		} else {
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
			setFacts(block, manager, session);
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
			}
		}
	}

	private void setFacts(ChunkBlock block, TerminologyManager manager, Session session) {

		// crate @Link{ChunkFeature} instance
		ChunkFeatures chunkFeatures = new ChunkFeatures(block, modelFactory);

//Alignment and Position

		//Alignment
		String alignment = block.readLeftRightMidLine();
		QuestionOC question = (QuestionOC) manager.searchQuestion("Alignment");
		for (Choice choice : question.getAllAlternatives()) {
			if (choice.getName().equalsIgnoreCase(alignment)) {
				ChoiceValue choiceValue = new ChoiceValue(choice);
				Fact fact = FactFactory.createFact(question, choiceValue, session, PSMethodUserSelected.getInstance());
				session.getBlackboard().addValueFact(fact);
			}
		}

		//Is Aligned with Column Boundaries?
		boolean alignedWithBoundaries = chunkFeatures.isAlignedWithColumnBoundaries();
		QuestionYN aWB = (QuestionYN) manager.searchQuestion("Is Aligned with Column Boundaries?");
		ChoiceValue alignBoundariesValue = alignedWithBoundaries ? new ChoiceValue(aWB.getAnswerChoiceYes()) : new ChoiceValue(aWB.getAnswerChoiceNo());
		Fact fact = FactFactory.createFact(aWB, alignBoundariesValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

		//Is Column Centered?
		boolean columnCentered = chunkFeatures.isColumnCentered();
		QuestionYN colCent = (QuestionYN) manager.searchQuestion("Is Column Centered?");
		ChoiceValue columnCenteredValue = columnCentered ? new ChoiceValue(colCent.getAnswerChoiceYes()) : new ChoiceValue(colCent.getAnswerChoiceNo());
		fact = FactFactory.createFact(colCent, columnCenteredValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

		//Is Outlier?
		boolean outlier = chunkFeatures.isOutlier();
		QuestionYN outl = (QuestionYN) manager.searchQuestion("Is Outlier?");
		ChoiceValue isOutlierValue = outlier ? new ChoiceValue(outl.getAnswerChoiceYes()) : new ChoiceValue(outl.getAnswerChoiceNo());
		fact = FactFactory.createFact(outl, isOutlierValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

		//Number of Line
		double nol = block.readNumberOfLine();
		QuestionNum numOfLine = (QuestionNum) manager.searchQuestion("Number of Line");
		NumValue numOfLineValue = new NumValue(nol);
		fact = FactFactory.createFact(numOfLine, numOfLineValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

		//Is Header or Footer
		boolean hof = chunkFeatures.isOutlier();
		QuestionYN headerOrFooter = (QuestionYN) manager.searchQuestion("Is Header or Footer");
		ChoiceValue headerOrFooterValue = hof ? new ChoiceValue(headerOrFooter.getAnswerChoiceYes()) : new ChoiceValue(headerOrFooter.getAnswerChoiceNo());
		fact = FactFactory.createFact(headerOrFooter, headerOrFooterValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

//Font

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
		ChoiceValue isAllCapitalsValue = allCapitals ? new ChoiceValue(allCap.getAnswerChoiceYes()) : new ChoiceValue(allCap.getAnswerChoiceNo());
		fact = FactFactory.createFact(allCap, isAllCapitalsValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

		//Is most popular font modifier bold?
		boolean bold = chunkFeatures.isMostPopularFontModifierBold();
		QuestionYN isBold = (QuestionYN) manager.searchQuestion("Is most popular font modifier bold?");
		ChoiceValue isBoldValue = bold ? new ChoiceValue(isBold.getAnswerChoiceYes()) : new ChoiceValue(isBold.getAnswerChoiceNo());
		fact = FactFactory.createFact(isBold, isBoldValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

		//Is most popular font modifier italic?
		boolean italic = chunkFeatures.isMostPopularFontModifierBold();
		QuestionYN isItalic = (QuestionYN) manager.searchQuestion("Is most popular font modifier italic?");
		ChoiceValue isItalicValue = italic ? new ChoiceValue(isItalic.getAnswerChoiceYes()) : new ChoiceValue(isItalic.getAnswerChoiceNo());
		fact = FactFactory.createFact(isItalic, isItalicValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

//Text in Chunk

		//Contains first Line of Page?
		boolean flP = chunkFeatures.isContainingFirstLineOfPage();
		QuestionYN containingFlP= (QuestionYN) manager.searchQuestion("Contains first Line of Page?");
		ChoiceValue flpValue = flP ? new ChoiceValue(containingFlP.getAnswerChoiceYes()) : new ChoiceValue(containingFlP.getAnswerChoiceNo());
		fact = FactFactory.createFact(containingFlP, flpValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

		//Contains last Line of Page?
		boolean llP = chunkFeatures.isContainingLastLineOfPage();
		QuestionYN containingllP= (QuestionYN) manager.searchQuestion("Contains last Line of Page?");
		ChoiceValue llpValue = llP ? new ChoiceValue(containingllP.getAnswerChoiceYes()) : new ChoiceValue(containingllP.getAnswerChoiceNo());
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
		TextValue chunkTextValue  = new TextValue(cText);
		fact = FactFactory.createFact(chunkText, chunkTextValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

//Document

		//Page Number
		double pNum = chunkFeatures.getHeightDifferenceBetweenChunkWordAndDocumentWord();
		QuestionNum pageNumber = (QuestionNum) manager.searchQuestion("Page Number");
		NumValue pageNumberValue = new NumValue(pNum);
		fact = FactFactory.createFact(pageNumber, pageNumberValue, session, PSMethodUserSelected.getInstance());
		session.getBlackboard().addValueFact(fact);

	}
}