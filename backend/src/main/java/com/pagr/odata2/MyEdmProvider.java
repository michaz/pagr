package com.pagr.odata2;

import org.apache.olingo.odata2.api.edm.EdmSimpleTypeKind;
import org.apache.olingo.odata2.api.edm.FullQualifiedName;
import org.apache.olingo.odata2.api.edm.provider.EdmProvider;
import org.apache.olingo.odata2.api.edm.provider.EntityContainer;
import org.apache.olingo.odata2.api.edm.provider.EntityContainerInfo;
import org.apache.olingo.odata2.api.edm.provider.EntitySet;
import org.apache.olingo.odata2.api.edm.provider.EntityType;
import org.apache.olingo.odata2.api.edm.provider.Key;
import org.apache.olingo.odata2.api.edm.provider.Property;
import org.apache.olingo.odata2.api.edm.provider.PropertyRef;
import org.apache.olingo.odata2.api.edm.provider.Schema;
import org.apache.olingo.odata2.api.edm.provider.SimpleProperty;
import org.apache.olingo.odata2.api.exception.ODataException;

import java.util.ArrayList;
import java.util.List;

public class MyEdmProvider extends EdmProvider {

    private static final String NAMESPACE = "com.pagr.odata2.OCellUpdate";
    private static final String ENTITY_NAME_CELLUPDATE = "CellUpdate";
    private static final FullQualifiedName ENTITY_TYPE_1_1 = new FullQualifiedName(NAMESPACE, ENTITY_NAME_CELLUPDATE);
    private static final String ENTITY_CONTAINER = "ODataCellUpdatesEntityContainer";
    static final String ENTITY_SET_NAME_CELLUPDATES = "CellUpdates";


    public List<Schema> getSchemas() throws ODataException {
        List<Schema> schemas = new ArrayList<>();
        Schema schema = new Schema();
        schema.setNamespace(NAMESPACE);
        List<EntityType> entityTypes = new ArrayList<>();
        entityTypes.add(getEntityType(ENTITY_TYPE_1_1));
        schema.setEntityTypes(entityTypes);
        List<EntityContainer> entityContainers = new ArrayList<>();
        EntityContainer entityContainer = new EntityContainer();
        entityContainer.setName(ENTITY_CONTAINER).setDefaultEntityContainer(true);
        List<EntitySet> entitySets = new ArrayList<>();
        entitySets.add(getEntitySet(ENTITY_CONTAINER, ENTITY_SET_NAME_CELLUPDATES));
        entityContainer.setEntitySets(entitySets);
        entityContainers.add(entityContainer);
        schema.setEntityContainers(entityContainers);
        schemas.add(schema);
        return schemas;
    }

    @Override
    public EntityType getEntityType(FullQualifiedName edmFQName) throws ODataException {
        if (ENTITY_TYPE_1_1.getName().equals(edmFQName.getName())) {

            //Properties
            List<Property> properties = new ArrayList<>();
            properties.add(new SimpleProperty().setName("Id").setType(EdmSimpleTypeKind.Int64));

            //Key
            List<PropertyRef> keyProperties = new ArrayList<>();
            keyProperties.add(new PropertyRef().setName("Id"));
            Key key = new Key().setKeys(keyProperties);

            return new EntityType().setName(ENTITY_TYPE_1_1.getName())
                    .setProperties(properties)
                    .setKey(key);

        }
        return null;
    }

    @Override
    public EntityContainerInfo getEntityContainerInfo(String name) throws ODataException {
        if (name == null || ENTITY_CONTAINER.equals(name)) {
            return new EntityContainerInfo().setName(ENTITY_CONTAINER).setDefaultEntityContainer(true);
        }

        return super.getEntityContainerInfo(name);
    }

    @Override
    public EntitySet getEntitySet(String entityContainer, String name) throws ODataException {
        if (ENTITY_CONTAINER.equals(entityContainer)) {
            if (ENTITY_SET_NAME_CELLUPDATES.equals(name)) {
                return new EntitySet().setName(name).setEntityType(ENTITY_TYPE_1_1);
            }
        }
        return super.getEntitySet(entityContainer, name);
    }

}
