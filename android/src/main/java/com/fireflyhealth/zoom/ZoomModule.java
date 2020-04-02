package com.fireflyhealth.zoom;

import us.zoom.sdk.MeetingError;
import us.zoom.sdk.MeetingService;
import us.zoom.sdk.MeetingServiceListener;
import us.zoom.sdk.MeetingStatus;
import us.zoom.sdk.MeetingSettingsHelper;
import us.zoom.sdk.JoinMeetingOptions;
import us.zoom.sdk.JoinMeetingParams;
import us.zoom.sdk.ZoomError;
import us.zoom.sdk.ZoomSDK;
import us.zoom.sdk.ZoomSDKInitParams;
import us.zoom.sdk.ZoomSDKInitializeListener;
import us.zoom.sdk.ZoomSDKAuthenticationListener;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;

import android.util.Log;
import android.widget.Toast;

public class ZoomModule extends ReactContextBaseJavaModule
    implements ZoomSDKInitializeListener, MeetingServiceListener {

  private Promise initializePromise;
  private Promise meetingPromise;
  private ZoomSDK mZoomSDK;

  private final ReactApplicationContext reactContext;

  public ZoomModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  public String getName() {
    return "Zoom";
  }

  /*
   * Sample method available to JS
   * 
   * @ReactMethod public void sampleMethod(String stringArgument, int
   * numberArgument, Callback callback) { // TODO: Implement some actually useful
   * functionality callback.invoke("Received numberArgument: " + numberArgument +
   * " stringArgument: " + stringArgument); }
   */

  @ReactMethod
  public void initialize(final String appKey, final String appSecret, final String webDomain, final Promise promise) {
    if (mZoomSDK != null && mZoomSDK.isInitialized()) {
      promise.resolve("Already initialized Zoom SDK successfully.");
      return;
    }

    try {
      initializePromise = promise;

      reactContext.getCurrentActivity().runOnUiThread(new Runnable() {
        @Override
        public void run() {

          ZoomSDKInitParams initParams = new ZoomSDKInitParams();
          initParams.appKey = appKey;
          initParams.appSecret = appSecret;
          initParams.domain = webDomain;
          mZoomSDK = ZoomSDK.getInstance();
          // Initialize SDK
          mZoomSDK.initialize(reactContext.getCurrentActivity(), ZoomModule.this, initParams);
        }
      });
    } catch (Exception ex) {
      promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
    }
  }

  @Override
  public void onZoomSDKInitializeResult(int errorCode, int internalErrorCode) {
    if (errorCode == ZoomError.ZOOM_ERROR_SUCCESS) {
      toast("Zoom SDK initialized");
      registerMeetingServiceListener();
    } else {
      toast("Failed to initialize Zoom. Error code: " + errorCode);
    }
  }

  @Override
  public void onZoomAuthIdentityExpired() {
    toast("IDK MAAAAN");
  }

  private void registerMeetingServiceListener() {
    MeetingService meetingService = mZoomSDK.getMeetingService();

    MeetingSettingsHelper MeetingSettings = mZoomSDK.getMeetingSettingsHelper();
    // Enable meeting settings
    MeetingSettings.enableForceAutoStartMyVideoWhenJoinMeeting(true);
    MeetingSettings.disableChatUI(true);
    MeetingSettings.setAutoConnectVoIPWhenJoinMeeting(true);
    if (meetingService != null) {
      meetingService.addListener(ZoomModule.this);
      toast("Added listener");
    }
  }

  private void toast(String string) {
    Toast.makeText(reactContext.getCurrentActivity(), string, Toast.LENGTH_LONG).show();
  }

  @ReactMethod
  public void joinMeeting(final String meetingNo, final String displayName, Promise promise) {
    toast(meetingNo + ", " + displayName);
    try {
      meetingPromise = promise;
      if (!mZoomSDK.isInitialized()) {
        promise.reject("ERR_ZOOM_JOIN", "ZoomSDK has not been initialized successfully");
        return;
      }

      final MeetingService meetingService = mZoomSDK.getMeetingService();

      JoinMeetingOptions opts = new JoinMeetingOptions();
      // Some available options

      // Query whether to hide DRIVING MODE.
      opts.no_driving_mode = true;
      // Query whether to hide INVITATION.
      opts.no_invite = true;
      // Query whether to hide MESSAGE OF ENDING MEETING.
      opts.no_meeting_end_message = true;
      // Query whether to hide MEETING ERROR MESSAGE.
      // opts.no_meeting_error_message = false;
      // Query whether to hide MEETING TITLE-BAR.
      opts.no_titlebar = false;
      // Query whether to hide TOOLBAR at bottom.
      opts.no_bottom_toolbar = false;
      // Query whether to hide CALL IN BY PHONE.
      opts.no_dial_in_via_phone = true;
      // Query whether to hide CALL OUT.
      opts.no_dial_out_to_phone = true;
      // Query whether to hide DISCONNECT AUDIO.
      opts.no_disconnect_audio = true;
      // Query whether to hide SHARE.
      opts.no_share = true;
      // Query whether to hide VIDEO.
      opts.no_video = false;
      // Meeting view options.
      // opts.meeting_views_options = Int;
      // Invitation options. The host can enable all the items to invite attendees.
      // opts.invite_options = Int;
      // Participant ID.
      // opts.participant_id = String;
      // Set to change meeting ID displayed on meeting view title.
      opts.custom_meeting_id = "Firefly Health";
      // Query whether to hide host unmute yourself confirm dialog.
      opts.no_unmute_confirm_dialog = true;
      // Query whether to hide webinar need register dialog.
      opts.no_webinar_register_dialog = true;

      JoinMeetingParams params = new JoinMeetingParams();
      params.displayName = displayName;
      params.meetingNo = meetingNo;

      int joinMeetingResult = meetingService.joinMeetingWithParams(reactContext.getCurrentActivity(), params, opts);

      if (joinMeetingResult != MeetingError.MEETING_ERROR_SUCCESS) {
        toast("Nope. Error: " + joinMeetingResult);
        return;
        // promise.reject("ERR_ZOOM_JOIN", "joinMeeting, errorCode=" +
        // joinMeetingResult);
      }
      // if (joinMeetingResult == ZoomError.ZOOM_ERROR_DEVICE_NOT_SUPPORTED) {
      // promise.reject("ERR_ZOOM_JOIN", "Device not supported.");
      // }
      // if (joinMeetingResult != MeetingError.MEETING_ERROR_SUCCESS) {
      // promise.reject("ERR_ZOOM_JOIN", "joinMeeting, errorCode=" +
      // joinMeetingResult);
      // }
    } catch (Exception ex) {
      promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
    }
  }

  @Override
  public void onMeetingStatusChanged(MeetingStatus meetingStatus, int errorCode, int internalErrorCode) {
    toast("Status Changed: " + errorCode + "/" + internalErrorCode);
    if (meetingStatus == meetingStatus.MEETING_STATUS_FAILED
        && errorCode == MeetingError.MEETING_ERROR_CLIENT_INCOMPATIBLE) {
      toast("Version of ZoomSDK is too low!");
    }

    if (meetingStatus == MeetingStatus.MEETING_STATUS_IDLE || meetingStatus == MeetingStatus.MEETING_STATUS_FAILED) {
      toast("This meeting is idle");
    }
  }

}
