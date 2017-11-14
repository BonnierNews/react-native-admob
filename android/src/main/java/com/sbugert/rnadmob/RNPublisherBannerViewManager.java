package com.sbugert.rnadmob;

import android.content.Context;
import android.location.Location;
import android.support.annotation.Nullable;
import android.view.View;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableNativeMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableNativeArray;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.PixelUtil;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.facebook.react.views.view.ReactViewGroup;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.doubleclick.AppEventListener;
import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.doubleclick.PublisherAdView;

import java.sql.Array;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

class ReactPublisherAdView extends ReactViewGroup implements AppEventListener {

    protected PublisherAdView adView;

    String[] testDevices;
    AdSize[] validAdSizes;
    String adUnitID;
    AdSize adSize;
    ReadableMap targeting;

    public ReactPublisherAdView(final Context context) {
        super(context);
        this.createAdView();
    }

    private void createAdView() {
        if (this.adView != null) this.adView.destroy();

        final Context context = getContext();
        this.adView = new PublisherAdView(context);
        this.adView.setAppEventListener(this);
        this.adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                int width = adView.getAdSize().getWidthInPixels(context);
                int height = adView.getAdSize().getHeightInPixels(context);
                int left = adView.getLeft();
                int top = adView.getTop();
                adView.measure(width, height);
                adView.layout(left, top, left + width, top + height);
                sendOnSizeChangeEvent();
                sendEvent(RNPublisherBannerViewManager.EVENT_AD_LOADED, null);
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                String errorMessage = "Unknown error";
                switch (errorCode) {
                    case PublisherAdRequest.ERROR_CODE_INTERNAL_ERROR:
                        errorMessage = "Internal error, an invalid response was received from the ad server.";
                        break;
                    case PublisherAdRequest.ERROR_CODE_INVALID_REQUEST:
                        errorMessage = "Invalid ad request, possibly an incorrect ad unit ID was given.";
                        break;
                    case PublisherAdRequest.ERROR_CODE_NETWORK_ERROR:
                        errorMessage = "The ad request was unsuccessful due to network connectivity.";
                        break;
                    case PublisherAdRequest.ERROR_CODE_NO_FILL:
                        errorMessage = "The ad request was successful, but no ad was returned due to lack of ad inventory.";
                        break;
                }
                WritableMap event = Arguments.createMap();
                WritableMap error = Arguments.createMap();
                error.putString("message", errorMessage);
                event.putMap("error", error);
                sendEvent(RNPublisherBannerViewManager.EVENT_AD_FAILED_TO_LOAD, event);
            }

            @Override
            public void onAdOpened() {
                sendEvent(RNPublisherBannerViewManager.EVENT_AD_OPENED, null);
            }

            @Override
            public void onAdClosed() {
                sendEvent(RNPublisherBannerViewManager.EVENT_AD_CLOSED, null);
            }

