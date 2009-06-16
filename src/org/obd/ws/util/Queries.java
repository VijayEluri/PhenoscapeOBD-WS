package org.obd.ws.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.obd.query.Shard;
import org.obd.query.impl.OBDSQLShard;

public class Queries {
/**
 * @PURPOSE: This class works with queries. Stores them as instance variables and processes them so they are
 * available for other classes which need to implement thesse queries
 * @author cartik 
 */
	/*
	 * relationNodeIds is a map of actual relation nodes ids that are used in links and the
	 * node ids they are associated in the database. For example, a row from the NODE table 
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
	
	/*
	 * An enumeration to keep track of the patterns to look for
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
		HAS_NUMBER("___has_number", HAS_NUMBER_RELATION_ID);

		
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
	 * @INPUT: An anatomical entity (E)
	 * This query finds all the phenotypes (P) associated with a given anatomical entity. From the found phenotypes (P), 
	 * this query finds the related taxa (T), genes (G), qualities (Q), and characters (C)that these qualities (Q) 
	 * are attributes of 
	 */
	
	private String anatomyQuery = 
		"SELECT DISTINCT " +
		"phenotype_node.uid AS phenotype, " +
		"CASE WHEN gene_node.uid IS NULL THEN taxon_node.uid ELSE gene_node.uid END AS taxonIdOrgeneId, " +
		"CASE WHEN gene_node.uid IS NULL THEN taxon_node.label ELSE gene_node.label END AS taxonOrgene, " +
		"quality_node.uid AS StateUid, " +
		"quality_node.label AS State, " +
		"character_node.uid AS CharacterUid, " +
		"character_node.label AS Character, " +
		"anatomy_node.uid AS entityUid, " +
		"anatomy_node.label AS entity," +
		"exhibits_link.reiflink_node_id AS reif_id " +
		"FROM " +
		"link AS search_link " +
		"JOIN node AS phenotype_node ON (search_link.node_id = phenotype_node.node_id) " +
		"JOIN link AS inheres_in_link ON (inheres_in_link.node_id = phenotype_node.node_id) " +
		"JOIN node AS anatomy_node ON (inheres_in_link.object_id = anatomy_node.node_id) " +
		"JOIN link AS is_a_link ON (is_a_link.node_id = phenotype_node.node_id) " +
		"JOIN node AS quality_node ON (is_a_link.object_id = quality_node.node_id) " +
		"JOIN link AS exhibits_link ON (exhibits_link.object_id = phenotype_node.node_id) " +
		"JOIN node AS taxon_node ON (exhibits_link.node_id = taxon_node.node_id) " +
		"LEFT OUTER JOIN (link AS has_allele_link " +
		"JOIN node AS gene_node " +
		"ON (gene_node.node_id = has_allele_link.node_id AND " +
		"has_allele_link.predicate_id = ___has_allele)) " +
		"ON (has_allele_link.object_id = exhibits_link.node_id) " +
		"JOIN link AS value_for_link ON (value_for_link.node_id = quality_node.node_id) " +
		"JOIN node AS character_node ON  (value_for_link.object_id = character_node.node_id) " +
		"WHERE " +
		"exhibits_link.predicate_id = ___exhibits AND " +
		"is_a_link.predicate_id = ___is_a AND " +
		"search_link.predicate_id = ___inheres_in AND " +
		"inheres_in_link.predicate_id = ___inheres_in AND " +
		"value_for_link.predicate_id = ___value_for AND " +
		"search_link.object_id = (SELECT node_id FROM node WHERE uid = ?) AND " +
		"inheres_in_link.is_inferred = 'f' AND " +
		"is_a_link.is_inferred = 'f' AND " +
		"exhibits_link.is_inferred = 'f'";
	
	/**
	 * @INPUT: A gene (G)
	 * This query finds the anatomical entity - quality (EQ) combinations expressed by the input gene.
	 * Then it finds the characters (C) for the qualities (Q) directly associated with the gene to
	 * find a list of entity-character (EC) combinations. 
	 * Lastly, it finds all the taxa (T) and genes (G) that are associated with phenotypes (P), 
	 * that are associated with the (EC) combinations 
	 */
	
