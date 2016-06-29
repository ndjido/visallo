package org.visallo.core.security;

import org.vertexium.Element;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.ClientApiElement;
import org.visallo.web.clientapi.model.ClientApiProperty;

public class AllowNoneAclProvider extends ACLProvider {
    @Override
    public boolean canDeleteElement(Element element, User user) {
        return false;
    }

    @Override
    public boolean canDeleteProperty(Element element, String propertyKey, String propertyName, User user) {
        return false;
    }

    @Override
    public boolean canUpdateElement(Element element, User user) {
        return false;
    }

    @Override
    public boolean canUpdateProperty(Element element, String propertyKey, String propertyName, User user) {
        return false;
    }

    @Override
    public boolean canAddProperty(Element element, String propertyKey, String propertyName, User user) {
        return false;
    }
}
