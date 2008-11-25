package org.obd.ws.application;

import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.Router;

public class OBDApplication extends Application {

	public OBDApplication(Context context){
		super(context);
	}
	
    @Override
    public Restlet createRoot() {
        final Router router = new Router(this.getContext());
        // URL mappings
        router.attach("/phenoscape/term/{termID}", org.obd.ws.resources.TermResource.class);
        router.attach("/phenoscape/term/search/{text}?name=[true|false]&syn=[true|false]&def=[true|false]" +
        					"&ontology=[TTO, TAO, ZFA, PATO, OBO_RO, SPATIAL, UNIT, SEQUENCE, COLLECTION, PHENOSCAPE]", 
        					org.obd.ws.resources.AutoCompleteResource.class);
        return router;
    }

}