package christus.keycloak.actiontoken;

public class ErrorResponse {
    private String error;
    private String error_description;

    public ErrorResponse() {}

    public ErrorResponse(String error, String error_description) {
        this.error_description = error_description;
        this.error = error;
    }

    public String getError() {
        return error;
    }

    public String getError_description() {
        return error_description;
    }
}
