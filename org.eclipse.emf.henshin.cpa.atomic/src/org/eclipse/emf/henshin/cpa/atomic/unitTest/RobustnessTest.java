package org.eclipse.emf.henshin.cpa.atomic.unitTest;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.henshin.cpa.atomic.ConflictAnalysis;
import org.eclipse.emf.henshin.cpa.atomic.Span;
import org.eclipse.emf.henshin.cpa.atomic.computation.AtomCandidateComputation;
import org.eclipse.emf.henshin.cpa.atomic.conflict.MinimalConflictReason;
import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.Unit;
import org.eclipse.emf.henshin.model.resource.HenshinResourceSet;
import org.junit.Before;
import org.junit.Test;

public class RobustnessTest {


	final String PATH = "testData/refactoring/";
	final String henshinFileName = "refactorings.henshin";

	Rule decapsulateAttributeRule;
	Rule pullUpEncapsulatedAttributeRule;

	@Before
	public void setUp() throws Exception {
		HenshinResourceSet resourceSet = new HenshinResourceSet(PATH);
		Module module = resourceSet.getModule(henshinFileName, false);

		for (Unit unit : module.getUnits()) {
			if (unit.getName().equals("decapsulateAttribute"))
				decapsulateAttributeRule = (Rule) unit;
			if (unit.getName().equals("pullUpEncapsulatedAttribute"))
				pullUpEncapsulatedAttributeRule = (Rule) unit;
		}
	}


		/*
		 * TODO: alle drei Methoden
		 * conflict atoms? [!]
		 * conflict atom/part candidates?
		 * conflict reason? [!]
		 * 
		 * mit "Null" als �bergabeparameter pr�fen.
		 * Was k�nnten noch invalide Eingaben sein?
		 * Validierung der Regeln, dass es sich bei diesen um g�ltige Regeln handelt.
		 * 		ABER: was ist eine "g�ltige" Regel?
		 * BESSER: die Einschr�nkungen auf Regeln abpr�fen: 
		 * 			- keine MultiRegeln
		 * 			- keine ACs
		 * 			- Attribute ????
		 * 
		 */
	
	
	// TODO: tests for unsupported rules (PAC,NAC, multiRule) with loaded rules -> set up unsupported rules	
	
	
	//TODO: the Exception checks need to be executed on the ConflictAnalysis constructor
	@Test
	public void computeConflictAtomsNotNullTest() {		
		ConflictAnalysis atomicCoreCPA = new ConflictAnalysis(null, pullUpEncapsulatedAttributeRule);
		boolean illeagalArgumentExceptionThrownOnRule1 = false;
		boolean illeagalArgumentExceptionThrownOnRule2 = false;
		try {			
			atomicCoreCPA.computeConflictAtoms();
		} catch (IllegalArgumentException e) {
			illeagalArgumentExceptionThrownOnRule1 = true;
		}
		try {			
			atomicCoreCPA.computeConflictAtoms();
		} catch (IllegalArgumentException e) {
			illeagalArgumentExceptionThrownOnRule2 = true;
		}
		assertTrue("A IllegalArgumentException where expected but hadnt been thrown.", illeagalArgumentExceptionThrownOnRule1);
		assertTrue("A IllegalArgumentException where expected but hadnt been thrown.", illeagalArgumentExceptionThrownOnRule2);
	}	
	
	
	@Test
	public void computeConflictPartCandidatesNotNullTest() {		
		AtomCandidateComputation candComp = new AtomCandidateComputation(null,
				pullUpEncapsulatedAttributeRule);
		boolean illeagalArgumentExceptionThrownOnRule1 = false;
		boolean illeagalArgumentExceptionThrownOnRule2 = false;
		try {					
			candComp.computeAtomCandidates();
		} catch (IllegalArgumentException e) {
			illeagalArgumentExceptionThrownOnRule1 = true;
		}
		try {	
			candComp.computeAtomCandidates();
		} catch (IllegalArgumentException e) {
			illeagalArgumentExceptionThrownOnRule2 = true;
		}
		assertTrue("A IllegalArgumentException where expected but hadnt been thrown.", illeagalArgumentExceptionThrownOnRule1);
		assertTrue("A IllegalArgumentException where expected but hadnt been thrown.", illeagalArgumentExceptionThrownOnRule2);
	}
	

	@Test
	public void computeMinimalReasonNotNullTest() {		
		ConflictAnalysis atomicCoreCPA = new ConflictAnalysis(pullUpEncapsulatedAttributeRule,
				decapsulateAttributeRule);
		boolean illeagalArgumentExceptionThrownOnRule1 = false;
		boolean illeagalArgumentExceptionThrownOnRule2 = false;
		
		
		AtomCandidateComputation candComp = new AtomCandidateComputation(pullUpEncapsulatedAttributeRule,
				decapsulateAttributeRule);
		List<Span> conflictAtomCandidates = candComp.computeAtomCandidates();
		Set<MinimalConflictReason> reasons = new HashSet<>();//
		for (Span candidate : conflictAtomCandidates) {
			try {		
				atomicCoreCPA.computeMinimalConflictReasons(candidate,
						reasons);			 
			} catch (IllegalArgumentException e) {
				illeagalArgumentExceptionThrownOnRule1 = true;
			}
			try {	

				atomicCoreCPA.computeMinimalConflictReasons(candidate,
						reasons);
			} catch (IllegalArgumentException e) {
				illeagalArgumentExceptionThrownOnRule2 = true;
			}
		}
		assertTrue("A IllegalArgumentException where expected but hadnt been thrown.", illeagalArgumentExceptionThrownOnRule1);
		assertTrue("A IllegalArgumentException where expected but hadnt been thrown.", illeagalArgumentExceptionThrownOnRule2);
	}
	
}
