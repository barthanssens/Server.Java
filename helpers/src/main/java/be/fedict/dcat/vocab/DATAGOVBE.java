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
package be.fedict.dcat.vocab;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;

/**
 *
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class DATAGOVBE {
    public static final String NAMESPACE = "http://data.gov.be#";
    
    public final static String PREFIX = "datagovbe";
  
    public final static URI LICENSE;
    public final static URI MEDIA_TYPE;
    public final static URI ORG;
    public final static URI SPATIAL;   
    public final static URI THEME;
    public final static URI LICENSE_CC0;
    
    public final static String PREFIX_URI_CAT = "http://data.gov.be/catalog";
    public final static String PREFIX_URI_DATASET = "http://data.gov.be/dataset";
    public final static String PREFIX_URI_DIST = "http://data.gov.be/distribution";
    
    static {
	ValueFactory factory = ValueFactoryImpl.getInstance();

        LICENSE = factory.createURI(DATAGOVBE.NAMESPACE, "license");
        MEDIA_TYPE = factory.createURI(DATAGOVBE.NAMESPACE, "mediaType");
        ORG = factory.createURI(DATAGOVBE.NAMESPACE, "org");
        SPATIAL = factory.createURI(DATAGOVBE.NAMESPACE, "spatial");
        THEME = factory.createURI(DATAGOVBE.NAMESPACE, "theme");
        
        LICENSE_CC0 = factory.createURI("http://creativecommons.org/publicdomain/zero/1.0/");
    }    
}
