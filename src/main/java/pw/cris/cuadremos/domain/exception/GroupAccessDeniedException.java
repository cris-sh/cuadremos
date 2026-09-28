package pw.cris.cuadremos.domain.exception;

/** The caller is authenticated, but their place in the group does not allow this action. */
public class GroupAccessDeniedException extends RuntimeException {
    public GroupAccessDeniedException(String message) {
        super(message);
    }
}
