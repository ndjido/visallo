package org.visallo.core.security;

import com.google.inject.Inject;
import org.vertexium.*;
import org.vertexium.util.IterableUtils;
import org.visallo.core.exception.VisalloAccessDeniedException;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.ontology.*;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.*;

import java.util.*;
import java.util.stream.Collectors;

public abstract class ACLProvider {

    private Graph graph;
    private UserRepository userRepository;

    @Inject
    public final void setGraph(Graph graph) {
        this.graph = graph;
    }

    @Inject
    public final void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public abstract boolean canDeleteElement(Element element, User user);

    public abstract boolean canDeleteProperty(Element element, String propertyKey, String propertyName, User user);

    public abstract boolean canUpdateElement(Element element, User user);

    public abstract boolean canUpdateProperty(Element element, String propertyKey, String propertyName, User user);

    public abstract boolean canAddProperty(Element element, String propertyKey, String propertyName, User user);

    public final void checkCanAddOrUpdateProperty(Element element, String propertyKey, String propertyName, User user)
            throws VisalloAccessDeniedException {
        boolean isUpdate = element.getProperty(propertyKey, propertyName) != null;
        boolean canAddOrUpdate = isUpdate
                ? internalCanUpdateProperty(element, propertyKey, propertyName, user)
                : internalCanAddProperty(element, propertyKey, propertyName, user);

        if (!canAddOrUpdate) {
            throw new VisalloAccessDeniedException(
                    propertyName + " cannot be added or updated due to ACL restriction", user, element.getId());
        }
    }

    public final void checkCanDeleteProperty(Element element, String propertyKey, String propertyName, User user)
            throws VisalloAccessDeniedException {
        boolean canDelete = internalCanDeleteProperty(element, propertyKey, propertyName, user);

        if (!canDelete) {
            throw new VisalloAccessDeniedException(
                    propertyName + " cannot be deleted due to ACL restriction", user, element.getId());
        }
    }

    public final ClientApiElementAcl elementACL(Element element, User user, OntologyRepository ontologyRepository) {
        ClientApiElementAcl elementAcl = new ClientApiElementAcl();
        elementAcl.setAddable(true);
        elementAcl.setUpdateable(internalCanUpdateElement(element, user));
        elementAcl.setDeleteable(internalCanDeleteElement(element, user));

        List<ClientApiPropertyAcl> propertyAcls = elementAcl.getPropertyAcls();
        if (element instanceof Vertex) {
            String iri = VisalloProperties.CONCEPT_TYPE.getPropertyValue(element);
            while (iri != null) {
                Concept concept = ontologyRepository.getConceptByIRI(iri);
                populatePropertyAcls(concept, element, user, propertyAcls);
                iri = concept.getParentConceptIRI();
            }
        } else if (element instanceof Edge) {
            String iri = ((Edge) element).getLabel();
            while (iri != null) {
                Relationship relationship = ontologyRepository.getRelationshipByIRI(iri);
                populatePropertyAcls(relationship, element, user, propertyAcls);
                iri = relationship.getParentIRI();
            }
        } else {
            throw new VisalloException("unsupported Element class " + element.getClass().getName());
        }
        return elementAcl;
    }

    public final ClientApiObject appendACL(ClientApiObject clientApiObject, User user) {
        if (clientApiObject instanceof ClientApiElement) {
            ClientApiElement apiElement = (ClientApiElement) clientApiObject;
            appendACL(apiElement, user);
        } else if (clientApiObject instanceof ClientApiWorkspaceVertices) {
            appendACL(((ClientApiWorkspaceVertices) clientApiObject).getVertices(), user);
        } else if (clientApiObject instanceof ClientApiVertexMultipleResponse) {
            appendACL(((ClientApiVertexMultipleResponse) clientApiObject).getVertices(), user);
        } else if (clientApiObject instanceof ClientApiEdgeMultipleResponse) {
            appendACL(((ClientApiEdgeMultipleResponse) clientApiObject).getEdges(), user);
        } else if (clientApiObject instanceof ClientApiElementSearchResponse) {
            appendACL(((ClientApiElementSearchResponse) clientApiObject).getElements(), user);
        } else if (clientApiObject instanceof ClientApiEdgeSearchResponse) {
            appendACL(((ClientApiEdgeSearchResponse) clientApiObject).getResults(), user);
        } else if (clientApiObject instanceof ClientApiVertexEdges) {
            ClientApiVertexEdges vertexEdges = (ClientApiVertexEdges) clientApiObject;
            appendACL(vertexEdges, user);
        }

        return clientApiObject;
    }

    public final void appendACL(Collection<? extends ClientApiObject> clientApiObject, User user) {
        for (ClientApiObject apiObject : clientApiObject) {
            appendACL(apiObject, user);
        }
    }

    public Set<String> getAllPrivileges() {
        return Privilege.getAllBuiltIn();
    }

    protected final boolean isComment(String propertyName) {
        return VisalloProperties.COMMENT.isSameName(propertyName);
    }

    protected final boolean isAuthor(Element element, String propertyKey, String propertyName, User user) {
        Property property = element.getProperty(propertyKey, propertyName);
        String authorUserId = VisalloProperties.MODIFIED_BY_METADATA.getMetadataValue(property.getMetadata());
        return user.getUserId().equals(authorUserId);
    }

    protected final boolean hasPrivilege(User user, String privilege) {
        return user.getPrivileges().contains(privilege);
    }

