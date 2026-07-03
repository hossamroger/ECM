package ae.sahabss.ds.ecmservice.utils;

public enum FileTypeEnum {
    PNG("image/png", new byte[]{(byte) 0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A}, "png"),
    JPEG("image/jpeg", new byte[]{(byte)0xFF,(byte)0xD8,(byte)0xFF}, "jpg,jpeg"),
    PDF("application/pdf", new byte[]{0x25,0x50,0x44,0x46}, "pdf"),
    BMP("image/bmp", new byte[]{0x42,0x4D}, "bmp"),
    RAR("application/x-rar-compressed", new byte[]{0x52,0x61,0x72,0x21,(byte)0x1A,0x07,0x01,0x00}, "rar"),
    ZIP("application/x-zip-compressed", new byte[]{0x50,0x4B,0x03,0x04}, "zip"),
    MP4("video/mp4", new byte[]{0x66,0x74,0x79,0x70,0x69,0x73,0x6F,0x6D}, "mp4"),
    MOV("video/quicktime", new byte[]{0x66,0x74,0x79,0x70,0x71,0x74,0x20,0x20}, "mov"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{0x50,0x4B,0x03,0x04}, "xlsx"),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", new byte[]{0x50,0x4B,0x03,0x04}, "docx"),
    DOC("application/msword", new byte[]{(byte)0xD0,(byte)0xCF,0x11,(byte)0xE0,(byte)0xA1,(byte)0xB1,0x1A,(byte)0xE1}, "doc"),
    CSV("text/plain", new byte[]{}, "csv");


    final byte[] hexNumber;
    public final String mimeFileType;
    public final String fileExt;


    FileTypeEnum(String mimeFileType, byte[] hexNumber, String fileExt) {
        this.hexNumber = hexNumber;
        this.mimeFileType = mimeFileType;
        this.fileExt = fileExt;
    }

    private static boolean isMatch(byte[] pattern, byte[] data) {
        if (pattern.length <= data.length) {
            for (int idx = 0; idx < pattern.length; ++idx) {
                if (pattern[idx] != data[idx])
                    return false;
            }
            return true;
        }

        return false;
    }
    public static String getFileExtension(String fileName) {
        int lastIndexOf = fileName.lastIndexOf(".");
        if (lastIndexOf < 1) {
            return ""; // empty extension
        }
        return fileName.substring(lastIndexOf+1);
    }

    public static boolean validateExt(String allowedEXT , String fileName){
        String[] allowedExtList = allowedEXT.split(",");
        for (String ext : allowedExtList) {
            if ( ext.equalsIgnoreCase(getFileExtension(fileName)))
                return true;
        }
        return false;

    }
    public static Boolean isValidType(String actualMimeType, String mimeFileType,String fileName) {
        for (FileTypeEnum fileTypeEnum : FileTypeEnum.values()) {
            if (fileTypeEnum.mimeFileType.equalsIgnoreCase(actualMimeType) && fileTypeEnum.mimeFileType.equalsIgnoreCase(mimeFileType) && validateExt(fileTypeEnum.fileExt,fileName))
                return true;
        }
        return false;
    }
}
