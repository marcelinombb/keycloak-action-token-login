package christus.keycloak.actiontoken;

public class ActionTokenLoginResponse {
	private String userId;
	private String link;

    public ActionTokenLoginResponse(String userId, String link) {
        this.userId = userId;
        this.link = link;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
