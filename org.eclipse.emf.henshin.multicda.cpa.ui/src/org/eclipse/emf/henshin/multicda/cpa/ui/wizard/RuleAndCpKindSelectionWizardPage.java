/**
 * <copyright>
 * Copyright (c) 2010-2016 Henshin developers. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 */
package org.eclipse.emf.henshin.multicda.cpa.ui.wizard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.multicda.cpa.InputDataChecker;
import org.eclipse.emf.henshin.multicda.cpa.UnsupportedRuleException;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

import agg.util.Pair;

/**
 * First wizard page for the selection of the rules and whether the set of rules shall be anaylsed for conflict,
 * dependencies or both.
 * 
 * @author Kristopher Born
 *
 */
public class RuleAndCpKindSelectionWizardPage extends WizardPage {

	private Composite containerForBothGroups;
	private Group rulesGroup1;
	private Group rulesGroup2;
	private Button selectAllButton1;
	private Button selectAllButton2;
	private boolean sufficientRulesSelected;
	private HashMap<Rule, String> rulesAndAssociatedFileNames;

	private final class RuleNameComparator implements Comparator<Rule> {
		public int compare(Rule r1, Rule r2) {
			return r1.getName().compareTo(r2.getName());
		}
	}

	private enum CPTypesEnum {
		CONFLICT("Conflicts"), DEPENDENCY("Dependencies");
		public final String name;

		CPTypesEnum(String name) {
			this.name = name;
		}
	};

	EnumSet<CPTypesEnum> selectedCPTypes = EnumSet.noneOf(CPTypesEnum.class);

	/**
	 * Default constructor for this wizard page.
	 * 
	 * @param rulesAndAssociatedFileNames The rules which shall be provided as selection within in the wizard.
	 */
	public RuleAndCpKindSelectionWizardPage(HashMap<Rule, String> rulesAndAssociatedFileNames) {
		super("Precondition");
		setTitle("Conflict and Dependency Analysis - Rule Selection");
		setDescription("Please select the rules you want to check by the Conflict Reason Analysis.");

		this.rulesAndAssociatedFileNames = rulesAndAssociatedFileNames;

		setPageComplete(false);
	}

	/**
	 * A main container ‚containerForBothGroups‘ contains two groups side by side. On the left side the ‘rulesGroup’,
	 * containing a button for each rule to select or deselect it. On the right side the criticalPairKindGroup to select
	 * whether conflicts, dependencies or both shall be analysed.
	 */
	@Override
	public void createControl(Composite parent) {

		ScrolledComposite scrolledComposite = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);

		containerForBothGroups = new Composite(scrolledComposite, SWT.NONE);

		scrolledComposite.setContent(containerForBothGroups);

		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.makeColumnsEqualWidth = true;
		containerForBothGroups.setLayout(layout);
		rulesGroup1 = new Group(containerForBothGroups, SWT.NONE);
		rulesGroup1.setLayout(new GridLayout());
		rulesGroup1.setText("First Rules");
		rulesGroup1.setToolTipText("Select rules that should be executet at first");
		rulesGroup1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Group criticalPairKindGroup = new Group(containerForBothGroups, SWT.TOP);
		criticalPairKindGroup.setLayout(new GridLayout());
		criticalPairKindGroup.setText("Calculate...");
		criticalPairKindGroup.setToolTipText("Select conflict kind to be computed");
		criticalPairKindGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		rulesGroup2 = new Group(containerForBothGroups, SWT.NONE);
		rulesGroup2.setLayout(new GridLayout());
		rulesGroup2.setText("Second Rules");
		rulesGroup2.setToolTipText("Select rules that should be executet at second");
		rulesGroup2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite buttonsComposite = new Composite(containerForBothGroups, SWT.NONE);
		buttonsComposite.setLayout(new GridLayout(1, true));

		selectAllButton1 = new Button(buttonsComposite, SWT.CHECK);
		selectAllButton1.setText("Select all");
		selectAllButton1.addListener(SWT.Selection, selectAllListener1);
		selectAllButton1.addListener(SWT.Selection, checkListener);
		Button b = new Button(buttonsComposite, SWT.NONE);
		b.setVisible(false);

