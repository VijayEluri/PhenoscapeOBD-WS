package org.phenoscape.obd.query;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.obd.query.Shard;
import org.obd.query.impl.OBDSQLShard;

/**
 * This class contains queries (Strings), which are to be executed against the database
 * to retrieve results for the different REST services 
 * <p> Query Strings are processed before being accessed
 * by methods from the {@link OBDQuery} class which executes them
 * @author cartik 
 */

public class Queries {

	/**
	 * relationNodeIds is a map of actual relation nodes ids that are used in links and the
	 * node ids they are assigned by the database. For example, a row from the NODE table 
	 * in the database would be
	 * 
	 * NODE_ID		UID				LABEL
	 * -------      ----            ----- 
	 * 123			OBO_REL:is_a	is_a
	 * 
	 * In relationNodeIds, we store the Node-Ids associated with the UIDs because the queries 
	 * (see below) need them
	 */
	private Map<String, Integer> relationNodeIds;
	
	 /* 
     * These are actual relation UIDs coming from the OBO Relation and
     * PHENOSCAPE VOCAB ontologies
     */
	private final static String IS_A_RELATION_ID = "OBO_REL:is_a"; 
	private final static String INHERES_IN_RELATION_ID = "OBO_REL:inheres_in";
	private final static String HAS_ALLELE_RELATION_ID = "PHENOSCAPE:has_allele";
	private final static String EXHIBITS_RELATION_ID = "PHENOSCAPE:exhibits";
	private final static String VALUE_FOR_RELATION_ID = "PHENOSCAPE:value_for";
	
	private static final String HOMOLOG_TO_RELATION_ID = "OBO_REL:homologous_to";
	private static final String IN_TAXON_RELATION_ID = "PHENOSCAPE:in_taxon";
	private static final String HAS_PUBLICATION_RELATION_ID = "PHENOSCAPE:has_publication";
	private static final String HAS_EVIDENCE_CODE_RELATION_ID = "PHENOSCAPE:has_evidence_code";
	
	private static final String POSITED_BY_RELATION_ID = "posited_by";
	private static final String HAS_STATE_RELATION_ID = "cdao:has_State";
	private static final String HAS_DATUM_RELATION_ID = "cdao:has_Datum";
	private static final String HAS_CURATORS_RELATION_ID = "PHENOSCAPE:has_curators";
	private static final String HAS_COMMENT_RELATION_ID = "PHENOSCAPE:has_comment";
	private static final String HAS_NUMBER_RELATION_ID = "PHENOSCAPE:has_number";
	
	private static final String OBO_COMMENT_TAG_ID = "oboInOwl:comment";
	
	/**
	 * An enumeration to keep track of the placeholder patterns to look for
	 * in the raw query
	 */
	
	public static enum QueryPlaceholder{
		INHERES_IN("___inheres_in", INHERES_IN_RELATION_ID),
		VALUE_FOR("___value_for", VALUE_FOR_RELATION_ID),
		EXHIBITS("___exhibits", EXHIBITS_RELATION_ID),
		HAS_ALLELE("___has_allele", HAS_ALLELE_RELATION_ID),
		IS_A("___is_a", IS_A_RELATION_ID),
		HOMOLOGOUS_TO("___homologous_to", HOMOLOG_TO_RELATION_ID),
		IN_TAXON("___in_taxon", IN_TAXON_RELATION_ID),
		HAS_PUBLICATION("___has_publication", HAS_PUBLICATION_RELATION_ID),
		HAS_EVIDENCE_CODE("___has_evidence_code", HAS_EVIDENCE_CODE_RELATION_ID),
		POSITED_BY("___posited_by", POSITED_BY_RELATION_ID),
		HAS_STATE("___has_State", HAS_STATE_RELATION_ID),
		HAS_DATUM("___has_Datum", HAS_DATUM_RELATION_ID),
		HAS_CURATORS("___has_curators", HAS_CURATORS_RELATION_ID),
		HAS_COMMENT("___has_comment", HAS_COMMENT_RELATION_ID), 
		HAS_NUMBER("___has_number", HAS_NUMBER_RELATION_ID),
		COMMENT("___comment", OBO_COMMENT_TAG_ID);

		
		QueryPlaceholder(String name, String rId){
			this.pattern = name;
			this.relationUid = rId;
		}
		
		private final String pattern;
		private final String relationUid;
		
		public String pattern(){return pattern;}
		public String relationUid(){return relationUid;}
	};
	
	private Shard shard;
	
	public Logger log;
	
	/*
	 * These are the queries we are using now. 
	 */
	
	/**
	 * This query retrieves the database refresh timestamp
	 */
	private String timestampQuery = 
		"SELECT notes FROM obd_schema_metadata";
	
