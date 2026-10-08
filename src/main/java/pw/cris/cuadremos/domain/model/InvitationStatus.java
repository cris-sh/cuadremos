package pw.cris.cuadremos.domain.model;

/* PENDING is the only open state; the other three are final */
public enum InvitationStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    CANCELLED
}
