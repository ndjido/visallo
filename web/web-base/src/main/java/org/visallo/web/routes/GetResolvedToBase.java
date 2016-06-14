package org.visallo.web.routes;

import com.v5analytics.webster.annotations.Optional;
import org.vertexium.Authorizations;
import org.vertexium.Element;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.web.clientapi.model.ClientApiTermMentionsResponse;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class GetResolvedToBase {
    private final TermMentionRepository termMentionRepository;

    protected GetResolvedToBase(TermMentionRepository termMentionRepository) {
        this.termMentionRepository = termMentionRepository;
    }

    protected ClientApiTermMentionsResponse getClientApiTermMentionsResponse(
            Element element,
            @Optional(name = "propertyKey") String propertyKey,
            @Optional(name = "propertyName") String propertyName,
            @ActiveWorkspaceId String workspaceId,
            Authorizations authorizations
    ) {
        Stream<Vertex> termMentions;
        if (propertyKey != null || propertyName != null) {
            Property property = element.getProperty(propertyKey, propertyName);
            if (property == null) {
                throw new VisalloResourceNotFoundException(String.format(
                        "property %s:%s not found on element %s",
                        propertyKey,
                        propertyName,
                        element.getId()
                ));
            }
            termMentions = termMentionRepository.findResolvedToForRef(
                    element,
                    propertyKey,
                    propertyName,
                    authorizations
            );
        } else {
            termMentions = termMentionRepository.findResolvedToForRefElement(element, authorizations);
        }

        return termMentionRepository.toClientApi(
                termMentions.collect(Collectors.toList()),
                workspaceId,
                authorizations
        );
    }
}