	/**
	 * INPUT: An anatomical entity (E)
	 * This is a variant of the {@link #anatomyQuery} 
	 */
	private String genericPhenotypeQuery = 
		"SELECT " +
		"p1.phenotype_uid AS phenotype_uid, " +
		"p1.subject_uid AS subject_uid, p1.subject_label AS subject_Label, " +
		"p1.quality_uid AS quality_uid, p1.quality_label AS quality_label, " +
		"p1.character_uid AS character_uid, p1.character_label AS character_label, " +
		"p1.entity_uid AS entity_uid, p1.entity_label AS entity_label, " +
		"p1.reif_id AS reif_id, p1.count AS count,  " +
		"p1.related_entity_uid AS related_entity_uid, " +
		"p1.related_entity_label AS related_entity_label, " +
		"pub_reif.publication AS publication " +
		"FROM " +
		"phenotype_by_entity_character AS p1 " +
		"LEFT OUTER JOIN (link AS subtaxon_link " +
		"JOIN node AS taxon_node " +
		"ON (taxon_node.node_id = subtaxon_link.object_id AND " +
		"	subtaxon_link.predicate_id= ___is_a)) " +
		"ON (p1.subject_nid = subtaxon_link.node_id) " +
		"JOIN phenotype_inheres_in_part_of_entity AS p2 " +
		"ON (p1.phenotype_nid = p2.phenotype_nid) " +
		"LEFT OUTER JOIN dw_publication_reif_id_table AS pub_reif " + 
		"ON (pub_reif.reif_id = p1.reif_id) " + 
		"WHERE " +
		"p2.entity_nid = (SELECT node_id FROM node WHERE uid = ?)" ;
	
	private String genericPhenotypeQueryForTaxon = 
		"SELECT " +
		"p1.phenotype_uid AS phenotype_uid, " +
		"p1.subject_uid AS subject_uid, p1.subject_label AS subject_Label, " +
		"p1.quality_uid AS quality_uid, p1.quality_label AS quality_label, " +
		"p1.character_uid AS character_uid, p1.character_label AS character_label, " +
		"p1.entity_uid AS entity_uid, p1.entity_label AS entity_label, " +
		"p1.reif_id AS reif_id, p1.count AS count, " +
		"p1.related_entity_uid AS related_entity_uid, " +
		"p1.related_entity_label AS related_entity_label, " +
		"pub_reif.publication AS publication " +
		"FROM " +
		"phenotype_by_entity_character AS p1 " +
		"JOIN dw_taxon_is_a_taxon_table AS is_a " +
		"ON (is_a.subtaxon_nid = p1.subject_nid) " +
		"LEFT OUTER JOIN dw_publication_reif_id_table AS pub_reif " +
		"ON (pub_reif.reif_id = p1.reif_id) " +
		"WHERE " +
		"is_a.supertaxon_nid = (SELECT node_id FROM node WHERE uid = ?)";
	
	private String genericPhenotypeQueryForSpecificTaxon = 
		"SELECT " +
		"p1.phenotype_uid AS phenotype_uid, " +
		"p1.subject_uid AS subject_uid, " +
		"p1.subject_label AS subject_Label, " +
		"p1.quality_uid AS quality_uid, " +
		"p1.quality_label AS quality_label, " +
		"p1.character_uid AS character_uid, " +
		"p1.character_label AS character_label, " +
		"p1.entity_uid AS entity_uid, " +
		"p1.entity_label AS entity_label, " +
		"p1.reif_id AS reif_id, " +
		"p1.count AS count, " +
		"p1.related_entity_uid AS related_entity_uid, " +
		"p1.related_entity_label AS related_entity_label, " +
		"pub_reif.publication AS publication " +
		"FROM " +
		"phenotype_by_entity_character AS p1 " +
		"JOIN phenotype_inheres_in_part_of_entity AS p2 " +
		"ON (p1.phenotype_nid = p2.phenotype_nid) " +
		"LEFT OUTER JOIN dw_publication_reif_id_table AS pub_reif " + 
		"ON (pub_reif.reif_id = p1.reif_id) " + 
		"WHERE " +
		"p2.entity_nid = (SELECT node_id FROM node WHERE uid = ?)";
	
	/**
	 * INPUT: An anatomical entity (E)
	 * This query finds all the phenotypes (P) associated with a given anatomical entity. From the found phenotypes (P), 
	 * this query finds the related taxa (T), genes (G), qualities (Q), and characters (C)that these qualities (Q) 
	 * are attributes of 
	 */
	
	private String anatomyQuery = 
		"SELECT " +
		"p1.phenotype_uid AS phenotype_uid, " +
		"p1.subject_uid AS subject_uid, " +
		"p1.subject_label AS subject_Label, " +
		"p1.quality_uid AS quality_uid, " +
		"p1.quality_label AS quality_label, " +
		"p1.character_uid AS character_uid, " +
		"p1.character_label AS character_label, " +
		"p1.entity_uid AS entity_uid, " +
		"p1.entity_label AS entity_label, " +
		"p1.reif_id AS reif_id, " +
		"p1.count AS count,  " +
		"p1.related_entity_uid AS related_entity_uid, " +
		"p1.related_entity_label AS related_entity_label, " +
		"pub_reif.publication AS publication " +
		"FROM " +
		"phenotype_by_entity_character AS p1 " +
		"JOIN phenotype_inheres_in_part_of_entity AS p2 " +
		"ON (p1.phenotype_nid = p2.phenotype_nid) " + 
		"LEFT OUTER JOIN dw_publication_reif_id_table AS pub_reif " + 
		"ON (pub_reif.reif_id = p1.reif_id) " + 
		"WHERE " +
		"p2.entity_uid = ?";
	
