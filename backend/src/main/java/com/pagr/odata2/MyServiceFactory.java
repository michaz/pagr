package com.pagr.odata2;

import org.apache.olingo.odata2.api.ODataCallback;
import org.apache.olingo.odata2.api.ODataDebugCallback;
import org.apache.olingo.odata2.api.ODataService;
import org.apache.olingo.odata2.api.ODataServiceFactory;
import org.apache.olingo.odata2.api.edm.provider.EdmProvider;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.apache.olingo.odata2.api.exception.ODataApplicationException;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.processor.ODataContext;
import org.apache.olingo.odata2.api.processor.ODataErrorCallback;
import org.apache.olingo.odata2.api.processor.ODataErrorContext;
import org.apache.olingo.odata2.api.processor.ODataResponse;
import org.apache.olingo.odata2.api.processor.ODataSingleProcessor;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MyServiceFactory extends ODataServiceFactory {

    private static final Logger log = Logger.getLogger(MyServiceFactory.class.getName());

    @Override
    public ODataService createService(ODataContext ctx) throws ODataException {

        EdmProvider edmProvider = new MyEdmProvider();
        ODataSingleProcessor singleProcessor = new MyODataSingleProcessor();

        return createODataSingleProcessorService(edmProvider, singleProcessor);
    }

    @Override
    public <T extends ODataCallback> T getCallback(Class<? extends ODataCallback> callbackInterface) {
        if (callbackInterface.isAssignableFrom(ODataErrorCallback.class)) {
            return (T) new ODataErrorCallback() {
                @Override
                public ODataResponse handleError(ODataErrorContext context) throws ODataApplicationException {
                    log.severe(context.getException().getClass().getName() + ":" + context.getMessage());
                    log.severe(context.getRequestUri().toString());
                    log.log(Level.SEVERE, "an exception was thrown", context.getException());
                    return EntityProvider.writeErrorDocument(context);
                }
            };
        } else if (callbackInterface.isAssignableFrom(ODataDebugCallback.class)) {
            return (T) new ODataDebugCallback() {
                @Override
                public boolean isDebugEnabled() {
                    return false;
                }
            };
        }
        return super.getCallback(callbackInterface);
    }
}