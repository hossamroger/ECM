package ae.sahabss.ds.ecmservice.ucm;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import ae.sahabss.ds.ecmservice.exceptions.CheckInException;
import ae.sahabss.ds.ecmservice.exceptions.CheckOutException;
import ae.sahabss.ds.ecmservice.exceptions.UCMLoginException;
import oracle.stellent.ridc.IdcClient;
import oracle.stellent.ridc.IdcClientException;
import oracle.stellent.ridc.IdcClientManager;
import oracle.stellent.ridc.IdcContext;
import oracle.stellent.ridc.model.DataBinder;
import oracle.stellent.ridc.model.DataObject;
import oracle.stellent.ridc.model.DataResultSet;
import oracle.stellent.ridc.model.TransferFile;
import oracle.stellent.ridc.protocol.ServiceException;
import oracle.stellent.ridc.protocol.ServiceResponse;

import org.springframework.stereotype.Service;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


@Service
public class UCMUtilities implements IContent {
    private static String url;
    private static String username;
    private static String password;
    private IdcClient idcClient;
    private ServiceResponse response;
    private DataBinder serverBinder;
    private IdcContext userContext;
    
    static {
        try {
            initializeUCM();
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
    }

    public UCMUtilities() throws IdcClientException {
        this(url,username, password);        
    }


    public UCMUtilities(String url,String username, String password) throws IdcClientException {
        UCMUtilities.url = url;
        UCMUtilities.username = username;
        UCMUtilities.password = password;
        IdcClientManager manager = new IdcClientManager();
        //String url = protocol + "://" + serverHostname + ":" + serverPort;
        idcClient = manager.createClient(url);
    }

    public void login(String username, String password) throws UCMLoginException {
        DataBinder dataBinder = null;
        try {
            userContext = new IdcContext(username, password);
            dataBinder = idcClient.createBinder();
            dataBinder.putLocal("IdcService", "PING_SERVER");
            response = idcClient.sendRequest(userContext, dataBinder);
            serverBinder = response.getResponseAsBinder();
        } catch (IdcClientException e) {
            e.printStackTrace();
            throw new UCMLoginException();
        }
    }

    public void login(String username) {
        //        System.out.println("*********************************************************************");
        //        System.out.println("In UCM Login");
        DataBinder dataBinder = null;
        try {
            userContext = new IdcContext(username);
            dataBinder = idcClient.createBinder();
            dataBinder.putLocal("IdcService", "PING_SERVER");
            response = idcClient.sendRequest(userContext, dataBinder);
            serverBinder = response.getResponseAsBinder();
        } catch (IdcClientException e) {
            e.printStackTrace();
        }
    }
    
    public void login() {
        DataBinder dataBinder = null;
        try {
            userContext = new IdcContext(username, password);
            dataBinder = idcClient.createBinder();
            dataBinder.putLocal("IdcService", "PING_SERVER");
            response = idcClient.sendRequest(userContext, dataBinder);
            serverBinder = response.getResponseAsBinder();
        } catch (IdcClientException e) {
            e.printStackTrace();
        }
    }

    public String upload(String contentId, String contentType, String filename, InputStream inputStream,
                         Map<String, Object> customAttributes) throws CheckInException, IOException, Exception {
        return checkInDocument(resolveDocName(contentType,contentId), contentType + " for " + contentId, contentType, "DSharjahGroup", inputStream,
                               filename, customAttributes);
    }

    public UCMDocument getDocumentInfo(String contentId) {
        UCMDocument document = null;
        DataBinder dataBinder = null;
        try {
            String docName = contentId;
            dataBinder = idcClient.createBinder();
            dataBinder.putLocal("IdcService", "DOC_INFO_BY_NAME");
            dataBinder.putLocal("dDocName", docName);
            response = idcClient.sendRequest(userContext, dataBinder);
            DataObject documentInfo = response.getResponseAsBinder()
                                              .getResultSet("DOC_INFO")
                                              .getRows()
                                              .get(0);
            if (documentInfo == null)
                return null;
            Date checkedInDate = null;
            String format = response.getResponseAsBinder()
                                    .getLocal("blDateFormat")
                                    .split("!")[0];
            checkedInDate = resolveDate(format, documentInfo.get("dInDate").toString());
            // All document information keys
            //            for (String s: documentInfo.keySet()) {
            //                System.out.println(s);
            //            }
            document = new UCMDocument();
            document.setDId(documentInfo.get("dID"));
            document.setAuthor(documentInfo.get("dDocAuthor"));
            document.setDocName(documentInfo.get("dDocName"));
            document.setCheckInDate(checkedInDate);
            document.setTitle(documentInfo.get("dDocTitle"));
            document.setRevesionId(documentInfo.get("dRevisionID"));
            document.setFormat(documentInfo.get("dFormat"));
            document.setFilename(documentInfo.get("dOriginalName"));
            document.setDocUrl(response.getResponseAsBinder().getLocal("DocUrl"));
            document.setContentType(documentInfo.get("dDocType"));
        } catch (IdcClientException e) {
            if (e instanceof ServiceException) {
                return null;
            }
        }
        return document;
    }
    public String uploadReturnDID(String contentId, String contentType, String filename, InputStream inputStream,
                                  Map<String, Object> customAttributes) throws CheckInException, IOException,
            Exception {
        return checkInDocumentReturnDID(contentId, contentType + " for " + contentId, contentType, "public",
                inputStream, filename, customAttributes);
    }

    public Map<String, String> uploadReturnDocInfo(String contentId, String contentType, String filename,
                                                   InputStream inputStream,
                                                   Map<String, Object> customAttributes) throws CheckInException,
            IOException, Exception {
        return checkInDocumentReturnDocInfo(contentId, contentType + " for " + contentId, contentType, "public",
                inputStream, filename, customAttributes);
    }

    public void deleteContent(String contentId) {
        DataBinder dataBinder = null;
        dataBinder = idcClient.createBinder();
        dataBinder.putLocal("IdcService", "DELETE_DOC");
        // Document ID
        // dataBinder.putLocal("dID", myId);
        // Document Name
        dataBinder.putLocal("dDocName", contentId);
        try {
            response = idcClient.sendRequest(userContext, dataBinder);
        } catch (IdcClientException e) {
            e.printStackTrace();
        }
        System.out.println("File deleted successfully");
    }

    public void deleteContentByDID(String dID) {
        DataBinder dataBinder = null;
        dataBinder = idcClient.createBinder();
        dataBinder.putLocal("IdcService", "DELETE_DOC");
        // Document ID
        dataBinder.putLocal("dID", dID);
        try {
            response = idcClient.sendRequest(userContext, dataBinder);
        } catch (IdcClientException e) {
            e.printStackTrace();
        }
        System.out.println("File deleted successfully");
    }

    //EDIT_RENDITIONS

    public List<Map<String, String>> getDocumentAttachments(String dId) throws IdcClientException {
        DataBinder dataBinder = null;
        dataBinder = idcClient.createBinder();
        dataBinder.putLocal("IdcService", "EDIT_RENDITIONS_FORM");
        dataBinder.putLocal("dID", dId);
        //dataBinder.putLocal("IsAttachment", "1");
        try {
            response = idcClient.sendRequest(userContext, dataBinder);
            DataResultSet dataResultSet = response.getResponseAsBinder().getResultSet("manifest");
            ArrayList<Map<String, String>> attachments = new ArrayList<Map<String, String>>();
            for (DataObject dataObject : dataResultSet.getRows()) {
                Map<String, String> attachment = new HashMap<String, String>();
                attachment.put("name", dataObject.get("extRenditionName"));
                attachment.put("description", dataObject.get("extRenditionDescription"));
                attachments.add(attachment);
            }
            return attachments;
        } catch (IdcClientException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void addAttachment() {
        DataBinder dataBinder = null;
        dataBinder = idcClient.createBinder();
        dataBinder.getLocalData();
        dataBinder.putLocal("IdcService", "EDIT_RENDITIONS_FORM");
        dataBinder.putLocal("extRenditionName", "3407");
        dataBinder.putLocal("extRenditionDescription", "3407");
        try {
            response = idcClient.sendRequest(userContext, dataBinder);
            response.getHeaderNames();
        } catch (IdcClientException e) {
            e.printStackTrace();
        }
    }

    public UCMDocument getDocumentInfoById(String dId) {
        UCMDocument document = null;
        DataBinder dataBinder = null;
        try {
            dataBinder = idcClient.createBinder();
            dataBinder.putLocal("IdcService", "DOC_INFO");
            dataBinder.putLocal("dID", dId);
            response = idcClient.sendRequest(userContext, dataBinder);
            DataObject documentInfo = response.getResponseAsBinder()
                                              .getResultSet("DOC_INFO")
                                              .getRows()
                                              .get(0);
            if (documentInfo == null)
                return null;
            Date checkedInDate = null;
            String format = response.getResponseAsBinder()
                                    .getLocal("blDateFormat")
                                    .split("!")[0];
            checkedInDate = resolveDate(format, documentInfo.get("dInDate").toString());
            // All document information keys
            //            for (String s: documentInfo.keySet()) {
            //                System.out.println(s);
            //            }
            
            document = new UCMDocument();
            document.populateDocument(documentInfo);
            document.setCheckInDate(checkedInDate);
            document.setDocUrl(response.getResponseAsBinder().getLocal("DocUrl"));
        } catch (IdcClientException e) {
            System.out.println(e);
            if (e instanceof ServiceException) {
                return null;
            }
        }
        return document;
    }

    public InputStream download(String contentId) {
        //String docName = resolveDocName(contentType, contentId);
        System.out.println("*************************************");
        System.out.println("Content Id = " + contentId);
        System.out.println("***************************************");
        return getDocumentFile(contentId);
    }

    public InputStream downloadById(String dId) {
        try {
            DataBinder binder = idcClient.createBinder();
            binder.putLocal("IdcService", "GET_FILE");
            binder.putLocal("dID", dId);
            binder.putLocal("allowInterrupt", "1");
            binder.putLocal("Rendition", "Primary");
            response = idcClient.sendRequest(this.userContext, binder);
            return response.getResponseStream();
        } catch (IdcClientException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<UCMDocument> getRevisions(String contentType, String contentId) {
        String docName = resolveDocName(contentType, contentId);
        List<UCMDocument> documents = new ArrayList<UCMDocument>();
        DataBinder dataBinder = null;
        try {
            dataBinder = idcClient.createBinder();
            dataBinder.putLocal("IdcService", "DOC_INFO_BY_NAME");
            dataBinder.putLocal("dDocName", docName);
            response = idcClient.sendRequest(userContext, dataBinder);
            serverBinder = response.getResponseAsBinder();
            //System.out.println(serverBinder);
            DataResultSet dataResultSet = serverBinder.getResultSet("REVISION_HISTORY");
            String format = response.getResponseAsBinder()
                                    .getLocal("blDateFormat")
                                    .split("!")[0];
            // loop over the results
            UCMDocument document = null;
            for (DataObject dataObject : dataResultSet.getRows()) {
                document = new UCMDocument();
                Date date = resolveDate(format, dataObject.get("dInDate"));
                document.setDId(dataObject.get("dID"));
                document.setRevesionId(dataObject.get("dRevisionID"));
                document.setCheckInDate(date);
                document.setFormat(dataObject.get("dFormat"));
                document.setDocName(dataObject.get("dDocName"));
                documents.add(document);
            }
        } catch (IdcClientException e) {
            e.printStackTrace();
        }
        return documents;
    }
    
    public List<UCMDocument> getRevisions(String dId) {
        List<UCMDocument> documents = new ArrayList<UCMDocument>();
        DataBinder dataBinder = null;
        try {
            dataBinder = idcClient.createBinder();
            dataBinder.putLocal("IdcService", "DOC_INFO");
            dataBinder.putLocal("dID", dId);
            response = idcClient.sendRequest(userContext, dataBinder);
            serverBinder = response.getResponseAsBinder();
            //System.out.println(serverBinder);
            DataResultSet dataResultSet = serverBinder.getResultSet("REVISION_HISTORY");
            String format = response.getResponseAsBinder()
                                    .getLocal("blDateFormat")
                                    .split("!")[0];
            // loop over the results
            UCMDocument document = null;
            for (DataObject dataObject : dataResultSet.getRows()) {
                document = new UCMDocument();
                Date date = resolveDate(format, dataObject.get("dInDate"));
                document.setDId(dataObject.get("dID"));
                document.setRevesionId(dataObject.get("dRevisionID"));
                document.setRevesionLable(dataObject.get("dRevLabel"));
                document.setCheckInDate(date);
                document.setFormat(dataObject.get("dFormat"));
                document.setDocName(dataObject.get("dDocName"));
                documents.add(document);
            }
        } catch (IdcClientException e) {
            e.printStackTrace();
        }
        return documents;
    }

    public String checkInDocumentReturnDID(String documentName, String documentTitle, String documentType,
                                           String securityGroup, InputStream primaryFile, String documentFileName,
                                           Map<String, Object> customMetadata) throws IOException, Exception {
        DataBinder dataBinder = null;
        try {
            dataBinder = idcClient.createBinder();
            dataBinder.putLocal("IdcService", "CHECKIN_UNIVERSAL");
            dataBinder.putLocal("dDocTitle", documentTitle);
            if (documentName != null && documentName.length() > 0) {
                dataBinder.putLocal("dDocName", documentName);
            }
            //to send it to production and update it in data entry process ...farahat
            dataBinder.putLocal("xNEABAGID", "0");
            dataBinder.putLocal("xMAAZONYAID", "0");
            dataBinder.putLocal("xDOCPRIMCOURT", "0");
            ///end require
            dataBinder.putLocal("dDocType", documentType);
            dataBinder.putLocal("dSecurityGroup", securityGroup);
            dataBinder.addFile("primaryFile", new TransferFile(primaryFile, documentFileName, primaryFile.available()));
            dataBinder.putLocal("dDocAccount", "");
            if (customMetadata != null) {
                for (String s : customMetadata.keySet()) {
                    if (!s.equals("dDocTitle") && !s.equals("dSecurityGroup") && !s.equals("dDocType")) {
                        System.out.println(s + " "+ String.valueOf(customMetadata.get(s)));
                        dataBinder.putLocal(s, String.valueOf(customMetadata.get(s)));
                    }
                }
            }
            response = idcClient.sendRequest(this.userContext, dataBinder);
            System.out.println(dataBinder.getLocalData().toString());
            DataBinder responseBinder = response.getResponseAsBinder();
            Collection<String> did = responseBinder.getResultSetNames();
            //            System.out.println(" File checked in and document ID= :" + responseBinder.getLocalData().get("dID"));
            //            System.out.println(" File checked in and document ID= :" + responseBinder.getLocal("dDocName"));
            return responseBinder.getLocalData().get("dID");
        } catch (IdcClientException e) {
            System.out.println("*******************************************************************************************************************");
            System.out.println("IDC");
            System.out.println("*******************************************************************************************************************");
            e.printStackTrace();
            throw e;
        } catch (IOException e) {
            System.out.println("*******************************************************************************************************************");
            System.out.println("IO");
            System.out.println("*******************************************************************************************************************");
            e.printStackTrace();
            throw e;
        }
    }

    private Map<String, String> checkInDocumentReturnDocInfo(String documentName, String documentTitle,
                                                             String documentType, String securityGroup,
                                                             InputStream primaryFile, String documentFileName,
                                                             Map<String, Object> customMetadata) throws IOException,
                                                                                                        Exception {
        DataBinder dataBinder = null;
        try {
            dataBinder = idcClient.createBinder();
            dataBinder.putLocal("IdcService", "CHECKIN_UNIVERSAL");
            dataBinder.putLocal("dDocTitle", documentTitle);
            if (documentName != null && documentName.length() > 0) {
                dataBinder.putLocal("dDocName", documentName);
            }
            dataBinder.putLocal("dDocType", documentType);
            dataBinder.putLocal("dSecurityGroup", securityGroup);
            dataBinder.addFile("primaryFile", new TransferFile(primaryFile, documentFileName, primaryFile.available()));
            dataBinder.putLocal("dDocAccount", "");
            if (customMetadata != null) {
                for (String s : customMetadata.keySet()) {
                    if (!s.equals("dDocTitle") && !s.equals("dSecurityGroup") && !s.equals("dDocType")) {
                        dataBinder.putLocal(s, String.valueOf(customMetadata.get(s)));
                    }
                }
            }
            response = idcClient.sendRequest(this.userContext, dataBinder);
            DataBinder responseBinder = response.getResponseAsBinder();
            Map<String, String> docInfo = new HashMap<String, String>();
            docInfo.put("DID", responseBinder.getLocalData().get("dID"));
            System.out.println("------------------DID------BEFOR URL---------");
            // Get Doc Info
            dataBinder = idcClient.createBinder();
            dataBinder.putLocal("IdcService", "DOC_INFO");
            dataBinder.putLocal("dID", docInfo.get("DID"));
            ServiceResponse response = idcClient.sendRequest(this.userContext, dataBinder);
            DataBinder respBinder = response.getResponseAsBinder();
            String url = respBinder.getLocal("DocUrl");
            String tempFileName = url.substring(url.lastIndexOf("/") + 1);
            String fileName = tempFileName;
            if (tempFileName.contains("~")) {
                fileName = tempFileName.substring(0, tempFileName.lastIndexOf("~"));
                fileName = fileName + tempFileName.substring(tempFileName.lastIndexOf("."));
                url = url.replace(tempFileName, fileName);
            }
            docInfo.put("DocUrl", url);
            return docInfo;
        } catch (IdcClientException e) {
            System.out.println("*******************************************************************************************************************");
            System.out.println("IDC");
            System.out.println("*******************************************************************************************************************");
            e.printStackTrace();
            throw e;
        } catch (IOException e) {
            System.out.println("*******************************************************************************************************************");
            System.out.println("IO");
            System.out.println("*******************************************************************************************************************");
            e.printStackTrace();
            throw e;
        }
    }

    private String checkInDocument(String documentName, String documentTitle, String documentType, String securityGroup,
                                   InputStream primaryFile, String documentFileName,
                                   Map<String, Object> customMetadata) throws IOException, Exception {
        DataBinder dataBinder = null;
        try {
            dataBinder = idcClient.createBinder();
            dataBinder.putLocal("IdcService", "CHECKIN_UNIVERSAL");
            dataBinder.putLocal("dDocTitle", documentTitle);
            if (documentName != null && documentName.length() > 0) {
                dataBinder.putLocal("dDocName", documentName);
            }
            dataBinder.putLocal("dDocType", documentType);
            dataBinder.putLocal("dSecurityGroup", securityGroup);
            dataBinder.addFile("primaryFile", new TransferFile(primaryFile, documentFileName, primaryFile.available()));
            dataBinder.putLocal("dDocAccount", "");
            if (customMetadata != null) {
                for (String s : customMetadata.keySet()) {
                    if (!s.equals("dDocTitle") && !s.equals("dSecurityGroup") && !s.equals("dDocType")) {
                        dataBinder.putLocal(s, String.valueOf(customMetadata.get(s)));
                    }
                }
            }
            response = idcClient.sendRequest(this.userContext, dataBinder);
            DataBinder responseBinder = response.getResponseAsBinder();
            Collection<String> did = responseBinder.getResultSetNames();
            System.out.println(" File checked in and document ID= :" + responseBinder.getLocalData().get("dID"));
            System.out.println(" File checked in and document ID= :" + responseBinder.getLocal("dDocName"));
            return responseBinder.getLocalData().get("dDocName");
        } catch (IdcClientException e) {
            System.out.println("*******************************************************************************************************************");
            System.out.println("IDC");
            System.out.println("*******************************************************************************************************************");
            e.printStackTrace();
            throw e;
        } catch (IOException e) {
            System.out.println("*******************************************************************************************************************");
            System.out.println("IO");
            System.out.println("*******************************************************************************************************************");
            e.printStackTrace();
            throw e;
        }
    }

    private InputStream getDocumentFile(String documentID) {
        try {
            DataBinder binder = idcClient.createBinder();
            if (binder != null) {
                System.out.println("Binder is not null");
                System.out.println("USer  = " + userContext.getUser());
            }
            binder.putLocal("IdcService", "GET_FILE");
            binder.putLocal("dDocName", documentID);
            binder.putLocal("RevisionSelectionMethod", "LatestReleased");
            binder.putLocal("allowInterrupt", "1");
            binder.putLocal("Rendition", "Primary");
            response = idcClient.sendRequest(this.userContext, binder);
            if (response.getResponseStream() != null) {
                System.out.println("----------------------------- File is not null ------------------------");
            }
            return response.getResponseStream();
        } catch (IdcClientException e) {
            System.out.println("//////////////////////////////////////////////////////////////////////////////");
            e.printStackTrace();
            System.out.println("//////////////////////////////////////////////////////////////////////////////");
        }
        return null;
    }

    public String checkInNewVersion(String docTitle, String dId, String dDocAuthor, String dDocType,
                                    String dSecurityGroup, File dPrimaryFile,
                                    Map<String, String> docAttributes) throws IdcClientException, IOException,
                                                                              Exception {
        DataBinder dataBinder = null;
        dataBinder = idcClient.createBinder();
        String docName = this.getDocName(dId);
        String parentId = this.getDocParentId(dId);
        dataBinder.putLocal("IdcService", "CHECKIN_UNIVERSAL");
        dataBinder.putLocal("dDocTitle", docTitle);
        dataBinder.putLocal("dDocName", docName);
        dataBinder.putLocal("dDocAuthor", dDocAuthor);
        dataBinder.putLocal("dDocType", dDocType);
        dataBinder.putLocal("dSecurityGroup", dSecurityGroup);
        if (parentId != null && !parentId.equalsIgnoreCase("")) {
            dataBinder.putLocal("xCollectionID", parentId);
        }
        dataBinder.putLocal("dPrimaryFile", "ARCHIVE");
        if (dPrimaryFile.exists()) {
            dataBinder.addFile("primaryFile", dPrimaryFile);
        }
        if(docAttributes == null){
            docAttributes = new HashMap<>();
        }
        docAttributes.put("xISNEWVERSION", "TRUE");
        if (docAttributes != null) {
            Iterator it = docAttributes.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry) it.next();
                dataBinder.putLocal(pairs.getKey() + "", pairs.getValue() + "");
                it.remove();
            }
        }
        ServiceResponse response = idcClient.sendRequest(userContext, dataBinder);
        DataBinder responseBinder = response.getResponseAsBinder();
        Collection<String> did = responseBinder.getResultSetNames();
        System.out.println(" File Version Updated in and dID= " + responseBinder.getLocalData().get("dID"));
        if (response != null) {
            response.close();
        }
        return responseBinder.getLocalData().get("dID");
    }
    
    public String checkInNewVersion(String docTitle, String dId, String dDocAuthor, String dDocType,
                                    String dSecurityGroup, InputStream dPrimaryFile,String documentFileName,
                                    Map<String, String> docAttributes) throws IdcClientException, IOException,
                                                                              Exception {
        DataBinder dataBinder = null;
        dataBinder = idcClient.createBinder();
        String docName = this.getDocName(dId);
        String parentId = this.getDocParentId(dId);
        dataBinder.putLocal("IdcService", "CHECKIN_UNIVERSAL");
        dataBinder.putLocal("dDocTitle", docTitle);
        dataBinder.putLocal("dDocName", docName);
        dataBinder.putLocal("dDocAuthor", dDocAuthor);
        dataBinder.putLocal("dDocType", dDocType);
        dataBinder.putLocal("dSecurityGroup", dSecurityGroup);
        if (parentId != null && !parentId.equalsIgnoreCase("")) {
            dataBinder.putLocal("xCollectionID", parentId);
        }
        dataBinder.putLocal("dPrimaryFile", "ARCHIVE");
        dataBinder.addFile("primaryFile", new TransferFile(dPrimaryFile, documentFileName, dPrimaryFile.available()));
        if(docAttributes == null){
            docAttributes = new HashMap<>();
        }
        docAttributes.put("xISNEWVERSION", "TRUE");
        if (docAttributes != null) {
            Iterator it = docAttributes.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry) it.next();
                dataBinder.putLocal(pairs.getKey() + "", pairs.getValue() + "");
                it.remove();
            }
        }
        ServiceResponse response = idcClient.sendRequest(userContext, dataBinder);
        DataBinder responseBinder = response.getResponseAsBinder();
        Collection<String> did = responseBinder.getResultSetNames();
        System.out.println(" File Version Updated in and dID= " + responseBinder.getLocalData().get("dID"));
        if (response != null) {
            response.close();
        }
        return responseBinder.getLocalData().get("dID");
    }

    public String getDocParentId(String dId) {
        String dDocName = " ";
        try {
            DataBinder binder = idcClient.createBinder();
            binder.putLocal("IdcService", "DOC_INFO");
            binder.putLocal("dID", dId);
            ServiceResponse response = idcClient.sendRequest(userContext, binder);
            DataBinder responseData = response.getResponseAsBinder();
            System.out.println(responseData.getResultSetNames());
            DataResultSet ds = responseData.getResultSet("DOC_INFO");
            dDocName = ds.getRows()
                         .get(0)
                         .get("xCollectionID");
        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
            dDocName = null;
        }
        return dDocName;
    }

    public String getDocName(String dID) throws Exception {
        ServiceResponse myServiceResponse = null;
        DataBinder myBinder = idcClient.createBinder();
        myBinder.putLocal("IdcService", "DOC_INFO");
        myBinder.putLocal("dID", dID);
        myServiceResponse = idcClient.sendRequest(userContext, myBinder);
        DataBinder myResponseDataBinder = myServiceResponse.getResponseAsBinder();
        return myResponseDataBinder.getLocal("dDocName");
    }

    public InputStream getDocumentFileByDID(String did) {
        try {
            DataBinder binder = idcClient.createBinder();
            if (binder != null) {
                System.out.println("Binder is not null");
                System.out.println("USer  = " + userContext.getUser());
            }
            binder.putLocal("IdcService", "GET_FILE");
            binder.putLocal("dID", did);
            binder.putLocal("RevisionSelectionMethod", "LatestReleased");
            binder.putLocal("allowInterrupt", "1");
            binder.putLocal("Rendition", "Primary");
            response = idcClient.sendRequest(this.userContext, binder);
            if (response.getResponseStream() != null) {
                System.out.println("----------------------------- File is not null ------------------------");
            }
            return response.getResponseStream();
        } catch (IdcClientException e) {
            System.out.println("//////////////////////////////////////////////////////////////////////////////");
            e.printStackTrace();
            System.out.println("//////////////////////////////////////////////////////////////////////////////");
        }
        return null;
    }

    public void checkOutDocument(String documentName) throws CheckOutException {
        DataBinder dataBinder = null;
        try {
            dataBinder = idcClient.createBinder();
            dataBinder.putLocal("IdcService", "CHECKOUT_BY_NAME");
            dataBinder.putLocal("dDocName", documentName);
            response = idcClient.sendRequest(userContext, dataBinder);
            serverBinder = response.getResponseAsBinder();
        } catch (IdcClientException e) {
            throw new CheckOutException(e.getMessage());
        }
    }

    public void unDoCheckOutDocument(String documentName) throws CheckOutException {
        DataBinder dataBinder = null;
        try {
            dataBinder = idcClient.createBinder();
            dataBinder.putLocal("IdcService", "UNDO_CHECKOUT_BY_NAME");
            dataBinder.putLocal("dDocName", documentName);
            response = idcClient.sendRequest(userContext, dataBinder);
            serverBinder = response.getResponseAsBinder();
        } catch (IdcClientException e) {
            throw new CheckOutException(e.getMessage());
        }
    }

    private String resolveDocName(String contentType, String contentId) {
        return "DS-" + contentType.substring(0, 2).toUpperCase() +"-"+ contentId;
    }

    private Date resolveDate(String format, String dateStr) {
        Date date = null;
        try {
            format = format.replace("Z", "'Z'");
            SimpleDateFormat df = new SimpleDateFormat(format);
            date = df.parse(dateStr);
            return date;
        } catch (ParseException pe) {
            // TODO: Add catch code
            pe.printStackTrace();
        }
        return date;
    }

    public static void initializeUCM() throws ConfigurationException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        try {
            //Get a new instance of parser
            SAXParser sp = spf.newSAXParser();
            //parse the ucm-config.xml file
            InputStream in = UCMUtilities.class.getResourceAsStream("/ucm_config.xml");
            // Register the file and ReportFactory for call back of the parser
            sp.parse(in, new XMLHandler());
        } catch (SAXException exception) {
            exception.printStackTrace();
        } catch (ParserConfigurationException exception) {
            exception.printStackTrace();
        } catch (Exception exception) {
            throw new ConfigurationException("Error reading /ucm_config.xml");
        }
    }