	private String anatomyDetailsQuery = 
		"SELECT " +
		"p1.phenotype_uid AS phenotype_uid, " +
		"p1.subject_uid AS subject_uid, p1.subject_label AS subject_Label, " +
		"p1.quality_uid AS quality_uid, p1.quality_label AS quality_label, " +
		"p1.character_uid AS character_uid, p1.character_label AS character_label, " +
		"p1.entity_uid AS entity_uid, p1.entity_label AS entity_label, " +
		"p1.reif_id AS reif_id, p1.count AS count, " +
		"p1.related_entity_uid AS related_entity_uid, p1.related_entity_label AS related_entity_label, " +
		"pub_reif.publication AS publication " +
		"FROM " +
		"phenotype_by_entity_character AS p1 " +
		"LEFT OUTER JOIN dw_publication_reif_id_table AS pub_reif ON (pub_reif.reif_id = p1.reif_id) " +
		"JOIN dw_entity_is_a_entity_table AS dw ON (dw.subentity_nid = p1.entity_nid) " +
		"WHERE " +
		"dw.superentity_nid = (SELECT node_id FROM node WHERE uid = ?)";
	
	/**
	 * INPUT: A gene (G)
	 * This query finds the anatomical entity - quality (EQ) combinations expressed by the input gene.
	 * Then it finds the characters (C) for the qualities (Q) directly associated with the gene to
	 * find a list of entity-character (EC) combinations. 
	 * Lastly, it finds all the taxa (T) and genes (G) that are associated with phenotypes (P), 
	 * that are associated with the (EC) combinations 
	 */
	
	private String geneSummaryQuery = 
		"SELECT " +
		"p1.phenotype_uid AS phenotype_uid, " +
		"p1.subject_uid AS subject_uid, " +
		"p1.subject_label AS subject_Label, " +
		"p1.quality_uid AS quality_uid, " +
		"p1.quality_label AS quality_label, " +
		"p1.character_uid AS character_uid, " +
		"p1.character_label AS character_label, " +
		"p1.entity_uid AS entity_uid, " +
		"p1.entity_label AS entity_label, " +
		"p1.reif_id AS reif_id, " +
		"p1.count AS count,  " +
		"p1.related_entity_uid AS related_entity_uid, " +
		"p1.related_entity_label AS related_entity_label " +
		"FROM " +
		"phenotype_by_entity_character AS p1 " +
		"WHERE " +
		"(p1.entity_nid, p1.character_nid) " +
		"IN (" +
		" SELECT DISTINCT " +
		" p1.entity_nid, " +
		" p1.character_nid " +
		" FROM " +
		" phenotype_by_entity_character AS p1 " +
		" WHERE " +
		" p1.subject_uid = ? " +
		")";
	
	/**
	 * INPUT: A taxon (T)
	 * This query retrieves all the entities (E), qualities (Q), and characters (C)
	 * that are associated with the input taxon (T) and its subtaxa (ST)
	 */
	
	
	private String taxonQuery = 
		"SELECT " +
		"p1.phenotype_uid AS phenotype_uid, " +
		"p1.subject_uid AS subject_uid, p1.subject_label AS subject_Label, " +
		"p1.quality_uid AS quality_uid, p1.quality_label AS quality_label, " +
		"p1.character_uid AS character_uid, p1.character_label AS character_label, " +
		"p1.entity_uid AS entity_uid, p1.entity_label AS entity_label, " +
		"p1.reif_id AS reif_id, p1.count AS count, " +
		"p1.related_entity_uid AS related_entity_uid, p1.related_entity_label AS related_entity_label, " +
		"pub_reif.publication AS publication " +
		"FROM " +
		"phenotype_by_entity_character AS p1 " +
		"JOIN dw_taxon_is_a_taxon_table AS is_a ON (is_a.subtaxon_nid = p1.subject_nid) " +
		"LEFT OUTER JOIN dw_publication_reif_id_table AS pub_reif ON (pub_reif.reif_id = p1.reif_id) " +
		"JOIN dw_entity_is_a_entity_table AS dw ON (dw.subentity_nid = p1.entity_nid) " +
		"WHERE " +
		"is_a.supertaxon_nid = (SELECT node_id FROM node WHERE uid = ?)";
	
	/**
	 * INPUT: A gene (G)
	 * This query retrieves all the Entities (E), Qualities (Q), and Characters (C) associated
	 * with the given gene (G) through the expressed phenotypes (P)
	 */
	
