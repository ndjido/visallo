package org.visallo.core.model.user;

import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.Privilege;

import java.util.Set;

public abstract class PrivilegeRepositoryBase implements PrivilegeRepository {
    private UserRepository userRepository;

    public boolean hasPrivilege(User user, String privilege) {
        Set<String> privileges = getPrivileges(user);
        return privileges.contains(privilege);
    }

    public boolean hasAllPrivileges(User user, Set<String> requiredPrivileges) {
        return Privilege.hasAll(getPrivileges(user), requiredPrivileges);
    }

    // Need to late bind since UserRepository injects AuthorizationRepository in constructor
    protected UserRepository getUserRepository() {
        if (userRepository == null) {
            userRepository = InjectHelper.getInstance(UserRepository.class);
        }
        return userRepository;
    }
}
