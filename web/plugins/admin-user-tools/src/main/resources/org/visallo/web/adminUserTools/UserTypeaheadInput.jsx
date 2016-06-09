define([
    'react',
    'public/v1/api',
    'jsx!./WorkspaceList',
    'jsx!util/react-alert'
], function (React,
             visallo,
             WorkspaceList,
             ReactAlert) {

    const UserTypeaheadInput = React.createClass({
        dataRequest: null,

        componentWillMount() {
            visallo.connect()
                .then(({dataRequest})=> {
                    this.dataRequest = dataRequest;
                });
        },

        componentDidMount() {
            this.setupTypeahead(this.refs.username);
        },

        setupTypeahead(usernameInput) {
            var groupedByDisplayName;

            $(usernameInput).typeahead({
                source: (query, callback) => {
                    this.dataRequest('user', 'search', query)
                        .done((users) => {
                            groupedByDisplayName = _.indexBy(users, 'displayName');
                            callback(_.keys(groupedByDisplayName));
                        });
                },
                updater: (displayName) => {
                    this.props.onChange(displayName);
                    this.props.onSelected(displayName);
                    return displayName;
                }
            });
        },

        handleInputChange(e) {
            this.props.onChange(e.target.value);
        },

        render() {
            return (
                <input type="text" ref="username" value={this.props.username} onChange={this.handleInputChange}
                       className="user"/>
            )
        }
    });

    return UserTypeaheadInput;
});
