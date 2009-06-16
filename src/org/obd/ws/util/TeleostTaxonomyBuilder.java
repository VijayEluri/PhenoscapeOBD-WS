package org.obd.ws.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.bbop.dataadapter.DataAdapterException;
import org.obo.dataadapter.OBOAdapter;
import org.obo.dataadapter.OBOFileAdapter;
import org.obo.datamodel.Link;
import org.obo.datamodel.OBOClass;
import org.obo.datamodel.OBOSession;
import org.obo.util.TermUtil;

/**
 * @author cartik
 * @PURPOSE The purpose of this class is to recreate the hierarchy of 
 * teleost species in a data structure. In addition, it includes a method
 * to determine the Most Recent Common Ancestor (MRCA) of any set of taxa 
 */

public class TeleostTaxonomyBuilder {
	
	private Map<OBOClass, Set<OBOClass>> parentToChildrenMap;
	private Map<OBOClass, OBOClass> childToParentMap;

	private Set<OBOClass> roots;
	private Set<OBOClass> leaves;
	
	private Map<String, OBOClass> idToClassMapper; 

	/*
	 * GETTER methods for instance variables
	 */
	public Set<OBOClass> getRoots() {
		return roots;
	}

	public Set<OBOClass> getLeaves() {
		return leaves;
	}
	
	public Map<OBOClass, Set<OBOClass>> getParentToChildrenMap() {
		return parentToChildrenMap;
	}
	
	public Map<OBOClass, OBOClass> getChildToParentMap(){
		return childToParentMap;
	}
	
	public Map<String, OBOClass> getIdToClassMapper(){
		return idToClassMapper;
	}
	
	/**
	 * The default constructor reads in the ontology from the 
	 * URL and stores it in a file. It also intializes the
	 * instance variables
	 * @throws IOException
	 * @throws DataAdapterException 
	 */
	public TeleostTaxonomyBuilder() throws IOException, DataAdapterException{
		parentToChildrenMap = new HashMap<OBOClass, Set<OBOClass>>();
		childToParentMap = new HashMap<OBOClass, OBOClass>();
		roots = new HashSet<OBOClass>();
		leaves = new HashSet<OBOClass>();
		idToClassMapper = new HashMap<String, OBOClass>();
		
		URL ttoURL = new URL("http://www.berkeleybop.org/ontologies/obo-all/teleost_taxonomy/teleost_taxonomy.obo");
		BufferedReader br = new BufferedReader(new InputStreamReader(ttoURL.openStream()));
		
		String line; 
		File ttoFile = new File("teleost_taxonomy.obo");
		
		BufferedWriter pw = new BufferedWriter(new FileWriter(ttoFile));
		
		while((line = br.readLine()) != null){
			pw.write(line + "\n");
		}
		
		pw.flush();
		pw.close();
		
		setUpTaxonomy();
	}
	
	/**
	 * This method reads in the TTO and arranges the terms in 
	 * a taxonomy. In addition, it also adds all the terms to 
	 * a map from ID to the OBOClass
	 * @throws DataAdapterException 
	 * @throws IOException 
	 */
	public void setUpTaxonomy() throws DataAdapterException, IOException{
		
		List<String> paths = new ArrayList<String>();
		paths.add("teleost_taxonomy.obo");
		
		//setting up the file adapter and its configuration
		final OBOFileAdapter fileAdapter = new OBOFileAdapter();
		OBOFileAdapter.OBOAdapterConfiguration config = new OBOFileAdapter.OBOAdapterConfiguration();
	    config.setReadPaths(paths);
	    config.setBasicSave(false);
	    config.setAllowDangling(true);
	    config.setFollowImports(false);
	    
	    //read in the terms into the session
	    OBOSession session = 
	    	fileAdapter.doOperation(OBOAdapter.READ_ONTOLOGY, config, null);
	    	    
	    for(OBOClass oboClass : TermUtil.getTerms(session)){
	    	if(oboClass.getNamespace() != null && !oboClass.isObsolete() &&
	    			oboClass.getID().matches("TTO:[0-9]+")){
	    		idToClassMapper.put(oboClass.getID(), oboClass);
	    		if(oboClass.getChildren().size() == 0){//a leaf node
	    			leaves.add(oboClass);
	    			addNodeToParent(oboClass);
	    		}
	    		else if(oboClass.getParents().size() == 0){//a root node
	    			roots.add(oboClass);
	    			for(Link child : oboClass.getChildren()){
	    				roots.add((OBOClass)child.getChild());
	    			}
	    			addChildrenToNode(oboClass);
	    		}
	    		else{// a regular node
	    			addNodeToParent(oboClass);
	    			addChildrenToNode(oboClass);
	    		}
	    	}
	    }
	}
	
	/**
	 * A helper method to add a node to the list of children
	 * of a parent
	 * @param node - the node to be stored
	 */
	public void addNodeToParent(OBOClass node){
		Set<OBOClass> children;
		//we assume a tree. 
		Link pLink = (Link)node.getParents().toArray()[0];
		OBOClass parent = (OBOClass)pLink.getParent();
		if(parentToChildrenMap.containsKey(parent)){
			children = parentToChildrenMap.get(parent);
		}
		else{
			children = new HashSet<OBOClass>();
		}
		children.add(node);
		parentToChildrenMap.put(parent, children);
		childToParentMap.put(node, parent);
	}
	