	private String geneQuery = 
		"SELECT " +
		"p1.phenotype_uid AS phenotype_uid, " +
		"p1.subject_uid AS subject_uid, p1.subject_label AS subject_Label, " +
		"p1.quality_uid AS quality_uid, p1.quality_label AS quality_label, " +
		"p1.character_uid AS character_uid, p1.character_label AS character_label, " +
		"p1.entity_uid AS entity_uid, p1.entity_label AS entity_label, " +
		"p1.reif_id AS reif_id, p1.count AS count, " +
		"p1.related_entity_uid AS related_entity_uid, " +
		"p1.related_entity_label AS related_entity_label, " +
		"pub_reif.publication AS publication " +
		"FROM " +
		"phenotype_by_entity_character AS p1 " +
		"LEFT OUTER JOIN dw_publication_reif_id_table AS pub_reif ON (pub_reif.reif_id = p1.reif_id) " +
		"JOIN dw_entity_is_a_entity_table AS dw ON (dw.subentity_nid = p1.entity_nid) " +
		"WHERE " +
		"p1.subject_uid = ?";
	
	/**
	 * INPUT: A taxon (T)\n
	 * This query finds all the phenotypes (P) directly associated with input taxon (T)
	 * and its subtaxa (ST). Then it retrieves the unique Entity-Character (EC) 
	 * combinations from the phenotypes (P). Lastly, it finds all the genes (G) associated 
	 * with the EC combinations that are also associated with the taxa and its subtaxa
	 */
	
	private String taxonSummaryQuery =  
		"SELECT " +
		"p1.phenotype_uid AS phenotype_uid, " +
		"p1.subject_uid AS subject_uid, " +
		"p1.subject_label AS subject_Label, " +
		"p1.quality_uid AS quality_uid, " +
		"p1.quality_label AS quality_label, " +
		"p1.character_uid AS character_uid, " +
		"p1.character_label AS character_label, " +
		"p1.entity_uid AS entity_uid, " +
		"p1.entity_label AS entity_label, " +
		"p1.reif_id AS reif_id, " +
		"p1.count AS count,  " +
		"p1.related_entity_uid AS related_entity_uid, " +
		"p1.related_entity_label AS related_entity_label " +
		"FROM " +
		"phenotype_by_entity_character AS p1 " +
		"JOIN link AS subtaxon_link ON (p1.subject_nid = subtaxon_link.node_id AND " +
		"	subtaxon_link.predicate_id = ___is_a) " +
		"JOIN node AS search_node ON (search_node.node_id = subtaxon_link.object_id) " +
		"WHERE " +
		"search_node.uid = ? " +
		"UNION " + 
		"SELECT " +
		"p1.phenotype_uid AS phenotype_uid, " +
		"p1.subject_uid AS subject_uid, " +
		"p1.subject_label AS subject_Label, " +
		"p1.quality_uid AS quality_uid, " +
		"p1.quality_label AS quality_label, " +
		"p1.character_uid AS character_uid, " +
		"p1.character_label AS character_label, " +
		"p1.entity_uid AS entity_uid, " +
		"p1.entity_label AS entity_label, " +
		"p1.reif_id AS reif_id, " +
		"p1.count AS count,  " +
		"p1.related_entity_uid AS related_entity_uid, " +
		"p1.related_entity_label AS related_entity_label " +
		"FROM " +
		"phenotype_by_entity_character AS p1 " +
		"WHERE " +
		"p1.subject_uid = ?" + 
		"UNION " +
		"SELECT " +
		"p1.phenotype_uid AS phenotype_uid, " +
		"p1.subject_uid AS subject_uid, " +
		"p1.subject_label AS subject_Label, " +
		"p1.quality_uid AS quality_uid, " +
		"p1.quality_label AS quality_label, " +
		"p1.character_uid AS character_uid, " +
		"p1.character_label AS character_label, " +
		"p1.entity_uid AS entity_uid, " +
		"p1.entity_label AS entity_label, " +
		"p1.reif_id AS reif_id, " +
		"p1.count AS count,  " +
		"p1.related_entity_uid AS related_entity_uid, " +
		"p1.related_entity_label AS related_entity_label " +
		"FROM " +
		"phenotype_by_entity_character AS p1 " +
		"WHERE " +
		"p1.gene_or_taxon = 'G' AND " +
		"(p1.entity_nid, p1.character_nid) " +
		"IN " +
		"(" +
		" SELECT DISTINCT " +
		" p1.entity_nid, " +
		" p1.character_nid " +
		" FROM " +
		" phenotype_by_entity_character AS p1 " +
		" JOIN link AS subtaxon_link ON (subtaxon_link.node_id = p1.subject_nid AND " +
		"	subtaxon_link.predicate_id = ___is_a) " +
		" JOIN node AS search_node  ON (search_node.node_id = subtaxon_link.object_id) " +
		" WHERE " +
		" search_node.uid = ?" +
		" UNION " + 
		" SELECT DISTINCT " +
		" p1.entity_nid, " +
		" p1.character_nid " +
		" FROM " +
		" phenotype_by_entity_character AS p1 " +
		" WHERE p1.subject_uid = ? " + 
		")";
	
