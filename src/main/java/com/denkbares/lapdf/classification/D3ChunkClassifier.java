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
import edu.isi.bmkeg.lapdf.model.Block;
import edu.isi.bmkeg.lapdf.model.ChunkBlock;

import de.d3web.core.io.PersistenceManager;
import de.d3web.core.knowledge.KnowledgeBase;
import de.d3web.core.knowledge.TerminologyManager;
import de.d3web.core.knowledge.terminology.QuestionYN;
import de.d3web.core.knowledge.terminology.Rating;
import de.d3web.core.knowledge.terminology.Solution;
import de.d3web.core.session.Session;
import de.d3web.core.session.SessionFactory;
import de.d3web.core.session.blackboard.Fact;
import de.d3web.core.session.blackboard.FactFactory;
import de.d3web.core.session.values.ChoiceValue;
import de.d3web.indication.inference.PSMethodUserSelected;
import de.d3web.plugin.test.InitPluginManager;

/**
 * @author Sebastian Furth (denkbares GmbH)
 * @created 11.05.15
 */
public class D3ChunkClassifier implements Classifier<ChunkBlock> {

	private final KnowledgeBase knowledgeBase;

	public D3ChunkClassifier(File knowledgeBaseFile) throws IOException {
		this.knowledgeBase = loadKnowledgeBase(knowledgeBaseFile);
	}

	private KnowledgeBase loadKnowledgeBase(File knowledgeBaseFile) throws IOException {
		InitPluginManager.init();
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
			if (!state.hasState(Rating.State.UNCLEAR)) {
				block.setType(solution.getName());
			}
		}
	}

	private void setFacts(ChunkBlock block, TerminologyManager manager, Session session) {

		// alignment
		String alignment = block.readLeftRightMidLine();
		Fact fact = null;
		if (alignment.equals(Block.LEFT)) {
			QuestionYN question = (QuestionYN) manager.searchQuestion(" Is Aligned Left?");
			ChoiceValue choiceYes = new ChoiceValue(question.getAnswerChoiceYes());
			fact = FactFactory.createFact(question, choiceYes, session, PSMethodUserSelected.getInstance());
		}
		else if (alignment.equals(Block.RIGHT)) {
			QuestionYN alignedRight = (QuestionYN) manager.searchQuestion(" Is Aligned Right?");
			// TODO...
		}
		else if (alignment.equals(Block.MIDLINE)) {
			QuestionYN alignedMiddle = (QuestionYN) manager.searchQuestion(" Is Aligned Middle?");
			// TODO...
		}
		session.getBlackboard().addValueFact(fact);

		// TODO: position

		// TODO: ...
	}
}
