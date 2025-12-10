package christus.keycloak.actiontoken;

import org.keycloak.authentication.actiontoken.DefaultActionToken;

import java.util.UUID;

public class ActionTokenLogin extends DefaultActionToken {

	public static final String TOKEN_TYPE = "action-token-login";

	private String redirectUri;
	private boolean reuse;

    // required for JSON de-/serialization
    private ActionTokenLogin() {}

	public ActionTokenLogin(String userId, String clientId, int expirationInSeconds, boolean reuse,
                            String redirectUri, String nonce) {
		super(userId, TOKEN_TYPE, expirationInSeconds, uuidOf(nonce));
		this.issuedFor = clientId;
		this.redirectUri = redirectUri;
		this.reuse = reuse;
	}

    public String getRedirectUri() {
        return redirectUri;
    }

    public boolean isReuse() {
        return reuse;
    }

    static UUID uuidOf(String s) {
		try {
			return UUID.fromString(s);
		} catch (Exception ignored) {
		}
		return null;
	}
}
