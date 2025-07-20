package edu.udg.tfg.Trash.controllers.responses;

import java.util.Date;
import java.util.UUID;

public class TrashDetailsResponse {
    private Date deletedDate;
    private Date expirationDate;

    public Date getDeletedDate() {
        return deletedDate;
    }

    public void setDeletedDate(Date deletedDate) {
        this.deletedDate = deletedDate;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }
}
