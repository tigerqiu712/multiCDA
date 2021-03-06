package org.eclipse.emf.henshin.multicda.cda.unitTest;

import static org.junit.Assert.*;

import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.Unit;
import org.eclipse.emf.henshin.model.resource.HenshinResourceSet;
import org.eclipse.emf.henshin.multicda.cda.ConflictAnalysis;
import org.eclipse.emf.henshin.multicda.cda.Span;
import org.eclipse.emf.henshin.multicda.cda.computation.AtomCandidateComputation;
import org.junit.Before;
import org.junit.Test;

public class ComputeCandidatesTest {

	final String PATH = "testData/refactoring/";
	final String henshinFileName = "refactorings.henshin";

	Rule decapsulateAttributeRule;
	Rule pullUpEncapsulatedAttributeRule;
	
	
	@Before
	public void setUp() throws Exception {
		HenshinResourceSet resourceSet = new HenshinResourceSet(PATH);
		Module module = resourceSet.getModule(henshinFileName, false);
		
		for(Unit unit : module.getUnits()){
			if(unit.getName().equals("decapsulateAttribute"))
				decapsulateAttributeRule = (Rule) unit;
			if(unit.getName().equals("pullUpEncapsulatedAttribute"))
				pullUpEncapsulatedAttributeRule = (Rule) unit;
		}
	}

	@Test
	public void refactoringCandidatesDecapsulateEncapsulateTest() {
		
		AtomCandidateComputation atomicCoreCPA = new AtomCandidateComputation(decapsulateAttributeRule, pullUpEncapsulatedAttributeRule);
		
		List<Span> conflictAtomCandidates = atomicCoreCPA.computeAtomCandidates();
				
		assertEquals(5, conflictAtomCandidates.size());
		
		int amountOfConflictAtomCandidatesOfTypeMethod = 0;
		int amountOfConflictAtomCandidatesOfTypeParameter = 0;
		
		for(Span candidate : conflictAtomCandidates){
			EList<Node> nodesOfCandidate = candidate.getGraph().getNodes();
			assertEquals(1, nodesOfCandidate.size());

			if(nodesOfCandidate.get(0).getType().getName().equals("Method"))
				amountOfConflictAtomCandidatesOfTypeMethod++;
			if(nodesOfCandidate.get(0).getType().getName().equals("Parameter"))
				amountOfConflictAtomCandidatesOfTypeParameter++;
		}
		assertEquals(4, amountOfConflictAtomCandidatesOfTypeMethod);
		assertEquals(1, amountOfConflictAtomCandidatesOfTypeParameter);
	}
}