		new Label(containerForBothGroups, SWT.NONE);

		buttonsComposite = new Composite(containerForBothGroups, SWT.NONE);
		buttonsComposite.setLayout(new GridLayout(1, true));

		selectAllButton2 = new Button(buttonsComposite, SWT.CHECK);
		selectAllButton2.setText("Select all");
		selectAllButton2.addListener(SWT.Selection, selectAllListener2);
		selectAllButton2.addListener(SWT.Selection, checkListener);

		Button selectasFirstButton = new Button(buttonsComposite, SWT.NONE);
		selectasFirstButton.setText("Select same as First");
		selectasFirstButton.addListener(SWT.Selection, selectAsFirst);
		selectasFirstButton.addListener(SWT.Selection, checkListener);

		// sort the rules alphabetic
		List<Rule> rulesForSelectionList = new ArrayList<Rule>(rulesAndAssociatedFileNames.keySet());
		Collections.sort(rulesForSelectionList, new RuleNameComparator());

		for (Rule rule : rulesForSelectionList) {
			Button ruleSelectionButton1 = new Button(rulesGroup1, SWT.CHECK);
			String ruleName1 = (rule.getName() == null) ? "null" : rule.getName(); // handle unnamed rules
			ruleSelectionButton1.setText(ruleName1);
			ruleSelectionButton1.setData(rule);
			ruleSelectionButton1.setToolTipText(rulesAndAssociatedFileNames.get(rule));
			ruleSelectionButton1.addListener(SWT.Selection, checkListener);

			Button ruleSelectionButton2 = new Button(rulesGroup2, SWT.CHECK);
			String ruleName2 = (rule.getName() == null) ? "null" : rule.getName(); // handle unnamed rules
			ruleSelectionButton2.setText(ruleName2);
			ruleSelectionButton2.setData(rule);
			ruleSelectionButton2.setToolTipText(rulesAndAssociatedFileNames.get(rule));
			ruleSelectionButton2.addListener(SWT.Selection, checkListener);
		}

		Button conflictAnalysisButton = new Button(criticalPairKindGroup, SWT.CHECK);
		conflictAnalysisButton.setText(CPTypesEnum.CONFLICT.name);
		conflictAnalysisButton.setData(CPTypesEnum.CONFLICT);
		conflictAnalysisButton.addListener(SWT.Selection, calcListener);

		Button dependencyAnalysisButton = new Button(criticalPairKindGroup, SWT.CHECK);
		dependencyAnalysisButton.setText(CPTypesEnum.DEPENDENCY.name);
		dependencyAnalysisButton.setData(CPTypesEnum.DEPENDENCY);
		dependencyAnalysisButton.addListener(SWT.Selection, calcListener);

		scrolledComposite.setMinSize(containerForBothGroups.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		setControl(scrolledComposite);
		setPageComplete(false);
	}

	/**
	 * returns the selection
	 * 
	 * @return returns <code>true</code> for Critical Pairs or <code>false</code> for Dependencies
	 */
	public boolean getComputeConflicts() {
		return selectedCPTypes.contains(CPTypesEnum.CONFLICT);
	}

	public boolean getComputeDependencies() {
		return selectedCPTypes.contains(CPTypesEnum.DEPENDENCY);
	}

	Listener calcListener = new Listener() {
		public void handleEvent(Event event) {
			Button button = (Button) (event.widget);
			CPTypesEnum cpTypeSelection = (CPTypesEnum) button.getData();
			if (button.getSelection()) {
				selectedCPTypes.add(cpTypeSelection);
			} else {
				selectedCPTypes.remove(cpTypeSelection);
			}
			updateFinishButton();
		}
	};

