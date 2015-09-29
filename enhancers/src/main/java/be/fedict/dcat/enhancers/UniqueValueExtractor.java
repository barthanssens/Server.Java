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
package be.fedict.dcat.enhancers;

import be.fedict.dcat.helpers.Storage;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extract the unique values of a property.
 * 
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class UniqueValueExtractor extends Enhancer {
    private final Logger logger = LoggerFactory.getLogger(UniqueValueExtractor.class);
    
    /**
     * Extract unique values and write them to outputstream.
     * 
     * @param uri property
     * @param out output stream
     * @throws RepositoryException
     * @throws IOException 
     */
    private void extract(URI uri, OutputStream out) 
                                        throws RepositoryException, IOException {
        logger.info("Extracting unique {} to {}", uri.toString(), out.toString());
        
        Storage store = getStore();
        Set<String> subjs = store.queryUniqueValues(uri, null);
        for(String s : subjs) {
            out.write(s.getBytes());
            out.write("\n".getBytes());
        }
        logger.info("{} unique values", Integer.toString(subjs.size()));
    }
    
    @Override
    public void enhance() {
        try {
            URI property = getStore().getURI(getProperty("property"));
            try (OutputStream out = new BufferedOutputStream(
                    new FileOutputStream(getProperty("outfile")))) {
                extract(property, out);
            }
        } catch (RepositoryException ex) {
            logger.error("Repository error", ex);
        } catch (IOException ex) {
            logger.error("I/O error", ex);
        }
    }
    
    /**
     * Extracts unique values.
     * 
     * @param store 
     */
    public UniqueValueExtractor(Storage store) {
        super(store);
    }
}
