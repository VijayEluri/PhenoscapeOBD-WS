package org.obd.ws.application;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.bbop.dataadapter.DataAdapterException;
import org.obd.query.impl.OBDSQLShard;
import org.obd.ws.exceptions.PhenoscapeDbConnectionException;
import org.obd.ws.util.Queries;
import org.obd.ws.util.TTOTaxonomy;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;
import org.restlet.routing.Template;

public class OBDApplication extends Application {

    private Queries queries;
    private OBDSQLShard obdsql;
    /** A structure which maps ontology prefixes to their 
     * default namespaces */
    final private Map<String, Set<String>> prefixToDefaultNamespacesMap = new HashMap<String, Set<String>>();
    /** A structure to map default namespaces of ontologies to their
     * node ids in the database */
    final private Map<String, String> defaultNamespaceToNodeIdMap = new HashMap<String, String>();
    
    /** GETTER for the map from default namespaces of ontologies 
     * to their node ids in the database */
    public Map<String, String> getDefaultNamespaceToNodeIdMap() {
        return defaultNamespaceToNodeIdMap;
    }
    /** GETTER for the map from ontology prefixes to default namespaces
     */
    public Map<String, Set<String>> getPrefixToDefaultNamespacesMap() {
        return prefixToDefaultNamespacesMap;
    }

    private static final String JNDI_KEY = "java:/comp/env/jdbc/OBD";
    public static final String DATA_SOURCE_KEY = "org.phenoscape.jndi.obd.datasource";
    public static final String PREFIX_TO_NS_FILE = "PrefixToDefaultNamespaceOfOntology.properties";
    public static final String PREFIX_TO_DEFAULT_NAMESPACE_MAP_STRING = "prefixToDefaultNamespacesMap";
    public static final String DEFAULT_NAMESPACE_TO_SOURCE_ID_MAP_STRING = "defaultNamespacesToSourceIdMap";
    public static final String TTO_TAXONOMY_STRING = "ttoTaxonomy";
    public static final String QUERIES_STRING = "queries";  

    /**
     * Selects the Shard pointing to the most recently updated database to be used by the 
     * data services
     * Then this method sets a number of context level parameters
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws IOException
     * @throws ParseException
     * @throws PhenoscapeDbConnectionException
     * @throws DataAdapterException
     * @throws NamingException 
     * @throws NamingException 
     * @throws ClassNotFoundException 
     * @throws SQLException 
     */
    private void connect() throws IOException, ParseException, PhenoscapeDbConnectionException, DataAdapterException, NamingException, SQLException, ClassNotFoundException {
        //TODO this method should probably be removed or significantly revised
        final InitialContext initialContext = new InitialContext();
        final DataSource dataSource = (DataSource)(initialContext.lookup(JNDI_KEY));
        this.getContext().getAttributes().put(DATA_SOURCE_KEY, dataSource);

        obdsql = new OBDSQLShard();
        obdsql.connect(dataSource);
        if(obdsql != null) {
            queries = new Queries(obdsql);
            this.getContext().getAttributes().put(QUERIES_STRING, queries);
            this.constructPrefixToDefaultNamespacesMap();
            this.constructDefaultNamespaceToNodeIdMap();
            this.getContext().getAttributes().put(PREFIX_TO_DEFAULT_NAMESPACE_MAP_STRING, this.prefixToDefaultNamespacesMap);
            this.getContext().getAttributes().put(DEFAULT_NAMESPACE_TO_SOURCE_ID_MAP_STRING, this.defaultNamespaceToNodeIdMap);
        } else {
            throw new PhenoscapeDbConnectionException("Failed to obtain a connection to the database. " +
            "This is because neither database is ready to be queried. ");
        }
        TTOTaxonomy ttoTaxonomy = new TTOTaxonomy();
        this.getContext().getAttributes().put(TTO_TAXONOMY_STRING, ttoTaxonomy);
    }

