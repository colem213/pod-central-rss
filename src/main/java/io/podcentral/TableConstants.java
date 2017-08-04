package io.podcentral;

public class TableConstants {
  public static class Channel {
    public static final String TABLE_NAME = "Channel";
  }
  public static class Item {
    public static final String TABLE_NAME = "Item";
    public static final String GSI_CHANNEL_INDEX = "ChannelIndex";
  }
  public static class Subscription {
    public static final String TABLE_NAME = "Subscription";
    public static final String LSI_DATE_INDEX = "SubDateIndex";
  }
  public static class ChannelUrl {
    public static final String TABLE_NAME = "ChannelUrl";
    public static final String GSI_CHANNEL_INDEX = "ChannelIndex";
  }
}
