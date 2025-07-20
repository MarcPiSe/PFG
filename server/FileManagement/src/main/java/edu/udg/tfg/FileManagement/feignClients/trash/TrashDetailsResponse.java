package edu.udg.tfg.FileManagement.feignClients.trash;

import java.util.Date;

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
