package org.phenoscape.ws.resource;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrServerException;
import org.json.JSONException;
import org.json.JSONObject;
import org.phenoscape.obd.model.GeneTerm;
import org.phenoscape.obd.query.AnnotationsQueryConfig;
import org.phenoscape.obd.query.AnnotationsQueryConfig.SORT_COLUMN;

public class GenesResource extends AnnotationQueryingResource<GeneTerm> {

    private static final Map<String,SORT_COLUMN> COLUMNS = new HashMap<String,SORT_COLUMN>();
    static {
        COLUMNS.put("gene", SORT_COLUMN.GENE);
        COLUMNS.put("fullname", SORT_COLUMN.GENE_FULLNAME);
    }

    @Override
    protected String getItemsKey() {
        return "genes";
    }

    @Override
    protected SORT_COLUMN getDefaultSortColumn() {
        return SORT_COLUMN.GENE;
    }

    @Override
    protected long queryForItemsCount(AnnotationsQueryConfig config) throws SQLException, SolrServerException {
        config.setLimit(0);
        return this.getDataStore().getAnnotatedGenesSolr(config).getTotal();
    }

    @Override
    protected List<GeneTerm> queryForItemsSubset(AnnotationsQueryConfig config) throws SQLException, SolrServerException {
        return this.getDataStore().getAnnotatedGenesSolr(config).getList();
    }

    @Override
    protected JSONObject translateToJSON(GeneTerm gene) throws JSONException {
        final JSONObject json = this.createBasicJSONTerm(gene);
        json.put("fullname", gene.getFullName());
        return json;
    }

    @Override
    protected String translateToText(GeneTerm gene) {
        final StringBuffer text = new StringBuffer();
        final String tab = "\t";
        text.append(gene.getUID());
        text.append(tab);
        text.append(gene.getLabel());
        text.append(tab);
        text.append(gene.getFullName());
        return text.toString();
    }

    @Override
    protected Map<String, SORT_COLUMN> getSortColumns() {
        return COLUMNS;
    }

}
