package org.springframework.cloud.service.common;

import org.springframework.cloud.service.ServiceInfo;

@ServiceInfo.ServiceLabel("oracle")
public class OracleServiceInfo extends RelationalServiceInfo {

	public OracleServiceInfo(String id, String url) {
		super(id, url, "oracle");
	}

	@Override
	public String getJdbcUrl() {
		return String.format("jdbc:%s:thin:%s/%s@%s:%d/%s",
				jdbcUrlDatabaseType, getUserName(), getPassword(),
				getHost(), getPort(), getPath());
	}

}
