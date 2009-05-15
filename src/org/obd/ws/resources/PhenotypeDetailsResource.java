package org.obd.ws.resources;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.obd.model.Node;
import org.obd.model.Statement;
import org.obd.query.Shard;
import org.obd.ws.util.Queries;
import org.phenoscape.obd.OBDQuery;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

public class PhenotypeDetailsResource extends Resource {

	Logger log = Logger.getLogger(this.getClass());
	
	private String subject_id;
	private String entity_id;
	private String quality_id; 
	private String publication_id;
	private String type;
	
	private Map<String, String> parameters;
	
	private JSONObject jObjs;
	private Shard obdsql;
	private OBDQuery obdq;
	
	private Queries queries;
    /**
     * FIXME Constructor and parameter documentation missing.
     */
	public PhenotypeDetailsResource(Context context, Request request, Response response) {
		super(context, request, response);

		this.obdsql = (Shard) this.getContext().getAttributes().get("shard");
		this.getVariants().add(new Variant(MediaType.APPLICATION_JSON));

		if(request.getResourceRef().getQueryAsForm().getFirstValue("subject") != null){
			this.subject_id = Reference.decode((String) (request.getResourceRef().getQueryAsForm().getFirstValue("subject")));
		}
		if(request.getResourceRef().getQueryAsForm().getFirstValue("entity") != null){
			this.entity_id = Reference.decode((String)(request.getResourceRef().getQueryAsForm().getFirstValue("entity")));
		}
		if(request.getResourceRef().getQueryAsForm().getFirstValue("quality") != null){
			this.quality_id = Reference.decode((String)(request.getResourceRef().getQueryAsForm().getFirstValue("quality")));
		}
		if(request.getResourceRef().getQueryAsForm().getFirstValue("publication") != null){
			this.publication_id = Reference.decode((String)(request.getResourceRef().getQueryAsForm().getFirstValue("publication")));
		}
		if(request.getResourceRef().getQueryAsForm().getFirstValue("type") != null){
			this.type = Reference.decode((String)(request.getResourceRef().getQueryAsForm().getFirstValue("type")));
		}
		
		queries = new Queries(obdsql);
		obdq = new OBDQuery(obdsql);
		jObjs = new JSONObject();
		parameters = new HashMap<String, String>();
	}
	
    /**
     * FIXME Method and parameter documentation missing.
     */
	public Representation represent(Variant variant) 
            throws ResourceException {

		Representation rep;
		List<List<String[]>> annots;

		if (subject_id != null && !subject_id.startsWith("TTO:") && !subject_id.contains("GENE")) {
			this.jObjs = null;
			getResponse().setStatus(
					Status.CLIENT_ERROR_BAD_REQUEST,
					"ERROR: The input parameter for subject "
							+ "is not a recognized taxon or gene");
			return null;
		}
		if(entity_id != null && !entity_id.startsWith("TAO:") && !entity_id.startsWith("ZFA:")){
			this.jObjs = null;
			getResponse().setStatus(
					Status.CLIENT_ERROR_BAD_REQUEST,
					"ERROR: The input parameter for entity "
							+ "is not a recognized anatomical entity");
			return null;
		}
		if(quality_id != null && !quality_id.startsWith("PATO:")){
			this.jObjs = null;
			getResponse().setStatus(
					Status.CLIENT_ERROR_BAD_REQUEST,
					"ERROR: The input parameter for quality "
							+ "is not a recognized PATO quality");
			return null;
		}
		if(type != null && !type.equals("evo") && !type.equals("devo")){
			this.jObjs = null;
			getResponse().setStatus(
					Status.CLIENT_ERROR_BAD_REQUEST,
					"ERROR: [INVALID PARAMETER] The input parameter for taxon type can only be "
							+ "'evo' or 'devo'");
			return null;
		}
			
		//TODO Publication ID check
		
		parameters.put("entity_id", entity_id);
		parameters.put("quality_id", quality_id);
		parameters.put("subject_id", subject_id);
		parameters.put("publication_id", publication_id);
		
		for(String key : parameters.keySet()){
			if(parameters.get(key) != null){
				if(obdsql.getNode(parameters.get(key)) == null){
					getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,
					"No annotations were found with the specified search parameters");
					return null;
				}
			}
		}
		