	/**
	 * INPUT - An anatomical entity (E)
	 * This query finds all the homologous entities (E1) and associated taxa (T1) of
	 * a given input entity (E). The referenced publications and 
	 * evidence codes are also retrieved.  
	 */
	
	private String homologyQuery =  
		"SELECT " +
		"homolog1_node.uid AS homolog1, " +
		"taxon1_node.uid AS taxon1_uid, " +
		"taxon1_node.label AS taxon1, " +
		"entity1_node.uid AS entity1_uid, " +
		"entity1_node.label AS entity1, " +
		"homolog2_node.uid AS homolog2, " +
		"taxon2_node.uid AS taxon_uid2, " +
		"taxon2_node.label AS taxon2, " +
		"entity2_node.uid AS entity2_uid, " +
		"entity2_node.label AS entity2, " +
		"pub_node.uid AS publication, " +
		"evid_node.uid AS evidence_id, " +
		"evid_node.label AS evidence " +
		"FROM " +
		"link AS homology_link " +
		"JOIN node AS homolog1_node ON (homology_link.node_id = homolog1_node.node_id) " +
		"JOIN node AS homolog2_node ON (homology_link.object_id = homolog2_node.node_id) " +
		"JOIN link AS taxon1_link ON (taxon1_link.node_id = homolog1_node.node_id AND " +
			"taxon1_link.predicate_id = ___in_taxon) " +
		"JOIN node AS taxon1_node ON (taxon1_link.object_id = taxon1_node.node_id) " +
		"JOIN link AS entity1_link ON (entity1_link.node_id = homolog1_node.node_id AND " +
			"entity1_link.predicate_id = ___is_a) " +
		"JOIN node AS entity1_node ON (entity1_link.object_id = entity1_node.node_id) " +
		"JOIN link AS taxon2_link ON (taxon2_link.node_id = homolog2_node.node_id AND " +
		"	taxon2_link.predicate_id = ___in_taxon) " +
		"JOIN node AS taxon2_node ON (taxon2_link.object_id = taxon2_node.node_id) " +
		"JOIN link AS entity2_link ON (entity2_link.node_id = homolog2_node.node_id AND " +
			"entity2_link.predicate_id = ___is_a) " +
		"JOIN node AS entity2_node ON (entity2_link.object_id = entity2_node.node_id) " +
		"JOIN link AS has_pub_link ON (homology_link.reiflink_node_id = has_pub_link.node_id AND " +
			"has_pub_link.predicate_id = ___has_publication) " +
		"JOIN node AS pub_node ON (has_pub_link.object_id = pub_node.node_id) " +
		"JOIN link AS has_evid_link ON (homology_link.reiflink_node_id = has_evid_link.node_id AND " +
			"has_evid_link.predicate_id = ___has_evidence_code) " +
		"JOIN node AS evid_node ON (has_evid_link.object_id = evid_node.node_id) " +
		"WHERE " +
		"homology_link.predicate_id = ___homologous_to AND " +
		"(entity1_node.node_id = (SELECT node_id FROM node WHERE uid = ?) OR " +
		"entity2_node.node_id = (SELECT node_id FROM node WHERE uid = ?))";
	
	
	/**
	 * INPUT a reif_link_node_id that keeps track of metadata about the <TAXON><EXHIBITS><PHENOTYPE> assertion\n
	 * This query is used to retrieve metadata about a <TAXON><EXHIBITS><PHENOTYPE> assertion, such
	 * as publications, text notes about the state and character, and the curators names as well. 
	 * The <TAXON> <ENTITY> <QUALITY> triple is retrieved as well
	 */
	private String freeTextDataQuery = 
		"SELECT " +
		"tpm.reif_id, " +
		"tpm.taxon_uid, tpm.taxon_label, " +
		"tpm.entity_uid, tpm.entity_label, " +
		"tpm.quality_uid, tpm.quality_label, " +
		"tpm.publication, " +
		"tpm.character_text, tpm.character_comment, tpm.character_number, " +
		"tpm.state_text, tpm.state_comment, tpm.curators, " + 
		"rel.uid, rel.label " +
		"FROM " +
		"dw_taxon_phenotype_table AS tp " +
		"JOIN taxon_phenotype_metadata AS tpm ON (tpm.reif_id = tp.reif_id) " +
		"JOIN dw_phenotype_table AS phen ON (tp.phenotype_nid = phen.phenotype_nid) " +
		"JOIN node AS rel ON (rel.node_id = phen.towards_entity_nid) " +
		"WHERE " +
		"tp.reif_id = ?";