            @Override
            public void onAdLeftApplication() {
                sendEvent(RNPublisherBannerViewManager.EVENT_AD_LEFT_APPLICATION, null);
            }
        });
        this.addView(this.adView);
    }

    private void sendOnSizeChangeEvent() {
        int width;
        int height;
        ReactContext reactContext = (ReactContext) getContext();
        WritableMap event = Arguments.createMap();
        AdSize adSize = this.adView.getAdSize();
        if (adSize == AdSize.SMART_BANNER) {
            width = (int) PixelUtil.toDIPFromPixel(adSize.getWidthInPixels(reactContext));
            height = (int) PixelUtil.toDIPFromPixel(adSize.getHeightInPixels(reactContext));
        } else {
            width = adSize.getWidth();
            height = adSize.getHeight();
        }
        event.putDouble("width", width);
        event.putDouble("height", height);
        sendEvent(RNPublisherBannerViewManager.EVENT_SIZE_CHANGE, event);
    }

    private void sendEvent(String name, @Nullable WritableMap event) {
        ReactContext reactContext = (ReactContext) getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                getId(),
                name,
                event);
    }

    public void loadBanner() {
        ArrayList<AdSize> adSizes = new ArrayList<AdSize>();
        if (this.adSize != null) {
            adSizes.add(this.adSize);
        }
        if (this.validAdSizes != null) {
            for (int i = 0; i < this.validAdSizes.length; i++) {
                adSizes.add(this.validAdSizes[i]);
            }
        }

        if (adSizes.size() == 0) {
            adSizes.add(AdSize.BANNER);
        }

        AdSize[] adSizesArray = adSizes.toArray(new AdSize[adSizes.size()]);
        this.adView.setAdSizes(adSizesArray);

        PublisherAdRequest.Builder adRequestBuilder = new PublisherAdRequest.Builder();
        if (testDevices != null) {
            for (int i = 0; i < testDevices.length; i++) {
                adRequestBuilder.addTestDevice(testDevices[i]);
            }
        }

        if (targeting != null) {
            if (targeting.hasKey("customTargeting")) {
                ReadableMap customTargeting = targeting.getMap("customTargeting");
                ReadableMapKeySetIterator iterator = customTargeting.keySetIterator();
                while (iterator.hasNextKey()) {
                    String key = iterator.nextKey();
                    String value = "";
                    switch (customTargeting.getType(key)) {
                        case Boolean:
                            value = Boolean.toString(customTargeting.getBoolean(key));
                            adRequestBuilder.addCustomTargeting(key, value);
                            break;
                        case Number:
                            double d = customTargeting.getDouble(key);
                            if ((d % 1) == 0) {
                                value = Integer.toString((int) d);
                            } else {
                                value = Double.toString(d);
                            }
                            adRequestBuilder.addCustomTargeting(key, value);
                            break;
                        case String:
                            value = customTargeting.getString(key);
                            adRequestBuilder.addCustomTargeting(key, value);
                            break;
                        case Array:
                            ReadableArray a = customTargeting.getArray(key);
                            List<String> values = new ArrayList<String>();
                            for (int i = 0; i < a.size(); i++) {
                                switch (a.getType(i)) {
                                    case String:
                                        values.add(a.getString(i));
                                        break;
                                    default:
                                        break;
                                }
                            }
                            adRequestBuilder.addCustomTargeting(key, values);
                        default:
                            break;
                    }
                }
            }

            if (targeting.hasKey("categoryExclusions")) {
                ReadableArray categoryExclusions = targeting.getArray("categoryExclusions");
                for (int i = 0; i < categoryExclusions.size(); i++) {
                    try {
                        String category = categoryExclusions.getString(i);
                        adRequestBuilder.addCategoryExclusion(category);
                    } catch (Exception e) {
                    }
                }
            }

            if (targeting.hasKey("keywords")) {
                ReadableArray keywords = targeting.getArray("keywords");
                for (int i = 0; i < keywords.size(); i++) {
                    try {
                        String keyword = keywords.getString(i);
                        adRequestBuilder.addCategoryExclusion(keyword);
                    } catch (Exception e) {
                    }
                }
            }

            if (targeting.hasKey("gender")) {
                try {
                    String gender = targeting.getString("gender");
                    if (gender == "male") {
                        adRequestBuilder.setGender(PublisherAdRequest.GENDER_MALE);
                    } else if (gender == "female") {
                        adRequestBuilder.setGender(PublisherAdRequest.GENDER_FEMALE);
                    } else {
                        adRequestBuilder.setGender(PublisherAdRequest.GENDER_UNKNOWN);
                    }
                } catch (Exception e) {
                }
            }

      /*if (targeting.hasKey("birthday")) {
        try {
          String birthdayString = targeting.getString("birthday");
          DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.ENGLISH);
          Date birthday = new Date(format.parse(birthdayString).getTime());
          adRequestBuilder.setBirthday(birthday);
          Log.d("PublisherAdBanner", "birthday");
        } catch (Exception e) {
          Log.d("PublisherAdBanner", "could not set birthday");
        }
      }*/

            if (targeting.hasKey("childDirectedTreatment")) {
                Boolean childDirectedTreatment = targeting.getBoolean("childDirectedTreatment");
                adRequestBuilder.tagForChildDirectedTreatment(childDirectedTreatment);
            }

            if (targeting.hasKey("contentURL")) {
                try {
                    String contentURL = targeting.getString("contentURL");
                    adRequestBuilder.setContentUrl(contentURL);
                } catch (Exception e) {
                }
            }

            if (targeting.hasKey("publisherProvidedID")) {
                try {
                    String publisherProvidedID = targeting.getString("publisherProvidedID");
                    adRequestBuilder.setPublisherProvidedId(publisherProvidedID);
                } catch (Exception e) {
                }
            }

            if (targeting.hasKey("location")) {
                try {
                    ReadableMap locationMap = targeting.getMap("location");
                    double lat = locationMap.getDouble("latitude");
                    double lon = locationMap.getDouble("longitude");
                    double accuracy = locationMap.getDouble("accuracy");
                    Location location = new Location("");
                    location.setLatitude(lat);
                    location.setLongitude(lon);
                    location.setAccuracy((float) accuracy);
                    adRequestBuilder.setLocation(location);
                } catch (Exception e) {
                }
            }
        }

        PublisherAdRequest adRequest = adRequestBuilder.build();
        this.adView.loadAd(adRequest);
    }

    public void setAdUnitID(String adUnitID) {
        if (this.adUnitID != null) {
            // We can only set adUnitID once, so when it was previously set we have
            // to recreate the view
            this.createAdView();
        }
        this.adUnitID = adUnitID;
        this.adView.setAdUnitId(adUnitID);
    }

    public void setTestDevices(String[] testDevices) {
        this.testDevices = testDevices;
    }

    public void setAdSize(AdSize adSize) {
        this.adSize = adSize;
    }

    public void setValidAdSizes(AdSize[] adSizes) {
        this.validAdSizes = adSizes;
    }

    public void setTargeting(ReadableMap targeting) {
        this.targeting = targeting;
    }

    @Override
    public void onAppEvent(String name, String info) {
        WritableMap event = Arguments.createMap();
        event.putString("name", name);
        event.putString("info", info);
        sendEvent(RNPublisherBannerViewManager.EVENT_APP_EVENT, event);
    }
}