	private String geneSummaryQuery = 
		"SELECT DISTINCT " +
		"phenotype_node.uid AS phenotype, " +
		"CASE WHEN gene_node.uid IS NULL THEN taxon_node.uid ELSE gene_node.uid END AS taxonIdOrgeneId, " +
		"CASE WHEN gene_node.uid IS NULL THEN taxon_node.label ELSE gene_node.label END AS taxonOrgene, " +
		"quality_node.uid AS StateUid, " +
		"quality_node.label AS State, " +
		"character_node.uid AS CharacterUid, " +
		"character_node.label AS Character, " +
		"anatomy_node.uid AS entityUid, " +
		"anatomy_node.label AS entity, " +
		"NULL AS reif_id " +
		"FROM " +
		"link AS search_link " +
		"JOIN node AS phenotype_node ON (search_link.node_id = phenotype_node.node_id) " +
		"JOIN (link AS inheres_in_link " +
		"JOIN node AS anatomy_node " +
		"ON (inheres_in_link.object_id = anatomy_node.node_id)) " +
		"ON (inheres_in_link.node_id = phenotype_node.node_id) " +
		"JOIN (link AS is_a_link " +
		"JOIN (node AS quality_node " +
		"JOIN (link AS value_for_link " +
		"JOIN node AS character_node " +
		"ON (value_for_link.object_id = character_node.node_id)) " +
		"ON (value_for_link.node_id = quality_node.node_id)) " +
		"ON (is_a_link.object_id = quality_node.node_id)) " +
		"ON (is_a_link.node_id = phenotype_node.node_id) " +
		"JOIN (link AS exhibits_link " +
		"JOIN (node AS taxon_node " +
		"LEFT OUTER JOIN (link AS gene_link " +
		"JOIN node AS gene_node " +
		"ON (gene_node.node_id = gene_link.node_id " +
		"AND gene_link.predicate_id = ___has_allele)) " +
		"ON (gene_link.object_id = taxon_node.node_id)) " +
		"ON (exhibits_link.node_id = taxon_node.node_id)) " +
		"ON (exhibits_link.object_id = phenotype_node.node_id) " +
		"WHERE " +
		"exhibits_link.predicate_id = ___exhibits AND " +
		"is_a_link.predicate_id = ___is_a AND " +
		"search_link.predicate_id = ___inheres_in AND " +
		"inheres_in_link.predicate_id = ___inheres_in AND " +
		"value_for_link.predicate_id = ___value_for AND " +
		"inheres_in_link.is_inferred = 'f' AND " +
		"is_a_link.is_inferred = 'f' AND " +
		"exhibits_link.is_inferred = 'f' AND " +
		"(search_link.object_id, value_for_link.object_id) IN " +
		"(SELECT DISTINCT " +
		"entity_node.node_id, character_node.node_id " +
		"FROM " +
		"link AS start_link " +
		"JOIN link AS exhibits_link ON (start_link.object_id = exhibits_link.node_id) " +
		"JOIN (link AS is_a_link " +
		"JOIN (link AS value_for_link " +
		"JOIN node AS character_node " +
		"ON (value_for_link.object_id = character_node.node_id)) " +
		"ON (is_a_link.object_id = value_for_link.node_id)) " +
		"ON (exhibits_link.object_id = is_a_link.node_id) " +
		"JOIN (link AS inheres_in_link " +
		"JOIN node AS entity_node " +
		"ON (inheres_in_link.object_id = entity_node.node_id)) " +
		"ON (inheres_in_link.node_id = exhibits_link.object_id) " +
		"WHERE " +
		"start_link.node_id = (SELECT node_id FROM node WHERE uid = ?) AND " +
		"start_link.predicate_id = ___has_allele AND " +
		"exhibits_link.predicate_id = ___exhibits AND " +
		"is_a_link.predicate_id = ___is_a AND " +
		"inheres_in_link.predicate_id = ___inheres_in AND " +
		"value_for_link.predicate_id = ___value_for AND " +
		"inheres_in_link.is_inferred = 'f' AND " +
		"is_a_link.is_inferred = 'f' AND " +
		"exhibits_link.is_inferred = 'f')";
	
