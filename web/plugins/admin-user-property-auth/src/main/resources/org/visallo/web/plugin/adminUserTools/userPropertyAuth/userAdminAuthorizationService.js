define('data/web-worker/services/com-visallo-userAdminAuthorization', ['data/web-worker/util/ajax'], function (ajax) {
    'use strict';

    var api = {
        userAuthAdd: function(userName, auth) {
            return ajax('POST', '/user/auth/add', {
                'user-name': userName,
                auth: auth
            });
        },

        userAuthRemove: function(userName, auth) {
            return ajax('POST', '/user/auth/remove', {
                'user-name': userName,
                auth: auth
            });
        }
    };

    return api;
});
