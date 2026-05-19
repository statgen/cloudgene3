package cloudgene.mapred.core;

import java.time.Instant;

public record ApiToken(String accessToken, String hash, Instant expiresOn) {}
