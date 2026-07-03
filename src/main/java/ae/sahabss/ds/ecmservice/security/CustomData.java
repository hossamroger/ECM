package ae.sahabss.ds.ecmservice.security;

public class CustomData {

    private String mob;
    private String code;
    private String authLevel;
    private String dsUserCode;
    private boolean entity;

    public boolean getEntity() {
        return entity;
    }

    public void setEntity(boolean entity) {
        this.entity = entity;
    }

    public String getDsUserCode() {
        return dsUserCode;
    }

    public void setDsUserCode(String dsUserCode) {
        this.dsUserCode = dsUserCode;
    }

    public String getMob() {
        return mob;
    }

    public void setMob(String mob) {
        this.mob = mob;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getAuthLevel() {
        return authLevel;
    }

    public void setAuthLevel(String authLevel) {
        this.authLevel = authLevel;
    }
}