    /**
     * The router method. 
     * It holds mappings from URL patterns to the appropriate REST service to be invoked
     */
    @Override
    public Restlet createInboundRoot() {
        try {
            connect();
        } catch (SQLException e) {
            log().fatal("Error connecting to SQL shard", e);
        } catch (ClassNotFoundException e) {
            log().fatal("Error creating SQL shard", e);
        } catch (IOException e) {
            log().fatal("Error reading connection properties file", e);
        } catch (ParseException e) {
            log().fatal("Error parsing the date", e);
        } catch (PhenoscapeDbConnectionException e) {
            log().fatal("Error with the database connection", e);
        } catch (DataAdapterException e) {
            log().fatal("Error reading in the OBO files", e);
        } catch (NamingException e) {
            log().fatal("Failed to create JDBC adapter via JNDI");
        }
        final Router router = new Router(this.getContext());
        // URL mappings
        router.attach("/phenotypes", org.obd.ws.resources.PhenotypeDetailsResource.class);
        router.attach("/phenotypes/summary", org.obd.ws.resources.PhenotypeSummaryResource.class);
        router.attach("/phenotypes/source/{annotation_id}", org.obd.ws.resources.AnnotationResource.class);
        router.attach("/term/search", org.obd.ws.resources.AutoCompleteResource.class);
        router.attach("/term/{termID}", org.obd.ws.resources.TermResource.class);
        router.attach("/term/{termID}/homology", org.obd.ws.resources.HomologyResource.class);
        router.attach("/timestamp", org.obd.ws.resources.KbRefreshTimestampResource.class).setMatchingMode(Template.MODE_STARTS_WITH);
        router.attach("/taxon/{taxonID}/treemap/",org.obd.ws.resources.SquarifiedTaxonMapResource.class);
        /* These resources generate data consistency reports*/
        router.attach("/statistics/consistencyreports/relationalqualitieswithoutrelatedentities",
                org.obd.ws.statistics.reports.resources.DataConsistencyReportGeneratorForQuestion21A.class);
        router.attach("/statistics/consistencyreports/nonrelationalqualitieswithrelatedentities",
                org.obd.ws.statistics.reports.resources.DataConsistencyReportGeneratorForQuestion21B.class);
        router.attach("/statistics/consistencyreports/characterswithonlyoneannotatedstate",
                org.obd.ws.statistics.reports.resources.DataConsistencyReportGeneratorForQuestion9.class);
        router.attach("/statistics/consistencyreports/characterswithonlyoneoftwopossibleannotations",
                org.obd.ws.statistics.reports.resources.DataConsistencyReportGeneratorForQuestion13.class);
        /* These resources generate summary statistics of the data */
        router.attach("/statistics/phenotypeannotationcount",
                org.obd.ws.statistics.resources.PhenotypeAnnotationCounts.class);
        router.attach("/statistics/countsofgenesandcharactersbyattribute",
                org.obd.ws.statistics.resources.CharactersAndGenesByAttribute.class);
        router.attach("/statistics/countsofgenesandcharactersbysystem",
                org.obd.ws.statistics.resources.CharactersAndGenesBySystem.class);
        router.attach("/statistics/countsofgenesandcharactersbysystemandclade",
                org.obd.ws.statistics.resources.CharactersAndGenesBySystemAndClade.class);
        router.attach("/statistics/countsofcharactersdatasetsandtaxabyclade",
                org.obd.ws.statistics.resources.CharactersDatasetsAndTaxaByClade.class);
        return router;
    }

    /**
     * PURPOSE This method reads in the list of default namespaces from a file and
     * adds the corresponding node ids to a map
     * @throws IOException
     * @throws SQLException 
     */
    private void constructDefaultNamespaceToNodeIdMap() throws SQLException{
        final String sourceNodeQuery = queries.getQueryForNodeIdsForOntologies();
        String nodeId, uid;
        final Connection conn = obdsql.getConnection();
        final java.sql.Statement stmt = conn.createStatement();
        final ResultSet rs = stmt.executeQuery(sourceNodeQuery);
        while(rs.next()){
            nodeId = rs.getString(1);
            uid = rs.getString(2);
            if (uid.length() > 0) {
                this.defaultNamespaceToNodeIdMap.put(uid, nodeId);
            }
        }
    }

    /**
     * PURPOSE This method constructs a mapping
     * from every prefix used in the autocompletion
     * service to the set of default namespaces of the
     * ontologies the prefix comes from\n
     * PROCEDURE This method reads the allowed 
     * prefix to namespace mappings from a static text 
     * file. This is converted into a map
     * @throws IOException
     */
    private void constructPrefixToDefaultNamespacesMap() throws IOException {
        final Properties props = new Properties();
        props.load(new StringReader(
                "oboInOwl=oboInOwl,oboInOwl:Subset\n" +
                "OBO_REL=relationship\n" +
                "PATO=quality,pato.ontology\n" +
                "ZFA=zebrafish_anatomy\n" +
                "TAO=teleost_anatomy\n" +
                "TTO=teleost-taxonomy\n" +
                "COLLECTION=museum\n" +
                "BSPO=spatial\n" +
                "SO=sequence\n" +
                "UO=unit.ontology\n" +
                "PHENOSCAPE=phenoscape_vocab\n" +
                "GO=gene_ontology,biological_process,molecular_function,cellular_component\n" +
                "ECO=evidence_code2.obo\n"));
        for (Object key : props.keySet()) {
            final Set<String> namespaceSet = new HashSet<String>();
            final String prefix = key.toString();
            final String commaDelimitedNamespaces = props.get(key).toString();
            for(String namespace : commaDelimitedNamespaces.split(",")){
                namespaceSet.add(namespace);
            }
            prefixToDefaultNamespacesMap.put(prefix, namespaceSet);
        }
    }
    
    private Logger log() {
        return Logger.getLogger(this.getClass());
    }
}
