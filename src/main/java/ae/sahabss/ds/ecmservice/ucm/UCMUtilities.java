package ae.sahabss.ds.ecmservice.ucm;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * UCM (Oracle WebCenter Content) gateway.
 *
 * Thread-safety: this class is a singleton Spring bean used concurrently by all requests.
 * All per-call state (DataBinder, ServiceResponse) is kept in local variables — never in
 * instance fields — so concurrent requests cannot clobber each other's state.
 * The only shared mutable field is the authenticated {@link IdcContext}, which is volatile
 * and written under synchronization in {@link #login(String, String)}.
 */
@Service
public class UCMUtilities implements IContent {

    private static final Logger logger = LoggerFactory.getLogger(UCMUtilities.class);

    private static String url;
    private static String username;
    private static String password;
    private final IdcClient idcClient;
    private volatile IdcContext userContext;

    static {
        try {
            initializeUCM();
        } catch (ConfigurationException e) {
            logger.error("Failed to initialize UCM configuration", e);
        }
    }

    public UCMUtilities() throws IdcClientException {
        this(url, username, password);
    }


    public UCMUtilities(String url, String username, String password) throws IdcClientException {
        UCMUtilities.url = url;
        UCMUtilities.username = username;
        UCMUtilities.password = password;
        IdcClientManager manager = new IdcClientManager();
        idcClient = manager.createClient(url);
    }

    /**
     * Authenticates against UCM and caches the resulting context.
     *
     * The admin credentials are fixed per deployment, so once a context has been validated
     * for the given username there is no need to repeat the PING_SERVER round-trip on
     * every request — this saves one network hop per upload/download.
     */
    public void login(String username, String password) throws UCMLoginException {
        IdcContext existingContext = this.userContext;
        if (existingContext != null && existingContext.getUser() != null
                && existingContext.getUser().equals(username)) {
            return;
        }
        synchronized (this) {
            existingContext = this.userContext;
            if (existingContext != null && existingContext.getUser() != null
                    && existingContext.getUser().equals(username)) {
                return;
            }
            try {
                IdcContext newContext = new IdcContext(username, password);
                DataBinder dataBinder = idcClient.createBinder();
                dataBinder.putLocal("IdcService", "PING_SERVER");
                ServiceResponse response = idcClient.sendRequest(newContext, dataBinder);
                try {
                    response.getResponseAsBinder();
                } finally {
                    response.close();
                }
                this.userContext = newContext;
            } catch (IdcClientException e) {
                logger.error("UCM login failed for user {}", username, e);
                throw new UCMLoginException();
            }
        }
    }

    public void login(String username) {
        try {
            IdcContext newContext = new IdcContext(username);
            DataBinder dataBinder = idcClient.createBinder();
            dataBinder.putLocal("IdcService", "PING_SERVER");
            ServiceResponse response = idcClient.sendRequest(newContext, dataBinder);
            try {
                response.getResponseAsBinder();
            } finally {
                response.close();
            }
            this.userContext = newContext;
        } catch (IdcClientException e) {
            logger.error("UCM login failed for user {}", username, e);
        }
    }

    public void login() {
        try {
            login(username, password);
        } catch (UCMLoginException e) {
            logger.error("UCM login failed for configured user", e);
        }
    }

    public String upload(String contentId, String contentType, String filename, InputStream inputStream,
                         Map<String, Object> customAttributes) throws CheckInException, IOException, Exception {
        return checkInDocument(resolveDocName(contentType, contentId), contentType + " for " + contentId, contentType, "DSharjahGroup", inputStream,
                               filename, customAttributes);
    }

    public UCMDocument getDocumentInfo(String contentId) {
        try {
            DataBinder dataBinder = idcClient.createBinder();
            dataBinder.putLocal("IdcService", "DOC_INFO_BY_NAME");
            dataBinder.putLocal("dDocName", contentId);
            ServiceResponse response = idcClient.sendRequest(userContext, dataBinder);
            try {
                DataBinder responseBinder = response.getResponseAsBinder();
                DataObject documentInfo = responseBinder.getResultSet("DOC_INFO")
                                                        .getRows()
                                                        .get(0);
                if (documentInfo == null)
                    return null;
                String format = responseBinder.getLocal("blDateFormat")
                                              .split("!")[0];
                Date checkedInDate = resolveDate(format, documentInfo.get("dInDate").toString());
                UCMDocument document = new UCMDocument();
                document.setDId(documentInfo.get("dID"));
                document.setAuthor(documentInfo.get("dDocAuthor"));
                document.setDocName(documentInfo.get("dDocName"));
                document.setCheckInDate(checkedInDate);
                document.setTitle(documentInfo.get("dDocTitle"));
                document.setRevesionId(documentInfo.get("dRevisionID"));
                document.setFormat(documentInfo.get("dFormat"));
                document.setFilename(documentInfo.get("dOriginalName"));
                document.setDocUrl(responseBinder.getLocal("DocUrl"));
                document.setContentType(documentInfo.get("dDocType"));
                return document;
            } finally {
                response.close();
            }
        } catch (IdcClientException e) {
            if (e instanceof ServiceException) {
                return null;
            }
            logger.error("Failed to fetch document info for {}", contentId, e);
            return null;
        }
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
        DataBinder dataBinder = idcClient.createBinder();
        dataBinder.putLocal("IdcService", "DELETE_DOC");
        dataBinder.putLocal("dDocName", contentId);
        try {
            ServiceResponse response = idcClient.sendRequest(userContext, dataBinder);
            try {
                response.getResponseAsBinder();
            } finally {
                response.close();
            }
            logger.debug("File {} deleted successfully", contentId);
        } catch (IdcClientException e) {
            logger.error("Failed to delete content {}", contentId, e);
        }
    }

    public void deleteContentByDID(String dID) {
        DataBinder dataBinder = idcClient.createBinder();
        dataBinder.putLocal("IdcService", "DELETE_DOC");
        dataBinder.putLocal("dID", dID);
        try {
            ServiceResponse response = idcClient.sendRequest(userContext, dataBinder);
            try {
                response.getResponseAsBinder();
            } finally {
                response.close();
            }
            logger.debug("File with dID {} deleted successfully", dID);
        } catch (IdcClientException e) {
            logger.error("Failed to delete content by dID {}", dID, e);
        }
    }

    //EDIT_RENDITIONS

    public List<Map<String, String>> getDocumentAttachments(String dId) throws IdcClientException {
        DataBinder dataBinder = idcClient.createBinder();
        dataBinder.putLocal("IdcService", "EDIT_RENDITIONS_FORM");
        dataBinder.putLocal("dID", dId);
        try {
            ServiceResponse response = idcClient.sendRequest(userContext, dataBinder);
            try {
                DataResultSet dataResultSet = response.getResponseAsBinder().getResultSet("manifest");
                List<Map<String, String>> attachments = new ArrayList<Map<String, String>>();
                for (DataObject dataObject : dataResultSet.getRows()) {
                    Map<String, String> attachment = new HashMap<String, String>();
                    attachment.put("name", dataObject.get("extRenditionName"));
                    attachment.put("description", dataObject.get("extRenditionDescription"));
                    attachments.add(attachment);
                }
                return attachments;
            } finally {
                response.close();
            }
        } catch (IdcClientException e) {
            logger.error("Failed to fetch attachments for dID {}", dId, e);
            return null;
        }
    }

    public void addAttachment() {
        DataBinder dataBinder = idcClient.createBinder();
        dataBinder.getLocalData();
        dataBinder.putLocal("IdcService", "EDIT_RENDITIONS_FORM");
        dataBinder.putLocal("extRenditionName", "3407");
        dataBinder.putLocal("extRenditionDescription", "3407");
        try {
            ServiceResponse response = idcClient.sendRequest(userContext, dataBinder);
            try {
                response.getHeaderNames();
            } finally {
                response.close();
            }
        } catch (IdcClientException e) {
            logger.error("Failed to add attachment", e);
        }
    }

    public UCMDocument getDocumentInfoById(String dId) {
        try {
            DataBinder dataBinder = idcClient.createBinder();
            dataBinder.putLocal("IdcService", "DOC_INFO");
            dataBinder.putLocal("dID", dId);
            ServiceResponse response = idcClient.sendRequest(userContext, dataBinder);
            try {
                DataBinder responseBinder = response.getResponseAsBinder();
                DataObject documentInfo = responseBinder.getResultSet("DOC_INFO")
                                                        .getRows()
                                                        .get(0);
                if (documentInfo == null)
                    return null;
                String format = responseBinder.getLocal("blDateFormat")
                                              .split("!")[0];
                Date checkedInDate = resolveDate(format, documentInfo.get("dInDate").toString());

                UCMDocument document = new UCMDocument();
                document.populateDocument(documentInfo);
                document.setCheckInDate(checkedInDate);
                document.setDocUrl(responseBinder.getLocal("DocUrl"));
                return document;
            } finally {
                response.close();
            }
        } catch (IdcClientException e) {
            if (e instanceof ServiceException) {
                return null;
            }
            logger.error("Failed to fetch document info for dID {}", dId, e);
            return null;
        }
    }

    public InputStream download(String contentId) {
        logger.debug("Downloading content id {}", contentId);
        return getDocumentFile(contentId);
    }

    public InputStream downloadById(String dId) {
        try {
            DataBinder binder = idcClient.createBinder();
            binder.putLocal("IdcService", "GET_FILE");
            binder.putLocal("dID", dId);
            binder.putLocal("allowInterrupt", "1");
            binder.putLocal("Rendition", "Primary");
            ServiceResponse response = idcClient.sendRequest(this.userContext, binder);
            return response.getResponseStream();
        } catch (IdcClientException e) {
            logger.error("Failed to download file by dID {}", dId, e);
        }
        return null;
    }

    public List<UCMDocument> getRevisions(String contentType, String contentId) {
        String docName = resolveDocName(contentType, contentId);
        List<UCMDocument> documents = new ArrayList<UCMDocument>();
        try {
            DataBinder dataBinder = idcClient.createBinder();
            dataBinder.putLocal("IdcService", "DOC_INFO_BY_NAME");
            dataBinder.putLocal("dDocName", docName);
            ServiceResponse response = idcClient.sendRequest(userContext, dataBinder);
            try {
                DataBinder responseBinder = response.getResponseAsBinder();
                DataResultSet dataResultSet = responseBinder.getResultSet("REVISION_HISTORY");
                String format = responseBinder.getLocal("blDateFormat")
                                              .split("!")[0];
                for (DataObject dataObject : dataResultSet.getRows()) {
                    UCMDocument document = new UCMDocument();
                    Date date = resolveDate(format, dataObject.get("dInDate"));
                    document.setDId(dataObject.get("dID"));
                    document.setRevesionId(dataObject.get("dRevisionID"));
                    document.setCheckInDate(date);
                    document.setFormat(dataObject.get("dFormat"));
                    document.setDocName(dataObject.get("dDocName"));
                    documents.add(document);
                }
            } finally {
                response.close();
            }
        } catch (IdcClientException e) {
            logger.error("Failed to fetch revisions for {}", docName, e);
        }
        return documents;
    }

    public List<UCMDocument> getRevisions(String dId) {
        List<UCMDocument> documents = new ArrayList<UCMDocument>();
        try {
            DataBinder dataBinder = idcClient.createBinder();
            dataBinder.putLocal("IdcService", "DOC_INFO");
            dataBinder.putLocal("dID", dId);
            ServiceResponse response = idcClient.sendRequest(userContext, dataBinder);
            try {
                DataBinder responseBinder = response.getResponseAsBinder();
                DataResultSet dataResultSet = responseBinder.getResultSet("REVISION_HISTORY");
                String format = responseBinder.getLocal("blDateFormat")
                                              .split("!")[0];
                for (DataObject dataObject : dataResultSet.getRows()) {
                    UCMDocument document = new UCMDocument();
                    Date date = resolveDate(format, dataObject.get("dInDate"));
                    document.setDId(dataObject.get("dID"));
                    document.setRevesionId(dataObject.get("dRevisionID"));
                    document.setRevesionLable(dataObject.get("dRevLabel"));
                    document.setCheckInDate(date);
                    document.setFormat(dataObject.get("dFormat"));
                    document.setDocName(dataObject.get("dDocName"));
                    documents.add(document);
                }
            } finally {
                response.close();
            }
        } catch (IdcClientException e) {
            logger.error("Failed to fetch revisions for dID {}", dId, e);
        }
        return documents;
    }

    public String checkInDocumentReturnDID(String documentName, String documentTitle, String documentType,
                                           String securityGroup, InputStream primaryFile, String documentFileName,
                                           Map<String, Object> customMetadata) throws IOException, Exception {
        try {
            DataBinder dataBinder = idcClient.createBinder();
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
                        logger.debug("Custom metadata {} = {}", s, customMetadata.get(s));
                        dataBinder.putLocal(s, String.valueOf(customMetadata.get(s)));
                    }
                }
            }
            ServiceResponse response = idcClient.sendRequest(this.userContext, dataBinder);
            try {
                DataBinder responseBinder = response.getResponseAsBinder();
                return responseBinder.getLocalData().get("dID");
            } finally {
                response.close();
            }
        } catch (IdcClientException e) {
            logger.error("UCM check-in failed for document {}", documentName, e);
            throw e;
        } catch (IOException e) {
            logger.error("IO error during check-in of document {}", documentName, e);
            throw e;
        }
    }

    private Map<String, String> checkInDocumentReturnDocInfo(String documentName, String documentTitle,
                                                             String documentType, String securityGroup,
                                                             InputStream primaryFile, String documentFileName,
                                                             Map<String, Object> customMetadata) throws IOException,
                                                                                                        Exception {
        try {
            DataBinder dataBinder = idcClient.createBinder();
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
            Map<String, String> docInfo = new HashMap<String, String>();
            ServiceResponse checkInResponse = idcClient.sendRequest(this.userContext, dataBinder);
            try {
                DataBinder responseBinder = checkInResponse.getResponseAsBinder();
                docInfo.put("DID", responseBinder.getLocalData().get("dID"));
            } finally {
                checkInResponse.close();
            }
            // Get Doc Info
            dataBinder = idcClient.createBinder();
            dataBinder.putLocal("IdcService", "DOC_INFO");
            dataBinder.putLocal("dID", docInfo.get("DID"));
            ServiceResponse docInfoResponse = idcClient.sendRequest(this.userContext, dataBinder);
            try {
                DataBinder respBinder = docInfoResponse.getResponseAsBinder();
                String url = respBinder.getLocal("DocUrl");
                String tempFileName = url.substring(url.lastIndexOf("/") + 1);
                String fileName = tempFileName;
                if (tempFileName.contains("~")) {
                    fileName = tempFileName.substring(0, tempFileName.lastIndexOf("~"));
                    fileName = fileName + tempFileName.substring(tempFileName.lastIndexOf("."));
                    url = url.replace(tempFileName, fileName);
                }
                docInfo.put("DocUrl", url);
            } finally {
                docInfoResponse.close();
            }
            return docInfo;
        } catch (IdcClientException e) {
            logger.error("UCM check-in failed for document {}", documentName, e);
            throw e;
        } catch (IOException e) {
            logger.error("IO error during check-in of document {}", documentName, e);
            throw e;
        }
    }

    private String checkInDocument(String documentName, String documentTitle, String documentType, String securityGroup,
                                   InputStream primaryFile, String documentFileName,
                                   Map<String, Object> customMetadata) throws IOException, Exception {
        try {
            DataBinder dataBinder = idcClient.createBinder();
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
            ServiceResponse response = idcClient.sendRequest(this.userContext, dataBinder);
            try {
                DataBinder responseBinder = response.getResponseAsBinder();
                logger.debug("File checked in with dID {} and dDocName {}",
                        responseBinder.getLocalData().get("dID"), responseBinder.getLocal("dDocName"));
                return responseBinder.getLocalData().get("dDocName");
            } finally {
                response.close();
            }
        } catch (IdcClientException e) {
            logger.error("UCM check-in failed for document {}", documentName, e);
            throw e;
        } catch (IOException e) {
            logger.error("IO error during check-in of document {}", documentName, e);
            throw e;
        }
    }

    private InputStream getDocumentFile(String documentID) {
        try {
            DataBinder binder = idcClient.createBinder();
            binder.putLocal("IdcService", "GET_FILE");
            binder.putLocal("dDocName", documentID);
            binder.putLocal("RevisionSelectionMethod", "LatestReleased");
            binder.putLocal("allowInterrupt", "1");
            binder.putLocal("Rendition", "Primary");
            ServiceResponse response = idcClient.sendRequest(this.userContext, binder);
            return response.getResponseStream();
        } catch (IdcClientException e) {
            logger.error("Failed to fetch file {} from UCM", documentID, e);
        }
        return null;
    }

    public String checkInNewVersion(String docTitle, String dId, String dDocAuthor, String dDocType,
                                    String dSecurityGroup, File dPrimaryFile,
                                    Map<String, String> docAttributes) throws IdcClientException, IOException,
                                                                              Exception {
        DataBinder dataBinder = idcClient.createBinder();
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
        if (docAttributes == null) {
            docAttributes = new HashMap<>();
        }
        docAttributes.put("xISNEWVERSION", "TRUE");
        Iterator it = docAttributes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            dataBinder.putLocal(pairs.getKey() + "", pairs.getValue() + "");
            it.remove();
        }
        ServiceResponse response = idcClient.sendRequest(userContext, dataBinder);
        try {
            DataBinder responseBinder = response.getResponseAsBinder();
            logger.debug("File version updated with dID {}", responseBinder.getLocalData().get("dID"));
            return responseBinder.getLocalData().get("dID");
        } finally {
            response.close();
        }
    }

    public String checkInNewVersion(String docTitle, String dId, String dDocAuthor, String dDocType,
                                    String dSecurityGroup, InputStream dPrimaryFile, String documentFileName,
                                    Map<String, String> docAttributes) throws IdcClientException, IOException,
                                                                              Exception {
        DataBinder dataBinder = idcClient.createBinder();
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
        if (docAttributes == null) {
            docAttributes = new HashMap<>();
        }
        docAttributes.put("xISNEWVERSION", "TRUE");
        Iterator it = docAttributes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            dataBinder.putLocal(pairs.getKey() + "", pairs.getValue() + "");
            it.remove();
        }
        ServiceResponse response = idcClient.sendRequest(userContext, dataBinder);
        try {
            DataBinder responseBinder = response.getResponseAsBinder();
            logger.debug("File version updated with dID {}", responseBinder.getLocalData().get("dID"));
            return responseBinder.getLocalData().get("dID");
        } finally {
            response.close();
        }
    }

    public String getDocParentId(String dId) {
        String dDocName = " ";
        try {
            DataBinder binder = idcClient.createBinder();
            binder.putLocal("IdcService", "DOC_INFO");
            binder.putLocal("dID", dId);
            ServiceResponse response = idcClient.sendRequest(userContext, binder);
            try {
                DataBinder responseData = response.getResponseAsBinder();
                DataResultSet ds = responseData.getResultSet("DOC_INFO");
                dDocName = ds.getRows()
                             .get(0)
                             .get("xCollectionID");
            } finally {
                response.close();
            }
        } catch (Exception ex) {
            logger.error("Failed to fetch parent id for dID {}", dId, ex);
            dDocName = null;
        }
        return dDocName;
    }

    public String getDocName(String dID) throws Exception {
        DataBinder myBinder = idcClient.createBinder();
        myBinder.putLocal("IdcService", "DOC_INFO");
        myBinder.putLocal("dID", dID);
        ServiceResponse myServiceResponse = idcClient.sendRequest(userContext, myBinder);
        try {
            DataBinder myResponseDataBinder = myServiceResponse.getResponseAsBinder();
            return myResponseDataBinder.getLocal("dDocName");
        } finally {
            myServiceResponse.close();
        }
    }

    public InputStream getDocumentFileByDID(String did) {
        try {
            DataBinder binder = idcClient.createBinder();
            binder.putLocal("IdcService", "GET_FILE");
            binder.putLocal("dID", did);
            binder.putLocal("RevisionSelectionMethod", "LatestReleased");
            binder.putLocal("allowInterrupt", "1");
            binder.putLocal("Rendition", "Primary");
            ServiceResponse response = idcClient.sendRequest(this.userContext, binder);
            return response.getResponseStream();
        } catch (IdcClientException e) {
            logger.error("Failed to fetch file by dID {} from UCM", did, e);
        }
        return null;
    }

    public void checkOutDocument(String documentName) throws CheckOutException {
        try {
            DataBinder dataBinder = idcClient.createBinder();
            dataBinder.putLocal("IdcService", "CHECKOUT_BY_NAME");
            dataBinder.putLocal("dDocName", documentName);
            ServiceResponse response = idcClient.sendRequest(userContext, dataBinder);
            try {
                response.getResponseAsBinder();
            } finally {
                response.close();
            }
        } catch (IdcClientException e) {
            throw new CheckOutException(e.getMessage());
        }
    }

    public void unDoCheckOutDocument(String documentName) throws CheckOutException {
        try {
            DataBinder dataBinder = idcClient.createBinder();
            dataBinder.putLocal("IdcService", "UNDO_CHECKOUT_BY_NAME");
            dataBinder.putLocal("dDocName", documentName);
            ServiceResponse response = idcClient.sendRequest(userContext, dataBinder);
            try {
                response.getResponseAsBinder();
            } finally {
                response.close();
            }
        } catch (IdcClientException e) {
            throw new CheckOutException(e.getMessage());
        }
    }

    private String resolveDocName(String contentType, String contentId) {
        return "DS-" + contentType.substring(0, 2).toUpperCase() + "-" + contentId;
    }

    private Date resolveDate(String format, String dateStr) {
        Date date = null;
        try {
            format = format.replace("Z", "'Z'");
            SimpleDateFormat df = new SimpleDateFormat(format);
            date = df.parse(dateStr);
            return date;
        } catch (ParseException pe) {
            logger.error("Failed to parse date {} with format {}", dateStr, format, pe);
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
            logger.error("Failed to parse /ucm_config.xml", exception);
        } catch (ParserConfigurationException exception) {
            logger.error("Failed to configure XML parser for /ucm_config.xml", exception);
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
            } else if (qName.equalsIgnoreCase("username")) {
                username = tempVal;
            } else if (qName.equalsIgnoreCase("password")) {
                password = tempVal;
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
            logger.error("Failed to create document type {}", contentTypeName, e);
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
            logger.error("Failed to delete document type {}", contentTypeName, e);
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

    public void UpdateDocInfo(Map<String, String> metaData, String did, String contentId) throws Exception {

        DataBinder dataBinder = idcClient.createBinder();
        dataBinder.putLocal("IdcService", "UPDATE_DOCINFO");
        dataBinder.putLocal("dID", did);

        String docName = getDocName(did);
        logger.debug("dID: {} | docName: {}", did, docName);

        dataBinder.putLocal("dDocName", docName);
        dataBinder.putLocal("dSecurityGroup", "Public");

        logger.debug("metaData size = {}", metaData.size());
        Iterator it = metaData.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();

            //match meta desc with actual meta at UCM
            String trimemdMeta = pair.getKey()
                                     .toString()
                                     .replaceAll("\\s+", "_");
            dataBinder.putLocal(trimemdMeta, pair.getValue() == null ? null : pair.getValue().toString());
            it.remove();
        }

        ServiceResponse response = idcClient.sendRequest(new IdcContext(username), dataBinder);
        try {
            checkResponseValidity(response);
        } finally {
            response.close();
        }
    }

    public List<UCMDocument> getDocUrlByContentId(String contentId) throws IdcClientException, IOException {
        String query = "dDocName <matches> `" + contentId + "`";
        logger.debug("Search query: {}", query);
        IdcClient client = idcClient;
        DataBinder dataBinder = client.createBinder();
        dataBinder.putLocal("IdcService", "GET_SEARCH_RESULTS");
        dataBinder.putLocal("QueryText", query);
        dataBinder.putLocal("ResultCount", "2");
        IdcContext searchContext = new IdcContext(username);
        ServiceResponse response = client.sendRequest(searchContext, dataBinder);
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
            ServiceResponse responses = client.sendRequest(searchContext, dataBinder);
            DataBinder responseData = responses.getResponseAsBinder();
            String docUrl = responseData.getLocal("DocUrl");
            resultObject.setDocUrl(docUrl);
            docList.add(resultObject);
        }
        return docList;
    }

    public String getDocUrlByDid(String did) throws IdcClientException, IOException {

        String query = "dID <matches> `" + did + "`";
        logger.debug("Search query: {}", query);
        String fileUrl = "";
        IdcClient client = idcClient;
        DataBinder dataBinder = client.createBinder();
        dataBinder.putLocal("IdcService", "GET_SEARCH_RESULTS");
        dataBinder.putLocal("QueryText", query);
        dataBinder.putLocal("ResultCount", "2");
        IdcContext searchContext = new IdcContext(username);
        ServiceResponse response = client.sendRequest(searchContext, dataBinder);
        DataBinder binder = response.getResponseAsBinder();
        DataResultSet resultSet = binder.getResultSet("SearchResults");
        // loop over the results
        for (DataObject dataObject : resultSet.getRows()) {
            UCMDocument resultObject = new UCMDocument();
            resultObject.setDId(dataObject.get("dID"));
            resultObject.setTitle(dataObject.get("dDocTitle"));
            resultObject.setFilename(dataObject.get("dDocName"));
            dataBinder.putLocal("IdcService", "DOC_INFO");
            String Did = resultObject.getDId();
            dataBinder.putLocal("dID", Did);
            ServiceResponse responses = client.sendRequest(searchContext, dataBinder);
            DataBinder responseData = responses.getResponseAsBinder();
            String docUrl = responseData.getLocal("DocUrl");
            resultObject.setDocUrl(docUrl);
            fileUrl = docUrl;
        }
        logger.debug("fileUrl/download >>>> {}", fileUrl);
        return fileUrl;
    }

    public String getDocNameByDid(String did) throws IdcClientException, IOException {

        String query = "dID <matches> `" + did + "`";
        logger.debug("Search query: {}", query);
        IdcClient client = idcClient;
        DataBinder dataBinder = client.createBinder();
        dataBinder.putLocal("IdcService", "GET_SEARCH_RESULTS");
        dataBinder.putLocal("QueryText", query);
        dataBinder.putLocal("ResultCount", "2");
        IdcContext searchContext = new IdcContext(username);
        ServiceResponse response = client.sendRequest(searchContext, dataBinder);
        try {
            DataBinder binder = response.getResponseAsBinder();
            DataResultSet resultSet = binder.getResultSet("SearchResults");
            DataObject dataObject = resultSet.getRows().get(0);
            return dataObject.get("dDocName");
        } finally {
            response.close();
        }
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
        IdcContext metaContext = new IdcContext(username);
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
        ServiceResponse response = client.sendRequest(metaContext, binder);
        logger.debug("addMetaDataDef result: {}", checkResponseValidity(response));
    }

    public void updateMetaTable() throws IdcClientException {
        DataBinder binder = idcClient.createBinder();
        binder.putLocal("IdcService", "UPDATE_META_TABLE");
        ServiceResponse response = idcClient.sendRequest(userContext, binder);
        logger.debug("updateMetaTable result: {}", checkResponseValidity(response));
    }
}
