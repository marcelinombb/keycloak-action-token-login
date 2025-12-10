package christus.keycloak.actiontoken;

import com.google.auto.service.AutoService;
import jakarta.ws.rs.core.Response;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.actiontoken.AbstractActionTokenHandler;
import org.keycloak.authentication.actiontoken.ActionTokenContext;
import org.keycloak.authentication.actiontoken.ActionTokenHandlerFactory;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.util.ResolveRelative;
import org.keycloak.sessions.AuthenticationSessionModel;

@AutoService(ActionTokenHandlerFactory.class)
public class ActionTokenLoginHandler extends AbstractActionTokenHandler<ActionTokenLogin> {

    public ActionTokenLoginHandler() {
        super(
            ActionTokenLogin.TOKEN_TYPE,
            ActionTokenLogin.class,
            Messages.INVALID_REQUEST,
            EventType.EXECUTE_ACTION_TOKEN,
            Errors.INVALID_REQUEST
        );
    }

    @Override
    public Response handleToken(ActionTokenLogin token, ActionTokenContext<ActionTokenLogin> tokenContext) {
        KeycloakSession session = tokenContext.getSession();
        AuthenticationSessionModel authSession = tokenContext.getAuthenticationSession();
        ClientModel client = authSession.getClient();
        String redirectUri = token.getRedirectUri() != null
            ? token.getRedirectUri()
            : ResolveRelative.resolveRelativeUri(session, client.getRootUrl(), client.getBaseUrl());

        String redirect = RedirectUtils.verifyRedirectUri(session, redirectUri, client);

        if (redirect != null) {
            authSession.setAuthNote(
                AuthenticationManager.SET_REDIRECT_URI_AFTER_REQUIRED_ACTIONS, Boolean.TRUE.toString());
            authSession.setRedirectUri(redirect);
            authSession.setClientNote(OIDCLoginProtocol.REDIRECT_URI_PARAM, redirectUri);

            if (token.getActionVerificationNonce() != null) {
                authSession.setClientNote(OIDCLoginProtocol.NONCE_PARAM, token.getActionVerificationNonce().toString());
            }
        }

        authSession.setClientNote(OAuth2Constants.SCOPE, OAuth2Constants.SCOPE_OPENID);
        AuthenticationManager.setClientScopesInSession(session, authSession);

        authSession.getAuthenticatedUser().setEmailVerified(true);

        String nextRequiredAction =
            AuthenticationManager.nextRequiredAction(session, authSession, tokenContext.getRequest(), tokenContext.getEvent());

        return AuthenticationManager.redirectToRequiredActions(
            session, tokenContext.getRealm(), authSession, tokenContext.getUriInfo(), nextRequiredAction);
    }

    @Override
    public AuthenticationSessionModel startFreshAuthenticationSession(
            ActionTokenLogin token, ActionTokenContext<ActionTokenLogin> tokenContext) {
        return tokenContext.createAuthenticationSessionForClient(token.getIssuedFor());
    }

    @Override
    public boolean canUseTokenRepeatedly(ActionTokenLogin token, ActionTokenContext<ActionTokenLogin> tokenContext) {
        return token.isReuse();
    }

}
