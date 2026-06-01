package de.punishsystem.vpn;

public class VpnResult {

    private final boolean vpn;
    private final String isp;
    private final String countryCode;
    private final long checkedAt;

    public VpnResult(boolean vpn, String isp, String countryCode) {
        this.vpn = vpn;
        this.isp = isp != null ? isp : "";
        this.countryCode = countryCode != null ? countryCode : "";
        this.checkedAt = System.currentTimeMillis();
    }

    public boolean isVpn() { return vpn; }
    public String getIsp() { return isp; }
    public String getCountryCode() { return countryCode; }
    public long getCheckedAt() { return checkedAt; }
}
