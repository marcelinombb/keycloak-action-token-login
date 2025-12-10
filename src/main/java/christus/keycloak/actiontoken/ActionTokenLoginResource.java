package christus.keycloak.actiontoken;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.keycloak.Config;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.Urls;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.UserPermissionEvaluator;

public class ActionTokenLoginResource {

	private final KeycloakSession session;
	private final RealmModel realm;
	private final AdminPermissionEvaluator auth;

    public ActionTokenLoginResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth) {
        this.session = session;
        this.realm = realm;
        this.auth = auth;
    }

	@POST
	@Path("")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createTokenLink(ActionTokenLoginRequest payload) {
		// do the authorization with the existing admin permissions, here we need the role 'manage-users'
		final UserPermissionEvaluator userPermissionEvaluator = auth.users();
		userPermissionEvaluator.requireManage();

		String clientId = payload.getClientId();
		ClientModel client = session.clients().getClientByClientId(realm, clientId);
		if (client == null) {
			throw new NotFoundException("clientId %s not found.".formatted(clientId));
		}

		if (client.getRedirectUris().isEmpty() || !client.isPublicClient()) {
			throw new BadRequestException("redirectUri disallowed by client.");
		}

        UserModel user = session.users().getUserById(realm, payload.getUserId());

        if (user == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid user_id").build();
        }

		ActionTokenLogin token = createActionToken(
			user, clientId, client.getRedirectUris().iterator().next());

		String tokenUrl = getActionTokenUrl(token);

		ActionTokenLoginResponse response = new ActionTokenLoginResponse(user.getId(), tokenUrl);

		return Response.ok(response).build();
	}

	private boolean validateRedirectUri(ClientModel client, String redirectUri) {
		String redirect = RedirectUtils.verifyRedirectUri(session, redirectUri, client);
		return redirectUri.equals(redirect);
	}

	private ActionTokenLogin createActionToken(UserModel user, String clientId, String redirectUri) {
		int expirationInSecs = 120;
		int absoluteExpirationInSecs = Time.currentTime() + expirationInSecs;
		return new ActionTokenLogin(user.getId(), clientId, absoluteExpirationInSecs, false, redirectUri, null);
	}

	private String getActionTokenUrl(ActionTokenLogin token) {
		KeycloakContext context = session.getContext();
		UriInfo uriInfo = context.getUri();

		// creating this kind of token for the admin (master) realm is of high risk, thus we don't allow this
		String adminRealm = Config.getAdminRealm();
		if (adminRealm.equals(realm.getName())) {
			throw new IllegalStateException("This token type is not allowed for realm %s".formatted(adminRealm));
		}

		// If you are using a different realm to call this method than the one you want to create the action token,
		// we need to temporarily set the session context realm to the latter one, because the SignatureProvider
		// uses the keys from the current sessionContextRealm.
		RealmModel sessionContextRealm = session.getContext().getRealm();
		session.getContext().setRealm(realm);

		// now do the work
		String tokenString = token.serialize(session, realm, uriInfo);
		UriBuilder uriBuilder = Urls.actionTokenBuilder(uriInfo.getBaseUri(), tokenString, token.issuedFor, "", "");

		// and then reset the realm to the proper one
		session.getContext().setRealm(sessionContextRealm);

		return uriBuilder.build(realm.getName()).toString();
	}

}
