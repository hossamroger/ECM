package ae.sahabss.ds.ecmservice.ucm;

import java.util.Date;

import oracle.stellent.ridc.model.DataObject;

public class UCMDocument {
    public UCMDocument() {
        super();
    }
    private String author;
    private String docName;
    private Date checkInDate;
    private String title;
    private String dId;
    private String revesionId;
    private String revesionLable;
    private String format;
    private String filename;
    private boolean checkedOut;
    private String docUrl;
    private String contentType;
    


    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAuthor() {
        return author;
    }

    public void setDocName(String docName) {
        this.docName = docName;
    }

    public String getDocName() {
        return docName;
    }

    public void setCheckInDate(Date docInDate) {
        this.checkInDate = docInDate;
    }

    public Date getCheckInDate() {
        return checkInDate;
    }

    public void setTitle(String docTitle) {
        this.title = docTitle;
    }

    public String getTitle() {
        return title;
    }

    public void setDId(String dId) {
        this.dId = dId;
    }

    public String getDId() {
        return dId;
    }

    public void setRevesionId(String dRevesionId) {
        this.revesionId = dRevesionId;
    }

    public String getRevesionId() {
        return revesionId;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getFormat() {
        return format;
    }

    public void setCheckedOut(boolean checkedOut) {
        this.checkedOut = checkedOut;
    }

    public boolean getCheckedOut() {
        return checkedOut;
    }
    
    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }
    
    public void setDocUrl(String docUrl) {
        this.docUrl = docUrl;
    }

    public String getDocUrl() {
        return docUrl;
    }
    
    @Override
    public String toString() {
        return 
        
        "Document Name: " + getDocName() + "\n" + 
        "Document ID: " + getDId() + "\n" +
        "Filename: " + getFilename() + "\n" +
        "Document Author: " + getAuthor() + "\n" + 
        "Document Title: " + getTitle() + "\n" + 
        "Document Revision: " + getRevesionId() + "\n" +
        "Checked In Date: " + getCheckInDate() + "\n" +
        "Document Format: " + getFormat() + "\n" +
        "Is Checked Out: " + getCheckedOut() + "\n";
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String docContentType) {
        this.contentType = docContentType;
    }

    public void populateDocument(DataObject dataObj) {
        
//        setId(dataObj.get("dID"));
//        setDocName(dataObj.get("dDocName"));
//        setTitle(dataObj.get("dDocTitle"));
//        setRevLabel(dataObj.get("dRevLabel"));
//        setSecurityGroup(dataObj.get("dSecurityGroup"));
//        setDocAccount(dataObj.get("dDocAccount"));
        
        setDId(dataObj.get("dID"));
        setAuthor(dataObj.get("dDocAuthor"));
        setDocName(dataObj.get("dDocName"));
        setTitle(dataObj.get("dDocTitle"));
        setRevesionId(dataObj.get("dRevisionID"));
        setFormat(dataObj.get("dFormat"));
        setFilename(dataObj.get("dOriginalName"));
    }


    public void setRevesionLable(String revesionLable) {
        this.revesionLable = revesionLable;
    }

    public String getRevesionLable() {
        return revesionLable;
    }
}
