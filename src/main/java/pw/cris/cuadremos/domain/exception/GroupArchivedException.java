package pw.cris.cuadremos.domain.exception;

/** The group is archived: it can still be read, but nothing in it can change. */
public class GroupArchivedException extends RuntimeException {
    public GroupArchivedException(String message) {
        super(message);
    }
}
