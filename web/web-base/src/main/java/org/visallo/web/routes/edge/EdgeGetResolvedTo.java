package org.visallo.web.routes.edge;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Edge;
import org.vertexium.Graph;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.web.clientapi.model.ClientApiTermMentionsResponse;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;
import org.visallo.web.routes.GetResolvedToBase;

public class EdgeGetResolvedTo extends GetResolvedToBase implements ParameterizedHandler {
    private final Graph graph;

    @Inject
    public EdgeGetResolvedTo(
            Graph graph,
            TermMentionRepository termMentionRepository
    ) {
        super(termMentionRepository);
        this.graph = graph;
    }

    @Handle
    public ClientApiTermMentionsResponse handle(
            @Required(name = "graphEdgeId") String graphEdgeId,
            @Optional(name = "propertyKey") String propertyKey,
            @Optional(name = "propertyName") String propertyName,
            @ActiveWorkspaceId String workspaceId,
            Authorizations authorizations
    ) throws Exception {
        Edge edge = graph.getEdge(graphEdgeId, authorizations);
        if (edge == null) {
            throw new VisalloResourceNotFoundException(String.format("edge %s not found", graphEdgeId));
        }

        return getClientApiTermMentionsResponse(
                edge,
                propertyKey,
                propertyName,
                workspaceId,
                authorizations
        );
    }
}