	/**
	 * A helper method to add children to a node
	 * @param node - the node whose children are to be added
	 */
	public void addChildrenToNode(OBOClass node){
		Set<OBOClass> children = new HashSet<OBOClass>();
		
		for(Link cLink : node.getChildren()){
			OBOClass child = (OBOClass)cLink.getChild();
			children.add(child);
			childToParentMap.put(child, node);
		}
		parentToChildrenMap.put(node, children);
	}
	
	/**
	 * @PURPOSE This is a test method to print out the entire
	 * stored taxonomy with the proper indentation
	 * @CAUTION Hardcoded file path for writing out the taxonomy
	 * @param node - the node whose children are to printed along with the node
	 * itself
	 * @param tabCt - the number of indents
	 * @param bw - buffered reader which contains a pointer to a text file
	 * @throws IOException
	 */
	public void printTaxonomy(OBOClass node, int tabCt, BufferedWriter bw) 
		throws IOException{
		String tabs = "";

		for(int i = 0; i < tabCt; i++){
			tabs += "  ";
		}
		if(!getLeaves().contains(node)){//this is not a leaf node
			for(OBOClass child : getParentToChildrenMap().get(node)){
				bw.write(tabs + child.getID() + "\t" + child.getName() + "\n");
				printTaxonomy(child, tabCt + 1, bw);
			}
		}
		else{
			return;
		}
	}
	
	/**
	 * @PURPOSE This method determines the correct
	 * path from a given TTO node (its ID to be specific eg. TTO:0001979)
	 * to the root
	 * @param ttb - an instance of the TeleostTaxonomyBuilder
	 * @param nodeId - the node to be searched for
	 * @param path - the path from the node to the root of the TTO
	 * @return the path from the given node to the root
	 */
	public List<OBOClass> tracePathToRoot(String nodeId, List<OBOClass> path){
		//we check if this is a root node. Exit condition for 
		//recursion 
		for(OBOClass root : roots){
			if(root.getID().equals(nodeId)){
				path.add(root);
				return path;
			}
		}
		//otherwise, we recurse through the collection of nodes
		//and children
		OBOClass parent = getChildToParentMap().get(getIdToClassMapper().get(nodeId));
		path.add(getIdToClassMapper().get(nodeId));
		return tracePathToRoot(parent.getID(), path);
	}
	
	/**
	 * @PURPOSE This method computes the path between a given node and 
	 * an arbitrary node in the tree
	 * @NOTE This is the same as {@link tracePathToRoot} except the terminus
	 * is an arbitrary node and not the root in this case
	 * @param startNodeId - the node to start from 
	 * @param endNodeId - the node to end at
	 * @param path - the path from current node to end
	 * @return the complete path from source to end
	 */
	public List<OBOClass> tracePath(OBOClass startNode, OBOClass endNode, List<OBOClass> path){
		
		if(startNode.getID().equals(endNode.getID()) ||
				!this.tracePathToRoot(startNode.getID(), new ArrayList<OBOClass>()).contains(endNode)){
			path.add(endNode);
			return path;
		}
		
		OBOClass parent = childToParentMap.get(startNode);
		path.add(startNode);
		return tracePath(parent, endNode, path);
	}
	
	/**
	 * @PURPOSE This method takes in a list of taxa and returns their MRCA
	 * @param taxa - the list of taxa whose MRCA is to be determined
	 * @param mrca - the current MRCA
	 * @return the MRCA of the entire list of taxa
	 */
	public OBOClass findMRCA(LinkedList<OBOClass> taxa, OBOClass mrca){
		//if we have reached the end of the list,
		//or if we have already reached the root, return 
		if(taxa.size() == 0 || getRoots().contains(mrca))
			return mrca;
		//get the first element in the list
		OBOClass first = taxa.remove(0);
		//find the mrca of the first element and the input mrca
		OBOClass result = findMRCA(first, mrca); 
		//recursively call the same method, with the shortened list
		//and the new MRCA as arguments
		return findMRCA(taxa, result);
	}
	
	/**
	 * @PURPOSE A helper method to find the MRCA of any two given taxa.
	 * @NOTE this is an overloaded method and is private access only
	 * @param oClass - one of the two taxa
	 * @param mrca - the other taxon
	 * @return - the mrca of the two taxa
	 */
	public OBOClass findMRCA(OBOClass oClass, OBOClass mrca){
		//if the MRCA is null, we are just getting started trivially
		//return the oClass as the MRCA
		if(mrca == null)
			return oClass;
		//if the MRCA lies in the path from the oClass to the Root,
		//the MRCA stays the MRCA
		else if(this.tracePathToRoot(oClass.getID(), new ArrayList<OBOClass>()).contains(mrca))
			return mrca;
		//otherwise, if the oClass lies in the path from the MRCA to its root,
		//the oClass becomes the new MRCA
		else if(this.tracePathToRoot(mrca.getID(), new ArrayList<OBOClass>()).contains(oClass))
			return oClass;
		else{
			//get the path from oClass to Root
			List<OBOClass> pathFromOclass = this.tracePathToRoot(oClass.getID(), new ArrayList<OBOClass>());
			//get the path from mrca to Root
			List<OBOClass> pathFromMrca = this.tracePathToRoot(mrca.getID(), new ArrayList<OBOClass>());
			//find where they intersect, this intersection point is the new MRCA
			return findBranchingPointInPaths(pathFromOclass, pathFromMrca);
		}
	}
	
