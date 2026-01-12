package cloudgene.mapred.server.filter;

import io.micronaut.core.order.Ordered;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.ResponseFilter;
import io.micronaut.http.annotation.ServerFilter;
import io.micronaut.http.filter.ServerFilterPhase;

@ServerFilter("/api/v2/**")
public class ApiFilters implements Ordered {

    @ResponseFilter
    public void addSecurityHeaders(MutableHttpResponse<?> response) {
        response.getHeaders().set("Cache-Control", "no-store");
    }

    @Override
    public int getOrder() {
        return ServerFilterPhase.SECURITY.order();
    }
}