	/**
	 * @INPUT: A taxon (T)
	 * This query retrieves all the entities (E), qualities (Q), and characters (C)
	 * that are associated with the input taxon (T) and its subtaxa (ST)
	 */
	
	
	private String taxonQuery = 
		"SELECT DISTINCT " +
		"phenotype_node.uid AS phenotype, " +
		"taxon_node.uid AS taxonId, " +
		"taxon_node.label AS taxon, " +
		"quality_node.uid AS StateUid, " +
		"quality_node.label AS State, " +
		"character_node.uid AS CharacterUid, " +
		"character_node.label AS Character, " +
		"anatomy_node.uid AS entity_uid, " +
		"anatomy_node.label AS entity_label, " +
		"exhibits_link.reiflink_node_id AS reif_id " +
		"FROM " +
		"link AS search_link " +
		"JOIN node AS phenotype_node ON (search_link.object_id = phenotype_node.node_id) " +
		"JOIN link AS exhibits_link ON (exhibits_link.object_id = phenotype_node.node_id) " +
		"JOIN node AS taxon_node ON (exhibits_link.node_id = taxon_node.node_id) " +
		"JOIN link AS inheres_in_link ON (inheres_in_link.node_id = phenotype_node.node_id) " +
		"JOIN node AS anatomy_node ON (inheres_in_link.object_id = anatomy_node.node_id) " +
		"JOIN link AS is_a_link ON (is_a_link.node_id = phenotype_node.node_id) " +
		"JOIN node AS quality_node ON (is_a_link.object_id = quality_node.node_id) " +
		"JOIN link AS value_for_link ON (value_for_link.node_id = quality_node.node_id) " +
		"JOIN node AS character_node ON (value_for_link.object_id = character_node.node_id) " +
		"JOIN link AS subtaxon_link ON ((subtaxon_link.object_id = search_link.node_id) " +
		"	AND (subtaxon_link.node_id = exhibits_link.node_id)) " +
		"WHERE " +
		"subtaxon_link.predicate_id = ___is_a AND " +
		"is_a_link.predicate_id = ___is_a AND " +
		"search_link.predicate_id = ___exhibits AND " +
		"exhibits_link.predicate_id = ___exhibits AND " +
		"inheres_in_link.predicate_id = ___inheres_in AND " +
		"value_for_link.predicate_id = ___value_for AND " +
		"search_link.node_id = (SELECT node_id FROM node WHERE uid = ?) AND " +
		"inheres_in_link.is_inferred = 'f' AND " +
		"is_a_link.is_inferred = 'f' AND " +
		"exhibits_link.is_inferred = 'f' " +
		"UNION " +
		"SELECT DISTINCT " +
		"phenotype_node.uid AS phenotype, " +
		"taxon_node.uid AS taxonId, " +
		"taxon_node.label AS taxon, " +
		"quality_node.uid AS StateUid, " +
		"quality_node.label AS State, " +
		"character_node.uid AS CharacterUid, " +
		"character_node.label AS Character, " +
		"anatomy_node.uid AS entity_uid, " +
		"anatomy_node.label AS entity_label, " +
		"exhibits_link.reiflink_node_id AS reif_id " +
		"FROM " +
		"link AS exhibits_link " +
		"JOIN node AS phenotype_node ON (exhibits_link.object_id = phenotype_node.node_id) " +
		"JOIN node AS taxon_node ON (exhibits_link.node_id = taxon_node.node_id) " +
		"JOIN link AS inheres_in_link ON (inheres_in_link.node_id = phenotype_node.node_id) " +
		"JOIN node AS anatomy_node ON (inheres_in_link.object_id = anatomy_node.node_id) " +
		"JOIN link AS is_a_link ON (is_a_link.node_id = phenotype_node.node_id) " +
		"JOIN node AS quality_node ON (is_a_link.object_id = quality_node.node_id) " +
		"JOIN link AS value_for_link ON (value_for_link.node_id = quality_node.node_id) " +
		"JOIN node AS character_node ON (value_for_link.object_id = character_node.node_id) " +
		"WHERE " +
		"is_a_link.predicate_id = ___is_a AND " +
		"exhibits_link.predicate_id = ___exhibits AND " +
		"inheres_in_link.predicate_id = ___inheres_in AND " +
		"value_for_link.predicate_id = ___value_for AND " +
		"exhibits_link.node_id = (SELECT node_id FROM node WHERE uid = ?) AND " +
		"inheres_in_link.is_inferred = 'f' AND " +
		"is_a_link.is_inferred = 'f' AND " +
		"exhibits_link.is_inferred = 'f'";
	
