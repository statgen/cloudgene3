package cloudgene.mapred.core;

import java.util.Date;

public record ApiToken(String accessToken, String hash, Date expiresOn) {}