		try{
			annots = getAnnotations(subject_id, entity_id, quality_id, publication_id, type);
		}
		/* 'getAnnotations' method returns null in case of a server side exception*/
		catch(SQLException sqle){
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, 
					"[SQL EXCEPTION] Something broke server side. Consult server logs");
			return null;
		}
		List<String[]> comp;
		
		JSONObject subjectObj, qualityObj, entityObj, phenotypeObj, reifObj; 
		List<JSONObject> phenotypeObjs = new ArrayList<JSONObject>();
		
		try{
			for(int i = 0; i < annots.size(); i++){
				comp = annots.get(i);
				phenotypeObj = new JSONObject();
				subjectObj = new JSONObject();
				entityObj = new JSONObject();
				qualityObj = new JSONObject();
				reifObj = new JSONObject();
				subjectObj.put("id", comp.get(0)[0]);
				subjectObj.put("name", comp.get(0)[1]);
				entityObj.put("id", comp.get(1)[0]);
				entityObj.put("name", comp.get(1)[1]);
				qualityObj.put("id", comp.get(2)[0]);
				qualityObj.put("name", comp.get(2)[1]);
				reifObj.put("reif_id", comp.get(3)[0]);
				phenotypeObj.put("subject", subjectObj);
				phenotypeObj.put("entity", entityObj);
				phenotypeObj.put("quality", qualityObj);
				phenotypeObj.put("reified_link", reifObj);	
				phenotypeObjs.add(phenotypeObj);
			}
			log.trace(annots.size() + " annotations returned");
			this.jObjs.put("phenotypes", phenotypeObjs);
		}
		catch(JSONException jsone){
                    /* FIXME Need to provide information to the
                     * client, so add an appropriate message.
                     */
                    log.error(jsone);
                    throw new ResourceException(jsone);
		}
		rep = new JsonRepresentation(this.jObjs);
		return rep;
	}

	/**
	 * @PURPOSE: This method takes the nodes returned by OBDQuery class and packages them into a 
	 * data structure
	 * @param subject_id - can be a TTO taxon or ZFIN GENE
	 * @param entity_id - can only be a TAO term (anatomical entity)
	 * @param char_id - PATO character
	 * @param pub_id - publication id or citation information
	 * @param type - can take one of two values. 'evo' for taxon data or 'devo' for ZFIN data
	 * @return 
	 * @throws SQLException 
	 */
	private List<List<String[]>> 
			getAnnotations(String subject_id, String entity_id, String char_id, String pub_id, String type) 
			throws SQLException{
		
		Map<String, String> nodeProps;
				
		List<List<String[]>> results = new ArrayList<List<String[]>>();
		List<String[]> annots;
		
		/* This is a data structure to keep track of user specified filter options. 
		 * Four filtering options can be specified viz. entity, character, subject, and 
		 * publication  */
		Map<String, String> filterOptions = new HashMap<String, String>();
		
		String relId, target, characterId = null, taxonId = null, entityId = null, qualityId = null,
					character = null, taxon = null, entity = null, quality = null, reifId = null;
		String query, searchTerm;
		/* 
		 * This IF-THEN decides which query to use. Ideally if subject is provided, we will use
		 * gene or taxon query. Otherwise, we use entity query
		 */
		if(subject_id != null){
			if(subject_id.contains("GENE"))
				query = queries.getGeneQuery();
			else
				query = queries.getTaxonQuery();
			searchTerm = subject_id;
			filterOptions.put("subject", null);
			filterOptions.put("entity", entity_id);
		}
		else{
			query = queries.getAnatomyQuery();
			/*	neither subject or entity are provided. so we use the root TAO term
			 * which returns every phenotype in the database
			 */
			searchTerm = (entity_id != null ? entity_id : "TAO:0100000");
			filterOptions.put("subject", subject_id);
			filterOptions.put("entity", null);
		}
		filterOptions.put("character", char_id);
		filterOptions.put("publication", null); //TODO pub_id goes here;
		
		log.trace("Search Term: " + searchTerm + " Query: " + query);
		try{
			for(Node node : obdq.executeQueryAndAssembleResults(query, searchTerm, filterOptions)){
				nodeProps = new HashMap<String, String>();
				for(Statement stmt : node.getStatements()){
					relId = stmt.getRelationId();
					target = stmt.getTargetId();
					nodeProps.put(relId, target);
				} 
				nodeProps.put("id", node.getId());
				characterId = nodeProps.get("hasCharacterId");
				character = nodeProps.get("hasCharacter");
				taxonId = nodeProps.get("exhibitedById");
				taxon = nodeProps.get("exhibitedBy");
				entityId = nodeProps.get("inheresInId");
				entity = nodeProps.get("inheresIn");
				qualityId = nodeProps.get("hasStateId");
				quality = nodeProps.get("hasState");
				reifId = nodeProps.get("hasReifId");
				log.trace("Char: " + characterId + " [" + character + "] Taxon: " + taxonId + "[" + taxon + "] Entity: " +
						entityId + "[" + entity + "] Quality: " + qualityId + "[" + quality + "]");
				if((type != null && !filterNodeForEvoOrDevo(taxonId, type)) || //type is set, so we filter
						(type == null)){
					annots = new ArrayList<String[]>();
					annots.add(new String[]{taxonId, taxon});
					annots.add(new String[]{entityId, entity});
					annots.add(new String[]{qualityId, quality});
					annots.add(new String[]{reifId});
					results.add(annots);
				}
			}
		}
		catch(SQLException e){
			log.fatal(e);
			throw e;
		}
		return results;
	}
	
    /**
     * FIXME Method and parameter documentation incomplete.
     */
	/**
	 * This method filters a node based on input parameter 'type'
	 * @param searchTerm - this may be a TTO term or GENE term
	 * @param type - this can be 'evo' or 'devo'
	 * @return
	 */
	private boolean filterNodeForEvoOrDevo(String taxonId, String type){
		if((type.equals("evo") && taxonId.contains("TTO"))
				|| (type.equals("devo") && taxonId.contains("GENE"))){
			return false;
		}
		return true;
	}
}