	/**
	 * @PURPOSE A utility method to return the intersection of two paths
	 * @param pathFromOclass - path from one taxon to the root
	 * @param pathFromMrca - path from the other txon to the root
	 * @return - the intersection of the two paths
	 */
	private OBOClass findBranchingPointInPaths(List<OBOClass> pathFromOclass,
			List<OBOClass> pathFromMrca) {
		for(OBOClass taxon : pathFromMrca){
			if(pathFromOclass.contains(taxon)){
				return taxon;
			}
		}
		return pathFromMrca.get(0);
	}
	
	/**
	 * @PURPOSE The purpose of this method is to arrange
	 * the input list of taxa hierarchically in a tree with the
	 * MRCA at the root. Every intermediate node is also
	 * stored in the tree
	 * @param taxaList - input list of taxa
	 * @param tree - the tree of hierarchical taxa
	 * @return the completed tree given all the taxa in the input list
	 */
	public TaxonTree constructTreeFromTaxaList(LinkedList<OBOClass> taxaList, 
												TaxonTree tree){
		
		//get all the branching points, we'll use these
		Map<OBOClass, Set<OBOClass>> branches = tree.getBranchingPointsAndChildren();
		Set<OBOClass> children;
		OBOClass currRoot = null;
		OBOClass parent;
		
		for(OBOClass oboClass : taxaList){
			//we call the findMRCA method only if the current root 
			//is not the root of entire ontology

			currRoot = this.findMRCA(oboClass, currRoot);
			//find path to current MRCA
			List<OBOClass> pathToMrca = this.tracePath(oboClass, currRoot, new ArrayList<OBOClass>());
			
			//process each node in the path
			for(int index = 0; index < pathToMrca.size() - 1; index++){
				OBOClass node = pathToMrca.get(index);
				parent = pathToMrca.get(index + 1);
				if(branches.containsKey(parent))
					children = branches.get(parent);
				else
					children = new HashSet<OBOClass>();
				children.add(node);
				branches.put(parent, children);
			}
		}
		//set the branches of the tree and the root
		tree.setBranchingPointsAndChildren(branches);
		tree.setRoot(currRoot);
		return tree;
	}
	
	/**
	 * The main method. This will be used to test the other methods before
	 * they are invoked directly from the other classes REST resources to
	 * be specific 
	 * @param args
	 * @throws DataAdapterException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws DataAdapterException, IOException{

		long startTime1 = System.currentTimeMillis();
		TeleostTaxonomyBuilder ttb = new TeleostTaxonomyBuilder();
		long endTime1 = System.currentTimeMillis();
		
		ttb.log().trace((endTime1 - startTime1) + " milliseconds to set up taxonomy");
		
//		System.out.println(ttb.tracePath("TTO:1001979", new ArrayList<OBOClass>()));
//		System.out.println(ttb.tracePath("TTO:1002351", new ArrayList<OBOClass>()));
//		System.out.println(ttb.tracePath("TTO:10000039", new ArrayList<OBOClass>()));

		
		LinkedList<OBOClass> ttoList = new LinkedList<OBOClass>();
		Iterator<String> it = ttb.getIdToClassMapper().keySet().iterator();
		while(it.hasNext()){
			String tto = it.next();
			ttoList.add(ttb.getIdToClassMapper().get(tto));
		}

//		ttoList.add(ttb.getIdToClassMapper().get("TTO:1001979"));
//		ttoList.add(ttb.getIdToClassMapper().get("TTO:1002351"));
//		ttoList.add(ttb.getIdToClassMapper().get("TTO:10000039"));
//		long startTime2 = System.currentTimeMillis();
		System.out.println(ttb.findMRCA(ttoList, null));
//		long endTime2 = System.currentTimeMillis();
//		System.out.println((endTime2 - startTime2) + " milliseconds to find MRCA");
		
		TaxonTree tree = new TaxonTree();
		
		long startTime2 = System.currentTimeMillis();
		tree = ttb.constructTreeFromTaxaList(ttoList, tree);
		long endTime2 = System.currentTimeMillis();
		System.out.println((endTime2 - startTime2) + " milliseconds to construct subtree from taxonList");
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File("/home/cartik/Desktop/subtree.txt")));
		System.out.println("root of the tree is " + tree.getRoot());
		tree.printTaxonomy(tree.getRoot(), 0, bw);
		bw.flush();
		bw.close();
	}

	private Logger log() {
		return Logger.getLogger(this.getClass());
	}
}