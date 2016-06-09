define([
    'react',
    'public/v1/api'
], function (React, visallo) {

    const WorkspaceList = React.createClass({
        dataRequest: null,

        componentWillMount() {
            visallo.connect()
                .then(({dataRequest})=> {
                    this.dataRequest = dataRequest;
                });
        },

        handleShareWithMeClick(workspace) {
            const workspaceId = workspace.workspaceId;
            this.dataRequest('admin', 'workspaceShare', workspaceId, this.props.user.userName)
                .then(this.props.onWorkspaceChanged)
                .catch(this.props.onError);
        },

        renderWorkspace(workspace) {
            return (
                <li key={workspace.workspaceId} className="highlight-on-hover">
                    <button className="share show-on-hover btn btn-mini btn-primary"
                            onClick={()=>this.handleShareWithMeClick(workspace)}>
                        {i18n('admin.user.editor.shareWorkspace')}
                    </button>
                    <span className="nav-list-title">{workspace.title} {workspace.isCurrent ? '(current)' : ''}</span>
                    <span className="nav-list-subtitle">{workspace.workspaceId}</span>
                    <ul className="inner-list">
                        {
                            workspace.users.map((workspaceUser) => {
                                return (
                                    <li key={workspace.workspaceId + '-'+workspaceUser.userId}
                                        className="nav-list-subtitle">{workspaceUser.access}: {workspaceUser.userId}</li>
                                );
                            })
                        }
                    </ul>
                </li>
            );
        },

        render() {
            return (
                <div>
                    <div className="nav-header">Workspaces</div>
                    <ul>
                        {
                            this.props.user.workspaces.map((workspace) => {
                                return this.renderWorkspace(workspace);
                            })
                        }
                    </ul>
                </div>
            );
        }
    });

    return WorkspaceList;
});