	/**
	 * @INPUT: A gene (G)
	 * This query retrieves all the Entities (E), Qualities (Q), and Characters (C) associated
	 * with the given gene (G) through the expressed phenotypes (P)
	 */
	
	private String geneQuery = 
		"SELECT DISTINCT " +
		"phenotype_node.uid AS phenotype, " +
		"gene_node.uid AS taxonId, " +
		"gene_node.label AS taxon, " +
		"quality_node.uid AS StateUid, " +
		"quality_node.label AS State, " +
		"character_node.uid AS CharacterUid, " +
		"character_node.label AS Character, " +
		"anatomy_node.uid AS entityUid, " +
		"anatomy_node.label AS entity, " +
		"exhibits_link.reiflink_node_id AS reif_id " +
		"FROM " +
		"link AS search_link JOIN node AS genotype_node ON (search_link.object_id = genotype_node.node_id) " +
		"JOIN node AS gene_node ON (search_link.node_id = gene_node.node_id) " +
		"JOIN link AS exhibits_link ON (exhibits_link.node_id = genotype_node.node_id) " +
		"JOIN node AS phenotype_node ON (exhibits_link.object_id = phenotype_node.node_id) " +
		"JOIN link AS is_a_link ON (is_a_link.node_id = phenotype_node.node_id) " +
		"JOIN node AS quality_node ON (is_a_link.object_id = quality_node.node_id) " +
		"JOIN link AS inheres_in_link ON (inheres_in_link.node_id = phenotype_node.node_id) " +
		"JOIN node AS anatomy_node ON (inheres_in_link.object_id = anatomy_node.node_id) " +
		"JOIN link AS value_for_link ON (value_for_link.node_id = quality_node.node_id) " +
		"JOIN node AS character_node ON  (value_for_link.object_id = character_node.node_id) " +
		"WHERE " +
		"exhibits_link.predicate_id = ___exhibits AND " +
		"is_a_link.predicate_id = ___is_a AND " +
		"search_link.predicate_id = ___has_allele AND " +
		"inheres_in_link.predicate_id = ___inheres_in AND " +
		"value_for_link.predicate_id = ___value_for AND " +
		"search_link.node_id = (SELECT node_id FROM node WHERE uid = ?) AND " +
		"inheres_in_link.is_inferred = 'f' AND " +
		"is_a_link.is_inferred = 'f'";
	
	/**
	 * @INPUT: A taxon (T)
	 * This query finds all the phenotypes (P) directly associated with input taxon (T)
	 * and its subtaxa (ST). Then it retrieves the unique Entity-Character (EC) 
	 * combinations from the phenotypes (P). Lastly, it finds all the genes (G) associated 
	 * with the EC combinations that are also associated with the taxa and its subtaxa
	 */
	
