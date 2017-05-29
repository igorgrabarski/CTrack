package igorgrabarski.com.ctrack;

import java.util.Date;

/**
 * Created by igorgrabarski on 2017-05-27.
 */

public class PersonLocation {
    private double lat;
    private double lng;
    private Date currentDateTime;
    private int cid;
    private int lac;



    public double getLat() {

        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public Date getCurrentDateTime() {
        return currentDateTime;
    }

    public void setCurrentDateTime(Date currentDateTime) {
        this.currentDateTime = currentDateTime;
    }


    public int getCid() {
        return cid;
    }

    public void setCid(int cid) {
        this.cid = cid;
    }

    public int getLac() {
        return lac;
    }

    public void setLac(int lac) {
        this.lac = lac;
    }
}
