/**
 * 
 */
package org.eclipse.emf.henshin.multicda.cda.computation;

import java.security.cert.CertPathValidatorException.Reason;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.henshin.model.Action;
import org.eclipse.emf.henshin.model.Edge;
import org.eclipse.emf.henshin.model.Graph;
import org.eclipse.emf.henshin.model.GraphElement;
import org.eclipse.emf.henshin.model.Mapping;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.impl.HenshinFactoryImpl;
import org.eclipse.emf.henshin.model.impl.RuleImpl;
import org.eclipse.emf.henshin.multicda.cda.Pushout;
import org.eclipse.emf.henshin.multicda.cda.Span;
import org.eclipse.emf.henshin.multicda.cda.conflict.DeleteDeleteConflictReason;
import org.eclipse.emf.henshin.multicda.cda.conflict.DeleteReadConflictReason;
import org.eclipse.emf.henshin.multicda.cda.conflict.DeleteUseConflictReason;

/**
 * 
 * @author vincentcuccu 17.12.2017
 */
public class DeleteUseConflictReasonComputation {

	private static final String NODESEPARATOR = "_";
	private static final String INTERSECTIONSEPERATOR = "%";
	private Rule rule1;
	private Rule rule2;
	private Set<Span> conflictReasonsFromR2;
	private MinimalReasonComputation helperForCheckDangling = new MinimalReasonComputation(rule1, rule2);
	private HenshinFactoryImpl helper = new HenshinFactoryImpl();

	/**
	 * constructor
	 * 
	 * @param rule1
	 * @param rule2
	 * @param conflictReasonsFromR2
	 */
	public DeleteUseConflictReasonComputation(Rule rule1, Rule rule2, Set<Span> conflictReasonsFromR2) {
		this.rule1 = rule1;
		this.rule2 = rule2;
		this.conflictReasonsFromR2 = conflictReasonsFromR2;
	}

	/**
	 * constructs all Initial Reasons as candidates for r1 and r2
	 * 
	 * @param conflictReasons
	 * @return result
	 */
	public Set<DeleteUseConflictReason> computeDeleteUseConflictReason(Set<Span> conflictReasons) {
		Set<DeleteUseConflictReason> result = new HashSet<DeleteUseConflictReason>();
		for (Span conflictReason : conflictReasons) {
			computeDeleteUseConflictReasons(conflictReason, result);
		}
		return result;
	}

	/**
	 * the Method to encounter the delete read conflict reasons
	 * 
	 * @param conflictReason
	 * @param result
	 */
	private void computeDeleteUseConflictReasons(Span conflictReason, Set<DeleteUseConflictReason> result) {
		Rule conflictRule2 = conflictReason.getRule2();

		if (computeL1SL2Span(conflictReason, rule2)) {
			Pushout pushout = new Pushout(rule1, conflictReason, conflictRule2);
			if (helperForCheckDangling.findDanglingEdgesOfRule1(rule1, pushout.getRule1Mappings()).isEmpty()
					&& helperForCheckDangling.findDanglingEdgesOfRule1(conflictRule2, pushout.getRule2Mappings())
							.isEmpty()) {
				DeleteUseConflictReason res = new DeleteReadConflictReason(conflictReason);
				result.add(res);
			}
		} else {
			Set<DeleteUseConflictReason> ddset = ConstructDeleteDeleteSet(rule1, rule2, conflictReason);
			if (!ddset.isEmpty()) {
				ddset.forEach(s -> result.add(s));
			}
		}
	}

	/**
	 * returns true, if there is a match from S1 to K2, which is equal to the
	 * match of S1 to lhs of rule 2
	 * 
	 * @param conflictReason
	 * @param rule2
	 * @return boolean
	 */
	public static boolean computeL1SL2Span(Span conflictReason, Rule rule2) {
		Graph s1 = conflictReason.getGraph();
		Action preserve = new Action(Action.Type.PRESERVE);
		EList<Node> l2N = rule2.getActionNodes(preserve);
		EList<Edge> l2E = rule2.getActionEdges(preserve);
		Map<GraphElement, GraphElement> s1tol2 = computeMappings(s1, l2N, l2E);
		boolean empty = s1tol2.isEmpty();
		return !empty;
	}

