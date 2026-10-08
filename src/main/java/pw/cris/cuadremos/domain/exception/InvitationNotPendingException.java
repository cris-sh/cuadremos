package pw.cris.cuadremos.domain.exception;

/* The invitation was already accepted, declined or canceled, so it cannot change again. */
public class InvitationNotPendingException extends RuntimeException {
    public InvitationNotPendingException(String message) {
        super(message);
    }
}
