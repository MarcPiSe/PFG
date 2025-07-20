package edu.udg.tfg.Trash.feignClients.requests;

public class SetDeletedRequest {
    private Boolean deleted;

    public SetDeletedRequest(Boolean deleted) {
        this.deleted = deleted;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
