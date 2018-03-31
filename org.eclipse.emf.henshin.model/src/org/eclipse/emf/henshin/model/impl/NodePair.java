/**
 * 
 */
package org.eclipse.emf.henshin.model.impl;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.henshin.model.GraphElement;
import org.eclipse.emf.henshin.model.Node;

/**
 * @author vincentcuccu
 * 24.03.2018
 */
public class NodePair extends NodeImpl implements GraphElement {

	private Node node1;
	private Node node2;

	/**
	 * 
	 */
	public NodePair() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param name
	 * @param type
	 */
	public NodePair(String name, EClass type) {
		super(name, type);
		// TODO Auto-generated constructor stub
	}
	
	public NodePair(Node node1, Node node2){
		this.setNode1(node1);
		this.setNode2(node2);
	}

	public Node getNode1() {
		return node1;
	}

	public void setNode1(Node node1) {
		this.node1 = node1;
	}

	public Node getNode2() {
		return node2;
	}

	public void setNode2(Node node2) {
		this.node2 = node2;
	}
	

}
