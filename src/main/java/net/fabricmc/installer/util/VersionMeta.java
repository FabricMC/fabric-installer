package net.fabricmc.installer.util;

import java.util.Map;

public class VersionMeta {

	public String id;
	public Map<String, Download> downloads;

	public static class Download {
		public String sha1;
		public long size;
		public String url;
	}


}
