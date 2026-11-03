package cloudgene.mapred.server.filter;

import io.micronaut.core.order.Ordered;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.annotation.ResponseFilter;
import io.micronaut.http.annotation.ServerFilter;
import io.micronaut.http.filter.ServerFilterPhase;

@ServerFilter(Filter.MATCH_ALL_PATTERN)
public class GlobalFilters implements Ordered {

	@ResponseFilter
	public void addSecurityHeaders(MutableHttpResponse<?> response) {
		response.getHeaders().set("Cache-Control", "no-store");
		response.getHeaders().set("Content-Security-Policy", "upgrade-insecure-requests; frame-ancestors 'none'");
		response.getHeaders().set("X-Content-Type-Options", "nosniff");
		response.getHeaders().set("X-Frame-Options", "DENY");
	}

	@Override
	public int getOrder() {
		return ServerFilterPhase.SECURITY.order();
	}
}