	/** GROUP Query part for auto completion  
	 * The main query string for auto completion. */
	private String autocompleteLabelQuery =
		"SELECT " +
		"n.uid AS uid, " +
		"n.label AS label, " +
		"NULL AS synonym, " +
		"NULL AS definition, " +
		"n.source_id AS source_id " +
		"FROM node AS n " +
		"WHERE " +
		"lower(n.label) LIKE ";
	/** GROUP Query part for auto completion  
	 * The synonym query string */
	private String autocompleteSynonymQuery = 
		"SELECT " +
		"n.uid AS uid, " +
		"n.label AS label, " +
		"a.label AS synonym, " +
		"NULL AS definition, " +
		"n.source_id AS source_id " +
		"FROM node AS n, alias AS a " +
		"WHERE " +
		"a.node_id = n.node_id AND " +
		"lower(a.label) LIKE ";
	/** GROUP Query part for auto completion  
	 * The definition query string */
	private String autocompleteDefinitionQuery = 
		"SELECT " +
		"n.uid AS uid, " +
		"n.label AS label, " +
		"NULL AS synonym, " +
		"d.label AS definition, " +
		"n.source_id AS source_id " +
		"FROM node AS n, description AS d " +
		"WHERE " +
		"d.node_id = n.node_id AND " +
		"lower(d.label) LIKE ";
	
	/**This query is used for retrieving the node ids 
	 * in the database for the ontologies sp. ontology
	 * default namespaces Eg. teleost-taxonomy, teleost_anatomy etc */
	private String queryForNodeIdsForOntologies = 
		"SELECT DISTINCT " +
		"source_node.node_id AS node_id, " +
		"source_node.uid AS uid " + 
		"FROM " +
		"node AS source_node " +
		"JOIN node ON (node.source_id = source_node.node_id) " +
		"WHERE " +
		"node.source_id IS NOT NULL";
	
	/**This query is used for getting targets of relations where the 
	 * search term is the subject */
	private String parentOfTermQuery = 
		"SELECT " +
		"subject_node.uid AS subject_uid, " +
		"subject_node.label AS subject_label, " +
		"predicate_node.uid AS predicate_uid, " +
		"predicate_node.label AS predicate_label, " +
		"object_node.uid AS object_uid, " +
		"object_node.label AS object_label, " +
		"rank_node.uid AS rank_uid, " +
		"rank_node.label AS rank_label " +
		"FROM link " +
		"JOIN node AS predicate_node ON (predicate_node.node_id = link.predicate_id) " +
		"JOIN node AS object_node ON (object_node.node_id = link.object_id) " +
		"JOIN node AS subject_node ON (subject_node.node_id = link.node_id AND " +
		"	subject_node.node_id = (SELECT node_id FROM node WHERE uid = ?)) " +
		"LEFT OUTER JOIN (link AS has_rank_link " +
		"JOIN node AS rank_node ON " +
		"(has_rank_link.object_id = rank_node.node_id AND " +
		"		has_rank_link.predicate_id = (SELECT node_id FROM node WHERE uid = 'has_rank') AND 	" +
		"		has_rank_link.is_inferred = 'f')) " +
		"ON (has_rank_link.node_id = object_node.node_id) " +
		"WHERE " +
		"link.is_inferred = 'f' AND " +
		"object_node.label IS NOT NULL AND " +
		"object_node.source_id IS NOT NULL";
	
	/** This query gets the sources of relations where the input term
	 * is an object */
	private String childrenOfTermQuery = 
		"SELECT " +
		"subject_node.uid AS subject_uid, " +
		"subject_node.label AS subject_label, " +
		"predicate_node.uid AS predicate_uid, " +
		"predicate_node.label AS predicate_label, " +
		"object_node.uid AS object_uid, " +
		"object_node.label AS object_label, " +
		"rank_node.uid AS rank_uid, " + 
		"rank_node.label AS rank_label " + 
		"FROM link " +
		"JOIN node AS predicate_node ON (predicate_node.node_id = link.predicate_id) " +
		"JOIN node AS subject_node ON (subject_node.node_id = link.node_id) " +
		"JOIN node AS object_node ON (object_node.node_id = link.object_id AND " +
		"	object_node.node_id = (SELECT node_id FROM node WHERE uid = ?)) " +
		"LEFT OUTER JOIN (link AS has_rank_link " + 
		"JOIN node AS rank_node " + 
		"ON (rank_node.node_id = has_rank_link.object_id AND " +
		"	has_rank_link.predicate_id = (SELECT node_id FROM node WHERE uid = 'has_rank') AND " +
		"	has_rank_link.is_inferred = 'f')) " +
		"ON (has_rank_link.node_id = subject_node.node_id) " +
		"WHERE " +
		"link.is_inferred = 'f' AND " +
		"subject_node.label IS NOT NULL AND " +
		"subject_node.source_id IS NOT NULL";
	
	/** This query gets the comments for the term which are
	 * extracted from the ontology 	 */
	private String commentOnTermQuery = 
		"SELECT val AS comment " +
		"FROM tagval " + 
		"WHERE " +
		"tag_id = ___comment AND " +
		"node_id = (SELECT node_id FROM node WHERE uid = ?)" ;
	
