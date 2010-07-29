package org.phenoscape.ws.resource;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.phenoscape.obd.model.TaxonAnnotation;
import org.phenoscape.obd.query.AnnotationsQueryConfig;
import org.phenoscape.obd.query.AnnotationsQueryConfig.SORT_COLUMN;

public class TaxonAnnotationsResource extends AnnotationQueryingResource<TaxonAnnotation> {

    private static final Map<String,SORT_COLUMN> COLUMNS = new HashMap<String,SORT_COLUMN>();
    static {
        COLUMNS.put("taxon", SORT_COLUMN.TAXON);
        COLUMNS.put("entity", SORT_COLUMN.ENTITY);
        COLUMNS.put("quality", SORT_COLUMN.QUALITY);
        COLUMNS.put("relatedentity", SORT_COLUMN.RELATED_ENTITY);
    }

    @Override
    protected int queryForItemsCount(AnnotationsQueryConfig config) throws SQLException {
        return this.getDataStore().getCountOfDistinctTaxonomicAnnotations(config);
    }

    @Override
    protected List<TaxonAnnotation> queryForItemsSubset(AnnotationsQueryConfig config) throws SQLException {
        return this.getDataStore().getDistinctTaxonAnnotations(config);
    }

    @Override
    protected JSONObject translateToJSON(TaxonAnnotation annotation) throws JSONException {
        final JSONObject json = new JSONObject();
        final JSONObject taxon = new JSONObject();
        taxon.put("id", annotation.getTaxon().getUID());
        taxon.put("name", annotation.getTaxon().getLabel());
        taxon.put("extinct", annotation.getTaxon().isExtinct());
        if (annotation.getTaxon().getRank() != null) {
            final JSONObject rank = new JSONObject();
            rank.put("id", annotation.getTaxon().getRank().getUID());
            rank.put("name", annotation.getTaxon().getRank().getLabel());
            taxon.put("rank", rank);
        }
        json.put("taxon", taxon);
        final JSONObject entity = new JSONObject();
        entity.put("id", annotation.getEntity().getUID());
        entity.put("name", annotation.getEntity().getLabel());
        json.put("entity", entity);
        final JSONObject quality = new JSONObject();
        quality.put("id", annotation.getQuality().getUID());
        quality.put("name", annotation.getQuality().getLabel());
        json.put("quality", quality);
        if (annotation.getRelatedEntity() != null) {
            final JSONObject relatedEntity = new JSONObject();
            relatedEntity.put("id", annotation.getRelatedEntity().getUID());
            relatedEntity.put("name", annotation.getRelatedEntity().getLabel());
            json.put("related_entity", relatedEntity);
        }
        return json;    
    }

    @Override
    protected String translateToText(TaxonAnnotation annotation) {
        final StringBuffer buffer = new StringBuffer();
        final String tab = "\t";
        buffer.append(annotation.getTaxon().getUID());
        buffer.append(tab);
        buffer.append(annotation.getTaxon().getLabel());
        buffer.append(tab);
        buffer.append(annotation.getEntity().getUID());
        buffer.append(tab);
        buffer.append(annotation.getEntity().getLabel());
        buffer.append(tab);
        buffer.append(annotation.getQuality().getUID());
        buffer.append(tab);
        buffer.append(annotation.getQuality().getLabel());
        buffer.append(tab);
        buffer.append(annotation.getRelatedEntity() != null ? annotation.getRelatedEntity().getUID() : "");
        buffer.append(tab);
        buffer.append(annotation.getRelatedEntity() != null ? annotation.getRelatedEntity().getLabel() : "");
        return buffer.toString();
    }

    @Override
    protected String getItemsKey() {
        return "annotations";
    }

    @Override
    protected SORT_COLUMN getDefaultSortColumn() {
        return SORT_COLUMN.TAXON;
    }

    @Override
    protected Map<String, SORT_COLUMN> getSortColumns() {
        return COLUMNS;
    }


}
