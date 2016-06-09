define('data/web-worker/services/com-visallo-userAdminPrivileges', ['data/web-worker/util/ajax'], function (ajax) {
    'use strict';

    var api = {
        userUpdatePrivileges: function (userName, privileges) {
            return ajax('POST', '/user/privileges/update', {
                'user-name': userName,
                privileges: _.isArray(privileges) ? privileges.join(',') : privileges
            });
        }
    };

    return api;
});