	/** This query retrieves the synonyms of the search term 
	 * and their scopes  */
	private String synonymOfTermQuery = 
		"SELECT label AS synonym " +
		"FROM alias " +
		"WHERE node_id = (SELECT node_id FROM node WHERE uid = ?)";
	
	/** This query retrieves the definition of the 
	 * search term  */
	private String definitionOfTermQuery = 
		"SELECT label AS definition FROM description " +
		"WHERE node_id = (SELECT node_id FROM node WHERE uid = ?)";
	
	private String queryForSquarifiedTaxonMapResource1 = 
	    	"SELECT " +
	    	"subtaxon.uid, subtaxon.label, COUNT(DISTINCT tp.phenotype_nid), COUNT(DISTINCT tp.taxon_nid) " +
	    	"FROM " +
	    	"node AS taxon " +
	    	"JOIN (link AS is_a_link " +
	    	"JOIN (node AS subtaxon " +
	    	"JOIN (dw_taxon_is_a_taxon_table AS is_a " +
	    	"JOIN dw_taxon_phenotype_table AS tp " +
	    	"ON (tp.taxon_nid = is_a.subtaxon_nid)) " +
	    	"ON (is_a.supertaxon_nid = subtaxon.node_id)) " +
	    	"ON (subtaxon.node_id = is_a_link.node_id AND " +
	    	"	is_a_link.predicate_id = ___is_a AND " +
	    	"	is_a_link.is_inferred = 'f')) " +
	    	"ON (is_a_link.object_id = taxon.node_id) " +
	    	"JOIN link AS rank_link " +
	    	"ON (rank_link.node_id = tp.taxon_nid AND " +
	    	"	rank_link.predicate_id = (SELECT node_id FROM node WHERE uid = 'has_rank') AND " +
	    	"	rank_link.is_inferred = 'f') " +
	    	"WHERE " +
	    	"taxon.uid = ? AND " +
	    	"rank_link.object_id = (SELECT node_id FROM node WHERE uid = 'TTO:species') " +
	    	"GROUP BY subtaxon.uid, subtaxon.label";
	
	private String queryForSquarifiedRaxonMapResource2 = 
		"SELECT " +
		"taxon.taxon_uid, taxon.taxon_label, " +
		"COUNT(DISTINCT tp.phenotype_nid) AS phenotype_count, " +
		"COUNT(DISTINCT is_a.subtaxon_nid) AS taxon_count " +
		"FROM " +
		"dw_taxon_table AS taxon " +
		"JOIN (dw_taxon_is_a_taxon_table AS is_a " +
		"JOIN dw_taxon_phenotype_table AS tp " +
		"ON (is_a.subtaxon_nid = tp.taxon_nid)) " +
		"ON (is_a.supertaxon_nid = taxon.taxon_nid) " +
		"JOIN link AS rank_link " +
		"ON (is_a.subtaxon_nid = rank_link.node_id AND " +
		"	rank_link.predicate_id = (SELECT node_id FROM node WHERE uid = 'has_rank') AND " +
		"	rank_link.is_inferred = 'f') " +
		"WHERE " +
		"taxon.taxon_uid = ? AND " +
		"rank_link.object_id = (SELECT node_id FROM node WHERE uid = 'TTO:species') " +
		"GROUP BY taxon.taxon_uid, taxon.taxon_label";
	
