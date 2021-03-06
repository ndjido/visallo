
User Account Plugin
=====================

Plugin to configure the new pages for user account dialog

To register a page:

```js
require(['configuration/plugins/registry'], function(registry) {
    registry.registerExtension('org.visallo.user.account.page', {
        identifier: 'changePassword',
        pageComponentPath: 'org.visallo.useraccount.changePassword'
    });


    define('org.visallo.useraccount.changePassword', [
        'flight/lib/component'
    ], function(defineComponent) {
        return defineComponent(ChangePassword);

        function ChangePassword() {
            this.after('initialize', function() {
                this.$node.html('Change Password');
            })
        }
    })
});
```

Remember to add a i18n value in a MessageBundle.properties. This will be displayed in the left pane

    useraccount.page.[Page Identifier].displayName=[String to display]

For example:

    useraccount.page.changePassword.displayName=Change Password