public class RNPublisherBannerViewManager extends ViewGroupManager<ReactPublisherAdView> {

    public static final String REACT_CLASS = "RNDFPBannerView";

    public static final String PROP_AD_SIZE = "adSize";
    public static final String PROP_VALID_AD_SIZES = "validAdSizes";
    public static final String PROP_AD_UNIT_ID = "adUnitID";
    public static final String PROP_TEST_DEVICES = "testDevices";
    public static final String PROP_TARGETING = "targeting";

    public static final String EVENT_SIZE_CHANGE = "onSizeChange";
    public static final String EVENT_AD_LOADED = "onAdLoaded";
    public static final String EVENT_AD_FAILED_TO_LOAD = "onAdFailedToLoad";
    public static final String EVENT_AD_OPENED = "onAdOpened";
    public static final String EVENT_AD_CLOSED = "onAdClosed";
    public static final String EVENT_AD_LEFT_APPLICATION = "onAdLeftApplication";
    public static final String EVENT_APP_EVENT = "onAppEvent";

    public static final int COMMAND_LOAD_BANNER = 1;

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    protected ReactPublisherAdView createViewInstance(ThemedReactContext themedReactContext) {
        ReactPublisherAdView adView = new ReactPublisherAdView(themedReactContext);
        return adView;
    }

    @Override
    public void addView(ReactPublisherAdView parent, View child, int index) {
        throw new RuntimeException("RNPublisherBannerView cannot have subviews");
    }

    @Override
    @Nullable
    public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
        MapBuilder.Builder<String, Object> builder = MapBuilder.builder();
        String[] events = {
                EVENT_SIZE_CHANGE,
                EVENT_AD_LOADED,
                EVENT_AD_FAILED_TO_LOAD,
                EVENT_AD_OPENED,
                EVENT_AD_CLOSED,
                EVENT_AD_LEFT_APPLICATION,
                EVENT_APP_EVENT
        };
        for (int i = 0; i < events.length; i++) {
            builder.put(events[i], MapBuilder.of("registrationName", events[i]));
        }
        return builder.build();
    }

    @ReactProp(name = PROP_AD_SIZE)
    public void setPropAdSize(final ReactPublisherAdView view, final String sizeString) {
        AdSize adSize = getAdSizeFromString(sizeString);
        view.setAdSize(adSize);
    }

    @ReactProp(name = PROP_VALID_AD_SIZES)
    public void setPropValidAdSizes(final ReactPublisherAdView view, final ReadableArray adSizeStrings) {
        ReadableNativeArray nativeArray = (ReadableNativeArray) adSizeStrings;
        ArrayList<Object> list = nativeArray.toArrayList();
        String[] adSizeStringsArray = list.toArray(new String[list.size()]);
        AdSize[] adSizes = new AdSize[list.size()];

        for (int i = 0; i < adSizeStringsArray.length; i++) {
            String adSizeString = adSizeStringsArray[i];
            adSizes[i] = getAdSizeFromString(adSizeString);
        }
        view.setValidAdSizes(adSizes);
    }

    @ReactProp(name = PROP_AD_UNIT_ID)
    public void setPropAdUnitID(final ReactPublisherAdView view, final String adUnitID) {
        view.setAdUnitID(adUnitID);
    }

    @ReactProp(name = PROP_TEST_DEVICES)
    public void setPropTestDevices(final ReactPublisherAdView view, final ReadableArray testDevices) {
        ReadableNativeArray nativeArray = (ReadableNativeArray) testDevices;
        ArrayList<Object> list = nativeArray.toArrayList();
        view.setTestDevices(list.toArray(new String[list.size()]));
    }

    @ReactProp(name = PROP_TARGETING)
    public void setPropTargeting(final ReactPublisherAdView view, final ReadableMap targeting) {
        view.setTargeting(targeting);
    }

    private AdSize getAdSizeFromString(String adSize) {
        switch (adSize) {
            case "banner":
                return AdSize.BANNER;
            case "largeBanner":
                return AdSize.LARGE_BANNER;
            case "mediumRectangle":
                return AdSize.MEDIUM_RECTANGLE;
            case "fullBanner":
                return AdSize.FULL_BANNER;
            case "leaderBoard":
                return AdSize.LEADERBOARD;
            case "smartBannerPortrait":
                return AdSize.SMART_BANNER;
            case "smartBannerLandscape":
                return AdSize.SMART_BANNER;
            case "smartBanner":
                return AdSize.SMART_BANNER;
            default:
                return AdSize.BANNER;
        }
    }

    @Nullable
    @Override
    public Map<String, Object> getExportedViewConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put("simulatorId", PublisherAdRequest.DEVICE_ID_EMULATOR);
        return constants;
    }

    @Nullable
    @Override
    public Map<String, Integer> getCommandsMap() {
        return MapBuilder.of("loadBanner", COMMAND_LOAD_BANNER);
    }

    @Override
    public void receiveCommand(ReactPublisherAdView root, int commandId, @javax.annotation.Nullable ReadableArray args) {
        switch (commandId) {
            case COMMAND_LOAD_BANNER:
                root.loadBanner();
                break;
        }
    }
}
