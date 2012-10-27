/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.background.test;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.mozilla.android.sync.test.helpers.HTTPServerTestHelper;
import org.mozilla.android.sync.test.helpers.MockServer;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.background.announcements.Announcement;
import org.mozilla.gecko.background.announcements.AnnouncementsConstants;
import org.mozilla.gecko.background.announcements.AnnouncementsFetchDelegate;
import org.mozilla.gecko.background.announcements.AnnouncementsFetcher;
import org.mozilla.gecko.sync.net.BaseResource;
import org.simpleframework.http.Path;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

public class TestAnnouncementFetch {
  private static final int    TEST_PORT   = HTTPServerTestHelper.getTestPort();
  private static final String TEST_SERVER = "http://127.0.0.1:" + TEST_PORT;
  private static final String TEST_USER_AGENT = "TEST USER AGENT";
  private static final String BASE_PATH   = "/announce/";
  private static final String BASE_URI    = TEST_SERVER + BASE_PATH + AnnouncementsConstants.ANNOUNCE_PATH_SUFFIX;

  private HTTPServerTestHelper data = new HTTPServerTestHelper();

  private static void debug(String s) {
    System.out.println(s);
  }

  public static final class MockFetchDelegate implements AnnouncementsFetchDelegate {
    private final long now;
    public List<Announcement> fetchedAnnouncements;

    public MockFetchDelegate(long now) {
      this.now = now;
    }

    private void done() {
      WaitHelper.getTestWaiter().performNotify();
    }

    private void done(Exception e) {
      e.printStackTrace();
      Assert.fail("Error. " + e);
      done();
    }

    @Override
    public void onNewAnnouncements(List<Announcement> announcements, long fetched) {
      this.fetchedAnnouncements = announcements;
      Assert.assertTrue(fetched >= now);
      done();
    }

    @Override
    public void onNoNewAnnouncements(long fetched) {
      Assert.fail("No new announcements. Fetched = " + fetched);
      done();
    }

    @Override
    public void onRemoteError(Exception e) {
      done(e);
    }

    @Override
    public void onLocalError(Exception e) {
      done(e);
    }

    @Override
    public void onRemoteFailure(int status) {
      Assert.fail("Failure. " + status);
      done();
    }

    @Override
    public Locale getLocale() {
      return new Locale("en", "gb");
    }

    @Override
    public long getLastFetch() {
      return 0L;
    }

    @Override
    public String getUserAgent() {
      return TEST_USER_AGENT;
    }

    @Override
    public String getServiceURL() {
      return BASE_URI;
    }

    @Override
    public void onBackoff(int retryAfterInSeconds) {
      // Do nothing.
    }
  }

  public class TestAnnouncement extends Announcement {
    private int idleLower = 0;
    private int idleUpper = Integer.MAX_VALUE;

    private long availableFrom = 0;
    private long availableTo   = Long.MAX_VALUE;

    private Locale targetLocale = null;
    private String versionRegex = null;
    private String channelRegex = null;
    private String platformRegex = null;

    /**
     * Set the range of idle days that should match this snippet.
     * @param lower If less than zero, rounds to zero.
     * @param upper If zero, any value will match.
     */
    public void setIdleBounds(int lower, int upper) {
      this.idleLower = Math.min(0, lower);
      this.idleUpper = (upper == 0) ? Integer.MAX_VALUE : upper;
    }

    public void setAvailableBounds(long lower, long upper) {
      this.availableFrom = Math.min(0L, lower);
      this.availableTo   = (upper == 0L) ? Long.MAX_VALUE : upper;
    }

    public void setTargetLocale(Locale locale) {
      this.targetLocale = locale;
    }
    public void setVersionRegex(String regex) {
      this.versionRegex = regex;
    }
    public void setChannelRegex(String regex) {
      this.channelRegex = regex;
    }
    public void setPlatformRegex(String regex) {
      this.platformRegex = regex;
    }

    public TestAnnouncement(int id, String title, String text, URI uri) {
      super(id, title, text, uri);
    }

    protected boolean localesMatch(List<Locale> locales) {
      if (null == this.targetLocale) {
        return true;
      }
      for (Locale locale : locales) {
        // Thanks, tools, for messing around with case.
        if (locale.toString().equalsIgnoreCase(this.targetLocale.toString().toLowerCase())) {
          return true;
        }
      }
      return false;
    }

    public boolean matches(String channel, String version, String platform, List<Locale> locales, int idle) {
      long now = System.currentTimeMillis();
      if (now < this.availableFrom ||
          now > this.availableTo) {
        debug("Not available.");
        return false;
      }

      if (null != this.channelRegex) {
        if (!channel.matches(this.channelRegex)) {
          debug("No channel match.");
          return false;
        }
      }
      if (null != this.platformRegex) {
        if (!platform.matches(this.platformRegex)) {
          debug("No platform match.");
          return false;
        }
      }
      if (null != this.versionRegex) {
        if (!version.matches(this.versionRegex)) {
          debug("No version match.");
          return false;
        }
      }
      if (!this.localesMatch(locales)) {
        debug("No locale match.");
        return false;
      }
      if (idle < this.idleLower) {
        debug("No idle match (too low).");
        return false;
      }
      if (idle > this.idleUpper) {
        debug("No idle match (too high).");
        return false;
      }
      return true;
    }
  }

