package com.pagr.odata2;

import com.googlecode.objectify.Key;
import com.pagr.backend.CellUpdate;

import org.apache.olingo.odata2.api.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.edm.EdmLiteralKind;
import org.apache.olingo.odata2.api.edm.EdmProperty;
import org.apache.olingo.odata2.api.edm.EdmSimpleType;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.apache.olingo.odata2.api.ep.EntityProviderWriteProperties;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.exception.ODataNotFoundException;
import org.apache.olingo.odata2.api.exception.ODataNotImplementedException;
import org.apache.olingo.odata2.api.processor.ODataResponse;
import org.apache.olingo.odata2.api.processor.ODataSingleProcessor;
import org.apache.olingo.odata2.api.uri.KeyPredicate;
import org.apache.olingo.odata2.api.uri.info.GetEntitySetUriInfo;
import org.apache.olingo.odata2.api.uri.info.GetEntityUriInfo;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static com.pagr.backend.OfyService.ofy;


public class MyODataSingleProcessor extends ODataSingleProcessor {

    private static final Logger log = Logger.getLogger(MyODataSingleProcessor.class.getName());


    @Override
    public ODataResponse readEntitySet(GetEntitySetUriInfo uriInfo, String contentType) throws ODataException {

        EdmEntitySet entitySet;

        if (uriInfo.getNavigationSegments().size() == 0) {
            entitySet = uriInfo.getStartEntitySet();

            if (MyEdmProvider.ENTITY_SET_NAME_CELLUPDATES.equals(entitySet.getName())) {
                return EntityProvider.writeFeed(contentType, entitySet, getCellUpdates(), EntityProviderWriteProperties.serviceRoot(getContext().getPathInfo().getServiceRoot()).build());
            }

            throw new ODataNotFoundException(ODataNotFoundException.ENTITY);

        }

        throw new ODataNotImplementedException();
    }

    private List<Map<String, Object>> getCellUpdates() {
        List<CellUpdate> list = ofy().load().type(CellUpdate.class).list();
        List<Map<String, Object>> result = new ArrayList<>();
        for (CellUpdate cellUpdate : list) {
            Map<String, Object> element = serialize(cellUpdate);
            result.add(element);
        }
        return result;
    }

    private Map<String, Object> serialize(CellUpdate cellUpdate) {
        Map<String, Object> element = new HashMap<>();
        element.put("Id", cellUpdate.getId());
        return element;
    }

    private Map<String, Object> getCellUpdate(long id) {
        CellUpdate cellUpdate = ofy().load().key(Key.create(CellUpdate.class, id)).now();
        return serialize(cellUpdate);
    }

    @Override
    public ODataResponse readEntity(GetEntityUriInfo uriInfo, String contentType) throws ODataException {
        if (uriInfo.getNavigationSegments().size() == 0) {
            EdmEntitySet entitySet = uriInfo.getStartEntitySet();
            if (MyEdmProvider.ENTITY_SET_NAME_CELLUPDATES.equals(entitySet.getName())) {
                int id = getKeyValue(uriInfo.getKeyPredicates().get(0));
                Map<String, Object> data = getCellUpdate(id);
                if (data != null) {
                    URI serviceRoot = getContext().getPathInfo().getServiceRoot();
                    EntityProviderWriteProperties.ODataEntityProviderPropertiesBuilder propertiesBuilder = EntityProviderWriteProperties.serviceRoot(serviceRoot);
                    return EntityProvider.writeEntry(contentType, entitySet, data, propertiesBuilder.build());
                }
            }
            throw new ODataNotFoundException(ODataNotFoundException.ENTITY);
        }
        throw new ODataNotImplementedException();
    }

    private int getKeyValue(KeyPredicate key) throws ODataException {
        EdmProperty property = key.getProperty();
        EdmSimpleType type = (EdmSimpleType) property.getType();
        return type.valueOfString(key.getLiteral(), EdmLiteralKind.DEFAULT, property.getFacets(), Integer.class);
    }

}