    private void appendACL(ClientApiElement apiElement, User user) {
        for (ClientApiProperty property : apiElement.getProperties()) {
            property.setUpdateable(internalCanUpdateProperty(apiElement, property, user));
            property.setDeleteable(internalCanDeleteProperty(apiElement, property, user));
            property.setAddable(internalCanAddProperty(apiElement, property, user));
        }
        apiElement.setUpdateable(internalCanUpdateElement(apiElement, user));
        apiElement.setDeleteable(internalCanDeleteElement(apiElement, user));

        if (apiElement instanceof ClientApiEdgeWithVertexData) {
            appendACL(((ClientApiEdgeWithVertexData) apiElement).getSource(), user);
            appendACL(((ClientApiEdgeWithVertexData) apiElement).getTarget(), user);
        }
    }

    private void appendACL(ClientApiVertexEdges edges, User user) {
        for (ClientApiVertexEdges.Edge vertexEdge : edges.getRelationships()) {
            appendACL(vertexEdge.getRelationship(), user);
            appendACL(vertexEdge.getVertex(), user);
        }
    }

    private void populatePropertyAcls(
            HasOntologyProperties hasOntologyProperties, Element element, User user,
            List<ClientApiPropertyAcl> propertyAcls
    ) {
        Collection<OntologyProperty> ontologyProperties = hasOntologyProperties.getProperties();
        Set<String> addedPropertyNames = new HashSet<>();
        for (OntologyProperty ontologyProperty : ontologyProperties) {
            String name = ontologyProperty.getTitle();
            List<Property> properties = IterableUtils.toList(element.getProperties(name));
            for (Property property : properties) {
                propertyAcls.add(newClientApiPropertyAcl(element, property.getKey(), name, user));
                addedPropertyNames.add(name);
            }
        }

        // for properties that don't exist on the element, use the ontology property definition and omit the key.
        propertyAcls.addAll(
                ontologyProperties.stream()
                        .filter(ontologyProperty -> !addedPropertyNames.contains(ontologyProperty.getTitle()))
                        .map(ontologyProperty -> newClientApiPropertyAcl(
                                element,
                                null,
                                ontologyProperty.getTitle(),
                                user
                        ))
                        .collect(Collectors.toList())
        );
    }

    private ClientApiPropertyAcl newClientApiPropertyAcl(Element element, String key, String name, User user) {
        ClientApiPropertyAcl propertyAcl = new ClientApiPropertyAcl();
        propertyAcl.setKey(key);
        propertyAcl.setName(name);
        propertyAcl.setAddable(internalCanAddProperty(element, key, name, user));
        propertyAcl.setUpdateable(internalCanUpdateProperty(element, key, name, user));
        propertyAcl.setDeleteable(internalCanDeleteProperty(element, key, name, user));
        return propertyAcl;
    }

    private Element findElement(ClientApiElement apiElement) {
        Authorizations authorizations = userRepository.getAuthorizations(userRepository.getSystemUser());
        if (apiElement instanceof ClientApiVertex) {
            return graph.getVertex(apiElement.getId(), authorizations);
        } else if (apiElement instanceof ClientApiEdge) {
            return graph.getEdge(apiElement.getId(), authorizations);
        } else {
            throw new VisalloException("unexpected ClientApiElement type " + apiElement.getClass().getName());
        }
    }

    private boolean internalCanDeleteElement(Element element, User user) {
        return hasPrivilege(user, Privilege.EDIT) && canDeleteElement(element, user);
    }

    private boolean internalCanDeleteProperty(Element element, String propertyKey, String propertyName, User user) {
        boolean canDelete = hasPrivilege(user, Privilege.EDIT) &&
                canDeleteProperty(element, propertyKey, propertyName, user);
        if (canDelete && isComment(propertyName)) {
            canDelete = hasPrivilege(user, Privilege.COMMENT_DELETE_ANY) ||
                    (hasPrivilege(user, Privilege.COMMENT) && isAuthor(element, propertyKey, propertyName, user));
        }
        return canDelete;
    }

    private boolean internalCanUpdateElement(Element element, User user) {
        return hasPrivilege(user, Privilege.EDIT) && canUpdateElement(element, user);
    }

    private boolean internalCanUpdateProperty(Element element, String propertyKey, String propertyName, User user) {
        boolean canUpdate = hasPrivilege(user, Privilege.EDIT) &&
                canUpdateProperty(element, propertyKey, propertyName, user);
        if (canUpdate && isComment(propertyName)) {
            canUpdate = hasPrivilege(user, Privilege.COMMENT_EDIT_ANY) ||
                    (hasPrivilege(user, Privilege.COMMENT) && isAuthor(element, propertyKey, propertyName, user));
        }
        return canUpdate;
    }

    private boolean internalCanAddProperty(Element element, String propertyKey, String propertyName, User user) {
        boolean canAdd = hasPrivilege(user, Privilege.EDIT) && canAddProperty(element, propertyKey, propertyName, user);
        if (canAdd && isComment(propertyName)) {
            canAdd = hasPrivilege(user, Privilege.COMMENT);
        }
        return canAdd;
    }

    private boolean internalCanDeleteElement(ClientApiElement element, User user) {
        return internalCanDeleteElement(findElement(element), user);
    }

    private boolean internalCanDeleteProperty(ClientApiElement element, ClientApiProperty p, User user) {
        return internalCanDeleteProperty(findElement(element), p.getKey(), p.getName(), user);
    }

    private boolean internalCanUpdateElement(ClientApiElement element, User user) {
        return internalCanUpdateElement(findElement(element), user);
    }

    private boolean internalCanUpdateProperty(ClientApiElement element, ClientApiProperty p, User user) {
        return internalCanUpdateProperty(findElement(element), p.getKey(), p.getName(), user);
    }

    private boolean internalCanAddProperty(ClientApiElement element, ClientApiProperty p, User user) {
        return internalCanAddProperty(findElement(element), p.getKey(), p.getName(), user);
    }
}
