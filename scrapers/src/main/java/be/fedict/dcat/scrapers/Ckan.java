/*
 * Copyright (c) 2015, Bart Hanssens <bart.hanssens@fedict.be>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package be.fedict.dcat.scrapers;

import be.fedict.dcat.helpers.Storage;
import be.fedict.dcat.helpers.Cache;
import be.fedict.dcat.vocab.DCAT;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import org.apache.http.client.fluent.Request;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scraper CKAN
 * 
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class Ckan extends Scraper {
    private final Logger logger = LoggerFactory.getLogger(Ckan.class);
   
    // CKAN JSON fields
    public final static String ID = "id";
    public final static String CREATED = "created";
    public final static String FORMAT = "format";
    public final static String MODIFIED = "last_modified";
    public final static String NAME = "name";
    public final static String NOTES = "notes";
    public final static String RESOURCES = "resources";
    public final static String TAGS = "tags";
    public final static String TITLE = "title";
    public final static String URL = "url";
    public final static String EXTRA = "extras";
    
    public final static String KEY = "key";
    public final static String VALUE = "value";
    
    
    public final static String RESULT = "result";
    public final static String SUCCESS = "success";
    
    // CKAN API
    public final static String API_LIST = "/api/3/action/package_list";
    public final static String API_PKG = "/api/3/action/package_show?id=";
    public final static String API_RES = "/api/3/action/resource_show?id=";
    

    private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSS");
    
    /**
     * Get URL of a CKAN Package (DCAT Dataset) 
     * 
     * @param id
     * @return URL
     * @throws java.net.MalformedURLException 
     */
    protected URL getPackageURL(String id) throws MalformedURLException {
        return new URL(getBase(), Ckan.API_PKG + id);
    }
    
    /**
     * Get URL of a CKAN resource (DCAT Distribution).
     * 
     * @param id
     * @return URL
     * @throws java.net.MalformedURLException 
     */
    protected URL getResourceURL(String id) throws MalformedURLException {
        return new URL(getBase(), Ckan.API_RES + id);
    }
    
    /**
     * Make HTTP GET request.
     * 
     * @param url
     * @return JsonObject containing CKAN info
     * @throws IOException 
     */
    protected JsonObject makeRequest(URL url) throws IOException {
        Request request = Request.Get(url.toString());
        if (getProxy() != null) {
            request = request.viaProxy(getProxy());
        }
        String json = request.execute().returnContent().asString();
        JsonReader reader = Json.createReader(new StringReader(json));
        return reader.readObject();
    }
    
    /**
     * Get a CKAN package (DCAT Dataset).
     * 
     * @param url
     * @return JsonObject containing CKAN Package or NULL
     * @throws IOException 
     */
    protected JsonObject getPackage(URL url) throws IOException {
        JsonObject obj = makeRequest(url);
        if (obj.getBoolean(Ckan.SUCCESS)) {
            return obj.getJsonObject(Ckan.RESULT);
        }
        return null;
    }
    
    /**
     * Get the list of all the CKAN packages (DCAT Dataset).
     * 
     * @return List of URLs
     * @throws MalformedURLException
     * @throws IOException 
     */
    protected List<URL> getPackageList() throws MalformedURLException, IOException {
        List<URL> urls = new ArrayList<>();
        URL getPackages = new URL(getBase(), Ckan.API_LIST);
        
        JsonObject obj = makeRequest(getPackages);
        if (! obj.getBoolean(Ckan.SUCCESS)) {
            return urls;
        }
        JsonArray arr = obj.getJsonArray(Ckan.RESULT);
        for (JsonString str : arr.getValuesAs(JsonString.class)) {
            urls.add(getPackageURL(str.getString()));
        }
        return urls;
    }
    
    /**
     * Fetch all metadata from the CKAN repository.
     * 
     * @throws IOException 
     */
    @Override
    public void scrape() throws IOException {
        logger.info("Start scraping");
        Cache cache = getCache();
        String lang = getDefaultLang();
        
        List<URL> urls = cache.retrieveURLList();
        if (urls.isEmpty()) {
            urls = getPackageList();
            cache.storeURLList(urls);
        }
        urls = cache.retrieveURLList();
        
        logger.info("Found {} CKAN packages", String.valueOf(urls.size()));
        
        int i = 0;
        for (URL u : urls) {
            Map<String, String> page = cache.retrievePage(u);
            if (page.isEmpty()) {
                sleep();
                if (++i % 100 == 0) {
                    logger.info("Package {}...", Integer.toString(i));
                }
                JsonObject obj = getPackage(u);
                cache.storePage(u, obj.toString(), lang);
            }
        }
        logger.info("Done scraping");
    }

    
    /**
     * Parse a CKAN string and store it in the RDF store.
     * 
     * @param store RDF store
     * @param uri RDF subject URI
     * @param obj JsonObject
     * @param field CKAN field name 
     * @param property RDF property
     * @param lang language
     * @throws RepositoryException 
     */
    protected void parseString(Storage store, URI uri, JsonObject obj, 
            String field, URI property, String lang) throws RepositoryException {
        String s = obj.getString(field, "");
        if (! s.isEmpty()) {
            if (lang != null) {
                store.add(uri, property, s, lang);
            } else {
                store.add(uri, property, s);
            }
        }
    }
    
    /**
     * Parse a CKAN string and store it in the RDF store
     * 
     * @param store RDF store
     * @param uri RDF subject URI
     * @param obj JsonObject
     * @param field CKAN field name 
     * @param property RDF property
     * @return
     * @throws RepositoryException 
     */
    protected void parseURI(Storage store, URI uri, JsonObject obj, 
                        String field, URI property) throws RepositoryException {
        String s = obj.getString(field, "");
        if (! s.isEmpty()) {
            store.add(uri, property, store.getURI(s));
        }
    }
    
   /**
     * Parse a CKAN date and store it in the RDF store
     * 
     * @param store RDF store
     * @param uri RDF subject URI
     * @param obj JsonObject
     * @param field CKAN field name 
     * @param property RDF property
     * @return
     * @throws RepositoryException 
     */
    protected Date parseDate(Storage store, URI uri, JsonObject obj, 
                        String field, URI property) throws RepositoryException {
        Date res = null;
        
        String s = obj.getString(field, "");
        if (! s.isEmpty()) {
            try {
                res = df.parse(s);
                store.add(uri, property, res);
            } catch (ParseException ex) {
                logger.warn("Could not parse date {}", s, ex);
            }
        }
        return res;
    }
    
    /**
     * Parse CKAN dataset in JSON format.
     * 
     * @param store RDF store
     * @param uri RDF subject
     * @param lang language
     * @param json
     * @throws RepositoryException 
     */
    protected void ckanGeneral(Storage store, URI uri, JsonObject json, String lang) 
                                                throws RepositoryException {
        parseString(store, uri, json, Ckan.ID, DCTERMS.IDENTIFIER, null);
        parseString(store, uri, json, Ckan.TITLE, DCTERMS.TITLE, lang); 
        parseString(store, uri, json, Ckan.NOTES, DCTERMS.DESCRIPTION, lang);
        
        parseDate(store, uri, json, "metadata_created", DCTERMS.CREATED);
        parseDate(store, uri, json, "metadata_modified", DCTERMS.MODIFIED);
        parseURI(store, uri, json, "license_url", DCTERMS.LICENSE);
    }
    
    /**
     * Parse CKAN tags in JSON format.
     * 
     * @param store RDF store
     * @param uri RDF subject
     * @param json
     * @param lang language
     * @throws RepositoryException 
     */
    protected void ckanTags(Storage store, URI uri, JsonObject json, String lang) 
                                                    throws RepositoryException {
        JsonArray arr = json.getJsonArray(Ckan.TAGS);
        
        for (JsonObject obj : arr.getValuesAs(JsonObject.class)) {
            parseString(store, uri, obj, Ckan.NAME, DCAT.KEYWORD, lang);
        }
    }
    
    /**
     * Parse CKAN resources.
     * 
     * @param store RDF store
     * @param uri 
     * @param json JSON
     * @param lang language
     * @throws RepositoryException
     * @throws MalformedURLException 
     */
    protected void ckanResources(Storage store, URI uri, JsonObject json, String lang) 
                                throws RepositoryException, MalformedURLException {
        JsonArray arr = json.getJsonArray(Ckan.RESOURCES);
        
        for (JsonObject obj : arr.getValuesAs(JsonObject.class)) {
            String s = obj.getString(Ckan.ID, "");
            URI distr = store.getURI(getResourceURL(s).toString());
            store.add(uri, DCAT.DISTRIBUTION, distr);
            store.add(distr, RDF.TYPE, DCAT.A_DISTRIBUTION);
        
            parseString(store, distr, obj, Ckan.ID, DCTERMS.IDENTIFIER, null);
            parseString(store, distr, obj, Ckan.NAME, DCTERMS.TITLE, lang);
            parseDate(store, distr, obj, Ckan.CREATED, DCTERMS.CREATED);
            parseDate(store, distr, obj, Ckan.MODIFIED, DCTERMS.MODIFIED);
            parseString(store, distr, obj, Ckan.FORMAT, DCAT.MEDIA_TYPE, null);
            parseString(store, distr, obj, Ckan.URL, DCAT.DOWNLOAD_URL, null);
        }
    }
    
    /**
     * Parse CKAN extra
     * 
     * @param store
     * @param uri
     * @param json
     * @param lang language
     * @throws RepositoryException
     * @throws MalformedURLException 
     */
    protected void ckanExtras(Storage store, URI uri, JsonObject json, String lang)
                               throws RepositoryException, MalformedURLException {
   //     JsonArray arr = json.getJsonArray("extras");
    }
    
    /**
     * Parse DCAT Datasets
     * 
     * @param page
     * @param store
     * @param lang language
     * @throws MalformedURLException
     * @throws RepositoryException 
     */
    public void parseDatasets(String page, Storage store, String lang) 
                            throws MalformedURLException, RepositoryException {
        JsonReader reader = Json.createReader(new StringReader(page));
        JsonObject obj = reader.readObject();
        
        String s = obj.getString(Ckan.NAME, "");
        URI dataset = store.getURI(getPackageURL(s).toString());
        store.add(dataset, RDF.TYPE, DCAT.A_DATASET);

        /* Parse different sections of CKAN JSON */
        ckanGeneral(store, dataset, obj, lang);
        ckanTags(store, dataset, obj, lang);
        ckanResources(store, dataset, obj, lang);
        ckanExtras(store, dataset,obj, lang);
    }
    
    /**
     * Parse DCAT Catalog.
     * 
     * @param urls
     * @param store
     * @throws RepositoryException 
     */
    public void parseCatalog(List<URL> urls, Storage store) throws RepositoryException {
        URI catalog = store.getURI(getBase().toString());
        store.add(catalog, RDF.TYPE, DCAT.A_CATALOG);
        
        for (URL u : urls ){
            store.add(catalog, DCAT.DATASET, store.getURI(u.toString()));
        }
    }
    
    /**
     * Write DCAT file to output.
     * 
     * @param out
     * @throws RepositoryException
     * @throws MalformedURLException 
     */
    @Override
    public void writeDcat(Writer out) throws RepositoryException, MalformedURLException {
        Storage store = getTripleStore();
        store.startup();
        
        Cache cache = getCache();
        String lang = getDefaultLang();
        
        /* Get the list of all datasets */
        List<URL> urls = cache.retrieveURLList();
        parseCatalog(urls, store);
        
        /* Get and parse all the datasets */
        for (URL u : urls) {
            Map<String, String> page = cache.retrievePage(u);
            String s = page.getOrDefault(lang, "");
            parseDatasets(s, store, lang);
        }
        cache.shutdown();
        
        store.write(out);
        store.shutdown();
    }
  
    /**
     * CKAN scraper.
     * 
     * @param caching local cache file
     * @param storage local triple store file
     * @param base URL of the CKAN site
     */
    public Ckan(File caching, File storage, URL base) {
        super(caching, storage, base);

   } 
}