	/**
	 * This constructor sets up the shard and uses it to find node ids for all the relations used
	 * @param shard
	 */
	public Queries(Shard shard){
		this.shard = shard;
		this.log = Logger.getLogger(this.getClass());
		
		relationNodeIds = new HashMap<String, Integer>();
		/*
		 * We use the shard to pull out node_ids for the relations from the database
		 */
		relationNodeIds.put(IS_A_RELATION_ID, ((OBDSQLShard) this.shard).getNodeInternalId(IS_A_RELATION_ID));
		relationNodeIds.put(INHERES_IN_RELATION_ID, ((OBDSQLShard) this.shard).getNodeInternalId(INHERES_IN_RELATION_ID));
		relationNodeIds.put(HAS_ALLELE_RELATION_ID, ((OBDSQLShard) this.shard).getNodeInternalId(HAS_ALLELE_RELATION_ID));
		relationNodeIds.put(EXHIBITS_RELATION_ID, ((OBDSQLShard) this.shard).getNodeInternalId(EXHIBITS_RELATION_ID));
		relationNodeIds.put(VALUE_FOR_RELATION_ID, ((OBDSQLShard) this.shard).getNodeInternalId(VALUE_FOR_RELATION_ID));
		
		relationNodeIds.put(IN_TAXON_RELATION_ID, ((OBDSQLShard) this.shard).getNodeInternalId(IN_TAXON_RELATION_ID));
		relationNodeIds.put(HOMOLOG_TO_RELATION_ID, ((OBDSQLShard) this.shard).getNodeInternalId(HOMOLOG_TO_RELATION_ID));
		relationNodeIds.put(HAS_PUBLICATION_RELATION_ID, ((OBDSQLShard) this.shard).getNodeInternalId(HAS_PUBLICATION_RELATION_ID));
		relationNodeIds.put(HAS_EVIDENCE_CODE_RELATION_ID, ((OBDSQLShard) this.shard).getNodeInternalId(HAS_EVIDENCE_CODE_RELATION_ID));
		
		relationNodeIds.put(POSITED_BY_RELATION_ID, ((OBDSQLShard) this.shard).getNodeInternalId(POSITED_BY_RELATION_ID));
		relationNodeIds.put(HAS_STATE_RELATION_ID, ((OBDSQLShard) this.shard).getNodeInternalId(HAS_STATE_RELATION_ID));
		relationNodeIds.put(HAS_DATUM_RELATION_ID, ((OBDSQLShard) this.shard).getNodeInternalId(HAS_DATUM_RELATION_ID));
		relationNodeIds.put(HAS_CURATORS_RELATION_ID, ((OBDSQLShard) this.shard).getNodeInternalId(HAS_CURATORS_RELATION_ID));
		relationNodeIds.put(HAS_COMMENT_RELATION_ID, ((OBDSQLShard) this.shard).getNodeInternalId(HAS_COMMENT_RELATION_ID));
		relationNodeIds.put(HAS_NUMBER_RELATION_ID, ((OBDSQLShard) this.shard).getNodeInternalId(HAS_NUMBER_RELATION_ID));
		relationNodeIds.put(OBO_COMMENT_TAG_ID, ((OBDSQLShard) this.shard).getNodeInternalId(OBO_COMMENT_TAG_ID));
		
	}
	
	/*
	 * These getter methods return the queries with all the node ids correctly inserted
	 */
	public String getTimestampQuery(){
		return timestampQuery;
	}
	
	public String getTaxonSummaryQuery(){
		return replacePatternsWithIds(taxonSummaryQuery);
	}
	
	public String getAnatomyQuery() {
		return replacePatternsWithIds(anatomyQuery);
	}

	public String getAnatomyDetailsQuery() {
		return replacePatternsWithIds(anatomyDetailsQuery);
	}

	public String getGeneSummaryQuery() {
		return replacePatternsWithIds(geneSummaryQuery);
	}

	public String getTaxonQuery() {
		return replacePatternsWithIds(taxonQuery);
	}

	public String getGeneQuery() {
		return replacePatternsWithIds(geneQuery);
	}
	
	public String getHomologyQuery() {
		return replacePatternsWithIds(homologyQuery);
	}
	
	public String getFreeTextDataQuery(){
		return replacePatternsWithIds(freeTextDataQuery);
	}
	
	public String getAutocompleteLabelQuery(){
		return autocompleteLabelQuery;
	}
	
	public String getAutocompleteSynonymQuery(){
		return autocompleteSynonymQuery;
	}
	
	public String getAutocompleteDefinitionQuery(){
		return autocompleteDefinitionQuery;
	}

	public String getQueryForNodeIdsForOntologies() {
		return queryForNodeIdsForOntologies;
	}

	public String getParentOfTermQuery() {
		return parentOfTermQuery;
	}
	
	public String getChildrenOfTermQuery() {
		return childrenOfTermQuery;
	}

	public String getCommentOnTermQuery() {
		return replacePatternsWithIds(commentOnTermQuery);
	}

	public String getSynonymOfTermQuery() {
		return synonymOfTermQuery;
	}

	public String getDefinitionOfTermQuery() {
		return definitionOfTermQuery;
	}

	public String getGenericPhenotypeQuery() {
		return replacePatternsWithIds(genericPhenotypeQuery);
	}

	public String getGenericPhenotypeQueryForSpecificTaxon(){
		return this.genericPhenotypeQueryForSpecificTaxon;
	}
	
	public String getGenericPhenotypeQueryForTaxon(){
		return this.genericPhenotypeQueryForTaxon;
	}
	
	public String getQueryForSquarifiedTaxonMapResource1(){
		return this.replacePatternsWithIds(this.queryForSquarifiedTaxonMapResource1);
	}
	
	public String getQueryForSquarifiedTaxonMapResource2(){
		return this.queryForSquarifiedRaxonMapResource2;
	}
	
	/**
	 * This method cycles through the input query and replaces all the placeholder patterns  
	 * that it finds in the query with the correct node id
	 * @param query
	 * @return the raw query where the placeholder patterns are replaced with actual node ids
	 */
	public String replacePatternsWithIds(String query){
		String repQuery = query;
		for(QueryPlaceholder pattern : QueryPlaceholder.values()){
			repQuery = repQuery.replace(pattern.pattern(), getRelationNodeIds().get(pattern.relationUid()) + "");
		}
		return repQuery;
	}

	/**
	 * A getter method for the map
	 */
	public Map<String, Integer> getRelationNodeIds(){
		return relationNodeIds;
	}
}