package com.purelyprep.pojo;

import java.io.Serializable;

public class Proxy implements Serializable {

    public Proxy() {}

    public Proxy(String ip, String port) {
        this.ip = ip;
        this.port = port;
    }

    public String ip;
    public String port;

    public String getProxyStr() {
        return ip + ":" + port;
    }

}
