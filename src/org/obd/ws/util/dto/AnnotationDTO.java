package org.obd.ws.util.dto;

/**
* This is a Data Transfer Object to be used for
* ferrying the annotation data retrieved by SQL queries to the 
* REST services in a defacto persistence layer
* <p>
* Each of the fields in this DTO corresponds to the columns
* returned by an SQL query, which retrieves metadata about 
* an assertion. 
* @author cartik
*/

public class AnnotationDTO{
	
	/** This field is the unique identifier for the retrieved metadata information. 
	 * It can be set only by the constructor */
	private String annotationId; 
	
	/*
	 * These variables are for storing the values of the 
	 * columns from a standard free text annotation query
	 */
	
	private String taxonId;
	private String taxon;
	
	private String entityId; 
	private String entity;
	
	private String qualityId;
	private String quality;
	
	private String publication;
	
	private String charText;
	private String charComments;
	private String charNumber;
	
	private String stateText;
	private String stateComments;
	
	private String curators; 
	
	/*
	 * GETTERs and SETTERs
	 */

	public String getTaxonId() {
		return taxonId;
	}

	public void setTaxonId(String taxonId) {
		this.taxonId = taxonId;
	}

	public String getTaxon() {
		return taxon;
	}

	public void setTaxon(String taxon) {
		this.taxon = taxon;
	}

	public String getEntityId() {
		return entityId;
	}

	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}

	public String getEntity() {
		return entity;
	}

	public void setEntity(String entity) {
		this.entity = entity;
	}

	public String getQualityId() {
		return qualityId;
	}

	public void setQualityId(String qualityId) {
		this.qualityId = qualityId;
	}

	public String getQuality() {
		return quality;
	}

	public void setQuality(String quality) {
		this.quality = quality;
	}

	public String getPublication() {
		return publication;
	}

	public void setPublication(String publication) {
		this.publication = publication;
	}

	public String getCharText() {
		return charText;
	}

	public void setCharText(String charText) {
		this.charText = charText;
	}

	public String getCharComments() {
		return charComments;
	}

	public void setCharComments(String charComments) {
		this.charComments = charComments;
	}

	public String getCharNumber(){
		return charNumber;
	}
	
	public void setCharNumber(String charNumber){
		this.charNumber = charNumber;
	}
	
	public String getStateText() {
		return stateText;
	}

	public void setStateText(String stateText) {
		this.stateText = stateText;
	}

	public String getStateComments() {
		return stateComments;
	}

	public void setStateComments(String stateComments) {
		this.stateComments = stateComments;
	}

	public String getCurators() {
		return curators;
	}

	public void setCurators(String curators) {
		this.curators = curators;
	}

	//Annotation ID is read only
	public String getAnnotationId() {
		return annotationId;
	}
	
	/**
	 * Constructor sets the annotation id
	 * @param annotationId In the database, this is the unique identifier that links the actual Taxon-Phenotype annotation
	 * to the metadata. It serves a similar purpose in the DTO
	 */
	public AnnotationDTO(String annotationId){
		this.annotationId = annotationId;	
	}
	
}