  /**
   * Respond to an announce API request by returning announcement JSON or
   * response codes per
   *
   * https://wiki.mozilla.org/Services/Roadmaps/Campaign-Manager#Client_API
   *
   */
  public class AnnouncementFetchMockServer extends MockServer {
    // announce/1/android/channel/version/platform
    private static final int    EXPECTED_PATH_LENGTH = 6;

    private final ArrayList<TestAnnouncement> announcements = new ArrayList<TestAnnouncement>();

    @SuppressWarnings("unchecked")
    public void handle(Request request, Response response) {
      try {
        final String ua = request.getValue("user-agent");
        debug("User-Agent: " + ua);
        Assert.assertEquals(TEST_USER_AGENT, ua);

        final List<Locale> locales = request.getLocales();
        debug("Locales: " + locales + ", " + locales.get(0));
        final String method = request.getMethod();
        final Path path = request.getPath();
        debug("Path: " + path);

        if (!method.equalsIgnoreCase("get")) {
          this.handleBasicHeaders(request, response, 405, "text/plain");
        }

        final String[] segments = path.getSegments();
        if (segments.length != EXPECTED_PATH_LENGTH) {
          this.handleBasicHeaders(request, response, 400, "text/plain");
        }

        // Don't bother with additional validation. This is test code!
        String protocol = segments[1];
        String product  = segments[2];
        String channel  = segments[3];
        String version  = segments[4];
        String platform = segments[5];
        int idle        = request.getQuery().getInteger("idle");

        // These will cause the test to bomb out if they fail.
        Assert.assertEquals("android", product);
        Assert.assertEquals("1", protocol);

        JSONArray matchingAnnouncements = this.getAnnouncements(channel, version, platform, locales, idle);
        JSONObject body = new JSONObject();
        body.put("announcements", matchingAnnouncements);
        final PrintStream bodyStream = this.handleBasicHeaders(request, response, 200, "application/json");
        bodyStream.print(body.toJSONString());
        bodyStream.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    @SuppressWarnings("unchecked")
    private JSONArray getAnnouncements(String channel, String version,
                                  String platform, List<Locale> locales,
                                  int idle) {
      JSONArray out = new JSONArray();
      for (TestAnnouncement snippet : announcements) {
        if (snippet.matches(channel, version, platform, locales, idle)) {
          out.add(snippet.asJSON());
        }
      }
      return out;
    }

    public void addAnnouncement(TestAnnouncement an) {
      this.announcements.add(an);
    }
  }

  private static final String TEST_ANNOUNCEMENT_ONE_TITLE = "Test announce one";

  private TestAnnouncement prepareTestAnnouncementOne() {
    URI uri;
    try {
      uri = new URI("http://example.com/");
    } catch (URISyntaxException e) {
      // It's fine.
      return null;
    }
    String text  = "Test snippet body is longer than the title.";
    String title = TEST_ANNOUNCEMENT_ONE_TITLE;
    int id       = 1234;
    TestAnnouncement announcement = new TestAnnouncement(id, title, text, uri);
    announcement.setChannelRegex("^beta$");
    announcement.setIdleBounds(2, 5);
    announcement.setPlatformRegex("^arm.*$");
    announcement.setTargetLocale(new Locale("en", "gb"));
    announcement.setVersionRegex("^17\\..*$");
    debug("Adding snippet:\n" + announcement.asJSON());
    return announcement;
  }

  @Test
  public void testAnnouncementFetch() throws URISyntaxException {
    BaseResource.rewriteLocalhost = false;

    AnnouncementFetchMockServer mockServer = new AnnouncementFetchMockServer();

    // Add a mock announcement.
    mockServer.addAnnouncement(prepareTestAnnouncementOne());

    data.startHTTPServer(mockServer);
    debug("Server started.");

    // Make a request that matches.
    final URI uri = AnnouncementsFetcher.getSnippetURI(BASE_URI, "beta", "17.0a1", "armeabi-v7a", 4);
    final long now = System.currentTimeMillis();

    final MockFetchDelegate delegate = new MockFetchDelegate(now);

    WaitHelper.getTestWaiter().performWait(new Runnable() {
      @Override
      public void run() {
        AnnouncementsFetcher.fetchAnnouncements(uri, delegate);
      }
    });

    Assert.assertEquals(1, delegate.fetchedAnnouncements.size());
    Assert.assertEquals(TEST_ANNOUNCEMENT_ONE_TITLE, delegate.fetchedAnnouncements.get(0).getTitle());
    data.stopHTTPServer();
  }
}