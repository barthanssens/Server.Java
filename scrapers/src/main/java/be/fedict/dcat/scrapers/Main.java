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


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class Main {
    private final static Logger logger = LoggerFactory.getLogger(Main.class);
    
    private final static Properties prop = new Properties();
    
    /**
     * Exit cleanly
     * 
     * @param code return code 
     */
    protected static void exit(int code) {
        logger.info("-- STOP --");
        System.exit(code);
    }
    
    /**
     * Set proxy, if needed.
     * 
     * @param s Scraper instance.
     */
    private static void setProxy(Scraper s) {
        String proxy = prop.getProperty(Scraper.PROP_PREFIX + ".proxy.host", "");
        String port = prop.getProperty(Scraper.PROP_PREFIX + ".proxy.port", "");
        if (!proxy.isEmpty()) {
            s.setProxy(proxy, Integer.parseInt(port));
        }
    }
        
     /**
     * Load a scraper and scrape the site.
     * 
     * @param prefix properties prefix for additional configuration
     */
    private static Scraper getScraper() {
        String name = prop.getProperty(Scraper.PROP_PREFIX + ".classname");
        
        String cache = prop.getProperty(Scraper.PROP_PREFIX + ".cache", "");
        String store = prop.getProperty(Scraper.PROP_PREFIX + ".store", "");
        String url = prop.getProperty(Scraper.PROP_PREFIX + ".url", "");
         
        Scraper s = null;
        try {
            Class<? extends Scraper> c = Class.forName(name).asSubclass(Scraper.class);
            s = c.getConstructor(File.class, File.class, URL.class).
                newInstance(new File(cache), new File(store), new URL(url));
            s.setProperties(prop, Scraper.PROP_PREFIX);
            setProxy(s);
        } catch (ClassNotFoundException|InstantiationException|NoSuchMethodException|
                            IllegalAccessException|InvocationTargetException ex) {
            logger.error("Scraper class {} could not be loaded", name, ex);
            exit(-3);
        } catch (MalformedURLException ex) {
            logger.error("Base URL invalid {}", url, ex);
            exit(-3);
        }
        return s;
    }
    
    
    /**
     * Write result of scrape to DCAT file
     * 
     * @param scraper 
     */
    private static void writeDcat(Scraper scraper) {
        String out = prop.getProperty(Scraper.PROP_PREFIX + ".rdfout");
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(out)));
            scraper.writeDcat(bw);
        } catch (IOException ex) {
            logger.error("Error writing output file {}", out, ex);
            exit(-5);
        } catch (RepositoryException ex) {
            logger.error("Repository error", ex);
            exit(-6);          
        }
    }
    
    public static void main(String[] args) { 
        logger.info("-- START --");
        if (args.length == 0) {
            logger.error("No config file");
            exit(-1);
        }
        
        File config = new File(args[0]);
        try {
            prop.load(new FileInputStream(config));
        } catch (IOException ex) {
            logger.error("I/O Exception while reading {}", config, ex);
            exit(-2);
        }
        
        Scraper scraper = getScraper();
        
        try {
            scraper.scrape();
        } catch (IOException ex) {
            logger.error("Error while scraping", ex);
            exit(-4);
        }
        
        writeDcat(scraper);
        
        exit(0);
    }
}