    public static void setUsername(String username) {
        UCMUtilities.username = username;
    }

    public static String getUsername() {
        return username;
    }

    public static void setPassword(String password) {
        UCMUtilities.password = password;
    }

    public static String getPassword() {
        return password;
    }

    static class XMLHandler extends DefaultHandler {
        private String tempVal;

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equalsIgnoreCase("url")) {
                url = tempVal;
           /* } else if (qName.equalsIgnoreCase("serverHostname")) {
                serverHostname = tempVal;
                System.out.println(serverHostname);
            } else if (qName.equalsIgnoreCase("serverPort")) {
                serverPort = tempVal;*/
            //} else if (qName.equalsIgnoreCase("soaServerPort")) {
                //soaServerPort = tempVal;
            //} else if (qName.equalsIgnoreCase("soaServerHostname")) {
                //soaServerHostName = tempVal;
            } else if (qName.equalsIgnoreCase("username")) {
                username = tempVal;
            } else if (qName.equalsIgnoreCase("password")) {
                password = tempVal;
            //} else if (qName.equalsIgnoreCase("datasource")) {
                //datasource = tempVal;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            tempVal = new String(ch, start, length);
        }

        @Override
        public void startElement(String uri, String localName, String qName,
                                 Attributes attributes) throws SAXException {
            tempVal = "";
        }
    }

    public ServiceResponse createDocumentType(String contentTypeName, String description) {
        DataBinder binder = this.idcClient.createBinder();
        binder.putLocal("IdcService", "ADD_DOCTYPE");
        binder.putLocal("dDocType", contentTypeName); //Content Type name
        binder.putLocal("dDescription", description);
        binder.putLocal("dGif", "adcorp.gif");
        try {
            return this.idcClient.sendRequest(this.userContext, binder);
        } catch (IdcClientException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ServiceResponse deleteContentType(String contentTypeName) {
        DataBinder binder = this.idcClient.createBinder();
        binder.putLocal("IdcService", "DELETE_DOCTYPE");
        binder.putLocal("dDocType", contentTypeName);
        try {
            return this.idcClient.sendRequest(this.userContext, binder);
        } catch (IdcClientException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updateDocType(String dDocType, String dDescription, String dGif) throws Exception {
        DataBinder dataBinder = this.idcClient.createBinder();
        dataBinder.putLocal("IdcService", "EDIT_DOCTYPE");
        dataBinder.putLocal("dDocType", dDocType);
        dataBinder.putLocal("dDescription", dDescription);
        dataBinder.putLocal("dGif", dGif);
        this.idcClient.sendRequest(userContext, dataBinder);
    }

//    public static String getServerHostname() {
//        return serverHostname;
//    }

//    public static String getSOAServerPort() {
//        return soaServerPort;
//    }

//    public static String getSOAServerHostname() {
//        return soaServerHostName;
//    }

//    public static String getDatasourceName() {
//        return datasource;
//    }


    public void UpdateDocInfo(Map<String, String> metaData, String did,String contentId) throws Exception {

        DataBinder dataBinder = idcClient.createBinder();
        dataBinder.putLocal("IdcService", "UPDATE_DOCINFO");
        dataBinder.putLocal("dID", did);
               
        String docName = getDocName(did);
        System.out.println("dID: " + did + " | docName: " + docName);
        
        ///////////////////////////
        dataBinder.putLocal("dDocName", docName);

        dataBinder.putLocal("dSecurityGroup", "Public");


        System.out.println("metaData size ===== " + metaData.size());
        Iterator it = metaData.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();

            //match meta desc with actual meta at UCM
            String trimemdMeta = pair.getKey()
                                     .toString()
                                     .replaceAll("\\s+", "_");
//            System.out.println("trimemdMeta:" + trimemdMeta);
            dataBinder.putLocal(trimemdMeta, pair.getValue() == null ? null : pair.getValue().toString());
//            System.out.println(pair.getKey() + " = " + pair.getValue() + "from ucmbean");
            it.remove();
        }

        ServiceResponse response = idcClient.sendRequest(new IdcContext(username), dataBinder);
        System.out.println(response.getResponseAsString());
        checkResponseValidity(response);
    }

    public List<UCMDocument> getDocUrlByContentId(String contentId) throws IdcClientException, IOException {
        String query = "dDocName <matches> `" + contentId + "`";
        System.out.println("Search Query :: " + query);
        String fileUrl = "";
        IdcClient client = idcClient;
        DataBinder dataBinder = client.createBinder();
        dataBinder.putLocal("IdcService", "GET_SEARCH_RESULTS");
        dataBinder.putLocal("QueryText", query);
        dataBinder.putLocal("ResultCount", "2");
        IdcContext userContext = new IdcContext(username);
        ServiceResponse response = client.sendRequest(userContext, dataBinder);
        DataBinder binder = response.getResponseAsBinder();
        DataResultSet resultSet = binder.getResultSet("SearchResults");
        // loop over the results
        List<UCMDocument> docList = new ArrayList<UCMDocument>();
        for (DataObject dataObject : resultSet.getRows()) {
            UCMDocument resultObject = new UCMDocument();
            resultObject.setDId(dataObject.get("dID"));
            resultObject.setTitle(dataObject.get("dDocTitle"));
            resultObject.setFilename(dataObject.get("dDocName"));
            dataBinder.putLocal("IdcService", "DOC_INFO");
            String Did = resultObject.getDId();
            dataBinder.putLocal("dID", Did);
            ServiceResponse responses = client.sendRequest(userContext, dataBinder);
            DataBinder responseData = responses.getResponseAsBinder();
            String docUrl = responseData.getLocal("DocUrl");
            resultObject.setDocUrl(docUrl);
            fileUrl = docUrl;
            docList.add(resultObject);
        }
        //response.close();
        System.out.println("fileUrl/download >>>> " + fileUrl);
        return docList;
    }

    public String getDocUrlByDid(String did) throws IdcClientException, IOException {

        String query = "dID <matches> `" + did + "`";
        System.out.println("Search Query :: " + query);
        String fileUrl = "";
        IdcClient client = idcClient;
        DataBinder dataBinder = client.createBinder();
        dataBinder.putLocal("IdcService", "GET_SEARCH_RESULTS");
        dataBinder.putLocal("QueryText", query);
        dataBinder.putLocal("ResultCount", "2");
        IdcContext userContext = new IdcContext(username);
        ServiceResponse response = client.sendRequest(userContext, dataBinder);
        DataBinder binder = response.getResponseAsBinder();
        DataResultSet resultSet = binder.getResultSet("SearchResults");
        // loop over the results
        List<UCMDocument> docList = new ArrayList<UCMDocument>();
        for (DataObject dataObject : resultSet.getRows()) {
            UCMDocument resultObject = new UCMDocument();
            resultObject.setDId(dataObject.get("dID"));
            resultObject.setTitle(dataObject.get("dDocTitle"));
            resultObject.setFilename(dataObject.get("dDocName"));
            dataBinder.putLocal("IdcService", "DOC_INFO");
            String Did = resultObject.getDId();
            dataBinder.putLocal("dID", Did);
            ServiceResponse responses = client.sendRequest(userContext, dataBinder);
            DataBinder responseData = responses.getResponseAsBinder();
            String docUrl = responseData.getLocal("DocUrl");
            resultObject.setDocUrl(docUrl);
            fileUrl = docUrl;
            docList.add(resultObject);
        }
        //response.close();
        System.out.println("fileUrl/download >>>> " + fileUrl);
        return fileUrl;
    }
    
    public String getDocNameByDid(String did) throws IdcClientException, IOException {

         String query = "dID <matches> `" + did + "`";
         System.out.println("Search Query :: " + query);
         String fileUrl = "";
         IdcClient client = idcClient;
         DataBinder dataBinder = client.createBinder();
         dataBinder.putLocal("IdcService", "GET_SEARCH_RESULTS");
         dataBinder.putLocal("QueryText", query);
         dataBinder.putLocal("ResultCount", "2");
         IdcContext userContext = new IdcContext(username);
         ServiceResponse response = client.sendRequest(userContext, dataBinder);
         DataBinder binder = response.getResponseAsBinder();
         DataResultSet resultSet = binder.getResultSet("SearchResults");
         
         DataObject dataObject = resultSet.getRows().get(0);
         return dataObject.get("dDocName");
     }
    
    
    public String checkResponseValidity(ServiceResponse response) throws IdcClientException, IllegalStateException {
        if (response.getResponseType().equals(ServiceResponse.ResponseType.BINDER)) {
            DataBinder responseBinder = response.getResponseAsBinder(false); // do not check for errors
            int statusCode = responseBinder.getLocalData().getInteger("StatusCode");
            String statusMessage = responseBinder.getLocal("StatusMessage");
            if (statusCode != -1) {
                throw new IllegalStateException("Error: " + statusCode + " - " + statusMessage);
            } else {
                return "VALID";
            }
        }
        return null;
    }
    
    public void addMetaDataDef(String Metadataname, String Metadatatype) throws IdcClientException, IOException {
        IdcClient client = idcClient;
        IdcContext userContext = new IdcContext(username);
        DataBinder binder = client.createBinder();
        binder.putLocal("IdcService", "ADD_METADEF");
        binder.putLocal("dName", "x" + Metadataname);
        binder.putLocal("dCaption", Metadataname);
        binder.putLocal("dIsRequired", "0");
        binder.putLocal("dIsEnabled", "1");
        binder.putLocal("dIsSearchable", "1");
        binder.putLocal("dIsOptionList", "0");
        binder.putLocal("dOptionListType", "choice");
        /*dType desc by Abdelrahman
        The type of field. Values can be Text (Text), BigText (Long Text), Int
        (Integer), Date (Date), Memo (Memo). Default: Text
       */
        binder.putLocal("dType", Metadatatype);
        binder.putLocal("dOrder", "70000");
        binder.putLocal("dDefaultValue", "");
        ServiceResponse response = client.sendRequest(userContext, binder);
        //        System.out.println("add_metaDataDef Done .." + response.getResponseAsString());
    //        this.updateMetaTable();
        System.out.println(checkResponseValidity(response));
    }
    
    public void updateMetaTable() throws IdcClientException {
        DataBinder binder = idcClient.createBinder();
        binder.putLocal("IdcService", "UPDATE_META_TABLE");
        ServiceResponse response = idcClient.sendRequest(userContext, binder);
        System.out.println(checkResponseValidity(response));
    }
}