	private String taxonSummaryQuery =  
		"SELECT DISTINCT " +
		"phenotype_node.uid AS phenotype, " +
		"gene_node.uid AS taxonIdOrgeneId, " +
		"gene_node.label AS taxonOrgene, " +
		"quality_node.uid AS StateUid, " +
		"quality_node.label AS State, " +
		"character_node.uid AS CharacterUid, " +
		"character_node.label AS Character, " +
		"anatomy_node.uid AS entityUid, " +
		"anatomy_node.label AS entity, " +
		"NULL AS reif_id " +
		"FROM " +
		"node AS gene_node " +
		"JOIN (link AS search_link " +
		"JOIN (link AS exhibits_link " +
		"JOIN node AS phenotype_node " +
		"ON (exhibits_link.object_id = phenotype_node.node_id)) " +
		"ON (search_link.object_id = exhibits_link.node_id)) " +
		"ON (search_link.node_id = gene_node.node_id) " +
		"JOIN (link AS inheres_in_link " +
		"JOIN node AS anatomy_node " +
		"ON (inheres_in_link.object_id = anatomy_node.node_id)) " +
		"ON (inheres_in_link.node_id = phenotype_node.node_id) " +
		"JOIN (link AS is_a_link " +
		"JOIN (node AS quality_node " +
		"JOIN (link AS value_for_link " +
		"JOIN node AS character_node " +
		"ON (value_for_link.object_id = character_node.node_id)) " +
		"ON (value_for_link.node_id = quality_node.node_id)) " +
		"ON (is_a_link.object_id = quality_node.node_id)) " +
		"ON (is_a_link.node_id = phenotype_node.node_id) " +
		"WHERE " +
		"is_a_link.predicate_id = ___is_a AND " +
		"search_link.predicate_id = ___has_allele AND " +
		"inheres_in_link.predicate_id = ___inheres_in AND " +
		"value_for_link.predicate_id = ___value_for AND " +
		"exhibits_link.predicate_id = ___exhibits AND " +
		"inheres_in_link.is_inferred = 'f' AND " +
		"is_a_link.is_inferred = 'f' AND " +
		"(inheres_in_link.object_id, value_for_link.object_id) " +
		"IN " +
		"(" +
		" SELECT DISTINCT " +
		" anatomy_node.node_id, " +
		" character_node.node_id " +
		" FROM " +
		" link AS search_link " +
		" JOIN (link AS inheres_in_link " +
		" JOIN node AS anatomy_node " +
		" ON (inheres_in_link.object_id = anatomy_node.node_id)) " +
		" ON (inheres_in_link.node_id = search_link.object_id) " +
		" JOIN (link AS is_a_link " +
		" JOIN (link AS value_for_link " +
		" JOIN node AS character_node " +
		" ON (value_for_link.object_id = character_node.node_id)) " +
		" ON (value_for_link.node_id = is_a_link.object_id)) " +
		" ON (is_a_link.node_id = search_link.object_id) " +
		" WHERE " +
		" is_a_link.predicate_id = ___is_a AND " +
		" search_link.predicate_id = ___exhibits AND " +
		" inheres_in_link.predicate_id = ___inheres_in AND " +
		" value_for_link.predicate_id = ___value_for AND " +
		" search_link.node_id = (SELECT node_id FROM node WHERE uid = ?) AND " +
		" inheres_in_link.is_inferred = 'f' AND " +
		" is_a_link.is_inferred = 'f' " +
		") " +
		"UNION " +
		"SELECT DISTINCT " +
		"phenotype_node.uid AS phenotype, " +
		"taxon_node.uid AS taxonIdOrgeneId, " +
		"taxon_node.label AS taxonOrgene, " +
		"quality_node.uid AS StateUid, " +
		"quality_node.label AS State, " +
		"character_node.uid AS CharacterUid, " +
		"character_node.label AS Character, " +
		"anatomy_node.uid AS entityUid, " +
		"anatomy_node.label AS entity, " +
		"NULL AS reif_id " +
		"FROM " +
		"link AS search_link " +
		"JOIN node AS phenotype_node ON (search_link.object_id = phenotype_node.node_id) " +
		"JOIN (link AS exhibits_link " +
		"JOIN node AS taxon_node " +
		"ON (exhibits_link.node_id = taxon_node.node_id)) " +
		"ON (exhibits_link.object_id = phenotype_node.node_id) " +
		"JOIN link AS subtaxon_link ON (taxon_node.node_id = subtaxon_link.node_id AND " +
		"	subtaxon_link.object_id = search_link.node_id AND " +
		"	subtaxon_link.predicate_id = ___is_a) " +
		"JOIN (link AS inheres_in_link " +
		"JOIN node AS anatomy_node " +
		"ON (inheres_in_link.object_id = anatomy_node.node_id)) " +
		"ON (inheres_in_link.node_id = phenotype_node.node_id) " +
		"JOIN (link AS is_a_link " +
		"JOIN (node AS quality_node " +
		"JOIN (link AS value_for_link " +
		"JOIN node AS character_node " +
		"ON (value_for_link.object_id = character_node.node_id)) " +
		"ON (value_for_link.node_id = quality_node.node_id)) " +
		"ON (is_a_link.object_id = quality_node.node_id)) " +
		"ON (is_a_link.node_id = phenotype_node.node_id) " +
		"WHERE " +
		"is_a_link.predicate_id = ___is_a AND " +
		"search_link.predicate_id = ___exhibits AND " +
		"exhibits_link.predicate_id = ___exhibits AND " +
		"inheres_in_link.predicate_id = ___inheres_in AND " +
		"value_for_link.predicate_id = ___value_for AND " +
		"search_link.node_id = (SELECT node_id FROM node WHERE uid = ?) AND " +
		"inheres_in_link.is_inferred = 'f' AND " +
		"is_a_link.is_inferred = 'f' AND " +
		"exhibits_link.is_inferred = 'f' " +
		"UNION " +
		"SELECT DISTINCT " +
		"phenotype_node.uid AS phenotype, " +
		"taxon_node.uid AS taxonIdOrgeneId, " +
		"taxon_node.label AS taxonOrgene, " +
		"quality_node.uid AS StateUid, " +
		"quality_node.label AS State, " +
		"character_node.uid AS CharacterUid, " +
		"character_node.label AS Character, " +
		"anatomy_node.uid AS entityUid, " +
		"anatomy_node.label AS entity, " +
		"NULL AS reif_id " +
		"FROM " +
		"node AS taxon_node " +
		"JOIN (link AS exhibits_link " +
		"JOIN node AS phenotype_node " +
		"ON (exhibits_link.object_id = phenotype_node.node_id)) " +
		"ON (exhibits_link.node_id = taxon_node.node_id) " +
		"JOIN (link AS inheres_in_link " +
		"JOIN node AS anatomy_node " +
		"ON (inheres_in_link.object_id = anatomy_node.node_id)) " +
		"ON (inheres_in_link.node_id = phenotype_node.node_id) " +
		"JOIN (link AS is_a_link " +
		"JOIN (node AS quality_node " +
		"JOIN (link AS value_for_link " +
		"JOIN node AS character_node " +
		"ON (value_for_link.object_id = character_node.node_id)) " +
		"ON (value_for_link.node_id = quality_node.node_id)) " +
		"ON (is_a_link.object_id = quality_node.node_id)) " +
		"ON (is_a_link.node_id = phenotype_node.node_id) " +
		"WHERE " +
		"is_a_link.predicate_id = ___is_a AND " +
		"exhibits_link.predicate_id = ___exhibits AND " +
		"inheres_in_link.predicate_id = ___inheres_in AND " +
		"value_for_link.predicate_id = ___value_for AND " +
		"taxon_node.node_id = (SELECT node_id FROM node WHERE uid = ?) AND " +
		"inheres_in_link.is_inferred = 'f' AND " +
		"is_a_link.is_inferred = 'f' AND " +
		"exhibits_link.is_inferred = 'f'";
	
