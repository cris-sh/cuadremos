package pw.cris.cuadremos.domain.exception;

import java.util.UUID;

public class NotGroupMemberException extends RuntimeException {
    public NotGroupMemberException(UUID userId, UUID groupId) {
        super("User " + userId + " is not a member of group " + groupId);
    }
}