	/**
	 * @param r2
	 * @param r1
	 * @param sp1
	 * @return
	 */
	private Set<DeleteUseConflictReason> ConstructDeleteDeleteSet(Rule r1, Rule r2, Span sp1) {
		Set<DeleteUseConflictReason> result = new HashSet<>();
		for (Span sp2 : conflictReasonsFromR2) {
			boolean sp2DRCR = computeL1SL2Span(sp2, r1);
			if (sp2DRCR) {
//				 DeleteReadConflictReason deleteReadConflictReason = new
//				 DeleteReadConflictReason(sp2); //TODO Das hier unkommentieren und man hat sogar die Rückrichtung
//				 result.add(deleteReadConflictReason);
			} else {
				Span s = compatibleSpans(sp1, sp2);
				if (s != null) {
					if (!isEmpty(s.getGraph())) {
						Pushout pushout = new Pushout(s.getRule1(), s, s.getRule2());
						Span l1Sl2 = computeL1SL2Span(r1, pushout, r2, s, sp1, sp2);
						Pushout po = new Pushout(r1, l1Sl2, r2);
						if (helperForCheckDangling.findDanglingEdgesOfRule1(r1, po.getRule1Mappings()).isEmpty()
								&& helperForCheckDangling.findDanglingEdgesOfRule1(r2, po.getRule2Mappings())
										.isEmpty()) {
							DeleteDeleteConflictReason res = new DeleteDeleteConflictReason(sp1, sp2);
							result.add(res);
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * @param rule1
	 * @param pushout
	 * @param sap
	 * @param sp2
	 * @param sp1
	 * @return
	 */
	private Span computeL1SL2Span(Rule rule1, Pushout pushout, Rule rule2, Span sap, Span sp1, Span sp2) {
		Span uniqueSpan = null;
		Graph pushoutGraph = pushout.getResultGraph();
		boolean precondition = precondition(sap, sp1, sp2);
		if (precondition) {
			return null;
		}
		Set<Mapping> mappingsInL1 = computeMappingStoL(pushout, rule1, sap, sp1, sp2);
		Set<Mapping> mappingsInL2 = computeMappingStoL(pushout, rule2, sap, sp2, sp1);
		uniqueSpan = new Span(mappingsInL1, pushoutGraph, mappingsInL2);
		uniqueSpan.setRule1(rule1);
		uniqueSpan.setRule2(rule2);
		return uniqueSpan;
	}

	/**
	 * @param pushout
	 * @param sap
	 * @param sp2
	 * @param sp1
	 * @param rule12
	 * @return
	 */
	private Set<Mapping> computeMappingStoL(Pushout pushout, Rule rule, Span sap, Span sp1, Span sp2) {
		HashSet<Mapping> result = new HashSet<Mapping>();
		Graph pushoutGraph = pushout.getResultGraph();
		List<Mapping> s1ToS = pushout.getRule1Mappings();
		List<Mapping> s2ToS = pushout.getRule2Mappings();
		EList<Node> nodes = pushoutGraph.getNodes();
		for (Node s : nodes) {
			Node node = (Node) s;
			Mapping c = getMappingFromSpan(node, s1ToS);
			Mapping d = getMappingFromSpan(node, s2ToS);
			if (c != null) {
				Node s1Element = c.getOrigin();
				Mapping e = null;
				Mapping e1 = sp1.getMappingIntoRule1(s1Element);
				Mapping e2 = sp2.getMappingIntoRule2(s1Element);
				if (e1 != null) {
					e = e1;
				} else if (e2 != null) {
					e = e2;
				} else {
					return null;
				}
				Node lElement = e.getImage();
				Graph lhs = rule.getLhs();
				nodes = lhs.getNodes();
				if (nodes.contains(lElement)) {
					Mapping createMapping = helper.createMapping(node, lElement);
					result.add(createMapping);
				} else {
					for (Node n : nodes) {
						if (n.getName().equals(lElement.getName())) {
							Mapping createMapping = helper.createMapping(node, n);
							result.add(createMapping);
						}
					}
				}
			} else if (d != null) {
				Node s2Element = d.getOrigin();
				Mapping f = null;
				Mapping f1 = sp2.getMappingIntoRule2(s2Element);
				Mapping f2 = sp1.getMappingIntoRule1(s2Element);
				if (f1 != null) {
					f = f1;
				} else if (f2 != null) {
					f = f2;
				} else {
					return null;
				}
				Node lElement = f.getImage();
				Graph lhs = rule.getLhs();
				nodes = lhs.getNodes();
				if (nodes.contains(lElement)) {
					Mapping createMapping = helper.createMapping(node, lElement);
					result.add(createMapping);
				} else {
					for (Node n : nodes) {
						if (n.getName().equals(lElement.getName())) {
							Mapping createMapping = helper.createMapping(node, n);
							result.add(createMapping);
						}
					}
				}
			}
		}

		return result;
	}

	private boolean precondition(Span sap, Span sp1, Span sp2) {
		boolean result = false;
		Graph sapGraph = sap.getGraph();
		EList<Node> sapNodes = sapGraph.getNodes();
		for (Node node : sapNodes) {
			Mapping a = sap.getMappingIntoRule1(node);
			Mapping b = sap.getMappingIntoRule2(node);
			if (a != null && b != null) {
				Node s1Element = a.getImage();
				Node s2Element = b.getImage();
				if (s1Element != null && s2Element != null) {
					Mapping e1 = sp1.getMappingIntoRule1(s1Element);
					Mapping f1 = sp2.getMappingIntoRule2(s2Element);
					Mapping e2 = sp1.getMappingIntoRule2(s1Element);
					Mapping f2 = sp2.getMappingIntoRule1(s2Element);
					if (e1 != null && e2 != null && f1 != null && f2 != null) {
						Node l1eElement = e1.getImage();
						Node l1fElement = f1.getImage();
						Node l2eElement = e2.getImage();
						Node l2fElement = f2.getImage();
						if (l1eElement.equals(l1fElement) && l2eElement.equals(l2fElement)) {
							result = true;
						} else {
							result = false;
						}
					} else {
						result = false;
					}
				} else {
					result = false;
				}
			} else {
				result = false;
			}
		}
		return result;
	}

	/**
	 * @param node
	 * @param mappings
	 * @return
	 */
	private Mapping getMappingFromSpan(Node node, List<Mapping> mappings) {
		for (Mapping mapping : mappings) {
			if (node.equals(mapping.getImage())) {
				return mapping;
			}
		}
		return null;
	}

	/**
	 * @param graph
	 * @return
	 */
	private boolean isEmpty(Graph graph) {
		if (graph.getNodes().isEmpty() && graph.getEdges().isEmpty()) {
			return true;
		}
		return false;
	}

	/**
	 * @param sp2
	 * @param sp1
	 * @return
	 */
	private Span compatibleSpans(Span sp1, Span sp2) {
		Span s1Apostrophe = compatibleElements(sp1, sp2);
		if (s1Apostrophe != null) {
			Span s2Apostrophe = compatibleElements(sp2, sp1);
			if (s2Apostrophe != null) {
				Span intersection = intersection(s1Apostrophe, s2Apostrophe);
				Graph sApostroph = intersection.getGraph();
				if (intersection != null && sApostroph != null) {
					RuleImpl ruleSp1 = new RuleImpl();
					ruleSp1.setLhs(sp1.getGraph());
					RuleImpl ruleSp2 = new RuleImpl();
					ruleSp2.setLhs(sp2.getGraph());
					intersection.setRule1(ruleSp1);
					intersection.setRule2(ruleSp2);
					return intersection;
				}
			} else {
				return null;
			}
		} else {
			return null;
		}
		return null;
	}

	/**
	 * @param graph1
	 * @param graph2
	 * @param sp2
	 * @param sp1
	 * @return
	 */
	private Span intersection(Span sp1, Span sp2) {
		Graph result = helper.createGraph("S'");
		Graph graph1 = sp1.getGraph();
		Graph graph2 = sp2.getGraph();
		EList<Node> s1Nodes = graph1.getNodes();
		EList<Edge> s1Edges = graph1.getEdges();
		EList<Node> s2Nodes = graph2.getNodes();
		EList<Edge> s2Edges = graph2.getEdges();
		Set<Mapping> mappingsIntoSpan1 = new HashSet<Mapping>();
		Set<Mapping> mappingsIntoSpan2 = new HashSet<Mapping>();
		for (Node node1 : s1Nodes) {
			for (Node node2 : s2Nodes) {
				if (checkOriginNodes(node1, node2, INTERSECTIONSEPERATOR)) {
					Node newNode = helper.createNode(result, node1.getType(), node1.getName());
					Mapping mappingFromGraphToRule12 = sp1.getMappingIntoRule1(node1);
					Node mappingFromGraphToRule1 = mappingFromGraphToRule12.getImage();
					Mapping createMapping = helper.createMapping(newNode, mappingFromGraphToRule1);
					mappingsIntoSpan1.add(createMapping);
					Mapping mappingIntoRule2 = sp2.getMappingIntoRule1(node2);
					Node image = mappingIntoRule2.getImage();
					Mapping createMapping2 = helper.createMapping(newNode, image);
					mappingsIntoSpan2.add(createMapping2);
				}
			}
		}
		for (Edge e1 : s1Edges) {
			for (Edge e2 : s2Edges) {
				Node source1 = e1.getSource();
				Node source2 = e2.getSource();
				Node target1 = e1.getTarget();
				Node target2 = e2.getTarget();
				if (checkEdges(e1, e2, INTERSECTIONSEPERATOR)) {
					Node source = null;
					Node target = null;
					EReference type = e1.getType();
					for (Node node : result.getNodes()) {
						if (checkOriginNodes(node, source1, INTERSECTIONSEPERATOR)
								&& checkOriginNodes(node, source2, INTERSECTIONSEPERATOR)) {
							source = node;
						}
						if (checkOriginNodes(node, target1, INTERSECTIONSEPERATOR)
								&& checkOriginNodes(node, target2, INTERSECTIONSEPERATOR)) {
							target = node;
						}
					}
					if (source != null && target != null) {
						helper.createEdge(source, target, type);
					}
				}
			}
		}
		Span span = new Span(mappingsIntoSpan1, result, mappingsIntoSpan2);
		return span;
	}

	/**
	 * @param sp1
	 * @param sp2
	 * @param s1Apostrophe
	 * @return
	 */
	private Span compatibleElements(Span sp1, Span sp2) {
		HenshinFactoryImpl factory = new HenshinFactoryImpl();
		Graph compatibleGraph = factory.createGraph();
		EList<Node> s1Nodes = sp1.getGraph().getNodes();
		Set<Mapping> mappingsIntoSpan1 = new HashSet<Mapping>();
		Set<Mapping> mappingsIntoSpan2 = new HashSet<Mapping>();
		for (Node x : s1Nodes) {
			try {
				GraphElement y = existCompatibleElement(x, sp1, sp2);
				if (y != null) {
					EClass xType = ((Node) x).getType();
					Node xNode = (Node) x;
					Node yNode = (Node) y;
					String newName = xNode.getName() + INTERSECTIONSEPERATOR + yNode.getName();
					Node newNode = factory.createNode(compatibleGraph, xType, newName);
					Mapping createMapping = factory.createMapping(newNode, xNode);
					Mapping createMapping2 = factory.createMapping(newNode, yNode);
					mappingsIntoSpan1.add(createMapping);
					mappingsIntoSpan2.add(createMapping2);
				}
			} catch (NotCompatibleException e) {
				break;
			}
		}
		Span comSpan = new Span(mappingsIntoSpan1, compatibleGraph, mappingsIntoSpan2);
		return comSpan;
	}

	/**
	 * @param x
	 * 
	 * @param sp1
	 * 
	 * @param sp2
	 * 
	 * @param conflictReason2
	 * 
	 * @return
	 * 
	 * @throws Exception
	 */
	private GraphElement existCompatibleElement(Node x, Span sp1, Span sp2) throws NotCompatibleException {
		EList<Node> s1Nodes = sp1.getGraph().getNodes();
		EList<Node> s2Nodes = sp2.getGraph().getNodes();
		if (s1Nodes.contains(x)) {
			for (Node y : s2Nodes) {
				int result = checkEquality(x, y, sp1, sp2);
				if (result == 2) {
					return y;
				} else if (result == 1)
					throw new NotCompatibleException();
			}
			return null;
		} else {
			throw new NotCompatibleException();
		}
	}

	/**
	 * @param x1
	 * @param y1
	 * @param sp1
	 * @param sp2
	 * @return
	 */
	private int checkEquality(Node x, Node y, Span sp1, Span sp2) {
		Mapping s11 = getMappingInRule(x, sp1.getMappingsInRule1());
		Mapping s21 = getMappingInRule(y, sp2.getMappingsInRule2());

		Mapping s12 = getMappingInRule(x, sp1.getMappingsInRule2());
		Mapping s22 = getMappingInRule(y, sp2.getMappingsInRule1());

		if (s11 != null && s21 != null && s12 != null && s22 != null) {
			boolean b1 = s11.getImage().getName().equals(s21.getImage().getName())
					&& s11.getImage().getType() == s21.getImage().getType();
			boolean b2 = s12.getImage().getName().equals(s22.getImage().getName())
					&& s12.getImage().getType() == s22.getImage().getType();
			return (b1 ? 1 : 0) + (b2 ? 1 : 0);
		}
		return 0;
	}

	/**
	 * @param e1
	 * @param e2
	 * @param regex
	 */
	private boolean checkEdges(Edge e1, Edge e2, String regex) {
		EReference e1Type = e1.getType();
		Node e1Source = e1.getSource();
		Node e1target = e1.getTarget();
		EReference e2Type = e2.getType();
		Node e2Source = e2.getSource();
		Node e2target = e2.getTarget();
		if (!e1Type.equals(e2Type)) {
			return false;
		}
		if (!checkOriginNodes(e1Source, e2Source, regex)) {
			return false;
		}
		if (!checkOriginNodes(e1target, e2target, regex)) {
			return false;
		}
		return true;
	}

	/**
	 * @param originNode
	 * @param mappingsInRule
	 * @return Mapping
	 */
	public Mapping getMappingInRule(Node originNode, Set<Mapping> mappingsInRule) {
		for (Mapping mapping : mappingsInRule) {
			if (checkOriginNodes(originNode, mapping.getOrigin(), NODESEPARATOR)) {
				return mapping;
			}
		}
		return null;
	}

	/**
	 * @param originNode
	 * @param origin
	 * @return
	 */
	private boolean checkOriginNodes(Node originNode, Node originNode2, String regex) {
		String[] revert = originNode.getName().split(regex);
		String[] revertMapping = originNode2.getName().split(regex);
		return (revert[0].equals(revertMapping[0]) && revert[1].equals(revertMapping[1])
				|| revert[0].equals(revertMapping[1]) && revert[1].equals(revertMapping[0]))
				&& (originNode.getType().equals(originNode2.getType()));
	}

	/**
	 * computes Mappings of two ELists of Nodes our of two Graphs
	 * 
	 * @param l2e
	 * @param l2n
	 * @param s1
	 * @param conflictReason
	 * @param k
	 * @return
	 */
	private static Map<GraphElement, GraphElement> computeMappings(Graph s1, EList<Node> l2n, EList<Edge> l2e) {
		HashMap<GraphElement, GraphElement> empty = new HashMap<GraphElement, GraphElement>();
		HashMap<GraphElement, GraphElement> result = new HashMap<GraphElement, GraphElement>();

		int n = s1.getNodes().size();
		int e = s1.getEdges().size();
		int checkN = 0;
		int checkE = 0;

		for (Node node : s1.getNodes()) {
			EClass nType = node.getType();
			String nName = node.getName();
			String[] split = nName.split(NODESEPARATOR);
			String searchName = split[1];
			for (Node node2 : l2n) {
				String name = node2.getName();
				EClass type = node2.getType();
				if (name.equals(searchName) && type.equals(nType)) {
					result.put(node, node2);
					checkN += 1;
				}
			}
		}
		if (n != checkN) {
			return empty;
		}
		for (Edge edge : s1.getEdges()) {
			for (Edge edge2 : l2e) {
				Node source = edge.getSource();
				Node source2 = edge2.getSource();
				Node target = edge.getTarget();
				Node target2 = edge2.getTarget();
				if (result.get(source).equals(source2) && result.get(target).equals(target2)) {
					result.put(edge, edge2);
					checkE += 1;
				}
			}
		}
		if (e == checkE) {
			return result;
		} else {
			return empty;
		}
	}
}