	/**
	 * @INPUT - An anatomical entity (E)
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
	 * @INPUT a reif_link_node_id that keeps track of metadata about the <TAXON><EXHIBITS><PHENOTYPE> assertion
	 * This query is used to retrieve metadata about a <TAXON><EXHIBITS><PHENOTYPE> assertion, such
	 * as publications, text notes about the state and character, and the curators names as well. 
	 * The <TAXON> <ENTITY> <QUALITY> triple is retrieved as well
	 */
	private String freeTextDataQuery = 
		"SELECT " +
		"phenotype_node.uid AS phenotype, " +
		"taxon_node.uid AS taxon_id, " +
		"taxon_node.label AS taxon, " +
		"entity_node.uid AS entity_id, " +
		"entity_node.label AS entity, " +
		"quality_node.uid AS quality_id, " +
		"quality_node.label AS quality, " +
		"pub_node.uid AS publication, " +
		"character_node.label AS character_text, " +
		"char_tag.val AS char_comment, " +
		"char_num_tag.val AS char_number, " +
		"state_node.label AS state_text, " +
		"state_tag.val AS state_comment, " +
		"curator_tag.val AS curators " +
		"FROM " +
		"link AS exhibits_link " +
		"JOIN link AS posited_by_link ON (posited_by_link.node_id = exhibits_link.reiflink_node_id) " +
		"JOIN (link AS has_pub_link " +
		"JOIN node AS pub_node " +
		"ON (has_pub_link.object_id = pub_node.node_id)) " +
		"ON (posited_by_link.object_id = has_pub_link.node_id) " +
		"JOIN link AS has_state_link ON (has_state_link.node_id =  exhibits_link.reiflink_node_id) " +
		"JOIN link AS has_state_link2 ON (has_state_link2.node_id = has_state_link.object_id) " +
		"JOIN node AS state_node ON (has_state_link2.object_id = state_node.node_id) " +
		"JOIN link AS has_character_link ON (has_character_link.object_id = state_node.node_id) " +
		"JOIN node AS character_node ON (has_character_link.node_id = character_node.node_id) " +
		"JOIN node AS phenotype_node ON (exhibits_link.object_id = phenotype_node.node_id) " +
		"JOIN node AS taxon_node ON (exhibits_link.node_id = taxon_node.node_id) " +
		"JOIN link AS inheres_in_link ON (inheres_in_link.node_id = phenotype_node.node_id AND " +
		"	inheres_in_link.is_inferred = 'f') " +
		"JOIN node AS entity_node ON (inheres_in_link.object_id = entity_node.node_id) " +
		"JOIN link AS is_a_link ON (is_a_link.node_id = phenotype_node.node_id AND " +
		"	is_a_link.is_inferred = 'f') " +
		"JOIN node AS quality_node ON (is_a_link.object_id = quality_node.node_id) " +
		"JOIN tagval AS curator_tag ON ((curator_tag.node_id = posited_by_link.object_id) " +
		"	AND (curator_tag.tag_id = ___has_curators)) " +
		"JOIN tagval AS char_tag ON ((char_tag.node_id = character_node.node_id)  " +
		"	AND (char_tag.tag_id = ___has_comment)) " +
		"JOIN tagval AS state_tag ON ((state_tag.node_id = state_node.node_id) " +
		"	AND (state_tag.tag_id = ___has_comment)) " + 
		"JOIN tagval AS char_num_tag ON ((char_num_tag.node_id = character_node.node_id) AND " +
		"	(char_num_tag.tag_id = ___has_number)) " + 
		"WHERE " +
		"exhibits_link.reiflink_node_id = ? AND " +
		"inheres_in_link.predicate_id = ___inheres_in AND " +
		"is_a_link.predicate_id = ___is_a AND " +
		"posited_by_link.predicate_id = ___posited_by AND " +
		"has_state_link.predicate_id = ___has_State AND " +
		"has_state_link2.predicate_id = ___has_State AND " +
		"has_character_link.predicate_id = ___has_Datum AND " +
		"has_pub_link.predicate_id = ___has_publication AND " +
		"exhibits_link.is_inferred = 'f' ";

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
	}
	
	/*
	 * These getter methods return the queries with all the node ids correctly inserted
	 */
	public String getTaxonSummaryQuery(){
		return replacePatternsWithIds(taxonSummaryQuery);
	}
	
	public String getAnatomyQuery() {
		return replacePatternsWithIds(anatomyQuery);
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
	
	/**
	 * This method cycles through the input query and replaces all the patterns from the enumeration 
	 * that it finds in the query with the correct node id
	 * @param query
	 * @return
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
	 * @return
	 */
	public Map<String, Integer> getRelationNodeIds(){
		return relationNodeIds;
	}
	
}
