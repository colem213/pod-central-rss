package io.podcentral.config;

public class TableConstants {
  public static class Feed {
    public static final String TABLE_NAME = "Feed";
    public static final String GSI_URI_INDEX = "UriIndex";
  }
  public static class Entry {
    public static final String TABLE_NAME = "Entry";
    public static final String GSI_FEED_INDEX = "FeedIndex";
  }
  public static class UserFeed {
    public static final String TABLE_NAME = "UserFeed";
    public static final String LSI_DATE_INDEX = "SubDateIndex";
  }
  public static class ChannelUrl {
    public static final String TABLE_NAME = "ChannelUrl";
    public static final String GSI_CHANNEL_INDEX = "ChannelIndex";
  }
}