	Listener checkListener = new Listener() {
		public void handleEvent(Event event) {
			boolean selectAll = true;
			for (Control elem : rulesGroup1.getChildren()) {
				if (elem instanceof Button) {
					selectAll &= ((Button) elem).getSelection();
				}
			}
			selectAllButton1.setSelection(selectAll);

			selectAll = true;
			for (Control elem : rulesGroup2.getChildren()) {
				if (elem instanceof Button) {
					selectAll &= ((Button) elem).getSelection();
				}
			}

			selectAllButton2.setSelection(selectAll);

			checkAtLeastOneRuleIsSelected();

			try {
				Pair<Set<Rule>, Set<Rule>> rules = getEnabledRules();
				Set<Rule> allRules=new HashSet<Rule>(rules.first);
				allRules.addAll(rules.second);
				InputDataChecker.getInstance().check(allRules);
				setErrorMessage(null);
			} catch (UnsupportedRuleException e) {
				setErrorMessage(e.getDetailedMessage());
			}
		}
	};

	/**
	 * Listener for the "Select all1" rules button.
	 */
	Listener selectAllListener1 = new Listener() {

		public void handleEvent(Event event) {
			Control[] controlArray = rulesGroup1.getChildren();
			boolean selectAll = ((Button) (event.widget)).getSelection();
			for (Control elem : controlArray) {
				if (elem instanceof Button) {
					((Button) elem).setSelection(selectAll);
				}
			}
			checkAtLeastOneRuleIsSelected();
		}

	};
	/**
	 * Listener for the "Select all2" rules button.
	 */
	Listener selectAllListener2 = new Listener() {

		public void handleEvent(Event event) {
			Control[] controlArray = rulesGroup2.getChildren();
			boolean selectAll = ((Button) (event.widget)).getSelection();
			for (Control elem : controlArray) {
				if (elem instanceof Button) {
					((Button) elem).setSelection(selectAll);
				}
			}
			checkAtLeastOneRuleIsSelected();
		}

	};

	/**
	 * Listener for the "Select all2" rules button.
	 */
	Listener selectAsFirst = new Listener() {

		public void handleEvent(Event event) {
			Control[] controlArray1 = rulesGroup1.getChildren();
			Control[] controlArray2 = rulesGroup2.getChildren();
			for (Control elem1 : controlArray1)
				for (Control elem2 : controlArray2)
					if (elem1 instanceof Button && elem2 instanceof Button)
						if (elem1.getData() == elem2.getData())
							((Button) elem2).setSelection(((Button) elem1).getSelection());
			checkAtLeastOneRuleIsSelected();
		}

	};

	/**
	 * This check ensures, that at least one rule is select before the wizard can be finished and by that the critical
	 * analysis is started.
	 */
	private void checkAtLeastOneRuleIsSelected() {
		sufficientRulesSelected = false;
		int checked1 = 0;
		int checked2 = 0;
		Control[] controlArray1 = rulesGroup1.getChildren();
		Control[] controlArray2 = rulesGroup2.getChildren();

		for (Control elem : controlArray1) {
			if (elem instanceof Button) {
				if (((Button) elem).getSelection()) {
					checked1++;
				}
			}
		}
		for (Control elem : controlArray2) {
			if (elem instanceof Button) {
				if (((Button) elem).getSelection()) {
					checked2++;
				}
			}
		}
		if (checked1 >= 1 && checked2 >= 1)
			sufficientRulesSelected = true;
		updateFinishButton();
	}

	private void updateFinishButton() {
		setPageComplete(false);
		if (sufficientRulesSelected && selectedCPTypes.size() > 0) {
			setPageComplete(true);
		}
		getWizard().getContainer().updateButtons();
	}

	public Pair<Set<Rule>, Set<Rule>> getEnabledRules() {
		Pair<Set<Rule>, Set<Rule>> result = new Pair<Set<Rule>, Set<Rule>>(new HashSet<Rule>(), new HashSet<Rule>());

		Control[] controlArray1 = rulesGroup1.getChildren();
		Control[] controlArray2 = rulesGroup2.getChildren();
		for (Control elem : controlArray1) {
			if (elem instanceof Button) {
				if (((Button) elem).getSelection()) {
					Object data = elem.getData();
					if (data instanceof Rule) {
						result.first.add((Rule) data);
					}
				}
			}
		}
		for (Control elem : controlArray2) {
			if (elem instanceof Button) {
				if (((Button) elem).getSelection()) {
					Object data = elem.getData();
					if (data instanceof Rule) {
						result.second.add((Rule) data);
					}
				}
			}
		}
		return result;
	}
}