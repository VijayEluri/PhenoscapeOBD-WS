package org.phenoscape.ws.resource.report;

import java.sql.SQLException;

import org.json.JSONException;
import org.json.JSONObject;
import org.phenoscape.obd.query.AnnotationsQueryConfig;
import org.phenoscape.ws.resource.AbstractPhenoscapeResource;
import org.restlet.data.CharacterSet;
import org.restlet.data.Language;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

public class CuratedTaxonAnnotationCountsResource extends AbstractPhenoscapeResource {
    
    private AnnotationsQueryConfig config = new AnnotationsQueryConfig();

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        try {
            this.config = this.initializeQueryConfig(this.getJSONQueryValue("query", new JSONObject()));
        } catch (JSONException e) {
            log().error("Bad JSON format", e);
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e);
        }
    }

    @Get("tsv")
    public Representation getTable() {
        try {
            final StringBuffer result = new StringBuffer();
            result.append("Count of curated taxon annotations");
            result.append(System.getProperty("line.separator"));
            result.append(this.getDataStore().getCountOfCuratedTaxonomicAnnotations(this.config));
            result.append(System.getProperty("line.separator"));
            return new StringRepresentation(result.toString(), MediaType.TEXT_TSV, Language.DEFAULT, CharacterSet.UTF_8);
        } catch (SQLException e) {
            log().error("Error querying taxon annotation counts", e);
            this.setStatus(Status.SERVER_ERROR_INTERNAL, e);
            return null;
        }
    }
    
}
